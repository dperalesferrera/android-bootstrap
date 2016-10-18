package com.donnfelker.android.bootstrap.ui;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import butterknife.Bind;
import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.services.lambda.model.InvocationType;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.donnfelker.android.bootstrap.R;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

public class SendSMSActivity extends BootstrapActivity {

    private static final String LOG_TAG = SendSMSActivity.class.getSimpleName();
    public static final String FUNCTION_NAME = "send-sms";

    private static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");
    private static final CharsetEncoder ENCODER = CHARSET_UTF8.newEncoder();
    private static final CharsetDecoder DECODER = CHARSET_UTF8.newDecoder();

    @Bind(R.id.edt_message)
    protected EditText message;

    @Bind(R.id.btn_send)
    protected Button btnSend;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.send_sms);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        message.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                if (s.length() != 0)
                    btnSend.setEnabled(true);
                else
                    btnSend.setEnabled(false);
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                invokeFunction(message.getText().toString());
            }
        });

    }


    public void sendSMS(final View view) {

        if (message.getText() != null) {
            invokeFunction(message.getText().toString());
        }
    }


    private void invokeFunction(final String requestPayload) {


        new AsyncTask<Void, Void, InvokeResult>() {
            @Override
            protected InvokeResult doInBackground(Void... params) {
                try {

                    JSONObject sms = new JSONObject();
                    try {
                        sms.put("message", requestPayload);
                    } catch (JSONException e) {
                        Log.e(LOG_TAG, "Error creating message body message: " + e.getMessage(), e);
                    }

                    final ByteBuffer payload = ENCODER.encode(CharBuffer.wrap(sms.toString()));

                    final InvokeRequest invokeRequest =
                            new InvokeRequest()
                                    .withFunctionName(FUNCTION_NAME)
                                    .withInvocationType(InvocationType.RequestResponse)
                                    .withPayload(payload);

                    final InvokeResult invokeResult =
                            AWSMobileClient
                                    .defaultMobileClient()
                                    .getCloudFunctionClient()
                                    .invoke(invokeRequest);

                    return invokeResult;
                } catch (final Exception e) {
                    Log.e(LOG_TAG, "AWS Lambda invocation failed : " + e.getMessage(), e);
                    final InvokeResult result = new InvokeResult();
                    result.setStatusCode(500);
                    result.setFunctionError(e.getMessage());
                    return result;
                }
            }

            @Override
            protected void onPostExecute(final InvokeResult invokeResult) {

                try {
                    final int statusCode = invokeResult.getStatusCode();
                    final String functionError = invokeResult.getFunctionError();
                    final String logResult = invokeResult.getLogResult();

                    if (statusCode != 200) {
                        showError(invokeResult.getFunctionError());
                    } else {
                        final ByteBuffer resultPayloadBuffer = invokeResult.getPayload();
                        final String resultPayload = DECODER.decode(resultPayloadBuffer).toString();
                        Toast.makeText(SendSMSActivity.this, resultPayload, Toast.LENGTH_LONG).show();
                    }

                    if (functionError != null) {
                        Log.e(LOG_TAG, "AWS Lambda Function Error: " + functionError);
                    }

                    if (logResult != null) {
                        Log.d(LOG_TAG, "AWS Lambda Log Result: " + logResult);
                    }
                } catch (final Exception e) {
                    Log.e(LOG_TAG, "Unable to decode results. " + e.getMessage(), e);
                    showError(e.getMessage());
                }
            }
        }.execute();
    }

    public void showError(final String errorMessage) {
        new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.cloud_logic_error_title))
                .setMessage(errorMessage)
                .setNegativeButton(getResources().getString(R.string.cloud_logic_error_dismiss), null)
                .create().show();
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
