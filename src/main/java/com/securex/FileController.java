package com.securex;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

@RestController
public class FileController {

    // ---------- Single file encrypt ----------
    @PostMapping("/encrypt")
    public ResponseEntity<byte[]> encryptFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("password") String password) throws Exception {

        byte[] fileData = file.getBytes();
        byte[] encrypted = AESUtil.encrypt(fileData, password);
        String fileName = file.getOriginalFilename() + ".enc";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(encrypted);
    }

    // ---------- Single file decrypt ----------
    @PostMapping("/decrypt")
    public ResponseEntity<byte[]> decryptFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("password") String password) throws Exception {

        byte[] encrypted = file.getBytes();
        byte[] decrypted = AESUtil.decrypt(encrypted, password);
        String fileName = file.getOriginalFilename().replace(".enc", "");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(decrypted);
    }

    // ---------- Batch encrypt/decrypt (sequential vs parallel) ----------
    @PostMapping("/process-batch")
    public ResponseEntity<String> processBatch(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("password") String password,
            @RequestParam(value = "encrypt", defaultValue = "true") boolean encrypt,
            @RequestParam(value = "mode", defaultValue = "parallel") String mode) throws Exception {

        // Save uploaded files to a temp working directory
        List<File> tempFiles = new ArrayList<>();
        for (MultipartFile mf : files) {
            File temp = File.createTempFile("securex_", "_" + mf.getOriginalFilename());
            mf.transferTo(temp);
            tempFiles.add(temp);
        }

        long start = System.nanoTime();

        if (mode.equalsIgnoreCase("parallel")) {
            int threads = Math.min(Runtime.getRuntime().availableProcessors(), tempFiles.size());
            ParallelFileEncryptor encryptor = new ParallelFileEncryptor(Math.max(threads, 1));

            List<Future<File>> futures = encryptor.process(tempFiles, password, encrypt);
            for (Future<File> f : futures) {
                f.get(); // wait for completion / surface exceptions
            }
            encryptor.shutdown();
        } else {
            new SequentialFileEncryptor().process(tempFiles, password, encrypt);
        }

        long durationMs = (System.nanoTime() - start) / 1_000_000;

        String action = encrypt ? "Encrypted" : "Decrypted";
        String result = String.format(
                "%s %d file(s) in %d ms using %s mode (%d thread(s))",
                action,
                files.length,
                durationMs,
                mode,
                mode.equalsIgnoreCase("parallel")
                        ? Math.min(Runtime.getRuntime().availableProcessors(), tempFiles.size())
                        : 1
        );

        return ResponseEntity.ok(result);
    }
}
