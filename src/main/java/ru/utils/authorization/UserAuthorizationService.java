package ru.utils.authorization;

import org.apache.commons.codec.binary.Hex;
/*
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
*/
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.utils.db.DBService;
import ru.utils.db.DBType;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.UUID;

/**
 * Created by Сергей on 01.05.2018.
 */
public class UserAuthorizationService implements UserAuthorizationServiceI {

//    private static final Logger LOG = LogManager.getLogger();
    private static final Logger LOG = LoggerFactory.getLogger(UserAuthorizationService.class);

    DateFormat dateFormatTime = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    private Level loggerLevel = null;
    private boolean dbServiceIn = false;
    private DBService dbService = null;
    private DBType dbType = null;
    private String dbUrl;
    private String dbHost;
    private String dbBase;
    private int dbPort = 1521;
    private String dbUserName;
    private String dbPassword;
    private String userName;
    private String fullUserName;
    private String session;
    private long timeEnd;
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

    public DBService getDbService() {
        return dbService;
    }

    public String getUserName() {
        return userName;
    }

    public String getFullUserName() {
        return fullUserName;
    }

    public String getSession() {
        return session;
    }

    public long getTimeEnd() {
        return timeEnd;
    }

    public void setSessionDuration(long sessionDuration) {
        this.sessionDuration = sessionDuration;
    }

    public void setLoggerLevel(Level loggerLevel) {
        Configurator.setLevel(LOG.getName(), loggerLevel);
        if (dbService != null) {
            dbService.setLoggerLevel(loggerLevel);
        }
    }


    public void setDbService(DBService dbService) {
        this.dbService = dbService;
    }

    public static class Builder {
        private Level loggerLevel = null;
        private DBService dbService = null;
        private DBType dbType = null;
        private String dbUrl;
        private String dbHost;
        private String dbBase;
        private int dbPort = 1521;
        private String dbUserName;
        private String dbPassword;

        public Builder loggerLevel(Level val) {
            loggerLevel = val;
            return this;
        }

        public Builder dbService(DBService val) {
            dbService = val;
            return this;
        }

        public Builder dbType(DBType val) {
            dbType = val;
            return this;
        }

        public Builder dbUrl(String val) {
            dbUrl = val;
            return this;
        }

        public Builder dbHost(String val) {
            dbHost = val;
            return this;
        }

        public Builder dbBase(String val) {
            dbBase = val;
            return this;
        }

        public Builder dbPort(int val) {
            dbPort = val;
            return this;
        }

        public Builder dbUserName(String val) {
            dbUserName = val;
            return this;
        }

        public Builder dbPassword(String val) {
            dbPassword = val;
            return this;
        }

        public UserAuthorizationService build() {
            return new UserAuthorizationService(this);
        }
    }

    private UserAuthorizationService(Builder builder) {
        loggerLevel = builder.loggerLevel;
        dbService = builder.dbService;
        dbType = builder.dbType;
        dbUrl = builder.dbUrl;
        dbHost = builder.dbHost;
        dbBase = builder.dbBase;
        dbPort = builder.dbPort;
        dbUserName = builder.dbUserName;
        dbPassword = builder.dbPassword;

        if (dbService == null) {
            this.dbServiceIn = true;

            if (dbType == null) {
                dbType = DBType.HSQLDB;
            }
            if (dbHost == null) {
                dbHost = "hsqlUsers";
            }
            if (dbBase == null) {
                dbBase = "dbUsers";
            }
            if (dbUserName == null) {
                dbUserName = "admin";
            }
            if (dbPassword == null) {
                dbPassword = "admin";
            }
        }

        if (loggerLevel != null) {
            setLoggerLevel(loggerLevel);
        }
    }

    public boolean connect() {

        boolean r = false;

        if (dbService != null && dbService.isConnection()) {
            LOG.trace("SQL Подключение активно, используем: {}", dbService.getConnectInfo());

            if (doneCreateTable || createTables()) {
                return true;
            } else {
                return false;
            }
        }

        if (dbService == null) {
            dbService = new DBService.Builder()
                    .dbType(dbType)
                    .dbUrl(dbUrl)
                    .dbHost(dbHost)
                    .dbBase(dbBase)
                    .dbPort(dbPort)
                    .dbUserName(dbUserName)
                    .dbPassword(dbPassword)
                    .loggerLevel(loggerLevel)
                    .build();
        }
        if (dbService.connect()) {
            if (doneCreateTable || createTables()) {
                r = true;
            }
        }
        if (!r) {
            this.error = Error.CONNECT;
            this.errorMessage.append("Ошибка при подключении к базе данных");
        }
        return r;
    }

    public void disconnect() {
        if (dbService != null && dbServiceIn) {
            LOG.debug("SQL Disconnect");
            dbService.close();
            dbService = null;
        }
    }


    public void endSession() {
        endSession(this.session);
    }

