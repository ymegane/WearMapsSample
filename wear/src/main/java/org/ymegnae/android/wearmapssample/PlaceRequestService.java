package org.ymegnae.android.wearmapssample;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.ymegnae.android.wearmapssample.event.BusProvider;
import org.ymegnae.android.wearmapssample.event.FailurePlaceRequestEvent;
import org.ymegnae.android.wearmapssample.event.SuccessPlaceRequestEvent;

import java.util.concurrent.TimeUnit;

/**
 * Place要求Service
 */
public class PlaceRequestService extends IntentService {
    private static final String TAG = PlaceRequestService.class.getSimpleName();

    private static final int CONNECT_TIMEOUT_MS = 10000;
    public static final String REQUEST_PLACES = "/request/places";

    public PlaceRequestService() {
        super(TAG);
    }

    public static void startPlaceRequestService(Context context) {
        Intent intent = new Intent(context, PlaceRequestService.class);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        ConnectionResult result = googleApiClient.blockingConnect(CONNECT_TIMEOUT_MS,
                TimeUnit.MILLISECONDS);

        try {
            if (!result.isSuccess()) {
                Log.w(TAG, "Failed to connect to GoogleApiClient.");
                BusProvider.getInstance().post(new FailurePlaceRequestEvent());
                return;
            }

            final NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
            if (nodes.getNodes().isEmpty()) {
                Log.w(TAG, "Failed to connect to GoogleApiClient.");
                BusProvider.getInstance().post(new FailurePlaceRequestEvent());
                return;
            }

            for (Node node : nodes.getNodes()) {
                MessageApi.SendMessageResult result1 = Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), REQUEST_PLACES, null).await();
                Log.d(TAG, "result:" + result1.getStatus().getStatus());
            }
            BusProvider.getInstance().post(new SuccessPlaceRequestEvent());
        } finally {
            googleApiClient.disconnect();
        }
    }
}
