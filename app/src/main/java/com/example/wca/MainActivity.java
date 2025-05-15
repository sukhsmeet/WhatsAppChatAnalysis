package com.example.wca;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    TextView mapDataText;
    Button pickFileButton;
    Spinner spinner;
    PyObject globalResult;
    ImageView chart_1;
    private static final int FILE_PICKER_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        mapDataText = findViewById(R.id.mapDataText);
        pickFileButton = findViewById(R.id.uploadButton);
        spinner = findViewById(R.id.contactSpinner);
        chart_1 = findViewById(R.id.mapImageView);

        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        Python py  = Python.getInstance();



        pickFileButton.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");  // Show all files
            String[] mimeTypes = {"text/plain"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            startActivityForResult(Intent.createChooser(intent, "Select WhatsApp Chat (.txt)"), 1);
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_PICKER_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try (InputStream inputStream = getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }

                String chatText = stringBuilder.toString();
                processChatWithPython(chatText);

            } catch (Exception e) {
                e.printStackTrace();
                mapDataText.setText("Error reading file: " + e.getMessage());
            }
        }
    }

    private void processChatWithPython(String chatText) {
        try {
            Python py = Python.getInstance();
            PyObject pyObj = py.getModule("preprocesser"); // chat_processor.py
            globalResult = pyObj.callAttr("preprocess", chatText);

            PyObject senders = py.getModule("helper")
                    .callAttr("users", globalResult);
            List<PyObject> sendersList = senders.asList();
            sendersList.add(PyObject.fromJava("Overall"));

            List<String> senderNames = new ArrayList<>();
            for (PyObject sender : sendersList) {
                senderNames.add(sender.toString());
            }

            // Set up Spinner
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    senderNames
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String selectedUser = senderNames.get(position);
                    Toast.makeText(MainActivity.this, "Selected User: " + selectedUser, Toast.LENGTH_SHORT).show();
                    updateStatsForUser(selectedUser); // fetch stats for this user
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // Do nothing
                }
            });



        } catch (Exception e) {
            e.printStackTrace();
            //  resultTextView.setText("Error running Python: " + e.getMessage());
        }
    }
    private void updateStatsForUser(String user) {
        try {
            Python py = Python.getInstance();
            PyObject stats = py.getModule("helper");
            PyObject result2 = stats.callAttr("fetch_stats", user, globalResult);
            List<PyObject> values = result2.asList();

            String TotalChats = values.get(0).toString();
            String TotalWords = values.get(1).toString();
            String TotalMedia = values.get(2).toString();
            String TotalLinks = values.get(3).toString();

            String output = "Stats for " + user + ":\n\n"
                    + "Total Messages: " + TotalChats + "\n"
                    + "Total Words: " + TotalWords + "\n"
                    + "Media Shared: " + TotalMedia + "\n"
                    + "Links Shared: " + TotalLinks;

            if(Objects.equals(user, "Overall")){
                PyObject encodedImage = stats.callAttr("most_busy_user", globalResult);
                byte[] imageBytes = android.util.Base64.decode(encodedImage.toString(), android.util.Base64.DEFAULT);
                Bitmap chartBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                chart_1.setImageBitmap(chartBitmap);

            }

            mapDataText.setText(output);



        } catch (Exception e) {
            e.printStackTrace();
            mapDataText.setText("Error fetching user stats: " + e.getMessage());
        }
    }
}