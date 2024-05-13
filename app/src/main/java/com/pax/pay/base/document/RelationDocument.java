/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2016 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-9-13
 * Module Author: laiyi
 * Description:
 *
 * ============================================================================
 */

package com.pax.pay.base.document;

import com.pax.appstore.DocumentBase;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.AcqIssuerRelation;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.Issuer;
import com.pax.pay.trans.model.AcqManager;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

public class RelationDocument extends DocumentBase {
    private List<AcqIssuerRelation> relationList = new ArrayList<>();

    public RelationDocument(String filePath) {
        super(filePath);
    }

    @Override
    public int parse() {
        Document document = getDocument();
        if (document == null) {
            return -1;
        }

        relationList.clear();
        NodeList root = document.getChildNodes();
        Node param = root.item(0);
        NodeList node = param.getChildNodes();
        int index;
        for (int i = 1; i < node.getLength(); i = index) {
            index = i;
            AcqIssuerRelation relation = new AcqIssuerRelation();
            //set acq
            String text = node.item(i).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            Acquirer acquirer = FinancialApplication.getAcqManager().findAcquirer(text);
            if (acquirer == null) {
                return -1;
            }
            relation.setAcquirer(acquirer);

            //set issuer
            text = node.item(index).getTextContent();
            if (text == null || text.isEmpty()) {
                return -1;
            }
            index += 2;
            Issuer issuer = FinancialApplication.getAcqManager().findIssuer(text);
            if (issuer == null) {
                return -1;
            }
            relation.setIssuer(issuer);

            relationList.add(relation);
        }

        return 0;
    }

    @Override
    public void save() {
        AcqManager acqManager = FinancialApplication.getAcqManager();
        for (AcqIssuerRelation relation : relationList) {
            acqManager.bind(relation.getAcquirer(), relation.getIssuer());
        }
        relationList.clear();
    }
}
