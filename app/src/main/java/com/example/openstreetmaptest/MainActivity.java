package com.example.openstreetmaptest;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @brief The "Ankerwache" app is used to capture the position of the mobile device using openstreetmap.
 * To do this, a radius around the given position is defined by clicking on the map or using a menu. If this is left, a warning is sent to a second device via SMS.
 * If the battery level drops under 15% an SMS is send.
 */
public class MainActivity extends /*Activity*/AppCompatActivity {
    final Context ctx = this;   //get the context of the app
    //Create Map
    MapView map = null;     //declare an emtpy map object
    private IMapController iMapController;      //declare a controller for the map

    //items for polygon
    List<GeoPoint> geoPoints = new ArrayList<>();   //list for all points the polygon is created with
    private float radius = 0;       //radius of the polygon
    private boolean commingFromOutside = false;     //variable for checking if the last position is outside the circle

    //gps location
    private LocationManager locManager;     //create location manager for the gps
    private LocationListener locListener;   //create location listener for the position change

    //Geopoints for testing
    public GeoPoint actualGeoPoint = new GeoPoint(1.0, 1.0);
    private GeoPoint startGeoPoint = new GeoPoint(1.0, 1.0);
    private GeoPoint circleCenter;

    private Button okButton;        //declaring button for the user interface
    private Switch switch1, switch2;

    private Polygon polygon;        //declare polygon for displaying a circle
    private Marker marker;          //declare a marker for the overlay of the map

    ScaleBarOverlay mScaleBarOverlay;
    CompassOverlay mCompassOverlay;

    private EditText inputRadius;
    private EditText smsInput;
    private EditText messageInput;
    private EditText emailInput;

    private MyLocationNewOverlay mLocationOverlay;
    private RotationGestureOverlay mRotationGestureOverlay;

    final SmsManager m = SmsManager.getDefault();
    private String phoneNumber = "+15555215554";        //phone number for testing with the simulator
    //private String phoneNumber = "017650182055";
    private String messageText = "Your Ship Is Out Of Range!";
    private String messageTextBat = "AnchorWatch: The Battery Level Of Your Phone Is Low!";

    String sendTo = "NaFred.eit@web.de";
    String emailBody = "Hi I am test body";

    private boolean isFirstStart = true;
    //declare shared preference and editor
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    private MapEventsReceiver mReceive;

    private boolean isRadiusWindowOpen = false;
    private boolean isPersonalWindowOpen = false;
    private boolean isAlertWindowOpen = false;
    private boolean isAboutWindowOpen = false;
    private boolean isBatLowSMSsend = false;

    private boolean isSMSChosen = false;
    private boolean isEmailChosen = false;

    private double zoom = 18.0;





