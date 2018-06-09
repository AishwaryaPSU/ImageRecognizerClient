package com.aishwaryagm.objectrecogniser;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

/**
 * Created by aishwaryagm on 6/9/18.
 */

public class LogoActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.i("INFO",String.format("Logo activity started"));
        setTheme(R.style.AppTheme);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.logo_activity);
        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        try {
            Thread.sleep(5000);
            startActivity(mainActivityIntent);
        } catch (Exception exception) {
            Log.e("ERROR", String.format("Exception occurred in logo activity %s", exception.getMessage()));
            exception.printStackTrace();
        }
    }
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        Log.i("INFO",String.format("On focus window change entered %s",hasFocus));
        if (!hasFocus) {
            return;
        }
        setContentView(R.layout.logo_activity);
        ImageView logoImageView = findViewById(R.id.imageView);
        logoImageView.setImageResource(R.drawable.object_detection_logo);
        super.onWindowFocusChanged(hasFocus);
    }
}
