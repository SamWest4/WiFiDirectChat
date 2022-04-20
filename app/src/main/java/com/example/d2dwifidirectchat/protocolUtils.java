package com.example.d2dwifidirectchat;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class protocolUtils {

//    public static byte[] generateNonce(int byteLen){
//
//        SecureRandom rnd = new SecureRandom();
//        byte[] nonce = new byte[byteLen];
//        rnd.nextBytes(nonce);
//        return nonce;
//    }

    static SecureRandom rnd = new SecureRandom();

    public static BigInteger generateNonce(int len){
        BigInteger n = new BigInteger(len,rnd);
        return n;
    }

    public static byte[] getRandomNonce(int bytesNo){
        byte[] nonce = new byte[bytesNo];
        rnd.nextBytes(nonce);
        return nonce;
    }


    public static SecretKey getKeyFromPass(char[] password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        int iterationCount = 65536;
        int keyLength = 256;
        KeySpec spec = new PBEKeySpec(password, salt, iterationCount, keyLength);
        SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        return secret;
    }


    public String encrypt(String plainText, SecretKey key){

        return null;
    }

    public String decrypt(String cipherText, SecretKey key){

        return null;
    }


    public static SecretKey generateKey(int n) throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(n);
        SecretKey key = keyGenerator.generateKey();
        return key;
    }
}
