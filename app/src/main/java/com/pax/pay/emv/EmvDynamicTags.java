package com.pax.pay.emv;

/**
 * Created by SORAYA S on 29-Jun-18.
 */

public class EmvDynamicTags {

    private int tag;
    private byte[] value;
    private String typeLength;
    private int lenTag;

    public EmvDynamicTags(int tag, String typeLength, int lenTag) {
        this.tag = tag;
        this.typeLength = typeLength;
        this.lenTag = lenTag;
    }

    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    public String getTypeLength() {
        return typeLength;
    }

    public void setTypeLength(String typeLength) {
        this.typeLength = typeLength;
    }

    public int getLenTag() {
        return lenTag;
    }

    public void setLenTag(int lenTag) {
        this.lenTag = lenTag;
    }
}
