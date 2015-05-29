package org.ymegnae.android.wearmapssample;

import android.Manifest;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.DismissOverlayView;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.otto.Subscribe;

import org.ymegnae.android.wearmapssample.common.Place;
import org.ymegnae.android.wearmapssample.event.BusProvider;
import org.ymegnae.android.wearmapssample.event.FailurePlaceReceiveEvent;
import org.ymegnae.android.wearmapssample.event.FailurePlaceRequestEvent;
import org.ymegnae.android.wearmapssample.event.SuccessPlaceReceiveEvent;
import org.ymegnae.android.wearmapssample.event.SuccessPlaceRequestEvent;

import java.util.List;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends WearableActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    private MapFragment mapFragment;
    private GoogleMap googleMap;
    private DismissOverlayView dismissOverlayView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        BusProvider.getInstance().register(this);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                initViews(stub);
            }
        });
    }

    private void initViews(WatchViewStub stub) {
        dismissOverlayView = (DismissOverlayView) stub.findViewById(R.id.dismiss_overlay);
        dismissOverlayView.setIntroText(R.string.dismiss_message);
        dismissOverlayView.showIntroIfNecessary();

        mapFragment =
                (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        mapFragment.onEnterAmbient(ambientDetails);
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
        mapFragment.onExitAmbient();
    }

    @Override
    protected void onDestroy() {
        BusProvider.getInstance().unregister(this);
        super.onDestroy();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        MainActivityPermissionsDispatcher.initGoogleMapsSettingsWithCheck(this);
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        dismissOverlayView.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    void initGoogleMapsSettings() {
        this.googleMap.setOnMapLongClickListener(this);

        this.googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        this.googleMap.getUiSettings().setCompassEnabled(true);
        this.googleMap.setMyLocationEnabled(true);
        this.googleMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                if (googleMap.getMyLocation() == null) {
                    LatLng mayPlace = new LatLng(location.getLatitude(), location.getLongitude());
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mayPlace, 15));
                }
            }
        });

        // 周辺検索を要求
        PlaceRequestService.startPlaceRequestService(getApplication());
    }

    @Subscribe
    public void onPlaceRequestSuccess(SuccessPlaceRequestEvent event) {
        Log.d(TAG, "onPlaceRequestSuccess");
    }

    @Subscribe
    public void onPlaceRequestFailure(FailurePlaceRequestEvent event) {
        Log.w(TAG, "onPlaceRequestFailure");

    }

    @Subscribe
    public void onPlaceReceiveSuccess(SuccessPlaceReceiveEvent event) {
        final List<Place> placeList = event.getPlaceList();
        int n = placeList.size();
        Log.d(TAG, "onPlaceReceiveSuccess " + n);

        if (n == 0) {
            Log.d(TAG, "onPlaceReceiveSuccess place empty");
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (Place place : placeList) {
                    googleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(place.getLat(), place.getLon()))
                            .title(place.getName()));
                }
            }
        });
    }

    @Subscribe
    public void onPlaceReceiveFailure(FailurePlaceReceiveEvent event) {
        Log.w(TAG, "onPlaceReceiveFailure");
    }
}
