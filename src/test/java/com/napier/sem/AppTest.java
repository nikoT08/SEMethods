package com.napier.sem;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Report_base. None of them need a real database.
 *
 * - JDBC objects (Connection, PreparedStatement, ResultSet) are mocked.
 * - For connect() and main(), the real MySQL driver is swapped out for a fake driver that
 *   hands back the mocked Connection, so those methods run all of their real code.
 * - System.out is captured so the printed reports can be checked.
 */
class AppTest {

    // ------------------------------------------------------------------
    // Test helpers
    // ------------------------------------------------------------------

    /** A stand-in for the MySQL driver that returns whatever Connection the test gives it. */
    private static class FakeDriver implements Driver {
        Connection connection;
        int failuresBeforeSuccess = 0;
        int connectCalls = 0;
        String lastUrl;

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            connectCalls++;
            lastUrl = url;
            if (failuresBeforeSuccess > 0) {
                failuresBeforeSuccess--;
                throw new SQLException("not ready yet");
            }
            return connection;
        }

        @Override
        public boolean acceptsURL(String url) {
            return url != null && url.startsWith("jdbc:mysql://");
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
            return new DriverPropertyInfo[0];
        }

        @Override
        public int getMajorVersion() {
            return 1;
        }

        @Override
        public int getMinorVersion() {
            return 0;
        }

