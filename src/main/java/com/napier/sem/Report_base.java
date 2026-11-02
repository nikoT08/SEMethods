
package com.napier.sem;

import java.sql.*;
import java.util.Scanner;

public class Report_base {

    public static void main(String[] args) throws Exception {

        //Use localHhost when running from intelliJ
        String location = "localhost:33060";
        int delay = 2000;

        // Docker can supply a different database location and delay
        if (args.length >=1) {
            location = args[0];
        }

        if (args.length >=2) {
            delay = Integer.parseInt(args[1]);
        }

        Connection connection = connect(location, delay);

        try (Scanner scanner = new Scanner(System.in)) {

            boolean running = true;

            while (running) {
                System.out.println("\n===== WORLD DATABASE REPORTS ====="); // Prints menu header title
                System.out.println("1. All countries worldwide"); // Prints menu option 1
                System.out.println("2. Countries by continent");
                System.out.println("3. Countries by region");
                System.out.println("4. Top N populated countries worldwide");
                System.out.println("5. All cities worldwide");
                System.out.println("6. Cities by country");
                System.out.println("7. Top N populated cities worldwide");
                System.out.println("8. All capital cities worldwide");
                System.out.println("9. Population of the world");
                System.out.println("10. Population of a country");
                System.out.println("11. Population of a city");
                System.out.println("12. Language speaker report"); // prints menu option 12
                System.out.println("0. Exit");
                System.out.print("Choose a report: ");

                int choice; // Declares a vatiable to store the user's numeric choices

                try {
                    choice = Integer.parseInt(scanner.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Please enter a valid number.");
                    continue;
                }

                try {
                    switch (choice) {

                        case 1: // Choice 1: All countries worldwide
                            printReport(connection, // Calls helper method to execute query and print results
                                    "SELECT Code, Name, Continent, Region, " +
                                            "Population, Capital " +
                                            "FROM country " +
                                            "ORDER BY Population DESC", // Orders countries by population descending
                                    new String[]{}); // Passes an empty parameter array
                            break; // Exits switch block

                        case 2: // Choice 2: Countries filtered by continent
                            System.out.print("Enter continent: "); // Prompts user for a continent name
                            String continent = scanner.nextLine(); // Reads the user's typed continent name

                            printReport(connection, // Calls helper method with parameter
                                    "SELECT Code, Name, Continent, Region, " +
                                            "Population, Capital " +
                                            "FROM country " +
                                            "WHERE Continent = ? " +
                                            "ORDER BY Population DESC", // Filters by continent placeholder
                                    new String[]{continent}); // Passes continent as query parameter
                            break; // Exits switch block

                        case 3: // Choice 3: Countries filtered by region
                            System.out.print("Enter region: ");
                            String region = scanner.nextLine();

                            printReport(connection,
                                    "SELECT Code, Name, Continent, Region, " +
                                            "Population, Capital " +
                                            "FROM country " +
                                            "WHERE Region = ? " +
                                            "ORDER BY Population DESC",
                                    new String[]{region});
                            break;

                        case 4: // Choice 4: Top N populated countries worldwide
                            int countryLimit = readLimit(scanner);

                            if (countryLimit > 0) { // Checks if valid limit was provided
                                printReport(connection,
                                        "SELECT Code, Name, Continent, Region, " +
                                                "Population, Capital " +
                                                "FROM country " +
                                                "ORDER BY Population DESC LIMIT ?",
                                        new String[]{String.valueOf(countryLimit)});
                            }
                            break;

                        case 5: // Choice 5: All cities worldwide
                            printReport(connection,
                                    "SELECT city.Name, country.Name AS Country, " +
                                            "city.District, city.Population " +
                                            "FROM city " +
                                            "JOIN country ON city.CountryCode = country.Code " +
                                            "ORDER BY city.Population DESC",
                                    new String[]{});
                            break;

                        case 6: // Choice 6: Cities filtered by country name
                            System.out.print("Enter country name: ");
                            String country = scanner.nextLine();

                            printReport(connection,
                                    "SELECT city.Name, country.Name AS Country, " +
                                            "city.District, city.Population " +
                                            "FROM city " +
                                            "JOIN country ON city.CountryCode = country.Code " + // Joins city and country tables
                                            "WHERE country.Name = ? " +
                                            "ORDER BY city.Population DESC",
                                    new String[]{country});
                            break; // Exits switch block

                        case 7: // Choice 7: Top N populated cities worldwide
                            int cityLimit = readLimit(scanner);

                            if (cityLimit > 0) {
                                printReport(connection,
                                        "SELECT city.Name, country.Name AS Country, " +
                                                "city.District, city.Population " +
                                                "FROM city " +
                                                "JOIN country ON city.CountryCode = country.Code " +
                                                "ORDER BY city.Population DESC LIMIT ?",
                                        new String[]{String.valueOf(cityLimit)});
                            }
                            break;

                        case 8:
                            printReport(connection,
                                    "SELECT capital.Name, country.Name AS Country, " +
                                            "capital.Population " +
                                            "FROM country " +
                                            "JOIN city AS capital ON country.Capital = capital.ID " +
                                            "ORDER BY capital.Population DESC",
                                    new String[]{});
                            break;

                        case 9:
                            printReport(connection,
                                    "SELECT 'World' AS Name, " +
                                            "SUM(Population) AS Population " +
                                            "FROM country",
                                    new String[]{});
                            break;

                        case 10:
                            System.out.print("Enter country name: ");
                            String populationCountry = scanner.nextLine();

                            long population = getCountryPopulation(
                                    connection, populationCountry);

                            if (population == -1) {
                                System.out.println("No results found.");
                            } else {
                                System.out.println(populationCountry + ": " + population);
                            }

                            break;

                        case 11:
                            System.out.print("Enter city name: ");
                            String city = scanner.nextLine();

                            printReport(connection,
                                    "SELECT Name, Population " +
                                            "FROM city WHERE Name = ?",
                                    new String[]{city});
                            break;

                        case 12:
                            printReport(connection,
                                    "SELECT cl.Language, " +
                                            "ROUND(SUM(c.Population * cl.Percentage / 100)) " +
                                            "AS Speakers, " +
                                            "ROUND(SUM(c.Population * cl.Percentage / 100) " +
                                            "/ (SELECT SUM(Population) FROM country) * 100, 2) " +
                                            "AS WorldPercentage " +
                                            "FROM countrylanguage cl " +
                                            "JOIN country c ON cl.CountryCode = c.Code " +
                                            "WHERE cl.Language IN " +
                                            "('Chinese', 'English', 'Hindi', 'Spanish', 'Arabic') " +
                                            "GROUP BY cl.Language " +
                                            "ORDER BY Speakers DESC",
                                    new String[]{});
                            break;

                        case 0:
                            running = false;
                            System.out.println("Exiting reports.");
                            break;

                        default:
                            System.out.println("Invalid choice. Try again.");
                    }

                } catch (SQLException e) {
                    System.out.println("Could not generate report: " +
                            e.getMessage());
                }
            }

        } finally {
            try {
                connection.close();
                System.out.println("Database connection closed.");
            } catch (SQLException e) {
                System.out.println("Could not close database connection.");
            }
        }
    }

    // Read a positive number for top-N reports
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

    // Execute a report and print its column names and results
    private static void printReport(
            Connection connection,
            String sql,
            String[] parameters) throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            for (int i = 0; i < parameters.length; i++) {
                statement.setString(i + 1, parameters[i]);
            }

            // LIMIT parameters must be integers, not strings
            if (sql.contains("LIMIT ?")) {
                int limit = Integer.parseInt(
                        parameters[parameters.length - 1]);
                statement.setInt(parameters.length, limit);
            }

            try (ResultSet results = statement.executeQuery()) {

                ResultSetMetaData metadata = results.getMetaData();
                int columns = metadata.getColumnCount();

                System.out.println();

                for (int i = 1; i <= columns; i++) {
                    System.out.printf("%-25s",
                            metadata.getColumnLabel(i));
                }

                System.out.println();
                System.out.println("=".repeat(columns * 25));

                boolean found = false;

                while (results.next()) {
                    found = true;

                    for (int i = 1; i <= columns; i++) {
                        Object value = results.getObject(i);

                        System.out.printf("%-25s",
                                value == null ? "NULL" : value.toString());
                    }

                    System.out.println();
                }

                if (!found) {
                    System.out.println("No results found.");
                }
            }
        }
    }

    static Connection connect(String location, int delay)
        throws SQLException, InterruptedException {

        String url = "jdbc:mysql://" + location + "/world?useSSL=false&allowPublicKeyRetrieval=true";

        //Try to 10 times while MySQL starts
        for (int attempt = 1; attempt <=10; attempt++) {
            try {
                Thread.sleep(delay);

                Connection connection = DriverManager.getConnection(url, "root", "example");

                System.out.println("Connected to MySQL.");
                return connection;

            } catch (SQLException e) {
                System.out.println("Connection attempt " + attempt + "Failed.");

                if (attempt == 10) {
                    throw e;
                }

            }
        }
        throw new SQLException("Could not connect to MySQL.");
    }

    static long getCountryPopulation(
            Connection connection, String country) throws SQLException {

        String sql = "SELECT Population FROM country WHERE Name = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, country);

            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    return results.getLong("Population");
                }
            }
        }

        // Country not found.
        return -1;
    }
}
