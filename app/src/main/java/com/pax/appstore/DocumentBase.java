/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-2-16
 * Module Author: laiyi
 * Description:
 *
 * ============================================================================
 */
package com.pax.appstore;

import th.co.bkkps.utils.Log;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public abstract class DocumentBase {
    private String filePath;

    public DocumentBase(String filePath) {
        this.filePath = DownloadManager.getInstance().getFilePath() + File.separator + filePath;
    }

    protected Document getDocument() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        try {
            db = dbf.newDocumentBuilder();
            return db.parse(new File(filePath));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            Log.e("DocumentBase", e.getMessage());
        }

        return null;
    }

    public boolean isExit() {
        return new File(filePath).exists();
    }

    public void delete() {
        new File(filePath).delete();
    }

    public abstract int parse();

    public abstract void save();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DocumentBase that = (DocumentBase) o;

        return filePath != null ? filePath.equals(that.filePath) : that.filePath == null;

    }

    @Override
    public int hashCode() {
        return filePath != null ? filePath.hashCode() : 0;
    }
}
