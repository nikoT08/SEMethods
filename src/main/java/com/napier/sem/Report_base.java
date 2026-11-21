package com.napier.sem;

import java.io.PrintStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Reports for the MySQL world database.
 *
 * Each report is a separate method that can be called directly by tests.
 * Scanner is used only by the command-line menu in runMenu().
 */
public class Report_base {

    // Inside Docker Compose the app reaches MySQL on the container port (3306).
    // 3307 is only the port published to the host machine.
    private static final String DB_URL = env("WORLD_DB_URL",
            "jdbc:mysql://db:3306/world?useSSL=false&allowPublicKeyRetrieval=true");
    private static final String DB_USER = env("WORLD_DB_USER", "root");
    private static final String DB_PASSWORD = env("WORLD_DB_PASSWORD", "example");

    // Shared SELECT clauses. Column names follow the report specifications.
    private static final String COUNTRY_SELECT =
            "SELECT c.Code, c.Name, c.Continent, c.Region, c.Population, cap.Name AS Capital " +
                    "FROM country c LEFT JOIN city cap ON c.Capital = cap.ID ";
    private static final String CITY_SELECT =
            "SELECT city.Name, country.Name AS Country, city.District, city.Population " +
                    "FROM city JOIN country ON city.CountryCode = country.Code ";
    private static final String CAPITAL_SELECT =
            "SELECT cap.Name, c.Name AS Country, cap.Population " +
                    "FROM country c JOIN city cap ON c.Capital = cap.ID ";

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    public static void main(String[] args) {
        try (Connection connection = connectWithRetry();
             Scanner scanner = new Scanner(System.in)) {

            runMenu(connection, scanner, System.out);

        } catch (SQLException e) {
            System.out.println("Could not connect to or use the database: " + e.getMessage());
        }
    }

    /** Connects to MySQL, retrying until a connection is available. */
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

    // ------------------------------------------------------------------
    // Menu
    // ------------------------------------------------------------------

