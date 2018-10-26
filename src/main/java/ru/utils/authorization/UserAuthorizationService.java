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
    private String fullUserName;
    private String session;
    private long sessionDuration = 900000;
    private Error error = Error.NO_ERROR;
    private StringBuilder errorMessage = new StringBuilder();
    private boolean doneCreateTable = false;
    public enum Error {NO_ERROR, EMPTY, LOGIN, PASSWORD, DOUBLE, CONNECT, EXEC}


    public Error getError() {
        return error;
    }
    public String getErrorMessage() {
        return errorMessage.toString();
    }

    public DBService dbService() {
        return dbService;
    }

    public String getFullUserName() {
        return fullUserName;
    }

    public String getSession() {
        return session;
    }

    public void setSessionDuration(long sessionDuration){
        this.sessionDuration = sessionDuration;
    }

    public void setLoggerLevel(Level loggerLevel){
        Configurator.setLevel(LOG.getName(), loggerLevel);
        dbService.setLoggerLevel(loggerLevel);
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
            String dbBase,
            String dbUserName,
            String dbPassword) {

        return connect(
                typeDB,
                dbHost,
                1251,
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

            if (doneCreateTable || createTables()){ r = true; }
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

    private boolean createTables(){
        boolean r = false;

        switch (typeDB) {
            case HSQLDB:
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
                                "TIME_END NUMERIC(20,0))")) {
                    r = true;
                }
                break;

            case ORACLE:
                if (dbService.execute(
                        "declare\n" +
                            "cnt int;\n" +
                            "begin\n" +
                            "select count(*) into cnt from all_tables where table_name='USERS';\n" +
                            "if (cnt = 0) then\n" +
                            "execute immediate ('CREATE TABLE \"USERS\" (\"USERNAME\" VARCHAR(25), \"FULLUSERNAME\" VARCHAR(100), \"PASSWORD\" VARCHAR(50))');\n" +
                            "end if;\n" +
                            "commit;\n" +
                            "end;")
                    &&
                    dbService.execute(
                        "declare\n" +
                            "cnt int;\n" +
                            "begin\n" +
                            "select count(*) into cnt from all_tables where table_name='SESSIONS';\n" +
                            "if (cnt = 0) then\n" +
                            "execute immediate ('CREATE TABLE \"SESSIONS\" (\"USERNAME\" VARCHAR(25), \"SESSION_ID\" VARCHAR(50), \"TIME_END\" NUMERIC(20,0))');\n" +
                            "end if;\n" +
                            "commit;\n" +
                            "end;")) {
                    r = true;
                }
                break;

            case SQLSERVER:
                if (dbService.execute(
                    "if not exists(select 1 from sysobjects where id = object_id('dbo.USERS') and xtype = 'U')\n" +
                        "begin\n" +
                        "CREATE TABLE [dbo].[USERS](\n" +
                        "[USERNAME] [varchar] (25),\n" +
                        "[FULLUSERNAME] [varchar](100) NULL,\n" +
                        "[PASSWORD] [varchar](50)) ON [PRIMARY]\n" +
                        "end")
                    &&
                    dbService.execute(
                    "if not exists(select 1 from sysobjects where id = object_id('dbo.SESSIONS') and xtype = 'U')\n" +
                        "begin\n" +
                        "CREATE TABLE [dbo].[SESSIONS](\n" +
                        "[USERNAME] [varchar] (25),\n" +
                        "[SESSION_ID] [varchar](50) NULL,\n" +
                        "[TIME_END] [numeric](20,0)) ON [PRIMARY]\n" +
                        "end") ) {
                    r = true;
                }
                break;
        }
        if (r) {doneCreateTable = true;}
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
        error = Error.NO_ERROR;
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
                                error = Error.PASSWORD;
                                errorMessage.append("Неверный пароль для пользователя ")
                                        .append(userName)
                                        .append(" (")
                                        .append(fullUserName)
                                        .append(")");
                            }
                        } else {
                            LOG.warn("Авторизация пользователя: {} - пользователь не зарегистрирован", userName);
                            error = Error.LOGIN;
                            errorMessage.append("Пользователь ")
                                    .append(userName)
                                    .append(" не зарегистрирован");
                        }
                        resultSet.close();
                        preparedStatement.close();

                    } catch (SQLException e) {
                        LOG.error(e);
                        error = Error.EXEC;
                        errorMessage.append(e);
                    }
                } else {
                    error = Error.EMPTY;
                    errorMessage.append("Необходимо указать пароль");
                }
            } else {
                error = Error.EMPTY;
                errorMessage.append("Необходимо указать пользователя");
            }
        } else {
            error = Error.CONNECT;
            errorMessage.append("Отсутствует подключение к базе данных");
        }
        return res;
    }

    @Override
    public boolean isSessionCorrect() {
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
            LOG.error(e);
        }
        return r;
    }

    @Override
    public boolean userAdd(String userName, String fullUserName, String password, String password2) {
        boolean res = false;
        this.fullUserName = "";
        error = Error.NO_ERROR;
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
                        error = Error.DOUBLE;
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
                    error = Error.EXEC;
                    errorMessage.append(e);
                }
            } else {
                error = Error.EMPTY;
                errorMessage.append("Ошибка регистрации пользователя ").append(userName);
                if (password.isEmpty()) { errorMessage.append(" - пароль не может быть пустым"); }
                else { errorMessage.append(" - пароль и подтверждение не совпадают"); }
            }
        } else {
            error = Error.CONNECT;
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