        @Override
        public boolean jdbcCompliant() {
            return false;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }
    }

    /** A mocked Connection whose queries return the supplied column labels and rows. */
    private static class MockDb {
        final Connection connection = mock(Connection.class);
        final PreparedStatement statement = mock(PreparedStatement.class);
        final ResultSet results = mock(ResultSet.class);

        MockDb(String[] labels, String[][] rows) throws SQLException {
            ResultSetMetaData metadata = mock(ResultSetMetaData.class);
            AtomicInteger cursor = new AtomicInteger(0);

            when(connection.prepareStatement(anyString())).thenReturn(statement);
            when(statement.executeQuery()).thenReturn(results);
            when(results.getMetaData()).thenReturn(metadata);
            when(metadata.getColumnCount()).thenReturn(labels.length);
            for (int i = 0; i < labels.length; i++) {
                when(metadata.getColumnLabel(i + 1)).thenReturn(labels[i]);
            }
            when(results.next()).thenAnswer(inv -> cursor.getAndIncrement() < rows.length);
            when(results.getString(anyInt()))
                    .thenAnswer(inv -> rows[cursor.get() - 1][(Integer) inv.getArgument(0) - 1]);
        }

        static MockDb empty() throws SQLException {
            return new MockDb(new String[]{"Name"}, new String[0][]);
        }
    }

    @FunctionalInterface
    private interface Action {
        void run() throws Exception;
    }

    /** Runs the action with System.out captured, and returns everything that was printed. */
    private static String capture(Action action) throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private static void assertSql(String sql, String... fragments) {
        for (String fragment : fragments) {
            assertTrue(sql.contains(fragment), "Expected [" + fragment + "] in: " + sql);
        }
    }

    private static long count(String text, String search) {
        return text.lines().filter(line -> line.contains(search)).count();
    }

    private final List<Driver> removedDrivers = new ArrayList<>();
    private FakeDriver fake;

    /** Removes the real MySQL driver and installs the fake one, so nothing touches a real database. */
    @BeforeEach
    void installFakeDriver() throws SQLException {
        for (Driver driver : Collections.list(DriverManager.getDrivers())) {
            if (driver.getClass().getName().startsWith("com.mysql")) {
                DriverManager.deregisterDriver(driver);
                removedDrivers.add(driver);
            }
        }
        fake = new FakeDriver();
        DriverManager.registerDriver(fake);
    }

    @AfterEach
    void restoreRealDriver() throws SQLException {
        DriverManager.deregisterDriver(fake);
        for (Driver driver : removedDrivers) {
            DriverManager.registerDriver(driver);
        }
        removedDrivers.clear();
    }

    // ------------------------------------------------------------------
    // populationSql
    // ------------------------------------------------------------------

    @Test
    void populationSqlGroupsByTheGivenColumn() {
        String sql = Report_base.populationSql("country.Continent");
        assertSql(sql, "GROUP BY country.Continent", "ORDER BY Population DESC");
        // the column appears in the SELECT (as Name) and in the GROUP BY
        assertEquals(2, sql.split("country\\.Continent", -1).length - 1);
    }

    @Test
    void populationSqlReturnsPeopleInAndNotInCitiesWithPercentages() {
        String sql = Report_base.populationSql("country.Region");
        assertSql(sql, "AS Name", "AS Population", "AS InCities", "AS InCitiesPercent",
                "AS NotInCities", "AS NotInCitiesPercent",
                "LEFT JOIN (SELECT CountryCode, SUM(Population) AS CityPop FROM city GROUP BY CountryCode)");
    }

    // ------------------------------------------------------------------
    // printReport
    // ------------------------------------------------------------------

    @Test
    void printReportPrintsTitleHeaderAndRowsInOrder() throws Exception {
        MockDb db = new MockDb(new String[]{"Name", "Population"},
                new String[][]{{"Paris", "2125246"}, {"Lyon", "445452"}});

        String output = capture(() -> Report_base.printReport(db.connection, "Cities in France", "SELECT 1"));

        assertTrue(output.contains("Cities in France"));
        assertTrue(output.contains("Name, Population"));
        assertTrue(output.contains("Paris, 2125246"));
        assertTrue(output.indexOf("Paris") < output.indexOf("Lyon"));
        assertFalse(output.contains("No results found."));
    }

    @Test
    void printReportSaysWhenThereAreNoResults() throws Exception {
        MockDb db = MockDb.empty();
        String output = capture(() -> Report_base.printReport(db.connection, "Nothing", "SELECT 1"));
        assertTrue(output.contains("No results found."));
    }

    @Test
    void printReportBindsParametersInOrder() throws Exception {
        MockDb db = MockDb.empty();
        capture(() -> Report_base.printReport(db.connection, "T", "SELECT 1", "Asia", 5));
        verify(db.statement).setObject(1, "Asia");
        verify(db.statement).setObject(2, 5);
    }

    @Test
    void printReportPassesTheSqlToTheDatabase() throws Exception {
        MockDb db = MockDb.empty();
        capture(() -> Report_base.printReport(db.connection, "T", "SELECT 42"));
        verify(db.connection).prepareStatement("SELECT 42");
    }

    @Test
    void printReportClosesTheStatementAndResults() throws Exception {
        MockDb db = MockDb.empty();
        capture(() -> Report_base.printReport(db.connection, "T", "SELECT 1"));
        verify(db.statement).close();
        verify(db.results).close();
    }

    @Test
    void printReportReportsDatabaseErrorsInsteadOfThrowing() throws Exception {
        Connection connection = mock(Connection.class);
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("boom"));

        String output = capture(() -> Report_base.printReport(connection, "Broken report", "SELECT 1"));

        assertTrue(output.contains("Broken report"));
        assertTrue(output.contains("Could not generate report: boom"));
    }

    // ------------------------------------------------------------------
    // readLimit
    // ------------------------------------------------------------------

    @Test
    void readLimitAcceptsAPositiveWholeNumber() throws Exception {
        AtomicInteger result = new AtomicInteger();
        String output = capture(() -> result.set(Report_base.readLimit(new Scanner("25\n"))));
        assertEquals(25, result.get());
        assertTrue(output.contains("Enter N:"));
        assertFalse(output.contains("must be greater"));
    }

    @Test
    void readLimitTrimsWhitespace() throws Exception {
        AtomicInteger result = new AtomicInteger();
        capture(() -> result.set(Report_base.readLimit(new Scanner("  7  \n"))));
        assertEquals(7, result.get());
    }

    @Test
    void readLimitRejectsZeroAndNegativeNumbers() throws Exception {
        AtomicInteger zero = new AtomicInteger(-1);
        AtomicInteger negative = new AtomicInteger(-1);
        String output = capture(() -> {
            zero.set(Report_base.readLimit(new Scanner("0\n")));
            negative.set(Report_base.readLimit(new Scanner("-3\n")));
        });
        assertEquals(0, zero.get());
        assertEquals(0, negative.get());
        assertEquals(2, count(output, "N must be greater than zero."));
    }

    @Test
    void readLimitRejectsTextAndEmptyInput() throws Exception {
        AtomicInteger text = new AtomicInteger(-1);
        AtomicInteger empty = new AtomicInteger(-1);
        String output = capture(() -> {
            text.set(Report_base.readLimit(new Scanner("abc\n")));
            empty.set(Report_base.readLimit(new Scanner("\n")));
        });
        assertEquals(0, text.get());
        assertEquals(0, empty.get());
        assertEquals(2, count(output, "Please enter a valid whole number."));
    }

    // ------------------------------------------------------------------
    // getCountryPopulation
    // ------------------------------------------------------------------

    @Test
    void getCountryPopulationReturnsThePopulationWhenFound() throws SQLException {
        MockDb db = MockDb.empty();
        when(db.results.next()).thenReturn(true);
        when(db.results.getLong("Population")).thenReturn(67886011L);

        assertEquals(67886011L, Report_base.getCountryPopulation(db.connection, "United Kingdom"));
        verify(db.statement).setString(1, "United Kingdom");
    }

    @Test
    void getCountryPopulationReturnsMinusOneWhenNotFound() throws SQLException {
        MockDb db = MockDb.empty();
        assertEquals(-1, Report_base.getCountryPopulation(db.connection, "Atlantis"));
    }

    // ------------------------------------------------------------------
    // connect
    // ------------------------------------------------------------------

    @Test
    void connectReturnsTheConnectionOnTheFirstAttempt() throws Exception {
        Connection expected = mock(Connection.class);
        fake.connection = expected;

        String output = capture(() -> assertSame(expected, Report_base.connect("localhost:1", 0)));

        assertTrue(output.contains("Connected to MySQL."));
        assertEquals(1, fake.connectCalls);
    }

    @Test
    void connectUsesTheLocationForTheWorldDatabase() throws Exception {
        fake.connection = mock(Connection.class);
        capture(() -> Report_base.connect("db:3306", 0));
        assertTrue(fake.lastUrl.startsWith("jdbc:mysql://db:3306/world"));
    }

    @Test
    void connectKeepsRetryingUntilTheDatabaseIsReady() throws Exception {
        fake.connection = mock(Connection.class);
        fake.failuresBeforeSuccess = 2;

        String output = capture(() -> Report_base.connect("localhost:1", 0));

        assertTrue(output.contains("Connection attempt 1 failed."));
        assertTrue(output.contains("Connection attempt 2 failed."));
        assertFalse(output.contains("Connection attempt 3 failed."));
        assertTrue(output.contains("Connected to MySQL."));
        assertEquals(3, fake.connectCalls);
    }

    @Test
    void connectGivesUpAfter30AttemptsWithTheLastError() throws Exception {
        fake.failuresBeforeSuccess = 1000;

        String output = capture(() ->
                assertThrows(SQLException.class, () -> Report_base.connect("localhost:1", 0)));

        assertTrue(output.contains("Connection attempt 30 failed."));
        assertFalse(output.contains("Connection attempt 31 failed."));
        assertEquals(30, fake.connectCalls);
    }

    // ------------------------------------------------------------------
    // main (runs every report, using the fake driver and a mocked connection)
    // ------------------------------------------------------------------

    @Test
    void mainRunsAll32ReportsAndClosesTheConnection() throws Exception {
        MockDb db = MockDb.empty();
        fake.connection = db.connection;

        String output = capture(() -> Report_base.main(new String[]{"localhost:1", "0", "5"}));

        verify(db.connection, times(32)).prepareStatement(anyString());
        verify(db.connection).close();
        assertTrue(output.contains("Connected to MySQL."));
        assertTrue(output.contains("Database connection closed."));
        assertFalse(output.contains("Could not generate report"));
    }

    @Test
    void mainPrintsAHeadingForEveryKindOfReport() throws Exception {
        MockDb db = MockDb.empty();
        fake.connection = db.connection;

        String output = capture(() -> Report_base.main(new String[]{"localhost:1", "0", "5"}));

        assertTrue(output.contains("All countries in the world"));
        assertTrue(output.contains("All countries in Asia"));
        assertTrue(output.contains("All countries in Caribbean"));
        assertTrue(output.contains("All cities in United Kingdom"));
        assertTrue(output.contains("All cities in Scotland"));
        assertTrue(output.contains("All capital cities in the world"));
        assertTrue(output.contains("Top 5 countries in the world"));
        assertTrue(output.contains("Top 5 cities in Scotland"));
        assertTrue(output.contains("Top 5 capital cities in Caribbean"));
        assertTrue(output.contains("Population of each continent"));
        assertTrue(output.contains("Population of each region"));
        assertTrue(output.contains("Population of each country"));
        assertTrue(output.contains("Population of the world"));
        assertTrue(output.contains("Population of Edinburgh"));
        assertTrue(output.contains("Language speakers"));
    }

    @Test
    void mainBuildsTheExpectedSqlForEveryReport() throws Exception {
        MockDb db = MockDb.empty();
        fake.connection = db.connection;

        capture(() -> Report_base.main(new String[]{"localhost:1", "0", "5"}));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(db.connection, times(32)).prepareStatement(captor.capture());
        List<String> sql = captor.getAllValues();

        // countries (0-5)
        assertSql(sql.get(0), "FROM country LEFT JOIN city AS capital", "ORDER BY country.Population DESC");
        assertFalse(sql.get(0).contains("WHERE"));
        assertSql(sql.get(1), "WHERE country.Continent = ?");
        assertSql(sql.get(2), "WHERE country.Region = ?");
        assertSql(sql.get(3), "LIMIT ?");
        assertSql(sql.get(4), "WHERE country.Continent = ?", "LIMIT ?");
        assertSql(sql.get(5), "WHERE country.Region = ?", "LIMIT ?");

        // cities (6-15)
        assertSql(sql.get(6), "FROM city JOIN country", "ORDER BY city.Population DESC");
        assertFalse(sql.get(6).contains("WHERE"));
        assertSql(sql.get(7), "WHERE country.Continent = ?");
        assertSql(sql.get(8), "WHERE country.Region = ?");
        assertSql(sql.get(9), "WHERE country.Name = ?");
        assertSql(sql.get(10), "WHERE city.District = ?");
        assertSql(sql.get(11), "LIMIT ?");
        assertSql(sql.get(12), "WHERE country.Continent = ?", "LIMIT ?");
        assertSql(sql.get(13), "WHERE country.Region = ?", "LIMIT ?");
        assertSql(sql.get(14), "WHERE country.Name = ?", "LIMIT ?");
        assertSql(sql.get(15), "WHERE city.District = ?", "LIMIT ?");

        // capital cities (16-21)
        assertSql(sql.get(16), "country.Capital = city.ID", "ORDER BY city.Population DESC");
        assertFalse(sql.get(16).contains("WHERE"));
        assertSql(sql.get(17), "WHERE country.Continent = ?");
        assertSql(sql.get(18), "WHERE country.Region = ?");
        assertSql(sql.get(19), "LIMIT ?");
        assertSql(sql.get(20), "WHERE country.Continent = ?", "LIMIT ?");
        assertSql(sql.get(21), "WHERE country.Region = ?", "LIMIT ?");

        // people in cities / not in cities (22-24)
        assertSql(sql.get(22), "GROUP BY country.Continent", "AS InCities", "AS NotInCities");
        assertSql(sql.get(23), "GROUP BY country.Region");
        assertSql(sql.get(24), "GROUP BY country.Name");

        // population numbers (25-30)
        assertEquals("SELECT SUM(Population) AS Population FROM country", sql.get(25));
        assertSql(sql.get(26), "WHERE Continent = ?");
        assertSql(sql.get(27), "WHERE Region = ?");
        assertSql(sql.get(28), "FROM country WHERE Name = ?");
        assertSql(sql.get(29), "FROM city WHERE District = ?");
        assertSql(sql.get(30), "FROM city WHERE Name = ?");

        // languages (31)
        assertSql(sql.get(31), "'Chinese', 'English', 'Hindi', 'Spanish', 'Arabic'",
                "AS Speakers", "AS WorldPercentage", "ORDER BY Speakers DESC");
    }

    @Test
    void mainUsesTheSamplePlacesAndN() throws Exception {
        MockDb db = MockDb.empty();
        fake.connection = db.connection;

        capture(() -> Report_base.main(new String[]{"localhost:1", "0", "5"}));

        verify(db.statement, atLeastOnce()).setObject(1, "Asia");
        verify(db.statement, atLeastOnce()).setObject(1, "Caribbean");
        verify(db.statement, atLeastOnce()).setObject(1, "United Kingdom");
        verify(db.statement, atLeastOnce()).setObject(1, "Scotland");
        verify(db.statement, atLeastOnce()).setObject(1, "Edinburgh");
        verify(db.statement, atLeastOnce()).setObject(1, 5);
        verify(db.statement, atLeastOnce()).setObject(2, 5);
    }

    @Test
    void mainUsesTheNPassedAsTheThirdArgument() throws Exception {
        MockDb db = MockDb.empty();
        fake.connection = db.connection;

        String output = capture(() -> Report_base.main(new String[]{"localhost:1", "0", "12"}));

        assertTrue(output.contains("Top 12 countries in the world"));
        assertTrue(output.contains("Top 12 capital cities in Caribbean"));
        verify(db.statement, atLeastOnce()).setObject(1, 12);
        verify(db.statement, atLeastOnce()).setObject(2, 12);
    }

    @Test
    void mainUsesTheLocationPassedAsTheFirstArgument() throws Exception {
        fake.connection = MockDb.empty().connection;

        capture(() -> Report_base.main(new String[]{"db:3306", "0"}));

        assertTrue(fake.lastUrl.startsWith("jdbc:mysql://db:3306/world"));
    }

    @Test
    void mainWorksWithNoArguments() throws Exception {
        // uses the default location and the default 2 second delay, so this test takes about 2 seconds
        MockDb db = MockDb.empty();
        fake.connection = db.connection;

        String output = capture(() -> Report_base.main(new String[]{}));

        verify(db.connection, times(32)).prepareStatement(anyString());
        assertTrue(output.contains("Top 5 countries in the world"));
    }

    @Test
    void runKeepsGoingAndClosesTheConnectionWhenReportsFail() throws Exception {
        Connection connection = mock(Connection.class);
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("boom"));
        fake.connection = connection;
        AtomicInteger failures = new AtomicInteger();

        String output = capture(() -> failures.set(Report_base.run(new String[]{"localhost:1", "0", "5"})));

        assertEquals(32, failures.get());
        assertEquals(32, count(output, "Could not generate report: boom"));
        assertTrue(output.contains("Language speakers"));
        verify(connection).close();
        assertTrue(output.contains("Database connection closed."));
    }

    @Test
    void mainRejectsADelayThatIsNotANumber() {
        assertThrows(NumberFormatException.class,
                () -> Report_base.main(new String[]{"localhost:1", "abc"}));
        assertEquals(0, fake.connectCalls);
    }

    @Test
    void mainRejectsAnNThatIsNotANumber() {
        assertThrows(NumberFormatException.class,
                () -> Report_base.main(new String[]{"localhost:1", "0", "abc"}));
        assertEquals(0, fake.connectCalls);
    }

    @Test
    void mainGivesUpWhenTheDatabaseNeverComesUp() throws Exception {
        fake.failuresBeforeSuccess = 1000;

        capture(() -> assertThrows(SQLException.class,
                () -> Report_base.main(new String[]{"localhost:1", "0"})));

        assertEquals(30, fake.connectCalls);
    }

    // ------------------------------------------------------------------
    // run / runAllReports
    // ------------------------------------------------------------------

    @Test
    void runReturnsZeroWhenEveryReportWorks() throws Exception {
        MockDb db = MockDb.empty();
        fake.connection = db.connection;
        AtomicInteger failures = new AtomicInteger(-1);

        capture(() -> failures.set(Report_base.run(new String[]{"localhost:1", "0", "5"})));

        assertEquals(0, failures.get());
    }

    @Test
    void runRejectsAnNThatIsNotAboveZero() {
        assertThrows(IllegalArgumentException.class,
                () -> Report_base.run(new String[]{"localhost:1", "0", "0"}));
        assertThrows(IllegalArgumentException.class,
                () -> Report_base.run(new String[]{"localhost:1", "0", "-3"}));
        assertEquals(0, fake.connectCalls);
    }

    @Test
    void runAllReportsRuns32ReportsToTheGivenStream() throws Exception {
        MockDb db = MockDb.empty();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int failures = Report_base.runAllReports(db.connection,
                new PrintStream(buffer, true, StandardCharsets.UTF_8), 7);

        String output = buffer.toString(StandardCharsets.UTF_8);
        assertEquals(0, failures);
        verify(db.connection, times(32)).prepareStatement(anyString());
        assertTrue(output.contains("Top 7 countries in the world"));
        assertTrue(output.contains("Language speakers"));
    }

    @Test
    void runAllReportsCountsOnlyTheReportsThatFail() throws Exception {
        MockDb db = MockDb.empty();
        when(db.connection.prepareStatement(contains("countrylanguage")))
                .thenThrow(new SQLException("bad language report"));
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int failures = Report_base.runAllReports(db.connection,
                new PrintStream(buffer, true, StandardCharsets.UTF_8), 5);

        assertEquals(1, failures);
        assertTrue(buffer.toString(StandardCharsets.UTF_8).contains("Could not generate report: bad language report"));
    }

    // ------------------------------------------------------------------
    // printReport (stream version), formatting and URL
    // ------------------------------------------------------------------

    @Test
    void printReportWritesToTheStreamItIsGivenAndReturnsTrue() throws Exception {
        MockDb db = new MockDb(new String[]{"Name"}, new String[][]{{"Paris"}});
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        boolean ok = Report_base.printReport(db.connection,
                new PrintStream(buffer, true, StandardCharsets.UTF_8), "A title", "SELECT 1");

        String output = buffer.toString(StandardCharsets.UTF_8);
        assertTrue(ok);
        assertTrue(output.contains("A title"));
        assertTrue(output.contains("Paris"));
    }

    @Test
    void printReportReturnsFalseWhenTheDatabaseFails() throws Exception {
        Connection connection = mock(Connection.class);
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("boom"));
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        boolean ok = Report_base.printReport(connection,
                new PrintStream(buffer, true, StandardCharsets.UTF_8), "T", "SELECT 1");

        assertFalse(ok);
        assertTrue(buffer.toString(StandardCharsets.UTF_8).contains("Could not generate report: boom"));
    }

    @Test
    void formatHeaderJoinsTheColumnLabels() throws SQLException {
        ResultSetMetaData metadata = mock(ResultSetMetaData.class);
        when(metadata.getColumnCount()).thenReturn(3);
        when(metadata.getColumnLabel(1)).thenReturn("Code");
        when(metadata.getColumnLabel(2)).thenReturn("Name");
        when(metadata.getColumnLabel(3)).thenReturn("Population");
        assertEquals("Code, Name, Population", Report_base.formatHeader(metadata));
    }

    @Test
    void formatRowJoinsTheValuesAndShowsMissingOnesAsNULL() throws SQLException {
        ResultSet results = mock(ResultSet.class);
        when(results.getString(1)).thenReturn("GBR");
        when(results.getString(2)).thenReturn(null);
        assertEquals("GBR, NULL", Report_base.formatRow(results, 2));
    }

    @Test
    void buildUrlPointsAtTheWorldDatabase() {
        assertTrue(Report_base.buildUrl("db:3306").startsWith("jdbc:mysql://db:3306/world"));
    }
}