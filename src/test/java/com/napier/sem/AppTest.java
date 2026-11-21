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





}