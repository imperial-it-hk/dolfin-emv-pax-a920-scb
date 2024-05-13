package com.pax.pay.utils;

import com.pax.dal.entity.ERoute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoutingTable {
    private static RoutingTable self;

    private Map<String, ERoute> table;

    private RoutingTable() {
        table = new HashMap<>();
    }

    public static RoutingTable getInstance() {
        if (self == null) {
            self = new RoutingTable();
        }

        return self;
    }

    public void clear() {
        table.clear();
    }

    public void add(String ipAddress, ERoute route) {
        if (!table.containsKey(ipAddress)) {
            table.put(ipAddress, route);
        }
    }

    public boolean contains(String ipAddress) {
        boolean isContains = table.containsKey(ipAddress);

        return isContains;
    }

    public ERoute get(String ipAddress) {
        ERoute route = null;

        if (table.containsKey(ipAddress)) {
            route = table.get(ipAddress);
        }

        return route;
    }

    public List<String> getKeys() {
        String[] keys = table.keySet().toArray(new String[table.keySet().size()]);

        List<String> keyList = new ArrayList<>();
        if (keys != null) {
            for (String key: keys) {
                keyList.add(key);
            }
        }

        return keyList;
    }
}
