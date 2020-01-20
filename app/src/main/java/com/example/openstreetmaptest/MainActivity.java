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
    private IMapController iMapController;


    //your items for polygon
    List<GeoPoint> geoPoints = new ArrayList<>();
    List<GeoPoint> points = new ArrayList<>();
    private float radius = 0;
    private boolean isSMSalreadySend = false;

    //gps location
    private LocationManager locManager;
    private LocationListener locListener;

    //Geopoints for testing
    private GeoPoint hochschule = new GeoPoint(49.867141, 8.638066);
    private GeoPoint actualGeoPoint = new GeoPoint(49.867141, 8.638066);
    private GeoPoint startGeoPoint = new GeoPoint(49.867141, 8.638066);
    private GeoPoint circleCenter;

    private Button okButton;
    private String test;

    private Polygon polygon;
    private Marker marker;

    ScaleBarOverlay mScaleBarOverlay;

    private LinearLayout radiusdialog;
    private EditText inputRadius;
    private EditText smsInput;
    private EditText messageInput;

    private MyLocationNewOverlay mLocationOverlay;
    private RotationGestureOverlay mRotationGestureOverlay;

    final SmsManager m = SmsManager.getDefault();
    private String phoneNumber = "+15555215554";
    //private String phoneNumber = "017650182055";
    private String messageText = "Your Ship Is Out Of Range!";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //start app in fullscreen mode without title bar
        startFullscreen();
        super.onCreate(savedInstanceState);

        //Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        //init LocationManager with Service Location
        locManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        //inflate and create the map
        setContentView(R.layout.main);
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);

        //Set overlays for the map
        //Zoom Buttons
        map.setBuiltInZoomControls(true);
        //Zoom with multi fingers
        map.setMultiTouchControls(true);
        //rotate
        mRotationGestureOverlay = new RotationGestureOverlay(ctx, map);
        mRotationGestureOverlay.setEnabled(true);
        //scalebar
        setScaleBar();
        //my location overlay
        mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(ctx),map);
        //mLocationOverlay.setPersonIcon();
        mLocationOverlay.enableMyLocation();

        //set the view of the map at start
        setStartView(actualGeoPoint,18.0);              //TODO start view
        //LocationListener erstellen

        locListener = new LocationListener() {
            @Override
            //when location changed (gps)
            public void onLocationChanged(Location location) {
                //Create GeoPoint
                actualGeoPoint = new GeoPoint(location.getLatitude(),location.getLongitude());
                //Add GeoPoint on Map
                addMarker(actualGeoPoint);          //uncomment for continious tracking
                geoPointInRadius();
                if(geoPointInRadius() == false && isSMSalreadySend == false){
                    sendSMS();
                    isSMSalreadySend = true;
                }
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

        //generate and init Polygon
        polygon = makeCircle(hochschule,radius,map);
        polygon.setStrokeWidth((float)5.0);

        map.getOverlays().clear();  //init
        getGPS();

        map.getOverlays().add(mScaleBarOverlay);
        map.getOverlays().add(mRotationGestureOverlay);
        //add my location
        map.getOverlays().add(mLocationOverlay);
        //map.getOverlays().add(polygon);
        //addMarker(hochschule);
        map.invalidate();

        final MapEventsReceiver mReceive = new MapEventsReceiver(){
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                return false;
            }
            @Override
            public boolean longPressHelper(GeoPoint p) {
                radius = (float) p.distanceToAsDouble(actualGeoPoint);
                updateCircle(actualGeoPoint,radius,map);
                return false;
            }
        };

        map.getOverlays().add(new MapEventsOverlay(mReceive));
        map.getOverlays().add(polygon);
        map.invalidate();
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

    public void setScaleBar(){
        //scalebar
        final DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        mScaleBarOverlay = new ScaleBarOverlay(map);
        mScaleBarOverlay.setCentred(true);
        //play around with these values to get the location on screen in the right place for your application
        mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 10);
    }
    public void setStartView(GeoPoint p,double d){
        iMapController = map.getController();
        iMapController.setZoom(d);
        iMapController.setCenter(p);
    }

    //Check permission results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 10:
                getGPS();
                break;
            case 20:
                sendSMS();
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
        //Init Geopoint with current gps location
        actualGeoPoint = mLocationOverlay.getMyLocation();
        locManager.requestLocationUpdates("gps", 5000, 0, locListener);
    }
    public void sendSMS(){
        //check permissions
        //When permission not granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            //when API >23
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //request permissions with code 20
                requestPermissions(new String[]{Manifest.permission.SEND_SMS}, 20);
            }
            return;
        }
        m.sendTextMessage(phoneNumber, null, messageText, null, null);
    }

    private Polygon makeCircle(GeoPoint geoPoint, double radius, MapView map){
        //reset geopints for circle
        geoPoints.clear();

        for(int i=0;i<360;i++){
            circleCenter = new GeoPoint(geoPoint.getLatitude(),geoPoint.getLongitude()).destinationPoint(radius,i);   //destination Point wegen der skalierung von Lat/long
            geoPoints.add(circleCenter);
        }
        polygon = new Polygon(map);

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

    public boolean geoPointInRadius(){
        if(actualGeoPoint.distanceToAsDouble(startGeoPoint)>radius){
            Toast.makeText(getBaseContext(),"ausserhalb", Toast.LENGTH_LONG).show();
            return false;
        }else{
            isSMSalreadySend = false;
            Toast.makeText(getBaseContext(),"innerhalb", Toast.LENGTH_LONG).show();
            return true;
        }
    }


    public void addMarker(GeoPoint center){
        marker = new Marker(map);
        marker.setPosition(center);
        marker.setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_BOTTOM);
        //marker.setTitle("Give it a title");
        map.getOverlays().add(marker);
        map.invalidate();
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

                    updateCircle(actualGeoPoint, radius, map);
                    dialog.dismiss();
                }
            }
        });
        dialog.show();
    }

    public void updateCircle(GeoPoint p, float radius, MapView map){
        map.getOverlays().remove(polygon);
        map.getOverlays().remove(marker);
        polygon = makeCircle(p,radius,map);
        polygon.setStrokeWidth((float)5.0);
        map.getOverlays().add(polygon);
        map.invalidate();
        Toast.makeText(getBaseContext(),"New Radius Set", Toast.LENGTH_LONG).show();
        startGeoPoint = p;
        addMarker(startGeoPoint);
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