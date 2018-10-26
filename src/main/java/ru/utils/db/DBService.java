package ru.utils.db;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.sql.*;

/**
 * Created by Сергей on 01.05.2018.
 */
public class DBService {

    private static final Logger LOG = LogManager.getLogger();
    private Connection connection = null;
    private Statement statement = null;
    private String dbDriver = null;
    private TypeDB typeDB = null;

    public enum TypeDB {
        HSQLDB    { public String getDriver() {return "org.hsqldb.jdbcDriver";} },
        ORACLE    { public String getDriver() {return "oracle.jdbc.driver.OracleDriver";} },
        SQLSERVER { public String getDriver() {return "com.microsoft.sqlserver.jdbc.SQLServerDriver";} };

        public abstract String getDriver();
    }


    public void setLoggerLevel(Level loggerLevel) {
        Configurator.setLevel(LOG.getName(), loggerLevel);
    }

    public boolean isConnection() {
        return connection == null ? false : true;
    }

    public TypeDB getTypeDB() {
        return typeDB;
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

    public Connection connection() {
        return connection;
    }

    public boolean connect(
            TypeDB typeDB,
            String dbHost,
            String dbBase,
            String dbUserName,
            String dbPassword) {

        return connect(
                typeDB,
                dbHost,
                1521,
                dbBase,
                dbUserName,
                dbPassword);
    }

    public boolean connect(
            TypeDB typeDB,
            String dbHost,
            int dbPort,
            String dbBase,
            String dbUserName,
            String dbPassword) {

        this.typeDB = typeDB;
        this.dbDriver = typeDB.getDriver();

        return connect(
                this.dbDriver,
                dbHost,
                dbPort,
                dbBase,
                dbUserName,
                dbPassword);
    }

    public boolean connect(
            String dbDriver,
            String dbHost,
            String dbBase,
            String dbUserName,
            String dbPassword) {

        return connect(
                dbDriver,
                dbHost,
                1521,
                dbBase,
                dbUserName,
                dbPassword);
    }

    public boolean connect(
            String dbDriver,
            String dbHost,
            int dbPort,
            String dbBase,
            String dbUserName,
            String dbPassword) {

        StringBuilder dbURL = new StringBuilder();

        if (dbDriver.equalsIgnoreCase("org.hsqldb.jdbcDriver")) {
            dbURL.append("jdbc:hsqldb:file:")
                    .append(dbHost)
                    .append("/")
                    .append(dbBase);

        } else if (dbDriver.equalsIgnoreCase("oracle.jdbc.driver.OracleDriver")) {
            dbURL.append("jdbc:oracle:thin:@//")
                    .append(dbHost)
                    .append(":")
                    .append(dbPort)
                    .append("/")
                    .append(dbBase);

        } else if (dbDriver.equalsIgnoreCase("com.microsoft.sqlserver.jdbc.SQLServerDriver")) {
            dbURL.append("jdbc:sqlserver://")
                    .append(dbHost)
                    .append(";")
                    .append("databaseName=")
                    .append(dbBase);
        } else {
            LOG.error("Неизвестный драйвер: {}", dbDriver);
            return false;
        }

        return connect(
                dbDriver,
                dbURL.toString(),
                dbUserName,
                dbPassword);
    }

    public boolean connect(
            String dbDriver,
            String dbURL,
            String dbUserName,
            String dbPassword) {

        LOG.debug("SQL connect: {}", dbURL);

        this.dbDriver = dbDriver;

        for (TypeDB type: TypeDB.values()){
            if (type.getDriver().equalsIgnoreCase(dbDriver)){
                this.typeDB = type;
            }
        }

        if (!loadDriver()) {return false;}

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
                if (getTypeDB().equals(TypeDB.HSQLDB)) {
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

    public void printConnectInfo() throws SQLException {
        LOG.info("SQL connect info" +
                        "\n\tDB name:    {}" +
                        "\n\tDB version: {}" +
                        "\n\tDriver:     {}" +
                        "\n\tAutocommit: {}",
                connection.getMetaData().getDatabaseProductName(),
                connection.getMetaData().getDatabaseProductVersion(),
                connection.getMetaData().getDriverName(),
                connection.getAutoCommit());
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

class DriverDB {
    private String type;
    private String driver;

    public DriverDB(String type, String driver) {
        this.type = type;
        this.driver = driver;
    }

    public String getType() {
        return type;
    }

    public String getDriver() {
        return driver;
    }
}