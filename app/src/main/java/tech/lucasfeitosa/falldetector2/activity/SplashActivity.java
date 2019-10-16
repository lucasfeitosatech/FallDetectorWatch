package tech.lucasfeitosa.falldetector2.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import tech.lucasfeitosa.falldetector2.R;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        Handler handler = new  Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(getApplicationContext(),HomeActivity.class);
                startActivity(i);
                finish();
            }
        };
        handler.postDelayed(runnable,1000);
    }
}
