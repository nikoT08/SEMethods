package com.napier.sem;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests and opt-in integration tests for Report_base.
 *
 * Unit tests run without MySQL.
 * Integration tests run only when WORLD_DB_TESTS=true.
 *
 * Optional environment variables:
 *   WORLD_DB_TESTS=true
 *   WORLD_DB_URL=jdbc:mysql://localhost:3306/world?useSSL=false&allowPublicKeyRetrieval=true
 *   WORLD_DB_USER=root
 *   WORLD_DB_PASSWORD=example
 */
class AppTest {

    private static final String DEFAULT_DB_URL =
            "jdbc:mysql://localhost:3306/world?useSSL=false&allowPublicKeyRetrieval=true";

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;

    @BeforeAll
    static void loadDatabaseConfiguration() {
        dbUrl = environmentOrDefault("WORLD_DB_URL", DEFAULT_DB_URL);
        dbUser = environmentOrDefault("WORLD_DB_USER", "root");
        dbPassword = environmentOrDefault("WORLD_DB_PASSWORD", "example");
    }

    private static String environmentOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private ByteArrayOutputStream outputBuffer() {
        return new ByteArrayOutputStream();
    }

    private PrintStream printStream(ByteArrayOutputStream buffer) {
        return new PrintStream(buffer, true, StandardCharsets.UTF_8);
    }

    private String outputText(ByteArrayOutputStream buffer) {
        return buffer.toString(StandardCharsets.UTF_8);
    }

    // -------------------------
    // Unit tests: no database
    // -------------------------

    @Test
    void readLimitAcceptsPositiveWholeNumber() {
        ByteArrayOutputStream buffer = outputBuffer();

        int result = Report_base.readLimit("25", printStream(buffer));

        assertEquals(25, result);
        assertEquals("", outputText(buffer));
    }

    @Test
    void readLimitTrimsWhitespace() {
        ByteArrayOutputStream buffer = outputBuffer();

        int result = Report_base.readLimit("  7  ", printStream(buffer));

        assertEquals(7, result);
        assertEquals("", outputText(buffer));
    }

    @Test
    void readLimitRejectsZero() {
        ByteArrayOutputStream buffer = outputBuffer();

        int result = Report_base.readLimit("0", printStream(buffer));

        assertEquals(0, result);
        assertTrue(outputText(buffer).contains("N must be greater than zero."));
    }

    @Test
    void readLimitRejectsNegativeNumber() {
        ByteArrayOutputStream buffer = outputBuffer();

        int result = Report_base.readLimit("-3", printStream(buffer));

        assertEquals(0, result);
        assertTrue(outputText(buffer).contains("N must be greater than zero."));
    }

    @Test
    void readLimitRejectsNonNumericInput() {
        ByteArrayOutputStream buffer = outputBuffer();

        int result = Report_base.readLimit("abc", printStream(buffer));

        assertEquals(0, result);
        assertTrue(outputText(buffer).contains("Please enter a valid whole number."));
    }

    @Test
    void readLimitRejectsEmptyInput() {
        ByteArrayOutputStream buffer = outputBuffer();

        int result = Report_base.readLimit("", printStream(buffer));

        assertEquals(0, result);
        assertTrue(outputText(buffer).contains("Please enter a valid whole number."));
    }

    @Test
    void printMenuDisplaysAllReportChoices() {
        ByteArrayOutputStream buffer = outputBuffer();

        Report_base.printMenu(printStream(buffer));

        String output = outputText(buffer);
        assertTrue(output.contains("1. All countries worldwide"));
        assertTrue(output.contains("4. Top N populated countries worldwide"));
        assertTrue(output.contains("9. Population of the world"));
        assertTrue(output.contains("12. Language speaker report"));
        assertTrue(output.contains("0. Exit"));
    }

    @Test
    void topCountriesRejectsInvalidLimitWithoutDatabaseAccess() throws SQLException {
        ByteArrayOutputStream buffer = outputBuffer();

        // The connection is deliberately null. An invalid limit should return before using it.

                Report_base.reportTopCountries(null, 0, printStream(buffer));

        assertTrue(outputText(buffer).contains("N must be greater than zero."));
    }

