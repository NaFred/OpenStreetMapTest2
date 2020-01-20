package com.example.openstreetmaptest;

import android.Manifest;
import android.app.Dialog;
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
import android.telephony.SmsManager;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import org.osmdroid.api.IMapController;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.views.MapController;

import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;


import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.ScaleBarOverlay;

import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;




/**
 *
 */
public class MainActivity extends /*Activity*/AppCompatActivity {
    final Context ctx = this;
    //Create Map
    MapView map = null;
    MapController mapController;

    //your items for polygon
    List<GeoPoint> geoPoints = new ArrayList<>();
    List<GeoPoint> points = new ArrayList<>();
    private float radius = 0;

    //gps location
    private LocationManager locManager;
    private LocationListener locListener;

    //Geopoints for testing
    private GeoPoint hochschule = new GeoPoint(49.867141, 8.638066);
    private GeoPoint actualGeoPoint = new GeoPoint(49.867141, 8.638066);
    private GeoPoint constGeoPoint = new GeoPoint(49.867141, 8.638066);

    private Button okButton;
    private String test;

    private LinearLayout radiusdialog;
    private EditText inputRadius;
    private EditText smsInput;
    private EditText messageInput;

    private MyLocationNewOverlay mLocationOverlay;

    final SmsManager m = SmsManager.getDefault();
    private String phoneNumber = "+15555215554";
    private String messageText = "Hallo, MaxWie geht es dir?";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //start app in fullscreen mode without title bar
        startFullscreen();
        super.onCreate(savedInstanceState);

        //Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without permissions
        //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name, abusing osm's tile servers will get you banned based on this string

        //init LocationManager with Service Location
        locManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        //inflate and create the map
        setContentView(R.layout.main);
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        //default view Point
        final IMapController mapController = map.getController();
        mapController.setZoom(18.0);

        //Zoom Buttons
        map.setBuiltInZoomControls(true);
        //Zoom with multi fingers
        map.setMultiTouchControls(true);
        //rotate
        RotationGestureOverlay mRotationGestureOverlay = new RotationGestureOverlay(ctx, map);
        mRotationGestureOverlay.setEnabled(true);



        //Request permission for sms
        //requestPermissions(new String[]{Manifest.permission.SEND_SMS}
         //       ,20);
        //m.sendTextMessage(phoneNumber, null, messageText, null, null);


        //scalebar
        //final Context context = this.getActivity();
        final DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        ScaleBarOverlay mScaleBarOverlay = new ScaleBarOverlay(map);
        mScaleBarOverlay.setCentred(true);
        //play around with these values to get the location on screen in the right place for your application
        mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 10);


        //my location overlay
        mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(ctx),map);
        //mLocationOverlay.setPersonIcon();
        mLocationOverlay.enableMyLocation();



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
                addMarker(geoPoint);          //uncomment for continious tracking
                constGeoPoint = geoPoint;       //for testing
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
        final Polygon polygon = makeCircle(hochschule,radius,map);
        polygon.setStrokeWidth((float)5.0);

        map.getOverlays().clear();  //init
        getGPS();
        map.getOverlays().add(mScaleBarOverlay);
        map.getOverlays().add(mRotationGestureOverlay);
        //add my location
        map.getOverlays().add(mLocationOverlay);
        map.getOverlays().add(polygon);
        //addMarker(hochschule);
        map.invalidate();

        //////////////////////////////////////////////////////////////////////////////
        final MapEventsReceiver mReceive = new MapEventsReceiver(){
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                //Toast.makeText(getBaseContext(),p.getLatitude() + " - "+p.getLongitude(), Toast.LENGTH_LONG).show();
                //removeAllMarker();
                map.getOverlays().remove(map.getOverlays().size()-1);
                //addMarker(p);
                actualGeoPoint = p;

                clickInRadius();

                return false;
            }
            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };
        //clearAllMarker();

        map.getOverlays().add(new MapEventsOverlay(mReceive));
        map.getOverlays().add(polygon);
        map.invalidate();

        //map.setOnTouchListener(this);
    };


    public void startFullscreen(){
        //start app without Title
        //requestWindowFeature(getWindow().FEATURE_NO_TITLE);
        requestWindowFeature(getWindow().FEATURE_NO_TITLE);
        getSupportActionBar().show();
        //start app in fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    //Check permission results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 10:
                getGPS();
                break;
            //case 20:
               // sendSMS();
                //break;
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

    public void sendSMS(){
        requestPermissions(new String[]{Manifest.permission.SEND_SMS}
               ,20);
        m.sendTextMessage(phoneNumber, null, messageText, null, null);
    }

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
        //polygon.setTitle("AnkerArea");
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

    public void clickInRadius(){
        GeoPoint p = new GeoPoint(mLocationOverlay.getMyLocation());
        if(p.distanceToAsDouble(hochschule)>radius){
            Toast.makeText(getBaseContext(),"ausserhalb", Toast.LENGTH_LONG).show();
        }else{

            Toast.makeText(getBaseContext(),"innerhalb", Toast.LENGTH_LONG).show();
        }
    }


    public void addMarker(GeoPoint center){
        Marker marker = new Marker(map);
        marker.setPosition(center);
        marker.setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_BOTTOM);
        marker.setTitle("Give it a title");
        map.getOverlays().add(marker);
        map.invalidate();
    }

    public void removeAllMarker(){
        map.getOverlays().clear();
        //map.invalidate();
    }


    public void openRadiusDialog(final Context context) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.radiusdialog);

        //inputRadius.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        inputRadius = (EditText) dialog.findViewById(R.id.radiusInput);     //dialog muss gegeben sein, weil sonst der View nicht klar ist und es in eine null object reference läuft
        //https://blog.codeonion.com/2015/10/03/android-how-to-fix-null-object-reference-error/
        okButton = dialog.findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (inputRadius.getText().toString().isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Fehler", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    radius = Float.parseFloat(inputRadius.getText().toString());
                    Toast.makeText(getApplicationContext(),String.valueOf(radius), Toast.LENGTH_SHORT).show();

                    Polygon polygon2 = makeCircle(actualGeoPoint,radius,map);
                    polygon2.setStrokeWidth((float)5.0);
                    map.getOverlays().add(polygon2);
                    //addMarker(hochschule);
                    map.invalidate();

                    dialog.dismiss();
                }
            }
        });
        dialog.show();
    }

    public void openPersonalDialog(final Context context) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.personallayout);

        //inputRadius.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        smsInput = (EditText) dialog.findViewById(R.id.smsInput);     //dialog muss gegeben sein, weil sonst der View nicht klar ist und es in eine null object reference läuft
        //https://blog.codeonion.com/2015/10/03/android-how-to-fix-null-object-reference-error/
        messageInput = (EditText) dialog.findViewById(R.id.smsMessageInput);
        okButton = dialog.findViewById(R.id.okButton2);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (smsInput.getText().toString().isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Fehler", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    phoneNumber = smsInput.getText().toString();
                    messageText = messageInput.getText().toString();
                    Toast.makeText(getApplicationContext(),String.valueOf(phoneNumber), Toast.LENGTH_SHORT).show();
                    sendSMS();
                    dialog.dismiss();
                }
            }
        });
        dialog.show();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.position:
                return true;
            case R.id.radius:
                openRadiusDialog(ctx);
                return true;
            case R.id.resetPosition:
                return true;
            case R.id.personalInfo:
                openPersonalDialog(ctx);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}