package com.napier.sem;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

public class ReportIntegrationTest {

    static Connection connection;

    // Connect before running the tests.
    @BeforeAll
    static void init() throws Exception {
        connection = Report_base.connect("localhost:33060", 2000);
    }

    // Close the connection after all tests finish.
    @AfterAll
    static void cleanup() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void testDatabaseConnection() throws Exception {
        assertTrue(connection.isValid(2));
    }

    @Test
    void testCountryDataIsLoaded() throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(
                     "SELECT COUNT(*) FROM country")) {

            assertTrue(results.next());

            assertTrue(results.getInt(1) > 0,
                    "The country table should contain data.");
        }
    }

    @Test
    void testExistingCountryHasPopulation() throws Exception {
        long population = Report_base.getCountryPopulation(
                connection, "United Kingdom");

        assertTrue(population > 0,
                "United Kingdom should have a positive population.");
    }

    @Test
    void testUnknownCountryReturnsMinusOne() throws Exception {
        long population = Report_base.getCountryPopulation(
                connection, "CountryThatDoesNotExist");

        assertEquals(-1L, population);
    }
}
