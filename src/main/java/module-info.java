/**
 * Módulo do jogo Battleship (jogo JavaFX + servidor REST Spring Boot).
 *
 * É declarado como "open" para que TODAS as bibliotecas que usam reflexão
 * (JUnit, Spring, Jackson, JavaFX) consigam aceder aos pacotes
 * "battleship" e "restserver", mesmo aos membros privados / package-private.
 */
open module battleship {
    // JavaFX
    requires javafx.controls;
    requires javafx.graphics;

    // Jackson
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;

    // Spring
    requires spring.context;
    requires spring.web;
    requires spring.boot;
    requires spring.boot.autoconfigure;

    // Logging / anotações / áudio
    requires org.apache.logging.log4j;
    requires org.jetbrains.annotations;
    requires audiocue;

    // JUnit (só necessário para compilar/executar os testes; ver scope 'provided' no pom.xml)
    requires static org.junit.jupiter.api;

    // Módulos do JDK (usados pelo código ou pelo Spring/Tomcat em runtime)
    requires java.desktop;      // javax.sound.sampled
    requires java.net.http;     // HttpClient (CallbackClient)
    requires java.logging;
    requires java.management;
    requires java.naming;
    requires java.sql;
    requires jdk.unsupported;

    exports battleship;
    exports restserver;
}
