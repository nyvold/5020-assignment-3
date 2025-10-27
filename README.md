# 5020-assignment-3

## Overview
This repository contains the Java code for simulating the Chord distributed hash table protocol. The entry point is `Simulator`, which builds a peer-to-peer network, configures the Chord protocol, and launches the lookup simulation.

## Requirements
- Java Development Kit (JDK) 11 or newer
- macOS/Linux shell (commands below use `bash`)

## Build
Compile every Java source under `src/` into the `out/` directory (create it once if it does not exist):

```bash
mkdir -p out
javac -d out $(find src -name '*.java')
```

## Run
Launch the simulator by passing the node count and the `m` value (identifier length) required by Chord:

```bash
java -cp out Simulator <nodeCount> <m>
```

Example:

```bash
java -cp out Simulator 10 10
java -cp out Simulator 100 20
java -cp out Simulator 1000 20
```

If you modify source files, rerun the build command before launching the simulator again to pick up the changes.
