# Panama Prova - Java Foreign Function & Memory (FFM) API Demo

A minimal Java and Gradle project demonstrating how to invoke native C code from Java using the **Foreign Function & Memory (FFM) API** (finalized in Java 22 via JEP 454 as part of **Project Panama**).

This project showcases how to replace traditional JNI (Java Native Interface) with the modern, safer, and higher-performance FFM API.

---

## Features & Concepts Demonstrated

- **Symbol Lookup (`SymbolLookup`)**: Loading a dynamic shared library (`libcalcul.dylib`) and resolving native function addresses (`suma`, `suma_array`).
- **Downcall Method Handles (`Linker`, `FunctionDescriptor`)**: Creating type-safe `MethodHandle` instances that bridge Java calls to native function pointers.
- **Off-Heap Memory Management (`Arena`, `MemorySegment`)**:
  - Allocating off-heap native memory safely using confined arenas (`Arena.ofConfined()`).
  - Copying data between Java heap arrays (`double[]`) and native memory segments (`MemorySegment`).
- **Data Types & Layouts (`ValueLayout`)**: Mapping native C types such as `double`, `int`, and pointers (`double*`) using `ValueLayout.JAVA_DOUBLE`, `ValueLayout.JAVA_INT`, and `ValueLayout.ADDRESS`.

---

## Project Structure

```text
panama-prova/
├── build.gradle               # Gradle build configuration
├── settings.gradle            # Gradle project settings
├── native/
│   ├── calcul.h               # C header declaring native functions
│   ├── calcul.c               # C implementation (suma, suma_array)
│   └── libcalcul.dylib        # Compiled native shared library (macOS)
└── src/
    └── main/
        └── java/
            └── com/xavierger/
                ├── Calculator.java   # Java wrapper binding FFM API to C functions
                └── Main.java         # Entry point demonstrating scalar and array additions
```

---

## Native Functions (C)

The native library in `native/calcul.c` exposes three functions:

```c
// Adds two double-precision floating-point numbers
double suma(double a, double b);

// Sums an array of double-precision numbers of length n
double suma_array(double *valores, int n);

// Duplicates each element in an array in-place
void duplicar_array(double *valores, int n);
```

---

## Prerequisites

- **Java Development Kit (JDK)**: JDK 22 or newer (with FFM API finalized in `java.lang.foreign`).
- **C Compiler**: `clang` (macOS / Xcode command line tools) or `gcc` (Linux).
- **Gradle**: Included via Gradle Wrapper (`./gradlew`).

---

## Building and Running

### 1. Compile the Native C Library

If `libcalcul.dylib` is not already compiled (or if you modify `native/calcul.c`), compile it into a shared library:

#### macOS:
```bash
clang -dynamiclib -o native/libcalcul.dylib native/calcul.c
```

#### Linux (`.so`):
```bash
gcc -shared -fPIC -o native/libcalcul.so native/calcul.c
```
*(Note: If running on Linux or Windows, update the library path/extension in `Calculator.java` accordingly).*

### 2. Run the Java Application

You can run the application directly using `java`:

```bash
# Compile Java sources
./gradlew compileJava

# Run the Main class (enable native access for FFM API)
java --enable-native-access=ALL-UNNAMED -cp build/classes/java/main com.xavierger.Main
```

Alternatively, open and run `Main.java` in an IDE like **IntelliJ IDEA** (ensure the project SDK is configured to Java 22+).

---

## Expected Output

When running `Main.java`, the output will be:

```text
6.2
10.0
Abans:
1.0
2.0
3.0
4.0
Després:
2.0
4.0
6.0
8.0
```

- `6.2`: Result of `calculator.suma(2.5, 3.7)`
- `10.0`: Result of `calculator.sumaArray(...)`
- `Abans` / `Després`: Output before and after `calculator.duplicarArray(...)` modifies the array in C and syncs back to Java.

---

## How It Works (FFM API Walkthrough)

### 1. Binding a Scalar Function (`suma`)

