package com.napier.sem;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class Report_base {

    public static void main(String[] args) {

        String url = "jdbc:mysql://mysql:3306/world";
        String username = "root";
        String password = "root";

        Connection connection = null;

        // Wait for MySQL to become available
        while (connection == null) {
            try {
                connection = DriverManager.getConnection(
                        url,
                        username,
                        password
                );

                System.out.println("Connected to MySQL.");

            } catch (Exception e) {
                System.out.println("Waiting for MySQL...");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        try {
            Statement statement = connection.createStatement();

            ResultSet results = statement.executeQuery(
                    "SELECT Name, Population " +
                            "FROM country " +
                            "ORDER BY Population DESC " +
                            "LIMIT 5"
            );

            while (results.next()) {
                System.out.println(
                        results.getString("Name") + ": " +
                                results.getInt("Population")
                );
            }

            connection.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}