package com.napier.devops;

import com.napier.sem.Report_base;

import org.junit.jupiter.api.Test;

import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AppTest {

    // Supply input without needing someone to type in the console.
    // This calls the actual method in Report_base.
    private int readLimitFrom(String input) {
        try (Scanner scanner = new Scanner(input + "\n")) {
            return Report_base.readLimit(scanner);
        }
    }

    @Test
    void readLimitAcceptsPositiveNumber() {
        assertEquals(5, readLimitFrom("5"));
    }

    @Test
    void readLimitAcceptsOne() {
        // One is the smallest valid limit.
        assertEquals(1, readLimitFrom("1"));
    }

    @Test
    void readLimitAcceptsSurroundingSpaces() {
        assertEquals(10, readLimitFrom("  10  "));
    }

    @Test
    void readLimitAcceptsLargestInteger() {
        // Largest value supported by the method's int return type.
        assertEquals(Integer.MAX_VALUE,
                readLimitFrom("2147483647"));
    }

    @Test
    void readLimitRejectsZero() {
        assertEquals(0, readLimitFrom("0"));
    }

    @Test
    void readLimitRejectsNegativeNumber() {
        assertEquals(0, readLimitFrom("-5"));
    }

    @Test
    void readLimitRejectsDecimalNumber() {
        assertEquals(0, readLimitFrom("2.5"));
    }

    @Test
    void readLimitRejectsText() {
        assertEquals(0, readLimitFrom("hello"));
    }

    @Test
    void readLimitRejectsEmptyInput() {
        assertEquals(0, readLimitFrom(""));
    }

    @Test
    void readLimitRejectsNumberLargerThanInteger() {
        // One above the largest supported integer.
        assertEquals(0, readLimitFrom("2147483648"));
    }
}