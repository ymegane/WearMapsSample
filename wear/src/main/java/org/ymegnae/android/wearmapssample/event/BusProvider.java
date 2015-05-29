package org.ymegnae.android.wearmapssample.event;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

public class BusProvider {
    private static final Bus BUS = new Bus(ThreadEnforcer.ANY);

    private BusProvider() {
    }

    public static Bus getInstance() {
        return BUS;
    }
}
