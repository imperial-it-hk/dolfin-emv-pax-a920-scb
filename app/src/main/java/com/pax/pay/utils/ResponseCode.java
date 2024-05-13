/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-11-26
 * Module Author: Steven.W
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.utils;

import com.pax.edc.R;
import com.pax.pay.app.FinancialApplication;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ResponseCode implements Serializable {
    private static final long serialVersionUID = 1L;

    private String code;
    private String message;
    private HashMap<String, ResponseCode> map;
    private static ResponseCode rcCode;

    private Map<String, Integer> responses;

    private ResponseCode() {
    }

    private ResponseCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public static ResponseCode getInstance() {
        if (rcCode == null) {
            rcCode = new ResponseCode();
        }
        return rcCode;
    }

    public void loadCode() {
        responses = new HashMap<>();
        responses.put("00", R.string.response_00);
        responses.put("01", R.string.response_01);
        responses.put("02", R.string.response_02);
        responses.put("03", R.string.response_03);
        responses.put("04", R.string.response_04);
        responses.put("05", R.string.response_05);
        responses.put("06", R.string.response_06);
        responses.put("07", R.string.response_07);
        responses.put("08", R.string.response_08);
        responses.put("09", R.string.response_09);
        responses.put("10", R.string.response_10);
        responses.put("11", R.string.response_11);

        responses.put("12", R.string.response_12);
        responses.put("13", R.string.response_13);
        responses.put("14", R.string.response_14);
        responses.put("15", R.string.response_15);
        responses.put("16", R.string.response_16);
        responses.put("17", R.string.response_17);
        responses.put("18", R.string.response_18);
        responses.put("19", R.string.response_19);
        responses.put("20", R.string.response_20);

        responses.put("21", R.string.response_21);
        responses.put("22", R.string.response_22);
        responses.put("23", R.string.response_23);
        responses.put("24", R.string.response_24);
        responses.put("25", R.string.response_25);
        responses.put("26", R.string.response_26);
        responses.put("27", R.string.response_27);
        responses.put("28", R.string.response_28);

        responses.put("30", R.string.response_30);
        responses.put("31", R.string.response_31);
        responses.put("32", R.string.response_32);
        responses.put("33", R.string.response_33);
        responses.put("34", R.string.response_34);
        responses.put("35", R.string.response_35);
        responses.put("36", R.string.response_36);
        responses.put("37", R.string.response_37);
        responses.put("38", R.string.response_38);
        responses.put("39", R.string.response_39);

        responses.put("40", R.string.response_40);
        responses.put("41", R.string.response_41);
        responses.put("42", R.string.response_42);
        responses.put("43", R.string.response_43);
        responses.put("44", R.string.response_44);
        responses.put("45", R.string.response_45);

        responses.put("51", R.string.response_51);
        responses.put("52", R.string.response_52);
        responses.put("53", R.string.response_53);
        responses.put("54", R.string.response_54);
        responses.put("55", R.string.response_55);
        responses.put("56", R.string.response_56);
        responses.put("57", R.string.response_57);
        responses.put("58", R.string.response_58);
        responses.put("59", R.string.response_59);

        responses.put("60", R.string.response_60);
        responses.put("61", R.string.response_61);
        responses.put("62", R.string.response_62);
        responses.put("63", R.string.response_63);
        responses.put("64", R.string.response_64);
        responses.put("65", R.string.response_65);
        responses.put("66", R.string.response_66);
        responses.put("67", R.string.response_67);
        responses.put("68", R.string.response_68);

        responses.put("70", R.string.response_70);
        responses.put("71", R.string.response_71);
//        responses.put("72", R.string.response_72);
//        responses.put("73", R.string.response_73);
//        responses.put("74", R.string.response_74);
        responses.put("75", R.string.response_75);

        responses.put("76", R.string.response_76);
        responses.put("77", R.string.response_77);
        responses.put("78", R.string.response_78);
        responses.put("79", R.string.response_79);

        responses.put("80", R.string.response_80);
        responses.put("81", R.string.response_81);
        responses.put("82", R.string.response_82);
        responses.put("84", R.string.response_84);
        responses.put("85", R.string.response_85);
        responses.put("86", R.string.response_86);
        responses.put("87", R.string.response_87);
        responses.put("88", R.string.response_88);
        responses.put("89", R.string.response_89);

        responses.put("90", R.string.response_90);
        responses.put("91", R.string.response_91);
        responses.put("92", R.string.response_92);
        responses.put("93", R.string.response_93);
        responses.put("94", R.string.response_94);
        responses.put("95", R.string.response_95);
        responses.put("96", R.string.response_96);
        responses.put("97", R.string.response_97);
        responses.put("98", R.string.response_98);
        responses.put("99", R.string.response_99);


        responses.put("C1", R.string.response_C1);
        responses.put("C2", R.string.response_C2);
        responses.put("C5", R.string.response_C5);
        responses.put("C6", R.string.response_C6);
        responses.put("C9", R.string.response_C9);
        responses.put("CA", R.string.response_CA);
        responses.put("CB", R.string.response_CB);
        responses.put("CF", R.string.response_CF);

        responses.put("N1", R.string.response_N1);
        responses.put("1A", R.string.response_1A);

        responses.put("Q1", R.string.response_Q1);
        responses.put("Y1", R.string.response_Y1);
        responses.put("Z1", R.string.response_Z1);

        responses.put("Y2", R.string.response_Y2);
        responses.put("Z2", R.string.response_Z2);

        responses.put("Y3", R.string.response_Y3);
        responses.put("Z3", R.string.response_Z3);

//        responses.put("NA", R.string.response_NA);
//        responses.put("P0", R.string.response_P0);
//        responses.put("XY", R.string.response_XY);
//        responses.put("XX", R.string.response_XX);

        responses.put("ER", R.string.response_ER);
        responses.put("**", R.string.response_asterisk);

        responses.put("LE", R.string.response_ER);

        //Redeemed Response code
        responses.put("L1", R.string.response_L1);
        responses.put("L2", R.string.response_L2);
        responses.put("L3", R.string.response_L3);
        responses.put("L4", R.string.response_L4);
        responses.put("L5", R.string.response_L5);
    }


    /**
     * init方法必须调用， 一般放在应用启动的时候
     */
    public void init() {
        map = new HashMap<>();
        loadCode();
        for (String i : responses.keySet()) {
            String msg = findResponse(i);
            ResponseCode rspCode = new ResponseCode(i, msg);
            map.put(i, rspCode);
        }
    }

    public ResponseCode parse(String code) {
        init(); // FixMe (can't get Thai) KiTty Hot Fix
        ResponseCode rc = map.get(code);
        if (rc == null)
            return new ResponseCode(code, Utils.getString(R.string.err_undefine_info));
        return rc;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        String prefix = "";
        if (!code.equals("00")) {
            prefix = Utils.getString(R.string.prompt_err_code) + code + "\n";
        }
        return prefix + message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    private String findResponse(final String code) {
        Integer id = responses.get(code);
        if (id == null) {
            id = R.string.response_unknown;
        }
        return FinancialApplication.getApp().getString(id);
    }

    @Override
    public String toString() {
        return this.getCode() + "\n" + this.getMessage();
    }
}
