package ru.utils.db;

public enum DBType {
//        com.mysql.jdbc.Driver

    HSQLDB {
        public String getDriver() {
            return "org.hsqldb.jdbcDriver";
        }

        public String getUrl(
                String dbHost,
                String dbBase,
                int dbPort) {

            return "jdbc:hsqldb:file:" +
                    dbHost +
                    "/" +
                    dbBase +
                    ";hsqldb.lock_file=false";
        }
    },

    ORACLE {
        public String getDriver() {
            return "oracle.jdbc.driver.OracleDriver";
        }

        public String getUrl(
                String dbHost,
                String dbBase,
                int dbPort) {

            return "jdbc:oracle:thin:@//" +
                    dbHost +
                    ":" +
                    dbPort +
                    "/" +
                    dbBase;
        }
    },

    SQLSERVER {
        public String getDriver() {
            return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        }

        public String getUrl(
                String dbHost,
                String dbBase,
                int dbPort) {

            return "jdbc:sqlserver://" +
                    dbHost +
                    ";databaseName=" +
                    dbBase;
        }
    };

    public abstract String getDriver();

    public abstract String getUrl(String dbHost, String dbBase, int dbPort);
}
