package com.securex;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

public class SequentialFileEncryptor {

    /**
     * Encrypts/decrypts files one after another on a single thread.
     */
    public void process(List<File> files, String password, boolean encrypt) throws Exception {
        for (File file : files) {
            byte[] data = Files.readAllBytes(file.toPath());
            byte[] result = encrypt
                    ? AESUtil.encrypt(data, password)
                    : AESUtil.decrypt(data, password);

            File outFile = resolveOutputFile(file, encrypt);
            Files.write(outFile.toPath(), result);
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
}
