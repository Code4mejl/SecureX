# SecureX

SecureX is a Spring Boot web application for encrypting and decrypting files using AES, with both **single-file** and **multithreaded batch processing** modes.

## Features

- **File Encryption / Decryption** — encrypt or decrypt a single file via REST API.
- **Batch Processing** — encrypt/decrypt multiple files at once.
- **Multithreaded Engine** (`ParallelFileEncryptor`) — uses a fixed `ExecutorService` thread pool to process multiple files concurrently, with memory-mapped I/O (`FileChannel` + `MappedByteBuffer`) for fast file reads/writes.
- **Sequential Engine** (`SequentialFileEncryptor`) — processes files one at a time, useful as a performance baseline.
- **Benchmarking** — the batch endpoint reports processing time so you can compare sequential vs. parallel performance.

## Tech Stack

- Java 17
- Spring Boot 3 (Web)
- `javax.crypto` (AES)
- `java.nio` (FileChannel, MappedByteBuffer)
- `java.util.concurrent` (ExecutorService, Future)
- Maven

## Project Structure

```
SecureX/
├── src/main/java/com/securex/
│   ├── SecureXApplication.java     # Spring Boot entry point
│   ├── FileController.java         # REST endpoints
│   ├── AESUtil.java                # AES encrypt/decrypt logic
│   ├── SequentialFileEncryptor.java# Single-threaded batch processor
│   └── ParallelFileEncryptor.java  # Multithreaded batch processor (thread pool + mmap)
├── src/main/resources/
│   └── application.properties
├── pom.xml
└── .gitignore
```

## Setup & Run

### Prerequisites
- JDK 17 or 21
- Maven

### Build
```bash
mvn clean package
```

### Run
```bash
java -jar target/securex-0.0.1-SNAPSHOT.jar
```

The app starts on port `8081` (or `$PORT` if set, e.g. on Render).

## API Endpoints

### `POST /encrypt`
Encrypts a single file.

| Field | Type | Description |
|---|---|---|
| `file` | File | File to encrypt |
| `password` | String | Password used to derive the AES key |

Returns the encrypted file (`<name>.enc`) as a download.

### `POST /decrypt`
Decrypts a single `.enc` file.

| Field | Type | Description |
|---|---|---|
| `file` | File | `.enc` file to decrypt |
| `password` | String | Password used during encryption |

Returns the original file as a download.

### `POST /process-batch`
Encrypts or decrypts multiple files, sequentially or in parallel.

| Field | Type | Default | Description |
|---|---|---|---|
| `files` | File[] | — | One or more files |
| `password` | String | — | Password used to derive the AES key |
| `encrypt` | boolean | `true` | `true` to encrypt, `false` to decrypt |
| `mode` | String | `parallel` | `parallel` or `sequential` |

Returns a text summary, e.g.:
```
Encrypted 5 file(s) in 42 ms using parallel mode (4 thread(s))
```

### Example (`curl`)

```bash
# Single file encrypt
curl -X POST http://localhost:8081/encrypt \
  -F "file=@document.pdf" \
  -F "password=mypassword" \
  -o document.pdf.enc

# Single file decrypt
curl -X POST http://localhost:8081/decrypt \
  -F "file=@document.pdf.enc" \
  -F "password=mypassword" \
  -o document.pdf

# Batch encrypt, parallel mode
curl -X POST http://localhost:8081/process-batch \
  -F "files=@file1.pdf" \
  -F "files=@file2.pdf" \
  -F "files=@file3.pdf" \
  -F "password=mypassword" \
  -F "encrypt=true" \
  -F "mode=parallel"

# Batch encrypt, sequential mode (for comparison)
curl -X POST http://localhost:8081/process-batch \
  -F "files=@file1.pdf" \
  -F "files=@file2.pdf" \
  -F "files=@file3.pdf" \
  -F "password=mypassword" \
  -F "encrypt=true" \
  -F "mode=sequential"
```

## How the Multithreaded Engine Works

`ParallelFileEncryptor`:
1. Creates a fixed-size thread pool (`Executors.newFixedThreadPool(...)`), sized to the number of available CPU cores.
2. Submits one task per file — each task is independent, so **no shared mutable state and no locks are needed**.
3. Each task:
   - Reads the file via a memory-mapped buffer (`FileChannel.map(...)`).
   - Encrypts/decrypts the bytes with `AESUtil`.
   - Writes the result via another memory-mapped buffer.
4. The controller waits for all `Future`s to complete and reports total elapsed time.

This design lets multiple files be encrypted/decrypted **simultaneously** across CPU cores, significantly reducing total processing time for batches compared to the sequential engine.


