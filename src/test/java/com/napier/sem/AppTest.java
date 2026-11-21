package com.napier.sem;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests and opt-in integration tests for Report_base.
 *
 * Unit tests run without MySQL.
 * Integration tests run only when WORLD_DB_TESTS=true.
 *
 * Optional environment variables:
 *   WORLD_DB_TESTS=true
 *   WORLD_DB_URL=jdbc:mysql://localhost:3307/world?useSSL=false&allowPublicKeyRetrieval=true
 *   WORLD_DB_USER=root
 *   WORLD_DB_PASSWORD=example
 */
class AppTest {

    private static final String DEFAULT_DB_URL =
            "jdbc:mysql://localhost:3307/world?useSSL=false&allowPublicKeyRetrieval=true";

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

    private static PrintStream stream(ByteArrayOutputStream buffer) {
        return new PrintStream(buffer, true, StandardCharsets.UTF_8);
    }

    private static String text(ByteArrayOutputStream buffer) {
        return buffer.toString(StandardCharsets.UTF_8);
    }

    // -------------------------
    // Unit tests: no database
    // -------------------------

    @Test
    void readLimitAcceptsPositiveWholeNumber() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        assertEquals(25, Report_base.readLimit("25", stream(buffer)));
        assertEquals("", text(buffer));
    }

    @Test
    void readLimitTrimsWhitespace() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        assertEquals(7, Report_base.readLimit("  7  ", stream(buffer)));
        assertEquals("", text(buffer));
    }

    @Test
    void readLimitRejectsZeroAndNegative() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        assertEquals(0, Report_base.readLimit("0", stream(buffer)));
        assertEquals(0, Report_base.readLimit("-3", stream(buffer)));
        assertTrue(text(buffer).contains("N must be greater than zero."));
    }

    @Test
    void readLimitRejectsNonNumericAndEmptyInput() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        assertEquals(0, Report_base.readLimit("abc", stream(buffer)));
        assertEquals(0, Report_base.readLimit("", stream(buffer)));
        assertTrue(text(buffer).contains("Please enter a valid whole number."));
    }

    @Test
    void printMenuDisplaysAllReportChoices() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Report_base.printMenu(stream(buffer));
        String output = text(buffer);
        assertTrue(output.contains("1. All countries worldwide"));
        assertTrue(output.contains("16. Top N populated cities in a district"));
        assertTrue(output.contains("22. Top N populated capital cities in a region"));
        assertTrue(output.contains("25. By country"));
        assertTrue(output.contains("31. City"));
        assertTrue(output.contains("32. Language speaker report"));
        assertTrue(output.contains("0. Exit"));
    }

    @Test
    void everyTopNReportRejectsInvalidLimitWithoutDatabaseAccess() throws SQLException {
        // The connection is deliberately null: an invalid limit must return before using it.
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = stream(buffer);

        Report_base.reportTopCountries(null, 0, out);
        Report_base.reportTopCountriesByContinent(null, "Asia", 0, out);
        Report_base.reportTopCountriesByRegion(null, "Caribbean", -1, out);
        Report_base.reportTopCities(null, -1, out);
        Report_base.reportTopCitiesByContinent(null, "Asia", 0, out);
        Report_base.reportTopCitiesByRegion(null, "Caribbean", 0, out);
        Report_base.reportTopCitiesByCountry(null, "France", 0, out);
        Report_base.reportTopCitiesByDistrict(null, "Kabul", 0, out);
        Report_base.reportTopCapitalCities(null, 0, out);
        Report_base.reportTopCapitalCitiesByContinent(null, "Asia", 0, out);
        Report_base.reportTopCapitalCitiesByRegion(null, "Caribbean", 0, out);

        long messages = text(buffer).lines()
                .filter(l -> l.contains("N must be greater than zero.")).count();
        assertEquals(11, messages);
    }

    @Test
    void menuExitsOnZero() throws SQLException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Report_base.runMenu(null, new Scanner("0\n"), stream(buffer));
        assertTrue(text(buffer).contains("Exiting reports."));
    }

    @Test
    void menuHandlesInvalidInputThenExits() throws SQLException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Report_base.runMenu(null, new Scanner("abc\n99\n0\n"), stream(buffer));
        String output = text(buffer);
        assertTrue(output.contains("Please enter a valid number."));
        assertTrue(output.contains("Invalid choice. Try again."));
        assertTrue(output.contains("Exiting reports."));
    }

    @Test
    void menuStopsCleanlyWhenInputEnds() throws SQLException {
        Report_base.runMenu(null, new Scanner(""), stream(new ByteArrayOutputStream()));
    }


}