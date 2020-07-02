package ru.utils.db;

//import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/*
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
*/
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import java.beans.PropertyVetoException;
//import java.util.Properties;
import java.sql.*;

/**
 * Класс для работы с базой данных
 */
public class DBService {
//    private static final Logger LOG = LogManager.getLogger();
    private static final Logger LOG = LoggerFactory.getLogger(DBService.class);

//    private ComboPooledDataSource pooledDataSource = null; //new ComboPooledDataSource();
    private HikariDataSource pooledDataSource = null;
    private Connection connection = null;
    private Statement statement = null;

    private Level loggerLevel;
    private DBType dbType;
    private String dbDriver;
    private String dbHost;
    private String dbBase;
    private int dbPort;
    private String dbUrl;
    private String dbUserName;
    private String dbPassword;

    public static class Builder {
        private Level loggerLevel = null;
        private DBType dbType = null;
        private String dbDriver = null;
        private String dbHost;
        private String dbBase;
        private int dbPort = 0; //1521;
        private String dbUrl = null;
        private String dbUserName;
        private String dbPassword;

        public Builder loggerLevel(Level val) {
            loggerLevel = val;
            return this;
        }

        public Builder dbType(DBType val) {
            dbType = val;
            return this;
        }

        public Builder dbDriver(String val) {
            dbDriver = val;
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

        public Builder dbUrl(String val) {
            dbUrl = val;
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

        public DBService build() {
            return new DBService(this);
        }
    }

    /**
     * Инициализация с использованием Builder()
     *
     * @param builder
     */
    private DBService(Builder builder) {
        loggerLevel = builder.loggerLevel;
        dbType = builder.dbType;
        dbDriver = builder.dbDriver;
        dbHost = builder.dbHost;
        dbBase = builder.dbBase;
        dbPort = builder.dbPort;
        dbUrl = builder.dbUrl;
        dbUserName = builder.dbUserName;
        dbPassword = builder.dbPassword;

        if (loggerLevel != null) {
            setLoggerLevel(loggerLevel);
        }

        if (dbDriver == null || dbDriver.isEmpty()) {
            if (dbType == null) {
                setDbTypeFromUrl(dbUrl);
            }
        } else if (dbType == null) {
            setDbTypeFromDriver(dbDriver);
        }

        if (dbType == null) {
            LOG.error("SQL Driver не задан");
        } else {
            if (dbUrl == null || dbUrl.isEmpty()) {
                this.dbUrl = dbType.getUrl(dbHost, dbBase, dbPort);
            }
        }
    }


    /**
     * Инициализация без параметров
     */
    public DBService() {
    }

    /**
     * Инициализация с параметрами
     *
     * @param dbUrl
     * @param dbUserName
     * @param dbPassword
     */
    public DBService(
            String dbUrl,
            String dbUserName,
            String dbPassword) {

        setParamsForConnect(
                null,
                dbUrl,
                dbUserName,
                dbPassword);
    }

    /**
     * Инициализация с параметрами
     *
     * @param loggerLevel
     * @param dbUrl
     * @param dbUserName
     * @param dbPassword
     */
    public DBService(
            Level loggerLevel,
            String dbUrl,
            String dbUserName,
            String dbPassword) {

        setParamsForConnect(
                loggerLevel,
                dbUrl,
                dbUserName,
                dbPassword);
    }

    /**
     * Инициализация с параметрами
     *
     * @param dbType
     * @param dbHost
     * @param dbBase
     * @param dbPort
     * @param dbUserName
     * @param dbPassword
     */
    public DBService(
            DBType dbType,
            String dbHost,
            String dbBase,
            int dbPort,
            String dbUserName,
            String dbPassword) {

        setParamsForConnect(
                null,
                dbType,
                dbHost,
                dbBase,
                dbPort,
                dbUserName,
                dbPassword);
    }

    /**
     * Инициализация с параметрами
     *
     * @param loggerLevel
     * @param dbType
     * @param dbHost
     * @param dbBase
     * @param dbPort
     * @param dbUserName
     * @param dbPassword
     */
    public DBService(
            Level loggerLevel,
            DBType dbType,
            String dbHost,
            String dbBase,
            int dbPort,
            String dbUserName,
            String dbPassword) {

        setParamsForConnect(
                loggerLevel,
                dbType,
                dbHost,
                dbBase,
                dbPort,
                dbUserName,
                dbPassword);
    }

    /**
     * Инициализация с параметрами
     *
     * @param dbDriver
     * @param dbHost
     * @param dbBase
     * @param dbPort
     * @param dbUserName
     * @param dbPassword
     */
    public DBService(
            String dbDriver,
            String dbHost,
            String dbBase,
            int dbPort,
            String dbUserName,
            String dbPassword) {

        setParamsForConnect(
                null,
                dbDriver,
                dbHost,
                dbBase,
                dbPort,
                dbUserName,
                dbPassword);
    }

    /**
     * Инициализация с параметрами
     *
     * @param loggerLevel
     * @param dbDriver
     * @param dbHost
     * @param dbBase
     * @param dbPort
     * @param dbUserName
     * @param dbPassword
     */
    public DBService(
            Level loggerLevel,
            String dbDriver,
            String dbHost,
            String dbBase,
            int dbPort,
            String dbUserName,
            String dbPassword) {

        setParamsForConnect(
                loggerLevel,
                dbDriver,
                dbHost,
                dbBase,
                dbPort,
                dbUserName,
                dbPassword);
    }


    /**
     * Закрытие Connection из пула
     * @param connection
     */
/*
    public void closeConnectionFromPool(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
                if (pooledDataSource != null) {
//                    LOG.debug("Закрытие Connection из пула: idleConnections = {}; busyConnections = {}",
//                            pooledDataSource.getNumIdleConnections(),
//                            pooledDataSource.getNumBusyConnections());
                    LOG.debug("Закрытие Connection из пула...");
                }
            } catch (SQLException e) {
                LOG.error("Ошибка при закрытии Connection из пула\n", e);
            }
        }
    }
*/


    /**
     * Установка параметов для подключения к БД
     *
     * @param loggerLevel
     * @param dbUrl
     * @param dbUserName
     * @param dbPassword
     */
    private void setParamsForConnect(
            Level loggerLevel,
            String dbUrl,
            String dbUserName,
            String dbPassword) {

        if (loggerLevel != null) {
            this.loggerLevel = loggerLevel;
            setLoggerLevel(loggerLevel);
        }
        this.dbUrl = dbUrl;
        this.dbUserName = dbUserName;
        this.dbPassword = dbPassword;
        setDbTypeFromUrl(dbUrl);
    }

    /**
     * Установка параметов для подключения к БД
     *
     * @param loggerLevel
     * @param dbType
     * @param dbHost
     * @param dbBase
     * @param dbPort
     * @param dbUserName
     * @param dbPassword
     */
    private void setParamsForConnect(
            Level loggerLevel,
            DBType dbType,
            String dbHost,
            String dbBase,
            int dbPort,
            String dbUserName,
            String dbPassword) {

        if (loggerLevel != null) {
            this.loggerLevel = loggerLevel;
            setLoggerLevel(loggerLevel);
        }
        this.dbType = dbType;
        this.dbHost = dbHost;
        this.dbBase = dbBase;
        this.dbPort = dbPort;
        this.dbUrl = dbType.getUrl(dbHost, dbBase, dbPort);
        this.dbUserName = dbUserName;
        this.dbPassword = dbPassword;
    }

    /**
     * Установка параметров для подключения к БД
     *
     * @param loggerLevel
     * @param dbDriver
     * @param dbHost
     * @param dbBase
     * @param dbPort
     * @param dbUserName
     * @param dbPassword
     */
    private void setParamsForConnect(
            Level loggerLevel,
            String dbDriver,
            String dbHost,
            String dbBase,
            int dbPort,
            String dbUserName,
            String dbPassword) {

        if (loggerLevel != null) {
            this.loggerLevel = loggerLevel;
            setLoggerLevel(loggerLevel);
        }
        this.dbDriver = dbDriver;
        setDbTypeFromDriver(dbDriver);
        this.dbHost = dbHost;
        this.dbBase = dbBase;
        this.dbPort = dbPort;
        this.dbUrl = dbType.getUrl(dbHost, dbBase, dbPort);
        this.dbUserName = dbUserName;
        this.dbPassword = dbPassword;
    }


    /**
     * Определение типа БД по URL
     *
     * @param dbUrl
     */
    private void setDbTypeFromUrl(String dbUrl) {
        this.dbType = null;
        if (dbUrl != null && !dbUrl.isEmpty()) {
            if (dbUrl.toLowerCase().startsWith("jdbc:hsqldb:file:")) {
                this.dbType = DBType.HSQLDB;
            } else if (dbUrl.toLowerCase().startsWith("jdbc:oracle:thin:")) {
                this.dbType = DBType.ORACLE;
            } else if (dbUrl.toLowerCase().startsWith("jdbc:sqlserver:")) {
                this.dbType = DBType.SQLSERVER;
            } else {
                LOG.error("SQL драйвер не определен {}", dbUrl);
            }
        } else {
            LOG.error("SQL не заданы параметры для подключения");
        }
    }

    /**
     * Определение типа БД по драйверу
     *
     * @param dbDriver
     */
    private void setDbTypeFromDriver(String dbDriver) {
        this.dbType = null;
        for (DBType type : DBType.values()) {
            if (type.getDriver().equalsIgnoreCase(dbDriver)) {
                this.dbType = type;
                break;
            }
        }
        if (dbType == null) {
            LOG.error("SQL драйвер не определен {}", dbUrl);
        }
    }

    /**
     * Текущий тип соедиенния
     *
     * @return
     */
    public DBType getDbType() {
        return dbType;
    }

    /**
     * Текущий dbUrl
     *
     * @return
     */
    public String getDbUrl() {
        return dbUrl;
    }

    /**
     * Установка уровня логирования
     *
     * @param loggerLevel
     */
    public void setLoggerLevel(Level loggerLevel) {
//        Configurator.setRootLevel(loggerLevel);
        Configurator.setLevel(LOG.getName(), loggerLevel);
    }

    /**
     * Проверка активности соединения
     *
     * @return true or false
     */
    public boolean isConnection() {
        return (connection == null && pooledDataSource == null) ? false : true;
    }

    /**
     * Текущее соедиенние
     *
     * @return Connection
     */
    public Connection connection() {
        return connection;
    }

    /**
     * Получить Connection, если пул активен берется из него
     *
     * @return
     */
    public Connection getConnection() {
        if (pooledDataSource != null) {
            Connection connection = null;
            try {
                connection = pooledDataSource.getConnection();
/*
                LOG.debug("Создание Connection и пула: idleConnections = {}; busyConnections = {}",
                        pooledDataSource.getNumIdleConnections(),
                        pooledDataSource.getNumBusyConnections());
*/
                LOG.debug("Создание Connection и пула...");
            } catch (Exception e) {
                LOG.error("Ошибка при создании Connection из пула\n", e);
            }
            return connection;
        } else {
            return this.connection;
        }
    }

    public Statement createStatement(Connection connection){
        return createStatement(
                connection,
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);
    }

    /**
     * Создание Statement
     * ResultSet.TYPE_FORWARD_ONLY
     * Указатель двигается только вперёд по множеству полученных результатов.
     * ResultSet.TYPE_SCROLL_INTENSIVE
     * Указатель может двигаться вперёд и назад и не чуствителен к изменениям в БД, которые сделаны другими пользователями после того, как ResultSet был создан.
     * ResultSet.TYPE_SCROLL_SENSITIVE
     * Указатель может двигаться вперёд и назад и чувствителен к изменениям в БД, которые сделаны другими пользователями после того, как ResultSet был создан.
     * -------------------------------------------------------------------------------------
     * ResultSet.CONCUR_READ_ONLY
     * Создаёт экземпляр ResultSet только для чтения. Устанавливается по умолчанию.
     * ResultSet.CONCUR_UPDATABLE
     * Создаёт экземпляр ResultSet, который может изменять данные.
     *
     * @return
     */
    public Statement createStatement(
            Connection connection,
            int resultSetType,
            int resultSetConcurrency
    ) {
        Statement statement = null;
        if (connection != null) {
            try {
                statement = connection.createStatement(resultSetType, resultSetConcurrency);
            } catch (SQLException e) {
                LOG.error("Ошибка при создании Statement\n", e);
            }
        } else {
            LOG.error("SQL Отсутствует подключение к базе данных");
        }
        return statement;
    }

    /**
     * Ддрайвер для работы с БД
     *
     * @return true or false
     */
    private boolean loadDriver() {
        if (dbDriver == null || dbDriver.isEmpty()) {
            if (dbType == null) {
                LOG.error("SQL Driver не задан");
                return false;
            } else {
                dbDriver = dbType.getDriver();
            }
        }

        LOG.trace("SQL Driver: {}", dbDriver);
        try {
            DriverManager.registerDriver((Driver) Class.forName(dbDriver).newInstance());
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | SQLException e) {
            LOG.error("SQL Ошибка при работе с драйвером: {}\n", dbDriver, e);
            return false;
        }
        return true;
    }


    /**
     * Инициализация пула подключений к БД
     */
    public boolean connectPooled() {
        return connectPooled(
                50,
                100,
                150,
                0
                );
    }

    /**
     * Инициализация пула подключений к БД
     *
     * @param minPoolSize
     * @param maxPoolSize
     * @param maxStatements
//     * @param maxStatementsPerConnection
     * @param maxIdleTime
//     * @param acquireIncrement
     */
    public boolean connectPooled(
            int minPoolSize,
            int maxPoolSize,
            int maxStatements,
//            int maxStatementsPerConnection,
            int maxIdleTime
//            int acquireIncrement
    ) {
        try {
            if (pooledDataSource == null) {
                LOG.info("Инициализация пула подключений к БД {}...", dbUrl);

                // HikariDataSource
                HikariConfig config = new HikariConfig();

                config.setJdbcUrl(dbUrl);
                config.setUsername(dbUserName);
                config.setPassword(dbPassword);

//                config.setConnectionTimeout(30000); // максимальное количество миллисекунд, в течение которых клиент будет ожидать подключения из пула
//                config.setInitializationFailTimeout(); // время ожидания сбоя при инициализации пула
                config.setIdleTimeout(maxIdleTime); //максимальное количество времени (в миллисекундах), в течение которого соединению разрешено бездействовать в пуле
                config.setMinimumIdle(minPoolSize);
                config.setMaximumPoolSize(maxPoolSize);
                config.setAutoCommit(false);
                config.addDataSourceProperty("cachePrepStmts", "true"); // Кэшировать или нет
                config.addDataSourceProperty("prepStmtCacheSize", String.valueOf(maxStatements)); // Количество Prepared Statements кэшируемых соединений
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048"); // Максимальная длина SQL Statement

                pooledDataSource = new HikariDataSource(config);

                LOG.debug("InitializationFailTimeout: {}, ConnectionTimeout: {}, IdleTimeout: {}",
                        pooledDataSource.getInitializationFailTimeout(),
                        pooledDataSource.getConnectionTimeout(),
                        pooledDataSource.getIdleTimeout());

/*
                // ComboPooledDataSource
                pooledDataSource = new ComboPooledDataSource();
                pooledDataSource.setDriverClass(dbType.getDriver());
                pooledDataSource.setJdbcUrl(dbUrl);
                pooledDataSource.setUser(dbUserName);
                pooledDataSource.setPassword(dbPassword);

                Properties properties = new Properties();
                properties.setProperty("user", dbUserName);
                properties.setProperty("password", dbPassword);
                properties.setProperty("useUnicode", "true");
                properties.setProperty("characterEncoding", "UTF8");
                pooledDataSource.setProperties(properties);

                // set options
                pooledDataSource.setMinPoolSize(3);
                pooledDataSource.setMaxPoolSize(maxPoolSize);
                pooledDataSource.setMaxStatements(maxStatements);
                pooledDataSource.setMaxStatementsPerConnection(150);
                pooledDataSource.setAcquireIncrement(3);
                pooledDataSource.setMaxIdleTime(maxIdleTime);
*/

                LOG.info("Пул подключений к БД {} инициализирован", dbUrl);
            } else {
                LOG.warn("Пул подключений к БД {} инициализирован ранее", dbUrl);
            }
            return true;
        } catch (Exception e) {
            LOG.error("Ошибка при инициализации пула подключений к БД {}\n", dbUrl, e);
            return false;
        }
    }

    /**
     * Подключение к БД с параметрами
     *
     * @param dbType
     * @param dbHost
     * @param dbBase
     * @param dbPort
     * @param dbUserName
     * @param dbPassword
     * @return true or false
     */
    public boolean connect(
            DBType dbType,
            String dbHost,
            String dbBase,
            int dbPort,
            String dbUserName,
            String dbPassword) {

        if (isConnection()) {
            close();
        }

        setParamsForConnect(
                null,
                dbType,
                dbHost,
                dbBase,
                dbPort,
                dbUserName,
                dbPassword);

        return connect();
    }


    /**
     * Подключение к БД с параметрами
     *
     * @param dbUrl
     * @param dbUserName
     * @param dbPassword
     * @return true or false
     */
    public boolean connect(
            String dbUrl,
            String dbUserName,
            String dbPassword) {

        if (isConnection()) {
            close();
        }

        setParamsForConnect(
                null,
                dbUrl,
                dbUserName,
                dbPassword);

        return connect();
    }


    /**
     * Подключение к БД
     * используем заданные ранее параметры
     *
     * @return true or false
     */
    public boolean connect() {
        if (isConnection()) {
            LOG.trace("SQL Подключение активно, используем: {}", getConnectInfo());
            return true;
        } else {
            LOG.info("SQL Connect: {}", dbUrl);
        }

        if (this.dbType == null || !loadDriver()) {
            return false;
        }

        try {
            connection = DriverManager.getConnection(dbUrl, dbUserName, dbPassword);
/*
ResultSet.TYPE_FORWARD_ONLY
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

/*
//            statement = connection.createStatement();
            statement = connection.createStatement(
                    ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
*/
            statement = createStatement(connection);
            LOG.debug("SQL Connected: {}", getConnectInfo());
        } catch (SQLException e) {
            LOG.error("SQL Ошибка при подключении к базе данных {}\n", dbUrl, e);
            return false;
        }
        return true;
    }

    /**
     * Отключение от БД
     * Для HSQLDB делаем SHUTDOWN
     */
    public void close() {
        close(true);
    }

    public void close(boolean shutdownForHSQLDB) {
        if (isConnection()) {
            LOG.info("SQL Disconnect: {}", dbUrl);
            try {
                if (statement != null) {
                    if (dbType == DBType.HSQLDB && shutdownForHSQLDB) {
                        execute("SHUTDOWN");
                    }
                    statement.close();
                }
                if (connection != null) {
                    connection.commit();
                    connection.close();
                    connection = null;
                }
            } catch (SQLException e) {
                LOG.error("SQL Disconnect\n", e);
            }
        }
        if (pooledDataSource != null){
            pooledDataSource.close();
            pooledDataSource = null;
        }
    }

    /**
     * Информация по текущему соединению с БД
     *
     * @return
     */
    public String getConnectInfo() {
        String connectInfo = "";
        if (isConnection()) {
            try {
                connectInfo = String.format(
                        "\n\tDB Url:     %s" +
                                "\n\tDB Host:    %s" +
                                "\n\tDB Base:    %s" +
                                "\n\tDB Port:    %s" +
                                "\n\tDB Driver:  %s" +
                                "\n\tDB Name:    %s" +
                                "\n\tDB Version: %s" +
                                "\n\tAutocommit: %s",

                        dbUrl,
                        dbHost != null ? dbHost : "",
                        dbBase != null ? dbBase : "",
                        dbPort != 0 ? dbPort : "",
                        connection.getMetaData().getDriverName(),
                        connection.getMetaData().getDatabaseProductName(),
                        connection.getMetaData().getDatabaseProductVersion(),
                        connection.getAutoCommit());

            } catch (SQLException e) {
                LOG.error("SQL ConnectInfo\n", e);
            }
        } else {
            connectInfo = "Соединение не установлено";
        }
        LOG.trace(connectInfo);
        return connectInfo;
    }

    public boolean execute(String sql) {
        return execute(statement, sql);
    }

    /**
     * Executes the given SQL statement, which may return multiple results.
     * In some (uncommon) situations, a single SQL statement may return
     * multiple result sets and/or update counts.  Normally you can ignore
     * this unless you are (1) executing a stored procedure that you know may
     * return multiple results or (2) you are dynamically executing an
     * unknown SQL string.
     * <p>
     * The <code>execute</code> method executes an SQL statement and indicates the
     * form of the first result.  You must then use the methods
     * <code>getResultSet</code> or <code>getUpdateCount</code>
     * to retrieve the result, and <code>getMoreResults</code> to
     * move to any subsequent result(s).
     * <p>
     * <strong>Note:</strong>This method cannot be called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>.
     *
     * @param statement
     * @param sql       any SQL statement
     * @return <code>true</code> if the first result is a <code>ResultSet</code>
     * object; <code>false</code> if it is an update count or there are
     * no results
     */
    public boolean execute(Statement statement, String sql) {
        boolean res = false;
        try {
            if (statement != null && !statement.isClosed()) {
                LOG.trace("SQL Request:\n{}", sql);
                statement.execute(sql);
                ;
                res = true;
            } else {
                LOG.error("SQL Отсутствует подключение к базе данных");
            }
        } catch (SQLException e) {
            LOG.error("Ошибка при выполнении запроса\n{}\n", sql, e);
        }
        return res;
    }

    public int executeUpdate(String sql) {
        return executeUpdate(statement, sql);
    }

    /**
     * Executes the given SQL statement, which may be an <code>INSERT</code>,
     * <code>UPDATE</code>, or <code>DELETE</code> statement or an
     * SQL statement that returns nothing, such as an SQL DDL statement.
     * <p>
     * <strong>Note:</strong>This method cannot be called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>.
     *
     * @param statement
     * @param sql       an SQL Data Manipulation Language (DML) statement, such as <code>INSERT</code>, <code>UPDATE</code> or
     *                  <code>DELETE</code>; or an SQL statement that returns nothing,
     *                  such as a DDL statement.
     * @return either (1) the row count for SQL Data Manipulation Language (DML) statements
     * or (2) 0 for SQL statements that return nothing
     */
    public int executeUpdate(Statement statement, String sql) {
        int res = 0;
        try {
            if (statement != null && !statement.isClosed()) {
                LOG.trace("SQL Request:\n{}", sql);
                res = statement.executeUpdate(sql);
                ;
            } else {
                LOG.error("SQL Отсутствует подключение к базе данных");
            }
        } catch (SQLException e) {
            LOG.error("Ошибка при выполнении запроса\n{}\n", sql, e);
        }
        return res;
    }


    public ResultSet executeQuery(String sql) {
        return executeQuery(statement, sql);
    }

    /**
     * Executes the given SQL statement, which returns a single
     * <code>ResultSet</code> object.
     * <p>
     * <strong>Note:</strong>This method cannot be called on a
     * <code>PreparedStatement</code> or <code>CallableStatement</code>.
     *
     * @param statement
     * @param sql       an SQL statement to be sent to the database, typically a
     *                  static SQL <code>SELECT</code> statement
     * @return a <code>ResultSet</code> object that contains the data produced
     * by the given query; never <code>null</code>
     */
    public ResultSet executeQuery(Statement statement, String sql) {
        ResultSet resultSet = null;
        try {
            if (statement != null && !statement.isClosed()) {
                LOG.trace("SQL Request:\n{}", sql);
                resultSet = statement.executeQuery(sql);
            } else {
                LOG.error("SQL Отсутствует подключение к базе данных");
            }
        } catch (SQLException e) {
            LOG.error("Ошибка при выполнении запроса\n{}\n", sql, e);
        }
        return resultSet;
    }

    /**
     * Количество записей в ResultSet
     *
     * @param resultSet
     * @return
     */
    public int getCountResultSet(ResultSet resultSet) {
        int r = 0;
        try {
            int getRow = resultSet.getRow();
            LOG.trace("SQL Запоминаем текущую запись {}", getRow);
            resultSet.last();
            LOG.trace("SQL Перемещаемся на последнюю запись");
            r = resultSet.getRow();
            LOG.trace("SQL Получаем количество записей {}", r);
            resultSet.absolute(getRow);
//            resultSet.beforeFirst();
            LOG.trace("SQL Возвращаемся на запомненную запись");
        } catch (SQLException e) {
            LOG.error("", e);
        }
        return r;
    }

}
