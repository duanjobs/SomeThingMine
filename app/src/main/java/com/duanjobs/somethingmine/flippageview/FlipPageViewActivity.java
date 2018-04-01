package com.duanjobs.somethingmine.flippageview;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.duanjobs.somethingmine.R;
import com.duanjobs.somethingmine.flippageview.view.FlipPageView;

public class FlipPageViewActivity extends Activity implements FlipPageView.FlipPageListener{
  private FlipPageView mPageView;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_flip_page_view);
    mPageView = (FlipPageView) findViewById(R.id.fp_page);
    mPageView.setFlipPageListener(this);
    mPageView.setPageCount(6);
    mPageView.setCurPage(0);
  }

  @Override public View onCreatePage(View oldpage, int index) {
    View newpage = oldpage == null ? LayoutInflater.from(this).inflate(R.layout.layout_flip_page_view_content,null) : oldpage;
    updatePageViews(newpage,index);
    return newpage;
  }


  @Override public void onPageChanged(View newpage, int index) {

  }


  private void updatePageViews(View page,int index) {
    page = page != null ? page : mPageView.getCurPageView();
    TextView tvHint  = (TextView) page.findViewById(R.id.text_content);
    tvHint.setText("这是第"+index+"页");
  }
}
