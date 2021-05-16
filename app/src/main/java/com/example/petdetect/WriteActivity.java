package com.example.petdetect;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.hash.Hashing;

import org.bson.Document;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import io.realm.mongodb.App;
import io.realm.mongodb.AppConfiguration;
import io.realm.mongodb.RealmResultTask;
import io.realm.mongodb.mongo.MongoClient;
import io.realm.mongodb.mongo.MongoCollection;
import io.realm.mongodb.mongo.MongoDatabase;
import io.realm.mongodb.mongo.iterable.MongoCursor;

/*
NFC CODE REPURPOSED FROM: https://www.youtube.com/watch?v=n-8Aq3tp5IE&list=WL&index=3
MONGODB REALM AND ATLAS CODE REPURPOSED FROM: https://www.youtube.com/watch?v=pcNnk-k7YGk&list=WL&index=71
BOTTOMNAVIGATIONVIEW CODE REPURPOSED FROM: https://www.youtube.com/watch?v=JjfSjMs0ImQ&t=326s
TEXTINPUTLAYOUT CODE REPURPOSED FROM: https://www.youtube.com/watch?v=IxhIa3eZxz8
EMAIL REGEX CODE REPURPOSED FROM: https://www.youtube.com/watch?v=OOdO785p3Qo
*/

@RequiresApi(api = Build.VERSION_CODES.O)
public class WriteActivity extends AppCompatActivity {
/*    private static final String TAG = "NFCActivity";*/
    public static final String Error_Detected = "No NFC Tag Detected";
    public static final String Write_Success = "Text Written Successfully";
    public static final String Write_Error = "Error during writing, it is possible the tag has moved, try again";
    //get current date
    private LocalDate currentDate = LocalDate.now();
    //get current time
    private LocalTime currentTime = LocalTime.now();

    //concatenate the users emaiil address with the current date and time
    private String userDateTime = LoginActivity.userEmail+currentDate+currentTime;

    //hashing the userDateTime string. This is the string that gets added to the tag when written to
    //this is done for security purposes, the users email becomes unreadable
    //guava library used, dependency in build.gradle app file
    //Code repurposed from here: https://www.baeldung.com/sha-256-hashing-java
    //Last modified: March 5, 2021, by baeldung
    final String hashed = Hashing.sha256()
            .hashString(userDateTime, StandardCharsets.UTF_8)
            .toString();
    public static String text = "";
    BottomNavigationView bottom_navigation, top_navigation;

    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter writingTagFilters[];
    boolean writeMode;
    Tag myTag;

    MongoDatabase mongoDatabase;
    MongoClient mongoClient;
    MongoCollection<Document> mongoCollection;

    String appId = "petdetect-yzybq";
    App app = new App(new AppConfiguration.Builder(appId).build());

