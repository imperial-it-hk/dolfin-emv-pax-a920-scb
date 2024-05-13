/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-8-28
 * Module Author: laiyi
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.base.document;

import com.pax.appstore.DocumentBase;
import com.pax.appstore.DownloadManager;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.trans.model.AcqManager;

import org.w3c.dom.Document;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AcqDocumentRead extends DocumentBase {
    private List<Acquirer> acquirerList = new ArrayList<>();
    String filePath;
    public AcqDocumentRead(String filePath) {
        super(filePath);
        this.filePath = filePath;
    }

    @Override
    public int parse() {
        Document document = getDocument();
        if (document == null) {
            return -1;
        }

        acquirerList.clear();
        filePath = DownloadManager.getInstance().getFilePath() + filePath;
        XmlPullParserFactory pullParserFactory;
        try{
            pullParserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = pullParserFactory.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(GetStreamFromXmlFileOnSDCard(filePath), null);

            //boolean inCustom = false;
            boolean aqcFound = false;
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.getName().equalsIgnoreCase("CUSTOM") ) {
                    //inCustom = true;

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        aqcFound = false;
                        if (parser.getEventType()== XmlPullParser.START_TAG && parser.getName().equals("Acquirers")){
                            aqcFound = true;
                        }
                        if (aqcFound) {
                            while  (eventType != XmlPullParser.END_DOCUMENT){
                                if (parser.getEventType()== XmlPullParser.START_TAG && parser.getName().equals("Acquire")){
                                    while  (eventType != XmlPullParser.END_DOCUMENT){
                                        if (parser.getEventType()== XmlPullParser.START_TAG && parser.getName().equals("AcquireInformation")){
                                            loadItem(parser);
                                        }
                                        eventType = parser.next();
                                    }
                                }
                                eventType = parser.next();
                            }
                        }else{
                            //break;
                        }
                        eventType = parser.next();
                    }
                }
                eventType = parser.next();
            }

        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void save() {
        AcqManager acqManager = FinancialApplication.getAcqManager();
        for (Acquirer acquirer : acquirerList) {
            Acquirer data = acqManager.findAcquirer(acquirer.getName());
            if (data == null) {
                acqManager.insertAcquirer(acquirer);
            } else {
                data.update(acquirer);
                acqManager.updateAcquirer(data);
            }
        }
        acquirerList.clear();
    }

    public InputStream GetStreamFromXmlFileOnSDCard(String filePath)
    {
        File file = new File(filePath);
        InputStream istr = null;
        try
        {
            istr = new FileInputStream(file);
        }
        catch (FileNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return istr;

    }

    private void loadItem(XmlPullParser parser) throws XmlPullParserException, IOException {

        int eventType = parser.getEventType();
        if ( eventType == XmlPullParser.START_TAG  && parser.getName().equalsIgnoreCase("AcquireInformation") ) {
            String name;
            Acquirer acq = new Acquirer();
            while ( eventType != XmlPullParser.END_TAG || 0 != parser.getName().compareTo("AcquireInformation") ) {
                switch (eventType){
                    case XmlPullParser.START_TAG:
                        name = parser.getName();
                        if (name.equals("AcquirerName")){
                            acq.setName(parser.nextText());
                        } else  if (name.equals("NII")){
                            acq.setNii(parser.nextText());
                        }else  if (name.equals("AcceptID")){
                            acq.setTerminalId(parser.nextText());
                        }
                        else  if (name.equals("MerchantID")){
                            acq.setMerchantId(parser.nextText());
                        }
                        break;
                    case XmlPullParser.END_TAG:
                }
                eventType = parser.next();
            }
            if (acq.getName() != null){
                acq.setCurrBatchNo(1);
                acq.setIp("172.17.0.190");
                acq.setPort(5555);
                acq.setEnableKeyIn(true);
                acq.setEnableQR(true);
                acq.setId(3);
                acquirerList.add(acq);
            }
        }
    }
}
