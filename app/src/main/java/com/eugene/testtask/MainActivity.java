package com.eugene.testtask;

import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.ArrayList;
import java.util.List;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener {

    private PermissionsManager permissionsManager;
    private MapView mapView;
    private MapboxMap mapboxMap;
    private static final String SOURCE_ID = "SOURCE_ID";
    private static final String ICON_ID = "ICON_ID";
    private static final String LAYER_ID = "LAYER_ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        setContentView(R.layout.activity_main);
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        MainActivity.this.mapboxMap = mapboxMap;

        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                enableLocation(style); //метод нахождения местоположения
                drawMarker(style); //метод добавки маркеров
            }
        });
    }

    @SuppressWarnings({"MissingPermission"})
    private void enableLocation(Style style) {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            LocationComponent locationComponent = mapboxMap.getLocationComponent();
            locationComponent.activateLocationComponent(LocationComponentActivationOptions.builder(this, style).build());
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.COMPASS);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            mapboxMap.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    enableLocation(style);
                }
            });
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    public void drawMarker(Style style) {
        LocationComponent locationComponent = mapboxMap.getLocationComponent();
        Location location = locationComponent.getLastKnownLocation();

        if (location != null) {
            double longitude = location.getLongitude(); //получаем долготу
            double latitude = location.getLatitude(); //получаем широту

            List<Feature> featureList = new ArrayList<>();
            featureList.add(Feature.fromGeometry(Point.fromLngLat(longitude, latitude)));

            double radius = 10000; //радиус в метрах
            int degreesBetweenPoints = 10; // угол между точками
            int numberOfPoints = 360 / degreesBetweenPoints; //количество точек
            // радиус земли в метрах 6371000.0
            double distRadians = radius / 6371000.0;
            double centerLatRadians = latitude * Math.PI / 180;
            double centerLonRadians = longitude * Math.PI / 180;

            for (int i = 0; i < numberOfPoints; i++) {
                double degrees = i * degreesBetweenPoints;
                double degreeRadians = degrees * Math.PI / 180;
                double pointLatRadians = Math.asin(Math.sin(centerLatRadians) * Math.cos(distRadians)
                        + Math.cos(centerLatRadians) * Math.sin(distRadians) * Math.cos(degreeRadians));
                double pointLonRadians = centerLonRadians + Math.atan2(Math.sin(degreeRadians)
                        * Math.sin(distRadians) * Math.cos(centerLatRadians), Math.cos(distRadians)
                        - Math.sin(centerLatRadians) * Math.sin(pointLatRadians));

                double pointLat = pointLatRadians * 180 / Math.PI;
                double pointLon = pointLonRadians * 180 / Math.PI;

                featureList.add(Feature.fromGeometry(Point.fromLngLat(pointLon, pointLat)));
            }

            style.addImage(ICON_ID, BitmapFactory.decodeResource(MainActivity.this.getResources(), R.drawable.mapbox_marker_icon_default));
            style.addSource(new GeoJsonSource(SOURCE_ID, FeatureCollection.fromFeatures(featureList)));
            style.addLayer(new SymbolLayer(LAYER_ID, SOURCE_ID).withProperties(iconImage(ICON_ID), iconAllowOverlap(true), iconIgnorePlacement(true)));
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
}
