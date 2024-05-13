/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2016-11-30
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.utils;


import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import android.content.Context;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;

import th.co.bkkps.utils.Log;

public class CurrencyConverter {

    private static final String TAG = "CurrencyConv";
    private static final List<Locale> locales = new ArrayList<>();
    private static final List<Currency> currencies = new ArrayList<>();

    private static Locale defLocale = Locale.US;

    private static final String countryName = "Thai (Thailand)";

    static {
        Locale[] tempLocales = Locale.getAvailableLocales();
        for (Locale i : tempLocales) {
            try {
                if (i.getDisplayName(Locale.US).equals(countryName)) {
                    defLocale = i;
                    Locale.setDefault(defLocale);
                }

                CountryCode country = CountryCode.getByCode(i.getISO3Country());
                Currency.getInstance(i); // just for filtering
                if (country != null) {
                    locales.add(i);
                }
            } catch (IllegalArgumentException | MissingResourceException e) {
                //Log.e(TAG, "", e);
            }
        }

        Set<Currency> tempCurrencies = Currency.getAvailableCurrencies();
        currencies.addAll(tempCurrencies);
    }

    private CurrencyConverter() {
        //do nothing
    }

    public static List<Locale> getSupportedLocale() {
        return locales;
    }

    /**
     * @param countryName : {@see Locale#getDisplayName(Locale)}
     */
    public static Locale setDefCurrency(String countryName) {
        for (Locale i : locales) {
            if (i.getDisplayName(Locale.US).equals(countryName)) {
                if (!i.equals(defLocale)) {
                    defLocale = i;
                    Locale.setDefault(defLocale);
                }
                return defLocale;
            }
        }
        return defLocale;
    }

    public static Locale getDefCurrency() {
        return defLocale;
    }

    /**
     * @param amount
     * @return
     */
    public static String convert(Long amount) {
        return convert(amount, defLocale);
    }

    /**
     * @param amount
     * @param locale
     * @return
     */
    public static String convert(Long amount, Locale locale) {

        Currency currency = Currency.getInstance(locale);
        NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);

        if (formatter instanceof DecimalFormat) {
            DecimalFormat df = (DecimalFormat) formatter;
            // use local/default decimal symbols with original currency symbol
            DecimalFormatSymbols symbol = new DecimalFormat().getDecimalFormatSymbols();

            // if want to change back to Symbol change here!
            symbol.setCurrencySymbol(currency.getCurrencyCode());
            df.setDecimalFormatSymbols(symbol);
        }

