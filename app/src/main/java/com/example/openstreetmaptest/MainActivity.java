package com.example.openstreetmaptest;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

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
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;



public class MainActivity extends /*Activity*/AppCompatActivity{
    MapView map = null;
    //MapEventsReceiver receiver;
    //Context context;

    MapController mapController;
    GeoPoint hochschule = new GeoPoint(49.867141, 8.638066);
    private MyLocationNewOverlay mLocationOverlay;


    @Override public void onCreate(Bundle savedInstanceState){
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
        //map listener
        //map events overlay
        //on single tab auf map events overlay und events weiterreichen


        //map.setOnTouchListener(this);
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
        map.getOverlays().clear();
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