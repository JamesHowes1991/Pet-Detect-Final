package com.example.petdetect;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.bson.Document;

import java.io.UnsupportedEncodingException;

import io.realm.Realm;
import io.realm.mongodb.App;
import io.realm.mongodb.AppConfiguration;
import io.realm.mongodb.User;
import io.realm.mongodb.mongo.MongoClient;
import io.realm.mongodb.mongo.MongoCollection;
import io.realm.mongodb.mongo.MongoDatabase;

/*
NFC CODE REPURPOSED FROM: https://www.youtube.com/watch?v=n-8Aq3tp5IE&list=WL&index=3
MONGODB REALM AND ATLAS CODE REPURPOSED FROM: https://www.youtube.com/watch?v=pcNnk-k7YGk&list=WL&index=71
SEARCH ICON ANIMATION FROM: https://edit.lottiefiles.com/?src=https%3A%2F%2Fassets1.lottiefiles.com%2Fpackages%2Flf20_4cntnmut.json
BOTTOMNAVIGATIONVIEW CODE REPURPOSED FROM: https://www.youtube.com/watch?v=JjfSjMs0ImQ&t=326s
*/

public class MainActivity extends AppCompatActivity {

    public static final String Error_Detected = "No NFC Tag Detected";
/*    public static final String Write_Success = "Text Written Successfully";
    public static final String Write_Error = "Error during writing, try again";*/

    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter readingTagFilters[];
    boolean writeMode;
    Tag myTag;
    String text = "";

    String appId = "petdetect-yzybq";

    private TextView txtViewReadName, getTxtViewReadNumber;
    BottomNavigationView bottom_navigation, top_navigation;
    LottieAnimationView lottieAnimationView2;

    MongoDatabase mongoDatabase;
    MongoClient mongoClient;
    MongoCollection<Document> mongoCollection;
    User user;
    App app;
    ConstraintLayout constraintLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Realm.init(this);
        App app = new App(new AppConfiguration.Builder(appId).build());
        user = app.currentUser();
        initNFCViews();

        //when on the 'home' page, that icon will be highlighted in blue
        bottom_navigation.setSelectedItemId(R.id.home);
        //'spacer' is an invisible button on the top navigation bar, to provide spacing for the two elements
        top_navigation.setSelectedItemId(R.id.spacer);