    private Button btnClear, btnReadTag;
    private TextView txtViewReadTag;
    LottieAnimationView lottieAnimationView3;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_nfc);

        //initialise views
        initWriteNfcViews();

        //set the write icon to selected
        bottom_navigation.setSelectedItemId(R.id.write);
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
                        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                        overridePendingTransition(0,0);
                        return true;
                    case R.id.write:
                        //current activity, do nothing
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
                        startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
                        overridePendingTransition(0,0);
                        return true;
                    case R.id.spacer:
                        startActivity(new Intent(getApplicationContext(), WriteActivity.class));
                        overridePendingTransition(0,0);
                        return true;
                }
                return false;
            }
        });


        lottieAnimationView3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                try {
                    if(myTag == null) {
                        Toast.makeText(WriteActivity.this, Error_Detected, Toast.LENGTH_SHORT).show();
                    }
                    else if(LoginActivity.userEmail == null){
                        Toast.makeText(WriteActivity.this, "Login to write to a tag", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        //store user id
                        String userId = app.currentUser().getId();
                        //pass the write function the hashed string which is to be the message
                        //and the tag object
                        write(hashed, myTag);
                        Toast.makeText(WriteActivity.this, Write_Success, Toast.LENGTH_SHORT).show();
                        //access the database
                        mongoClient = LoginActivity.user.getMongoClient("mongodb-atlas");
                        mongoDatabase = mongoClient.getDatabase("PetDetectDB");
                        mongoCollection = mongoDatabase.getCollection("UserCollection");

                        //the users id is used to find the correct entry in the user database
                        //once found that entry is updated with the newly written tag data
                        Document queryFilter = new Document().append("userId", userId);
                        RealmResultTask<MongoCursor<Document>> findTask = mongoCollection.find(queryFilter).iterator();
                        findTask.getAsync(task -> {
                            if (task.isSuccess()){
                                MongoCursor<Document> results = task.get();

                                if (results.hasNext()){
                                    Document result = results.next();
                                    //add tag data to user collection
                                    result.append("Tag Data", hashed);

                                    mongoCollection.updateOne(queryFilter, result).getAsync(result1 -> {
                                        if (result1.isSuccess()){
                                            Toast.makeText(WriteActivity.this, "Updated!", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(WriteActivity.this, "Error updating!", Toast.LENGTH_SHORT).show();
                                        }
                                    });

                                } else {
                                    //added so in the event of an error, the app will not crash
                                    //should be impossible to reach as a user can only write to a tag if logged in
                                    //and if a user is logged in the must have an entry in the database and a userId
                                    Toast.makeText(WriteActivity.this, "Found Nothing", Toast.LENGTH_SHORT).show();
                                    mongoCollection.insertOne(new Document("userId", LoginActivity.user.getId()).append("Tag Data", hashed)).getAsync( result -> {
                                        if (result.isSuccess()){
                                            Toast.makeText(WriteActivity.this, "Data inserted", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(WriteActivity.this, "Error inserting!", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }

                            } else {
                                Toast.makeText(WriteActivity.this, "Finding task failed!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    //the tag can move in the time between capturing the tag and write the data to it
                    //if the tag moves out of range then catch blocks will prevent crashing and
                    //display a toast message to the user
                } catch (IOException e) {
                    Toast.makeText(WriteActivity.this, Write_Error, Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                } catch (FormatException e) {
                    Toast.makeText(WriteActivity.this, Write_Error, Toast.LENGTH_SHORT).show();
                }
            }
        });

        //having the ability to clear tags was useful in testing
        //functionality stayed on the application as being able to clear tags is useful for users too
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if(myTag == null) {
                        Toast.makeText(WriteActivity.this, Error_Detected, Toast.LENGTH_SHORT).show();
                    }
                    else if(LoginActivity.userEmail == null){
                        Toast.makeText(WriteActivity.this, "Login to clear a tag", Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        //write an empty string to 'clear' the tag
                        write("", myTag);
                        Toast.makeText(WriteActivity.this, "Tag has been cleared!", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(WriteActivity.this, Write_Error, Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                } catch (FormatException e) {
                    Toast.makeText(WriteActivity.this, Write_Error, Toast.LENGTH_SHORT).show();
                }
            }
        });

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        //application will close if the device does not support NFC
        if(nfcAdapter == null) {
            Toast.makeText(WriteActivity.this, "This device does not support NFC", Toast.LENGTH_SHORT).show();
            finish();
        }

        readFromIntent(getIntent());
        pendingIntent = PendingIntent.getActivity(WriteActivity.this, 0, new Intent(WriteActivity.this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writingTagFilters = new IntentFilter[] { tagDetected };
    }

    //see main activity for comments
    private void readFromIntent(Intent intent) {
        String action = intent.getAction();
        text = "";
        txtViewReadTag.setText("");
        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || nfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if(rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            }
            buildTagViews(msgs);
        }
    }

    //see main activity for comments, line 309 is of interest for this activity however
    private void buildTagViews(NdefMessage[] msgs) {
        if (msgs == null || msgs.length == 0) return;

        byte[] payload = msgs[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
        int languageCodeLength = payload[0] & 0063;

        try {
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        } catch (UnsupportedEncodingException e) {
            Log.e("UnsupportedEncoding", e.toString());
        }

        if (msgs == null || msgs.length == 0 || text.equals("")) {
            btnReadTag.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Toast.makeText(WriteActivity.this, "Empty Tag", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        String finalText = text;

        //reading a tag on the write page will display the hashed value as opposed to the
        //main activity which will search the database and return owner information
        //this is designed to give user the ability to check the string against the database
        btnReadTag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                txtViewReadTag.setText(finalText);
            }
        });
    }


    private void write(String text, Tag tag) throws IOException, FormatException {

            //create the record
            NdefRecord[] records = { createRecord(text) };
            //NdefMessages are a container for NdefRecords
            NdefMessage message = new NdefMessage(records);

            //get an instance of Ndef for the tag
            Ndef ndef = Ndef.get(tag);
            //connecting enables input / output operations to the tag such as writting
            ndef.connect();
            //write the message
            ndef.writeNdefMessage(message);
            //once the message has been written the connection must be closed
            ndef.close();

    }

    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
        //setting the language to english
        String lang = "en";
        //encoding the string into a sequence of bytes in an array
        byte[] textBytes = text.getBytes();
        //specifying US-ASCII encoding for the byte array
        byte[] langBytes = lang.getBytes("US-ASCII");
        int textLength = textBytes.length;
        int langLength = langBytes.length;
        byte[] payload = new byte[1 + langLength + textLength];

        payload[0] = (byte) langLength;

        //copying the arrays from the original array at position 0 to the new array 'payload'
        System.arraycopy(langBytes, 0, payload, 1, langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        //specifying RDT_Text as the text type, used in conjunction with TNF_WELL_KNOWN
        //TNF_WELL_KNOWN states the type field contains a well-known RTD type name, that type name being RTD_TEXT
        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);

        return recordNFC;
    }

    //see main activity for comments
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        readFromIntent(intent);

        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            //the tag object, required by the write function
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        }
    }

    //see main activity for comments
    @Override
    public void onPause() {
        super.onPause();
        WriteModeOff();
    }

    //see main activity for comments
    @Override
    public void onResume() {
        super.onResume();
        WriteModeOn();
    }

    //see main activity for comments
    private void WriteModeOn() {
        writeMode = true;
        nfcAdapter.enableForegroundDispatch(WriteActivity.this, pendingIntent, writingTagFilters, null);
    }

    //see main activity for comments
    private void WriteModeOff() {
        writeMode = false;
        nfcAdapter.disableForegroundDispatch(WriteActivity.this);
    }

    //initialise views
    private void initWriteNfcViews() {

        lottieAnimationView3 = findViewById(R.id.lottieAnimationView3);
        btnClear = findViewById(R.id.btnClear);
        txtViewReadTag = findViewById(R.id.txtViewReadTag);
        btnReadTag = findViewById(R.id.btnReadTag);
        bottom_navigation = findViewById(R.id.bottom_navigation);
        top_navigation = findViewById(R.id.top_navigation);


    }

}