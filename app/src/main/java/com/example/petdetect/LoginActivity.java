package com.example.petdetect;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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
import io.realm.mongodb.Credentials;
import io.realm.mongodb.User;

/*MONGODB REALM AND ATLAS CODE REPURPOSED FROM: https://www.youtube.com/watch?v=pcNnk-k7YGk&list=WL&index=71
BOTTOMNAVIGATIONVIEW CODE REPURPOSED FROM: https://www.youtube.com/watch?v=JjfSjMs0ImQ&t=326s
TEXTINPUTLAYOUT CODE REPURPOSED FROM: https://www.youtube.com/watch?v=IxhIa3eZxz8
EMAIL REGEX CODE REPURPOSED FROM: https://www.youtube.com/watch?v=OOdO785p3Qo
*/

public class LoginActivity extends AppCompatActivity {
/*    private static final String TAG = "LoginActivity";*/

    String appId = "petdetect-yzybq";

    private TextInputLayout edtTxtLoginEmail, edtTxtLoginPassword;
    public static String userEmail;
    Button btnLogin;



    public static User user;
    BottomNavigationView bottom_navigation, top_navigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //initialise views
        initLoginViews();

        //set login icon to active
        bottom_navigation.setSelectedItemId(R.id.login);
        //'spacer' set to selected - spacers selected colour is set to match the background colour
        //this results in an invisible button. It exists to provide separate of the two other buttons
        //in the top navigation, without it these two buttons would be too close together
        top_navigation.setSelectedItemId(R.id.spacer);

        bottom_navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.home:
                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        overridePendingTransition(0,0);
                        return true;
                    case R.id.login:
                        //current activity, do nothing
                        return true;
                    case R.id.write:
                        if(LoginActivity.userEmail == null) {
                            Toast.makeText(LoginActivity.this, "Login to write to a tag.", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
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
                        startActivity(new Intent(getApplicationContext(), RegisterActivity.class));
                        overridePendingTransition(0,0);
                        return true;
                    case R.id.account:
                        if(LoginActivity.userEmail == null) {
                            Toast.makeText(LoginActivity.this, "Login to view your account.", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                            overridePendingTransition(0,0);
                            return true;
                        } else {
                            startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
                            overridePendingTransition(0,0);
                            return true;
                        }
                    case R.id.spacer:
                        startActivity(new Intent(getApplicationContext(), WriteActivity.class));
                        overridePendingTransition(0,0);
                            return true;
                }
                return false;
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //if either of the validation checks fail, do not start the login process
                if(!validateLoginEmail() | !validateLoginPassword()) {
                    return;
                }
                initLogin();
            }
        });

    }

    private void initLogin() {
        //create instance of application
        App app = new App(new AppConfiguration.Builder(appId).build());

        if(validateLoginEmail() & validateLoginPassword()) {
            //store user input to Strings
            String email = edtTxtLoginEmail.getEditText().getText().toString().trim();
            String password = edtTxtLoginPassword.getEditText().getText().toString().trim();

            //saving the email and password to a credentials variable
            Credentials emailPasswordCredentials = Credentials.emailPassword(email, password);

            //passing the credentials variable to the loginAsync function, MongoDB Atlas and Realm
            //check that the credentials used match that of a 'pending' or 'confirmed' user
            //if successful the user is logged in a taken to the 'profile' activity
            app.loginAsync(emailPasswordCredentials, it-> {
                if(it.isSuccess()) {
                    //email stored to be used as a greeting on the profile activity
                    //also used as a check in other activities to confirm if the user has logged in
                    userEmail = email;
                    //giving the user object the current user, this enables database access
                    user = app.currentUser();
                    Intent i = new Intent(this, ProfileActivity.class);
                    startActivity(i);

                } else{
                    Toast.makeText(this, "Invalid email or password, try again.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    //email validation
    private boolean validateLoginEmail() {
        String emailInput = edtTxtLoginEmail.getEditText().getText().toString().trim();
        String emailRegex = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$";
        Pattern emailPattern = Pattern.compile(emailRegex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = emailPattern.matcher(emailInput);

        if(matcher.find()) {
            edtTxtLoginEmail.setError(null);
            return true;
        } else if (emailInput.isEmpty()) {
            edtTxtLoginEmail.setError("Field cannot be empty");
            return false;
        } else {
            edtTxtLoginEmail.setError("Invalid email structure");
            return false;
        }
    }

    //password validation
    private boolean validateLoginPassword() {
        String passwordInput = edtTxtLoginPassword.getEditText().getText().toString().trim();

        if (passwordInput.isEmpty()) {
            edtTxtLoginPassword.setError("Field cannot be empty");
            return false;
        } else {
            edtTxtLoginPassword.setError(null);
            return true;
        }
    }

    //initialise views
    private void initLoginViews() {

        edtTxtLoginEmail = findViewById(R.id.edtTxtLoginEmail);
        edtTxtLoginPassword = findViewById(R.id.edtTxtLoginPassword);
        bottom_navigation = findViewById(R.id.bottom_navigation);
        top_navigation = findViewById(R.id.top_navigation);
        btnLogin = findViewById(R.id.btnRegister);
    }
}