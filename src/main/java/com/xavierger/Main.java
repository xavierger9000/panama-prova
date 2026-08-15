package com.xavierger;

public class Main {

    static void main() throws Throwable {

        Calculadora calculadora = new Calculadora();

        double result = calculadora.suma(2.5, 3.7);

        IO.println(result);

        double[] valors = {
                1.0,
                2.0,
                3.0,
                4.0
        };

        double suma = calculadora.sumaArray(valors);

        IO.println(suma);
    }
}