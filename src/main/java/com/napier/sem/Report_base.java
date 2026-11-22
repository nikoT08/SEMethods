package com.napier.sem;

import java.io.PrintStream;
import java.sql.*;
import java.util.Scanner;
import java.util.StringJoiner;

/**
 * Population reports for the MySQL world database.
 * Runs every report automatically (no user input).
 *
 * Optional arguments: [0] database location, [1] delay between connection attempts (ms),
 * [2] N for the "top N" reports. The location can also come from the DB_LOCATION
 * environment variable (docker-compose sets it to db:3306).
 */
public class Report_base {

    // connection defaults - localhost:3307 is the port docker-compose publishes for IntelliJ runs
    static final String DEFAULT_LOCATION = "localhost:3307";
    static final int DEFAULT_DELAY = 2000;
    static final int DEFAULT_N = 5; // how many to show in the top N reports
    static final int MAX_ATTEMPTS = 30;
    static final String DB_USER = "root";
    static final String DB_PASSWORD = "example";

    // places to use for the reports
    static final String CONTINENT = "Asia";
    static final String REGION = "Caribbean";
    static final String COUNTRY = "United Kingdom";
    static final String DISTRICT = "Scotland";
    static final String CITY = "Edinburgh";

    // these get reused a lot so I put them here (each select ends with a space)
    static final String COUNTRY_SQL =
            "SELECT country.Code, country.Name, country.Continent, country.Region, " +
                    "country.Population, capital.Name AS Capital " +
                    "FROM country LEFT JOIN city AS capital ON country.Capital = capital.ID ";

    static final String CITY_SQL =
            "SELECT city.Name, country.Name AS Country, city.District, city.Population " +
                    "FROM city JOIN country ON city.CountryCode = country.Code ";

    static final String CAPITAL_SQL =
            "SELECT city.Name, country.Name AS Country, city.Population " +
                    "FROM city JOIN country ON country.Capital = city.ID ";

    static final String COUNTRY_ORDER = "ORDER BY country.Population DESC";
    static final String CITY_ORDER = "ORDER BY city.Population DESC";

    static final String LANGUAGE_SQL =
            "SELECT cl.Language, " +
                    "ROUND(SUM(c.Population * cl.Percentage / 100)) AS Speakers, " +
                    "ROUND(SUM(c.Population * cl.Percentage / 100) " +
                    "/ (SELECT SUM(Population) FROM country) * 100, 2) AS WorldPercentage " +
                    "FROM countrylanguage cl " +
                    "JOIN country c ON cl.CountryCode = c.Code " +
                    "WHERE cl.Language IN ('Chinese', 'English', 'Hindi', 'Spanish', 'Arabic') " +
                    "GROUP BY cl.Language " +
                    "ORDER BY Speakers DESC";

    /** Runs every report. Exits with code 1 if any report failed (so CI can see it). */
    public static void main(String[] args) throws Exception {
        int failures = run(args);
        if (failures > 0) {
            System.exit(1);
        }
    }

    /**
     * Reads the arguments, connects, runs every report and closes the connection.
     *
     * @return the number of reports that failed (0 means everything ran)
     */
    static int run(String[] args) throws Exception {

        // localhost is for running in IntelliJ
        String location = System.getenv("DB_LOCATION") != null
                ? System.getenv("DB_LOCATION") : DEFAULT_LOCATION;
        int delay = DEFAULT_DELAY;
        int n = DEFAULT_N;

        // docker can pass these in as arguments
        if (args.length >= 1) {
            location = args[0];
        }
        if (args.length >= 2) {
            delay = Integer.parseInt(args[1]);
        }
        if (args.length >= 3) {
            n = Integer.parseInt(args[2]);
        }
        if (n <= 0) {
            throw new IllegalArgumentException("N must be greater than zero.");
        }

        Connection connection = connect(location, delay);

        try {
            return runAllReports(connection, System.out, n);
        } finally {
            connection.close();
            System.out.println("Database connection closed.");
        }
    }

