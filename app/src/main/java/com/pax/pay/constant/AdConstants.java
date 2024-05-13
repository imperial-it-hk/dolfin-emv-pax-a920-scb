/*
 * ============================================================================
 * COPYRIGHT
 *              Pax CORPORATION PROPRIETARY INFORMATION
 *   This software is supplied under the terms of a license agreement or
 *   nondisclosure agreement with Pax Corporation and may not be copied
 *   or disclosed except in accordance with the terms in that agreement.
 *      Copyright (C) 2017 - ? Pax Corporation. All rights reserved.
 * Module Date: 2017-2-28
 * Module Author: Kim.L
 * Description:
 *
 * ============================================================================
 */
package com.pax.pay.constant;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * temp workaround for store AD info, can be replaced with real-time data easily
 */
public class AdConstants {
    private static final Map<String, String> AD = new LinkedHashMap<>();

    static {
        AD.put("http://www.paxsz.com/testimg/about_banner.png", "http://www.paxsz.com");
        AD.put("http://www.paxsz.com/Upload/banner/Banner_creative-15515726077.jpg", "http://www.pax.com.cn/bfcp_list_other.aspx?CateID=124");
        AD.put("http://www.paxsz.com/Upload/banner/Homepage_02-11164232229.jpg", "http://www.pax.com.cn/solutions_detail.aspx?id=675");
    }

    private AdConstants() {

    }

    public static Map<String, String> getAd() {
        return AD;
    }
}
