/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-4-25
 * Module Author: lixc
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.emv.clss;

import com.pax.eemv.entity.TagsTable;
import com.pax.eemv.enums.EKernelType;

import java.util.ArrayList;
import java.util.List;

class ClssDE55Tag {
    private int emvTag;
    private byte option;
    private int len;

    public static final byte DE55_MUST_SET = 0x10;// 必须存在
    public static final byte DE55_OPT_SET = 0x20;// 可选择存在
    public static final byte DE55_COND_SET = 0x30;// 根据条件存在

    public ClssDE55Tag(int emvTag, byte option, int len) {
        this.emvTag = emvTag;
        this.option = option;
        this.len = len;
    }

    public int getEmvTag() {
        return emvTag;
    }

    public void setEmvTag(int emvTag) {
        this.emvTag = emvTag;
    }

    public byte getOption() {
        return option;
    }

    public void setOption(byte option) {
        this.option = option;
    }

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }

    public static List<ClssDE55Tag> getClssDE55Tags(EKernelType kernelType) {
        switch (kernelType) {
            case MC:
                return genMCClssDE55Tags();
            case VIS:
                return genVISAClssDE55Tags();
            case PBOC:
                return genPBOCClssDE55Tags();
            case JCB:
                return genJCBClssDE55Tags();
            default:
                return genClssDE55Tags();
        }
    }

    // clss sale tags list
    public static List<ClssDE55Tag> genClssDE55Tags() {
        List<ClssDE55Tag> clssDE55Tags = new ArrayList<>();
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.TRACK2, DE55_OPT_SET, 0));
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.APP_PAN, DE55_OPT_SET, 0));
        clssDE55Tags.add(new ClssDE55Tag(0x5F24, DE55_OPT_SET, 0));
        clssDE55Tags.add(new ClssDE55Tag(0x5F2A, DE55_MUST_SET, 0));
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.PAN_SEQ_NO, DE55_OPT_SET, 0));
        clssDE55Tags.add(new ClssDE55Tag(0x82, DE55_MUST_SET, 0));
        clssDE55Tags.add(new ClssDE55Tag(0x84, DE55_MUST_SET, 0));
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.TVR, DE55_MUST_SET, 0));
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.TRANS_DATE, DE55_MUST_SET, 0));
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.TSI, DE55_OPT_SET, 0));
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.TRANS_TYPE, DE55_MUST_SET, 0));
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.AMOUNT, DE55_MUST_SET, 0));
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.AMOUNT_OTHER, DE55_MUST_SET, 0));
        clssDE55Tags.add(new ClssDE55Tag(0x9F08, DE55_OPT_SET, 0));
        clssDE55Tags.add(new ClssDE55Tag(0x9F09, DE55_OPT_SET, 0));
        clssDE55Tags.add(new ClssDE55Tag(0x9F10, DE55_OPT_SET, 0));
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.COUNTRY_CODE, DE55_MUST_SET, 0));
        clssDE55Tags.add(new ClssDE55Tag(0x9F1E, DE55_OPT_SET, 0));
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.APP_CRYPTO, DE55_MUST_SET, 0));
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.CRYPTO, DE55_MUST_SET, 0));
        clssDE55Tags.add(new ClssDE55Tag(0x9F33, DE55_MUST_SET, 0));
        clssDE55Tags.add(new ClssDE55Tag(0x9F34, DE55_MUST_SET, 0));
        clssDE55Tags.add(new ClssDE55Tag(0x9F35, DE55_OPT_SET, 0));
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.ATC, DE55_MUST_SET, 0));
        clssDE55Tags.add(new ClssDE55Tag(0x9F37, DE55_MUST_SET, 0));
        clssDE55Tags.add(new ClssDE55Tag(0x9F41, DE55_OPT_SET, 0));
        clssDE55Tags.add(new ClssDE55Tag(0x9F5B, DE55_OPT_SET, 0));
        return clssDE55Tags;
    }

    /**
     * DE55 M/Chip Mastercard Spec.
     * @return
     */
    public static List<ClssDE55Tag> genMCClssDE55Tags() {
        List<ClssDE55Tag> clssDE55Tags = new ArrayList<>();
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.CURRENCY_CODE, DE55_MUST_SET, 0));//0x5F2A
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.PAN_SEQ_NO, DE55_COND_SET, 0));//0x5F34
        clssDE55Tags.add(new ClssDE55Tag(0x82, DE55_MUST_SET, 0));//0x82
        clssDE55Tags.add(new ClssDE55Tag(0x84, DE55_MUST_SET, 0));//0x84
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.TVR, DE55_MUST_SET, 0));//0x95
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.TRANS_DATE, DE55_MUST_SET, 0));//0x9A
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.TRANS_TYPE, DE55_MUST_SET, 0));//0x9C
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.AMOUNT, DE55_MUST_SET, 0));//0x9F02
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.AMOUNT_OTHER, DE55_COND_SET, 0));//0x9F03
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.APP_VER, DE55_MUST_SET, 0));//0x9F09
        clssDE55Tags.add(new ClssDE55Tag(0x9F10, DE55_MUST_SET, 0));//0x9F10
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.COUNTRY_CODE, DE55_MUST_SET, 0));//0x9F1A
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.INTER_DEV_NUM, DE55_COND_SET, 0));//0x9F1E
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.APP_CRYPTO, DE55_MUST_SET, 0));//0x9F26
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.CRYPTO, DE55_MUST_SET, 0));//0x9F27
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.TERMINAL_CAPABILITY, DE55_MUST_SET, 0));//0x9F33
        clssDE55Tags.add(new ClssDE55Tag(0x9F34, DE55_MUST_SET, 0));//0x9F34
        clssDE55Tags.add(new ClssDE55Tag(0x9F35, DE55_OPT_SET, 0));//0x9F35
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.ATC, DE55_MUST_SET, 0));//0x9F36
        clssDE55Tags.add(new ClssDE55Tag(0x9F37, DE55_MUST_SET, 0));//0x9F37
        return clssDE55Tags;
    }

    /**
     * DE55 M/Chip Visa Spec.
     * @return
     */
    public static List<ClssDE55Tag> genVISAClssDE55Tags() {
        List<ClssDE55Tag> clssDE55Tags = new ArrayList<>();
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.TRACK2, DE55_MUST_SET, 0));//0x57
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.APP_PAN, DE55_OPT_SET, 0));
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.CURRENCY_CODE, DE55_MUST_SET, 0));//0x5F2A
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.PAN_SEQ_NO, DE55_COND_SET, 0));//0x5F34
        clssDE55Tags.add(new ClssDE55Tag(0x82, DE55_MUST_SET, 0));//0x82
        clssDE55Tags.add(new ClssDE55Tag(0x84, DE55_MUST_SET, 0));//0x84
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.TVR, DE55_MUST_SET, 0));//0x95
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.TRANS_DATE, DE55_MUST_SET, 0));//0x9A
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.TRANS_TYPE, DE55_MUST_SET, 0));//0x9C
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.AMOUNT, DE55_MUST_SET, 0));//0x9F02
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.AMOUNT_OTHER, DE55_COND_SET, 0));//0x9F03
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.APP_VER, DE55_MUST_SET, 0));//0x9F09
        clssDE55Tags.add(new ClssDE55Tag(0x9F10, DE55_MUST_SET, 0));//0x9F10
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.COUNTRY_CODE, DE55_MUST_SET, 0));//0x9F1A
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.INTER_DEV_NUM, DE55_COND_SET, 0));//0x9F1E
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.APP_CRYPTO, DE55_MUST_SET, 0));//0x9F26
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.CRYPTO, DE55_MUST_SET, 0));//0x9F27
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.TERMINAL_CAPABILITY, DE55_MUST_SET, 0));//0x9F33
        clssDE55Tags.add(new ClssDE55Tag(0x9F34, DE55_MUST_SET, 0));//0x9F34
        clssDE55Tags.add(new ClssDE55Tag(0x9F35, DE55_OPT_SET, 0));//0x9F35
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.ATC, DE55_MUST_SET, 0));//0x9F36
        clssDE55Tags.add(new ClssDE55Tag(0x9F37, DE55_MUST_SET, 0));//0x9F37
        clssDE55Tags.add(new ClssDE55Tag(0x9F6E, DE55_COND_SET, 0));//0x9F6E
        clssDE55Tags.add(new ClssDE55Tag(0x9F7C, DE55_COND_SET, 0));//0x9F7C
        return clssDE55Tags;
    }

    /**
     * DE55 M/Chip PBOC Spec.
     * @return
     */
    public static List<ClssDE55Tag> genPBOCClssDE55Tags() {
        List<ClssDE55Tag> clssDE55Tags = new ArrayList<>();
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.TRACK2, DE55_MUST_SET, 0));//0x57 -- Trans. Online only
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.APP_PAN, DE55_MUST_SET, 0));//0x5A
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.CURRENCY_CODE, DE55_MUST_SET, 0));//0x5F2A
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.PAN_SEQ_NO, DE55_COND_SET, 0));//0x5F34
        clssDE55Tags.add(new ClssDE55Tag(0x82, DE55_MUST_SET, 0));//0x82
        clssDE55Tags.add(new ClssDE55Tag(0x84, DE55_MUST_SET, 0));//0x84
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.TVR, DE55_MUST_SET, 0));//0x95
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.TRANS_DATE, DE55_MUST_SET, 0));//0x9A
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.TRANS_TYPE, DE55_MUST_SET, 0));//0x9C
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.AMOUNT, DE55_MUST_SET, 0));//0x9F02
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.AMOUNT_OTHER, DE55_MUST_SET, 0));//0x9F03
        clssDE55Tags.add(new ClssDE55Tag(0x9F10, DE55_MUST_SET, 0));//0x9F10
        clssDE55Tags.add(new ClssDE55Tag(0x9F1F, DE55_COND_SET, 0));//0x9F1F -- Trans. Online only
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.COUNTRY_CODE, DE55_MUST_SET, 0));//0x9F1A
        clssDE55Tags.add(new ClssDE55Tag(0x9F24, DE55_COND_SET, 0));//0x9F24
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.APP_CRYPTO, DE55_MUST_SET, 0));//0x9F26
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.CRYPTO, DE55_MUST_SET, 0));//0x9F27
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.TERMINAL_CAPABILITY, DE55_MUST_SET, 0));//0x9F33
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.ATC, DE55_MUST_SET, 0));//0x9F36
        clssDE55Tags.add(new ClssDE55Tag(0x9F37, DE55_MUST_SET, 0));//0x9F37
        clssDE55Tags.add(new ClssDE55Tag(0x9F63, DE55_COND_SET, 0));//0x9F63
        return clssDE55Tags;
    }

    /**
     * DE55 EMV/Legacy/MagStripe JCB Spec.
     * EMV/Legacy: 9F02,9F03,9F26,82,5F34,9F36,5F20,9F34,9F27,9F10,9F1A,95,9F1F,57,5F2A,9A,9F21,9C,9F37
     * MagStripe: 9F02,9F03,5F20,9F34,9F1A,9F1F,57,5F2A,9A,9F21,9C
     * @return
     */
    public static List<ClssDE55Tag> genJCBClssDE55Tags() {
        List<ClssDE55Tag> clssDE55Tags = new ArrayList<>();
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.AMOUNT, DE55_MUST_SET, 0));//0x9F02
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.AMOUNT_OTHER, DE55_MUST_SET, 0));//0x9F03
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.APP_CRYPTO, DE55_MUST_SET, 0));//0x9F26
        clssDE55Tags.add(new ClssDE55Tag(0x82, DE55_MUST_SET, 0));//0x82
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.PAN_SEQ_NO, DE55_COND_SET, 0));//0x5F34
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.ATC, DE55_MUST_SET, 0));//0x9F36
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.CARD_HOLDER_NAME, DE55_COND_SET, 0));//0x5F20
        clssDE55Tags.add(new ClssDE55Tag(0x9F34, DE55_MUST_SET, 0));//0x9F34
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.CRYPTO, DE55_MUST_SET, 0));//0x9F27
        clssDE55Tags.add(new ClssDE55Tag(0x9F10, DE55_MUST_SET, 0));//0x9F10
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.COUNTRY_CODE, DE55_MUST_SET, 0));//0x9F1A
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.TVR, DE55_MUST_SET, 0));//0x95
        clssDE55Tags.add(new ClssDE55Tag(0x9F1F, DE55_COND_SET, 0));//0x9F1F
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.TRACK2, DE55_MUST_SET, 0));//0x57
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.CURRENCY_CODE, DE55_MUST_SET, 0));//0x5F2A
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.TRANS_DATE, DE55_MUST_SET, 0));//0x9A
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.TRANS_TIME, DE55_MUST_SET, 0));//0x9F21
        clssDE55Tags.add(new ClssDE55Tag(TagsTable.TRANS_TYPE, DE55_MUST_SET, 0));//0x9C
        clssDE55Tags.add(new ClssDE55Tag(0x9F37, DE55_MUST_SET, 0));//0x9F37
        return clssDE55Tags;
    }
}
