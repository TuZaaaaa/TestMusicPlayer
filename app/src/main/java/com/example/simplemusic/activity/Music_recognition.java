package com.example.simplemusic.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.example.simplemusic.R;

public class Music_recognition extends AppCompatActivity {

    private ImageView imageView;
    private RippleAnimationView rippleAnimationView;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_recognition);

        imageView = (ImageView) findViewById(R.id.ImageView);
        rippleAnimationView = (RippleAnimationView) findViewById(R.id.layout_RippleAnimation);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (rippleAnimationView.isRippleRunning()) {
                    rippleAnimationView.stopRippleAnimation();
                } else {
                    rippleAnimationView.startRippleAnimation();
                }
            }
        });
    }
}
