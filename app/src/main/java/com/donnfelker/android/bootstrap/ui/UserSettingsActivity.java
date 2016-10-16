package com.donnfelker.android.bootstrap.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.ToggleButton;
import butterknife.Bind;
import com.amazonaws.AmazonClientException;
import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobile.push.PushManager;
import com.amazonaws.mobile.push.SnsTopic;
import com.amazonaws.mobileconnectors.cognito.Dataset;
import com.amazonaws.mobileconnectors.cognito.DefaultSyncCallback;
import com.amazonaws.mobileconnectors.cognito.Record;
import com.amazonaws.mobileconnectors.cognito.SyncConflict;
import com.amazonaws.mobileconnectors.cognito.exceptions.DataStorageException;
import com.donnfelker.android.bootstrap.R;
import com.donnfelker.android.bootstrap.ui.content.UserSettings;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.List;

public class UserSettingsActivity extends BootstrapActivity {

    private static final String LOG_TAG = UserSettingsActivity.class.getSimpleName();

    // Arbitrary activity request ID. You can handle this in the main activity,
    // if you want to take action when a google services result is received.
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 1363;

    private static final String SHARED_FILE_NAME = "app_preferences";

    @Bind(R.id.tgl_notifications)
    protected ToggleButton tglNotifications;

    @Bind(R.id.push_demo_topics_list)
    protected ListView topicsListView;

    @Bind(R.id.push_demo_enable_push_checkbox)
    protected CheckBox enablePushCheckBox;

    private PushManager pushManager;

