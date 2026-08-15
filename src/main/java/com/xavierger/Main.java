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
    }
}