    /**
     * Runs all 32 reports once, in order, writing to the given stream.
     * A report that fails is printed and skipped so the rest still run.
     *
     * @return the number of reports that failed
     */
    static int runAllReports(Connection connection, PrintStream out, int n) {

        String continent = CONTINENT;
        String region = REGION;
        String country = COUNTRY;
        String district = DISTRICT;
        String city = CITY;
        int failures = 0;

        // country reports
        failures += runReport(connection, out, "All countries in the world",
                COUNTRY_SQL + COUNTRY_ORDER);
        failures += runReport(connection, out, "All countries in " + continent,
                COUNTRY_SQL + "WHERE country.Continent = ? " + COUNTRY_ORDER, continent);
        failures += runReport(connection, out, "All countries in " + region,
                COUNTRY_SQL + "WHERE country.Region = ? " + COUNTRY_ORDER, region);
        failures += runReport(connection, out, "Top " + n + " countries in the world",
                COUNTRY_SQL + COUNTRY_ORDER + " LIMIT ?", n);
        failures += runReport(connection, out, "Top " + n + " countries in " + continent,
                COUNTRY_SQL + "WHERE country.Continent = ? " + COUNTRY_ORDER + " LIMIT ?", continent, n);
        failures += runReport(connection, out, "Top " + n + " countries in " + region,
                COUNTRY_SQL + "WHERE country.Region = ? " + COUNTRY_ORDER + " LIMIT ?", region, n);

        // city reports
        failures += runReport(connection, out, "All cities in the world",
                CITY_SQL + CITY_ORDER);
        failures += runReport(connection, out, "All cities in " + continent,
                CITY_SQL + "WHERE country.Continent = ? " + CITY_ORDER, continent);
        failures += runReport(connection, out, "All cities in " + region,
                CITY_SQL + "WHERE country.Region = ? " + CITY_ORDER, region);
        failures += runReport(connection, out, "All cities in " + country,
                CITY_SQL + "WHERE country.Name = ? " + CITY_ORDER, country);
        failures += runReport(connection, out, "All cities in " + district,
                CITY_SQL + "WHERE city.District = ? " + CITY_ORDER, district);
        failures += runReport(connection, out, "Top " + n + " cities in the world",
                CITY_SQL + CITY_ORDER + " LIMIT ?", n);
        failures += runReport(connection, out, "Top " + n + " cities in " + continent,
                CITY_SQL + "WHERE country.Continent = ? " + CITY_ORDER + " LIMIT ?", continent, n);
        failures += runReport(connection, out, "Top " + n + " cities in " + region,
                CITY_SQL + "WHERE country.Region = ? " + CITY_ORDER + " LIMIT ?", region, n);
        failures += runReport(connection, out, "Top " + n + " cities in " + country,
                CITY_SQL + "WHERE country.Name = ? " + CITY_ORDER + " LIMIT ?", country, n);
        failures += runReport(connection, out, "Top " + n + " cities in " + district,
                CITY_SQL + "WHERE city.District = ? " + CITY_ORDER + " LIMIT ?", district, n);

        // capital city reports
        failures += runReport(connection, out, "All capital cities in the world",
                CAPITAL_SQL + CITY_ORDER);
        failures += runReport(connection, out, "All capital cities in " + continent,
                CAPITAL_SQL + "WHERE country.Continent = ? " + CITY_ORDER, continent);
        failures += runReport(connection, out, "All capital cities in " + region,
                CAPITAL_SQL + "WHERE country.Region = ? " + CITY_ORDER, region);
        failures += runReport(connection, out, "Top " + n + " capital cities in the world",
                CAPITAL_SQL + CITY_ORDER + " LIMIT ?", n);
        failures += runReport(connection, out, "Top " + n + " capital cities in " + continent,
                CAPITAL_SQL + "WHERE country.Continent = ? " + CITY_ORDER + " LIMIT ?", continent, n);
        failures += runReport(connection, out, "Top " + n + " capital cities in " + region,
                CAPITAL_SQL + "WHERE country.Region = ? " + CITY_ORDER + " LIMIT ?", region, n);

        // people in cities vs not in cities
        failures += runReport(connection, out, "Population of each continent",
                populationSql("country.Continent"));
        failures += runReport(connection, out, "Population of each region",
                populationSql("country.Region"));
        failures += runReport(connection, out, "Population of each country",
                populationSql("country.Name"));

        // just the population numbers
        failures += runReport(connection, out, "Population of the world",
                "SELECT SUM(Population) AS Population FROM country");
        failures += runReport(connection, out, "Population of " + continent,
                "SELECT SUM(Population) AS Population FROM country WHERE Continent = ?", continent);
        failures += runReport(connection, out, "Population of " + region,
                "SELECT SUM(Population) AS Population FROM country WHERE Region = ?", region);
        failures += runReport(connection, out, "Population of " + country,
                "SELECT Population FROM country WHERE Name = ?", country);
        failures += runReport(connection, out, "Population of " + district,
                "SELECT SUM(Population) AS Population FROM city WHERE District = ?", district);
        failures += runReport(connection, out, "Population of " + city,
                "SELECT Name, Population FROM city WHERE Name = ?", city);

        // languages
        failures += runReport(connection, out, "Language speakers", LANGUAGE_SQL);

        return failures;
    }

