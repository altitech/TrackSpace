package altitech.trackspace;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class Main extends AppCompatActivity implements OnMapReadyCallback {

    volatile boolean stopThread = false;

    static boolean nyanCatMode = false;

    Menu menu;

    private GoogleMap mMap;
    static Thread thread;
    LatLng oldPos;
    Marker marker;
    String urlISS = "http://api.open-notify.org/iss-now.json";

    String markerName = "ISS Position";
    int colorSwitch = Color.RED;
    static int updateInterval = 5000;

    int[] colorArray = {    Color.RED,
                            Color.rgb(255, 165, 0),
                            Color.YELLOW,
                            Color.GREEN,
                            Color.rgb(0, 191, 255),
                            Color.rgb(148, 0, 211)
    };

    float zoomLevel;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopThread = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map);

        stopThread = true;
        Toast.makeText(this.getApplicationContext(), "Click on play button to start tracking", Toast.LENGTH_LONG);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {

            private float currentZoom = -1;

            @Override
            public void onCameraChange(CameraPosition position) {
                if (position.zoom != currentZoom){
                    currentZoom = position.zoom;  // here you get zoom level
                    zoomLevel = position.zoom;
                }
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return true;
            }
        });

        thread = new Thread() {
            @Override
            public void run() {
                try {
                    while(true) {
                        if(!stopThread) {
                            startTask();
                            sleep(updateInterval);
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        this.menu = menu;

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem){

        switch(menuItem.getItemId()){
            case R.id.action_settings:

                createDialog();

                return true;

            case R.id.action_play_pause:

                if(!stopThread) {
                    stopThread = true;
                    Toast.makeText(this.getApplicationContext(), "Tracking paused", Toast.LENGTH_LONG).show();
                    menu.getItem(0).setIcon(R.drawable.ic_play_dark);
                } else {
                    stopThread = false;
                    Toast.makeText(this.getApplicationContext(), "Tracking...", Toast.LENGTH_LONG).show();
                    menu.getItem(0).setIcon(R.drawable.ic_pause_dark);
                }

                return true;

            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    private void addNewMarkerToMap(LatLng newPos) {

        if(marker == null){
            setMarker(newPos);
        }

        if(nyanCatMode){

            double offset = -0.005;
            for(int i=0; i < colorArray.length; i++) {

                LatLng tmpnew = new LatLng(newPos.latitude + ((i)*offset), newPos.longitude);
                LatLng tmpold = new LatLng(oldPos.latitude + ((i)*offset), oldPos.longitude);
                mMap.addPolyline(new PolylineOptions()
                        .add(tmpold, tmpnew)
                        .width(10)
                        .color(colorArray[i]));
            }

        }else{
            colorSwitch = colorSwitch == Color.RED ? Color.BLUE : Color.RED;

            mMap.addPolyline(new PolylineOptions()
                    .add(oldPos, newPos)
                    .width(25)
                    .color(colorSwitch));
        }

        marker.remove();

        setMarker(newPos);
    }

    private void setMarker(LatLng newPos) {
        marker = mMap.addMarker(new MarkerOptions()
                .position(newPos)
                .title(markerName));

        if(nyanCatMode) {
            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.iss));
            marker.setAnchor(0, (float) 0.5);
        }else{
            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.iss));
            marker.setAnchor((float) 0.5, (float) 0.5);
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLng(newPos));
        oldPos = newPos;
    }

    public void createDialog(){

        FragmentManager fragmentManager = getSupportFragmentManager();

        MyDialogFragment dialogFragment = new MyDialogFragment();
        dialogFragment.show(fragmentManager, "dialogFragment");


    }

    class MyAsyncTask extends AsyncTask<Void, Void, LatLng> {

        @Override
        protected LatLng doInBackground(Void... params) {

            URL url;
            HttpURLConnection conn;
            InputStream is;
            BufferedReader reader;
            String data;

            String response ="";
            double lat = 0;
            double lon = 0;

            try {
                url = new URL(urlISS);
                conn = (HttpURLConnection) url.openConnection();
                conn.connect();

                is = conn.getInputStream();
                reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

                while ((data = reader.readLine()) != null){
                    response += data + "\n";
                }

                conn.disconnect();

                lat = Double.parseDouble(new JSONObject(response).getJSONObject("iss_position").getString("latitude"));
                lon = Double.parseDouble(new JSONObject(response).getJSONObject("iss_position").getString("longitude"));

            }catch (MalformedURLException e){
                e.printStackTrace();
            }catch (IOException e) {
                e.printStackTrace();
            }catch (JSONException e) {
                e.printStackTrace();
            }

            return new LatLng(lat, lon);
        }

        public void onPostExecute(LatLng newPos) {
            addNewMarkerToMap(newPos);
        }
    }

    public void startTask() {
        new MyAsyncTask().execute();
    }
}