    public void endSession(String session) {
        LOG.info("End session: {} {}", getFullUserName(session), session);
        PreparedStatement preparedStatement = null;
        try {
            boolean c = false;
            if (dbService != null && dbService.isConnection()) {
                c = true;
            }
            if (connect()) {
                preparedStatement = dbService.connection().prepareStatement("DELETE FROM SESSIONS WHERE SESSION_ID = ?");
                preparedStatement.setString(1, session);
                preparedStatement.execute();
                preparedStatement.close();
                if (!c) { disconnect(); }
            }
        } catch (SQLException e) {
            LOG.error("End session ", e);
        }
    }

    public String getUserName(String session){
        return getFieldValue(session, "UserName");
    }

    public String getFullUserName(String session){
        return getFieldValue(session, "FullUserName");
    }

    public long getTimeEnd(String session){
        return Long.parseLong(getFieldValue(session, "Time_End"));
    }

    public String getFieldValue(String session, String field){
        String r = null;
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = dbService.connection().prepareStatement(
                        "select u.USERNAME, u.FULLUSERNAME, s.TIME_END " +
                            "from users u, SESSIONS s " +
                            "where s.USERNAME = u.USERNAME and s.SESSION_ID = ?");
            preparedStatement.setString(1, session);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                r = resultSet.getString(field);
            }
            resultSet.close();
            preparedStatement.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return r;
    }

    private boolean createTables(){
        LOG.debug("SQL Create tables");
        boolean r = false;
        switch (dbService.getDbType()) {
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
            LOG.error("", e);
        }
        byte[] digest = md.digest();
        return new String(Hex.encodeHex(digest));
    }

    @Override
    public boolean isUserCorrect(
            String userName,
            String fullUserName,
            String password,
            String password2){

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
    public boolean isUserCorrect(
            String userName,
            String password) {

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
                                this.session = UUID.randomUUID().toString();
                                LOG.trace("Авторизация пользователя: {} ({}) - успешно, {}",
                                        userName,
                                        fullUserName,
                                        session);

                                // наличие старой сессии
                                preparedStatement = dbService.connection().prepareStatement("SELECT SESSION_ID FROM SESSIONS WHERE USERNAME = ? and TIME_END < ?");
                                preparedStatement.setString(1, userName.toLowerCase());
                                preparedStatement.setLong(2, System.currentTimeMillis());
                                resultSet = preparedStatement.executeQuery();
                                timeEnd = System.currentTimeMillis() + sessionDuration;
                                if (resultSet.next()){
                                    preparedStatement = dbService.connection().prepareStatement("UPDATE SESSIONS SET SESSION_ID = ?, TIME_END = ? WHERE SESSION_ID = ?");
                                    preparedStatement.setString(1, session);
                                    preparedStatement.setLong(2, timeEnd);
                                    preparedStatement.setString(3, resultSet.getString(1));                                    ;
                                    preparedStatement.executeUpdate();
                                } else { // записи с неактуальной сессией не обнаружено, создаем новую запись
                                    preparedStatement = dbService.connection().prepareStatement("INSERT INTO SESSIONS (USERNAME, SESSION_ID, TIME_END) VALUES(?, ?, ?)");
                                    preparedStatement.setString(1, userName.toLowerCase());
                                    preparedStatement.setString(2, session);
                                    preparedStatement.setLong(3, timeEnd);
                                    preparedStatement.executeUpdate();
                                }
                                res = true;

                                LOG.info("Авторизация пользователя: {} ({}) - успешно, {} до {}",
                                        userName,
                                        fullUserName,
                                        session,
                                        dateFormatTime.format(timeEnd));

                            } else {
                                LOG.warn("Авторизация пользователя: {} ({}) - неверный пароль",
                                        userName,
                                        fullUserName);

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
                        LOG.error("", e);
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
        return isSessionCorrect(this.session);
    }

    @Override
    public boolean isSessionCorrect(String session) {
        boolean r = false;
        try {
            boolean c = false;
            if (dbService != null && dbService.isConnection()) {c = true;}
            if (connect()){
                PreparedStatement preparedStatement = dbService.connection().prepareStatement("SELECT TIME_END FROM SESSIONS WHERE SESSION_ID = ?");
                preparedStatement.setString(1, session);
                ResultSet resultSet = preparedStatement.executeQuery();
                if (resultSet.next()){
                    if (resultSet.getLong(1) > System.currentTimeMillis()) {
                        timeEnd = System.currentTimeMillis() + sessionDuration;
                        preparedStatement = dbService.connection().prepareStatement("UPDATE SESSIONS SET TIME_END = ? WHERE SESSION_ID = ?");
                        preparedStatement.setLong(1, timeEnd);
                        preparedStatement.setString(2, session);
                        preparedStatement.executeUpdate();
                        r = true;
                    }
                }
                resultSet.close();
                preparedStatement.close();
                if (!c) { disconnect(); }
            }
        } catch (SQLException e) {
            LOG.error("", e);
        }
        return r;
    }


    @Override
    public boolean userAdd(
            String userName,
            String fullUserName,
            String password,
            String password2) {

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
                        LOG.warn("Регистрация пользователя: {} ({}) - пользователь уже зарегистрирован",
                                userName,
                                fullUserName);

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
                    LOG.error("", e);
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
    public boolean userUpdate(
            String userName,
            String password) {

        boolean r = false;
        return r;
    }
}
