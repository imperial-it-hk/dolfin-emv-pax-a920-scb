/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-1-6
 * Module Author: Frose.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.trans.model;

import androidx.annotation.NonNull;

import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.pax.abl.utils.PanUtils;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.base.AcqIssuerRelation;
import com.pax.pay.base.Acquirer;
import com.pax.pay.base.CardRange;
import com.pax.pay.base.Issuer;
import com.pax.pay.base.TransTypeMapping;
import com.pax.pay.constant.Constants;
import com.pax.pay.db.AcqDb;
import com.pax.pay.trans.transmit.TransactionIPAddressCollection;
import com.pax.pay.utils.Utils;
import com.pax.settings.SysParam;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import th.co.bkkps.utils.Log;

public class AcqManager {

    private static final String TAG = "AcqManager";

    private static AcqManager acqmanager;
    private static AcqDb acqDbHelper;
    private Acquirer acquirer;

    public static synchronized AcqManager getInstance() {
        if (acqmanager == null) {
            acqmanager = new AcqManager();
            init();
        }
        return acqmanager;
    }

    public static synchronized AcqManager getInstance(boolean forceInit) {
        if (acqmanager == null) {
            acqmanager = new AcqManager();
        }
        if (forceInit) {
            init();
        }
        return acqmanager;
    }

    public void setCurAcq(Acquirer acq) {
        acquirer = acq;
    }

    public Acquirer getCurAcq() {
        return acquirer;
    }

