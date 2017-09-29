package com.zwh.nfcdemo;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.*;

import android.widget.Toast;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    NfcAdapter nfcAdapter;
    TextView infoView;
    PendingIntent pi;
    IntentFilter tagDetected;
    String lock0;
    String lock1;
    String lock2;
    String lock3;
    /** A MIFARE Ultralight tag */
    public static final int TYPE_ULTRALIGHT = 1;
    /** A MIFARE Ultralight C tag */
    public static final int TYPE_ULTRALIGHT_C = 2;

    public static final int BL_15_10 = 2;
    public static final int BL_9_4 = 1;
    public static final int BL_OTP = 0;

    public static final int PAGE_36_39 = 7;
    public static final int PAGE_32_35 = 6;
    public static final int PAGE_28_31 = 5;
    public static final int BL_5_7 = 4;
    public static final int PAGE_24_27 = 3;
    public static final int PAGE_20_23 = 2;
    public static final int PAGE_16_19 = 1;
    public static final int BL_1_3 = 0;

    public static final int PAGE_44_47 = 7;
    public static final int PAGE_43 = 6;
    public static final int PAGE_42 = 5;
    public static final int PAGE_41 = 4;
    public static final int LB_KEY = 3;
    public static final int LB_AUTH1 = 2;
    public static final int LB_AUTH0 = 1;
    public static final int LB_CNT = 0;

    public static final String STATE_DOT_STRING = " . ";
    public static final String STATE_STAR_STRING = " * ";
    public static final String STATE_PLUS_STRING = " + ";
    public static final String STATE_CROSS_STRING = " x ";
    public static final int OFFSET = 7;

    byte[] mifareULCDefaultKey = {//default ulc authenticate key -- BREAKMEIFYOUCAN!
            (byte) 0x49, (byte) 0x45, (byte) 0x4D, (byte) 0x4B,
            (byte) 0x41, (byte) 0x45, (byte) 0x52, (byte) 0x42,
            (byte) 0x21, (byte) 0x4E, (byte) 0x41, (byte) 0x43,
            (byte) 0x55, (byte) 0x4F, (byte) 0x59, (byte) 0x46 };

    byte[] mifareULCSimpleKey = { //simple ulc authenticate key -- ALL ZERO for test purpose
            (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00};

    Tag tagFromIntent;
    MifareUltralight mifareUltralight;
    NfcA nfcA;
    NdefFormatable ndefFormatable;
    private Button writeNFC;
    private Button authNFC;
    private Button tagInfo;
    Intent mIntent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.e("zhangwenhao","oncreate");

        writeNFC = (Button) findViewById(R.id.writeNFC);
        writeNFC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    if (mifareUltralight != null){
                        Log.e("zhangwenhao","write nfc clicked");
                        writeData();
                    }
            }
        });

        authNFC = (Button) findViewById(R.id.authNFC);
        authNFC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mifareUltralight != null){
                    Log.e("zhangwenhao","auth nfc clicked");
                    authNFC();
                }
            }
        });

        tagInfo = (Button) findViewById(R.id.getTagInfo);
        tagInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getIntent() != null){
                    processIntent();
                }
            }
        });

        infoView = (TextView) findViewById(R.id.promt);
        infoView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() > 0){

                }
            }
        });
        nfcAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
        if (nfcAdapter == null) {
            showErrorMessages("Device doesn't support NFC!");
            finish();
            return;
        }
        if (!nfcAdapter.isEnabled()) {
            showErrorMessages("Please enable NFC first!");
            finish();
            return;
        }
        //init_NFC();
    }

    public void showErrorMessages(String string){
        Toast.makeText(this,string,Toast.LENGTH_LONG).show();
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.e("zhangwenhao","onResume");

        PendingIntent intent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        NfcAdapter.getDefaultAdapter(this).enableForegroundDispatch(this,intent, null, null);

        /*nfcAdapter.enableForegroundDispatch(this, pi,
                new IntentFilter[] { tagDetected }, null);*/

    }
    /*private void init_NFC() {
        // 初始化PendingIntent，当有NFC设备连接上的时候，就交给当前Activity处理
        Log.e("zhangwenhao","init nfc");
        pi = PendingIntent.getActivity(this, 0, new Intent(this, getClass())
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        // 新建IntentFilter，使用的是第二种的过滤机制
        tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
    }*/

    //make byte[] result readable
    private String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("0x");
        if (src == null || src.length <= 0) {
            return null;
        }
        char[] buffer = new char[2];
        for (int i = 0; i < src.length; i++) {
            buffer[0] = Character.forDigit((src[i] >>> 4) & 0x0F, 16);
            buffer[1] = Character.forDigit(src[i] & 0x0F, 16);
            System.out.println(buffer);
            stringBuilder.append(buffer);
        }
        return stringBuilder.toString();
    }
    //get each bit of a byte data
    private String byteToBinaryString(byte src){
        StringBuilder stringBuilder = new StringBuilder("");
        char[] buffer = new char[8];
        String binaryString = Integer.toBinaryString(src);
        for (int i = 8; i > binaryString.length(); i--){
            stringBuilder.append("0");
        }
        stringBuilder.append(binaryString);
        return stringBuilder.toString();
    }
    //get all locks of ulc
    private void getLockState(byte[] lock_temp1,byte[] lock_temp2){
        lock0 =  byteToBinaryString(lock_temp1[2]);
        lock1 =  byteToBinaryString(lock_temp1[3]);
        lock2 =  byteToBinaryString(lock_temp2[0]);
        lock3 =  byteToBinaryString(lock_temp2[1]);
    }
    //get page state string -- (. * x +)
    private String getPageSate(int pageIndex){
        if (pageIndex < 0){
            return " error ";
        }
        int locked = 0;
        int blocked = 0;
        if (pageIndex > 43){
            locked = 2;
            blocked =2;
        }else if (pageIndex == 0x2B){
            blocked = getLockIndexValue(lock3,PAGE_43);
            locked = getLockIndexValue(lock3,LB_AUTH1);
        }else if (pageIndex == 0x2A){
            blocked = getLockIndexValue(lock3,PAGE_42);
            locked = getLockIndexValue(lock3,LB_AUTH0);
        }else if (pageIndex == 0x29){
            blocked = getLockIndexValue(lock3,PAGE_41);
            locked = getLockIndexValue(lock3,LB_CNT);
        }else if (pageIndex == 0x28){
            locked = getLock23PageState();
            if (locked == 6){
                return STATE_STAR_STRING;
            }else {
                return STATE_DOT_STRING;
            }
        }
        else if (pageIndex > 0x23){
            blocked = getLockIndexValue(lock2,PAGE_36_39);
            locked = getLockIndexValue(lock2,BL_5_7);
        }else if (pageIndex > 0x1F){
            blocked = getLockIndexValue(lock2,PAGE_32_35);
            locked = getLockIndexValue(lock2,BL_5_7);
        }else if (pageIndex > 0x1B){
            blocked = getLockIndexValue(lock2,PAGE_28_31);
            locked = getLockIndexValue(lock2,BL_5_7);
        }else if (pageIndex > 0x17){
            blocked = getLockIndexValue(lock2,PAGE_24_27);
            locked = getLockIndexValue(lock2,BL_1_3);
        }else if (pageIndex > 0x13){
            blocked = getLockIndexValue(lock2,PAGE_20_23);
            locked = getLockIndexValue(lock2,BL_1_3);
        }else if (pageIndex > 0x0F){
            blocked = getLockIndexValue(lock2,PAGE_16_19);
            locked = getLockIndexValue(lock2,BL_1_3);
        }else if (pageIndex > 0x09){
            blocked = getLockIndexValue(lock1,pageIndex - 8);
            locked = getLockIndexValue(lock0,BL_15_10);
        }else if (pageIndex > 0x07){
            blocked = getLockIndexValue(lock1,pageIndex - 8);
            locked = getLockIndexValue(lock0,BL_9_4);
        }else if (pageIndex > 0x03){
            blocked = getLockIndexValue(lock0,pageIndex);
            locked = getLockIndexValue(lock0,BL_9_4);
        }else if (pageIndex == 3){
            blocked = 0;
            locked = 0;
        }else if (pageIndex == 2){
            locked = getLock01State();
            if (locked == 3){
                return STATE_STAR_STRING;
            }else {
                return STATE_DOT_STRING;
            }
        }
        else {
            blocked = 1;
            locked = 1;
        }
        return getStateString(blocked,locked);
    }
    private String getStateString(int locked, int blocked){

        if (locked + blocked == 0){
            return STATE_DOT_STRING;
        }

        if (locked + blocked == 2){
            return STATE_STAR_STRING;
        }

        if (locked + blocked == 4){
            return " .- ";
        }

        if (blocked == 1){
            return STATE_PLUS_STRING;
        }
        if (locked == 1){
            return STATE_CROSS_STRING;
        }
        return " ";

    }

    //get lock page state string
    private int getLock01State(){
        int count = 0;
        for (int i = 0; i< 3; i ++){
            count += Character.digit(lock0.charAt(i),10);
        }
        return count;
    }
    private int getLock23PageState(){
        int count = 0;
        count += Character.digit(lock2.charAt(4),10);
        count += Character.digit(lock2.charAt(0),10);
        for (int i = 0; i < 4; i++){
            count += Character.digit(lock3.charAt(i),10);
        }
        return count;

    }
    //get value of lock at position offset
    private int getLockIndexValue(String lock, int offset){
        return Character.digit(lock.charAt(OFFSET - offset),2);
    }

    public void initNFC(Intent intent){
        //get TAG from intent
        tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        //get tech from TAG
        //tech list: {android.nfc.tech.NfcA, android.nfc.tech.MifareUltralight, android.nfc.tech.NdefFormatable}
        nfcA = NfcA.get(tagFromIntent);
        mifareUltralight = MifareUltralight.get(tagFromIntent);
        //nfcA & mifareUltralight are equivalent when send raw NfcA data to a tag and receive the response.
    }
    private void processIntent() {


        try {
            if (mifareUltralight == null){
                showErrorMessages("WARNING: Not Mifare Ultralight C card!");
                return;
            }
            StringBuilder output = new StringBuilder("");//for show information of TAG
            if (mifareUltralight.isConnected()){
                Log.e("zhangwenhao","close connect");
                mifareUltralight.close();
            }
            mifareUltralight.connect();//noted: connect before operate TAG
            //get lock state
            byte[] lock_temp1 = mifareUltralight.readPages(0x02);
            byte[] lock_temp2 = mifareUltralight.readPages(0x28);
            getLockState(lock_temp1,lock_temp2);
            //get information
            String[] tech = tagFromIntent.getTechList();
            output.append("Type:" + getTypeString(mifareUltralight.getType()) + "\n");
            output.append("\nTECH list:\n");
            for (int i = 0 ; i < tech.length; i++){
                output.append(tech[i] + "\n");
            }
            output.append("\nID:\n" + bytesToHexString(tagFromIntent.getId()) + "\n" );
            output.append("\nATQA:\n" + bytesToHexString(nfcA.getAtqa()) +"\n" );
            output.append("\nSAK:\n" + nfcA.getSak() +"\n" );
            output.append("\nMaxTransceiveLength:" + mifareUltralight.getMaxTransceiveLength() + "\n");
            output.append("\nContents:");
            //get memory content from page 0x00 to 0x2B and format them
            StringBuilder temp = new StringBuilder("");
            for (int i = 0 ;i < 0x2C ; ) {
                //these two command are same
                //temp.append(bytesToHexString(mifareUltralight.readPages(i)).substring(2));
                temp.append(bytesToHexString(mifareUltralight.transceive(new byte[]{(byte)0x30,(byte)(i & 0x0ff)})).substring(2));
                i = i + 4;
            }
            for (int i = 0 , j = 0; i < temp.length(); i += 2)
            {
                if( j % 4 == 0){
                    output.append("\n");
                    output.append(String.format("[%02X]",j/4));
                    output.append(getPageSate(j/4));
                }
                j ++;
                if(i+2<=temp.length())
                {
                    String str = temp.substring(i, i + 2);
                    output.append(str + " ");
                }
                /*if (i == temp.length() - 2){
                    output.append(get0x2CTo0x2F());
                }*/
            }
            mifareUltralight.close();//close connect after operation finished
            output.append("\n" + noteString());
            infoView.setText(output);
        }catch (IOException e){
            e.printStackTrace();
            showErrorMessages("Error: " + e.getMessage());
            Log.e("zhangwenhao","processIntent error : " + e.getMessage());
        }
    }
    public String get0x2CTo0x2F(){
        return "";
    }

    private void writeData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!mifareUltralight.isConnected()) {
                        mifareUltralight.connect();
                    }
                    Log.e("zhangwenhao", "begin write");
                    Log.e("zhangwenhao", "respond 0x0A means command succeed");

                    byte[] result;
                    //write authenticate key (default key: BREAKMEIFYOUCAN!)
                    /*result = mifareUltralight.transceive(new byte[]{ (byte)0xA2, 0x2C, (byte) 0x42,(byte) 0x52,(byte) 0x45,(byte) 0x41});
                    Log.e("zhangwenhao", "result : 0x2C " + bytesToHexString(result));
                    result = mifareUltralight.transceive(new byte[]{ (byte)0xA2, 0x2D, (byte) 0x4B,(byte) 0x4D,(byte) 0x45,(byte) 0x49});
                    Log.e("zhangwenhao", "result : 0x2D " + bytesToHexString(result));
                    result = mifareUltralight.transceive(new byte[]{ (byte)0xA2, 0x2E, (byte) 0x46,(byte) 0x59,(byte) 0x4F,(byte) 0x55});
                    Log.e("zhangwenhao", "result : 0x2E " + bytesToHexString(result));
                    result = mifareUltralight.transceive(new byte[]{ (byte)0xA2, 0x2F, (byte) 0x43,(byte) 0x41,(byte) 0x4E,(byte) 0x21});
                    Log.e("zhangwenhao", "result : 0x2F " + bytesToHexString(result));*/

                    //write simple key (simple key: all zero)
                    /*result = mifareUltralight.transceive(new byte[]{ (byte)0xA2, 0x2C, (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00});
                    Log.e("zhangwenhao", "result : 0x2C " + bytesToHexString(result));
                    result = mifareUltralight.transceive(new byte[]{ (byte)0xA2, 0x2D, (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00});
                    Log.e("zhangwenhao", "result : 0x2D " + bytesToHexString(result));
                    result = mifareUltralight.transceive(new byte[]{ (byte)0xA2, 0x2E, (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00});
                    Log.e("zhangwenhao", "result : 0x2E " + bytesToHexString(result));
                    result = mifareUltralight.transceive(new byte[]{ (byte)0xA2, 0x2F, (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00});
                    Log.e("zhangwenhao", "result : 0x2F " + bytesToHexString(result));*/

                    //write auth0 to protect 0x2C to 0x2F
                    /*result = mifareUltralight.transceive(new byte[]{ (byte) 0xA2,(byte) 0x06, (byte) 0x33,(byte) 0x00,(byte) 0x00,(byte) 0x00});
                    Log.e("zhangwenhao", "result : 0x23 " + bytesToHexString(result));*/
                    //test
                    //mifareUltralight.setTimeout(10000);

                    Log.e("zhangwenhao", "Timeout: " + mifareUltralight.getTimeout());

                    result = mifareUltralight.transceive(new byte[]{ (byte)0xA2, 0x05, (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00});
                    //Log.e("zhangwenhao", "result : 0x02 " + bytesToHexString(result));
                    //mifareUltralight.writePage(0x05,new byte[]{  (byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00});

                    Log.e("zhangwenhao", "end write");
                    mifareUltralight.close();

                }catch (IOException e){
                    e.printStackTrace();
                    //showErrorMessages("writeData error : " + e.getMessage());
                    Log.e("zhangwenhao","writeData error : " + e.getMessage());

                }
            }
        }).start();
    }

    private void authNFC() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mifareUltralight.isConnected()) {
                        mifareUltralight.close();
                        Log.e("zhangwenhao", "connect ");
                    }
                    mifareUltralight.connect();
                    Log.e("zhangwenhao", "auth begin ");
                    Log.e("zhangwenhao", "Step1: use command: 0x1A,0x00");
                    byte[] encRndB = mifareUltralight.transceive(new byte[]{
                            0x1A,0x00 //authenticate step1 command and get encrypted RndB
                    });
                    encRndB = Arrays.copyOfRange(encRndB,1,9);//extract encRndB which just removed first byte
                    byte[] rndB = desDecrypt(mifareULCDefaultKey,encRndB);//decrypted encRndB to get RndB
                    byte[] rndBLeft = rotateLeft(rndB);//get rndB'(just move the first byte to the end)
                    byte[] rndA = new byte[8];
                    generateRandom(rndA);//get rndA
                    byte[] rndAB = byteMerger(rndA,rndBLeft);//concat rndA and rndB'
                    byte[] step2_header_command = new byte[]{(byte)0xAF};
                    //get step2 command (concat 0xAF and encrypted rndAB)
                    byte[] step2_command = byteMerger(step2_header_command,desEncrypt(mifareULCDefaultKey,rndAB));

                    Log.e("zhangwenhao", "rndA: " + bytesToHexString(rndA));
                    Log.e("zhangwenhao", "rndB: " + bytesToHexString(rndB));
                    Log.e("zhangwenhao", "encRndB: " + bytesToHexString(encRndB));
                    Log.e("zhangwenhao", "rndBLeft: " + bytesToHexString(rndBLeft));
                    Log.e("zhangwenhao", "iv: " + bytesToHexString(iv));
                    Log.e("zhangwenhao", "rndAB: " + bytesToHexString(rndAB));
                    Log.e("zhangwenhao", "Step2: use command: " + bytesToHexString(step2_command));

                    byte[] step2_result =  mifareUltralight.transceive(step2_command);//the response is just encRndA
                    Log.e("zhangwenhao", "encRndA: " + bytesToHexString(step2_result));
                    //here is just calculate ek(rndA') with key
                    //compare encRndA with step2_result and they are same :)
                    byte[] rndALeft = rotateLeft(rndA);
                    byte[] encRndA = desEncrypt(mifareULCDefaultKey,rndALeft);
                    Log.e("zhangwenhao", "rndALeft: " + bytesToHexString(rndALeft));
                    Log.e("zhangwenhao", "encRndA: " + bytesToHexString(encRndA));
                    Log.e("zhangwenhao", "iv: " + bytesToHexString(iv));
                    //if error happened, connect will be closed
                    Log.e("zhangwenhao", "authenticate state : " + mifareUltralight.isConnected());

                    //shouldn't close after authenticate because we may need do some operation after this
                    //mifareUltralight.close();
                }catch (IOException e){
                    e.printStackTrace();
                    Log.e("zhangwenhao","authNFC error : " + e.getMessage());
                }
            }
        }).start();

    }
    public static byte[] byteMerger(byte[] byte_1, byte[] byte_2){
        byte[] byte_3 = new byte[byte_1.length+byte_2.length];
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
        return byte_3;
    }
    protected static SecureRandom rnd = new SecureRandom();
    protected static void generateRandom(byte[] rndA) {
        rnd.nextBytes(rndA);
    }
    protected byte[] desEncrypt(byte[] key, byte[] data) {
        return performDes(Cipher.ENCRYPT_MODE, key, data);
    }
    protected byte[] desDecrypt(byte[] key, byte[] data) {
        return performDes(Cipher.DECRYPT_MODE, key, data);
    }
    private byte[] iv = new byte[8];
    public byte[] performDes(int opMode, byte[] key, byte[] data){
        try {
            Cipher des = Cipher.getInstance("DESede/CBC/NoPadding");
            SecretKeyFactory desKeyFactory = SecretKeyFactory.getInstance("DESede");
            Key desKey = desKeyFactory.generateSecret(new DESedeKeySpec(byteMerger(key, Arrays.copyOf(key, 8))));
            if (opMode == Cipher.DECRYPT_MODE) {
                iv = new byte[8];
            }
            des.init(opMode, desKey, new IvParameterSpec(iv));
            byte[] ret = des.doFinal(data);
            if(opMode==Cipher.ENCRYPT_MODE) {
                iv=Arrays.copyOfRange(ret, ret.length-8, ret.length);
            } else {
                iv=Arrays.copyOfRange(data, data.length-8, data.length);
            }
            return ret;
        }catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidKeySpecException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e){
            throw new RuntimeException(e);
        }
    }
    protected static byte[] rotateLeft(byte[] in) {
        return byteMerger(Arrays.copyOfRange(in, 1, 8), Arrays.copyOf(in,1));
    }
    public String getTypeString(int id){
        switch (id){
            case TYPE_ULTRALIGHT:
                return "MIFARE Ultralight";
            case TYPE_ULTRALIGHT_C:
                return "MIFARE Ultralight C";
            default:
                return "UNKNOWN";
        }
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.e("zhangwenhao","onNewIntent " + intent.getAction());
        if (intent.getAction().equals("android.nfc.action.TAG_DISCOVERED")) {
            setIntent(intent);
            initNFC(intent);
            processIntent();
        }


    }
    public String noteString(){
        StringBuilder note = new StringBuilder();

        note.append("\n*:locked & blocked, x:locked,");
        note.append("\n+:blocked, .:un(b)locked,?:unknown");
        note.append("\nr:readable (write-protected),");
        note.append("\np:password protected, -:write-only");
        note.append("\nP:password protected write-only");

        return note.toString();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e("zhangwenhao","onPause");
        if (NfcAdapter.getDefaultAdapter(this) != null) {
            NfcAdapter.getDefaultAdapter(this).disableForegroundDispatch(this);
        }
    }
}