    private ArrayAdapter<SnsTopic> topicsAdapter;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.user_settings);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        prefs = getSharedPreferences(SHARED_FILE_NAME, Context.MODE_PRIVATE);

        //Instance PushManager
        pushManager = AWSMobileClient.defaultMobileClient().getPushManager();

        loadUserSettings();

        tglNotifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                boolean on = ((ToggleButton) view).isChecked();
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("notifications", on);
                editor.apply();
                saveSettings();
            }

        });

        enablePushCheckBox.setChecked(pushManager.isPushEnabled());
        enablePushCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleNotification(enablePushCheckBox.isChecked());
            }
        });

        topicsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final SnsTopic snsTopic = topicsAdapter.getItem(position);
                toggleSubscription(snsTopic, true);
            }
        });

        topicsAdapter = new ArrayAdapter<SnsTopic>(this, R.layout.list_item_text_with_checkbox) {
            @Override
            public View getView(final int position, final View convertView,
                                final ViewGroup parent) {
                final CheckedTextView view = (CheckedTextView) super.getView(position, convertView,
                        parent);
                view.setChecked(getItem(position).isSubscribed());
                view.setEnabled(pushManager.isPushEnabled());
                return view;
            }

            @Override
            public boolean isEnabled(final int position) {
                return pushManager.isPushEnabled();
            }
        };
        topicsAdapter.addAll(pushManager.getTopics().values());
        topicsListView.setAdapter(topicsAdapter);

        final GoogleApiAvailability api = GoogleApiAvailability.getInstance();

        final int code = api.isGooglePlayServicesAvailable(this);

        if (ConnectionResult.SUCCESS != code) {
            final String errorString = api.getErrorString(code);
            Log.e(LOG_TAG, "Google Services Availability Error: " + errorString + " (" + code + ")");

            if (api.isUserResolvableError(code)) {
                Log.e(LOG_TAG, "Google Services Error is user resolvable.");
                api.showErrorDialogFragment(this, code, REQUEST_GOOGLE_PLAY_SERVICES);
                enablePushCheckBox.setEnabled(false);
                return;
            } else {
                Log.e(LOG_TAG, "Google Services Error is NOT user resolvable.");
                showErrorMessage(R.string.push_demo_error_message_google_play_services_unavailable);
                enablePushCheckBox.setEnabled(false);
                return;
            }
        }

    }

    private void loadUserSettings() {
        final UserSettings userSettings = UserSettings.getInstance(this);
        final Dataset dataset = userSettings.getDataset();
        final ProgressDialog dialog = ProgressDialog.show(this,
                getString(R.string.settings_fragment_dialog_title),
                getString(R.string.settings_fragment_dialog_message));

        Log.d(LOG_TAG, "Loading user settings from remote");
        dataset.synchronize(new DefaultSyncCallback() {
            @Override
            public void onSuccess(final Dataset dataset, final List<Record> updatedRecords) {
                super.onSuccess(dataset, updatedRecords);
                userSettings.loadFromDataset();
                updateUI(dialog, userSettings.isNotifications());
            }

            @Override
            public void onFailure(final DataStorageException dse) {
                Log.w(LOG_TAG, "Failed to load user settings from remote, using default.", dse);
                updateUI(dialog, userSettings.isNotifications());

            }

            @Override
            public boolean onDatasetsMerged(final Dataset dataset,
                                            final List<String> datasetNames) {
                // Handle dataset merge. One can selectively copy records from merged datasets
                // if needed. Here, simply discard merged datasets
                for (String name : datasetNames) {
                    Log.d(LOG_TAG, "found merged datasets: " + name);
                    AWSMobileClient.defaultMobileClient().getSyncManager().openOrCreateDataset(name).delete();
                }
                return true;
            }
        });
    }

    private void updateUI(final ProgressDialog dialog, final boolean notifications) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (dialog != null) {
                    dialog.dismiss();
                }
                tglNotifications.setChecked(notifications);
            }
        });
    }


    private void saveSettings() {

        final UserSettings userSettings = UserSettings.getInstance(this);
        userSettings.setNotifications(tglNotifications.isChecked());

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... params) {
                userSettings.saveToDataset();
                return null;
            }

            @Override
            protected void onPostExecute(final Void aVoid) {

                // save user settings to remote on background thread
                userSettings.getDataset().synchronize(new Dataset.SyncCallback() {
                    @Override
                    public void onSuccess(Dataset dataset, List<Record> updatedRecords) {
                        Log.d(LOG_TAG, "onSuccess - dataset updated");
                    }

                    @Override
                    public boolean onConflict(Dataset dataset, List<SyncConflict> conflicts) {
                        Log.d(LOG_TAG, "onConflict - dataset conflict");
                        return false;
                    }

                    @Override
                    public boolean onDatasetDeleted(Dataset dataset, String datasetName) {
                        Log.d(LOG_TAG, "onDatasetDeleted - dataset deleted");
                        return false;
                    }

                    @Override
                    public boolean onDatasetsMerged(Dataset dataset, List<String> datasetNames) {
                        Log.d(LOG_TAG, "onDatasetsMerged - datasets merged");
                        return false;
                    }

                    @Override
                    public void onFailure(DataStorageException dse) {
                        Log.e(LOG_TAG, "onFailure - " + dse.getMessage(), dse);
                    }
                });
            }
        }.execute();
    }


    private void toggleNotification(final boolean enabled) {
        final ProgressDialog dialog = showWaitingDialog(
                R.string.push_demo_wait_message_update_notification);

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(final Void... params) {
                // register device first to ensure we have a push endpoint.
                pushManager.registerDevice();

                // if registration succeeded.
                if (pushManager.isRegistered()) {
                    try {
                        pushManager.setPushEnabled(enabled);
                        // Automatically subscribe to the default SNS topic
                        if (enabled) {
                            pushManager.subscribeToTopic(pushManager.getDefaultTopic());
                        }
                        return null;
                    } catch (final AmazonClientException ace) {
                        Log.e(LOG_TAG, "Failed to change push notification status", ace);
                        return ace.getMessage();
                    }
                }
                return "Failed to register for push notifications.";
            }

            @Override
            protected void onPostExecute(final String errorMessage) {
                dialog.dismiss();
                topicsAdapter.notifyDataSetChanged();
                enablePushCheckBox.setChecked(pushManager.isPushEnabled());

                if (errorMessage != null) {
                    showErrorMessage(R.string.push_demo_error_message_update_notification,
                            errorMessage);
                }
            }
        }.execute();
    }


    private void toggleSubscription(final SnsTopic snsTopic, final boolean showConfirmation) {
        if (snsTopic.isSubscribed() && showConfirmation) {
            new AlertDialog.Builder(this).setIconAttribute(android.R.attr.alertDialogIcon)
                    .setTitle(android.R.string.dialog_alert_title)
                    .setMessage(getString(R.string.push_demo_confirm_message_unsubscribe,
                            snsTopic.getDisplayName()))
                    .setNegativeButton(android.R.string.no, null)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            toggleSubscription(snsTopic, false);
                        }
                    })
                    .show();
            return;
        }

        final ProgressDialog dialog = showWaitingDialog(
                R.string.push_demo_wait_message_update_subscription);

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(final Void... params) {
                try {
                    if (snsTopic.isSubscribed()) {
                        pushManager.unsubscribeFromTopic(snsTopic);
                    } else {
                        pushManager.subscribeToTopic(snsTopic);
                    }
                    return null;
                } catch (final AmazonClientException ace) {
                    Log.e(LOG_TAG, "Error occurred during subscription", ace);
                    return ace.getMessage();
                }
            }

            @Override
            protected void onPostExecute(final String errorMessage) {
                dialog.dismiss();
                topicsAdapter.notifyDataSetChanged();

                if (errorMessage != null) {
                    showErrorMessage(R.string.push_demo_error_message_update_subscription,
                            errorMessage);
                }
            }
        }.execute();
    }

    private AlertDialog showErrorMessage(final int resId, final Object... args) {
        return new AlertDialog.Builder(this).setMessage(getString(resId, (Object[]) args))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private ProgressDialog showWaitingDialog(final int resId, final Object... args) {
        return ProgressDialog.show(this,
                getString(R.string.push_demo_progress_dialog_title),
                getString(resId, (Object[]) args));
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
