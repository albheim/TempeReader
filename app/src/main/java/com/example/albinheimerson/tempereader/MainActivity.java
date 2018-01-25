package com.example.albinheimerson.tempereader;

import java.math.BigDecimal;
import java.util.EnumSet;

import android.support.v7.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.dsi.ant.plugins.antplus.pcc.AntPlusEnvironmentPcc;
import com.dsi.ant.plugins.antplus.pcc.AntPlusEnvironmentPcc.ITemperatureDataReceiver;
import com.dsi.ant.plugins.antplus.pcc.defines.DeviceState;
import com.dsi.ant.plugins.antplus.pcc.defines.EventFlag;
import com.dsi.ant.plugins.antplus.pcc.defines.RequestAccessResult;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IDeviceStateChangeReceiver;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc.IPluginAccessResultReceiver;
import com.dsi.ant.plugins.antplus.pccbase.PccReleaseHandle;

public class MainActivity extends AppCompatActivity {
    AntPlusEnvironmentPcc envPcc = null;
    PccReleaseHandle<AntPlusEnvironmentPcc> releaseHandle = null;

    TextView tv_status;

    TextView tv_estTimestamp;

    TextView tv_currentTemperature;
    TextView tv_eventCount;
    TextView tv_lowLast24Hours;
    TextView tv_highLast24Hours;

    IPluginAccessResultReceiver<AntPlusEnvironmentPcc> mResultReceiver = new IPluginAccessResultReceiver<AntPlusEnvironmentPcc>()
    {
        // Handle the result, connecting to events on success or reporting
        // failure to user.
        @Override
        public void onResultReceived(AntPlusEnvironmentPcc result,
                                     RequestAccessResult resultCode, DeviceState initialDeviceState)
        {
            switch (resultCode)
            {
                case SUCCESS:
                    envPcc = result;
                    tv_status.setText(result.getDeviceName() + ": " + initialDeviceState);
                    subscribeToEvents();
                    break;
                case CHANNEL_NOT_AVAILABLE:
                    Toast.makeText(MainActivity.this, "Channel Not Available",
                            Toast.LENGTH_SHORT).show();
                    tv_status.setText("Error. Do Menu->Reset.");
                    break;
                case ADAPTER_NOT_DETECTED:
                    Toast
                            .makeText(
                                    MainActivity.this,
                                    "ANT Adapter Not Available. Built-in ANT hardware or external adapter required.",
                                    Toast.LENGTH_SHORT).show();
                    tv_status.setText("Error. Do Menu->Reset.");
                    break;
                case BAD_PARAMS:
                    // Note: Since we compose all the params ourself, we should
                    // never see this result
                    Toast.makeText(MainActivity.this, "Bad request parameters.",
                            Toast.LENGTH_SHORT).show();
                    tv_status.setText("Error. Do Menu->Reset.");
                    break;
                case OTHER_FAILURE:
                    Toast.makeText(MainActivity.this,
                            "RequestAccess failed. See logcat for details.", Toast.LENGTH_SHORT)
                            .show();
                    tv_status.setText("Error. Do Menu->Reset.");
                    break;
                case DEPENDENCY_NOT_INSTALLED:
                    tv_status.setText("Error. Do Menu->Reset.");
                    AlertDialog.Builder adlgBldr = new AlertDialog.Builder(
                            MainActivity.this);
                    adlgBldr.setTitle("Missing Dependency");
                    adlgBldr
                            .setMessage("The required service\n\""
                                    + AntPlusEnvironmentPcc.getMissingDependencyName()
                                    + "\"\n was not found. You need to install the ANT+ Plugins service or you may need to update your existing version if you already have it. Do you want to launch the Play Store to get it?");
                    adlgBldr.setCancelable(true);
                    adlgBldr.setPositiveButton("Go to Store", new OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            Intent startStore = null;
                            startStore = new Intent(Intent.ACTION_VIEW, Uri
                                    .parse("market://details?id="
                                            + AntPlusEnvironmentPcc
                                            .getMissingDependencyPackageName()));
                            startStore.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                            MainActivity.this.startActivity(startStore);
                        }
                    });
                    adlgBldr.setNegativeButton("Cancel", new OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            dialog.dismiss();
                        }
                    });

                    final AlertDialog waitDialog = adlgBldr.create();
                    waitDialog.show();
                    break;
                case USER_CANCELLED:
                    tv_status.setText("Cancelled. Do Menu->Reset.");
                    break;
                case UNRECOGNIZED:
                    Toast.makeText(MainActivity.this,
                            "Failed: UNRECOGNIZED. PluginLib Upgrade Required?",
                            Toast.LENGTH_SHORT).show();
                    tv_status.setText("Error. Do Menu->Reset.");
                    break;
                default:
                    Toast.makeText(MainActivity.this,
                            "Unrecognized result: " + resultCode, Toast.LENGTH_SHORT).show();
                    tv_status.setText("Error. Do Menu->Reset.");
                    break;
            }
        }


        private void subscribeToEvents()
        {
            envPcc.subscribeTemperatureDataEvent(new ITemperatureDataReceiver()
            {
                @Override
                public void onNewTemperatureData(final long estTimestamp,
                                                 final EnumSet<EventFlag> eventFlags, final BigDecimal currentTemperature,
                                                 final long eventCount, final BigDecimal lowLast24Hours,
                                                 final BigDecimal highLast24Hours)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            tv_estTimestamp.setText(String.valueOf(estTimestamp));

                            tv_currentTemperature.setText(String.valueOf(currentTemperature));
                            tv_eventCount.setText(String.valueOf(eventCount));
                            tv_lowLast24Hours.setText(String.valueOf(lowLast24Hours));
                            tv_highLast24Hours.setText(String.valueOf(highLast24Hours));
                        }
                    });
                }
            });
        }
    };

    // Receives state changes and shows it on the status display line
    IDeviceStateChangeReceiver mDeviceStateChangeReceiver = new IDeviceStateChangeReceiver()
    {
        @Override
        public void onDeviceStateChange(final DeviceState newDeviceState)
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    tv_status.setText(envPcc.getDeviceName() + ": " + newDeviceState);
                    if (newDeviceState == DeviceState.DEAD)
                        envPcc = null;
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_status = (TextView)findViewById(R.id.textView_Status);

        tv_estTimestamp = (TextView)findViewById(R.id.textView_EstTimestamp);

        tv_currentTemperature = (TextView)findViewById(R.id.textView_CurrentTemperature);
        tv_eventCount = (TextView)findViewById(R.id.textView_EventCount);
        tv_lowLast24Hours = (TextView)findViewById(R.id.textView_LowLast24Hours);
        tv_highLast24Hours = (TextView)findViewById(R.id.textView_HighLast24Hours);

        resetPcc();
    }

    /**
     * Resets the PCC connection to request access again and clears any existing display data.
     */
    private void resetPcc()
    {
        //Release the old access if it exists
        if(releaseHandle != null)
        {
            releaseHandle.close();
        }


        //Reset the text display
        tv_status.setText("Connecting...");

        tv_estTimestamp.setText("---");

        tv_currentTemperature.setText("---");
        tv_eventCount.setText("---");
        tv_lowLast24Hours.setText("---");
        tv_highLast24Hours.setText("---");


        releaseHandle = AntPlusEnvironmentPcc.requestAccess(this, this, mResultReceiver,
                mDeviceStateChangeReceiver);
    }

    @Override
    protected void onDestroy()
    {
        releaseHandle.close();
        super.onDestroy();
    }

}
