package com.securex;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Multithreaded file encryption/decryption engine.
 *
 * Uses a fixed thread pool so multiple files are processed concurrently,
 * and memory-mapped I/O (FileChannel + MappedByteBuffer) for fast file
 * reads/writes instead of standard stream-based I/O.
 *
 * Each file is handled independently by its own task, so there is no
 * shared mutable state between threads (no locks required).
 */
public class ParallelFileEncryptor {

    private final ExecutorService threadPool;

    public ParallelFileEncryptor(int threadCount) {
        this.threadPool = Executors.newFixedThreadPool(threadCount);
    }

    /**
     * Submits all files for concurrent processing.
     *
     * @param files     files to encrypt/decrypt
     * @param password  password used to derive the AES key
     * @param encrypt   true = encrypt, false = decrypt
     * @return list of Futures, one per file, resolving to the output File
     */
    public List<Future<File>> process(List<File> files, String password, boolean encrypt) {
        List<Future<File>> results = new ArrayList<>();
        for (File file : files) {
            results.add(threadPool.submit(() -> processSingleFile(file, password, encrypt)));
        }
        return results;
    }

    private File processSingleFile(File inputFile, String password, boolean encrypt) throws Exception {
        byte[] inputData = readWithMemoryMap(inputFile);

        byte[] outputData = encrypt
                ? AESUtil.encrypt(inputData, password)
                : AESUtil.decrypt(inputData, password);

        File outputFile = resolveOutputFile(inputFile, encrypt);
        writeWithMemoryMap(outputFile, outputData);
        return outputFile;
    }

    /**
     * Reads a file fully into memory using a memory-mapped buffer.
     */
    private byte[] readWithMemoryMap(File file) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             FileChannel channel = raf.getChannel()) {

            long size = channel.size();
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, size);

            byte[] data = new byte[(int) size];
            buffer.get(data);
            return data;
        }
    }

    /**
     * Writes byte data to a file using a memory-mapped buffer.
     */
    private void writeWithMemoryMap(File file, byte[] data) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw");
             FileChannel channel = raf.getChannel()) {

            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, data.length);
            buffer.put(data);
        }
    }

    private File resolveOutputFile(File inputFile, boolean encrypt) {
        String path = inputFile.getAbsolutePath();
        if (encrypt) {
            return new File(path + ".enc");
        } else {
            return new File(path.replace(".enc", ""));
        }
    }

    /**
     * Shuts down the thread pool. Call this after all work is submitted
     * and results have been retrieved.
     */
    public void shutdown() {
        threadPool.shutdown();
    }
}