    /**
     * @brief This method creates the map and initializes the values for displaying positions. The map is build up with a scalebar, rotation feature, zoom feature, a moving position target
     * and a location listener.
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        //start app in fullscreen mode without title bar
        startFullscreen();
        super.onCreate(savedInstanceState);

        //read shared preferences and save in editor
        pref = getApplicationContext().getSharedPreferences("myPref",MODE_PRIVATE);
        editor = pref.edit();

        if(pref.contains("phoneNumber")){
            phoneNumber = pref.getString("phoneNumber", "0123456789");
        }
        if(pref.contains("messageText")){
            messageText = pref.getString("messageText", "Your Ship Is Out Of Range!");
        }
        if(pref.contains("firstStart")){
            //read out value if it was saved before
            isFirstStart =  pref.getBoolean("firstStart", true);
        }
        if(pref.contains("isSMSChosen")){
            //read out value if it was saved before
            isSMSChosen =  pref.getBoolean("isSMSChosen", false);
        }
        if(pref.contains("isEmailChosen")){
            //read out value if it was saved before
            isEmailChosen =  pref.getBoolean("isEmailChosen", false);
        }

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
        mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(ctx), map);
        //mLocationOverlay.setPersonIcon();
        mLocationOverlay.enableMyLocation();

        mCompassOverlay = new CompassOverlay(ctx, new InternalCompassOrientationProvider(ctx), map);
        mCompassOverlay.enableCompass();

        actualGeoPoint.setLongitude(1.0);
        actualGeoPoint.setLatitude(1.0);

        if(savedInstanceState != null) {
            actualGeoPoint.setLatitude(savedInstanceState.getDouble("actualLatitude"));
            actualGeoPoint.setLongitude(savedInstanceState.getDouble("actualLongitude"));
            startGeoPoint.setLatitude(savedInstanceState.getDouble("startLatitude"));
            startGeoPoint.setLongitude(savedInstanceState.getDouble("startLongitude"));
            addMarker(startGeoPoint);
            radius = savedInstanceState.getFloat("radius");
            commingFromOutside = savedInstanceState.getBoolean("commingFromOutside");
            phoneNumber = savedInstanceState.getString("phoneNumber");

            locManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            isAlertWindowOpen = savedInstanceState.getBoolean("isAlertWindowOpen");
            if (isAlertWindowOpen) {
                openAlertDialog(ctx);
            }
            isPersonalWindowOpen = savedInstanceState.getBoolean("isPersonalWindowOpen");
            if(isPersonalWindowOpen) {
                openPersonalDialog(ctx);
            }
            isRadiusWindowOpen = savedInstanceState.getBoolean("isRadiusWindowOpen");
            if(isRadiusWindowOpen) {
                openRadiusDialog(ctx);
            }
            isAboutWindowOpen = savedInstanceState.getBoolean("isAboutWindowOpen");
            if (isAboutWindowOpen) {
                openAboutDialog(ctx);
            }
            isBatLowSMSsend= savedInstanceState.getBoolean("isBatLowSMSsend");
            zoom = savedInstanceState.getDouble("zoom");
            isSMSChosen = savedInstanceState.getBoolean("smsChosen");
            isEmailChosen = savedInstanceState.getBoolean("emailChosen");
        }


        locListener = new LocationListener() {

            @Override
            //when location changed (gps)
            public void onLocationChanged(Location location) {

                actualGeoPoint.setLongitude(location.getLongitude());
                actualGeoPoint.setLatitude(location.getLatitude());


                //mLocationOverlay.enableFollowLocation();
                //iMapController.animateTo((IGeoPoint)actualGeoPoint);


                if (isFirstStart == true) {
                    centerMap();
                    isFirstStart = false;
                }
                BatteryManager bm = (BatteryManager)getSystemService(BATTERY_SERVICE);
                int batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                if(batLevel < 16 && isBatLowSMSsend == false){
                    isBatLowSMSsend = true;
                    sendBatSMS();
                }
                if(batLevel >= 16)
                {
                    isBatLowSMSsend = false;
                }
                if (radius > 0) {
                    geoPointInRadius();
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
        polygon = makeCircle(startGeoPoint, radius, map);
        polygon.setStrokeWidth((float) 5.0);

        map.getOverlays().add(mScaleBarOverlay);
        map.getOverlays().add(mRotationGestureOverlay);
        //add my location
        map.getOverlays().add(mLocationOverlay);

        map.getOverlays().add(mCompassOverlay);


        //set the view of the map at start
        setStartView(actualGeoPoint, zoom);              //TODO start view


        //setStartView(actualGeoPoint, 18.0);              //TODO start view
        //iMapController.animateTo((IGeoPoint)actualGeoPoint);

        map.invalidate();
        if(isFirstStart == true){
            openPersonalDialog(ctx);
            openAboutDialog(ctx);
            editor.putBoolean("firstStart",false);
            editor.commit();
            //setStartView(actualGeoPoint, 18.0);              //TODO start view
        }
        //mLocationOverlay.enableFollowLocation();
        startGPSTracking();

        mReceive = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                radius = (float) p.distanceToAsDouble(actualGeoPoint);
                startGeoPoint =mLocationOverlay.getMyLocation();
                map.getOverlays().remove(marker);
                addMarker(startGeoPoint);
                updateCircle(startGeoPoint, radius, map);
                return false;
            }
        };

        map.getOverlays().add(new MapEventsOverlay(mReceive));
        //map.getOverlays().add(polygon);
    }

    private boolean askForPermissionOnStartup() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                //&& ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //when API >23
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //request permissions with code 40
                requestPermissions(new String[]{ Manifest.permission.ACCESS_FINE_LOCATION}
                        , 40);
            }
            return false;
        }
        return true;
    }
    private boolean askForPermissionOnStartupZwei(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED){
            //when API >23
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //request permissions with code 50
                requestPermissions(new String[]{Manifest.permission.SEND_SMS}
                        , 50);
            }
            return false;
        }
        return true;
    }

    /**
     * @brief This function sets the start up parameters for fullscreen mode. The app is launched in fullscreen without title bar.
     */
    public void startFullscreen() {
        //start app without Title
        //requestWindowFeature(getWindow().FEATURE_NO_TITLE);
        requestWindowFeature(getWindow().FEATURE_NO_TITLE);
        getSupportActionBar().show();
        //start app in fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    /**
     * @brief This function initializes the scalebar. It is displayed in the middle top edge of the screen.
     */
    public void setScaleBar() {
        //scalebar
        final DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
        mScaleBarOverlay = new ScaleBarOverlay(map);
        mScaleBarOverlay.setCentred(true);
        //play around with these values to get the location on screen in the right place for your application
        mScaleBarOverlay.setScaleBarOffset(dm.widthPixels / 2, 10);
    }

    /**
     * @brief This function sets the start view of the app at the very first start. Zoom level and center view are set.
     * @param p The geoPoint fur the view center as a geoPoint
     * @param d The zoom level as double
     */
    public void setStartView(GeoPoint p, double d) {
        iMapController = map.getController();
        iMapController.setZoom(d);
        iMapController.setCenter(p);
    }

    /**
     * @brief This function centers the map view to the actual position
     */
    public void centerMap(){
        actualGeoPoint = mLocationOverlay.getMyLocation();
        iMapController.setCenter(actualGeoPoint);
    }

    /**
     * @brief This function saves the current state of the app in the RAM while turning the mobile device.
     * @param savedInstanceState
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        //get GeoPoint
        double actualLatitude = actualGeoPoint.getLatitude();
        double actualLongitude = actualGeoPoint.getLongitude();
        double startLatitude = startGeoPoint.getLatitude();
        double startLongitude = startGeoPoint.getLongitude();

        zoom = map.getZoomLevelDouble();


        map.invalidate();
        //save Geopoint
        savedInstanceState.putDouble("actualLatitude", actualLatitude);
        savedInstanceState.putDouble("actualLongitude", actualLongitude);
        savedInstanceState.putDouble("startLatitude", startLatitude);
        savedInstanceState.putDouble("startLongitude", startLongitude);
        savedInstanceState.putFloat("radius", radius);
        savedInstanceState.putBoolean("commingFromOutside", commingFromOutside);
        savedInstanceState.putString("phoneNumber",phoneNumber);
        savedInstanceState.putDouble("zoom",zoom);
        //savedInstanceState.putBoolean("isFirstStart",isFirstStart);
        locManager.removeUpdates(locListener);

        savedInstanceState.putBoolean("isRadiusWindowOpen", isRadiusWindowOpen);
        savedInstanceState.putBoolean("isPersonalWindowOpen", isPersonalWindowOpen);
        savedInstanceState.putBoolean("isAlertWindowOpen", isAlertWindowOpen);
        savedInstanceState.putBoolean("isAboutWindowOpen", isAboutWindowOpen);
        savedInstanceState.putBoolean("isBatLowSMSsend", isBatLowSMSsend);

        savedInstanceState.putBoolean("smsChosen",isSMSChosen);
        savedInstanceState.putBoolean("emailChosen",isEmailChosen);

    }

    /**
     * @brief This function resets the overlays of the map. All overlays are cleared and then added again with initial values.
     */
    private void resetOverlays(){
        map.getOverlays().clear();
        map.getOverlays().add(mLocationOverlay);
        map.getOverlays().add(mCompassOverlay);
        map.getOverlays().add(mRotationGestureOverlay);
        map.getOverlays().add(mScaleBarOverlay);
        map.getOverlays().add(new MapEventsOverlay(mReceive));
    }


    /**
     * @brief This function decides by the requestCode of the requestPermission, which member function is chosen and executed.
     * @param requestCode The code to be switched of choosing a function as integer
     * @param permissions The name of the permission as a string
     * @param grantResults The state of the permission result (permission granted, not granted) as an array of integers
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 10:
                startGPSTracking();
                break;
            case 20:
                sendSMS();
                break;
            case 30:
                sendBatSMS();
                break;
            case 40:
                askForPermissionOnStartup();
                break;
            case 50:
                askForPermissionOnStartupZwei();
                break;
            default:
                break;
        }
    }

    /**
     * @brief This function asks for gps permissions and if granted, requests a location update by the locationListener with minimum time of 5 seconds.
     */
    private void startGPSTracking(){
        //check permissions
        //When permission not granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //when API >23
            //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //request permissions with code 10
                //requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.INTERNET}
                  //      ,10);
            //}
            return;
        }
        // this code won't execute IF permissions are not allowed, because in the line above there is return statement.
        //Init Geopoint with current gps location
        //actualGeoPoint = mLocationOverlay.getMyLocation();
        locManager.requestLocationUpdates("gps", 5000, 10, locListener);
    }

