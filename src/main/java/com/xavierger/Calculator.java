package com.xavierger;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

public class Calculator {

    private final MethodHandle sumaHandle;
    private final MethodHandle sumaArrayHandle;
    private final MethodHandle duplicarArrayHandle;

    public Calculator() {

        // Busca la funció suma en el fitxer natiu libcalcul.dylib
        SymbolLookup library = SymbolLookup.libraryLookup(
                "native/libcalcul.dylib",
                Arena.global()
        );
        Linker linker = Linker.nativeLinker();

        // suma(double, double)

        // Representa una adreça de memòria nativa on es troba la funció suma
        MemorySegment sumaAddress = library.find("suma")
                .orElseThrow();

        FunctionDescriptor sumaDescriptor =
                FunctionDescriptor.of(
                        ValueLayout.JAVA_DOUBLE,
                        ValueLayout.JAVA_DOUBLE,
                        ValueLayout.JAVA_DOUBLE
                );

        sumaHandle = linker.downcallHandle(
                sumaAddress,
                sumaDescriptor
        );

        // suma_array(double*, int)

        MemorySegment sumaArrayAddress = library.find("suma_array")
                .orElseThrow();

        FunctionDescriptor sumaArrayDescriptor =
                FunctionDescriptor.of(
                        ValueLayout.JAVA_DOUBLE,
                        ValueLayout.ADDRESS,
                        ValueLayout.JAVA_INT
                );

        sumaArrayHandle = linker.downcallHandle(
                sumaArrayAddress,
                sumaArrayDescriptor
        );

        // duplicar_array(double*, int)
        MemorySegment duplicarArrayAddress = library.find("duplicar_array")
                .orElseThrow();

        FunctionDescriptor duplicarArrayDescriptor =
                FunctionDescriptor.ofVoid(
                        ValueLayout.ADDRESS,
                        ValueLayout.JAVA_INT
                );

        duplicarArrayHandle = linker.downcallHandle(
                duplicarArrayAddress,
                duplicarArrayDescriptor
        );
    }

    public double suma(double a, double b) throws Throwable {
        return (double) sumaHandle.invokeExact(a, b);
    }

    public double sumaArray(double[] valores) throws Throwable {

        try (Arena arena = Arena.ofConfined()) {

            /* Zona de memòria nativa accessible per a C
               Això eserva espai natiu per: 4 × 8 bytes = 32 bytes
            Java heap                    Memòria nativa
            double[]                     MemorySegment
            ┌──────────┐                 ┌──────────┐
            │   1.0    │ ──────────────► │   1.0    │
            │   2.0    │                 │   2.0    │
            │   3.0    │                 │   3.0    │
            │   4.0    │                 │   4.0    │
            └──────────┘                 └──────────┘
             */
            MemorySegment memoria = arena.allocate(
                    ValueLayout.JAVA_DOUBLE,
                    valores.length
            );

            memoria.copyFrom(
                    MemorySegment.ofArray(valores)
            );

            return (double) sumaArrayHandle.invokeExact(
                    memoria,
                    (int) valores.length
            );
        }
    }

    public void duplicarArray(double[] valores) throws Throwable {

        try (Arena arena = Arena.ofConfined()) {

            MemorySegment memoria = arena.allocate(
                    ValueLayout.JAVA_DOUBLE,
                    valores.length
            );

            memoria.copyFrom(
                    MemorySegment.ofArray(valores)
            );

            duplicarArrayHandle.invokeExact(
                    memoria,
                    (int) valores.length
            );

            MemorySegment.ofArray(valores)
                    .copyFrom(memoria);
        }
    }
}
