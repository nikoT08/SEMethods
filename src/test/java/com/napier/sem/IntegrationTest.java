package com.napier.sem;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests: these run the real reports against a real MySQL world database.
 *
 * - Locally, if no database is running they are skipped (start one with: docker compose up -d db).
 * - In CI, set WORLD_DB_TESTS=true: the tests then wait for the database and FAIL if it never appears.
 * - The database location comes from DB_LOCATION (default localhost:3307).
 */
class IntegrationTest {

    private static Connection connection;

    @BeforeAll
    static void connectToDatabase() throws Exception {
        String location = System.getenv("DB_LOCATION") != null
                ? System.getenv("DB_LOCATION") : Report_base.DEFAULT_LOCATION;
        boolean required = "true".equalsIgnoreCase(System.getenv("WORLD_DB_TESTS"));

        if (required) {
            // keeps retrying while MySQL starts, and throws (fails the tests) if it never does
            connection = Report_base.connect(location, 2000);
        } else {
            try {
                connection = DriverManager.getConnection(
                        Report_base.buildUrl(location) + "&connectTimeout=3000",
                        Report_base.DB_USER, Report_base.DB_PASSWORD);
            } catch (SQLException e) {
                Assumptions.assumeTrue(false, "No database at " + location
                        + " - skipping integration tests (set WORLD_DB_TESTS=true to require it)");
            }
        }
    }

