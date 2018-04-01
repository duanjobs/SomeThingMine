package com.duanjobs.somethingmine;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.duanjobs.somethingmine.flippageview.FlipPageViewActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
  private TextView tvFlipPageView;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    initView();
  }

  private void initView() {
    tvFlipPageView = (TextView) findViewById(R.id.tv_flip_page_view);
    tvFlipPageView.setOnClickListener(this);
  }

  @Override public void onClick(View v) {
    if(v.getId()==R.id.tv_flip_page_view){
      Intent i =new Intent(MainActivity.this, FlipPageViewActivity.class);
      startActivity(i);
    }
  }
}
