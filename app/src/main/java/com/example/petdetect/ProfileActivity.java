package com.example.petdetect;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/*
BOTTOMNAVIGATIONVIEW CODE REPURPOSED FROM: https://www.youtube.com/watch?v=JjfSjMs0ImQ&t=326s
*/

public class ProfileActivity extends AppCompatActivity {

    private TextView txtViewGreeting;
    //personalised greeting
    String userGreeting = "Hello, " + LoginActivity.userEmail+".";
    Button btnTagDetails, btnPersonalDetails;
    BottomNavigationView bottom_navigation, top_navigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        //initialise views
        initProfileViews();

        //set account icon to selected
        top_navigation.setSelectedItemId(R.id.account);

        bottom_navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.home:
                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        overridePendingTransition(0,0);
                        return true;
                    case R.id.login:
                        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                        overridePendingTransition(0,0);
                        return true;
                    case R.id.write:
                        startActivity(new Intent(getApplicationContext(), WriteActivity.class));
                        overridePendingTransition(0,0);
                        return true;
                }
                return false;
            }
        });

        top_navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.register:
                        startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
                        overridePendingTransition(0,0);
                        return true;
                    case R.id.account:
                        //current activity, do nothing
                        return true;
                    case R.id.spacer:
                        startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
                        overridePendingTransition(0,0);
                        return true;
                }
                return false;
            }
        });

        //set the user greeting
        txtViewGreeting.setText(userGreeting);

    }

    //initialise views
    private void initProfileViews() {

        txtViewGreeting = findViewById(R.id.txtViewGreeting);
        bottom_navigation = findViewById(R.id.bottom_navigation);
        top_navigation = findViewById(R.id.top_navigation);
        btnTagDetails = findViewById(R.id.btnTagDetails);
        btnPersonalDetails = findViewById(R.id.btnPersonalDetails);
    }
}
