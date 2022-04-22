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
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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


//            try {
//                String plain = "hello there!";
//                Log.d("RSA",plain);
//                byte[] cText = protocolUtils.rsaEncrypt(plain,pubKey);
//                Log.d("RSA",new String(cText,StandardCharsets.UTF_8));
//                String dec =  protocolUtils.rsaDecrypt(cText,privKey);
//                Log.d("RSA", dec);
//
//                byte[] signed = protocolUtils.rsaEncrypt(plain, privKey);
//                Log.d("SIGNEDRSA", new String(signed,StandardCharsets.UTF_8));
//                String decSigned =  protocolUtils.rsaDecrypt(signed,pubKey);
//                Log.d("SIGNEDRSA", decSigned);
//
//            } catch (BadPaddingException| NoSuchPaddingException| IllegalBlockSizeException | InvalidKeyException e) {
//                e.printStackTrace();
//            }


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
//            if(isHost){
//                Log.d(tag,"Sending first message");
//                String mess = "{'NonceI':'"+myNonce+"'}";
//
//                Log.d(tag,mess);
//                serverClient.writeProtocol(mess.getBytes(StandardCharsets.UTF_8));
//                authStep++;
//            }
            if(isHost){
                Log.d(tag,"Sending public key");
                String mess = null;
                try {
                    mess = "{'cert':'"+ protocolUtils.convertCertToBase64PEMString(myCert)+"'}";
                } catch (IOException e) {
                    e.printStackTrace();
                }

                serverClient.writeProtocol(mess.getBytes(StandardCharsets.UTF_8));
                //authStep++;
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

            Log.d("MYCERT"+isHost,myCert.toString());
            Log.d("OTHERCERT"+isHost,otherCert.toString());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
    }



    public messagesChangedListener incomingMessagesListener = new messagesChangedListener(){
        @Override
        public void onMessagesChangedListener() {

            Log.d(tag, "current step = " + authStep.toString());
            Log.d(tag, authStrings.toString());
            String incoming = authStrings.get(authStrings.size() - 1);


            JSONObject messageObj = null;
            try {
                messageObj = new JSONObject(incoming);
                Log.d(tag,String.valueOf(messageObj));
            } catch (JSONException e) {
                Log.d(tag, "failed to convert message to object");
                Log.d(tag, incoming);
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
                                Log.d(tag, "Message 3 received as expected");

                                //Calculate g^j mod p
                                gExp = G.modPow(exp,P);

                                String message = "{NonceJ:'"+myNonce+"',NonceI:'"+otherNonce+"',resJ:'"+gExp +"'}";
                                serverClient.writeProtocol(message.getBytes(StandardCharsets.UTF_8));
                                authStep++;
                            }
                            catch(JSONException e){
                                Log.d(tag, "Message 3 not as expected!");
                                Log.d(tag,messageObj.toString());
                                Log.d(tag+"ERROR", e.toString());
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

                                BigInteger returnedNonce = new BigInteger(messageObj.getString("NonceJ"));
                                if(returnedNonce.equals(myNonce)){
                                    Log.d(tag,"Nonce is the same!");

                                    BigInteger resI = new BigInteger(messageObj.getString("resI"));

                                    Log.d(tag, "Message 5 received as expected");

                                    BigInteger K = resI.modPow(exp,P);
                                    Log.d(tag,"Protocol completed!");


                                    authComplete(K,otherNonce);


                                }
                                else{
                                    Log.d(tag, "invalid nonce returned");
                                    Log.d(tag,myNonce.toString());
                                    Log.d(tag,returnedNonce.toString());
                                }
                            }
                            catch(JSONException e){
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


                                String mess = "{'NonceI':'"+myNonce+"'}";
                                Log.d(tag,"Sending Second message");
                                serverClient.writeProtocol(mess.getBytes(StandardCharsets.UTF_8));
                                authStep++;

                            } catch (JSONException | CertificateException e) {
                                Log.d(tag, "Message 2 error parsing");
                                Log.d(tag+"ERROR", e.toString());
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
                                BigInteger returnedNonce = new BigInteger(messageObj.getString("NonceI"));

                                if(returnedNonce.equals(myNonce)){
                                    Log.d(tag,"Nonce is the same!");

                                    otherNonce = new BigInteger(messageObj.getString("NonceJ"));
                                    BigInteger resJ = new BigInteger(messageObj.getString("resJ"));

                                    Log.d(tag, "Message 2 received as expected");

                                    gExp = G.modPow(exp,P);

                                    BigInteger K = resJ.modPow(exp,P);

                                    String message = "{NonceJ:'"+otherNonce+"',resI:'"+gExp +"'}";

                                    serverClient.writeProtocol(message.getBytes(StandardCharsets.UTF_8));
                                    authStep++;

                                    authComplete(K,myNonce);
                                }
                                else{
                                    Log.d(tag, "invalid nonce returned");
                                    Log.d(tag,myNonce.toString());
                                    Log.d(tag,returnedNonce.toString());
                                }
                            } catch (JSONException e) {
                                Log.d(tag, "Message 2 error parsing");
                                Log.d(tag+"ERROR", e.toString());
                            }
                        }
                        else{
                            Log.d(tag, "Message 2 not as expected!");
                        }
                        break;
                    default:
                        Log.d(tag,"default");
                }
            }
        }
    };


}
