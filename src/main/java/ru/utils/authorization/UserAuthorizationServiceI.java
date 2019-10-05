package ru.utils.authorization;

/**
 * Created by Сергей on 01.05.2018.
 */
public interface UserAuthorizationServiceI {
    boolean isUserCorrect(String userName, String fullUserName, String password, String password2);
    boolean isUserCorrect(String userName, String password);
    boolean isSessionCorrect();
    boolean isSessionCorrect(String session);
    boolean userAdd(String userName, String fullUserName, String password, String password2);
    boolean userUpdate(String userName, String password);
}
