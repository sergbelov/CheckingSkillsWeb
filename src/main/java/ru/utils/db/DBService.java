package ru.utils.db;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.*;

/**
 * Created by Сергей on 01.05.2018.
 */
public class DBService {

    private static final Logger LOG = LogManager.getLogger();
    private Connection connection = null;

    private boolean loadDriver(String driverName) {
        try {
            DriverManager.registerDriver((Driver) Class.forName(driverName).newInstance());
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | SQLException e) {
            LOG.error("Ошибка при работе с драйвером: {} ", driverName, e);
            return false;
        }
        LOG.debug("SQL Driver: {}", driverName);
        return true;
    }

    public boolean isConnection() {
        return connection == null ? false : true;
    }

    public Connection connection(){
        return connection;
    }

    public boolean getConnectionHSQL(
            String hSqlPath,
            String hSqlDb,
            String login,
            String password,
            Level loggerLevel) {

        Configurator.setLevel(LOG.getName(), loggerLevel);
        String driverNameHSQL = "org.hsqldb.jdbcDriver";
        boolean res = false;

        if (!isConnection()) {
            if (hSqlPath == null || hSqlPath.isEmpty()) {
                hSqlPath = "hSQL";
            }
            if (hSqlDb == null || hSqlDb.isEmpty()) {
                hSqlDb = "db";
            }
            if (login == null || login.isEmpty()) {
                login = "admin";
            }
            if (password == null || password.isEmpty()) {
                password = "admin";
            }

            if (loadDriver(driverNameHSQL)) {
                StringBuilder url = new StringBuilder();
                url.append("jdbc:hsqldb:file:")
                    .append(hSqlPath)
                    .append(hSqlDb);
                try {
                    connection = DriverManager.getConnection(url.toString(), login, password);
//                connection.setAutoCommit(false); // для обработки транзакций
//                connection.commit();
//                connection.rollback();
                    LOG.debug("Подключение к базе данных: DriverManager.getConnectionHSQL({}, {}, {})",
                            url.toString(),
                            login,
                            password);

                    if (createTableUsers()) { res = true; }

                } catch (SQLException e) {
                    LOG.error("Ошибка при подключении к базе данных: DriverManager.getConnectionHSQL({}, {}, {})",
                            url.toString(),
                            login,
                            password,
                            e);
                }
            }
        }
        return res;
    }

    public boolean closeConnectionHSQL() {
        boolean res = false;
//            try {
//                connection.rollback();
//                connection.setAutoCommit(true);
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
        if ((res = execUpdate("SHUTDOWN"))){
            connection = null;
        }
        return res;
    }


    private boolean createTableUsers() {
        return execUpdate("CREATE TABLE IF NOT EXISTS users (" +
                "ID IDENTITY, " +
                "USERNAME VARCHAR(25), " +
                "FULLUSERNAME VARCHAR(100), " +
                "PASSWORD VARCHAR(50))");
    }

    private boolean execUpdate(String sql) {
        boolean res = false;
        if (isConnection()) {
            try {
                Statement statement = connection.createStatement();
                statement.execute(sql);
                statement.close();
                LOG.debug("{}", sql);
                res = true;

            } catch (SQLException e) {
                LOG.error("{}", sql, e);
            }
        } else {
            LOG.error("Отсутствует подключение к базе данных");
        }
        return res;
    }

    // StackTrace to String
    private String stackTraceToString(Exception e) {
        StringWriter error = new StringWriter();
        e.printStackTrace(new PrintWriter(error));
        return error.toString();
    }

}
