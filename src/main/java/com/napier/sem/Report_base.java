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
        //Countries filitered by population descending
        try {
            Statement statement = connection.createStatement();

            ResultSet results = statement.executeQuery(
                    "SELECT Name, Population " +
                            "FROM country " +
                            "ORDER BY Population DESC " 
            );

            while (results.next()) {
                System.out.println(
                        results.getString("Name") + ": " +
                                results.getInt("Population")
                );
            }

            }
        //Countries in continent and filtered by population descending
        try {
            Statment statment = connection.createStatement();

            ResultSet results = statement.executeQuery(
                    "SELECT Name, Population" +
                            "FROM country" +
                            "WHERE Continent = 'Europe'" +
                            "ORDER BY population DESC" 
            );
            
            while (results.next()) {
                System.out.println(
                        results.getString("Name") + ": " +
                                results.getInt("Population")
                );
            }

            }
        //Countries in a region and filtered by population descending
        try {
            Statment statment = connection.createStatement();

            ResultSet results = statement.executeQuery(
                    "SELECT Name, Population" +
                            "FROM country" +
                            "WHERE Continent = 'Western Europe'" +
                            "ORDER BY population DESC" +
            );
            
            while (results.next()) {
                System.out.println(
                        results.getString("Name") + ": " +
                                results.getInt("Population")
                );
            }
            
            }
        //Top 5 countries filitered by population descending
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

            }
        //Top 5 countries in continent and filtered by population descending       
        try {
            Statment statment = connection.createStatement();

            ResultSet results = statement.executeQuery(
                    "SELECT Name, Population" +
                            "FROM country" +
                            "WHERE Continent = 'Europe'" +
                            "ORDER BY population DESC" +
                            "LIMIT 5"
            );
            
            while (results.next()) {
                System.out.println(
                        results.getString("Name") + ": " +
                                results.getInt("Population")
                    );
            }
        
            }
        //Top 5 countries in a region and filtered by population descending
        try {
            Statment statment = connection.createStatement();

            ResultSet results = statement.executeQuery(
                    "SELECT Name, Population" +
                            "FROM country" +
                            "WHERE Continent = 'Western Europe'" +
                            "ORDER BY population DESC" +
                            "LIMIT 5"
            );
            
            while (results.next()) {
                System.out.println(
                        results.getString("Name") + ": " +
                                results.getInt("Population")
                    );
            }
        
            }

            //Close
            connection.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
