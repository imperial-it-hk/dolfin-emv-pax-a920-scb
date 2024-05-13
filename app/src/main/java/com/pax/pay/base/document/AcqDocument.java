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
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Acquirer;
import com.pax.pay.trans.model.AcqManager;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

public class AcqDocument extends DocumentBase {
    private List<Acquirer> acquirerList = new ArrayList<>();

    public AcqDocument(String filePath) {
        super(filePath);
    }

    @Override
    public int parse() {
        Document document = getDocument();
        if (document == null) {
            return -1;
        }

        acquirerList.clear();
        NodeList root = document.getChildNodes();
        Node param = root.item(0);
        NodeList node = param.getChildNodes();
        int index;
        for (int i = 1; i < node.getLength(); i = index) {
            index = i;
            //set name
            String text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            Acquirer acq = new Acquirer(text);

            //set nii
            text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            acq.setNii(text);

            //set terminal id
            text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            acq.setTerminalId(text);

            //set merchant id
            text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            acq.setMerchantId(text);

            //set batch no
            text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            try {
                acq.setCurrBatchNo(Integer.parseInt(text));
            } catch (NumberFormatException e) {
                return -1;
            }

            //set ip
            text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            acq.setIp(text);

            //set port
            text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            try {
                acq.setPort(Integer.parseInt(text));
            } catch (NumberFormatException e) {
                return -1;
            }

            //set ipbak1
            text = node.item(index).getTextContent();
            if (text != null && !text.isEmpty()) {
                acq.setIpBak1(text);
            }
            index += 2;

            //set portbak1
            text = node.item(index).getTextContent();
            if (text != null && !text.isEmpty()) {
                acq.setPortBak1(Short.parseShort(text));
            }
            index += 2;

            //set ipbak2
            text = node.item(index).getTextContent();
            if (text != null && !text.isEmpty()) {
                acq.setIpBak2(text);
            }

            index += 2;
            //set portbak2
            text = node.item(index).getTextContent();
            if (text != null && !text.isEmpty()) {
                acq.setPortBak2(Short.parseShort(text));
            }
            index += 2;

            //set tcp timeout
            text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            try {
                acq.setTcpTimeOut(Integer.parseInt(text));
            } catch (NumberFormatException e) {
                return -1;
            }

            //set wireless timeout
            text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            try {
                acq.setWirelessTimeOut(Integer.parseInt(text));
            } catch (NumberFormatException e) {
                return -1;
            }

            //set isDisableTrickFeed
            text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            acq.setDisableTrickFeed("Y".equals(text));

            //set enable keyIn
            text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            acq.setEnableKeyIn("Y".equals(text));

            //set enable QR
            text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            acq.setEnableQR("Y".equals(text));

            acquirerList.add(acq);
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
}
