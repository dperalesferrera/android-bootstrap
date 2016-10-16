package com.donnfelker.android.bootstrap.ui;

import android.os.Bundle;
import android.widget.TextView;

import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.AnalyticsEvent;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.EventClient;
import com.donnfelker.android.bootstrap.R;
import com.donnfelker.android.bootstrap.core.News;

import butterknife.Bind;

import static com.donnfelker.android.bootstrap.core.Constants.Extra.NEWS_ITEM;

public class NewsActivity extends BootstrapActivity {

    private News newsItem;

    @Bind(R.id.tv_title) protected TextView title;
    @Bind(R.id.tv_content) protected TextView content;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.news);

        if (getIntent() != null && getIntent().getExtras() != null) {
            newsItem = (News) getIntent().getExtras().getSerializable(NEWS_ITEM);
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        setTitle(newsItem.getTitle());

        title.setText(newsItem.getTitle());
        content.setText(newsItem.getContent());

        if (newsItem != null) {
            //Send custom event
            final EventClient eventClient = AWSMobileClient.defaultMobileClient().getMobileAnalyticsManager().getEventClient();
            final AnalyticsEvent event = eventClient.createEvent("news_view")
                    .withAttribute("title", newsItem.getTitle());
            eventClient.recordEvent(event);
            eventClient.submitEvents();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // pause/resume Mobile Analytics collection
        AWSMobileClient.defaultMobileClient().handleOnResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // pause/resume Mobile Analytics collection
        AWSMobileClient.defaultMobileClient().handleOnPause();
    }
}
