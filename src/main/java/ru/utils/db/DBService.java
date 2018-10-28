package ru.utils.db;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.sql.*;

public class DBService {

    private static final Logger LOG = LogManager.getLogger();

    public enum TypeDB {
        HSQLDB    { public String getDriver() {return "org.hsqldb.jdbcDriver";} },
        ORACLE    { public String getDriver() {return "oracle.jdbc.driver.OracleDriver";} },
        SQLSERVER { public String getDriver() {return "com.microsoft.sqlserver.jdbc.SQLServerDriver";} };

        public abstract String getDriver();
    }

    private Connection connection = null;
    private Statement statement = null;

    private Level  loggerLevel = null;
    private TypeDB typeDB      = null;
    private String dbDriver    = null;
    private String dbHost;
    private String dbBase;
    private int    dbPort      = 1521;
    private String dbURL       = null;
    private String dbUserName;
    private String dbPassword;


    public static class Builder{
        private Level  loggerLevel = null;
        private TypeDB typeDB      = null;
        private String dbDriver    = null;
        private String dbHost;
        private String dbBase;
        private int    dbPort      = 1251;
        private String dbURL       = null;
        private String dbUserName;
        private String dbPassword;

        public Builder loggerLevel(Level val){
            loggerLevel = val;
            return this;
        }
        public Builder dbType(TypeDB val){
            typeDB = val;
            return this;
        }
        public Builder dbDriver(String val){
            dbDriver = val;
            return this;
        }
        public Builder dbHost(String val){
            dbHost = val;
            return this;
        }
        public Builder dbBase(String val){
            dbBase = val;
            return this;
        }
        public Builder dbPort(int val){
            dbPort = val;
            return this;
        }
        public Builder dbURL(String val){
            dbURL = val;
            return this;
        }
        public Builder dbUserName(String val){
            dbUserName = val;
            return this;
        }
        public Builder dbPassword(String val){
            dbPassword = val;
            return this;
        }
        public DBService build(){
            return new DBService(this);
        }
    }

    private DBService(Builder builder){
        loggerLevel = builder.loggerLevel;
        typeDB      = builder.typeDB;
        dbDriver    = builder.dbDriver;
        dbHost      = builder.dbHost;
        dbBase      = builder.dbBase;
        dbPort      = builder.dbPort;
        dbUserName  = builder.dbUserName;
        dbPassword  = builder.dbPassword;

        if (loggerLevel != null) { setLoggerLevel(loggerLevel); }

        if (dbDriver == null || dbDriver.isEmpty()){
            if (typeDB != null) {
                dbDriver = typeDB.getDriver();
            } else {
                LOG.error("SQL Driver не задан");
            }
        } else if (typeDB == null){
            for (TypeDB type: TypeDB.values()){
                if (type.getDriver().equalsIgnoreCase(dbDriver)){
                    typeDB = type;
                }
            }
        }

        if (dbURL == null || dbURL.isEmpty()){
            StringBuilder dbURL = new StringBuilder();

            switch (typeDB){
                case HSQLDB:
                    dbURL.append("jdbc:hsqldb:file:")
                            .append(dbHost)
                            .append("/")
                            .append(dbBase);
                    break;

                case ORACLE:
                    dbURL.append("jdbc:oracle:thin:@//")
                            .append(dbHost)
                            .append(":")
                            .append(dbPort)
                            .append("/")
                            .append(dbBase);
                    break;

                case SQLSERVER:
                    dbURL.append("jdbc:sqlserver://")
                            .append(dbHost)
                            .append(";")
                            .append("databaseName=")
                            .append(dbBase);
                    break;

                default:
                    LOG.error("SQL Driver неизвестен: {}", dbDriver);
            }
            this.dbURL = dbURL.toString();
        }
    }


    public void setLoggerLevel(Level loggerLevel) {
        Configurator.setLevel(LOG.getName(), loggerLevel);
    }

    public boolean isConnection() {
        return connection == null ? false : true;
    }

    public Connection connection() {
        return connection;
    }

    private boolean loadDriver() {
        LOG.trace("SQL Driver: {}", dbDriver);
        try {
            DriverManager.registerDriver((Driver) Class.forName(dbDriver).newInstance());
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | SQLException e) {
            LOG.error("Ошибка при работе с драйвером: {}", dbDriver, e);
            return false;
        }
        return true;
    }

    public boolean connect(){
        LOG.debug("SQL connect: {}", dbURL);

        if (isConnection()){
            LOG.warn("SQL активно предыдущее подключение");
            return true;
        }

        if (this.typeDB == null || !loadDriver()) {return false;}

        try {
            connection = DriverManager.getConnection(dbURL, dbUserName, dbPassword);
//            statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            statement = connection.createStatement();

        } catch (SQLException e) {
            LOG.error("Ошибка при подключении к базе данных {}", dbURL, e);
            return false;
        }
        return true;
    }

    public boolean disconnect() {
        LOG.debug("SQL disconnect");
        if (isConnection()) {
            try {
                if (typeDB == TypeDB.HSQLDB) {
                    execute("SHUTDOWN");
                }
                if (statement != null) {
                    statement.close();
                }
                if (connection != null) {
                    connection.commit();
                    connection.close();
                    connection = null;
                }
            } catch (SQLException e) {
                LOG.error("SQL disconnect", e);
                return false;
            }
        }
        return true;
    }

    public boolean execute(String sql) {
        boolean res = false;
        if (isConnection()) {
            try {
                LOG.trace("SQL request:\n{}", sql);
                statement.execute(sql);
                res = true;
            } catch (SQLException e) {
                LOG.error("{}", sql, e);
            }
        } else {
            LOG.error("Отсутствует подключение к базе данных");
        }
        return res;
    }

    public ResultSet executeQuery(String sql) {
        ResultSet resultSet = null;
        if (isConnection()) {
            try {
                LOG.trace("SQL request:\n{}", sql);
                resultSet = statement.executeQuery(sql);
            } catch (SQLException e) {
                LOG.error("{}", sql, e);
            }
        } else {
            LOG.error("Отсутствует подключение к базе данных");
        }
        return resultSet;
    }

}
