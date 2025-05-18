package com.example.wca;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import com.airbnb.lottie.LottieAnimationView;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    TextView mapDataText,totalwords,totalMedia,totalLinks,mostbusyUser,wordcloud,emojiAnalysis,monthlyTimeline,dailyTimeline,weekActivity,monthActivity,heatMap;
    Button pickFileButton;
    Spinner spinner;
    PyObject globalResult;
    LottieAnimationView lottieAnimationView;
    ImageView chart_1, chart_2, chart_3, chart_4, chart_5, chart_6, chart_7, chart_8, chart_9;
    private static final int FILE_PICKER_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        mapDataText = findViewById(R.id.mapDataText);
        totalwords = findViewById(R.id.totalwordText);
        totalMedia = findViewById(R.id.totalmediaText);
        totalLinks = findViewById(R.id.totallinksText);
        wordcloud = findViewById(R.id.wordcloud);
        emojiAnalysis = findViewById(R.id.emojiAnalysis);
        monthlyTimeline = findViewById(R.id.monthlyTimeline);
        dailyTimeline = findViewById(R.id.dailyTimeline);
        weekActivity = findViewById(R.id.weekActivity);
        monthActivity = findViewById(R.id.monthActivity);
        heatMap = findViewById(R.id.heatMap);
        pickFileButton = findViewById(R.id.uploadButton);
        mostbusyUser = findViewById(R.id.mostBusyUser);
        spinner = findViewById(R.id.contactSpinner);
        chart_1 = findViewById(R.id.mapImageView);
        chart_2 = findViewById(R.id.graphImageView);
        chart_3 = findViewById(R.id.chart3);
        chart_4 = findViewById(R.id.chart4);
        chart_5 = findViewById(R.id.chart5);
        chart_6 = findViewById(R.id.chart6);
        chart_7 = findViewById(R.id.chart7);
        chart_8 = findViewById(R.id.chart8);
        chart_9 = findViewById(R.id.chart9);
        lottieAnimationView = findViewById(R.id.lottieLoader);

        mostbusyUser.setVisibility(View.GONE);
        wordcloud.setVisibility(View.GONE);
        emojiAnalysis.setVisibility(View.GONE);
        monthlyTimeline.setVisibility(View.GONE);
        dailyTimeline.setVisibility(View.GONE);
        weekActivity.setVisibility(View.GONE);
        monthActivity.setVisibility(View.GONE);
        heatMap.setVisibility(View.GONE);


        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }



        lottieAnimationView.bringToFront();
        lottieAnimationView.setVisibility(View.GONE);
        pickFileButton.setOnClickListener(view -> {
            lottieAnimationView.setVisibility(View.VISIBLE);
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

            // Run on background thread
            Executor executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(() -> {
                try (InputStream inputStream = getContentResolver().openInputStream(uri);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }

                    String chatText = stringBuilder.toString();

                    Python py = Python.getInstance();
                    PyObject pyObj = py.getModule("preprocesser");
                    globalResult = pyObj.callAttr("preprocess", chatText);

                    PyObject senders = py.getModule("helper")
                            .callAttr("users", globalResult);
                    List<PyObject> sendersList = senders.asList();
                    sendersList.add(PyObject.fromJava("Overall"));

                    List<String> senderNames = new ArrayList<>();
                    for (PyObject sender : sendersList) {
                        senderNames.add(sender.toString());
                    }

                    handler.post(() -> {
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                MainActivity.this,
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
                                updateStatsForUser(selectedUser);
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {}
                        });
                    });

                } catch (Exception e) {
                    handler.post(() -> {
                        mapDataText.setText("Error reading file: " + e.getMessage());
                    });
                }
            });
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
        lottieAnimationView.setVisibility(View.VISIBLE);
        Executor executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                Python py = Python.getInstance();
                PyObject stats = py.getModule("helper");
                PyObject result2 = stats.callAttr("fetch_stats", user, globalResult);
                List<PyObject> values = result2.asList();

                String TotalChats = values.get(0).toString();
                String TotalWords = values.get(1).toString();
                String TotalMedia = values.get(2).toString();
                String TotalLinks = values.get(3).toString();

                String tempTotalChats = "Total Chats:\n" + TotalChats;
                String tempTotalWords = "Total Words:\n" + TotalWords;
                String tempTotalMedia = "Total Media:\n" + TotalMedia;
                String tempTotalLinks = "Total Links:\n" + TotalLinks;

                Bitmap chartBitmap = null;
                if (Objects.equals(user, "Overall")) {
                    PyObject encodedImage = stats.callAttr("most_busy_user", globalResult);
                    byte[] imageBytes = android.util.Base64.decode(encodedImage.toString(), android.util.Base64.DEFAULT);
                    chartBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                }

                // Prepare chart bitmaps
                PyObject[] chartFunctions = {
                        stats.callAttr("create_wordcloud", user, globalResult),
                        stats.callAttr("most_common_words", user, globalResult),
                        stats.callAttr("emoji_helper", user, globalResult),
                        stats.callAttr("monthly_timeline", user, globalResult),
                        stats.callAttr("daily_timeline", user, globalResult),
                        stats.callAttr("week_activity", user, globalResult),
                        stats.callAttr("month_activity", user, globalResult),
                        stats.callAttr("activity_heatmap", user, globalResult)
                };

                Bitmap[] bitmaps = new Bitmap[chartFunctions.length];
                for (int i = 0; i < chartFunctions.length; i++) {
                    byte[] imgBytes = android.util.Base64.decode(chartFunctions[i].toString(), android.util.Base64.DEFAULT);
                    bitmaps[i] = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
                }

                Bitmap finalChartBitmap = chartBitmap;

                handler.post(() -> {
                    try {
                        mapDataText.setText(tempTotalChats);
                        totalwords.setText(tempTotalWords);
                        totalMedia.setText(tempTotalMedia);
                        totalLinks.setText(tempTotalLinks);

                        wordcloud.setVisibility(View.VISIBLE);
                        emojiAnalysis.setVisibility(View.VISIBLE);
                        monthlyTimeline.setVisibility(View.VISIBLE);
                        dailyTimeline.setVisibility(View.VISIBLE);
                        weekActivity.setVisibility(View.VISIBLE);
                        monthActivity.setVisibility(View.VISIBLE);
                        heatMap.setVisibility(View.VISIBLE);

                        if (Objects.equals(user, "Overall") && finalChartBitmap != null) {
                            chart_1.setImageBitmap(finalChartBitmap);
                            mostbusyUser.setVisibility(View.VISIBLE);
                        } else {
                            chart_1.setImageBitmap(null);
                            mostbusyUser.setVisibility(View.GONE);
                        }

                        chart_2.setImageBitmap(bitmaps[0]);
                        chart_3.setImageBitmap(bitmaps[1]);
                        chart_4.setImageBitmap(bitmaps[2]);
                        chart_5.setImageBitmap(bitmaps[3]);
                        chart_6.setImageBitmap(bitmaps[4]);
                        chart_7.setImageBitmap(bitmaps[5]);
                        chart_8.setImageBitmap(bitmaps[6]);
                        chart_9.setImageBitmap(bitmaps[7]);
                        lottieAnimationView.setVisibility(View.GONE);

                    } catch (Exception uiEx) {
                        mapDataText.setText("Error updating UI: " + uiEx.getMessage());
                    }
                });

            } catch (Exception e) {
                handler.post(() -> {
                    mapDataText.setText("Error fetching user stats: " + e.getMessage());
                });
            }
        });
    }


}