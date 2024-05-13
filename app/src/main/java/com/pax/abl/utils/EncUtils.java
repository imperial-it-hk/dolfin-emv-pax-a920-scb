/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-2-13
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.abl.utils;

import th.co.bkkps.utils.Log;

import com.pax.device.UserParam;
import com.pax.eemv.utils.Tools;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.ped.PedManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * encryption utils
 */
public class EncUtils {
    private EncUtils() {
        //do nothing
    }

    /**
     * sha1 calc
     *
     * @param str input data
     * @return sha1 value
     */
    public static String sha1(String str) {
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("SHA-1");
            digest.update(str.getBytes());
            byte[] messageDigest = digest.digest();
            // Create Hex String
            StringBuilder hexString = new StringBuilder();

            for (byte i : messageDigest) {
                String shaHex = Integer.toHexString(i & 0xFF);
                if (shaHex.length() < 2) {
                    hexString.append(0);
                }
                hexString.append(shaHex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            Log.w("SHA-1", e);
        }
        return "";
    }

    public static byte[] sha256(byte[] input) {
        try {
            MessageDigest mDigest = MessageDigest.getInstance("SHA256");
            byte[] result = mDigest.digest(input);

            return result;

        } catch (NoSuchAlgorithmException e) {
            Log.w("SHA-256", e);
        }
        return null;
    }

    public static byte[] SHA_X919(byte[] input, Acquirer acquirer)
    {
        int icnt;
        byte[] eData, result;
        byte[] sha_data = sha256(input);

        PedManager ped = FinancialApplication.getPedInstance();
        eData  = ped.calcDes(PedManager.TRI_ENCRYPT, (byte)(UserParam.getMAKID(acquirer)), Arrays.copyOfRange(sha_data,0,8));
        System.arraycopy(eData, 0 , sha_data, 0, 8);

        for(icnt=0;icnt<8;icnt++)
        {
            sha_data[8+icnt] ^= sha_data[icnt];
        }

        eData  = ped.calcDes(PedManager.TRI_ENCRYPT, (byte)(UserParam.getMAKID(acquirer)), Arrays.copyOfRange(sha_data,8,16));
        System.arraycopy(eData, 0 , sha_data, 8, 8);

        for(icnt=0;icnt<8;icnt++)
        {
            sha_data[16+icnt] ^= sha_data[8 + icnt];
        }

        eData  = ped.calcDes(PedManager.TRI_ENCRYPT, (byte)(UserParam.getMAKID(acquirer)), Arrays.copyOfRange(sha_data,16,24));
        System.arraycopy(eData, 0 , sha_data, 16, 8);

        for(icnt=0;icnt<8;icnt++)
        {
            sha_data[24+icnt] ^= sha_data[16 + icnt];
        }

        eData  = ped.calcDes(PedManager.TRI_ENCRYPT, (byte)(UserParam.getMAKID(acquirer)), Arrays.copyOfRange(sha_data,24,32));
        System.arraycopy(eData, 0 , sha_data, 24, 8);

        result = new byte[8];
        System.arraycopy(sha_data, 24 , result, 0, 8);

        return result;
    }


    /**
     * SHA-512 calc
     *
     * @param str input data
     * @return SHA-512 HEX String value
     */
    public static String sha512(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(str.getBytes(StandardCharsets.UTF_8));

            byte[] byteData = md.digest();

            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < byteData.length; i++) {
                hexString.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }
            Log.i("SHA-512 STR: ", hexString.toString());
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.w("SHA-512", e);
        }
        return "";
    }

    /**
     * md5 calc
     *
     * @param str input data
     * @return md5 value
     */
    public static String md5(String str) {
        try {
            MessageDigest mdInst = MessageDigest.getInstance("MD5");

            mdInst.update(str.getBytes());

            byte[] md = mdInst.digest();
            // 把密文转换成十六进制的字符串形式
            StringBuilder hexString = new StringBuilder();
            // 字节数组转换为 十六进制 数
            for (byte i : md) {
                String shaHex = Integer.toHexString(i & 0xFF);
                if (shaHex.length() < 2) {
                    hexString.append(0);
                }
                hexString.append(shaHex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.w("MD5", e);
        }
        return "";
    }
}
