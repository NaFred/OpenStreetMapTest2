package com.example.openstreetmaptest;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.views.MapController;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.OverlayItem;


import org.osmdroid.views.overlay.OverlayManager;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;


/**
 *
 */
public class MainActivity extends /*Activity*/AppCompatActivity {
    //Create Map
    MapView map = null;
    //MapEventsReceiver receiver;
    //Context context;

    //your items for polygon
    List<GeoPoint> geoPoints = new ArrayList<>();
    List<GeoPoint> points = new ArrayList<>();

    MapController mapController;

    GeoPoint hochschule = new GeoPoint(49.867141, 8.638066);
    //private MyLocationNewOverlay mLocationOverlay;
    private LocationManager locManager;
    private LocationListener locListener;




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //handle permissions first, before map is created. not depicted here

        //load/initialize the osmdroid configuration, this can be done
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without permissions
        //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name, abusing osm's tile servers will get you banned based on this string

        //init LocationManager with Service Location
        locManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        //inflate and create the map
        //setContentView(R.layout.activity_main);
        setContentView(R.layout.main);

        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        //default view Point
        IMapController mapController = map.getController();
        mapController.setZoom(18.0);

        //Zoom Buttons
        map.setBuiltInZoomControls(true);
        //Zoom with multi fingers
        map.setMultiTouchControls(true);


        mapController.setCenter(hochschule);
        addMarker(hochschule);

        //LocationListener erstellen
        locListener = new LocationListener() {
            @Override
            //when location changed (gps)
            public void onLocationChanged(Location location) {
                //Create GeoPoint
                GeoPoint geoPoint = new GeoPoint(location.getLatitude(),location.getLongitude());
                //Add GeoPoint on Map
                addMarker(geoPoint);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            //when gps is not enabled
            public void onProviderDisabled(String provider) {
                //Go to Setting for GPS enable
                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                //Start GPS
                startActivity(i);
            }
        };

        //generate Polygon
        Polygon polygon = makeCircle(hochschule,1,map);
        polygon.setStrokeWidth((float)5.0);

        map.getOverlays().clear();
        getGPS();

        map.getOverlays().add(polygon);
        addMarker(hochschule);
        map.invalidate();

        //map.setOnTouchListener(this);

    };
    //Check permission results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 10:
                getGPS();
                break;
            default:
                break;
        }
    }

    private void getGPS(){
        //check permissions
        //When permission not granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //when API >23
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //request permissions with code 10
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.INTERNET}
                        ,10);
            }
            return;
        }
        // this code won't execute IF permissions are not allowed, because in the line above there is return statement.
        locManager.requestLocationUpdates("gps", 5000, 0, locListener);
    }


    /*
    public OsmCircle(CircleOptions circleOptions, MapView osmMap) {
        this.map = osmMap;
        Polygon osmCircle = new Polygon();


        osmCircle.setStrokeWidth(circleOptions.getStrokeWidth());
        osmCircle.setFillColor(circleOptions.getFillColor());
        osmCircle.setStrokeColor(circleOptions.getStrokeColor());


        final double radius = circleOptions.getRadius();
        this.setR = circleOptions.getCenter();
        osmCircle.setRadius((float) radius);
        map.getOverlays().add(osmCircle);
    }
    public void setRadius(float radius) {
        int nrOfPoints = 360;
        ArrayList<GeoPoint> circlePoints = new ArrayList<GeoPoint>();
        for (float f = 0; f < nrOfPoints; f ++){
            circlePoints.add(new GeoPoint(centre.latitude , centre.longitude ).destinationPoint(radius, f));
        }
        osmCircle.setPoints(circlePoints);
    }
*/
    private Polygon makeCircle(GeoPoint geoPoint, double radius, MapView map){
        //reset geopints for circle
        geoPoints.clear();

        for(int i=0;i<360;i++){
            GeoPoint g = new GeoPoint(geoPoint.getLatitude(),geoPoint.getLongitude()).destinationPoint(radius,i);   //destination Point wegen der skalierung von Lat/long
            geoPoints.add(g);
        }
        Polygon polygon = new Polygon(map);

        polygon.setFillColor(Color.argb(50, 255,0,0));
        geoPoints.add(geoPoints.get(0));    //forces the loop to close
        polygon.setPoints(geoPoints);
        polygon.setTitle("AnkerArea");

        return polygon;
    }


    public void onResume(){
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    public void onPause(){
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }


    public void addMarker(GeoPoint center){
        Marker marker = new Marker(map);
        marker.setPosition(center);
        marker.setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_BOTTOM);
        marker.setTitle("Give it a title");
        //map.getOverlays().clear();
        map.getOverlays().add(marker);
        map.invalidate();
    }

//Marker overlay zerstören über GONE
//Callback fehlt
//map events receiver single tab true->event nicht weiter
    //alle aktionen

    /*
    public boolean onTouch(View v,MotionEvent ev) {
        switch (v.getId()){
            case R.id.map:
                if(ev.getAction() == MotionEvent.ACTION_UP){
                    Projection proj = map.getProjection();
                    IGeoPoint p = proj.fromPixels((int)ev.getX(),(int)ev.getY());
                    GeoPoint geo = new GeoPoint(p.getLatitude(),p.getLongitude());
                    addMarker(geo);
                    break;
                }
                break;
        }
        return true;
    }
*/
    /* Test zu on SingleTapConfirmed
    @Override public boolean onSingleTapConfirmed(final MotionEvent event, final MapView mapView){
        boolean touched = hitTest(event, mapView);
        if (touched){
            if (mOnMarkerClickListener == null){
                return onMarkerClickDefault(this, mapView);
            } else {
                return mOnMarkerClickListener.onMarkerClick(this, mapView);
            }
        } else
            return touched;
    }
    */

 /*
    private class myMapReceiver implements MapEventsReceiver{
       @Override
        public boolean singleTapConfirmedHelper(GeoPoint p) {
            return false;
        }

       @Override
        public boolean longPressHelper(GeoPoint p) {
            return false;
        }
    }*/
}