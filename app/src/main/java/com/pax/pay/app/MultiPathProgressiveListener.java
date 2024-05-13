package com.pax.pay.app;

import com.pax.dal.entity.ERoute;

public interface MultiPathProgressiveListener {
    void onStart();
    void onError(final ERoute route, final String ipAddress);
    void onFinish(final boolean result);
}
