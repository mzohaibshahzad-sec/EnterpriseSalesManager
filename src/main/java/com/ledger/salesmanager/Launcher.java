package com.ledger.salesmanager;

/**
 * Separate entry point that does NOT extend javafx.application.Application.
 * Needed so packaged jars/exes launched via "java -jar" or launch4j don't
 * trigger the "JavaFX runtime components are missing" check, which the
 * JVM applies specifically when the Main-Class itself extends Application.
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}