package com.napier.devops;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class AppTest
{


    static App app;

    @BeforeAll
    static void init()
    {
        app = new App();
    }

    //Tests for Salaries
    @Test
    void printSalariesTestNull()
    {
        app.printSalaries(null);
    }

    @Test
    void printSalariesTestEmpty()
    {
        ArrayList<Employee> employess = new ArrayList<Employee>();
        app.printSalaries(employess);
    }

    @Test
    void printSalariesTestContainsNull()
    {
        ArrayList<Employee> employess = new ArrayList<Employee>();
        employess.add(null);
        app.printSalaries(employess);
    }

    //Tests for Employees
    @Test
    void displayEmployeeTestNull()
    {
        app.displayEmployee(null);
    }

    @Test
    void displayEmployeeTestValid()
    {
        Employee emp = new Employee();
        emp.emp_no = 1;
        emp.first_name = "Nikodem";
        emp.last_name = "Trojnar";
        emp.salary = 30000;

        app.displayEmployee(emp);
    }

}