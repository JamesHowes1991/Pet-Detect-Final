package com.example.petdetect;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputLayout;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.realm.mongodb.App;
import io.realm.mongodb.AppConfiguration;

/*MONGODB REALM AND ATLAS CODE REPURPOSED FROM: https://www.youtube.com/watch?v=pcNnk-k7YGk&list=WL&index=71
BOTTOMNAVIGATIONVIEW CODE REPURPOSED FROM: https://www.youtube.com/watch?v=JjfSjMs0ImQ&t=326s
TEXTINPUTLAYOUT CODE REPURPOSED FROM: https://www.youtube.com/watch?v=IxhIa3eZxz8
EMAIL REGEX CODE REPURPOSED FROM: https://www.youtube.com/watch?v=OOdO785p3Qo
*/
public class RegisterActivity extends AppCompatActivity {
/*    private static final String TAG = "RegisterActivity";*/


    String appId = "petdetect-yzybq";
    private TextInputLayout edtTxtEmail, edtTxtPassword, edtTxtRetypePassword;
    private ConstraintLayout parent;
    BottomNavigationView bottom_navigation, top_navigation;
    Button btnRegister;

/*    MongoDatabase mongoDatabase;
    MongoClient mongoClient;*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //initialise views
        initRegisterViews();

        //set register icon to selected
        top_navigation.setSelectedItemId(R.id.register);

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
                        if(LoginActivity.userEmail == null) {
                            Toast.makeText(RegisterActivity.this, "Login to write to a tag.", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
                            overridePendingTransition(0,0);
                            return true;
                        } else {
                            startActivity(new Intent(getApplicationContext(), WriteActivity.class));
                            overridePendingTransition(0,0);
                            return true;
                        }
                }
                return false;
            }
        });

        top_navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.register:
                        //current activity, no action required
                        return true;
                    case R.id.account:
                        if(LoginActivity.userEmail == null) {
                            Toast.makeText(RegisterActivity.this, "Login to view your account.", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
                            overridePendingTransition(0,0);
                            return true;
                        } else {
                            startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
                            overridePendingTransition(0,0);
                            return true;
                        }
                    case R.id.spacer:
                        if(LoginActivity.userEmail == null) {
                            Toast.makeText(RegisterActivity.this, "Login to view your account.", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
                            overridePendingTransition(0,0);
                            return true;
                        } else {
                            startActivity(new Intent(getApplicationContext(), WriteActivity.class));
                            overridePendingTransition(0,0);
                            return true;
                        }
                }
                return false;
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //if one or more of the validation checks fail, do not start the registration process
                if(!validateRegisterEmail() | !validateRegisterPassword() | !validateRegisterRetypePassword()) {
                    return;
                }
                initRegister();
            }
        });
    }

    private void initRegister() {
        //create instance of application
        App app = new App(new AppConfiguration.Builder(appId).build());

        //if all validation checks are satisfied continue
        if(validateRegisterEmail() && validateRegisterPassword() && validateRegisterRetypePassword()) {

                //converting the input data to string format, trim eliminates unwanted spaces
                String email = edtTxtEmail.getEditText().getText().toString().trim();
                String password = edtTxtPassword.getEditText().getText().toString().trim();

                //the input data is passed to registerUserAsync as credentials
                //if mongodb atlas accepts the credentials the user is presented a toast message and taken to the login activity
                //else an error toast message displays
                app.getEmailPassword().registerUserAsync(email, password, it -> {
                    if (it.isSuccess()) {
                        Toast.makeText(this, "Registration successful, Welcome to PetDetect!", Toast.LENGTH_SHORT).show();
                        Intent i = new Intent(this, LoginActivity.class);
                        startActivity(i);
                    } else {
                        Toast.makeText(RegisterActivity.this, "Failed to register", Toast.LENGTH_SHORT).show();
                    }
                });
        }
    }

    //this function ensures the email address is the correct format
    //'emailRegex', regex is short for regular expression, which is a sequence
    //of characters that specifies a pattern
    //Code for handling regex found here: https://www.youtube.com/watch?v=OOdO785p3Qo
    //Max O'Didily YouTube account
    //Sites this link as source code: https://hastebin.com/uzamewekev.java
    //However this link appears to be outdated
    private boolean validateRegisterEmail() {
        String emailInput = edtTxtEmail.getEditText().getText().toString().trim();
        String emailRegex = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$";

        //compile pattern, ignore capitalisation
        Pattern emailPattern = Pattern.compile(emailRegex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = emailPattern.matcher(emailInput);

        if(matcher.find()) {
            //if there is no error
            edtTxtEmail.setError(null);
            return true;
        } else if (emailInput.isEmpty()) {
            //.setError operates on the TextViewLayout's. If the above condition is met the error
            //message displays what is inside the double quotes
            edtTxtEmail.setError("Field cannot be empty");
            return false;
        } else {
            edtTxtEmail.setError("Invalid email structure");
            return false;
        }
    }

    private boolean validateRegisterPassword() {
        String passwordInput = edtTxtPassword.getEditText().getText().toString().trim();

        if (passwordInput.isEmpty()) {
            edtTxtPassword.setError("Field cannot be empty");
            return false;
        } else if(passwordInput.length() < 6) {

            //the restriction on character length is set by mongoDB atlas
            //this code ensures the chosen credentials meet requirements
            edtTxtPassword.setError("Password must be at least 6 characters");
            return false;
        } else if(passwordInput.length() > 24) {
            edtTxtPassword.setError("Password cannot exceed 24 characters");
            return false;
        } else {
            edtTxtPassword.setError(null);
            return true;
        }
    }

    private boolean validateRegisterRetypePassword() {
        String retypePasswordInput = edtTxtRetypePassword.getEditText().getText().toString().trim();

        //added to ensure a password is not mistyped
        if (!edtTxtPassword.getEditText().getText().toString().trim().equals(retypePasswordInput)) {
            edtTxtRetypePassword.setError("Passwords do not match, try again");
            return false;
        } else {
            edtTxtRetypePassword.setError(null);
            return true;
        }
    }


    private void initRegisterViews() {

        edtTxtEmail = findViewById(R.id.edtTxtEmail);
        edtTxtPassword = findViewById(R.id.edtTxtPassword);
        edtTxtRetypePassword = findViewById(R.id.edtTxtRetypePassword);

        parent = findViewById(R.id.parent);
        bottom_navigation = findViewById(R.id.bottom_navigation);
        top_navigation = findViewById(R.id.top_navigation);
        btnRegister = findViewById(R.id.btnRegister);


    }
}
