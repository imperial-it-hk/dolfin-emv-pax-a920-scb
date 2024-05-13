package com.pax.pay.utils;

import java.util.Locale;

public enum UILanguage {
    ENGLISH("ENGLISH", new Locale("en", "US"), 0),
    THAI("THAI", new Locale("th", "TH"), 1);

    private String display;
    private Locale locale;
    private int intValue;

    UILanguage(final String display, final Locale locale, final int intValue) {
        this.display = display;
        this.locale = locale;
        this.intValue = intValue;
    }

    public String getDisplay() {
        return this.display;
    }

    public Locale getLocale() {
        return this.locale;
    }

    public int getIntValue() {
        return this.intValue;
    }
}
