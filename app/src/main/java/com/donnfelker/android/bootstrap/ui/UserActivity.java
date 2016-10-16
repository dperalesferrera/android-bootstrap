package com.donnfelker.android.bootstrap.ui;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.Bind;
import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.AnalyticsEvent;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.EventClient;
import com.donnfelker.android.bootstrap.R;
import com.donnfelker.android.bootstrap.core.User;
import com.squareup.picasso.Picasso;

import static com.donnfelker.android.bootstrap.core.Constants.Extra.USER;

public class UserActivity extends BootstrapActivity {

    @Bind(R.id.iv_avatar)
    protected ImageView avatar;
    @Bind(R.id.tv_name)
    protected TextView name;

    private User user;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.user_view);

        if (getIntent() != null && getIntent().getExtras() != null) {
            user = (User) getIntent().getExtras().getSerializable(USER);
        }

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Picasso.with(this).load(user.getAvatarUrl())
                .placeholder(R.drawable.gravatar_icon)
                .into(avatar);

        name.setText(String.format("%s %s", user.getFirstName(), user.getLastName()));

        if (user != null) {
            //Send custom event
            final EventClient eventClient = AWSMobileClient.defaultMobileClient().getMobileAnalyticsManager().getEventClient();
            final AnalyticsEvent event = eventClient.createEvent("users_view")
                    .withAttribute("username", user.getUsername())
                    .withAttribute("firstname", user.getFirstName())
                    .withAttribute("lastname", user.getLastName())
                    .withAttribute("avatar_url", user.getAvatarUrl());
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