        bottom_navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    //current activity
                    case R.id.home:
                        return true;
                    case R.id.login:
                        //start activity
                        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                        overridePendingTransition(0,0);
                        return true;
                    case R.id.write:
                        //if a user has not logged in, they cannot access the profile activity or the write activity
                        if(LoginActivity.userEmail == null) {
                            Toast.makeText(MainActivity.this, "Login to write to a tag.", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getApplicationContext(), MainActivity.class));
                            overridePendingTransition(0,0);
                            return true;
                        } else {
                            startActivity(new Intent(getApplicationContext(), WriteActivity.class));
                            overridePendingTransition(0,0);
                            return true;
                        }
                }
                //base case
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
                            Toast.makeText(MainActivity.this, "Login to view your account.", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getApplicationContext(), MainActivity.class));
                            overridePendingTransition(0,0);
                            return true;
                        } else {
                            startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
                            overridePendingTransition(0,0);
                            return true;
                        }
                    case R.id.spacer:
                        //invisible button, however if tapped by accident will navigate to the activity the user is already on
                        //resulting in no action
                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        overridePendingTransition(0,0);
                }
                return false;
            }
        });

       //Gets the default NFC adapter for the device
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        //Displaying error message if device does not have NFC technology
        if(nfcAdapter == null) {
            Toast.makeText(MainActivity.this, "This device does not support NFC", Toast.LENGTH_SHORT).show();
            finish();
        }

        //passing the reference to the intent which was used to launch the MainActivity
        readFromIntent(getIntent());

        //PendingIntent gives the application permission to open a specified intent when an event of interest occurs
        //this also allows for the app to be opened automatically, if an NFC tag is discovered, the mainActivity intent will open
        //
        //'FLAG_SINGLE_TOP ensures that if the MainActivity is currently open or 'on top' then it will NOT open another instance of the activity
        //and will instead use the current instance
        pendingIntent = PendingIntent.getActivity(MainActivity.this, 0, new Intent(MainActivity.this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        //IntentFilter checks the AndroidManifest.xml file to determine what types of intent each element can receive
        //For example in the AndroidManifest.xml file the MainActivity is the default intent with mimeType 'text/plain'
        //This means the kind of data the activity can manage
        //ACTION_TAG_DISCOVERED & addCategory will start the MainActivity when the tag is discovered as 'CATEGORY_DEFAULT'
        //is requesting the default activity which as stated above is set in the AndroidManifest.xml file
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        //saving this filter for later use, line 328
        readingTagFilters = new IntentFilter[] { tagDetected };

        lottieAnimationView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(myTag == null) {
                    Toast.makeText(MainActivity.this, Error_Detected, Toast.LENGTH_LONG).show();
                }
            }
        });
    }


    private void readFromIntent(Intent intent) {
        //specifying the action for the intent
        String action = intent.getAction();
        //setting text views to empty when a new tag is discovered
        txtViewReadName.setText("");
        getTxtViewReadNumber.setText("");
        //logic to determine that the action passed is equivalent
        //the action is received from the intent which starts the activity
        //in this case the action is not specified so is null
        //if the actions have equivalency reading can begin
        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || nfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            //getting raw data from the discovered tag
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            //NdefMessage array to build messages from NDEF records
            NdefMessage[] msgs = null;
            //if the tag isn't empty, give msgs the same length as the information found on the tag
            //fill msgs with the tag information, one element at a time
            //Ndef messages contain byte data
            if(rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            }
            //call buildTagViews with the final array
            buildTagViews(msgs);
        }
    }

    private void buildTagViews(NdefMessage[] msgs) {

        //NdefMessages must contain at least 1 ndef record
        //therefore no logic is required to check for length 0 (empty messages)
        //getRecords() retrieves the NDEF record inside the message array at the index specified
        //0 is the MIME Type
        //getPayload() returns the variable length payload which is the actual message to be read
        byte[] payload = msgs[0].getRecords()[0].getPayload();
        //Getting string encoding, as current message is in byte format
        //UTF-8 Example: 0030 == 0. UTF-8 uses 1-4 bytes, UTF-16 uses 2-4 bytes
        //== 0 specifies UTF-8
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
        //Getting 'en' english language code
        //0063 is UTF-8 for 'c' which is the locale for the US-ASCII code set
        int languageCodeLength = payload[0] & 0063;

        try {
            //Creating the final string
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        } catch (UnsupportedEncodingException e) {
            Log.e("UnsupportedEncoding", e.toString());
        }

        //in the event a user tries to read a tag with no data written to it
        //the tag data is captured, however at length 0 the tag is empty.
        //if length is not 0 a different toast message displays informing the user tag
        //data has been read
        if (msgs == null || msgs.length == 0 || text.equals("")) {
            lottieAnimationView2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    lottieAnimationView2.playAnimation();

                    Toast.makeText(MainActivity.this, "Empty Tag", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        } else {
            Toast.makeText(this, "Tap search icon to display data!", Toast.LENGTH_LONG).show();
        }

        String finalText = text;

        lottieAnimationView2.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                //if the user object is empty, the database cannot be accessed, therefore information cannot be found
                if(user == null){
                    lottieAnimationView2.playAnimation();
                    Toast.makeText(MainActivity.this, "Not Found!", Toast.LENGTH_SHORT).show();
                } else {
                    //connect to the database and access the user collection
                    lottieAnimationView2.playAnimation();
                    mongoClient = user.getMongoClient("mongodb-atlas");
                    mongoDatabase = mongoClient.getDatabase("PetDetectDB");
                    mongoCollection = mongoDatabase.getCollection("UserCollection");
                    //provide the key 'tag data' which is a field name within UserCollection
                    //any record with the tag data that matches the string 'finalText' will be retrieved
                    Document queryFilter = new Document().append("Tag Data", finalText);
                    mongoCollection.findOne(queryFilter).getAsync(result -> {
                        if (result.isSuccess()) {
                            Document resultData = result.get();
                            //display the information
                            txtViewReadName.setText("Owner: "+resultData.getString("Owner"));
                            getTxtViewReadNumber.setText(resultData.getString("Contact Number"));
                        } else {
                            lottieAnimationView2.playAnimation();
                            Toast.makeText(MainActivity.this, "Not Found!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    //line 163 contains pendingIntent which user the FLAG_ACTIVITY_SINGLE_TOP flag
    //in the case another tag is scanned which relaunches the activity instead of a new instance
    //of the activity being started, onNewIntent is called on the existing instance with the intent
    //that was used to relaunch it
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        readFromIntent(intent);

        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            //'EXTRA_TAG' is required code, it is an object which represents the scanned tag
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        }
    }

    //onPause and onResume functions manage the lifecycle on an activity and are counterparts
    //on line 73 the onCreate function begins, this function is the start of the lifecycle
    //after an activity is launched
    //onResume is called just prior to the activity running
    //onPause is called when a new activity is opened
    @Override
    public void onPause() {
        super.onPause();
        WriteModeOff();
    }

    @Override
    public void onResume() {
        super.onResume();
        WriteModeOn();
    }

    //gives priority to the activity in the foreground when dispatching a found tag to an application
    private void WriteModeOn() {
        writeMode = true;
        nfcAdapter.enableForegroundDispatch(MainActivity.this, pendingIntent, readingTagFilters, null);
    }

    //disableForegroundDispatch must be called prior to the completion of onPause()
    private void WriteModeOff() {
        writeMode = false;
        nfcAdapter.disableForegroundDispatch(MainActivity.this);
    }

    private void initNFCViews() {

        //initialise all views
        txtViewReadName = findViewById(R.id.txtViewReadName);
        getTxtViewReadNumber = findViewById(R.id.txtViewReadNumber);
        bottom_navigation = findViewById(R.id.bottom_navigation);
        top_navigation = findViewById(R.id.top_navigation);
        lottieAnimationView2 = findViewById(R.id.lottieAnimationView2);
        constraintLayout = findViewById(R.id.constraintMain);
    }
}