    @Test
    void topCitiesRejectsInvalidLimitWithoutDatabaseAccess() throws SQLException {
        ByteArrayOutputStream buffer = outputBuffer();


                Report_base.reportTopCities(null, -1, printStream(buffer));

        assertTrue(outputText(buffer).contains("N must be greater than zero."));
    }

    // ---------------------------------------
    // Integration tests: require a real MySQL
    // ---------------------------------------

    private Connection openIntegrationConnection() throws SQLException {
        Assumptions.assumeTrue(
                "true".equalsIgnoreCase(System.getenv("WORLD_DB_TESTS")),
                "Skipping database integration test: set WORLD_DB_TESTS=true to enable it.");

        try {
            return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        } catch (SQLException e) {
            fail("Could not connect to the World database at " + dbUrl
                    + ". Check that MySQL is running and WORLD_DB_URL/credentials are correct. "
                    + e.getMessage());
            throw e; // Required by the compiler; fail() throws an assertion error.
        }
    }

    private void assertReportRuns(ReportAction action, String expectedOutput)
            throws SQLException {
        try (Connection connection = openIntegrationConnection()) {
            ByteArrayOutputStream buffer = outputBuffer();

              action.run(connection, printStream(buffer));

            String output = outputText(buffer);
            assertTrue(output.contains(expectedOutput),
                    () -> "Expected output to contain '" + expectedOutput + "' but was:\n" + output);
            assertTrue(output.contains("="), "Expected report column separator in output.");
        }
    }

    @FunctionalInterface
    private interface ReportAction {
        void run(Connection connection, PrintStream out) throws SQLException;
    }

    @Test
    void integrationAllCountriesReportRuns() throws SQLException {
        assertReportRuns(Report_base::reportAllCountries, "Code");
    }

    @Test
    void integrationCountriesByContinentReportRuns() throws SQLException {
        assertReportRuns((connection, out) ->
                Report_base.reportCountriesByContinent(connection, "Asia", out), "Code");
    }

    @Test
    void integrationCountriesByRegionReportRuns() throws SQLException {
        assertReportRuns((connection, out) ->
                Report_base.reportCountriesByRegion(connection, "Caribbean", out), "Code");
    }

    @Test
    void integrationTopCountriesReportRuns() throws SQLException {
        assertReportRuns((connection, out) ->
                Report_base.reportTopCountries(connection, 5, out), "Code");
    }

    @Test
    void integrationAllCitiesReportRuns() throws SQLException {
        assertReportRuns(Report_base::reportAllCities, "Country");
    }

    @Test
    void integrationCitiesByCountryReportRuns() throws SQLException {
        assertReportRuns((connection, out) ->
                Report_base.reportCitiesByCountry(connection, "Australia", out), "Country");
    }

    @Test
    void integrationTopCitiesReportRuns() throws SQLException {
        assertReportRuns((connection, out) ->
                Report_base.reportTopCities(connection, 5, out), "Country");
    }

    @Test
    void integrationCapitalCitiesReportRuns() throws SQLException {
        assertReportRuns(Report_base::reportCapitalCities, "Country");
    }

    @Test
    void integrationWorldPopulationReportRuns() throws SQLException {
        assertReportRuns(Report_base::reportWorldPopulation, "Population");
    }

    @Test
    void integrationCountryPopulationReportRuns() throws SQLException {
        assertReportRuns((connection, out) ->
                Report_base.reportCountryPopulation(connection, "Canada", out), "Population");
    }

    @Test
    void integrationCityPopulationReportRuns() throws SQLException {
        assertReportRuns((connection, out) ->
                Report_base.reportCityPopulation(connection, "Sydney", out), "Population");
    }

    @Test
    void integrationLanguageSpeakersReportRuns() throws SQLException {
        assertReportRuns(Report_base::reportLanguageSpeakers, "Language");
    }
}
