package com.cdap.androidapp.ManagingLifestyle;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.cdap.androidapp.R;

public class LifestyleNavigationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Making status bar and navigation bar transparent
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        getWindow().setStatusBarColor(Color.parseColor("#42000000"));
        getWindow().setNavigationBarColor(Color.parseColor("#42000000"));
        setContentView(R.layout.activity_lifestyle_navigation);
        setContentView(R.layout.activity_lifestyle_navigation);
    }

    public void openLifestyleTracking(View v)
    {
        Intent intent = new Intent(LifestyleNavigationActivity.this, LifestyleMainActivity.class);
        this.startActivity(intent);
    }

    public void openSuggestingImprovements(View v)
    {
        Intent intent = new Intent(LifestyleNavigationActivity.this, LifestyleSuggestingImprovementsActivity.class);
        this.startActivity(intent);
    }



    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}