package com.andreavendrame.ldb4docker;

public class Login {

    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public static Login getLoginDefaultInstance() {

        Login login = new Login();
        login.setUsername("Username di test");
        login.setPassword("Password di test");

        return login;
    }

    @Override
    public String toString() {
        return "Username: " + this.username + ", Password: " + this.password;
    }
}
