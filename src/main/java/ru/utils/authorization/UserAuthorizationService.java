package ru.utils.authorization;

import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import ru.utils.db.DBService;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.UUID;

/**
 * Created by Сергей on 01.05.2018.
 */
public class UserAuthorizationService implements UserAuthorizationServiceI {

    private final Logger LOG = LogManager.getLogger();

    private DBService dbService = new DBService();
    private DBService.TypeDB typeDB;
    private String dbHost;
    private int    dbPort;
    private String dbBase;
    private String dbUserName;
    private String dbPassword;
    private StringBuilder errorMessage = new StringBuilder();
    private String fullUserName;
    private String session;
    private long sessionDuration = 900000;
    private ErrorList error = ErrorList.NoError;
    public enum ErrorList{NoError, Empty, Login, Password, Double, Connect, Exec}


    public ErrorList getError() {
        return error;
    }

    public void setSessionDuration(long sessionDuration){
        this.sessionDuration = sessionDuration;
    }

    public boolean connect(){
        return connect(
                typeDB,
                dbHost,
                dbPort,
                dbBase,
                dbUserName,
                dbPassword);
    }

    public boolean connect(
            DBService.TypeDB typeDB,
            String dbHost,
            int    dbPort,
            String dbBase,
            String dbUserName,
            String dbPassword) {

        boolean r = false;
        this.typeDB = typeDB;
        this.dbHost = dbHost;
        this.dbPort = dbPort;
        this.dbBase = dbBase;
        this.dbUserName = dbUserName;
        this.dbPassword = dbPassword;

        if (dbService.connect(
                typeDB,
                dbHost,
                dbPort,
                dbBase,
                dbUserName,
                dbPassword)){

            if (createTables()){
                r = true;
            }

        }
        return r;
    }

    public void disconnect(){
        dbService.disconnect();
    }

    public void endSession(){
        LOG.info("End session: {}", session);
        PreparedStatement preparedStatement = null;
        try {
            if (connect()) {
                preparedStatement = dbService.connection().prepareStatement("DELETE FROM SESSIONS WHERE SESSION_ID = ?");
                preparedStatement.setString(1, session);
                preparedStatement.execute();
                preparedStatement.close();
                disconnect();
            }
        } catch (SQLException e) {
            LOG.error("End session ", e);
        }
    }

    public void setLoggerLevel(Level loggerLevel){
        Configurator.setLevel(LOG.getName(), loggerLevel);
        dbService.setLoggerLevel(loggerLevel);
    }

    public String getErrorMessage() {
        return errorMessage.toString();
    }

    public String getFullUserName() {
        return fullUserName;
    }

    public DBService dbService() {
        return dbService;
    }

    public String getSession() {
        return session;
    }

    private boolean createTables(){
        boolean r = false;
//        dbService.execute("drop table SESSIONS");

        if (dbService.execute(
                "CREATE TABLE IF NOT EXISTS USERS (" +
                        "ID IDENTITY, " +
                        "USERNAME VARCHAR(25), " +
                        "FULLUSERNAME VARCHAR(100), " +
                        "PASSWORD VARCHAR(50))")
                &&
            dbService.execute(
                "CREATE TABLE IF NOT EXISTS SESSIONS (" +
                        "USERNAME VARCHAR(25), " +
                        "SESSION_ID VARCHAR(50), " +
                        "TIME_END NUMERIC(20,0))")){
            r = true;
        }
        return r;
    }

