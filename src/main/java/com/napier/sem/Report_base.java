package com.napier.sem;

import java.sql.*;
import java.util.Scanner;

public class Report_base {

    public static void main(String[] args) throws Exception {

        // localhost is for running in IntelliJ
        String location = "localhost:33060";
        int delay = 2000;
        int n = 5; // how many to show in the top N reports

        // docker passes these in as arguments
        if (args.length >= 1) {
            location = args[0];
        }
        if (args.length >= 2) {
            delay = Integer.parseInt(args[1]);
        }
        if (args.length >= 3) {
            n = Integer.parseInt(args[2]);
        }

        // places to use for the reports
        String continent = "Asia";
        String region = "Caribbean";
        String country = "United Kingdom";
        String district = "Scotland";
        String city = "Edinburgh";

        Connection connection = connect(location, delay);

        // these get reused a lot so I put them here
        String countrySql =
                "SELECT country.Code, country.Name, country.Continent, country.Region, " +
                        "country.Population, capital.Name AS Capital " +
                        "FROM country LEFT JOIN city AS capital ON country.Capital = capital.ID ";

        String citySql =
                "SELECT city.Name, country.Name AS Country, city.District, city.Population " +
                        "FROM city JOIN country ON city.CountryCode = country.Code ";

        String capitalSql =
                "SELECT city.Name, country.Name AS Country, city.Population " +
                        "FROM city JOIN country ON country.Capital = city.ID ";

        String countryOrder = "ORDER BY country.Population DESC";
        String cityOrder = "ORDER BY city.Population DESC";

        try {
            // country reports
            printReport(connection, "All countries in the world",
                    countrySql + countryOrder);
            printReport(connection, "All countries in " + continent,
                    countrySql + "WHERE country.Continent = ? " + countryOrder, continent);
            printReport(connection, "All countries in " + region,
                    countrySql + "WHERE country.Region = ? " + countryOrder, region);
            printReport(connection, "Top " + n + " countries in the world",
                    countrySql + countryOrder + " LIMIT ?", n);
            printReport(connection, "Top " + n + " countries in " + continent,
                    countrySql + "WHERE country.Continent = ? " + countryOrder + " LIMIT ?", continent, n);
            printReport(connection, "Top " + n + " countries in " + region,
                    countrySql + "WHERE country.Region = ? " + countryOrder + " LIMIT ?", region, n);

            // city reports
            printReport(connection, "All cities in the world",
                    citySql + cityOrder);
            printReport(connection, "All cities in " + continent,
                    citySql + "WHERE country.Continent = ? " + cityOrder, continent);
            printReport(connection, "All cities in " + region,
                    citySql + "WHERE country.Region = ? " + cityOrder, region);
            printReport(connection, "All cities in " + country,
                    citySql + "WHERE country.Name = ? " + cityOrder, country);
            printReport(connection, "All cities in " + district,
                    citySql + "WHERE city.District = ? " + cityOrder, district);
            printReport(connection, "Top " + n + " cities in the world",
                    citySql + cityOrder + " LIMIT ?", n);
            printReport(connection, "Top " + n + " cities in " + continent,
                    citySql + "WHERE country.Continent = ? " + cityOrder + " LIMIT ?", continent, n);
            printReport(connection, "Top " + n + " cities in " + region,
                    citySql + "WHERE country.Region = ? " + cityOrder + " LIMIT ?", region, n);
            printReport(connection, "Top " + n + " cities in " + country,
                    citySql + "WHERE country.Name = ? " + cityOrder + " LIMIT ?", country, n);
            printReport(connection, "Top " + n + " cities in " + district,
                    citySql + "WHERE city.District = ? " + cityOrder + " LIMIT ?", district, n);

            // capital city reports
            printReport(connection, "All capital cities in the world",
                    capitalSql + cityOrder);
            printReport(connection, "All capital cities in " + continent,
                    capitalSql + "WHERE country.Continent = ? " + cityOrder, continent);
            printReport(connection, "All capital cities in " + region,
                    capitalSql + "WHERE country.Region = ? " + cityOrder, region);
            printReport(connection, "Top " + n + " capital cities in the world",
                    capitalSql + cityOrder + " LIMIT ?", n);
            printReport(connection, "Top " + n + " capital cities in " + continent,
                    capitalSql + "WHERE country.Continent = ? " + cityOrder + " LIMIT ?", continent, n);
            printReport(connection, "Top " + n + " capital cities in " + region,
                    capitalSql + "WHERE country.Region = ? " + cityOrder + " LIMIT ?", region, n);

            // people in cities vs not in cities
            printReport(connection, "Population of each continent", populationSql("country.Continent"));
            printReport(connection, "Population of each region", populationSql("country.Region"));
            printReport(connection, "Population of each country", populationSql("country.Name"));

            // just the population numbers
            printReport(connection, "Population of the world",
                    "SELECT SUM(Population) AS Population FROM country");
            printReport(connection, "Population of " + continent,
                    "SELECT SUM(Population) AS Population FROM country WHERE Continent = ?", continent);
            printReport(connection, "Population of " + region,
                    "SELECT SUM(Population) AS Population FROM country WHERE Region = ?", region);
            printReport(connection, "Population of " + country,
                    "SELECT Population FROM country WHERE Name = ?", country);
            printReport(connection, "Population of " + district,
                    "SELECT SUM(Population) AS Population FROM city WHERE District = ?", district);
            printReport(connection, "Population of " + city,
                    "SELECT Name, Population FROM city WHERE Name = ?", city);

            // languages
            printReport(connection, "Language speakers",
                    "SELECT cl.Language, " +
                            "ROUND(SUM(c.Population * cl.Percentage / 100)) AS Speakers, " +
                            "ROUND(SUM(c.Population * cl.Percentage / 100) " +
                            "/ (SELECT SUM(Population) FROM country) * 100, 2) AS WorldPercentage " +
                            "FROM countrylanguage cl " +
                            "JOIN country c ON cl.CountryCode = c.Code " +
                            "WHERE cl.Language IN ('Chinese', 'English', 'Hindi', 'Spanish', 'Arabic') " +
                            "GROUP BY cl.Language " +
                            "ORDER BY Speakers DESC");

        } finally {
            connection.close();
            System.out.println("Database connection closed.");
        }
    }