        formatter.setMinimumFractionDigits(currency.getDefaultFractionDigits());
        formatter.setMaximumFractionDigits(currency.getDefaultFractionDigits());
        Long newAmount = amount < 0 ? -amount : amount; // AET-58
        String prefix = amount < 0 ? "-" : "";
        try {
            double amt = Double.valueOf(newAmount) / (Math.pow(10, currency.getDefaultFractionDigits()));
            return prefix + formatter.format(amt);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "", e);
        }
        return "";
    }

    public static String convertWithoutCurrency(Long amount, Locale locale) {
        Currency currency = Currency.getInstance(locale);
        NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
        formatter.setMinimumFractionDigits(currency.getDefaultFractionDigits());
        formatter.setMaximumFractionDigits(currency.getDefaultFractionDigits());
        Long newAmount = amount < 0 ? -amount : amount; // AET-58
        String prefix = amount < 0 ? "-" : "";
        try {
            double amt = Double.valueOf(newAmount) / (Math.pow(10, currency.getDefaultFractionDigits()));
            return prefix + String.format(locale, "%09.2f", amt);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "", e);
        }
        return "";
    }

    /**
     * @param amount
     * @return
     */
    public static String convertToHundred(Long amount) {
        return convertToHundred(amount, defLocale);
    }

    /**
     * @param amount
     * @param locale
     * @return
     */
    public static String convertToHundred (Long amount, Locale locale) {
        Currency currency = Currency.getInstance(locale);
        NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
        formatter.setMinimumFractionDigits(currency.getDefaultFractionDigits());
        formatter.setMaximumFractionDigits(currency.getDefaultFractionDigits());
        Long newAmount = amount < 0 ? -amount : amount; // AET-58
        String prefix = amount < 0 ? "-" : "";
        newAmount = newAmount * 10000;
        try {
            double amt = Double.valueOf(newAmount) / (Math.pow(10, currency.getDefaultFractionDigits()));
            return prefix + formatter.format(amt);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "", e);
        }
        return "";
    }

    public static String convert(Long amount, String numericCode) {
        Locale locale = getLocaleByCurrencyNumeric(Utils.parseIntSafe(numericCode, 0));

        Currency currency = Currency.getInstance(locale);
        NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);

        if (formatter instanceof DecimalFormat) {
            DecimalFormat df = (DecimalFormat) formatter;
            // use local/default decimal symbols with original currency symbol
            DecimalFormatSymbols symbol = new DecimalFormat().getDecimalFormatSymbols();

            // if want to change back to Symbol change here!
            symbol.setCurrencySymbol(currency.getCurrencyCode());
            df.setDecimalFormatSymbols(symbol);
        }

        formatter.setMinimumFractionDigits(currency.getDefaultFractionDigits());
        formatter.setMaximumFractionDigits(currency.getDefaultFractionDigits());
        Long newAmount = amount < 0 ? -amount : amount; // AET-58
        String prefix = amount < 0 ? "-" : "";
        try {
            double amt = Double.valueOf(newAmount) / (Math.pow(10, currency.getDefaultFractionDigits()));
            return prefix + formatter.format(amt);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "", e);
        }
        return "";
    }

    public static Long parse(String formatterAmount) {
        return parse(formatterAmount, defLocale);
    }

    public static Long parse(String formatterAmount, Locale locale) {
        Currency currency = Currency.getInstance(locale);
        NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
        formatter.setMinimumFractionDigits(currency.getDefaultFractionDigits());
        formatter.setMaximumFractionDigits(currency.getDefaultFractionDigits());
        try {
            Number num = formatter.parse(formatterAmount);

            return Math.round(num.doubleValue() * Math.pow(10, currency.getDefaultFractionDigits()));
        } catch (ParseException | NumberFormatException e) {
            Log.e(TAG, "", e);
        }
        return 0L;
    }

    public static Long parse(double dAmount) {
        return parse(dAmount, defLocale);
    }

    public static Long parse(double dAmount, Locale locale) {
        Currency currency = Currency.getInstance(locale);
        NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
        formatter.setMinimumFractionDigits(currency.getDefaultFractionDigits());
        formatter.setMaximumFractionDigits(currency.getDefaultFractionDigits());
        try {
            return Math.round(dAmount * Math.pow(10, currency.getDefaultFractionDigits()));
        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return 0L;
    }

    private static Locale getLocaleByCurrencyNumeric(int numericCode) {
        CountryCode[] countries = CountryCode.values();
        for (CountryCode c : countries) {
            if (c.getCurrencyNumeric() == numericCode) {
                for (Locale l : locales) {
                    if (l.getCountry().equals(c.getAlpha2())) {
                        return l;
                    }
                }
            }
        }
        return getDefCurrency();
    }

    public static String getCurrencySymbol(String numericCode, boolean isSymbolRequired) {
        Locale locale = getLocaleByCurrencyNumeric(Utils.parseIntSafe(numericCode, 0));
        Currency currency = Currency.getInstance(locale);
        if (isSymbolRequired) {
            return Objects.requireNonNull(currency).getCurrencyCode() + " (" + Objects.requireNonNull(currency).getSymbol() + ")";
        }
        return Objects.requireNonNull(currency).getCurrencyCode();
    }

    public static String getCurrencySymbol(Locale locale, boolean isSymbolRequired) {
        Currency currency = Currency.getInstance(locale);
        if (isSymbolRequired) {
            return Objects.requireNonNull(currency).getCurrencyCode() + " (" + Objects.requireNonNull(currency).getSymbol() + ")";
        }
        return Objects.requireNonNull(currency).getCurrencyCode();
    }

    public static Locale getLocaleByCountry(String countryName) {
        for (Locale i : locales) {
            if (i.getCountry().equals(countryName)) {
                return  i;
            }
        }
        return null;
    }

    public static String formatter(Locale locale, Context context) {
        if (locale != null) {
            Currency currency = Currency.getInstance(locale);
            if (currency != null) {
                Locale loc = context.getResources().getConfiguration().locale;
                return "(" + currency.getSymbol(loc) + ") " + currency.getDisplayName(loc);
            }
        }
        return "";
    }
}
