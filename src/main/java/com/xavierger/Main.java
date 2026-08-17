package com.xavierger;

public class Main {

    static void main() throws Throwable {

        Calculator calculator = new Calculator();

        double result = calculator.suma(2.5, 3.7);

        IO.println(result);

        double[] values = {
                1.0,
                2.0,
                3.0,
                4.0
        };

        double sum = calculator.sumaArray(values);

        IO.println(sum);


        System.out.println("Abans:");

        for (double value : values) {
            System.out.println(value);
        }

        calculator.duplicarArray(values);

        System.out.println("Després:");

        for (double valor : values) {
            System.out.println(valor);
        }

        calculator.provarMemoriaNativa();
    }
}