/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-9-12
 * Module Author: laiyi
 * Description:
 *
 * ============================================================================
 */

package com.pax.pay.base.document;

import com.pax.appstore.DocumentBase;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.CardRange;
import com.pax.pay.base.Issuer;
import com.pax.pay.trans.model.AcqManager;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

public class CardRangeDocument extends DocumentBase {
    private List<CardRange> cardRangeList = new ArrayList<>();

    public CardRangeDocument(String filePath) {
        super(filePath);
    }

    @Override
    public int parse() {
        Document document = getDocument();
        if (document == null) {
            return -1;
        }

        cardRangeList.clear();
        NodeList root = document.getChildNodes();
        Node param = root.item(0);
        NodeList node = param.getChildNodes();
        int index;
        for (int i = 1; i < node.getLength(); i = index) {
            index = i;
            CardRange cardRange = new CardRange();
            //set name
            String text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            cardRange.setName(text);

            //set PanLength
            text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            try {
                cardRange.setPanLength(Integer.parseInt(text));
            } catch (NumberFormatException e) {
                return -1;
            }

            //set panRangeHigh
            text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            cardRange.setPanRangeHigh(text);

            //set panRangeLow
            text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            cardRange.setPanRangeLow(text);

            //set issuerName
            text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            cardRange.setIssuerName(text);

            //set issuer
            Issuer issuer = FinancialApplication.getAcqManager().findIssuer(cardRange.getIssuerName());
            if (issuer == null) {
                return -1;
            }
            cardRange.setIssuer(issuer);
            cardRangeList.add(cardRange);
        }

        return 0;
    }

    @Override
    public void save() {
        AcqManager acqManager = FinancialApplication.getAcqManager();
        for (CardRange cardRange : cardRangeList) {
            CardRange data = acqManager.findCardRange(Long.parseLong(cardRange.getPanRangeLow()), Long.parseLong(cardRange.getPanRangeHigh()));
            if (data == null) {
                acqManager.insertCardRange(cardRange);
            } else {
                data.update(cardRange);
                acqManager.updateCardRange(data);
            }
        }
        cardRangeList.clear();
    }
}
