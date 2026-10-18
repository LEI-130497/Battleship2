module battleship {
    requires javafx.controls;
    requires javafx.graphics;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires spring.context;
    requires java.net.http;
    requires spring.web;
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires org.apache.logging.log4j;
    requires org.jetbrains.annotations;

    exports battleship;
}