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




public class MainActivity extends /*Activity*/AppCompatActivity {
    MapView map = null;
    //MapEventsReceiver receiver;
    //Context context;

    //your items
    List<GeoPoint> geoPoints = new ArrayList<>();
    //add your points here
    //GeoPoint one = new GeoPoint(49.7,6.0);
    //GeoPoint two = new GeoPoint(49.867141,8.7);
    //GeoPoint three = new GeoPoint(49.8,8.78);




    MapController mapController;
    GeoPoint hochschule = new GeoPoint(49.867141, 8.638066);
    private MyLocationNewOverlay mLocationOverlay;
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

        locManager = (LocationManager) getSystemService(LOCATION_SERVICE);


//map listener
        //map events overlay
        //on single tab auf map events overlay und events weiterreichen


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

        locListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                GeoPoint geoPoint = new GeoPoint(location.getLatitude(),location.getLongitude());
                addMarker(geoPoint);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {
                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(i);
            }
        };


        Polygon polygon = makeCircle(hochschule,1,map);

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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.INTERNET}
                        ,10);
            }
            return;
        }
        // this code won't execute IF permissions are not allowed, because in the line above there is return statement.
        locManager.requestLocationUpdates("gps", 5000, 0, locListener);
    }



    private Polygon makeCircle(GeoPoint geoPoint, double radius, MapView map){
        //reset geopints for circle
        geoPoints.clear();

        double radius2 =radius *0.002;
        double xplus= (geoPoint.getLatitude()+radius2);
        double xminus= (geoPoint.getLatitude()-radius2);
        double yplus = (geoPoint.getLongitude()+radius2);
        double yplusdouble = (geoPoint.getLongitude()+(2*radius2));
        double yminus = (geoPoint.getLongitude()-radius2);
        double yminusdouble = (geoPoint.getLongitude()-(2*radius2));
        double test = geoPoint.getLatitude();


        GeoPoint one = new GeoPoint(xplus,yminus);
        GeoPoint two = new GeoPoint(xplus,yplus);
        GeoPoint three = new GeoPoint(test,yplusdouble);
        GeoPoint four = new GeoPoint(xminus,yplus);
        GeoPoint five = new GeoPoint(xminus,yminus);
        GeoPoint six = new GeoPoint(test,yminusdouble);

        geoPoints.add(one);
        geoPoints.add(two);
        geoPoints.add(three);
        geoPoints.add(four);
        geoPoints.add(five);
        geoPoints.add(six);
        Polygon polygon = new Polygon(map);
        polygon.setFillColor(Color.argb(75, 255,0,0));
        geoPoints.add(geoPoints.get(0));    //forces the loop to close
        polygon.setPoints(geoPoints);
        //polygon.setTitle("A sample polygon");

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