    public boolean isIssuerSupported(final Acquirer acquirer, final Issuer issuer) {
        try {
            List<Issuer> issuers = acqDbHelper.lookupIssuersForAcquirer(acquirer);
            return issuers.contains(issuer);
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return false;
    }

    public boolean isIssuerSupported(final Issuer issuer) {
        try {
            Acquirer acquirer = FinancialApplication.getAcqManager().mapAcquirerByIssuer(issuer);
            if (acquirer == null || !acquirer.isEnable())
                return false;

            List<Issuer> issuers = acqDbHelper.lookupIssuersForAcquirer(acquirer);
            for (Issuer tmp : issuers) {
                if (tmp.getName().equals(issuer.getName())) {
                    return true;
                }
            }
            return false;
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return false;
    }

    public Issuer findIssuerByPan(final String pan) {
        if (pan == null || pan.isEmpty())
            return null;
        CardRange cardRange = acqDbHelper.findCardRange(pan);
        if (cardRange == null) {
            return null;
        } else {
            return cardRange.getIssuer();
        }
    }

    public List<Issuer> findAllIssuerByPan(final String pan) {
        if (pan == null || pan.isEmpty())
            return null;
        List<CardRange> cardRange = acqDbHelper.findAllCardRange(pan);
        if (cardRange == null) {
            return null;
        } else {
            List<Issuer> issuers = new ArrayList<>();
            for (CardRange c : cardRange) {
                boolean isFound = false;
                for (Issuer i : issuers) {
                    if (i.getName().equalsIgnoreCase(c.getIssuer().getName())) {
                        isFound = true;
                        break;
                    }
                }
                if (!isFound) issuers.add(c.getIssuer());
            }
            return issuers;
        }
    }

    public List<Issuer> findAllIssuerByPan(final String pan, final String issuerBrand) {
        if (pan == null || pan.isEmpty())
            return null;

        Log.i(TAG, "findAllIssuerByPan[pan=" + PanUtils.maskCardNo(pan, Constants.DEF_PAN_MASK_PATTERN) + ",issuerBrand=" + issuerBrand + "]");

        List<CardRange> cardRanges;
        if (issuerBrand != null) {
            cardRanges = findCardRange(pan, issuerBrand);
        } else {
            cardRanges = findCardRange(pan);
        }

        List<Issuer> issuers = null;
        if (cardRanges != null) {
            //find all issuer by selecting the smallest range of card range
            CardRange card = cardRanges.get(0);
            issuers = new ArrayList<>(Collections.singletonList(card.getIssuer()));
            List<String> issuerName = new ArrayList<>(Collections.singletonList(card.getIssuerName()));
            long minPan = Utils.parseLongSafe(card.getPanRangeHigh(), 0) - Utils.parseLongSafe(card.getPanRangeLow(), 0);

            for (CardRange c : cardRanges) {
                long currPan = Utils.parseLongSafe(c.getPanRangeHigh(), 0) - Utils.parseLongSafe(c.getPanRangeLow(), 0);
                if (currPan == minPan && !issuerName.contains(c.getIssuerName())) {
                    issuers.add(c.getIssuer());
                    issuerName.add(c.getIssuerName());
                } else if (currPan < minPan) {
                    minPan = currPan;
                    issuers.clear();
                    issuerName.clear();
                    issuers.add(c.getIssuer());
                    issuerName.add(c.getIssuerName());
                }
            }
        }
        if (issuers == null || issuers.isEmpty()) {
            /* P'Kitty approved: If UP/TPN/JCB/AMEX, no need to check card range */
            if (issuerBrand != null && !issuerBrand.equals(Constants.ISSUER_VISA) && !issuerBrand.equals(Constants.ISSUER_MASTER)) {
                return FinancialApplication.getAcqManager().findIssuerByBrand(issuerBrand);
            }
            /* End */
        }

        if (issuerBrand == null && issuers != null && issuers.size() > 1) { //for swipe /key-in / fallback
            Iterator<Issuer> itIssuer = issuers.iterator();
            while (itIssuer.hasNext()) {
                Issuer is = itIssuer.next();
                if (is.getIssuerBrand().equals(Constants.ISSUER_BRAND_TBA)) {
                    itIssuer.remove();
                }
            }
        }

        return issuers;
    }

    public Issuer findIssuerByPan(final Acquirer acquirer, final String pan) {
        try {
            List<Issuer> issuers = acqDbHelper.lookupIssuersForAcquirer(acquirer);
            Issuer issuer = findIssuerByPan(pan);
            if (issuer != null && issuers.contains(issuer)) {
                return issuer;
            }
        } catch (SQLException e) {
            Log.e(TAG, "", e);
        }
        return null;
    }

    public void insertAcquirer(final Acquirer acquirer) {
        acqDbHelper.insertAcquirer(acquirer);
    }

    public Acquirer findAcquirer(final String acquirerName) {
        return acqDbHelper.findAcquirer(acquirerName);
    }

    public int findCurrentMaxKeyIndex() {
        return acqDbHelper.findCurrentMaxKeyId();
    }

    public Acquirer findActiveAcquirer(final String acquirerName) {
        return acqDbHelper.findActiveAcquirer(acquirerName);
    }

    public List<Acquirer> findAcquirer(final List<String> acquirerName) {
        return acqDbHelper.findAcquirer(acquirerName);
    }

    public Acquirer findActiveAcquirerTleHostName(final String supportTleHostName) {
        return acqDbHelper.findActiveAcquirerWithSpecificTleBankName(supportTleHostName);
    }

    public Map<String, Acquirer> findTleBankHostByActiveHost(){
        try {
            List<Acquirer> acquirerList = acqDbHelper.findAllAcquirers();
            Map<String, Acquirer> tleBankList = new HashMap<>();
            for (Acquirer localAcq : acquirerList) {
                if (localAcq.isEnable()
                        && localAcq.isEnableTle()
                        && localAcq.getTleBankName()!=null
                        && !tleBankList.containsKey(localAcq.getTleBankName())){
                    tleBankList.put(localAcq.getTleBankName(), localAcq);
                }
            }
            return tleBankList;

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }

        return new HashMap<>();
    }

    public int setRelatedIPAddress (@NonNull String srcAcquirerName,@NonNull String desAcquirerName) {
        int result = -1;
        if (srcAcquirerName!=null && desAcquirerName!=null) {
            int currentIpAddrID = 0 ;
            String ipaddress = null;
            int portNumb = -1;
            Acquirer srcAcquirer = FinancialApplication.getAcqManager().findAcquirer(srcAcquirerName);
            Acquirer desAcquirer = FinancialApplication.getAcqManager().findAcquirer(desAcquirerName);

            if (srcAcquirer!=null && desAcquirer!=null) {
                currentIpAddrID = srcAcquirer.getCurrentBackupAddressId();
                switch (currentIpAddrID) {
                    case 0 :
                        ipaddress = srcAcquirer.getIp();
                        portNumb  = srcAcquirer.getPort();
                    case 1 :
                        ipaddress = srcAcquirer.getIpBak1();
                        portNumb  = srcAcquirer.getPortBak1();
                    case 2 :
                        ipaddress = srcAcquirer.getIpBak2();
                        portNumb  = srcAcquirer.getPortBak2();
                    case 3 :
                        ipaddress = srcAcquirer.getIpBak3();
                        portNumb  = srcAcquirer.getPortBak3();
                }
            }

            int ipaddressId = -1;
            if (desAcquirer.getIp().equals(ipaddress) && desAcquirer.getPort()==portNumb) {
                ipaddressId = 0;
            } else if (desAcquirer.getIpBak1().equals(ipaddress) && desAcquirer.getPortBak1()==portNumb) {
                ipaddressId = 2;
            } else if (desAcquirer.getIpBak2().equals(ipaddress) && desAcquirer.getPortBak2()==portNumb) {
                ipaddressId = 3;
            } else if (desAcquirer.getIpBak3().equals(ipaddress) && desAcquirer.getPortBak3()==portNumb) {
                ipaddressId = 4;
            }

            if (ipaddressId != -1) {
                desAcquirer.setCurrentIpAddressID(ipaddressId);
                FinancialApplication.getAcqManager().updateAcquirer(desAcquirer);
                result = TransResult.SUCC;
            }
        }

        return result;
    }

    public List<Acquirer> findAllAcquirers() {
        return acqDbHelper.findAllAcquirers();
    }

    public List<Acquirer> findEnableAcquirer() {
        return acqDbHelper.findEnableAcquirers();
    }

    public static List<Acquirer> findEnableAcquirers() {
        return acqDbHelper.findEnableAcquirers();
    }

    public List<Acquirer> findEnableAcquirersWithSortMode(boolean ascMode) {
        return acqDbHelper.findEnableAcquirersBySortMode(ascMode);
    }

    public static List<Acquirer> findEnableAcquirersExcept(String exceptAcqName) {
        return acqDbHelper.findEnableAcquirers(exceptAcqName);
    }

    public List<Acquirer> findEnableAcquirersExcept(List<String> exceptAcqName) {
        return acqDbHelper.findEnableAcquirers(exceptAcqName);
    }

    public static List<Acquirer> findEnableAcquirersWithEnableERM() {
        return acqDbHelper.findEnableAcquirersWithEnableERM();
    }

    public int findCountEnableAcquirersWithEnableERM() {
        return acqDbHelper.findCountEnableAcquirersWithEnableERM();
    }

    public static List<Acquirer> findCountEnableAcquirersWithERMStatus(boolean erm_acquirer_upload_flag) {
        return acqDbHelper.findAcquirersWithERMStatus(erm_acquirer_upload_flag);
    }

    public int findAycapAcquirer(String NII) {
        return acqDbHelper.findAycapAcquirer(NII);
    }

    public static List<Acquirer> findEnableAcquirersWithKeyData() {
        return acqDbHelper.findEnableAcquirersWithKeyData();
    }

    public List<String> findActiveTLEAcquirerwithTleBankName() {
        List<Acquirer> acquirerList = this.findEnableAcquirer();
        List<String> tleBankName = new ArrayList<>();
        if (acquirerList != null) {
            try {
                if (acquirerList.size() > 0) {
                    for (Acquirer localAcq : acquirerList) {
                        if (localAcq.isEnable() && localAcq.isEnableTle() && localAcq.getTleBankName() != null) {
                            if (!tleBankName.contains(localAcq.getTleBankName())) {
                                tleBankName.add(localAcq.getTleBankName());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return tleBankName;
    }

    public void updateAcquirer(final Acquirer acquirer) {
        acqDbHelper.updateAcquirer(acquirer);
    }

    public boolean updateAcquirerCurrentHostId(final Acquirer acquirer, int CurrentHostId) {
        boolean curAcqId = acqDbHelper.updateAcquirerCurrentHostId(acquirer, CurrentHostId);
        return curAcqId;
    }

    public int getAcquirerCurrentHostId(final Acquirer acquirer) {
        int returnIndex = acqDbHelper.getAcquirerCurrentHostId(acquirer);
        Log.d("BACKUP-IP", " getAcquirerCurrentHostId = [HostIndex=" + acquirer.getId() + "] : " + acquirer.getName());
        Log.d("BACKUP-IP", "   >> latest-use-ipaddress-index = " + returnIndex);
        return returnIndex;
    }

    public List<String[]> getAllTables() {
        return acqDbHelper.getDatabaseAllTables();
    }

    public void deleteAcquirer(final int id) {
        acqDbHelper.deleteAcquirer(id);
    }

    public void insertIssuer(final Issuer issuer) {
        acqDbHelper.insertIssuer(issuer);
    }

    public Issuer findIssuer(final String issuerName) {
        return acqDbHelper.findIssuer(issuerName);
    }

    public List<Issuer> findIssuerByBrand(final String issuerBrand) {
        return acqDbHelper.findIssuerByBrand(issuerBrand);
    }

    public List<Issuer> findIssuerByName(final String issuerName) {
        return acqDbHelper.findIssuerByName(issuerName);
    }

    public List<Issuer> findIssuerByAcquirerName(final String acquirerName) {
        return acqDbHelper.findIssuerByAcquirerName(acquirerName);
    }

    public List<Issuer> findAllIssuers() {
        return acqDbHelper.findAllIssuers();
    }

    public void bind(final Acquirer root, final Issuer issuer) {
        acqDbHelper.bind(root, issuer);
    }

    public void updateIssuer(final Issuer issuer) {
        acqDbHelper.updateIssuer(issuer);
    }

    public void deleteIssuer(final int id) {
        acqDbHelper.deleteIssuer(id);
    }

    public boolean deleteIssuer(List<Issuer> issuers) {
        return acqDbHelper.deleteIssuer(issuers);
    }

    public void insertCardRange(final CardRange cardRange) {
        acqDbHelper.insertCardRange(cardRange);
    }

    public void updateCardRange(final CardRange cardRange) {
        acqDbHelper.updateCardRange(cardRange);
    }

    public CardRange findCardRange(final long low, final long high) {
        return acqDbHelper.findCardRange(low, high);
    }

    public List<CardRange> findCardRange(final Issuer issuer) {
        return acqDbHelper.findCardRange(issuer);
    }

    public List<CardRange> findCardRange(final String pan) {
        List<CardRange> cardRanges = acqDbHelper.findAllCardRange(pan);
        if (cardRanges != null && !cardRanges.isEmpty()) {
            return filterActivedCardRange(cardRanges);
        }
        return null;
    }

    public List<CardRange> findAllCardRanges() {
        return acqDbHelper.findAllCardRanges();
    }

    public void deleteCardRange(final int id) {
        acqDbHelper.deleteCardRange(id);
    }

    public boolean deleteCardRange(List<CardRange> cardRanges) {
        return acqDbHelper.deleteCardRange(cardRanges);
    }

    public List<CardRange> findCardRange(final String pan, final String issuerBrand) {
        List<CardRange> cardRanges = acqDbHelper.findAllCardRange(pan, issuerBrand);
        if (cardRanges != null && !cardRanges.isEmpty()) {
            return filterActivedCardRange(cardRanges);
        }
        return null;
    }

    private List<CardRange> filterActivedCardRange(List<CardRange> list) {
        List<CardRange> fCardRanges = new ArrayList<>();
        for (CardRange c : list) {
            Issuer issuer = findIssuer(c.getIssuerName());
            if (issuer != null) {
                Acquirer acquirer = findAcquirer(issuer.getAcqHostName());
                if (acquirer != null && acquirer.isEnable()) {
                    fCardRanges.add(c);
                }
            }
        }
        if (fCardRanges.isEmpty()) {
            return null;
        }
        return fCardRanges;
    }

    private static void init() {
        acqDbHelper = AcqDb.getInstance();
        SysParam sysParam = FinancialApplication.getSysParam();
        if (sysParam != null) {
            String name = sysParam.get(SysParam.StringParam.ACQ_NAME);

            if (!"".equals(name)) {
                Acquirer acquirer = acqDbHelper.findAcquirer(name);
                if (acquirer != null) {
                    acqmanager.setCurAcq(acquirer);
                }
            }
        }
    }

    public Acquirer mapAcquirerByIssuer(final Issuer issuer) {
        Acquirer acquirer = null;

        if (issuer != null) {
            List<Acquirer> acquirers = acqDbHelper.findEnableAcquirers();
            String acqHostName = issuer.getAcqHostName();

            if (acqHostName != null) {
                for (Acquirer a : acquirers) {
                    if (a.getName().equalsIgnoreCase(acqHostName)) {
                        acquirer = a;
                    }
                }
            }
        }
        return acquirer;
    }

    public List<AcqIssuerRelation> findRelation(final Acquirer acquirer) {
        return acqDbHelper.findRelation(acquirer);
    }

    public List<AcqIssuerRelation> findAllRelations() {
        return acqDbHelper.findAllRelations();
    }

    public boolean deleteAcqIssuerRelations(List<AcqIssuerRelation> relations) {
        return acqDbHelper.deleteAcqIssuerRelations(relations);
    }

    public boolean insertTransMapping(final ETransType type, final Acquirer root, final Issuer issuer, final int priority) {
        return acqDbHelper.insertTransMapping(type, root, issuer, priority);
    }

    public TransTypeMapping findTransMapping(final ETransType type, final Issuer issuer, final int priority) {
        return acqDbHelper.findTransMapping(type, issuer, priority);
    }

    public List<TransTypeMapping> findTransMapping(final Acquirer acquirer) {
        return acqDbHelper.findTransMapping(acquirer);
    }

    public static void clearInstance() {
        acqmanager = null;
        acqDbHelper.clearInstance();
    }
}
