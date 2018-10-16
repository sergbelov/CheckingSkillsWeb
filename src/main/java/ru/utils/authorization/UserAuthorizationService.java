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

/**
 * Created by Сергей on 01.05.2018.
 */
public class UserAuthorizationService implements UserAuthorizationServiceI {

    private final Logger LOG = LogManager.getLogger();
    private DBService dbService = new DBService();
    private StringBuilder errorMessage = new StringBuilder();
    private String fullUserName;


    public String getErrorMessage() {
        return errorMessage.toString();
    }

    public String getFullUserName() { return fullUserName;}

    public boolean getConnectionHSQL(
            String hSqlPath,
            String hSqlDb,
            String login,
            String password,
            Level  loggerLevel) {

        Configurator.setLevel(LOG.getName(), loggerLevel);

        return dbService.getConnectionHSQL(
                hSqlPath,
                hSqlDb,
                login,
                password,
                loggerLevel);
    }

    public void closeConnectionHSQL() { dbService.closeConnectionHSQL(); }

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
                                res = true;
                            } else {
                                LOG.warn("Авторизация пользователя: {} ({}) - неверный пароль", userName, fullUserName);
                                errorMessage.append("Неверный пароль для пользователя ")
                                        .append(userName)
                                        .append(" (")
                                        .append(fullUserName)
                                        .append(")");
                            }
                        } else {
                            LOG.warn("Авторизация пользователя: {} - пользователь не зарегистрирован", userName);
                            errorMessage.append("Пользователь ")
                                    .append(userName)
                                    .append(" не зарегистрирован");
                        }
                        resultSet.close();
                        preparedStatement.close();

                    } catch (SQLException e) {
                        LOG.error(e);
                        errorMessage.append(e);
                    }
                } else {
                    errorMessage.append("Необходимо указать пароль");
                }
            } else {
                errorMessage.append("Необходимо указать пользователя");
            }
        } else {
            errorMessage.append("Отсутствует подключение к базе данных");
        }
        return res;
    }

    @Override
    public boolean userAdd(String userName, String fullUserName, String password, String password2) {
        boolean res = false;
        this.fullUserName = "";
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
                    errorMessage.append(e);
                }
            } else {
                errorMessage.append("Ошибка регистрации пользователя ").append(userName);
                if (password.isEmpty()) { errorMessage.append(" - пароль не может быть пустым"); }
                else { errorMessage.append(" - пароль и подтверждение не совпадают"); }
            }
        } else {
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
