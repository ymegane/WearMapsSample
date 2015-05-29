package org.ymegnae.android.wearmapssample;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.PlaceFilter;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;

import org.ymegnae.android.wearmapssample.common.Place;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Place APIで周辺検索するService
 */
public class PlaceSearchService extends IntentService {
    private static final String TAG = PlaceSearchService.class.getSimpleName();

    private static final int CONNECT_TIMEOUT_MS = 10000;
    public static final String REQUEST_PLACES = "/request/places";

    public PlaceSearchService() {
        super(TAG);
    }

    /**
     * 周辺検索をリクエスト
     * @param context context
     */
    public static void startPlaceSearchService(Context context) {
        Intent intent = new Intent(context, PlaceSearchService.class);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();
        ConnectionResult result = googleApiClient.blockingConnect(CONNECT_TIMEOUT_MS,
                TimeUnit.MILLISECONDS);
        if (!result.isSuccess()) {
            Log.w(TAG, "Failed to connect to GoogleApiClient.");
            return;
        }

        // Place search
        String places = searchCurrentSearch(googleApiClient);
        Log.d(TAG, "Finish CurrentSearch. " + places);

        // Wearに送信
        sendWearable(googleApiClient, places);

        googleApiClient.disconnect();
    }

    /**
     * Place APIから周辺検索する
     * @param googleApiClient GoogleApiClient
     * @return 検索結果
     */
    private String searchCurrentSearch(GoogleApiClient googleApiClient) {

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final List<Place> placeList = new ArrayList<>();

        PlaceFilter filter = new PlaceFilter(true, null);
        PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi
                .getCurrentPlace(googleApiClient, filter);

        result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
            @Override
            public void onResult(PlaceLikelihoodBuffer likelyPlaces) {
                Log.d(TAG, "searchCurrentSearch result count" + likelyPlaces.getCount());

                for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                    Log.i(TAG, String.format("Place '%s' Type '%d' Id '%s' has likelihood: %g",
                            placeLikelihood.getPlace().getName(),
                            placeLikelihood.getPlace().getPlaceTypes().get(0),
                            placeLikelihood.getPlace().getId(),
                            placeLikelihood.getLikelihood()));
                    com.google.android.gms.location.places.Place place = placeLikelihood.getPlace();
                    int type = place.getPlaceTypes().get(0);
                    if (type == com.google.android.gms.location.places.Place.TYPE_ESTABLISHMENT ||
                            type == com.google.android.gms.location.places.Place.TYPE_BUS_STATION) {
                        continue;
                    }
                    placeList.add(new Place()
                            .setName(place.getName().toString())
                            .setAddress(place.getAddress().toString())
                            .setLat(place.getLatLng().latitude)
                            .setLon(place.getLatLng().longitude));
                }
                likelyPlaces.release();
                countDownLatch.countDown();
            }
        });

        try {
            countDownLatch.await(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return "";
        }

        return new Gson().toJson(placeList);
    }

    /**
     * Wearable Deviceにメッセージを送る
     * @param googleApiClient GoogleApiClient
     * @param message 送信メッセージ
     */
    private void sendWearable(GoogleApiClient googleApiClient, String message) {

        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
        if (nodes.getNodes().isEmpty()) {
            Log.w(TAG, "Failed to connect.");
            return;
        }
        for (Node node : nodes.getNodes()) {
            MessageApi.SendMessageResult result1 =
                    Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), REQUEST_PLACES,
                            message.getBytes()).await();
            if (!result1.getStatus().isSuccess()) {
                Log.w(TAG, "Failed to send.");
            }
        }
    }
}
