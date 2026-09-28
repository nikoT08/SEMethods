package com.napier.sem;

// Used to read input from the keyboard/console
import java.util.Scanner;

// Used to create ASCII/text-based figures
import com.github.lalyos.jfiglet.FigletFont;

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
        asciiPrint("Population");

       // Add a short delay displaying the next section.
        typewrite(220);


       // Display the Database heading using ASCII art.
        asciiPrint("DataBase");

       // Display a seperator to improve the readability of the interface.
        consoleUiNew();

       // Display the available report types to the user.
        displayReportTypes();

       // Ask the user to select the report type they require.
        retrunUserInput("Select report type  ");

       // Display a seperator after the user's selection.
        consoleUiNew();





    }








    static void consoleUiNew()
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

    static void asciiPrint(String txt)
    {
        try {
            // This converts your string into large ASCII art
            String asciiArt = FigletFont.convertOneLine(txt);
            System.out.println(asciiArt);


        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    static void typewrite( int delayMillis) {


            try {
                TimeUnit.MILLISECONDS.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();

            }

    }

    static void displayReportTypes()
    {
        System.out.println("Country[1] City[2] Population[3] Custom[4]");
    }

}

