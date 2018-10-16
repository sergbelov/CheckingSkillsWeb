package ru.web;

/**
 * Created by Belov Sergey on 20.11.2017.
 */
public class WebUser {
    private String userName;
    private String fullUserName;
    private String password;

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public String getFullUserName() {
        return fullUserName;
    }

    public void setFullUserName(String fullUserName) {
        this.fullUserName = fullUserName;
    }

    public boolean isDefinedUser(){ return userName != null && !userName.isEmpty(); }

    public void clear(){
        this.userName = "";
        this.fullUserName = "";
        this.password = "";
    }
}