    @AfterAll
    static void closeConnection() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Runs a report through printReport and returns what it printed. Fails if the report fails. */
    private static String report(String title, String sql, Object... parameters) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        boolean ok = Report_base.printReport(connection,
                new PrintStream(buffer, true, StandardCharsets.UTF_8), title, sql, parameters);
        String output = buffer.toString(StandardCharsets.UTF_8);
        assertTrue(ok, "Report failed: " + output);
        return output;
    }

    /** Output is: blank line, title, header, then one line per row. */
    private static long rows(String output) {
        return output.lines().count() - 3;
    }

    private static List<String> dataLines(String output) {
        return output.lines().skip(3).toList();
    }

    private static long count(String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            try (ResultSet results = statement.executeQuery()) {
                results.next();
                return results.getLong(1);
            }
        }
    }

    // ------------------------------------------------------------------
    // The right data is loaded
    // ------------------------------------------------------------------

    @Test
    void theWorldDatabaseIsLoaded() throws SQLException {
        assertEquals(239, count("SELECT COUNT(*) FROM country"));
        assertTrue(count("SELECT COUNT(*) FROM city") > 4000);
        assertTrue(count("SELECT COUNT(*) FROM countrylanguage") > 900);
    }

    // ------------------------------------------------------------------
    // Country reports
    // ------------------------------------------------------------------

    @Test
    void allCountriesReturnsEveryCountryLargestFirst() throws SQLException {
        String output = report("All countries", Report_base.COUNTRY_SQL + Report_base.COUNTRY_ORDER);

        assertTrue(output.contains("Code, Name, Continent, Region, Population, Capital"));
        assertEquals(count("SELECT COUNT(*) FROM country"), rows(output));
        List<String> lines = dataLines(output);
        assertTrue(lines.get(0).contains("China"));
        assertTrue(lines.get(1).contains("India"));
    }

    @Test
    void countriesInAContinentAreOnlyFromThatContinent() throws SQLException {
        String output = report("Europe", Report_base.COUNTRY_SQL
                + "WHERE country.Continent = ? " + Report_base.COUNTRY_ORDER, "Europe");

        assertEquals(count("SELECT COUNT(*) FROM country WHERE Continent = ?", "Europe"), rows(output));
        assertTrue(rows(output) > 0);
        dataLines(output).forEach(line -> assertTrue(line.contains(", Europe, "), line));
    }

    @Test
    void countriesInARegionAreOnlyFromThatRegion() throws SQLException {
        String output = report("Caribbean", Report_base.COUNTRY_SQL
                + "WHERE country.Region = ? " + Report_base.COUNTRY_ORDER, Report_base.REGION);

        assertEquals(count("SELECT COUNT(*) FROM country WHERE Region = ?", Report_base.REGION), rows(output));
        assertTrue(rows(output) > 0);
        dataLines(output).forEach(line -> assertTrue(line.contains(", Caribbean, "), line));
    }

    @Test
    void topNCountriesReturnsExactlyNLargestFirst() {
        String output = report("Top 5 countries", Report_base.COUNTRY_SQL
                + Report_base.COUNTRY_ORDER + " LIMIT ?", 5);

        assertEquals(5, rows(output));
        List<String> lines = dataLines(output);
        assertTrue(lines.get(0).contains("China"));
        assertTrue(lines.get(1).contains("India"));
        assertTrue(lines.get(2).contains("United States"));
    }

    @Test
    void topNCountriesInAContinentReturnsExactlyN() {
        String output = report("Top 3 in Asia", Report_base.COUNTRY_SQL
                + "WHERE country.Continent = ? " + Report_base.COUNTRY_ORDER + " LIMIT ?", "Asia", 3);

        assertEquals(3, rows(output));
        assertTrue(dataLines(output).get(0).contains("China"));
    }

    // ------------------------------------------------------------------
    // City reports
    // ------------------------------------------------------------------

    @Test
    void allCitiesReturnsEveryCityLargestFirst() throws SQLException {
        String output = report("All cities", Report_base.CITY_SQL + Report_base.CITY_ORDER);

        assertTrue(output.contains("Name, Country, District, Population"));
        assertEquals(count("SELECT COUNT(*) FROM city"), rows(output));
        assertTrue(dataLines(output).get(0).contains("Mumbai"));
    }

    @Test
    void citiesInADistrictAreOnlyFromThatDistrict() throws SQLException {
        String output = report("Scotland", Report_base.CITY_SQL
                + "WHERE city.District = ? " + Report_base.CITY_ORDER, Report_base.DISTRICT);

        assertEquals(count("SELECT COUNT(*) FROM city WHERE District = ?", Report_base.DISTRICT), rows(output));
        assertTrue(output.contains("Edinburgh"));
        assertTrue(output.contains("Glasgow"));
        dataLines(output).forEach(line -> assertTrue(line.contains(", Scotland, "), line));
    }

    @Test
    void citiesInACountryAreOnlyFromThatCountry() throws SQLException {
        String output = report("UK", Report_base.CITY_SQL
                + "WHERE country.Name = ? " + Report_base.CITY_ORDER, Report_base.COUNTRY);

        assertTrue(rows(output) > 4);
        assertTrue(output.contains("Edinburgh"));
        dataLines(output).forEach(line -> assertTrue(line.contains(", United Kingdom, "), line));
    }

    @Test
    void topNCitiesInAContinentReturnsExactlyN() {
        String output = report("Top 5 in Europe", Report_base.CITY_SQL
                + "WHERE country.Continent = ? " + Report_base.CITY_ORDER + " LIMIT ?", "Europe", 5);

        assertEquals(5, rows(output));
        assertTrue(dataLines(output).get(0).contains("Moscow"));
    }

    @Test
    void topNCitiesInADistrictReturnsExactlyN() {
        String output = report("Top 2 in Scotland", Report_base.CITY_SQL
                + "WHERE city.District = ? " + Report_base.CITY_ORDER + " LIMIT ?", Report_base.DISTRICT, 2);

        assertEquals(2, rows(output));
        assertTrue(dataLines(output).get(0).contains("Glasgow"));
    }

    // ------------------------------------------------------------------
    // Capital city reports
    // ------------------------------------------------------------------

    @Test
    void allCapitalCitiesReturnsOneRowPerCapitalLargestFirst() throws SQLException {
        String output = report("Capitals", Report_base.CAPITAL_SQL + Report_base.CITY_ORDER);

        assertTrue(output.contains("Name, Country, Population"));
        assertEquals(count("SELECT COUNT(*) FROM country JOIN city ON country.Capital = city.ID"), rows(output));
        assertTrue(dataLines(output).get(0).contains("Seoul"));
    }

    @Test
    void topNCapitalCitiesInARegionReturnsExactlyN() {
        String output = report("Top 3 capitals in the Caribbean", Report_base.CAPITAL_SQL
                + "WHERE country.Region = ? " + Report_base.CITY_ORDER + " LIMIT ?", Report_base.REGION, 3);

        assertEquals(3, rows(output));
    }

    // ------------------------------------------------------------------
    // Population in cities / not in cities
    // ------------------------------------------------------------------

    @Test
    void continentPopulationSplitsIntoCityAndNonCityPeople() throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(Report_base.populationSql("country.Continent"));
             ResultSet results = statement.executeQuery()) {

            int continents = 0;
            BigDecimal total = BigDecimal.ZERO;
            BigDecimal previous = null;

            while (results.next()) {
                continents++;
                BigDecimal population = results.getBigDecimal("Population");
                BigDecimal inCities = results.getBigDecimal("InCities");
                BigDecimal notInCities = results.getBigDecimal("NotInCities");
                BigDecimal percentages = results.getBigDecimal("InCitiesPercent")
                        .add(results.getBigDecimal("NotInCitiesPercent"));

                assertEquals(0, population.compareTo(inCities.add(notInCities)), results.getString("Name"));
                assertTrue(percentages.subtract(new BigDecimal(100)).abs().doubleValue() <= 0.02,
                        "percentages add up to " + percentages);
                if (previous != null) {
                    assertTrue(previous.compareTo(population) >= 0, "largest population first");
                }
                previous = population;
                total = total.add(population);
            }

            assertEquals(7, continents);
            assertEquals(0, total.compareTo(new BigDecimal(count("SELECT SUM(Population) FROM country"))));
        }
    }

    @Test
    void regionAndCountryPopulationReportsHaveOneRowPerGroup() throws SQLException {
        String regions = report("Regions", Report_base.populationSql("country.Region"));
        String countries = report("Countries", Report_base.populationSql("country.Name"));

        assertTrue(regions.contains("Name, Population, InCities, InCitiesPercent, NotInCities, NotInCitiesPercent"));
        assertEquals(count("SELECT COUNT(DISTINCT Region) FROM country"), rows(regions));
        assertEquals(count("SELECT COUNT(*) FROM country"), rows(countries));
    }

    // ------------------------------------------------------------------
    // Population lookups
    // ------------------------------------------------------------------

    @Test
    void populationOfTheWorldMatchesTheSumOfAllCountries() throws SQLException {
        String output = report("World", "SELECT SUM(Population) AS Population FROM country");

        assertEquals(1, rows(output));
        assertTrue(count("SELECT SUM(Population) FROM country") > 6_000_000_000L);
        assertTrue(output.contains(String.valueOf(count("SELECT SUM(Population) FROM country"))));
    }

    @Test
    void populationOfAContinentRegionDistrictAndCity() {
        assertEquals(1, rows(report("Continent",
                "SELECT SUM(Population) AS Population FROM country WHERE Continent = ?", "Asia")));
        assertEquals(1, rows(report("Region",
                "SELECT SUM(Population) AS Population FROM country WHERE Region = ?", Report_base.REGION)));
        assertEquals(1, rows(report("District",
                "SELECT SUM(Population) AS Population FROM city WHERE District = ?", Report_base.DISTRICT)));

        String city = report("City", "SELECT Name, Population FROM city WHERE Name = ?", Report_base.CITY);
        assertTrue(city.contains("Edinburgh"));
    }

    @Test
    void getCountryPopulationFindsRealCountriesOnly() throws SQLException {
        assertEquals(59623400L, Report_base.getCountryPopulation(connection, "United Kingdom"));
        assertEquals(-1, Report_base.getCountryPopulation(connection, "Atlantis"));
    }

    @Test
    void anUnknownPlaceGivesNoResults() {
        String output = report("Atlantis", "SELECT Population FROM country WHERE Name = ?", "Atlantis");
        assertTrue(output.contains("No results found."));
        assertEquals(0, rows(output) - 1); // only the "No results found." line follows the header
    }

    // ------------------------------------------------------------------
    // Languages
    // ------------------------------------------------------------------

    @Test
    void languageReportHasTheFiveLanguagesLargestFirstWithPercentages() {
        String output = report("Languages", Report_base.LANGUAGE_SQL);

        assertEquals(5, rows(output));
        List<String> lines = dataLines(output);
        assertTrue(output.contains("Language, Speakers, WorldPercentage"));
        assertTrue(lines.get(0).startsWith("Chinese, "));
        assertTrue(lines.get(4).startsWith("Arabic, "));

        long previousSpeakers = Long.MAX_VALUE;
        for (String line : lines) {
            String[] parts = line.split(", ");
            long speakers = new BigDecimal(parts[1]).longValue();
            double percentage = Double.parseDouble(parts[2]);
            assertTrue(speakers <= previousSpeakers, "largest first: " + line);
            assertTrue(percentage > 0 && percentage < 100, "percentage of the world: " + line);
            previousSpeakers = speakers;
        }
    }

    // ------------------------------------------------------------------
    // Everything together
    // ------------------------------------------------------------------

    @Test
    void runAllReportsRunsEveryReportAgainstTheRealDatabase() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int failures = Report_base.runAllReports(connection,
                new PrintStream(buffer, true, StandardCharsets.UTF_8), 5);

        String output = buffer.toString(StandardCharsets.UTF_8);
        assertEquals(0, failures, output);
        assertFalse(output.contains("Could not generate report"));
        assertTrue(output.contains("Top 5 countries in the world"));
        assertTrue(output.contains("Language speakers"));
        // all cities + all countries alone are more than 4000 lines
        assertTrue(output.lines().count() > 4000);
    }

    @Test
    void aBrokenQueryIsReportedAsAFailure() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        boolean ok = Report_base.printReport(connection,
                new PrintStream(buffer, true, StandardCharsets.UTF_8), "Broken", "SELEC nothing");

        assertFalse(ok);
        assertTrue(buffer.toString(StandardCharsets.UTF_8).contains("Could not generate report"));
    }
}