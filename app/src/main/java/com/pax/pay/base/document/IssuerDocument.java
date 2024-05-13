/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-8-31
 * Module Author: laiyi
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.base.document;

import com.pax.appstore.DocumentBase;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.Issuer;
import com.pax.pay.constant.Constants;
import com.pax.pay.trans.model.AcqManager;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

public class IssuerDocument extends DocumentBase {
    private List<Issuer> issuerList = new ArrayList<>();

    public IssuerDocument(String filePath) {
        super(filePath);
    }

    @Override
    public int parse() {
        Document document = getDocument();
        if (document == null) {
            return -1;
        }

        issuerList.clear();
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
            Issuer issuer = new Issuer(text);

            //set FloorLimit
            text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            issuer.setIssuerName(text);

            //set FloorLimit
            text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            issuer.setIssuerID(Integer.parseInt(text));

            //set FloorLimit
            text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            issuer.setFloorLimit(Long.parseLong(text));

            //set AdjustPercent
            text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            issuer.setAdjustPercent(Float.parseFloat(text));

            //set panMaskPattern
            text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            switch (text) {
                case "6-4":
                    issuer.setPanMaskPattern(Constants.DEF_PAN_MASK_PATTERN);
                    break;
                case "4-6":
                    issuer.setPanMaskPattern(Constants.PAN_MASK_PATTERN1);
                    break;
                case "all mask":
                    issuer.setPanMaskPattern(Constants.PAN_MASK_PATTERN2);
                    break;
                case "no mask":
                    issuer.setPanMaskPattern(Constants.PAN_MASK_PATTERN3);
                    break;
                default:
                    issuer.setPanMaskPattern(Constants.DEF_PAN_MASK_PATTERN);
                    break;
            }

            //set isEnableAdjust
            text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            issuer.setEnableAdjust("Y".equals(text));

            //set isEnableOffline
            text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            issuer.setEnableOffline("Y".equals(text));

            //set isAllowExpiry
            text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            issuer.setAllowExpiry("Y".equals(text));

            //set isAllowManualPan
            text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            issuer.setAllowManualPan("Y".equals(text));

            //set isAllowCheckExpiry
            text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            issuer.setAllowCheckExpiry("Y".equals(text));

            //set isAllowPrint
            text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            issuer.setAllowPrint("Y".equals(text));

            //set isAllowCheckPanMod10
            text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            issuer.setAllowCheckPanMod10("Y".equals(text));

            //set isRequirePIN
            text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            issuer.setRequirePIN("Y".equals(text));

            //set isRequireMaskExpiry
            text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            issuer.setRequireMaskExpiry("Y".equals(text));

            issuerList.add(issuer);
        }

        return 0;
    }

    @Override
    public void save() {
        AcqManager acqManager = FinancialApplication.getAcqManager();
        for (Issuer issuer : issuerList) {
            Issuer data = acqManager.findIssuer(issuer.getName());
            if (data == null) {
                acqManager.insertIssuer(issuer);
            } else {
                data.update(issuer);
                acqManager.updateIssuer(data);
            }
        }
        issuerList.clear();
    }
}
