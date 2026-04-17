package com.contactmanagement.contactservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.users")
public class SecurityUsersProperties {

    private UserCredentials read = new UserCredentials();
    private UserCredentials write = new UserCredentials();
    private UserCredentials admin = new UserCredentials();

    public UserCredentials getRead() {
        return read;
    }

    public void setRead(UserCredentials read) {
        this.read = read;
    }

    public UserCredentials getWrite() {
        return write;
    }

    public void setWrite(UserCredentials write) {
        this.write = write;
    }

    public UserCredentials getAdmin() {
        return admin;
    }

    public void setAdmin(UserCredentials admin) {
        this.admin = admin;
    }

    public static class UserCredentials {
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
    }
}
