package com.xavierger;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

public class Calculadora {

    private final MethodHandle sumaHandle;

    public Calculadora() {

        // Busca la funció suma en el fitxer natiu libcalcul.dylib
        SymbolLookup library = SymbolLookup.libraryLookup(
                "native/libcalcul.dylib",
                Arena.global()
        );

        // Representa una adreça de memòria nativa on es troba la funció suma
        MemorySegment sumaAddress = library.find("suma")
                .orElseThrow();

        // Descriu la signatura de la funció suma
        FunctionDescriptor descriptor = FunctionDescriptor.of(
                ValueLayout.JAVA_DOUBLE,
                ValueLayout.JAVA_DOUBLE,
                ValueLayout.JAVA_DOUBLE
        );

        Linker linker = Linker.nativeLinker();

        // Crea una handle per a la funció suma, permet fer la crida a la funció suma
        sumaHandle = linker.downcallHandle(
                sumaAddress,
                descriptor
        );
    }

    public double suma(double a, double b) throws Throwable {
        return (double) sumaHandle.invokeExact(a, b);
    }
}