    /** Runs one report and returns 1 if it failed or 0 if it worked. */
    private static int runReport(Connection connection, PrintStream out,
                                 String title, String sql, Object... parameters) {
        return printReport(connection, out, title, sql, parameters) ? 0 : 1;
    }

    // works out people living in cities and not in cities
    // adds up the cities for each country first so nothing gets counted twice
    // IFNULL stops places with no people (e.g. Antarctica) giving a NULL percentage
    static String populationSql(String groupColumn) {
        return "SELECT " + groupColumn + " AS Name, " +
                "SUM(country.Population) AS Population, " +
                "SUM(IFNULL(c.CityPop, 0)) AS InCities, " +
                "IFNULL(ROUND(SUM(IFNULL(c.CityPop, 0)) / SUM(country.Population) * 100, 2), 0) " +
                "AS InCitiesPercent, " +
                "SUM(country.Population) - SUM(IFNULL(c.CityPop, 0)) AS NotInCities, " +
                "IFNULL(ROUND((SUM(country.Population) - SUM(IFNULL(c.CityPop, 0))) " +
                "/ SUM(country.Population) * 100, 2), 0) AS NotInCitiesPercent " +
                "FROM country " +
                "LEFT JOIN (SELECT CountryCode, SUM(Population) AS CityPop " +
                "FROM city GROUP BY CountryCode) c ON c.CountryCode = country.Code " +
                "GROUP BY " + groupColumn + " " +
                "ORDER BY Population DESC";
    }

    // runs the sql and prints the results to System.out
    // parameters fill in the ? in the sql
    static void printReport(Connection connection, String title, String sql, Object... parameters) {
        printReport(connection, System.out, title, sql, parameters);
    }

    /**
     * Runs the sql and prints the results to the given stream as comma separated lines.
     *
     * @return true if the report ran, false if the database reported an error
     */
    static boolean printReport(Connection connection, PrintStream out,
                               String title, String sql, Object... parameters) {

        out.println();
        out.println(title);

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }

            try (ResultSet results = statement.executeQuery()) {

                ResultSetMetaData metadata = results.getMetaData();
                int columns = metadata.getColumnCount();

                out.println(formatHeader(metadata));

                boolean found = false;

                // one line per row
                while (results.next()) {
                    found = true;
                    out.println(formatRow(results, columns));
                }

                if (!found) {
                    out.println("No results found.");
                }
            }
            return true;

        } catch (SQLException e) {
            out.println("Could not generate report: " + e.getMessage());
            return false;
        }
    }

    // the column names joined with ", "
    static String formatHeader(ResultSetMetaData metadata) throws SQLException {
        StringJoiner header = new StringJoiner(", ");
        for (int i = 1; i <= metadata.getColumnCount(); i++) {
            header.add(metadata.getColumnLabel(i));
        }
        return header.toString();
    }

    // the current row's values joined with ", " (a missing value shows as NULL)
    static String formatRow(ResultSet results, int columns) throws SQLException {
        StringJoiner line = new StringJoiner(", ");
        for (int i = 1; i <= columns; i++) {
            String value = results.getString(i);
            line.add(value == null ? "NULL" : value);
        }
        return line.toString();
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

    static String buildUrl(String location) {
        return "jdbc:mysql://" + location + "/world?useSSL=false&allowPublicKeyRetrieval=true";
    }

    static Connection connect(String location, int delay)
            throws SQLException, InterruptedException {

        // keep trying because mysql takes a while to start
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                Thread.sleep(delay);
                Connection connection =
                        DriverManager.getConnection(buildUrl(location), DB_USER, DB_PASSWORD);
                System.out.println("Connected to MySQL.");
                return connection;

            } catch (SQLException e) {
                System.out.println("Connection attempt " + attempt + " failed.");
                if (attempt == MAX_ATTEMPTS) {
                    throw e;
                }
            }
        }
        throw new SQLException("Could not connect to MySQL.");
    }
}