    /**
     * @brief This function ask for permission to send a SMS to a secondary device, after getting the granted permission.
     */
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
        TextView x = new TextView(ctx);
        //x.setText("http://maps.google.com/maps?q="+actualGeoPoint.getLatitude()+","+actualGeoPoint.getLongitude());

        x.setText("http://www.openstreetmap.org/?mlat=" + actualGeoPoint.getLatitude() + "&mlon=" + actualGeoPoint.getLongitude()+"&zoom=14&layers=M");
        Pattern pattern = Pattern.compile(".*", Pattern.DOTALL);

        //Linkify.addLinks(x, pattern, "http://maps.google.com/maps?q="+actualGeoPoint.getLatitude()+","+actualGeoPoint.getLongitude());
        Linkify.addLinks(x,Linkify.WEB_URLS);


        m.sendTextMessage(phoneNumber, null, messageText + "\n" + x.getText(), null, null);
    }

    /**
     * @brief This function asks for permission to send a SMS if the battery level i below 15%.
     */
    public void sendBatSMS() {
        //check permissions
        //When permission not granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            //when API >23
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //request permissions with code 20
                requestPermissions(new String[]{Manifest.permission.SEND_SMS}, 30);
            }
            return;
        }
        m.sendTextMessage(phoneNumber, null, messageTextBat, null, null);
        isBatLowSMSsend = true;
    }

    /**
     * @brief This function returns a circle on the map as a polygon shape.
     * @param geoPoint The center point of the circle as a GeoPoint
     * @param radius The radius of the circle as a double (will be casted to a float)
     * @param map The map that is displayed as a MapView
     * @return The polygon shape (circle)
     */
    private Polygon makeCircle(GeoPoint geoPoint, double radius, MapView map){
        //reset geopints for circle
        geoPoints.clear();

        for(int i=0;i<360;i++){
            circleCenter = new GeoPoint(geoPoint.getLatitude(),geoPoint.getLongitude()).destinationPoint(radius,i);   //destination Point wegen der skalierung von Lat/long
            geoPoints.add(circleCenter);
        }
        polygon = new Polygon(map);

        polygon.setFillColor(Color.argb(70, 0,0,255));
        geoPoints.add(geoPoints.get(0));    //forces the loop to close
        polygon.setPoints(geoPoints);
        //polygon.setTitle("AnkerArea");
        return polygon;
    }

    /**
     * @brief This function resumes the app after a pause.
     */
    public void onResume(){
        super.onResume();

        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));

        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    /**
     * @brief This function pauses the app (e.g. getting into background).
     */
    public void onPause(){
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);

        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

    /**
     * @brief This function checks weather the current position is in the set radius (true) or not (false).
     * @return true for being into the radius, else false
     */
    public boolean geoPointInRadius(){
        if (startGeoPoint.distanceToAsDouble(actualGeoPoint) > radius) {
            if(commingFromOutside == false) {
                openAlertDialog(ctx);
                if(isSMSChosen){
                    sendSMS();
                }
                if(isEmailChosen){
                    sendEmail();
                }

            }
            commingFromOutside = true;
            //Toast.makeText(getBaseContext(), "ausserhalb", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            commingFromOutside = false;
            //Toast.makeText(getBaseContext(), "innerhalb", Toast.LENGTH_LONG).show();
            return true;
        }

    }

    /**
     * @brief This function adds a marker on the map.
     * @param center The point that is marked on the map as a GeoPoint
     */
    public void addMarker(GeoPoint center){
        marker = new Marker(map);
        marker.setPosition(center);
        marker.setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_BOTTOM);
        //marker.setTitle("Give it a title");
        map.getOverlays().add(marker);
        map.invalidate();
    }

    /**
     * @brief This function opens an user dialog (alert dialog) whrer the user can input the radius of the circle.
     * The input can be confirmed with the OK button.
     * @param context the current app as a Context
     */
    public void openRadiusDialog(final Context context) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.radiusdialog);
        dialog.setCanceledOnTouchOutside(false);
        isRadiusWindowOpen = true;

        //inputRadius.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        inputRadius = (EditText) dialog.findViewById(R.id.radiusInput);     //dialog muss gegeben sein, weil sonst der View nicht klar ist und es in eine null object reference läuft
        //https://blog.codeonion.com/2015/10/03/android-how-to-fix-null-object-reference-error/
        okButton = dialog.findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (inputRadius.getText().toString().isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Fehler", Toast.LENGTH_SHORT).show();
                    isRadiusWindowOpen = false;
                    dialog.dismiss();
                } else {
                    radius = Float.parseFloat(inputRadius.getText().toString());
                    Toast.makeText(getApplicationContext(),String.valueOf(radius), Toast.LENGTH_SHORT).show();
                    startGeoPoint = mLocationOverlay.getMyLocation();
                    map.getOverlays().remove(marker);
                    addMarker(startGeoPoint);
                    updateCircle(startGeoPoint, radius, map);
                    isRadiusWindowOpen = false;
                    dialog.dismiss();
                }
            }
        });
        dialog.show();
    }

    /**
     * @brief This function updates the circle on the map with new parameters.
     * @param p The center of the circle as a GeoPoint
     * @param radius The radius of the circle as a float
     * @param map The map as a MapView
     */
    public void updateCircle(GeoPoint p, float radius, MapView map){
        map.getOverlays().remove(polygon);
        polygon = makeCircle(p,radius,map);
        polygon.setStrokeWidth((float)5.0);
        map.getOverlays().add(polygon);
        map.invalidate();
        commingFromOutside = false;
        Toast.makeText(getBaseContext(),"New Radius Set", Toast.LENGTH_SHORT).show();
    }

    /**
     * @brief This function opens as user dialog (alert dialog) in which the user can enter personal information. The phone number and the message for the SMS can be entered.
     * The input can be confirmed with the OK button.
     * @param context The map
     */
    public void openPersonalDialog(final Context context) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.personallayout);
        dialog.setCanceledOnTouchOutside(false);
        isPersonalWindowOpen = true;

        //inputRadius.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        smsInput = (EditText) dialog.findViewById(R.id.smsInput);     //dialog muss gegeben sein, weil sonst der View nicht klar ist und es in eine null object reference läuft
        emailInput = (EditText) dialog.findViewById(R.id.emailinput);
        //https://blog.codeonion.com/2015/10/03/android-how-to-fix-null-object-reference-error/
        smsInput.setText(phoneNumber);
        messageInput = (EditText) dialog.findViewById(R.id.smsMessageInput);
        messageInput.setText(messageText);
        okButton = dialog.findViewById(R.id.okButton2);
        switch1 = (Switch) dialog.findViewById(R.id.switch1);
        switch2 = (Switch) dialog.findViewById(R.id.switch2);
        if(isSMSChosen){
            switch1.setChecked(true);
        }
        if(isEmailChosen){
            switch2.setChecked(true);
        }
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(switch1.isChecked()) {                   //SMS ist an
                    if (smsInput.getText().toString().isEmpty()) {
                        Toast.makeText(getApplicationContext(), "No number entered!", Toast.LENGTH_SHORT).show();
                        isPersonalWindowOpen = false;
                        isSMSChosen = false;
                        editor.putBoolean("isSMSChosen", false);
                        editor.commit();
                        dialog.dismiss();
                    } else {
                        phoneNumber = smsInput.getText().toString();
                        messageText = messageInput.getText().toString();
                        editor.putString("phoneNumber", phoneNumber);
                        editor.putString("messageText", messageText);
                        isSMSChosen = true;
                        isEmailChosen = false;
                        editor.putBoolean("isSMSChosen", true);
                        editor.putBoolean("isEmailChosen", false);
                        editor.commit();
                        Toast.makeText(getApplicationContext(), String.valueOf(phoneNumber), Toast.LENGTH_SHORT).show();
                            if(switch2.isChecked()){                //email ist ann
                                if (emailInput.getText().toString().isEmpty()) {
                                    Toast.makeText(getApplicationContext(), "No mail entered!", Toast.LENGTH_SHORT).show();
                                    isPersonalWindowOpen = false;
                                    isEmailChosen = false;
                                    editor.putBoolean("isEmailChosen", false);
                                    editor.commit();
                                    dialog.dismiss();
                                }else{
                                    sendTo = emailInput.getText().toString();
                                    messageText = messageInput.getText().toString();
                                    editor.putString("email", sendTo);
                                    editor.putString("messageText", messageText);
                                    isEmailChosen = true;
                                    editor.putBoolean("isEmailChosen", true);
                                    editor.commit();
                                    Toast.makeText(getApplicationContext(), String.valueOf(sendTo), Toast.LENGTH_SHORT).show();
                                    isPersonalWindowOpen = false;
                                    dialog.dismiss();
                                }
                            }
                        isPersonalWindowOpen = false;
                        dialog.dismiss();
                    }
                }else{      //SMS ist aus
                    isSMSChosen = false;
                    editor.putBoolean("isSMSChosen", false);
                    editor.commit();
                    if(switch2.isChecked()) {       //Email ist an
                        if (emailInput.getText().toString().isEmpty()) {
                            Toast.makeText(getApplicationContext(), "No mail entered!", Toast.LENGTH_SHORT).show();
                            isPersonalWindowOpen = false;
                            isEmailChosen = false;
                            editor.putBoolean("isEmailChosen", false);
                            editor.commit();
                            dialog.dismiss();
                        } else {
                            sendTo = emailInput.getText().toString();
                            messageText = messageInput.getText().toString();
                            editor.putString("email", sendTo);
                            editor.putString("messageText", messageText);
                            isEmailChosen = true;
                            Toast.makeText(getApplicationContext(), String.valueOf(sendTo), Toast.LENGTH_SHORT).show();
                            isPersonalWindowOpen = false;
                            editor.putBoolean("isEmailChosen", true);
                            editor.commit();
                            dialog.dismiss();
                        }
                    }else {
                        isEmailChosen = false;
                        editor.putBoolean("isSMSChosen", false);
                        editor.putBoolean("isEmailChosen", false);
                        editor.commit();
                        Toast.makeText(getApplicationContext(), "Nothing is entered!", Toast.LENGTH_SHORT).show();
                        isPersonalWindowOpen = false;
                        dialog.dismiss();
                    }
                }
            }
        });
        dialog.show();
    }

    /**
     * @brief This function opens an alertDialog to signal the user that there is an alert situation. The user has to confirm with 'OK'.
     * @param context The map
     */
    public void openAlertDialog(final Context context) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.alertlayout);
        dialog.setCanceledOnTouchOutside(false);
        isAlertWindowOpen = true;

        final MediaPlayer mp = MediaPlayer.create(ctx, R.raw.horn);
        mp.start();

        okButton = dialog.findViewById(R.id.okButton3);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mp.stop();
                isAlertWindowOpen = false;
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    /**
     * @brief This function opens a pop-up window with the about information an the manual. The user has to confirm with 'OK'.
     * @param context The map
     */
    public void openAboutDialog(final Context context) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.aboutlayout);
        dialog.setCanceledOnTouchOutside(false);
        isAboutWindowOpen = true;

        okButton = dialog.findViewById(R.id.okButton4);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isAboutWindowOpen = false;
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    /**
     * @brief This function creates an option menu in the menu bar.
     * The menu has a position, radius, reset and personal information item.
     * @param menu the menu that opens as a Menu
     * @return true, when the menu can be created
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    /**
     * @brief This function switches which menu item is chosen by a click.
     * @param item The item that is clicked as an MenuItem
     * @return true, if an item could be selected
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.position:
                centerMap();

                if(isSMSChosen){
                    sendSMS();
                }
                if(isEmailChosen){
                    sendEmail();
                }

                return true;
            case R.id.radius:
                openRadiusDialog(ctx);
                return true;
            case R.id.resetPosition:
                resetOverlays();
                return true;
            case R.id.personalInfo:
                openPersonalDialog(ctx);
                return true;
            case R.id.about:
                openAboutDialog(ctx);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void sendEmail(){
        String subject = "Your Ship is out of Range!";
        String mailTo = "mailto:" + sendTo +
                "?&subject=" + Uri.encode(subject) +
                "&body=" + Uri.encode(emailBody);
        Intent emailIntent = new Intent(Intent.ACTION_VIEW);
        emailIntent.setType("message/rfc822");
        emailIntent.setData(Uri.parse(mailTo));
        startActivity(emailIntent);
    }
}