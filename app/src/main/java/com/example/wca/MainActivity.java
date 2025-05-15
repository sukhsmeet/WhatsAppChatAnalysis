package com.example.wca;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
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

public class MainActivity extends AppCompatActivity {
    TextView mapDataText;
    Button pickFileButton;
    private static final int FILE_PICKER_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        mapDataText = findViewById(R.id.mapDataText);
        pickFileButton = findViewById(R.id.uploadButton);


        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

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
            PyObject result = pyObj.callAttr("preprocess", chatText);

            PyObject stats = py.getModule("helper");
            PyObject result2 = stats.callAttr("fetch_stats", "Overall",result);
            Toast.makeText(this, result2.toString(), Toast.LENGTH_SHORT).show();
            String resultJson = result2.toString();

            mapDataText.setText(resultJson);
        } catch (Exception e) {
            e.printStackTrace();
            //  resultTextView.setText("Error running Python: " + e.getMessage());
        }
    }
}