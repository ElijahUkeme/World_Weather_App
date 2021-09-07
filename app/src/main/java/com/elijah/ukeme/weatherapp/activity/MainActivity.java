package com.elijah.ukeme.weatherapp.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.elijah.ukeme.weatherapp.R;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import adapter.WeatherAdapter;
import model.WeatherModel;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {

    private RelativeLayout homeRL;
    private TextView cityNameTV, temperatureTV, conditionTV, forecastDateTV;
    private ImageView backIV, iconIV, searchIV;
    private ProgressBar loadingPB;
    private RecyclerView weatherRV;
    private TextInputEditText cityEdt;
    private List<WeatherModel> weatherModels;
    private WeatherAdapter weatherAdapter;
    private LocationManager locationManager;
    private int PERMISSION_CODE = 1;
    private String cityName;
    boolean cancel = false;
    private GoogleMap map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_main);
        homeRL = findViewById(R.id.idRLHome);
        cityNameTV = findViewById(R.id.idTVCityName);
        temperatureTV = findViewById(R.id.idTVTemperature);
        conditionTV = findViewById(R.id.idTVCondition);
        backIV = findViewById(R.id.idIVBack);
        iconIV = findViewById(R.id.idIVIcon);
        searchIV = findViewById(R.id.idIVSearch);
        loadingPB = findViewById(R.id.idPBLoading);
        weatherRV = findViewById(R.id.idRVWeather);
        cityEdt = findViewById(R.id.idEdtCity);
        forecastDateTV = findViewById(R.id.idForecastDate);
        weatherModels = new ArrayList<>();
        weatherAdapter = new WeatherAdapter(this, weatherModels);
        weatherRV.setAdapter(weatherAdapter);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != getPackageManager().PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
               ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
               Manifest.permission.ACCESS_COARSE_LOCATION},PERMISSION_CODE);
        }
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

       cityName = getCityName(location.getLongitude(),location.getLatitude());
       getWeatherInfo(cityName);


        searchIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String city = cityEdt.getText().toString();
                if (city.isEmpty()) {
                    cityEdt.setError("Please enter city name");
                    cityEdt.requestFocus();
                    cancel = true;
                } else {
                    cityNameTV.setText(cityName);
                    getWeatherInfo(city);
                }
            }
        });


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Please grant permission", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private String getCityName(double longitude, double latitude) {
        String cityName = "Nearby location";
        Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
        try {
            List<Address> addresses = gcd.getFromLocation(latitude, longitude, 10);

            for (Address address : addresses) {
                if (address != null) {
                    String city = address.getLocality();
                    if (city != null && !city.equals("")) {
                        cityName = city;
                    } else {

                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cityName;
    }

    private void getWeatherInfo(String cityName) {

        String url = "https://api.weatherapi.com/v1/forecast.json?key=dca892fc2d534382a1461857211207&q=" + cityName + "&days=1&aqi=no&alerts=no";
        cityNameTV.setText(cityName);
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                loadingPB.setVisibility(View.GONE);
                homeRL.setVisibility(View.VISIBLE);
                weatherModels.clear();

                try {
                    String temperature = response.getJSONObject("current").getString("temp_c");
                    temperatureTV.setText(temperature + "°C");
                    int isDay = response.getJSONObject("current").getInt("is_day");
                    String condition = response.getJSONObject("current").getJSONObject("condition").getString("text");
                    conditionTV.setText(condition);
                    String conditionIcon = response.getJSONObject("current").getJSONObject("condition").getString("icon");
                    Picasso.with(MainActivity.this).load("http:".concat(conditionIcon)).into(iconIV);
                    if (isDay == 1) {
                        // That means it is morning
                        Picasso.with(MainActivity.this).load("data:image/jpeg;base64,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").into(backIV);
                    } else {
                        Picasso.with(MainActivity.this).load("data:image/jpeg;base64,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").into(backIV);
                    }

                    JSONObject forecaseObject = response.getJSONObject("forecast");
                    JSONObject forecastOb = forecaseObject.getJSONArray("forecastday").getJSONObject(0);
                    JSONArray hourArray = forecastOb.getJSONArray("hour");
                    String date = forecastOb.getString("date");
                    forecastDateTV.setText("Weather forecast for " + date);

                    for (int i = 0; i < hourArray.length(); i++) {
                        JSONObject hourOb = hourArray.getJSONObject(i);
                        String time = hourOb.getString("time");
                        String temp = hourOb.getString("temp_c");
                        String image = hourOb.getJSONObject("condition").getString("icon");
                        String wind = hourOb.getString("wind_kph");
                        weatherModels.add(new WeatherModel(time, temp, image, wind));
                    }
                    weatherAdapter.notifyDataSetChanged();


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                temperatureTV.setText("City name not found");
                weatherModels.clear();
            }
        });

        requestQueue.add(jsonObjectRequest);
    }

}