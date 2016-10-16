package com.donnfelker.android.bootstrap.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import butterknife.Bind;
import com.amazonaws.AmazonClientException;
import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobile.util.ThreadUtils;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.AnalyticsEvent;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.EventClient;
import com.donnfelker.android.bootstrap.R;
import com.donnfelker.android.bootstrap.core.News;
import com.donnfelker.android.bootstrap.ui.nosql.NoSQLTableFactory;
import com.donnfelker.android.bootstrap.ui.nosql.DynamoDBUtils;
import com.donnfelker.android.bootstrap.ui.nosql.NoSQLTableBase;

import static com.donnfelker.android.bootstrap.core.Constants.Extra.NEWS_ITEM;

public class NewsActivity extends BootstrapActivity {

    private static final String LOG_TAG = NewsActivity.class.getSimpleName();

    public static final String TABLE_NAME = "favorite_news";

    private News newsItem;

    @Bind(R.id.tv_title) protected TextView title;
    @Bind(R.id.tv_content) protected TextView content;

    /** The NoSQL Table demo operations will be run against. */
    private NoSQLTableBase demoTable;


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

        //Instance NoSQLTableMapper
        demoTable = NoSQLTableFactory.instance(this)
                .getNoSQLTableByTableName(TABLE_NAME);


        if (newsItem != null) {
            //Send custom event
            final EventClient eventClient = AWSMobileClient.defaultMobileClient().getMobileAnalyticsManager().getEventClient();
            final AnalyticsEvent event = eventClient.createEvent("news_view")
                    .withAttribute("title", newsItem.getTitle());
            eventClient.recordEvent(event);
            eventClient.submitEvents();

            //Save in DynamoDB
            insertData(newsItem);
        }
    }

    private void insertData(final News newsItem) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    demoTable.insertData(newsItem);
                    Log.d(LOG_TAG, "insertData - news viewed added to table");
                } catch (final AmazonClientException ex) {
                    // The insertSampleData call already logs the error, so we only need to
                    // show the error dialog to the user at this point.
                    DynamoDBUtils.showErrorDialogForServiceException(NewsActivity.this,
                            getString(R.string.nosql_dialog_title_failed_operation_text), ex);
                    Log.e(LOG_TAG, "Error adding news viewed to table", ex);
                    return;
                }
                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(NewsActivity.this);
                        dialogBuilder.setTitle(R.string.nosql_dialog_title_added_news_data_text);
                        dialogBuilder.setMessage(R.string.nosql_dialog_message_added_news_data_text);
                        dialogBuilder.setNegativeButton(R.string.nosql_dialog_ok_text, null);
                        dialogBuilder.show();
                    }
                });
            }
        }).start();
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
