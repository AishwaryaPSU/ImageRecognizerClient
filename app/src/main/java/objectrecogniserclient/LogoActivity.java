package objectrecogniserclient;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.aishwaryagm.objectrecogniser.R;

/**
 * Created by aishwaryagm on 6/9/18.
 */

public class LogoActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.i("INFO",String.format("Logo activity started"));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.logo_activity);
        final Intent mainActivityIntent = new Intent(this, MainActivity.class);
        try {
            new CountDownTimer(3000, 1000) {
                public void onTick(long millisUntilFinished) {
                }
                public void onFinish() {
                    startActivity(mainActivityIntent);
                }
            }.start();

        } catch (Exception exception) {
            Log.e("ERROR", String.format("Exception occurred in logo activity %s", exception.getMessage()));
            exception.printStackTrace();
        }
    }
}
