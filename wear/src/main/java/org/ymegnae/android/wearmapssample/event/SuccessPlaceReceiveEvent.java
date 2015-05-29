package org.ymegnae.android.wearmapssample.event;

import android.support.annotation.NonNull;

import org.ymegnae.android.wearmapssample.common.Place;

import java.util.ArrayList;
import java.util.List;

public class SuccessPlaceReceiveEvent {
    List<Place> placeList;

    public SuccessPlaceReceiveEvent(List<Place> placeList) {
        this.placeList = placeList;
    }

    @NonNull
    public List<Place> getPlaceList() {
        if (placeList == null) {
            return new ArrayList<>(0);
        }
        return placeList;
    }
}