```java
// 1. Locate the native function address
SymbolLookup library = SymbolLookup.libraryLookup("native/libcalcul.dylib", Arena.global());
MemorySegment sumaAddress = library.find("suma").orElseThrow();

// 2. Define function descriptor: (double, double) -> double
FunctionDescriptor sumaDescriptor = FunctionDescriptor.of(
    ValueLayout.JAVA_DOUBLE,
    ValueLayout.JAVA_DOUBLE,
    ValueLayout.JAVA_DOUBLE
);

// 3. Create downcall MethodHandle
MethodHandle sumaHandle = Linker.nativeLinker().downcallHandle(sumaAddress, sumaDescriptor);

// 4. Invoke
double result = (double) sumaHandle.invokeExact(2.5, 3.7);
```

### 2. Passing Native Memory / Arrays (`suma_array`)

```java
// 1. Define function descriptor: (double*, int) -> double
FunctionDescriptor sumaArrayDescriptor = FunctionDescriptor.of(
    ValueLayout.JAVA_DOUBLE,
    ValueLayout.ADDRESS,
    ValueLayout.JAVA_INT
);
MethodHandle sumaArrayHandle = Linker.nativeLinker().downcallHandle(sumaArrayAddress, sumaArrayDescriptor);

// 2. Allocate native memory in a confined arena and copy heap array
try (Arena arena = Arena.ofConfined()) {
    MemorySegment memoria = arena.allocate(ValueLayout.JAVA_DOUBLE, valores.length);
    memoria.copyFrom(MemorySegment.ofArray(valores));

    double sum = (double) sumaArrayHandle.invokeExact(memoria, (int) valores.length);
}
// Native memory is automatically freed when exiting the try-with-resources block!
```
### 3. Modifying Arrays in C and Reflecting Changes in Java (`duplicar_array`)

When native C code modifies an array in-place, the mutation occurs directly within off-heap native memory. Because Project Panama maintains a strict separation between Java's garbage-collected heap and off-heap native memory, changes are not automatically reflected in Java heap arrays.

#### C Implementation (`native/calcul.c`)

```c
void duplicar_array(double *valores, int n) {
    for (int i = 0; i < n; i++) {
        valores[i] = valores[i] * 2.0;
    }
}
```

#### Java Implementation (`Calculator.java`)

```java
try (Arena arena = Arena.ofConfined()) {
    // 1. Allocate native memory off-heap
    MemorySegment memoria = arena.allocate(ValueLayout.JAVA_DOUBLE, valores.length);

    // 2. Copy initial values from Java heap array to native memory
    memoria.copyFrom(MemorySegment.ofArray(valores));

    // 3. Invoke native function to modify native memory in-place
    duplicarArrayHandle.invokeExact(memoria, (int) valores.length);

    // 4. Copy the modified data from native memory back to the Java heap array
    MemorySegment.ofArray(valores).copyFrom(memoria);
}
```

#### Memory Synchronization Flow

```text
Java Heap (double[])
┌─────┐
│ 1.0 │
│ 2.0 │
│ 3.0 │
│ 4.0 │
└─────┘
   │
   │ 1. copyFrom (Heap -> Native)
   ▼
Native Memory (MemorySegment)
┌─────┐
│ 1.0 │
│ 2.0 │
│ 3.0 │
│ 4.0 │
└─────┘
   │
   │ 2. C modifies values in-place (* 2.0)
   ▼
Native Memory (MemorySegment)
┌─────┐
│ 2.0 │
│ 4.0 │
│ 6.0 │
│ 8.0 │
└─────┘
   │
   │ 3. copyFrom (Native -> Heap)
   ▼
Java Heap (double[])
┌─────┐
│ 2.0 │
│ 4.0 │
│ 6.0 │
│ 8.0 │
└─────┘
```

> **Key Concept:** Project Panama does not automatically synchronize Java heap arrays with native memory. To mutate an array in native code and see the resulting changes in Java, two explicit copy operations are required:
> 1. **Heap to Native:** Copy the Java `double[]` into the native `MemorySegment` before calling the C function.
> 2. **Native to Heap:** Copy the updated `MemorySegment` back into the Java `double[]` after the C function completes.