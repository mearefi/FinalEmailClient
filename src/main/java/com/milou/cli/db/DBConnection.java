package com.milou.cli.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import java.io.InputStream;

public class DBConnection {
    private static final String CONFIG_FILE = "/config.properties";

    public static Connection getConnection() throws Exception {

        Class.forName("com.mysql.cj.jdbc.Driver");

        Properties props = new Properties();
        try (InputStream in = DBConnection.class.getResourceAsStream(CONFIG_FILE)) {
            if (in == null) throw new RuntimeException("Cannot find " + CONFIG_FILE);
            props.load(in);
        }

        String url = props.getProperty("jdbc.url");
        String user = props.getProperty("jdbc.user");
        String pass = props.getProperty("jdbc.password");

        return DriverManager.getConnection(url, user, pass);
    }
}
