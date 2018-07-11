package com.example.androidbtcontrol;

/**
 * Created by Wara on 7/5/2018.
 */

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class AboutActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        Bundle bundle = getIntent().getExtras();
//        if (bundle != null) {
//            String name = bundle.getString("val");
//
//
//            String result = String.format("values is %s",
//                    name);
//            Toast.makeText(this, result,Toast.LENGTH_SHORT).show();
//        }
//        Button buttonBack = (Button) findViewById(R.id.button_back);
//
//        buttonBack.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                finish();
//            }
//        });
    }
}