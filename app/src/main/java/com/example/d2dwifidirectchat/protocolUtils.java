package com.example.d2dwifidirectchat;

import android.annotation.SuppressLint;
import android.content.Context;
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
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
    }


    public static class certKeyPair {
        public X509Certificate certificate;
        public PrivateKey privateKey;

        public certKeyPair(X509Certificate cert, PrivateKey key){
            certificate = cert;
            privateKey = key;
        }
    }

    @SuppressLint("NewApi")
    public static certKeyPair getKeys(Context thisContext, String android_id) throws NoSuchAlgorithmException {

        File fileDir = thisContext.getFilesDir();
        //Check for keys already stored
        File f = new File(fileDir,"cert.cer");
        if(f.exists() && !f.isDirectory()) {
            try{
                KeyFactory keyFactory = KeyFactory.getInstance("EC");

                //Read certificates
                File fileCert = new File(fileDir,"cert.cer");
                FileInputStream fisCert = new FileInputStream(fileCert);
                InputStreamReader isrCert = new InputStreamReader(fisCert, StandardCharsets.UTF_8);
                BufferedReader readerCert = new BufferedReader(isrCert);

                StringBuilder certString = new StringBuilder();
                String line = readerCert.readLine();
                while(line != null){
                    certString.append(line+ '\n');
                    line = readerCert.readLine();
                }
                X509Certificate cert = protocolUtils.convertBase64StringToCert(certString.toString());

                Log.d("CERT", cert.toString());

                //Read in private key
                File filePrivateKey = new File(fileDir,"private.key");
                FileInputStream fis = new FileInputStream(filePrivateKey);
                byte[] encodedPrivateKey = new byte[(int) filePrivateKey.length()];
                fis.read(encodedPrivateKey);
                fis.close();
                PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(
                        encodedPrivateKey);
                PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

                Log.d("CERT", "Successfully read keys");
                return new certKeyPair(cert,privateKey);

            } catch (IOException | CertificateException | InvalidKeySpecException e) {
                Log.d("CERT", "ERROR loading keys");
                Log.d("CERT", e.toString());
                e.printStackTrace();
            }

        }
        else {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            keyGen.initialize(256, random);
            KeyPair pair = keyGen.generateKeyPair();

            try {
                X509Certificate cert = protocolUtils.generateCert(android_id, pair);

                Log.d("CERRRR", cert.toString());

                //Save certificate (public key)
                String s = protocolUtils.convertCertToBase64PEMString(cert);
                Log.d("BASE64 string",s);
                FileOutputStream os = new FileOutputStream(new File(fileDir,"cert.cer"));
                os.write(protocolUtils.convertCertToBase64PEMString(cert).getBytes(StandardCharsets.UTF_8));
                os.close();

                X509Certificate cer = protocolUtils.convertBase64StringToCert(s);
                Log.d("CERRRR", cer.toString());

                //Save private key
                PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(
                        pair.getPrivate().getEncoded());
                os = new FileOutputStream(new File(fileDir,"private.key"));
                os.write(pkcs8EncodedKeySpec.getEncoded());
                os.close();

                return new certKeyPair(cert,pair.getPrivate());

            } catch (Exception e) {
                Log.d("KEYS", "Failed to generate and save cert");
                e.printStackTrace();
            }
        }
        return null;
    }





}
