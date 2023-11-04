package com.example.client;

public class Logger {
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RESET = "\u001B[0m";

    public static void printYellow(String text) {
        System.out.println(ANSI_YELLOW + text + ANSI_RESET);
    }

    public static void printGreen(String text) {
        System.out.println(ANSI_GREEN + text + ANSI_RESET);
    }

    public static void print(String text) {
        System.out.println(text);
    }

}
