package com.thebitwisters.taptoroute;

import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private List<LatLng> lstLatLng = new ArrayList<>();

    private Spinner dropdown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        dropdown = findViewById(R.id.dropdown);
        dropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                draw();
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                //nothing
            }
        });
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                lstLatLng.add(point);
                if (lstLatLng.size() > 2) {
                    mMap.clear();
                    lstLatLng.clear();
                    lstLatLng.add(point);
                }
                mMap.addMarker(new MarkerOptions().position(point));
                if (lstLatLng.size() == 2) {
                    draw();
                } else {
                    dropdown.setSelection(0);
                }

            }
        });
        LatLng home = new LatLng(14.5553055, 121.0193095);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(home));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(17.0f));
    }

    private String mode;
    private void draw() {
        mMap.clear();
        if (lstLatLng.isEmpty() || lstLatLng.size() < 2) return;
        else {
            mMap.addMarker(new MarkerOptions().position(lstLatLng.get(0)));
            mMap.addMarker(new MarkerOptions().position(lstLatLng.get(1)));
        }
        String origin = lstLatLng.get(0).latitude+","+lstLatLng.get(0).longitude;
        String destination = lstLatLng.get(1).latitude+","+lstLatLng.get(1).longitude;
        String key = getString(R.string.google_maps_key);
        mode = "driving";
        Log.i("DD",dropdown.getSelectedItem().toString());
        if (dropdown.getSelectedItem().toString().equals(getResources().getStringArray(R.array.transpo_mode)[1])) mode = "transit";
        String url = String.format("https://maps.googleapis.com/maps/api/directions/json?origin=%1$s&destination=%2$s&key=%3$s&mode=%4$s",
                origin,destination,key,mode);
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray route = response.getJSONArray("routes");
                            for (int x=0; x<route.length(); x++) Log.i("ROUTE", route.getString(x));
                            JSONObject polyline = new JSONObject();
                            for(int i = 0; i < route.length(); i++) {
                                JSONObject obj = route.optJSONObject(i);
                                if(obj != null) {
                                    polyline = obj.optJSONObject("overview_polyline");
                                }
                            }
                            String polypath = polyline.getString("points");
                            List<LatLng> poly = new ArrayList<>();
                            int index = 0, len = polypath.length();
                            int lat = 0, lng = 0;
                            while (index < len) {
                                int b, shift = 0, result = 0;
                                do {
                                    b = polypath.charAt(index++) - 63;
                                    result |= (b & 0x1f) << shift;
                                    shift += 5;
                                } while (b >= 0x20);
                                int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                                lat += dlat;

                                shift = 0;
                                result = 0;
                                do {
                                    b = polypath.charAt(index++) - 63;
                                    result |= (b & 0x1f) << shift;
                                    shift += 5;
                                } while (b >= 0x20);
                                int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                                lng += dlng;

                                LatLng p = new LatLng((double) lat / 1E5, (double) lng / 1E5);
                                poly.add(p);
                            }
                            int color = Color.BLUE;
                            if (mode.equals("driving")) color = Color.RED;
                            mMap.addPolyline(new PolylineOptions()
                                    .addAll(poly)
                                    .width(5)
                                    .color(color)
                                    .visible(true));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub

                    }
                });
        MySingleton.getInstance(this).addToRequestQueue(jsObjRequest);
    }
}
