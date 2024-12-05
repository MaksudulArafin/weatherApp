package com.example.chatgptweatherapp2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;

    private TextView latitudeView, longitudeView, cityName, dateTime, celsiusTemperature, farenheitTemperature, humidity, atomPressure, visibility, weatherDec, sunrise, sunset;
    private Button locationButton;

    private final String WEATHER_API_KEY = "1a04cfa4c6474718a7d83d0229ad4dca";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize the UI components
        latitudeView = findViewById(R.id.latitude);
        longitudeView = findViewById(R.id.longitude);
        cityName = findViewById(R.id.cityName);
        dateTime = findViewById(R.id.dateTime);
        celsiusTemperature = findViewById(R.id.temperature_celsius);
        farenheitTemperature = findViewById(R.id.temperature_farenheit);
        humidity = findViewById(R.id.humidity);
        atomPressure = findViewById(R.id.atom_pressure);
        visibility = findViewById(R.id.visibility);
        weatherDec = findViewById(R.id.weatherDescription);
        sunrise = findViewById(R.id.sunrise);
        sunset = findViewById(R.id.sunset);
        locationButton = findViewById(R.id.location_button);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Set up the location button click listener
        locationButton.setOnClickListener(v -> getLocationAndFetchWeather());
    }

    private void getLocationAndFetchWeather() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();

                            // Update the latitude and longitude TextViews
                            latitudeView.setText(String.valueOf(latitude));
                            longitudeView.setText(String.valueOf(longitude));

                            // Fetch the weather data
                            fetchWeatherData(latitude, longitude);
                        } else {
                            Toast.makeText(MainActivity.this, "Unable to get location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void fetchWeatherData(double latitude, double longitude) {
        String url = "https://api.weatherbit.io/v2.0/current?lat=" + latitude + "&lon=" + longitude + "&key=" + WEATHER_API_KEY + "&include=minutely";

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        new Thread(() -> {
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    runOnUiThread(() -> parseAndDisplayWeather(responseData));
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "API Request Failed", Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error Fetching Weather", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void parseAndDisplayWeather(String responseData) {
        try {
            JSONObject jsonObject = new JSONObject(responseData);
            JSONObject data = jsonObject.getJSONArray("data").getJSONObject(0);

            // Extract required data from JSON
            String city = data.getString("city_name") + ", " + data.getString("country_code");
            String time = data.getString("ob_time");
            double tempCelsius = data.getDouble("temp");
            double tempFahrenheit = (tempCelsius * 9 / 5) + 32;
            String humidityValue = data.getString("rh");
            String pressure = data.getString("pres");
            String visibilityValue = data.getString("vis");
            //String observationTime = data.getJSONArray("weather").getJSONObject(0).getString("description");
            JSONObject dataObject = jsonObject.getJSONArray("data").getJSONObject(0); // Access the first object in the "data" array
            JSONObject weatherObject = dataObject.getJSONObject("weather"); // Access the "weather" object
            String weatherState = weatherObject.getString("description");
            String sunriseTime = data.getString("sunrise");
            String sunsetTime = data.getString("sunset");

            // Update the UI with the extracted data
            cityName.setText(city);
            dateTime.setText(time);
            celsiusTemperature.setText(String.format("%.1f", tempCelsius));
            farenheitTemperature.setText(String.format("%.1f", tempFahrenheit));
            humidity.setText(humidityValue + "%");
            atomPressure.setText(pressure + " hPa");
            visibility.setText(visibilityValue + " km");
            weatherDec.setText(weatherState);
            sunrise.setText(sunriseTime);
            sunset.setText(sunsetTime);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error Parsing Weather Data", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocationAndFetchWeather();
        } else {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
        }
    }
}
