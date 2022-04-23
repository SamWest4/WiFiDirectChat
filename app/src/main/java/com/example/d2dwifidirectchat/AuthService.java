package com.example.d2dwifidirectchat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import com.example.d2dwifidirectchat.ServerClient.messagesChangedListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class AuthService {



    public Integer authStep;
    ArrayList<String> authStrings;

    Context thisContext;

    ServerClient serverClient;
    Boolean isHost;
    String tag;
    ChatActivity.finishedInterface authDone;
    BigInteger myNonce;
    BigInteger otherNonce;
    BigInteger exp;
    BigInteger gExp;
    X509Certificate myCert;
    PublicKey pubKey;
    PrivateKey privKey;
    X509Certificate otherCert;
    BigInteger Tseq;

    String android_id;

    private BigInteger P = new BigInteger(
            "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1"+
            "29024E088A67CC74020BBEA63B139B22514A08798E3404DD"+
            "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245"+
            "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED"+
            "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D"+
            "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F"+
            "83655D23DCA3AD961C62F356208552BB9ED529077096966D"+
            "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B"+
            "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9"+
            "DE2BCBF6955817183995497CEA956AE515D2261898FA0510"+
            "15728E5A8AACAA68FFFFFFFFFFFFFFFF",16);

    private BigInteger G = new BigInteger("2");




    public AuthService(ServerClient _serverClient, Boolean _isHost, ArrayList<String> _authStrings, ChatActivity.finishedInterface _authDone, Context context){
        authStep = 0;
        authStrings = _authStrings;
        serverClient = _serverClient;
        isHost = _isHost;
        tag = "AUTH-" + isHost.toString();
        authDone=_authDone;
        myNonce = protocolUtils.generateNonce(128);
        exp = protocolUtils.generateNonce(128);
        thisContext = context;

        android_id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);


        try {
            protocolUtils.certKeyPair keys = protocolUtils.getKeys(context,android_id);
            myCert = keys.certificate;
            pubKey = myCert.getPublicKey();
            privKey = keys.privateKey;


        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }


    public void startAuth(){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(beginAuth);
    }



    Runnable beginAuth = new Runnable() {
        @Override
        public void run() {
            while(serverClient.outStream == null){

            }
            if(isHost){
                Log.d(tag,"Sending public key");
                String mess = null;
                try {
                    mess = "{'cert':'"+ protocolUtils.convertCertToBase64PEMString(myCert)+"'}";
                    Log.d("CERT",mess);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                serverClient.writeProtocol(mess.getBytes(StandardCharsets.UTF_8));
            }
        }
    };

    @SuppressLint("NewApi")
    public void authComplete(BigInteger pass, BigInteger salt ){

        String passString = pass.toString();
        char[] password = passString.toCharArray();

        byte[] saltBytes = salt.toByteArray();

        try {
            SecretKey finalK = protocolUtils.getKeyFromPass(password, saltBytes);
            Log.d("finalKey-"+isHost, Base64.getEncoder().encodeToString(finalK.getEncoded()));
            authDone.completed(finalK);
//
//            Log.d("MYCERT"+isHost,myCert.toString());
//            Log.d("OTHERCERT"+isHost,otherCert.toString());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }



    public messagesChangedListener incomingMessagesListener = new messagesChangedListener(){
        @SuppressLint("NewApi")
        @Override
        public void onMessagesChangedListener() {

            Log.d(tag, "current step = " + authStep.toString());
            //Log.d(tag, authStrings.toString());
            String incoming = authStrings.get(authStrings.size() - 1);


            JSONObject messageObj = null;
            try {
                messageObj = new JSONObject(new String(incoming));
                Log.d(tag,String.valueOf(messageObj));
            } catch (JSONException e) {
                Log.d(tag, "failed to convert message to object");
                Log.d(tag, incoming);
                e.printStackTrace();
            }

            //Protocol steps for client J
            if(!isHost){

                switch (authStep){

                    //Receive message 1, send message 2
                    case 0:
                        if(messageObj != null){
                            try{
                                String certString = messageObj.getString("cert");
                                Log.d(tag, "Message 1 received as expected");

                                otherCert = protocolUtils.convertBase64StringToCert(certString);

                                String message = "{'cert':'"+protocolUtils.convertCertToBase64PEMString(myCert)+"'}";
                                Log.d("OTHERCERT", message);
                                serverClient.writeProtocol(message.getBytes(StandardCharsets.UTF_8));
                                authStep++;
                            }
                            catch(JSONException | CertificateException | IOException e){
                                Log.d(tag, "Message 1 not as expected!");
                                Log.d(tag,messageObj.toString());
                                Log.d(tag+"ERROR", e.toString());
                            }
                        }else{
                            Log.d(tag, "Message 1 not as expected!");
                        }
                        break;
                    //Receive message 3, send Message 4
                    case 1:
                        if(messageObj != null){
                            try{


                                otherNonce = new BigInteger(messageObj.getString("NonceI"));
                                String AID = messageObj.getString("AID");
                                Tseq = new BigInteger(messageObj.getString("Tseq"));


                                byte[] encAID = Base64.getDecoder().decode(AID.getBytes(StandardCharsets.UTF_8));
                                String AidString = protocolUtils.ecDecrypt(encAID,privKey);

                                //Check AID
                                JSONObject AidObj = new JSONObject(AidString);

                                String id = AidObj.getString("ID");
                                BigInteger nCheck = new BigInteger(AidObj.getString("NonceI"));

                                String subjectDN = otherCert.getSubjectDN().toString().substring(3);

                                if(subjectDN.equals(id) && nCheck.equals(otherNonce)){
                                    Log.d(tag,"ID and nonce match matches certificate/AID");

                                    //Check tseq
                                    //TODO: Check tseq!

                                    Log.d(tag, "Message 3 received as expected");

                                    //Calculate g^j mod p
                                    gExp = G.modPow(exp,P);

                                    String toEnc = "{NonceI:'"+otherNonce+"',resJ:'"+gExp+"'}";

                                    byte[] encrypted = protocolUtils.ecEncrypt(toEnc,otherCert.getPublicKey());
                                    String encString = Base64.getEncoder().encodeToString(encrypted);


                                    byte[] signed = protocolUtils.ecSign(encString,privKey);
                                    String signedString = Base64.getEncoder().encodeToString(signed);

                                    //Log.d("SIGN",encString);
                                    Log.d("SIGN",signedString);

                                    String finalString = "{NonceJ:'"+myNonce+"',signed:'"+signedString+"',plain:'"+encString+"'}";

                                    Log.d("SENT-Message",finalString);
                                    //String message = "{NonceJ:'"+myNonce+"',NonceI:'"+otherNonce+"',resJ:'"+gExp +"'}";
                                    serverClient.writeProtocol(finalString.getBytes(StandardCharsets.UTF_8));
                                    authStep++;


                                }
                                else{
                                    Log.d(tag,"ID or nonce dont match");
                                }



                            }
                            catch(Exception e){
                                Log.d(tag, "Message 3 not as expected!");
                                Log.d(tag,messageObj.toString());
                                e.printStackTrace();
                            }
                        }
                        else{
                            Log.d(tag, "Message 3 not as expected!");
                        }
                        break;

                    //Receive Message 5
                    case 2:
                        if(messageObj != null){
                            try{

                                String signed = messageObj.getString("signed");
                                String plain = messageObj.getString("plain");
                                byte[] signedBytes = Base64.getDecoder().decode(signed);

                                if(protocolUtils.verify(plain,signedBytes,otherCert.getPublicKey())){
                                    Log.d(tag,"Signature verified.");

                                    byte[] toDec = Base64.getDecoder().decode(plain);
                                    String dec = protocolUtils.ecDecrypt(toDec,privKey);

                                    Log.d("Decryption",dec);
                                    JSONObject decMessageObj = new JSONObject(dec);
                                    BigInteger returnedNonce = new BigInteger(decMessageObj.getString("NonceJ"));

                                    if(returnedNonce.equals(myNonce)){
                                        Log.d(tag,"Returned nonce matches");

                                        BigInteger resI = new BigInteger(decMessageObj.getString("resI"));
                                        Log.d(tag, "Message 5 received as expected");

                                        BigInteger K = resI.modPow(exp,P);
                                        Log.d(isHost+" Has", ""+ gExp);
                                        Log.d(isHost+" Has", ""+ resI);


                                        authComplete(K,otherNonce);
                                    }
                                    else{
                                        Log.d(tag,"Nonces do not match!");
                                    }

                                }else{
                                    Log.d(tag,"Signatures don't match");
                                }



                            }
                            catch(Exception e){
                                Log.d(tag, "Message 5 error parsing!");
                                Log.d(tag,messageObj.toString());
                                Log.d(tag+"ERROR", e.toString());
                            }
                        }
                        else{
                            Log.d(tag, "Message 5 not as expected!");
                        }
                        break;
                    default:
                        Log.d(tag,"default");

                }
            }
            //Protocol steps for host of protocol (client I)
            else{
                switch (authStep){

                    //Receive message 2 send message 3
                    case 0:
                        if(messageObj != null){
                            try {
                                String certString = messageObj.getString("cert");
                                Log.d(tag, "Message 2 received as expected");
                                otherCert = protocolUtils.convertBase64StringToCert(certString);


                                BigInteger Tseq = protocolUtils.generateNonce(128);



                                String aidString = "{ID:'"+android_id+"',NonceI:'"+myNonce+"'}";
                                Log.d("STRING",aidString);

                                byte[] encAID = protocolUtils.ecEncrypt(aidString,otherCert.getPublicKey());
                                String AID = Base64.getEncoder().encodeToString(encAID);


                                String message = "{AID:'"+AID+"',NonceI:'"+myNonce+"',Tseq:'"+Tseq+"'}";

                                Log.d(tag,message);
//                                serverClient.writeProtocol(Base64.getEncoder().encodeToString(messageToSend).getBytes(StandardCharsets.UTF_8));
                                serverClient.writeProtocol(message.getBytes(StandardCharsets.UTF_8));
                                authStep++;

                            } catch (JSONException | CertificateException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException | NoSuchProviderException e) {
                                Log.d(tag, "Message 2 error");
                                Log.d(tag+"ERROR", e.toString());
                                e.printStackTrace();
                            }
                        }
                        else{
                            Log.d(tag, "Message 2 not as expected!");
                        }
                        break;
                    //Receive message 4 send message 5
                    case 1:

                        if(messageObj != null){
                            try {


                                otherNonce = new BigInteger(messageObj.getString("NonceJ"));
                                String signed = messageObj.getString("signed");
                                String plain = messageObj.getString("plain");
                                byte[] signedBytes = Base64.getDecoder().decode(signed);

                                Boolean verify = protocolUtils.verify(plain,signedBytes,otherCert.getPublicKey());

                                if(verify){
                                    Log.d(tag,"Signature verification successful");

                                    byte[] toDec = Base64.getDecoder().decode(plain);
                                    String dec = protocolUtils.ecDecrypt(toDec,privKey);

                                    Log.d("Decryption",dec);

                                    JSONObject decMessageObj = new JSONObject(dec);

                                    BigInteger returnedNonce = new BigInteger(decMessageObj.getString("NonceI"));

                                    if(returnedNonce.equals(myNonce)){
                                        Log.d(tag,"Returned nonce matches");

                                        BigInteger resJ = new BigInteger(decMessageObj.getString("resJ"));
                                        Log.d(tag, "Message 4 received as expected");


                                        gExp = G.modPow(exp,P);
                                        BigInteger K = resJ.modPow(exp,P);

                                        Log.d(isHost+" Has", ""+ gExp);
                                        Log.d(isHost+" Has", ""+ resJ);


                                        String strToEnc = "{NonceJ:'"+otherNonce+"',resI:'"+gExp+"'}";
                                        byte[] encrypted = protocolUtils.ecEncrypt(strToEnc,otherCert.getPublicKey());
                                        String encString = Base64.getEncoder().encodeToString(encrypted);
                                        byte[] signBytes = protocolUtils.ecSign(encString,privKey);

                                        String signString = Base64.getEncoder().encodeToString(signBytes);

                                        String finString = "{signed:'"+signString+"',plain:'"+encString+"'}";

                                        serverClient.writeProtocol(finString.getBytes(StandardCharsets.UTF_8));
                                        authStep++;

                                        authComplete(K,myNonce);
                                    }
                                    else{
                                        Log.d(tag,"Nonces do not match!");
                                    }
                                }


                            } catch (Exception e) {
                                Log.d(tag, "Message 4 error parsing");
                                Log.d(tag+"ERROR", e.toString());
                            }
                        }
                        else{
                            Log.d(tag, "Message 4 not as expected!");
                        }
                        break;
                    default:
                        Log.d(tag,"default");
                }
            }
        }
    };


}
