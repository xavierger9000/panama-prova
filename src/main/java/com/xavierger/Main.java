package com.xavierger;

public class Main {

    public static void main(String[] args) throws Throwable {

        Calculadora calculadora = new Calculadora();

        double resultat = calculadora.suma(2.5, 3.7);

        System.out.println(resultat);
    }
}