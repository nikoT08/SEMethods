package com.napier.sem;

import java.sql.*;
import java.util.Scanner;

/**
 * Reports for the MySQL world database.
 *
 * Each report is a separate method that can be called directly by tests.
 * Scanner is used only by the command-line menu in main().
 */
public class Report_base {

    private static final String DB_URL =
            "jdbc:mysql://db:33060/world?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "example";

    public static void main(String[] args) {
        try (Connection connection = connectWithRetry();
             Scanner scanner = new Scanner(System.in)) {

            runMenu(connection, scanner, System.out);

        } catch (SQLException e) {
            System.out.println("Could not connect to or use the database: " + e.getMessage());
        }
    }

    /**
     * Connects to MySQL, retrying until a connection is available.
     * Kept separate from main so it can be tested or replaced independently.
     */
    static Connection connectWithRetry() throws SQLException {
        while (true) {
            try {
                Connection connection =
                        DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                System.out.println("Connected to MySQL.");
                return connection;
            } catch (SQLException e) {
                System.out.println("Waiting for MySQL...");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Interrupted while waiting for MySQL.", interrupted);
                }
            }
        }
    }

    /**
     * Interactive command-line menu. Scanner is confined to this method.
     */
    static void runMenu(Connection connection, Scanner scanner, java.io.PrintStream out)
            throws SQLException {
        boolean running = true;

        while (running) {
            printMenu(out);
            String input = scanner.nextLine().trim();

            final int choice;
            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                out.println("Please enter a valid number.");
                continue;
            }

            try {
                switch (choice) {
                    case 1 -> reportAllCountries(connection, out);
                    case 2 -> {
                        out.print("Enter continent: ");
                        reportCountriesByContinent(connection, scanner.nextLine(), out);
                    }
                    case 3 -> {
                        out.print("Enter region: ");
                        reportCountriesByRegion(connection, scanner.nextLine(), out);
                    }
                    case 4 -> {
                        out.print("Enter N: ");
                        reportTopCountries(connection, readLimit(scanner.nextLine(), out), out);
                    }
                    case 5 -> reportAllCities(connection, out);
                    case 6 -> {
                        out.print("Enter country name: ");
                        reportCitiesByCountry(connection, scanner.nextLine(), out);
                    }
                    case 7 -> {
                        out.print("Enter N: ");
                        reportTopCities(connection, readLimit(scanner.nextLine(), out), out);
                    }
                    case 8 -> reportCapitalCities(connection, out);
                    case 9 -> reportWorldPopulation(connection, out);
                    case 10 -> {
                        out.print("Enter country name: ");
                        reportCountryPopulation(connection, scanner.nextLine(), out);
                    }
                    case 11 -> {
                        out.print("Enter city name: ");
                        reportCityPopulation(connection, scanner.nextLine(), out);
                    }
                    case 12 -> reportLanguageSpeakers(connection, out);
                    case 0 -> {
                        running = false;
                        out.println("Exiting reports.");
                    }
                    default -> out.println("Invalid choice. Try again.");
                }
            } catch (SQLException e) {
                out.println("Could not generate report: " + e.getMessage());
            }
        }
    }

    static void printMenu(java.io.PrintStream out) {
        out.println("\n===== WORLD DATABASE REPORTS =====");
        out.println("1. All countries worldwide");
        out.println("2. Countries by continent");
        out.println("3. Countries by region");
        out.println("4. Top N populated countries worldwide");
        out.println("5. All cities worldwide");
        out.println("6. Cities by country");
        out.println("7. Top N populated cities worldwide");
        out.println("8. All capital cities worldwide");
        out.println("9. Population of the world");
        out.println("10. Population of a country");
        out.println("11. Population of a city");
        out.println("12. Language speaker report");
        out.println("0. Exit");
        out.print("Choose a report: ");
    }

    /**
     * Parses a positive top-N limit without reading from Scanner.
     * Returns 0 for invalid input, matching the menu's existing behaviour.
     */
    static int readLimit(String input, java.io.PrintStream out) {
        try {
            int limit = Integer.parseInt(input.trim());
            if (limit > 0) {
                return limit;
            }
            out.println("N must be greater than zero.");
        } catch (NumberFormatException e) {
            out.println("Please enter a valid whole number.");
        }
        return 0;
    }

    static void reportAllCountries(Connection connection, java.io.PrintStream out)
            throws SQLException {
        executeReport(connection,
                "SELECT Code, Name, Continent, Region, Population, Capital " +
                        "FROM country ORDER BY Population DESC",
                new Object[]{}, out);
    }

    static void reportCountriesByContinent(
            Connection connection, String continent, java.io.PrintStream out)
            throws SQLException {
        executeReport(connection,
                "SELECT Code, Name, Continent, Region, Population, Capital " +
                        "FROM country WHERE Continent = ? ORDER BY Population DESC",
                new Object[]{continent}, out);
    }

    static void reportCountriesByRegion(
            Connection connection, String region, java.io.PrintStream out)
            throws SQLException {
        executeReport(connection,
                "SELECT Code, Name, Continent, Region, Population, Capital " +
                        "FROM country WHERE Region = ? ORDER BY Population DESC",
                new Object[]{region}, out);
    }

    static void reportTopCountries(
            Connection connection, int limit, java.io.PrintStream out) throws SQLException {
        if (limit <= 0) {
            out.println("N must be greater than zero.");
            return;
        }
        executeReport(connection,
                "SELECT Code, Name, Continent, Region, Population, Capital " +
                        "FROM country ORDER BY Population DESC LIMIT ?",
                new Object[]{limit}, out);
    }

    static void reportAllCities(Connection connection, java.io.PrintStream out)
            throws SQLException {
        executeReport(connection,
                "SELECT city.Name, country.Name AS Country, city.District, city.Population " +
                        "FROM city JOIN country ON city.CountryCode = country.Code " +
                        "ORDER BY city.Population DESC",
                new Object[]{}, out);
    }

    static void reportCitiesByCountry(
            Connection connection, String country, java.io.PrintStream out)
            throws SQLException {
        executeReport(connection,
                "SELECT city.Name, country.Name AS Country, city.District, city.Population " +
                        "FROM city JOIN country ON city.CountryCode = country.Code " +
                        "WHERE country.Name = ? ORDER BY city.Population DESC",
                new Object[]{country}, out);
    }

    static void reportTopCities(
            Connection connection, int limit, java.io.PrintStream out) throws SQLException {
        if (limit <= 0) {
            out.println("N must be greater than zero.");
            return;
        }
        executeReport(connection,
                "SELECT city.Name, country.Name AS Country, city.District, city.Population " +
                        "FROM city JOIN country ON city.CountryCode = country.Code " +
                        "ORDER BY city.Population DESC LIMIT ?",
                new Object[]{limit}, out);
    }

    static void reportCapitalCities(Connection connection, java.io.PrintStream out)
            throws SQLException {
        executeReport(connection,
                "SELECT capital.Name, country.Name AS Country, capital.Population " +
                        "FROM country JOIN city AS capital ON country.Capital = capital.ID " +
                        "ORDER BY capital.Population DESC",
                new Object[]{}, out);
    }

    static void reportWorldPopulation(Connection connection, java.io.PrintStream out)
            throws SQLException {
        executeReport(connection,
                "SELECT 'World' AS Name, SUM(Population) AS Population FROM country",
                new Object[]{}, out);
    }

    static void reportCountryPopulation(
            Connection connection, String country, java.io.PrintStream out)
            throws SQLException {
        executeReport(connection,
                "SELECT Name, Population FROM country WHERE Name = ?",
                new Object[]{country}, out);
    }

    static void reportCityPopulation(
            Connection connection, String city, java.io.PrintStream out)
            throws SQLException {
        executeReport(connection,
                "SELECT Name, Population FROM city WHERE Name = ?",
                new Object[]{city}, out);
    }

    static void reportLanguageSpeakers(Connection connection, java.io.PrintStream out)
            throws SQLException {
        executeReport(connection,
                "SELECT cl.Language, " +
                        "ROUND(SUM(c.Population * cl.Percentage / 100)) AS Speakers, " +
                        "ROUND(SUM(c.Population * cl.Percentage / 100) " +
                        "/ (SELECT SUM(Population) FROM country) * 100, 2) AS WorldPercentage " +
                        "FROM countrylanguage cl " +
                        "JOIN country c ON cl.CountryCode = c.Code " +
                        "WHERE cl.Language IN ('Chinese', 'English', 'Hindi', 'Spanish', 'Arabic') " +
                        "GROUP BY cl.Language ORDER BY Speakers DESC",
                new Object[]{}, out);
    }

    /**
     * Executes a report with bind parameters and writes the result to the supplied stream.
     * Object parameters allow integer LIMIT values to be bound as integers.
     */
    static void executeReport(
            Connection connection, String sql, Object[] parameters, java.io.PrintStream out)
            throws SQLException {

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                Object parameter = parameters[i];
                if (parameter instanceof Integer) {
                    statement.setInt(i + 1, (Integer) parameter);
                } else {
                    statement.setString(i + 1, parameter == null ? null : parameter.toString());
                }
            }

            try (ResultSet results = statement.executeQuery()) {
                ResultSetMetaData metadata = results.getMetaData();
                int columns = metadata.getColumnCount();

                out.println();
                for (int i = 1; i <= columns; i++) {
                    out.printf("%-25s", metadata.getColumnLabel(i));
                }
                out.println();
                out.println("=".repeat(columns * 25));

                boolean found = false;
                while (results.next()) {
                    found = true;
                    for (int i = 1; i <= columns; i++) {
                        Object value = results.getObject(i);
                        out.printf("%-25s", value == null ? "NULL" : value.toString());
                    }
                    out.println();
                }

                if (!found) {
                    out.println("No results found.");
                }
            }
        }
    }
}
