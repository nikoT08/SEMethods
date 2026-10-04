package com.napier.sem;

// Used to read input from the keyboard/console
import java.util.Scanner;

// Used to create ASCII/text-based figures


// Used when working with input/output operations that may cause errors
import java.io.IOException;

// Used for adding time delays to the program
import java.util.concurrent.TimeUnit;



public class App {

   // Scanner used to read user input from hte console
   static Scanner inputreader = new Scanner(System.in);

    public static void main(String[] args) {




       // Display a seperator before the mainsections of the application.
        consoleUiNew();

       // Add a short delay to control the presentation of the interface.
        typewrite(220);

       // Display the population heading using ASCII art.


       // Add a short delay displaying the next section.
        typewrite(220);


       // Display the Database heading using ASCII art.

       // Display a seperator to improve the readability of the interface.
        consoleUiNew();

        Report_base.main(args);

       // Display the available report types to the user.
        displayReportTypes();

       // Ask the user to select the report type they require.
       //trunUserInput("Select report type  ");

       // Display a seperator after the user's selection.
       //onsoleUiNew();





    }








    static void consoleUiNew()

   // Print 101 dash character to create a horizontal seperator.
    { for (int i =0; i<= 100; i++)
    {
        System.out.print("-");
    }
    System.out.println(" ");

    }
    static String retrunUserInput(String question)
    {
    System.out.print(question);
    return inputreader.next();


    }



    static void typewrite( int delayMillis) {


            try {
                TimeUnit.MILLISECONDS.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();

            }

    }
    static void selectReportType(String input){


    }

    static void displayReportTypes()
    {
        System.out.println("Country[1] City[2] Population[3] Custom[4]");
    }

}



