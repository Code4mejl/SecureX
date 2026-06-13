package com.securex;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class AESUtil {

    public static byte[] encrypt(byte[] data, String password) throws Exception {
        SecretKeySpec secretKey = buildKey(password);
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }

    public static byte[] decrypt(byte[] data, String password) throws Exception {
        SecretKeySpec secretKey = buildKey(password);
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }

    private static SecretKeySpec buildKey(String password) {
        byte[] key = password.getBytes();
        byte[] aesKey = new byte[16];
        for (int i = 0; i < 16; i++) {
            aesKey[i] = (i < key.length) ? key[i] : 0;
        }
        return new SecretKeySpec(aesKey, "AES");
    }
}
