package com.solomonj.ipynbviewer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity  implements AdapterView.OnItemSelectedListener {

    private Button button;
    private Spinner spinner;
    private String[] dropValues;
    private ImageView infologo;
    private String render;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = findViewById(R.id.getStarted);

        spinner = findViewById(R.id.selectRender);
        dropValues = getResources().getStringArray(R.array.dropdown);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, dropValues);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), WebView.class);
                intent.putExtra("render",render);
                startActivity(intent);
            }
        });


        infologo = findViewById(R.id.infologo);
        infologo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(),information.class);
                startActivity(intent);
            }
        });

    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        Toast.makeText(getApplicationContext(), dropValues[i], Toast.LENGTH_SHORT).show();
        render = dropValues[i];
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}