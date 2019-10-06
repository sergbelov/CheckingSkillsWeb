package ru.utils.db;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.sql.*;

public class DBService {

    private static final Logger LOG = LogManager.getLogger();

    public enum DBType {
        HSQLDB {
            public String getDriver() {return "org.hsqldb.jdbcDriver";}

            public String getURL(
                    String dbHost,
                    String dbBase,
                    int dbPort){
                return "jdbc:hsqldb:file:" +
                        dbHost +
                        "/" +
                        dbBase;}
        },

        ORACLE {
            public String getDriver() {return "oracle.jdbc.driver.OracleDriver";}

            public String getURL(
                    String dbHost,
                    String dbBase,
                    int dbPort){
                return "jdbc:oracle:thin:@//" +
                        dbHost +
                        ":" +
                        dbPort +
                        "/" +
                        dbBase;}
        },

        SQLSERVER {
            public String getDriver() {return "com.microsoft.sqlserver.jdbc.SQLServerDriver";}

            public String getURL(
                    String dbHost,
                    String dbBase,
                    int dbPort){
                return "jdbc:sqlserver://"+
                        dbHost +
                        ";databaseName=" +
                        dbBase;}
        };

        public abstract String getDriver();
        public abstract String getURL(String dbHost, String dbBase, int dbPort);
    }

    private Connection connection   = null;
    private Statement statement     = null;

    private Level loggerLevel;
    private DBType dbType;
    private String dbDriver;
    private String dbHost;
    private String dbBase;
    private int    dbPort;
    private String dbURL;
    private String dbUserName;
    private String dbPassword;


    public static class Builder{
        private Level  loggerLevel = null;
        private DBType dbType      = null;
        private String dbDriver    = null;
        private String dbHost;
        private String dbBase;
        private int    dbPort      = 1521;
        private String dbURL       = null;
        private String dbUserName;
        private String dbPassword;

        public Builder loggerLevel(Level val){
            loggerLevel = val;
            return this;
        }
        public Builder dbType(DBType val){
            dbType = val;
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
        dbType      = builder.dbType;
        dbDriver    = builder.dbDriver;
        dbHost      = builder.dbHost;
        dbBase      = builder.dbBase;
        dbPort      = builder.dbPort;
        dbURL       = builder.dbURL;
        dbUserName  = builder.dbUserName;
        dbPassword  = builder.dbPassword;

        if (loggerLevel != null) { setLoggerLevel(loggerLevel); }

        if (dbDriver == null || dbDriver.isEmpty()){
            if (dbType != null) {
                dbDriver = dbType.getDriver();
            } else {
                LOG.error("SQL Driver не задан");
            }
        } else if (dbType == null){
            for (DBType type: DBType.values()){
                if (type.getDriver().equalsIgnoreCase(dbDriver)){
                    dbType = type;
                    break;
                }
            }
        }

        if (dbURL == null || dbURL.isEmpty()){
            dbURL = dbType.getURL(dbHost, dbBase, dbPort);
        }
    }


    public DBType getDbType(){
        return dbType;
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
            LOG.error("SQL Ошибка при работе с драйвером: {}", dbDriver, e);
            return false;
        }
        return true;
    }

    public boolean connect(){
        LOG.debug("SQL Connect: {}", dbURL);

        if (isConnection()){
            LOG.trace("SQL Подключение активно, используем: {}", getConnectInfo());
            return true;
        }

        if (this.dbType == null || !loadDriver()) {return false;}

        try {
            connection = DriverManager.getConnection(dbURL, dbUserName, dbPassword);
/*
ResultSet.TYPE-FORWARD_ONLY
Указатель двигается только вперёд по множеству полученных результатов.

ResultSet.TYPE_SCROLL_INTENSIVE
Указатель может двигаться вперёд и назад и не чуствителен к изменениям в БД, которые сделаны другими пользователями после того, как ResultSet был создан.

ResultSet.TYPE_SCROLL_SENSITIVE
Указатель может двигаться вперёд и назад и чувствителен к изменениям в БД, которые сделаны другими пользователями после того, как ResultSet был создан.

-------------------------------------------------------------------------------------

ResultSet.CONCUR_READ_ONLY
Создаёт экземпляр ResultSet только для чтения. Устанавливается по умолчанию.

ResultSet.CONCUR_UPDATABLE
Создаёт экземпляр ResultSet, который может изменять данные.
*/

//            statement = connection.createStatement();
            statement = connection.createStatement(
                    ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);

            LOG.trace("SQL Connected: {}", getConnectInfo());

        } catch (SQLException e) {
            LOG.error("SQL Ошибка при подключении к базе данных {}", dbURL, e);
            return false;
        }
        return true;
    }

    public void disconnect() {
        LOG.debug("SQL Disconnect");
        if (isConnection()) {
            try {
                if (dbType == DBType.HSQLDB) {
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
                LOG.error("SQL Disconnect", e);
            }
        }
    }

    public String getConnectInfo(){
        String connectInfo = "";
        if (isConnection()) {
            try {
                connectInfo = String.format(
                        "\n\tDB name:    %s" +
                        "\n\tDB version: %s" +
                        "\n\tDriver:     %s" +
                        "\n\tAutocommit: %s" +
                        "\n\tDB Host:    %s" +
                        "\n\tDB Base:    %s",
                        connection.getMetaData().getDatabaseProductName(),
                        connection.getMetaData().getDatabaseProductVersion(),
                        connection.getMetaData().getDriverName(),
                        connection.getAutoCommit(),
                        dbHost,
                        dbBase);
            } catch (SQLException e) {
                LOG.error("SQL ConnectInfo", e);
            }
        }
        return connectInfo;
    }

    public boolean execute(String sql) {
        boolean res = false;
        if (isConnection()) {
            try {
                LOG.trace("SQL Request:\n{}", sql);
                statement.execute(sql);
                res = true;
            } catch (SQLException e) {
                LOG.error("{}", sql, e);
            }
        } else {
            LOG.error("SQL Отсутствует подключение к базе данных");
        }
        return res;
    }

    public ResultSet executeQuery(String sql) {
        ResultSet resultSet = null;
        if (isConnection()) {
            try {
                LOG.trace("SQL Request:\n{}", sql);
                resultSet = statement.executeQuery(sql);
            } catch (SQLException e) {
                LOG.error("{}", sql, e);
            }
        } else {
            LOG.error("SQL Отсутствует подключение к базе данных");
        }
        return resultSet;
    }

    /** Количество записей в ResultSet
     *
     * @param resultSet
     * @return
     */
    public int getCountResultSet(ResultSet resultSet){
        int r = 0;
        try {
            int getRow = resultSet.getRow();
//            LOG.trace("SQL Запоминаем текущую запись {}", getRow);
//            LOG.trace("SQL Перемещаемся на последнюю запись");
            resultSet.last();
//            LOG.trace("SQL Возвращаемся на запомненную запись");
            r = resultSet.getRow();
            resultSet.absolute(getRow);
        } catch (SQLException e) {
            LOG.error(e);
        }
        return r;
    }

}
