package org.ymegnae.android.wearmapssample;

import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * 位置情報の取得リクエストService
 */
public class PlaceRequestReceiveService extends WearableListenerService {
    private static final String TAG = PlaceRequestReceiveService.class.getSimpleName();

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);

        Log.d(TAG, "onMessageReceived " + messageEvent.getPath());
        if (messageEvent.getPath().equals(PlaceSearchService.REQUEST_PLACES)) {
            PlaceSearchService.startPlaceSearchService(getApplicationContext());
        }
    }
}
