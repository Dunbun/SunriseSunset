package com.example.taras.sunrisesunset;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MainActivity extends AppCompatActivity {
    private Button getLocationBtn;
    private final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private final static String TAG = "MainActivity";
    private final String apiKey = "AIzaSyD9rAVLGWQQ0N5QB8fbhtXqA17PpGpis4w";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Places.initialize(getApplicationContext(), apiKey);
        PlacesClient placesClient = Places.createClient(this);

        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));

        autocompleteFragment.setTypeFilter(TypeFilter.CITIES);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                String []splitedLatLng = splitLatLng(place.getLatLng().toString());

                GetSunRiseSunSetInfoTask mytusk = new GetSunRiseSunSetInfoTask();
                mytusk.execute("https://api.sunrise-sunset.org/json?lat=" + splitedLatLng[0] + "&lng=" + splitedLatLng[1] + "&date=today");
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });

        getLocationBtn = (Button)findViewById(R.id.getLocationBtn);

        getLocationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Place myPlace = getCurrentPlace(placesClient);
            }
        });

    }

    public Place getCurrentPlace(PlacesClient placesClient) {
        List<Place.Field> placeFields = Arrays.asList(Place.Field.NAME, Place.Field.LAT_LNG);
        FindCurrentPlaceRequest request = FindCurrentPlaceRequest.builder(placeFields).build();
        final Place[] myPlace = {null};

        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Task<FindCurrentPlaceResponse> placeResponse = placesClient.findCurrentPlace(request);
            placeResponse.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    double maxLikelihood = 0;

                    FindCurrentPlaceResponse response = task.getResult();

                    for (PlaceLikelihood placeLikelihood : response.getPlaceLikelihoods()) {
                        if (maxLikelihood < placeLikelihood.getLikelihood()) {
                            maxLikelihood = placeLikelihood.getLikelihood();
                            myPlace[0] = placeLikelihood.getPlace();
                        }
                    }
                    Log.i(TAG, "Place '" + myPlace[0].getName() + "' has LatLong " + myPlace[0].getLatLng());
                    //Toast.makeText(MainActivity.this,"Place '" + myPlace[0].getName() + "' has LatLong " + myPlace[0].getLatLng(),Toast.LENGTH_SHORT).show();

                    String []splitedLatLng = splitLatLng(myPlace[0].getLatLng().toString());

                    GetSunRiseSunSetInfoTask mytusk = new GetSunRiseSunSetInfoTask();
                    mytusk.execute("https://api.sunrise-sunset.org/json?lat=" + splitedLatLng[0] + "&lng=" + splitedLatLng[1] + "&date=today");

                } else {
                    Exception exception = task.getException();
                    if (exception instanceof ApiException) {
                        ApiException apiException = (ApiException) exception;
                        Log.e(TAG, " Place not found: " + apiException.getStatusCode());
                    }
                }
            });
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

        }

        return myPlace[0];
    }

    public boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null;
    }

    public boolean isLocationEnabled(Context context) {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        } else {
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }


    }

    public String[] splitLatLng(String latLng){
        String sremovedTrashLatLng = latLng.substring(0,latLng.length()-1);
        sremovedTrashLatLng = sremovedTrashLatLng.substring(10);
        String splitedArr[] = sremovedTrashLatLng.split(",");

        return splitedArr;
    }

    class GetSunRiseSunSetInfoTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            String pageContent = "";
            BufferedReader br = null;
            String line = "";
            StringBuilder sb = new StringBuilder();
            String sunRiseTime = "";
            String sunSetTime = "";

            try {
                URL url = new URL(urls[0]);
                br = new BufferedReader(new InputStreamReader(url.openStream()));

                while((line = br.readLine()) != null){
                    sb.append(line);
                    sb.append(System.lineSeparator());
                }

                pageContent = String.valueOf(sb);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                JSONObject jsonObject = new JSONObject(pageContent);
                String answerStatus = jsonObject.getString("status");

                if(answerStatus.equals("OK")){
                    String sResults = jsonObject.getString("results");
                    jsonObject = new JSONObject(sResults);
                    sunRiseTime = jsonObject.getString("sunrise");
                    sunSetTime = jsonObject.getString("sunset");
                    Log.i(TAG, sunRiseTime + " - " + sunSetTime);

                }else {
                    Log.e(TAG, "Request Failed");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }


            return sunRiseTime + "&" + sunSetTime;
        }

        @Override
        protected void onPostExecute(String id) {
            super.onPostExecute(id);

            String [] splitedArr = id.split("&");

            TextView sunrise = (TextView)findViewById(R.id.sunrise);
            TextView sunset = (TextView)findViewById(R.id.sunset);
            TextView note = (TextView)findViewById(R.id.noteTextView);

            sunrise.setText("Sunrise: " + splitedArr[0]);
            sunset.setText("Sunset: " + splitedArr[1]);

            note.setVisibility(View.VISIBLE);

            Log.i(TAG, id);

        }
    }


}
