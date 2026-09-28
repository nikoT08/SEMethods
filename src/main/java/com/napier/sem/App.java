package com.napier.sem;
import java.util.Scanner;
import com.github.lalyos.jfiglet.FigletFont;
import java.io.IOException;
import java.util.concurrent.TimeUnit;



public class App {

   static Scanner inputreader = new Scanner(System.in);

    public static void main(String[] args) {




        consoleUiNew();
        typewrite(220);
        asciiPrint("Population");
        typewrite(220);


        asciiPrint("DataBase");
        consoleUiNew();
        displayReportTypes();
        retrunUserInput("Select report type  ");
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

