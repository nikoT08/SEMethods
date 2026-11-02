
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

                            if (cityLimit > 0) { // Checks if valid limit was provided
                                printReport(connection,
                                        "SELECT city.Name, country.Name AS Country, " +
                                                "city.District, city.Population " +
                                                "FROM city " +
                                                "JOIN country ON city.CountryCode = country.Code " +
                                                "ORDER BY city.Population DESC LIMIT ?", // Queries top N cities
                                        new String[]{String.valueOf(cityLimit)}); // Passes limit as query parameter
                            }
                            break;

                        case 8: // Choice 8: All capital cities worldwide
                            printReport(connection,
                                    "SELECT capital.Name, country.Name AS Country, " +
                                            "capital.Population " +
                                            "FROM country " +
                                            "JOIN city AS capital ON country.Capital = capital.ID " +
                                            "ORDER BY capital.Population DESC",
                                    new String[]{});
                            break;

                        case 9: // Choice 9: Population of the world
                            printReport(connection,
                                    "SELECT 'World' AS Name, " +
                                            "SUM(Population) AS Population " +
                                            "FROM country", // Calculates total population of all countries
                                    new String[]{});
                            break;

                        case 10: // Choice 10: Population of a country
                            System.out.print("Enter country name: "); // Prompts user for a country name
                            String populationCountry = scanner.nextLine();

                            long population = getCountryPopulation(
                                    connection, populationCountry);

                            if (population == -1) { // Checks if country was not found
                                System.out.println("No results found."); // Prints message if country is missing
                            } else { // Executes if country population is found
                                System.out.println(populationCountry + ": " + population); // Prints country name and population
                            }

                            break;

                        case 11: // Choice 11: Population of a city
                            System.out.print("Enter city name: ");
                            String city = scanner.nextLine(); // Reads the user's typed city name

                            printReport(connection,
                                    "SELECT Name, Population " +
                                            "FROM city WHERE Name = ?", // Filters city by name placeholder
                                    new String[]{city}); // Passes city name as query parameter
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
                                            "ORDER BY Speakers DESC", // Calculates speaker totals and percentages
                                    new String[]{}); // Passes an empty parameter array
                            break; // Exits switch block

                        case 0: // Choice 0: Exit application
                            running = false; // Sets running flag to false to terminate loop
                            System.out.println("Exiting reports."); // Prints exit confirmation message
                            break; // Exits switch block

                        default: // Handles unexpected menu inputs
                            System.out.println("Invalid choice. Try again."); // Prints message for invalid choice
                    }

                } catch (SQLException e) { // Catches database access errors during report execution
                    System.out.println("Could not generate report: " +
                            e.getMessage()); // Prints the SQL error message
                }
            }

        } finally { // Ensures database resources cleanup always executes
            try {
                connection.close(); // Closes the active database connection
                System.out.println("Database connection closed."); // Prints connection closure message
            } catch (SQLException e) { // Catches exceptions during connection closure
                System.out.println("Could not close database connection."); // Prints failure message for closing connection
            }
        }
    }

    // Read a positive number for top-N reports
    public static int readLimit(Scanner scanner) {
        System.out.print("Enter N: "); // Prompts user to enter limit N

        try {
            int limit = Integer.parseInt(scanner.nextLine().trim());

            if (limit > 0) { // Checks if number is strictly positive
                return limit; // Returns valid limit
            }

            System.out.println("N must be greater than zero."); // Prints warning if number is zero or negative

        } catch (NumberFormatException e) { // Catches non-integer parsing errors
            System.out.println("Please enter a valid whole number."); // Prints warning for invalid whole number
        }

        return 0;
    }

    // Execute a report and print its column names and results
    private static void printReport(
            Connection connection,
            String sql,
            String[] parameters) throws SQLException { // Helper method to run SQL queries and print formatted results

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) { // Prepares the SQL query statement

            for (int i = 0; i < parameters.length; i++) { // Loops through each query parameter
                statement.setString(i + 1, parameters[i]); // Binds parameter string value to statement
            }

            // LIMIT parameters must be integers, not strings
            if (sql.contains("LIMIT ?")) { // Checks if query requires an integer LIMIT parameter
                int limit = Integer.parseInt(
                        parameters[parameters.length - 1]); // Extracts limit value from parameters array
                statement.setInt(parameters.length, limit); // Binds limit as an integer parameter
            }

            try (ResultSet results = statement.executeQuery()) { // Executes query and holds result set

                ResultSetMetaData metadata = results.getMetaData(); // Retrieves metadata for column information
                int columns = metadata.getColumnCount(); // Gets total number of columns in result set

                System.out.println(); // Prints an empty line for spacing

                for (int i = 1; i <= columns; i++) { // Loops through each column
                    System.out.printf("%-25s",
                            metadata.getColumnLabel(i)); // Prints formatted column header name
                }

                System.out.println();
                System.out.println("=".repeat(columns * 25)); // Prints a divider underline matching column widths

                boolean found = false; // Tracks whether any result rows exist

                while (results.next()) { // Iterates through each row in result set
                    found = true; // Marks that results were found

                    for (int i = 1; i <= columns; i++) { // Loops through each column in current row
                        Object value = results.getObject(i); // Retrieves object value for current column

                        System.out.printf("%-25s",
                                value == null ? "NULL" : value.toString()); // Prints column value or NULL placeholder
                    }

                    System.out.println();
                }

                if (!found) {
                    System.out.println("No results found."); // Prints message indicating no records found
                }
            }
        }
    }

    static Connection connect(String location, int delay)
        throws SQLException, InterruptedException {

        String url = "jdbc:mysql://" + location + "/world?useSSL=false&allowPublicKeyRetrieval=true";

        //Try to 10 times while MySQL starts
        for (int attempt = 1; attempt <=10; attempt++) { // Loops up to 10 times trying to connect
            try {
                Thread.sleep(delay); // Pauses execution for specified delay duration

                Connection connection = DriverManager.getConnection(url, "root", "example"); // Attempts to connect to MySQL database

                System.out.println("Connected to MySQL."); // Prints success message upon connection
                return connection; // Returns established connection instance

            } catch (SQLException e) { // Catches connection failure exceptions
                System.out.println("Connection attempt " + attempt + "Failed."); // Prints failure notice for current attempt

                if (attempt == 10) { // Checks if final attempt has failed
                    throw e; // Throws exception after all attempts fail
                }

            }
        }
        throw new SQLException("Could not connect to MySQL."); // Throws exception if loop finishes without connecting
    }

    static long getCountryPopulation(
            Connection connection, String country) throws SQLException { // Helper method to fetch specific country population

        String sql = "SELECT Population FROM country WHERE Name = ?"; // Defines SQL query to fetch country population

        try (PreparedStatement statement = connection.prepareStatement(sql)) { // Prepares the population query statement
            statement.setString(1, country); // Binds country name parameter to query

            try (ResultSet results = statement.executeQuery()) { // Executes query and obtains result set
                if (results.next()) { // Checks if a matching country record exists
                    return results.getLong("Population"); // Returns population value as long as integer
                }
            }
        }

        // Country not found.
        return -1; // Returns -1 if country does not exist in the database.
    }
}
