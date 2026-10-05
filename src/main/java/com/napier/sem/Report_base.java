package com.napier.sem;

import java.sql.*;

public class Report_base {

    public static void main(String[] args) {

       Connection connection = null;

        // Wait for MySQL to become available
        while (connection == null) {
            try {
                Thread.sleep(2000);
                connection = DriverManager.getConnection("jdbc:mysql://db:3306/world?useSSL=false&allowPublicKeyRetrieval=true", "root", "example");
                System.out.println("Connected to MySQL.");

            } catch (Exception e) {
                System.out.println("Waiting for MySQL...");
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