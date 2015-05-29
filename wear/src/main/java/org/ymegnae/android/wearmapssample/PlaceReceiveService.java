package org.ymegnae.android.wearmapssample;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.ymegnae.android.wearmapssample.common.Place;
import org.ymegnae.android.wearmapssample.event.BusProvider;
import org.ymegnae.android.wearmapssample.event.FailurePlaceReceiveEvent;
import org.ymegnae.android.wearmapssample.event.SuccessPlaceReceiveEvent;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Place受信Service
 */
public class PlaceReceiveService extends WearableListenerService {
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(PlaceRequestService.REQUEST_PLACES)) {
            if (messageEvent.getData() == null) {
                BusProvider.getInstance().post(new FailurePlaceReceiveEvent());
                return;
            }
            String appJson = new String(messageEvent.getData());
            Type listType = new TypeToken<List<Place>>() {}.getType();
            List<Place> placeList = new Gson().fromJson(appJson, listType);
            BusProvider.getInstance().post(new SuccessPlaceReceiveEvent(placeList));
        }
    }
}
