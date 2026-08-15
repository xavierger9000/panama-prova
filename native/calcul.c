#include "calcul.h"

double suma(double a, double b) {
    return a + b;
}

double suma_array(double *valores, int n) {
    double suma = 0.0;

    for (int i = 0; i < n; i++) {
        suma += valores[i];
    }

    return suma;
}

void duplicar_array(double *valores, int n) {
    for (int i = 0; i < n; i++) {
        valores[i] = valores[i] * 2.0;
    }
}