    // works out people living in cities and not in cities
    // adds up the cities for each country first so nothing gets counted twice
    static String populationSql(String groupColumn) {
        return "SELECT " + groupColumn + " AS Name, " +
                "SUM(country.Population) AS Population, " +
                "SUM(IFNULL(c.CityPop, 0)) AS InCities, " +
                "ROUND(SUM(IFNULL(c.CityPop, 0)) / SUM(country.Population) * 100, 2) AS InCitiesPercent, " +
                "SUM(country.Population) - SUM(IFNULL(c.CityPop, 0)) AS NotInCities, " +
                "ROUND((SUM(country.Population) - SUM(IFNULL(c.CityPop, 0))) " +
                "/ SUM(country.Population) * 100, 2) AS NotInCitiesPercent " +
                "FROM country " +
                "LEFT JOIN (SELECT CountryCode, SUM(Population) AS CityPop " +
                "FROM city GROUP BY CountryCode) c ON c.CountryCode = country.Code " +
                "GROUP BY " + groupColumn + " " +
                "ORDER BY Population DESC";
    }

    // runs the sql and prints the results as a table
    // parameters fill in the ? in the sql
    static void printReport(Connection connection, String title, String sql, Object... parameters) {

        System.out.println();
        System.out.println(title);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }

            try (ResultSet results = statement.executeQuery()) {

                int columns = results.getMetaData().getColumnCount();

                // column names
                String header = "";
                for (int i = 1; i <= columns; i++) {
                    if (i > 1) header += ", ";
                    header += results.getMetaData().getColumnLabel(i);
                }
                System.out.println(header);

                boolean found = false;

                // one line per row
                while (results.next()) {
                    found = true;
                    String line = "";
                    for (int i = 1; i <= columns; i++) {
                        if (i > 1) line += ", ";
                        line += results.getString(i);
                    }
                    System.out.println(line);
                }

                if (!found) {
                    System.out.println("No results found.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Could not generate report: " + e.getMessage());
        }
    }

    // asks for N and makes sure it's a positive number (used by the tests)
    public static int readLimit(Scanner scanner) {
        System.out.print("Enter N: ");

        try {
            int limit = Integer.parseInt(scanner.nextLine().trim());

            if (limit > 0) {
                return limit;
            }

            System.out.println("N must be greater than zero.");

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid whole number.");
        }

        return 0;
    }

    // gets one country's population, or -1 if it isn't found (used by the tests)
    static long getCountryPopulation(Connection connection, String country) throws SQLException {

        String sql = "SELECT Population FROM country WHERE Name = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, country);

            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    return results.getLong("Population");
                }
            }
        }

        return -1;
    }

    static Connection connect(String location, int delay)
            throws SQLException, InterruptedException {

        String url = "jdbc:mysql://" + location + "/world?useSSL=false&allowPublicKeyRetrieval=true";

        // keep trying because mysql takes a while to start
        for (int attempt = 1; attempt <= 10; attempt++) {
            try {
                Thread.sleep(delay);
                Connection connection = DriverManager.getConnection(url, "root", "example");
                System.out.println("Connected to MySQL.");
                return connection;

            } catch (SQLException e) {
                System.out.println("Connection attempt " + attempt + " failed.");
                if (attempt == 10) {
                    throw e;
                }
            }
        }
        throw new SQLException("Could not connect to MySQL.");
    }
}