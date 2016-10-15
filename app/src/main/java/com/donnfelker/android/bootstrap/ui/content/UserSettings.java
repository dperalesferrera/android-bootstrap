package com.donnfelker.android.bootstrap.ui.content;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobile.user.IdentityManager;
import com.amazonaws.mobileconnectors.cognito.Dataset;

/**
 * Simple class for user settings.
 */
public class UserSettings {
    private static final String LOG_TAG = UserSettings.class.getSimpleName();

    // dataset name to store user settings
    private static final String USER_SETTINGS_DATASET_NAME = "user_settings";

    // Intent action used in local broadcast
    public static final String ACTION_SETTINGS_CHANGED = "user-settings-changed";

    // key names in dataset
    private static final String USER_SETTINGS_KEY_NOTIFICATIONS = "notifications";

    // default color
    private static boolean DEFAULT_NOTIFICATIONS = true;

    private boolean notifications = DEFAULT_NOTIFICATIONS;


    private static UserSettings instance;

    public boolean isNotifications() {
        return notifications;
    }

    public void setNotifications(boolean notifications) {
        this.notifications = notifications;
    }

    /**
     * Loads user settings from local dataset into memory.
     */
    public void loadFromDataset() {
        Dataset dataset = getDataset();

        final String dataNotifications = dataset.get(USER_SETTINGS_KEY_NOTIFICATIONS);
        if (dataNotifications != null) {
            notifications = Boolean.valueOf(dataNotifications);
        }
    }

    /**
     * Saves in memory user settings to local dataset.
     */
    public void saveToDataset() {
        Dataset dataset = getDataset();
        dataset.put(USER_SETTINGS_KEY_NOTIFICATIONS, String.valueOf(notifications));
    }

    /**
     * Gets the Cognito dataset that stores user settings.
     *
     * @return Cognito dataset
     */
    public Dataset getDataset() {
        return AWSMobileClient.defaultMobileClient()
                .getSyncManager()
                .openOrCreateDataset(USER_SETTINGS_DATASET_NAME);
    }

    /**
     * Gets a singleton of user settings
     *
     * @return user settings
     */
    public static UserSettings getInstance(final Context context) {
        if (instance != null) {
            return instance;
        }
        instance = new UserSettings();
        final IdentityManager identityManager = AWSMobileClient.defaultMobileClient()
                .getIdentityManager();
        identityManager.addSignInStateChangeListener(
                new IdentityManager.SignInStateChangeListener() {
                    @Override
                    public void onUserSignedIn() {
                        Log.d(LOG_TAG, "load from dataset on user sign in");
                        instance.loadFromDataset();
                    }

                    @Override
                    public void onUserSignedOut() {
                        Log.d(LOG_TAG, "wipe user data after sign out");
                        AWSMobileClient.defaultMobileClient().getSyncManager().wipeData();
                        instance.setNotifications(DEFAULT_NOTIFICATIONS);
                        instance.saveToDataset();
                        final Intent intent = new Intent(ACTION_SETTINGS_CHANGED);
                        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                    }
                });
        return instance;
    }
}