    /** Interactive command-line menu. Scanner is confined to this method and its helpers. */
    static void runMenu(Connection connection, Scanner scanner, PrintStream out)
            throws SQLException {
        boolean running = true;

        while (running) {
            printMenu(out);
            if (!scanner.hasNextLine()) {
                return; // input closed
            }
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
                    // Countries
                    case 1 -> reportAllCountries(connection, out);
                    case 2 -> reportCountriesByContinent(connection, ask(scanner, out, "Enter continent: "), out);
                    case 3 -> reportCountriesByRegion(connection, ask(scanner, out, "Enter region: "), out);
                    case 4 -> {
                        int n = askLimit(scanner, out);
                        if (n > 0) reportTopCountries(connection, n, out);
                    }
                    case 5 -> {
                        String continent = ask(scanner, out, "Enter continent: ");
                        int n = askLimit(scanner, out);
                        if (n > 0) reportTopCountriesByContinent(connection, continent, n, out);
                    }
                    case 6 -> {
                        String region = ask(scanner, out, "Enter region: ");
                        int n = askLimit(scanner, out);
                        if (n > 0) reportTopCountriesByRegion(connection, region, n, out);
                    }

                    // Cities
                    case 7 -> reportAllCities(connection, out);
                    case 8 -> reportCitiesByContinent(connection, ask(scanner, out, "Enter continent: "), out);
                    case 9 -> reportCitiesByRegion(connection, ask(scanner, out, "Enter region: "), out);
                    case 10 -> reportCitiesByCountry(connection, ask(scanner, out, "Enter country name: "), out);
                    case 11 -> reportCitiesByDistrict(connection, ask(scanner, out, "Enter district: "), out);
                    case 12 -> {
                        int n = askLimit(scanner, out);
                        if (n > 0) reportTopCities(connection, n, out);
                    }
                    case 13 -> {
                        String continent = ask(scanner, out, "Enter continent: ");
                        int n = askLimit(scanner, out);
                        if (n > 0) reportTopCitiesByContinent(connection, continent, n, out);
                    }
                    case 14 -> {
                        String region = ask(scanner, out, "Enter region: ");
                        int n = askLimit(scanner, out);
                        if (n > 0) reportTopCitiesByRegion(connection, region, n, out);
                    }
                    case 15 -> {
                        String country = ask(scanner, out, "Enter country name: ");
                        int n = askLimit(scanner, out);
                        if (n > 0) reportTopCitiesByCountry(connection, country, n, out);
                    }
                    case 16 -> {
                        String district = ask(scanner, out, "Enter district: ");
                        int n = askLimit(scanner, out);
                        if (n > 0) reportTopCitiesByDistrict(connection, district, n, out);
                    }

                    // Capital cities
                    case 17 -> reportCapitalCities(connection, out);
                    case 18 -> reportCapitalCitiesByContinent(connection, ask(scanner, out, "Enter continent: "), out);
                    case 19 -> reportCapitalCitiesByRegion(connection, ask(scanner, out, "Enter region: "), out);
                    case 20 -> {
                        int n = askLimit(scanner, out);
                        if (n > 0) reportTopCapitalCities(connection, n, out);
                    }
                    case 21 -> {
                        String continent = ask(scanner, out, "Enter continent: ");
                        int n = askLimit(scanner, out);
                        if (n > 0) reportTopCapitalCitiesByContinent(connection, continent, n, out);
                    }
                    case 22 -> {
                        String region = ask(scanner, out, "Enter region: ");
                        int n = askLimit(scanner, out);
                        if (n > 0) reportTopCapitalCitiesByRegion(connection, region, n, out);
                    }

                    // City / non-city population breakdowns
                    case 23 -> reportContinentPopulationBreakdown(connection, out);
                    case 24 -> reportRegionPopulationBreakdown(connection, out);
                    case 25 -> reportCountryPopulationBreakdown(connection, out);

                    // Single population figures
                    case 26 -> reportWorldPopulation(connection, out);
                    case 27 -> reportContinentPopulation(connection, ask(scanner, out, "Enter continent: "), out);
                    case 28 -> reportRegionPopulation(connection, ask(scanner, out, "Enter region: "), out);
                    case 29 -> reportCountryPopulation(connection, ask(scanner, out, "Enter country name: "), out);
                    case 30 -> reportDistrictPopulation(connection, ask(scanner, out, "Enter district: "), out);
                    case 31 -> reportCityPopulation(connection, ask(scanner, out, "Enter city name: "), out);

                    // Languages
                    case 32 -> reportLanguageSpeakers(connection, out);

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

    private static String ask(Scanner scanner, PrintStream out, String label) {
        out.print(label);
        return scanner.hasNextLine() ? scanner.nextLine().trim() : "";
    }

    private static int askLimit(Scanner scanner, PrintStream out) {
        return readLimit(ask(scanner, out, "Enter N: "), out);
    }

    static void printMenu(PrintStream out) {
        out.println("\n===== WORLD DATABASE REPORTS =====");
        out.println("--- Countries ---");
        out.println("1. All countries worldwide");
        out.println("2. All countries in a continent");
        out.println("3. All countries in a region");
        out.println("4. Top N populated countries worldwide");
        out.println("5. Top N populated countries in a continent");
        out.println("6. Top N populated countries in a region");
        out.println("--- Cities ---");
        out.println("7. All cities worldwide");
        out.println("8. All cities in a continent");
        out.println("9. All cities in a region");
        out.println("10. All cities in a country");
        out.println("11. All cities in a district");
        out.println("12. Top N populated cities worldwide");
        out.println("13. Top N populated cities in a continent");
        out.println("14. Top N populated cities in a region");
        out.println("15. Top N populated cities in a country");
        out.println("16. Top N populated cities in a district");
        out.println("--- Capital cities ---");
        out.println("17. All capital cities worldwide");
        out.println("18. All capital cities in a continent");
        out.println("19. All capital cities in a region");
        out.println("20. Top N populated capital cities worldwide");
        out.println("21. Top N populated capital cities in a continent");
        out.println("22. Top N populated capital cities in a region");
        out.println("--- City / non-city population ---");
        out.println("23. By continent");
        out.println("24. By region");
        out.println("25. By country");
        out.println("--- Population lookups ---");
        out.println("26. World");
        out.println("27. Continent");
        out.println("28. Region");
        out.println("29. Country");
        out.println("30. District");
        out.println("31. City");
        out.println("--- Languages ---");
        out.println("32. Language speaker report");
        out.println("0. Exit");
        out.print("Choose a report: ");
    }

    /**
     * Parses a positive top-N limit without reading from Scanner.
     * Returns 0 for invalid input.
     */
    static int readLimit(String input, PrintStream out) {
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

    // ------------------------------------------------------------------
    // Country reports
    // ------------------------------------------------------------------

    static void reportAllCountries(Connection connection, PrintStream out) throws SQLException {
        ranked(connection, COUNTRY_SELECT, null, null, "c.Population", 0, out);
    }

    static void reportCountriesByContinent(Connection connection, String continent, PrintStream out)
            throws SQLException {
        ranked(connection, COUNTRY_SELECT, "c.Continent", continent, "c.Population", 0, out);
    }

    static void reportCountriesByRegion(Connection connection, String region, PrintStream out)
            throws SQLException {
        ranked(connection, COUNTRY_SELECT, "c.Region", region, "c.Population", 0, out);
    }

    static void reportTopCountries(Connection connection, int limit, PrintStream out)
            throws SQLException {
        if (invalidLimit(limit, out)) return;
        ranked(connection, COUNTRY_SELECT, null, null, "c.Population", limit, out);
    }

    static void reportTopCountriesByContinent(
            Connection connection, String continent, int limit, PrintStream out) throws SQLException {
        if (invalidLimit(limit, out)) return;
        ranked(connection, COUNTRY_SELECT, "c.Continent", continent, "c.Population", limit, out);
    }

    static void reportTopCountriesByRegion(
            Connection connection, String region, int limit, PrintStream out) throws SQLException {
        if (invalidLimit(limit, out)) return;
        ranked(connection, COUNTRY_SELECT, "c.Region", region, "c.Population", limit, out);
    }

    // ------------------------------------------------------------------
    // City reports
    // ------------------------------------------------------------------

    static void reportAllCities(Connection connection, PrintStream out) throws SQLException {
        ranked(connection, CITY_SELECT, null, null, "city.Population", 0, out);
    }

    static void reportCitiesByContinent(Connection connection, String continent, PrintStream out)
            throws SQLException {
        ranked(connection, CITY_SELECT, "country.Continent", continent, "city.Population", 0, out);
    }

    static void reportCitiesByRegion(Connection connection, String region, PrintStream out)
            throws SQLException {
        ranked(connection, CITY_SELECT, "country.Region", region, "city.Population", 0, out);
    }

    static void reportCitiesByCountry(Connection connection, String country, PrintStream out)
            throws SQLException {
        ranked(connection, CITY_SELECT, "country.Name", country, "city.Population", 0, out);
    }

    static void reportCitiesByDistrict(Connection connection, String district, PrintStream out)
            throws SQLException {
        ranked(connection, CITY_SELECT, "city.District", district, "city.Population", 0, out);
    }

    static void reportTopCities(Connection connection, int limit, PrintStream out)
            throws SQLException {
        if (invalidLimit(limit, out)) return;
        ranked(connection, CITY_SELECT, null, null, "city.Population", limit, out);
    }

    static void reportTopCitiesByContinent(
            Connection connection, String continent, int limit, PrintStream out) throws SQLException {
        if (invalidLimit(limit, out)) return;
        ranked(connection, CITY_SELECT, "country.Continent", continent, "city.Population", limit, out);
    }

    static void reportTopCitiesByRegion(
            Connection connection, String region, int limit, PrintStream out) throws SQLException {
        if (invalidLimit(limit, out)) return;
        ranked(connection, CITY_SELECT, "country.Region", region, "city.Population", limit, out);
    }

    static void reportTopCitiesByCountry(
            Connection connection, String country, int limit, PrintStream out) throws SQLException {
        if (invalidLimit(limit, out)) return;
        ranked(connection, CITY_SELECT, "country.Name", country, "city.Population", limit, out);
    }

    static void reportTopCitiesByDistrict(
            Connection connection, String district, int limit, PrintStream out) throws SQLException {
        if (invalidLimit(limit, out)) return;
        ranked(connection, CITY_SELECT, "city.District", district, "city.Population", limit, out);
    }

    // ------------------------------------------------------------------
    // Capital city reports
    // ------------------------------------------------------------------

    static void reportCapitalCities(Connection connection, PrintStream out) throws SQLException {
        ranked(connection, CAPITAL_SELECT, null, null, "cap.Population", 0, out);
    }

    static void reportCapitalCitiesByContinent(Connection connection, String continent, PrintStream out)
            throws SQLException {
        ranked(connection, CAPITAL_SELECT, "c.Continent", continent, "cap.Population", 0, out);
    }

    static void reportCapitalCitiesByRegion(Connection connection, String region, PrintStream out)
            throws SQLException {
        ranked(connection, CAPITAL_SELECT, "c.Region", region, "cap.Population", 0, out);
    }

    static void reportTopCapitalCities(Connection connection, int limit, PrintStream out)
            throws SQLException {
        if (invalidLimit(limit, out)) return;
        ranked(connection, CAPITAL_SELECT, null, null, "cap.Population", limit, out);
    }

    static void reportTopCapitalCitiesByContinent(
            Connection connection, String continent, int limit, PrintStream out) throws SQLException {
        if (invalidLimit(limit, out)) return;
        ranked(connection, CAPITAL_SELECT, "c.Continent", continent, "cap.Population", limit, out);
    }

    static void reportTopCapitalCitiesByRegion(
            Connection connection, String region, int limit, PrintStream out) throws SQLException {
        if (invalidLimit(limit, out)) return;
        ranked(connection, CAPITAL_SELECT, "c.Region", region, "cap.Population", limit, out);
    }

    // ------------------------------------------------------------------
    // Population reports (total, living in cities, not living in cities)
    // ------------------------------------------------------------------

    static void reportContinentPopulationBreakdown(Connection connection, PrintStream out)
            throws SQLException {
        populationBreakdown(connection, "c.Continent", "c.Continent", out);
    }

    static void reportRegionPopulationBreakdown(Connection connection, PrintStream out)
            throws SQLException {
        populationBreakdown(connection, "c.Region", "c.Region", out);
    }

    static void reportCountryPopulationBreakdown(Connection connection, PrintStream out)
            throws SQLException {
        populationBreakdown(connection, "c.Name", "c.Code, c.Name", out);
    }

    private static void populationBreakdown(
            Connection connection, String nameColumn, String groupBy, PrintStream out)
            throws SQLException {
        String cityPop = "SUM(COALESCE(u.CityPop, 0))";
        String total = "SUM(c.Population)";
        String sql =
                "SELECT " + nameColumn + " AS Name, " +
                        total + " AS TotalPopulation, " +
                        cityPop + " AS CityPopulation, " +
                        "CONCAT(COALESCE(ROUND(" + cityPop + " / " + total + " * 100, 2), 0), '%') " +
                        "AS CityPercentage, " +
                        "(" + total + " - " + cityPop + ") AS NonCityPopulation, " +
                        "CONCAT(COALESCE(ROUND((" + total + " - " + cityPop + ") / " + total +
                        " * 100, 2), 0), '%') AS NonCityPercentage " +
                        "FROM country c LEFT JOIN " +
                        "(SELECT CountryCode, SUM(Population) AS CityPop FROM city GROUP BY CountryCode) u " +
                        "ON u.CountryCode = c.Code " +
                        "GROUP BY " + groupBy + " ORDER BY TotalPopulation DESC";
        executeReport(connection, sql, new Object[]{}, out);
    }

    // ------------------------------------------------------------------
    // Single population figures
    // ------------------------------------------------------------------

    static void reportWorldPopulation(Connection connection, PrintStream out) throws SQLException {
        executeReport(connection,
                "SELECT 'World' AS Name, SUM(Population) AS Population FROM country",
                new Object[]{}, out);
    }

    static void reportContinentPopulation(Connection connection, String continent, PrintStream out)
            throws SQLException {
        executeReport(connection,
                "SELECT Continent AS Name, SUM(Population) AS Population FROM country " +
                        "WHERE Continent = ? GROUP BY Continent",
                new Object[]{continent}, out);
    }

    static void reportRegionPopulation(Connection connection, String region, PrintStream out)
            throws SQLException {
        executeReport(connection,
                "SELECT Region AS Name, SUM(Population) AS Population FROM country " +
                        "WHERE Region = ? GROUP BY Region",
                new Object[]{region}, out);
    }

    static void reportCountryPopulation(Connection connection, String country, PrintStream out)
            throws SQLException {
        executeReport(connection,
                "SELECT Name, Population FROM country WHERE Name = ?",
                new Object[]{country}, out);
    }

    static void reportDistrictPopulation(Connection connection, String district, PrintStream out)
            throws SQLException {
        // Grouped by country too, because district names can repeat across countries.
        executeReport(connection,
                "SELECT city.District AS Name, country.Name AS Country, " +
                        "SUM(city.Population) AS Population " +
                        "FROM city JOIN country ON city.CountryCode = country.Code " +
                        "WHERE city.District = ? GROUP BY city.District, country.Name",
                new Object[]{district}, out);
    }

    static void reportCityPopulation(Connection connection, String city, PrintStream out)
            throws SQLException {
        // Country and district are shown because city names are not unique.
        executeReport(connection,
                "SELECT city.Name, country.Name AS Country, city.District, city.Population " +
                        "FROM city JOIN country ON city.CountryCode = country.Code " +
                        "WHERE city.Name = ?",
                new Object[]{city}, out);
    }

    // ------------------------------------------------------------------
    // Languages
    // ------------------------------------------------------------------

    static void reportLanguageSpeakers(Connection connection, PrintStream out) throws SQLException {
        executeReport(connection,
                "SELECT cl.Language, " +
                        "CAST(ROUND(SUM(c.Population * cl.Percentage / 100)) AS UNSIGNED) AS Speakers, " +
                        "CONCAT(ROUND(SUM(c.Population * cl.Percentage / 100) " +
                        "/ (SELECT SUM(Population) FROM country) * 100, 2), '%') AS WorldPercentage " +
                        "FROM countrylanguage cl " +
                        "JOIN country c ON cl.CountryCode = c.Code " +
                        "WHERE cl.Language IN ('Chinese', 'English', 'Hindi', 'Spanish', 'Arabic') " +
                        "GROUP BY cl.Language ORDER BY SUM(c.Population * cl.Percentage / 100) DESC",
                new Object[]{}, out);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static boolean invalidLimit(int limit, PrintStream out) {
        if (limit <= 0) {
            out.println("N must be greater than zero.");
            return true;
        }
        return false;
    }

    /**
     * Builds and runs a ranked report: SELECT [WHERE filter = ?] ORDER BY column DESC [LIMIT ?].
     * filterColumn and orderColumn are internal constants, never user input.
     * A limit of 0 or less means "no limit".
     */
    private static void ranked(Connection connection, String select, String filterColumn,
                               String filterValue, String orderColumn, int limit, PrintStream out)
            throws SQLException {
        StringBuilder sql = new StringBuilder(select);
        List<Object> params = new ArrayList<>();

        if (filterColumn != null) {
            sql.append("WHERE ").append(filterColumn).append(" = ? ");
            params.add(filterValue);
        }
        sql.append("ORDER BY ").append(orderColumn).append(" DESC");
        if (limit > 0) {
            sql.append(" LIMIT ?");
            params.add(limit);
        }
        executeReport(connection, sql.toString(), params.toArray(), out);
    }

    /** Executes a report with bind parameters and writes the result to the supplied stream. */
    static void executeReport(
            Connection connection, String sql, Object[] parameters, PrintStream out)
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