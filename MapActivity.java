package com.example.firebasejavaapp;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.List;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private DrawerLayout drawerLayout;
    private ImageView menuIcon;
    private EditText editSearch;
    private ImageView searchIcon, clearSearch;

    private CheckBox cbFootball, cbBasket, cbTennis, cbVolley;
    private ImageView userImage;
    private TextView userName;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private boolean userMovedCamera = false;

    // 🔥 Firebase
    private FirebaseDatabase database;
    private DatabaseReference stadiumRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        LinearLayout navHome = findViewById(R.id.navHome);
        LinearLayout navDocs = findViewById(R.id.navDocs);
        LinearLayout navMore = findViewById(R.id.navMore);

        navHome.setOnClickListener(v ->
                Toast.makeText(this,"Home clicked",Toast.LENGTH_SHORT).show());

        navDocs.setOnClickListener(v ->
                Toast.makeText(this,"Docs clicked",Toast.LENGTH_SHORT).show());

        navMore.setOnClickListener(v ->
                Toast.makeText(this,"More clicked",Toast.LENGTH_SHORT).show());

        // =========================
        // 🔹 Search Bar setup
        // =========================
        editSearch = findViewById(R.id.editSearch);
        searchIcon = findViewById(R.id.searchIcon);
        clearSearch = findViewById(R.id.clearSearch);

        editSearch.setOnEditorActionListener((v, actionId, event) -> {
            String query = editSearch.getText().toString().trim();
            if(query.isEmpty()) return false;

            Geocoder geocoder = new Geocoder(MapActivity.this);
            try {
                List<Address> addressList = geocoder.getFromLocationName(query,1);
                if(addressList != null && !addressList.isEmpty()){
                    Address address = addressList.get(0);
                    LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                    if(mMap != null) mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,14));
                    userMovedCamera = true;
                } else {
                    Toast.makeText(MapActivity.this,"موقع غير موجود",Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                Toast.makeText(MapActivity.this,"Erreur Geocoder",Toast.LENGTH_SHORT).show();
            }
            return false;
        });

        clearSearch.setOnClickListener(v -> editSearch.setText(""));

        // =========================
        // 🔹 Drawer setup
        // =========================
        drawerLayout = findViewById(R.id.drawerLayout);
        menuIcon = findViewById(R.id.menuIcon);
        menuIcon.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.LEFT));

        cbFootball = findViewById(R.id.cbFootball);
        cbBasket = findViewById(R.id.cbBasket);
        cbTennis = findViewById(R.id.cbTennis);
        cbVolley = findViewById(R.id.cbVolley);
        userImage = findViewById(R.id.userImage);
        userName = findViewById(R.id.userName);

        // =========================
        // 🔹 Map Fragment
        // =========================
        SupportMapFragment mapFragment =
                (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);
        if(mapFragment != null){
            mapFragment.getMapAsync(this);
        }

        // =========================
        // 🔹 Firebase init
        // =========================
        database = FirebaseDatabase.getInstance();
        stadiumRef = database.getReference("stadiums");

        // 🔹 Test Firebase connection
        stadiumRef.child("test").setValue("Firebase connecté !")
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Firebase actif!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Erreur Firebase: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);

        LatLng tunis = new LatLng(36.8065, 10.1815);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(tunis,6));

        if(ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);

        } else {
            enableLocationTunisia();
        }

        // 🔥 Load stadiums from Firebase
        loadStadiumsFromFirebase();
    }

    private void loadStadiumsFromFirebase(){
        stadiumRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(mMap != null) mMap.clear();

                for(DataSnapshot data : snapshot.getChildren()){
                    Double lat = data.child("latitude").getValue(Double.class);
                    Double lng = data.child("longitude").getValue(Double.class);
                    String name = data.child("name").getValue(String.class);

                    if(lat != null && lng != null){
                        LatLng position = new LatLng(lat,lng);
                        if(mMap != null) mMap.addMarker(new MarkerOptions()
                                .position(position)
                                .title(name != null ? name : "Stade"));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MapActivity.this,"Erreur Firebase: " + error.getMessage(),Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void enableLocationTunisia(){
        if(ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        if(mMap != null) mMap.setMyLocationEnabled(true);

        mMap.setOnMyLocationChangeListener(location -> {
            if(!userMovedCamera){
                LatLng myPos = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myPos,14));
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if(requestCode == LOCATION_PERMISSION_REQUEST_CODE){
            if(grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED){
                enableLocationTunisia();
                Toast.makeText(this,"GPS activé", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,"Permission GPS requise", Toast.LENGTH_SHORT).show();
            }
        }
    }
}