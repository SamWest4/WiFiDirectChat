package com.example.d2dwifidirectchat;

import android.annotation.SuppressLint;
import android.util.Log;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
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


    @SuppressLint("NewApi")
    public static String encrypt(byte[] plainText, SecretKey key) throws Exception {

        byte[] salt = getRandomNonce(16);

        byte[] iv = getRandomNonce(12);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));

        byte[] cipherText = cipher.doFinal(plainText);

        byte[] cipherTextWithIvSalt = ByteBuffer.allocate(iv.length + salt.length + cipherText.length)
                .put(iv)
                .put(salt)
                .put(cipherText)
                .array();

        return Base64.getEncoder().encodeToString(cipherTextWithIvSalt);

    }

    @SuppressLint("NewApi")
    public static String decrypt(String cText, SecretKey key) throws Exception {

        byte[] decode = Base64.getDecoder().decode(cText.getBytes(StandardCharsets.UTF_8));

        ByteBuffer buffer = ByteBuffer.wrap(decode);
        byte[] iv = new byte[12];
        buffer.get(iv);
        byte[] salt = new byte[16];
        buffer.get(salt);
        byte[] cipherText = new byte[buffer.remaining()];
        buffer.get(cipherText);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));

        byte[] plainText = cipher.doFinal(cipherText);

        return new String(plainText, StandardCharsets.UTF_8);

    }


    public static String convertCertToBase64PEMString(X509Certificate x509Cert) throws IOException {
        StringWriter sw = new StringWriter();
        try (JcaPEMWriter pw = new JcaPEMWriter(sw)) {
            pw.writeObject(x509Cert);
        }
        return sw.toString();
    }

    @SuppressLint("NewApi")
    public static X509Certificate convertBase64StringToCert(String x509CertString) throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(x509CertString.getBytes(StandardCharsets.UTF_8)));
    }


    private static final Provider PROVIDER = new BouncyCastleProvider();
    public static X509Certificate generateCert(String name, KeyPair keypair)
            throws Exception {
        PrivateKey key = keypair.getPrivate();

        Date notBefore = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(notBefore);
        c.add(Calendar.YEAR, 1);
        Date notAfter = c.getTime();

        X500Name owner = new X500Name("CN=" + name);
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                owner, new BigInteger(64, rnd), notBefore, notAfter, owner, keypair.getPublic());

//        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
//                owner, new BigInteger(64, rnd), notBefore, notAfter, owner, SubjectPublicKeyInfo.getInstance(keypair.getPublic()));

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").build(key);
        X509CertificateHolder certHolder = builder.build(signer);
        X509Certificate cert = new JcaX509CertificateConverter().setProvider(PROVIDER).getCertificate(certHolder);
        cert.verify(keypair.getPublic());

        Log.d("CErT",cert.toString());
        return  cert;
        //return newSelfSignedCertificate(fqdn, key, cert);
    }





}