    public String encryptMD5(String data) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
            md.reset();
            md.update(data.getBytes(Charset.forName("UTF8")));
        } catch (NoSuchAlgorithmException e) {
            LOG.error(e);
        }
        byte[] digest = md.digest();
        return new String(Hex.encodeHex(digest));
    }

    @Override
    public boolean isUserCorrect(String userName, String fullUserName, String password, String password2){
        boolean res= true;
        if (password2 != null) {
            if (!(res = userAdd(
                    userName,
                    fullUserName,
                    password,
                    password2))) {
            }
        }
        if (res){ res = isUserCorrect(userName, password); }
        return res;
    }

    @Override
    public boolean isUserCorrect(String userName, String password) {
        boolean res = false;
        fullUserName = "";
        error = ErrorList.NoError;
        errorMessage.setLength(0);
        if (dbService.connection() != null) {
            if (userName != null && !userName.isEmpty()) {
                if (password != null && !password.isEmpty()) {
                    PreparedStatement preparedStatement = null;
                    try {
                        preparedStatement = dbService.connection().prepareStatement("select PASSWORD, FULLUSERNAME from users where LOWER(USERNAME) = ?");
                        preparedStatement.setString(1, userName.toLowerCase());
                        ResultSet resultSet = preparedStatement.executeQuery();
                        if (resultSet.next()) {
                            fullUserName = resultSet.getString(2);
                            if (encryptMD5(password).equals(resultSet.getString(1))) {
                                LOG.info("Авторизация пользователя: {} ({}) - успешно", userName, fullUserName);

                                this.session = UUID.randomUUID().toString();
                                LOG.debug("session: {}", session);

                                // наличие старой сессии
                                preparedStatement = dbService.connection().prepareStatement("SELECT SESSION_ID FROM SESSIONS WHERE USERNAME = ? and TIME_END < ?");
                                preparedStatement.setString(1, userName.toLowerCase());
                                preparedStatement.setLong(2, System.currentTimeMillis());
                                resultSet = preparedStatement.executeQuery();
                                if (resultSet.next()){
                                    preparedStatement = dbService.connection().prepareStatement("UPDATE SESSIONS SET SESSION_ID = ?, TIME_END = ? WHERE SESSION_ID = ?");
                                    preparedStatement.setString(1, session);
                                    preparedStatement.setLong(2, System.currentTimeMillis() + sessionDuration);
                                    preparedStatement.setString(3, resultSet.getString(1));                                    ;
                                    preparedStatement.executeUpdate();
                                } else { // записи с неактуальной сессией не обнаружено, создаем новую запись
                                    preparedStatement = dbService.connection().prepareStatement("INSERT INTO SESSIONS (USERNAME, SESSION_ID, TIME_END) VALUES(?, ?, ?)");
                                    preparedStatement.setString(1, userName.toLowerCase());
                                    preparedStatement.setString(2, session);
                                    preparedStatement.setLong(3, System.currentTimeMillis() + sessionDuration);
                                    preparedStatement.executeUpdate();
                                }
                                res = true;

                            } else {
                                LOG.warn("Авторизация пользователя: {} ({}) - неверный пароль", userName, fullUserName);
                                error = ErrorList.Password;
                                errorMessage.append("Неверный пароль для пользователя ")
                                        .append(userName)
                                        .append(" (")
                                        .append(fullUserName)
                                        .append(")");
                            }
                        } else {
                            LOG.warn("Авторизация пользователя: {} - пользователь не зарегистрирован", userName);
                            error = ErrorList.Login;
                            errorMessage.append("Пользователь ")
                                    .append(userName)
                                    .append(" не зарегистрирован");
                        }
                        resultSet.close();
                        preparedStatement.close();

                    } catch (SQLException e) {
                        LOG.error(e);
                        error = ErrorList.Exec;
                        errorMessage.append(e);
                    }
                } else {
                    error = ErrorList.Empty;
                    errorMessage.append("Необходимо указать пароль");
                }
            } else {
                error = ErrorList.Empty;
                errorMessage.append("Необходимо указать пользователя");
            }
        } else {
            error = ErrorList.Connect;
            errorMessage.append("Отсутствует подключение к базе данных");
        }
        return res;
    }

    @Override
    public boolean isSessionCorrect(String session) {
        boolean r = false;
        try {
            if (connect()){
                PreparedStatement preparedStatement = dbService.connection().prepareStatement("SELECT TIME_END FROM SESSIONS WHERE SESSION_ID = ?");
                preparedStatement.setString(1, session);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()){
                    if (resultSet.getLong(1) > System.currentTimeMillis()) {
                        preparedStatement = dbService.connection().prepareStatement("UPDATE SESSIONS SET TIME_END = ? WHERE SESSION_ID = ?");
                        preparedStatement.setLong(1, System.currentTimeMillis() + sessionDuration);
                        preparedStatement.setString(2, session);
                        preparedStatement.executeUpdate();
                        r = true;
                    }
                }
                resultSet.close();
                preparedStatement.close();
                disconnect();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return r;
    }

    @Override
    public boolean userAdd(String userName, String fullUserName, String password, String password2) {
        boolean res = false;
        this.fullUserName = "";
        error = ErrorList.NoError;
        errorMessage.setLength(0);
        if (dbService.connection() != null) {
            if (!password.isEmpty() & password.equals(password2)) {
                PreparedStatement preparedStatement = null;
                try {
                    preparedStatement = dbService.connection().prepareStatement("select FULLUSERNAME from users where LOWER(USERNAME) = ?");
                    preparedStatement.setString(1, userName.toLowerCase());
                    ResultSet resultSet = preparedStatement.executeQuery();
                    if (resultSet.next()) {
                        fullUserName = resultSet.getString(1);
                        LOG.warn("Регистрация пользователя: {} ({}) - пользователь уже зарегистрирован", userName, fullUserName);
                        error = ErrorList.Double;
                        errorMessage.append("Пользователь ")
                                .append(userName)
                                .append(" (")
                                .append(fullUserName)
                                .append(") уже зарегистрирован");
                    } else {
                        preparedStatement.close();
                        preparedStatement = dbService.connection().prepareStatement("INSERT INTO users (USERNAME, FULLUSERNAME, PASSWORD) VALUES(?, ?, ?)");
                        preparedStatement.setString(1, userName);
                        preparedStatement.setString(2, fullUserName);
                        preparedStatement.setString(3, encryptMD5(password));
                        preparedStatement.executeUpdate();
                        res = true;
                        this.fullUserName = fullUserName;
                        LOG.info("Регистрация пользователя: {}, {}, {} - успешно",
                                userName,
                                fullUserName,
                                encryptMD5(password));
                    }
                    resultSet.close();
                    preparedStatement.close();

                } catch (SQLException e) {
                    LOG.error(e);
                    error = ErrorList.Exec;
                    errorMessage.append(e);
                }
            } else {
                error = ErrorList.Empty;
                errorMessage.append("Ошибка регистрации пользователя ").append(userName);
                if (password.isEmpty()) { errorMessage.append(" - пароль не может быть пустым"); }
                else { errorMessage.append(" - пароль и подтверждение не совпадают"); }
            }
        } else {
            error = ErrorList.Connect;
            errorMessage.append("Отсутствует подключение к базе данных");
        }
        return res;
    }

    @Override
    public boolean userUpdate(String userName, String password) {
        boolean r = false;
        return r;
    }
}
