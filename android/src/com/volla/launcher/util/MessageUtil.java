package com.volla.launcher.util;

import androidnative.SystemDispatcher;
import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.pm.PackageManager;
import android.telephony.gsm.SmsManager;
import android.widget.Toast;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import org.qtproject.qt5.android.QtNative;
import com.volla.launcher.activity.ReceiveTextActivity;

public class MessageUtil {

    private static final String TAG = "MessageUtil";

    public static final String SEND_MESSAGE = "volla.launcher.messageAction";
    public static final String DID_SENT_MESSAGE = "volla.launcher.messageResponse";

    private static final String SMS_SEND_ACTION = "CTS_SMS_SEND_ACTION";
    private static final String SMS_DELIVERY_ACTION = "CTS_SMS_DELIVERY_ACTION";

    public static final int PERMISSIONS_REQUEST_SEND_SMS = 123;

    static {
        SystemDispatcher.addListener(new SystemDispatcher.Listener() {

            public void onDispatched(String type, Map message) {
                final Activity activity = QtNative.activity();

                if (type.equals(SEND_MESSAGE)) {

                    final String number = (String) message.get("number");
                    final String text = (String) message.get("text");

                    Runnable runnable = new Runnable () {

                        public void run() {
                            if (activity.checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {

                                if (text == null || text.length() < 1) {
                                    Map responseMessage = new HashMap();
                                    responseMessage.put("sent", false);
                                    responseMessage.put("text", "MissingText");
                                    SystemDispatcher.dispatch(DID_SENT_MESSAGE, responseMessage);
                                } else {
                                    SmsManager sm = SmsManager.getDefault();

                                    IntentFilter sendIntentFilter = new IntentFilter(SMS_SEND_ACTION);
                                    IntentFilter receiveIntentFilter = new IntentFilter(SMS_DELIVERY_ACTION);

                                    PendingIntent sentPI = PendingIntent.getBroadcast(activity.getApplicationContext(),0,new Intent(SMS_SEND_ACTION), 0);
                                    PendingIntent deliveredPI = PendingIntent.getBroadcast(activity.getApplicationContext(),0,new Intent(SMS_DELIVERY_ACTION), 0);

                                    BroadcastReceiver messageSentReceiver = new BroadcastReceiver()
                                    {
                                        @Override
                                        public void onReceive(Context context, Intent intent)
                                        {
                                            Map responseMessage = new HashMap();

                                            switch (getResultCode())
                                            {
                                                case Activity.RESULT_OK:
                                                    responseMessage.put("sent", true);
                                                    responseMessage.put("text", "MessageSent");
                                                    break;
                                                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                                                    responseMessage.put("sent", false);
                                                    responseMessage.put("text", "GenericFailure");
                                                    break;
                                                case SmsManager.RESULT_ERROR_NO_SERVICE:
                                                    responseMessage.put("sent", false);
                                                    responseMessage.put("text", "NoService");
                                                    break;
                                                case SmsManager.RESULT_ERROR_NULL_PDU:
                                                    responseMessage.put("sent", false);
                                                    responseMessage.put("text", "NullPdu");
                                                    break;
                                                case SmsManager.RESULT_ERROR_RADIO_OFF:
                                                    responseMessage.put("sent", false);
                                                    responseMessage.put("text", "RadioOff");
                                                    break;
                                            }

                                            SystemDispatcher.dispatch(DID_SENT_MESSAGE, responseMessage);
                                        }
                                    };

                                    activity.registerReceiver(messageSentReceiver, sendIntentFilter);

                                    BroadcastReceiver messageReceiveReceiver = new BroadcastReceiver()
                                    {
                                        @Override
                                        public void onReceive(Context arg0, Intent arg1)
                                        {
                                            Map responseMessage = new HashMap();

                                            switch (getResultCode())
                                            {
                                                case Activity.RESULT_OK:
                                                    responseMessage.put("sent", true);
                                                    responseMessage.put("text", "MessageDelivered");
                                                    break;
                                                case Activity.RESULT_CANCELED:
                                                    responseMessage.put("sent", false);
                                                    responseMessage.put("text", "MessageNotDelivered");
                                                break;
                                            }

                                        SystemDispatcher.dispatch(DID_SENT_MESSAGE, responseMessage);

                                        }
                                    };

                                    activity.registerReceiver(messageReceiveReceiver, receiveIntentFilter);

                                    ArrayList<String> parts =sm.divideMessage(text);

                                    ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>();
                                    ArrayList<PendingIntent> deliveryIntents = new ArrayList<PendingIntent>();

                                    for (int i = 0; i < parts.size(); i++)
                                    {
                                        sentIntents.add(PendingIntent.getBroadcast(activity.getApplicationContext(), 0, new Intent(SMS_SEND_ACTION), 0));
                                        deliveryIntents.add(PendingIntent.getBroadcast(activity.getApplicationContext(), 0, new Intent(SMS_DELIVERY_ACTION), 0));
                                    }

                                    sm.sendMultipartTextMessage(number,null, parts, sentIntents, deliveryIntents);
                                }
                            } else {
                                activity.requestPermissions(new String[] { Manifest.permission.SEND_SMS },
                                                            PERMISSIONS_REQUEST_SEND_SMS);
                            }
                        }
                    };

                    Thread thread = new Thread(runnable);
                    thread.start();
                }
            }
        });
    }
}
