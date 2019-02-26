/*
 *  Copyright (C) 2019 Mark Wickham
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.wickapps.android.smartmenuquick;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.epson.EpsonCom.EpsonCom;
import com.epson.EpsonCom.EpsonCom.ALIGNMENT;
import com.epson.EpsonCom.EpsonCom.ERROR_CODE;
import com.epson.EpsonCom.EpsonCom.FONT;
import com.epson.EpsonCom.EpsonComDevice;
import com.epson.EpsonCom.EpsonComDeviceParameters;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SmartMenuService extends Service {
    public static final String DEBUG_TAG = "SmartMenuService"; // Debug TAG

    private static final String SM_CONN_THREAD_NAME = "SmartMenuServiceConn[" + DEBUG_TAG + "]";
    private static final String SM_PRNT1_THREAD_NAME = "SmartMenuServicePrnt1[" + DEBUG_TAG + "]";
    private static final String SM_PRNT2_THREAD_NAME = "SmartMenuServicePrnt2[" + DEBUG_TAG + "]";
    private static final String SM_PRNT3_THREAD_NAME = "SmartMenuServicePrnt3[" + DEBUG_TAG + "]";
    private static final String SM_RESEND_THREAD_NAME = "SmartMenuServiceResend[" + DEBUG_TAG + "]";
    private static final String SM_SAVE_THREAD_NAME = "SmartMenuServiceSave[" + DEBUG_TAG + "]";

    private static final String ACTION_START = DEBUG_TAG + ".START"; // Action to start
    private static final String ACTION_STOP = DEBUG_TAG + ".STOP"; // Action to stop
    private static final String ACTION_PRINT_TICKET1 = DEBUG_TAG + ".PRINT1"; // Action to Print a ticket
    private static final String ACTION_PRINT_TICKET2 = DEBUG_TAG + ".PRINT2"; // Action to Print a ticket
    private static final String ACTION_PRINT_TICKET3 = DEBUG_TAG + ".PRINT3"; // Action to Print a ticket
    private static final String ACTION_RESEND = DEBUG_TAG + ".RESEND"; // Action to Resend Unsent Orders
    private static final String ACTION_SAVE = DEBUG_TAG + ".SAVE"; // Action to Save an Order

    // Order submit return codes
    public static final String SM_ACK = "SMACK";  // The order was accepted
    public static final String SM_NACK = "SMNACK"; // The order was not accepted
    public static final String SM_INV = "SMINV";  // The JSON validation failed

    Locale lc;

    WakeLock mWakeLock;
    WifiLock mWifiLock;

    private File retryDir, ordersDir;

    private boolean DUPLICATE = false;
    private static final List<String> LASTORDERS = new ArrayList<String>();
    private static int LASTORDERS_MAX_COUNT = 10;

    String[] kitchenLines = new String[]{};

    Editor prefEdit;

    public static final String TAG = "SmartMenuService";

    // Connection log for the push service. Good for debugging.
    private ConnectionLogService mLog;

    // Sending back notifications to the Activity
    public static final String TICKET_BUMPED = "ticketbumped";
    public static final String NEW_ORDER = "neworder";
    public static final String NEW_MSG = "newmsg";
    public static final String PREF_STARTED = "isstarted";
    public static final String PRINT_STATUS = "printstatus";

    private Handler mConnHandler; // Separate Handler thread for socket networking
    private Handler mPrnt1Handler;
    private Handler mPrnt2Handler;
    private Handler mPrnt3Handler;
    private Handler mResendHandler;
    private Handler mSaveHandler;

    private int numDish;

    private AlarmManager mAlarmManager; // Alarm manager to perform repeating tasks
    private ConnectivityManager mConnectivityManager; // To check for connectivity changes
    private SharedPreferences mPrefs; // used to store service state and uniqueId

    //EpsonCom Objects
    private static EpsonComDevice POS1Dev, POS2Dev, POS3Dev;
    private static EpsonComDeviceParameters POS1Params, POS2Params, POS3Params;
    private static ERROR_CODE err;

    // Start
    public static void actionStart(Context ctx) {
        Intent i = new Intent(ctx, SmartMenuService.class);
        i.setAction(ACTION_START);
        ctx.startService(i);
    }

    // Stop
    public static void actionStop(Context ctx) {
        Intent i = new Intent(ctx, SmartMenuService.class);
        i.setAction(ACTION_STOP);
        ctx.startService(i);
    }

    // Print a ticket1
    public static void actionPrintTicket1(Context ctx, String str, Boolean reprint) {
        Intent i = new Intent(ctx, SmartMenuService.class);
        i.setAction(ACTION_PRINT_TICKET1);
        i.putExtra("printdata", str);
        i.putExtra("reprint", reprint);
        ctx.startService(i);
    }

    // Print a ticket2
    public static void actionPrintTicket2(Context ctx, String str, Boolean reprint) {
        Intent i = new Intent(ctx, SmartMenuService.class);
        i.setAction(ACTION_PRINT_TICKET2);
        i.putExtra("printdata", str);
        i.putExtra("reprint", reprint);
        ctx.startService(i);
    }

    // Print a ticket3
    public static void actionPrintTicket3(Context ctx, String str, Boolean reprint) {
        Intent i = new Intent(ctx, SmartMenuService.class);
        i.setAction(ACTION_PRINT_TICKET3);
        i.putExtra("printdata", str);
        i.putExtra("reprint", reprint);
        ctx.startService(i);
    }

    // Resend Unsent Orders
    public static void actionResend(Context ctx) {
        Intent i = new Intent(ctx, SmartMenuService.class);
        i.setAction(ACTION_RESEND);
        ctx.startService(i);
    }

    // Save an Order
    public static void actionSave(Context ctx,
                                  String JSONOrderStr,
                                  String resend) {
        Intent i = new Intent(ctx, SmartMenuService.class);
        i.setAction(ACTION_SAVE);
        i.putExtra("JSONOrderStr", JSONOrderStr);
        i.putExtra("resend", resend);
        ctx.startService(i);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        log("Creating service");

        try {
            mLog = new ConnectionLogService(this);
        } catch (Exception e) {
        }

        retryDir = new File(getFilesDir(), "SmartMenuRetry");
        if (!retryDir.exists()) retryDir.mkdirs();

        ordersDir = new File(getFilesDir(), "SmartMenuOrders");
        if (!ordersDir.exists()) ordersDir.mkdirs();

        kitchenLines = Global.KITCHENTXT.split("\\n");

        POS1Dev = new EpsonComDevice();
        POS1Params = new EpsonComDeviceParameters();
        POS1Params.PortType = EpsonCom.PORT_TYPE.ETHERNET;
        POS1Params.IPAddress = Global.POS1Ip;
        POS1Params.PortNumber = 9100;
        POS1Dev.setDeviceParameters(POS1Params);

        POS2Dev = new EpsonComDevice();
        POS2Params = new EpsonComDeviceParameters();
        POS2Params.PortType = EpsonCom.PORT_TYPE.ETHERNET;
        POS2Params.IPAddress = Global.POS2Ip;
        POS2Params.PortNumber = 9100;
        POS2Dev.setDeviceParameters(POS2Params);

        POS3Dev = new EpsonComDevice();
        POS3Params = new EpsonComDeviceParameters();
        POS3Params.PortType = EpsonCom.PORT_TYPE.ETHERNET;
        POS3Params.IPAddress = Global.POS3Ip;
        POS3Params.PortNumber = 9100;
        POS3Dev.setDeviceParameters(POS3Params);

        HandlerThread thread1 = new HandlerThread(SM_CONN_THREAD_NAME);
        thread1.start();
        HandlerThread thread2 = new HandlerThread(SM_PRNT1_THREAD_NAME);
        thread2.start();
        HandlerThread thread3 = new HandlerThread(SM_PRNT2_THREAD_NAME);
        thread3.start();
        HandlerThread thread4 = new HandlerThread(SM_PRNT3_THREAD_NAME);
        thread4.start();
        HandlerThread thread5 = new HandlerThread(SM_RESEND_THREAD_NAME);
        thread5.start();
        HandlerThread thread6 = new HandlerThread(SM_SAVE_THREAD_NAME);
        thread6.start();

        mConnHandler = new Handler(thread1.getLooper());
        mPrnt1Handler = new Handler(thread2.getLooper());
        mPrnt2Handler = new Handler(thread3.getLooper());
        mPrnt3Handler = new Handler(thread4.getLooper());
        mResendHandler = new Handler(thread5.getLooper());
        mSaveHandler = new Handler(thread6.getLooper());

        LASTORDERS.clear();

        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        mConnectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        PowerManager lPowerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        mWakeLock = lPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SmartMenu:WakeLockTag");
        WifiManager lWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            mWifiLock = lWifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "LockTag");
        } else {
            mWifiLock = lWifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL, "LockTag");
        }
        mWifiLock.acquire();
        mWakeLock.acquire();
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        String action = intent.getAction();
        if (action.equals(ACTION_START)) {
            //log("Action_Start");
            Global.PausedOrder = false;

            // Handle the socket comm using tasks and Thread Pool Executor
            // Thread Pool for socket communications
            mConnHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        final ExecutorService es = Executors.newFixedThreadPool(50);
                        ServerSocket socket;
                        socket = new ServerSocket(8080);

                        while (true) {
                            //Server is waiting for client
                            final Socket connection = socket.accept();
                            Runnable task = new Runnable() {
                                public void run() {
                                    try {
                                        //log("Server is accepting connections: " + connection.isConnected());
                                        BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                                        String st = input.readLine();
                                        // Send back a SMACK so the Order app will know the order is received
                                        if (!Global.PausedOrder) {
                                            PrintWriter output = new PrintWriter(connection.getOutputStream(), true);
                                            output.println(SM_ACK);
                                            //output.flush();
                                            // Should be a order so process it
                                            JSONArray tmpJson;
                                            tmpJson = new JSONArray(st);
                                            String tablename = jsonGetter2(tmpJson, "tablename").toString();
                                            generateNotification(getApplicationContext(), "New Order: Table " + tablename);
                                            log("Socket New Order: tablename=" + tablename + " length=" + st.length());
                                            // Let the Activity know about the new order
                                            Intent i = new Intent(NEW_ORDER);
                                            i.putExtra("orderdata", st);
                                            sendBroadcast(i);
                                        } else {
                                            // Cant accept the orders now
                                            PrintWriter output = new PrintWriter(connection.getOutputStream(), true);
                                            output.println(SM_NACK);
                                            //output.flush();
                                            log("Socket New Order: Paused order not accepted");
                                        }
                                    } catch (Exception e1) {
                                        log("Service: runnable e=" + e1);
                                    }
                                }
                            };
                            es.execute(task);
                        }
                    } catch (Exception e) {
                        log("Service: Socket Out e=" + e);
                    }
                }
            });
        } else if (action.equals(ACTION_PRINT_TICKET1)) {
            //log("Action_Print1");
            mPrnt1Handler.post(new Runnable() {
                @Override
                public void run() {
                    Bundle extras = intent.getExtras();
                    String value = extras.getString("printdata");
                    Boolean reprint = extras.getBoolean("reprint");
                    //log("Service: Action_print: prid=1" + "  val.len=" + value.length() + " reprint=" + reprint);
                    sendPrinter1(value, reprint);
                }
            });
        } else if (action.equals(ACTION_PRINT_TICKET2)) {
            //log("Action_Print2");
            mPrnt2Handler.post(new Runnable() {
                @Override
                public void run() {
                    Bundle extras = intent.getExtras();
                    String value = extras.getString("printdata");
                    Boolean reprint = extras.getBoolean("reprint");
                    //log("Service: Action_print: prid=2" + "  val.len=" + value.length() + " reprint=" + reprint);
                    sendPrinter2(value, reprint);
                }
            });
        } else if (action.equals(ACTION_PRINT_TICKET3)) {
            //log("Action_Print3");
            mPrnt3Handler.post(new Runnable() {
                @Override
                public void run() {
                    Bundle extras = intent.getExtras();
                    String value = extras.getString("printdata");
                    Boolean reprint = extras.getBoolean("reprint");
                    //log("Service: Action_print: prid=3" + "  val.len=" + value.length() + " reprint=" + reprint);
                    sendPrinter3(value, reprint);
                }
            });
        } else if (action.equals(ACTION_RESEND)) {
            //log("Action_Resend");
            mResendHandler.post(new Runnable() {
                @Override
                public void run() {
                    //log("Service: Action_resend");
                    sendUnsentOrders();
                }
            });
        } else if (action.equals(ACTION_SAVE)) {
            //log("Action_Save");
            mSaveHandler.post(new Runnable() {
                @Override
                public void run() {
                    Bundle extras = intent.getExtras();
                    String s1 = extras.getString("JSONOrderStr");
                    //log("Service: Action_save");
                    saveSingleOrder(s1);
                }
            });
        }
        return Service.START_NOT_STICKY;
        //return Service.START_REDELIVER_INTENT;
        //return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            if (mLog != null)
                mLog.close();
        } catch (IOException e) {
        }
        mWakeLock.release();
        mWifiLock.release();
    }

    // log helper function
    private void log(String message) {
        log(message, null);
    }

    private void log(String message, Throwable e) {
        if (e != null) {
            Log.e(TAG, message, e);
        } else {
            Log.i(TAG, message);
        }
        if (mLog != null) {
            try {
                mLog.println(message);
            } catch (IOException ex) {
            }
        }
    }

    // Query's the NetworkInfo via ConnectivityManager to return the current connected state
    private boolean isNetworkAvailable() {
        NetworkInfo info = mConnectivityManager.getActiveNetworkInfo();
        return (info == null) ? false : info.isAvailable();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        log("Ibinder...");
        return null;
    }

    // Issues a notification to inform the user that server has sent a message.
    private void generateNotification(Context context, String message) {
        int icon = R.drawable.ic_launcher;
        long when = System.currentTimeMillis();

        //NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        //Notification notification = new Notification(icon, message, when);
        //String title = context.getString(R.string.app_name);
        //Intent notificationIntent = new Intent(context, PlaceOrder.class);
        //notification.setLatestEventInfo(context, title, message, null);
        //notification.flags |= Notification.FLAG_AUTO_CANCEL;
        //notification.defaults |= Notification.DEFAULT_SOUND;
        //notification.defaults |= Notification.DEFAULT_VIBRATE;
        //notificationManager.notify(0, notification);

        Notification myNotification;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(context, QuickActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(SmartMenuService.this, 1, notificationIntent, 0);
        //Notification.Builder builder = new Notification.Builder(PlaceOrder.this);

        //builder.setAutoCancel(false);
        //builder.setTicker("this is ticker text");
        //builder.setContentTitle("SmartMenu POS");
        //builder.setContentText(message);
        //builder.setSmallIcon(R.drawable.ic_launcher);
        //builder.setContentIntent(pendingIntent);
        //builder.setOngoing(true);
        //builder.setNumber(100);
        //builder.build();

        myNotification = new Notification.Builder(context)
                .setContentTitle("SmartMenu POS")
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_launcher)
                .setAutoCancel(false)
                .setContentIntent(pendingIntent)
                .build();

        //myNotification = builder.getNotification();
        manager.notify(11, myNotification);
    }

    private Object jsonGetter2(JSONArray json, String key) {
        Object value = null;
        for (int i = 0; i < json.length(); i++) {
            try {
                JSONObject obj = json.getJSONObject(i);
                if (obj.has(key)) {
                    value = obj.get(key);
                }
            } catch (JSONException e) {
                log("jsonGetter2 Exception");
            }
        }
        return value;
    }

    private int jsonGetter3(JSONArray json, String key) {
        int v = -1;
        for (int i = 0; i < json.length(); i++) {
            try {
                JSONObject obj = json.getJSONObject(i);
                if (obj.has(key)) {
                    v = i;
                }
            } catch (JSONException e) {
                log("jsonGetter3 Exception=" + e);
            }
        }
        return v;
    }

    // check for ping connectivity
    private boolean pingIP() {
        String ip1 = Global.ProtocolPrefix + Global.ServerIP + Global.ServerReturn204;
        int status = -1;
        Boolean downloadSuccess = false;
        try {
            URL url = new URL(ip1);
            OkHttpClient.Builder b = new OkHttpClient.Builder();
            b.readTimeout(Global.ReadTimeout, TimeUnit.MILLISECONDS);
            b.writeTimeout(Global.ReadTimeout, TimeUnit.MILLISECONDS);
            b.connectTimeout(Global.ConnectTimeout, TimeUnit.MILLISECONDS);
            final OkHttpClient client = b.build();
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            Response response = client.newCall(request).execute();
            status = response.code();
            if (status == 204) {
                downloadSuccess = true;
            } else {
                downloadSuccess = false;
                log("PingIP Fail SC=" + status + " expecting 204");
            }
        } catch (Exception e) {
            log("Service: ex=" + e);
            downloadSuccess = false;
        }
        log("Service: pingIP status=" + status);
        return downloadSuccess;
    }

    private void sendUnsentOrders() {
        new Thread(new Runnable() {
            public void run() {
                if ((!Global.CheckAvailability) || pingIP()) {
                    File[] files = retryDir.listFiles();
                    for (File f : files) {
                        String fname = f.getName();
                        String postURL = Global.ProtocolPrefix + Global.ServerIP + Global.PosSaveOrderJsonURL;

                        try {
                            File readFile = new File(retryDir, fname);
                            JSONArray JSONOrder = new JSONArray(Utils.ReadLocalFile(readFile));
                            String orderid = jsonGetter2(JSONOrder, "orderid").toString();

                            // update the sendtype so resend=2
                            JSONObject obj = new JSONObject();
                            obj.put("sendtype", "2");
                            JSONOrder.put(jsonGetter3(JSONOrder, "sendtype"), obj);

                            int sc = Utils.SendMultipartJsonOrder(postURL, JSONOrder.toString(1), Global.SMID);
                            log("Resent=" + orderid + " status code=" + sc);
                            if (sc == 200) {
                                if (readFile.delete()) {
                                    writeOutFile(ordersDir, fname, JSONOrder.toString());
                                    log("file deleted:" + fname + " orderid=" + orderid + " sc=" + sc);
                                } else {
                                    log("file not deleted:" + fname + " orderid=" + orderid + " sc=" + sc);
                                }
                            }
                        } catch (Exception e) {
                            log("Resending from JSON failed");
                        }
                    }
                }
            }
        }).start();
    }

    private void saveSingleOrder(final String JSONOrderStr) {
        new Thread(new Runnable() {
            public void run() {
                String postURL = Global.ProtocolPrefix + Global.ServerIP + Global.PosSaveOrderJsonURL;
                try {
                    JSONArray JSONOrder = new JSONArray(JSONOrderStr);
                    String orderid = jsonGetter2(JSONOrder, "orderid").toString();

                    // update the sendtype so resend=1
                    JSONObject obj = new JSONObject();
                    obj.put("sendtype", "1");
                    JSONOrder.put(jsonGetter3(JSONOrder, "sendtype"), obj);

                    int sc = Utils.SendMultipartJsonOrder(postURL, JSONOrder.toString(), Global.SMID);
                    //log("Service: SaveOrder=" + orderid + " status code=" + sc);

                    // write the file
                    String savefname = orderid + ".txt";
                    if (sc == 200) {
                        writeOutFile(ordersDir, savefname, JSONOrderStr);
                        // resend any unsent orders
                        File[] files = retryDir.listFiles();
                        if (files.length > 0) {
                            //SmartMenuService.actionResend(getApplicationContext());
                            sendUnsentOrders();
                        }
                    } else {
                        writeOutFile(retryDir, savefname, JSONOrderStr);
                    }

                } catch (Exception e) {
                    log("Service: saveOrder JSON failed");
                }
            }
        }).start();
    }

    private void writeOutFile(File fildir, String fname, String fcontent) {
        File writeFile = new File(fildir, fname);
        try {
            // if the file already exists, we dont want duplicate order, so delete and resave
            if (writeFile.exists()) {
                writeFile.delete();
            }
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(writeFile, false), "UTF-8"));
            writer.write(fcontent);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            log("Service: WriteOutFile Exception: Dir=" + fildir + " fname=" + fname);
        }
    }

    private void sendPrinter3(String printdata, Boolean reprint) {
        // String:	JSON order data
        // Boolean:	True indicates a reprint was requested. This will add some special formatting and full ticket print on  P2/P3
        Integer tmpTableId = -1;
        try {
            JSONArray JSONtmp;
            JSONtmp = new JSONArray(printdata);
            String formatTicket23;
            numDish = 0;
            // Choose print size based on KitchenCodes Mode
            Boolean largeSize = false;
            if (Global.P2KitchenCodes) largeSize = true;

            if (reprint) {
                formatTicket23 = formatTicket(printdata, false, 3); // full ticket
            } else {
                formatTicket23 = formatTicket(printdata, true, 3);  // partial ticket
            }

            //log("P3 printdata=" + printdata);
            String tmpOrderId = jsonGetter2(JSONtmp, "orderid").toString();
            tmpTableId = (Integer) jsonGetter2(JSONtmp, "currenttableid");
            updatePrintStatus(3, 0, tmpTableId);
            String tmpTableName = jsonGetter2(JSONtmp, "tablename").toString();
            String tmpGuests = jsonGetter2(JSONtmp, "guests").toString();
            String tmpSendTime = jsonGetter2(JSONtmp, "sendtime").toString();
            Integer tmpTABSTATE = (Integer) jsonGetter2(JSONtmp, "tabstate");
            String tmpDeliveryNumber = jsonGetter2(JSONtmp, "deliverynumber").toString();
            String tmpDeliveryAddress = jsonGetter2(JSONtmp, "deliveryaddress").toString();
            String tmpDeliveryAddress2 = jsonGetter2(JSONtmp, "deliveryaddress2").toString();

            if (Global.POS3Enable) {
                err = POS3Dev.openDevice();
                while (err == ERROR_CODE.SUCCESS) {
                    err = POS3Dev.selectAlignment(ALIGNMENT.LEFT);
                    err = POS3Dev.sendCommand("ESC d 5");
                    if (reprint)
                        err = POS3Dev.printString("REPRINT Full Ticket", FONT.FONT_A, true, false, true, true);
                    // Show them this is an OPEN TAB
                    //if (tmpTABSTATE == 1) {
                    //	err = POS3Dev.printString("Open Tab", FONT.FONT_A, true, false, largeSize, largeSize);
                    //}

                    err = POS3Dev.printString("Guests: " + tmpGuests, FONT.FONT_A, true, false, false, false);
                    err = POS3Dev.printString("Dishes: " + numDish, FONT.FONT_A, true, false, false, false);
                    err = POS3Dev.printString("Id: " + tmpOrderId, FONT.FONT_A, true, false, false, false);
                    err = POS3Dev.sendCommand("ESC d 1");
                    if (tmpDeliveryNumber.length() > 0)
                        err = POS3Dev.printString(tmpDeliveryNumber, FONT.FONT_A, true, false, largeSize, largeSize);
                    //if (tmpDeliveryAddress.length() > 0) err = POS3Dev.printString(tmpDeliveryAddress, FONT.FONT_A, true, false, true, true);
                    //if (tmpDeliveryAddress2.length() > 0) err = POS3Dev.printString(tmpDeliveryAddress2, FONT.FONT_A, true, false, true, true);
                    if (tmpDeliveryAddress.length() > 0)
                        err = printVector(tmpDeliveryAddress, POS3Dev);
                    if (tmpDeliveryAddress2.length() > 0)
                        err = printVector(tmpDeliveryAddress2, POS3Dev);

                    err = POS3Dev.printString("Table : " + tmpTableName, FONT.FONT_A, true, false, true, true);
                    if (Global.P3PrintSentTime)
                        err = POS3Dev.printString("Sent  : " + tmpSendTime, FONT.FONT_A, true, false, largeSize, largeSize);

                    err = POS3Dev.sendCommand("ESC d 2");

                    // Use Vector mode so Chinese in Special instructions can print correctly
                    err = printVector(formatTicket23, POS3Dev);

                    err = POS3Dev.cutPaper();
                    if (!Global.Printer3Type)
                        err = POS3Dev.sendCommand("ESC B 4 2"); // Gprinter, 4 beep 200mils each
                    err = POS3Dev.closeDevice();
                    break;
                }
                if (err != ERROR_CODE.SUCCESS) {
                    String errorString = "";
                    if (err != null) errorString = EpsonCom.getErrorText(err);
                    updatePrintStatus(3, 2, tmpTableId);
                } else {
                    updatePrintStatus(3, 1, tmpTableId);
                }
            } else {
                log("p3 not enabled");
                // not enabled so just return blank status
                updatePrintStatus(3, 0, tmpTableId);
            }
        } catch (JSONException ex) {
            log("p3 fail ex=" + ex + " printdata=" + printdata);
            if (tmpTableId >= 0) updatePrintStatus(3, 2, tmpTableId);
        }
    }

    private void sendPrinter2(String printdata, Boolean reprint) {
        Integer tmpTableId = -1;
        try {
            JSONArray JSONtmp;
            JSONtmp = new JSONArray(printdata);
            String formatTicket23;
            numDish = 0;
            // Choose print size based on KitchenCodes Mode
            Boolean largeSize = false;
            if (Global.P2KitchenCodes) largeSize = true;

            if (reprint) {
                formatTicket23 = formatTicket(printdata, false, 2); // full ticket
            } else {
                formatTicket23 = formatTicket(printdata, true, 2);  // partial ticket if open tab
            }

            //log("P2 printdata=" + printdata);
            String tmpOrderId = jsonGetter2(JSONtmp, "orderid").toString();
            tmpTableId = (Integer) jsonGetter2(JSONtmp, "currenttableid");
            updatePrintStatus(2, 0, tmpTableId);
            String tmpTableName = jsonGetter2(JSONtmp, "tablename").toString();
            String tmpGuests = jsonGetter2(JSONtmp, "guests").toString();
            String tmpSendTime = jsonGetter2(JSONtmp, "sendtime").toString();
            Integer tmpTABSTATE = (Integer) jsonGetter2(JSONtmp, "tabstate");
            String tmpDeliveryNumber = jsonGetter2(JSONtmp, "deliverynumber").toString();
            String tmpDeliveryAddress = jsonGetter2(JSONtmp, "deliveryaddress").toString();
            String tmpDeliveryAddress2 = jsonGetter2(JSONtmp, "deliveryaddress2").toString();

            if (Global.POS2Enable) {
                err = POS2Dev.openDevice();
                while (err == ERROR_CODE.SUCCESS) {
                    err = POS2Dev.selectAlignment(ALIGNMENT.LEFT);
                    err = POS2Dev.sendCommand("ESC d 5");
                    if (reprint)
                        err = POS2Dev.printString("REPRINT Full Ticket", FONT.FONT_A, true, false, true, true);
                    // Show them this is an OPEN TAB
                    //if (tmpTABSTATE == 1) {
                    //	err = POS3Dev.printString("Open Tab", FONT.FONT_A, true, false, largeSize, largeSize);
                    //}

                    err = POS2Dev.printString("Guests: " + tmpGuests, FONT.FONT_A, true, false, false, false);
                    err = POS2Dev.printString("Dishes: " + numDish, FONT.FONT_A, true, false, false, false);
                    err = POS2Dev.printString("Id: " + tmpOrderId, FONT.FONT_A, true, false, false, false);
                    err = POS2Dev.sendCommand("ESC d 1");
                    if (tmpDeliveryNumber.length() > 0)
                        err = POS2Dev.printString(tmpDeliveryNumber, FONT.FONT_A, true, false, largeSize, largeSize);
                    //if (tmpDeliveryAddress.length() > 0) err = POS2Dev.printString(tmpDeliveryAddress, FONT.FONT_A, true, false, true, true);
                    //if (tmpDeliveryAddress2.length() > 0) err = POS2Dev.printString(tmpDeliveryAddress2, FONT.FONT_A, true, false, true, true);
                    if (tmpDeliveryAddress.length() > 0)
                        err = printVector(tmpDeliveryAddress, POS2Dev);
                    if (tmpDeliveryAddress2.length() > 0)
                        err = printVector(tmpDeliveryAddress2, POS2Dev);

                    err = POS2Dev.printString("Table : " + tmpTableName, FONT.FONT_A, true, false, true, true);
                    if (Global.P2PrintSentTime)
                        err = POS2Dev.printString("Sent  : " + tmpSendTime, FONT.FONT_A, true, false, largeSize, largeSize);

                    err = POS2Dev.sendCommand("ESC d 2");

                    // Use Vector mode so Chinese in Special instructions can print correctly
                    err = printVector(formatTicket23, POS2Dev);

                    err = POS2Dev.cutPaper();
                    if (!Global.Printer2Type)
                        err = POS2Dev.sendCommand("ESC B 4 2"); // Gprinter, 4 beep 200mils each
                    err = POS2Dev.closeDevice();
                    break;
                }
                if (err != ERROR_CODE.SUCCESS) {
                    String errorString = "";
                    if (err != null) errorString = EpsonCom.getErrorText(err);
                    //log("p2 2,2," + tmpTableId);
                    updatePrintStatus(2, 2, tmpTableId);
                } else {
                    //log("p2 2,1," + tmpTableId);
                    updatePrintStatus(2, 1, tmpTableId);
                }
            } else {
                log("p2 not enabled");
                // not enabled so just return blank status
                updatePrintStatus(2, 0, tmpTableId);
            }
        } catch (JSONException ex) {
            log("p2 fail ex=" + ex + " printdata=" + printdata);
            if (tmpTableId >= 0) updatePrintStatus(2, 2, tmpTableId);
        }
    }

    private void sendPrinter1(String printdata, Boolean reprint) {
        Integer tmpTableId = -1;
        try {
            // grab the needed data from the json
            JSONArray JSONtmp;
            JSONtmp = new JSONArray(printdata);

            String formatTicket1;
            if (reprint) {
                formatTicket1 = formatTicket(printdata, false, 1); // full ticket for printer1
            } else if ((Integer) jsonGetter2(JSONtmp, "tabstate") != 1) {
                formatTicket1 = formatTicket(printdata, false, 1); // full ticket for printer1
            } else {
                formatTicket1 = formatTicket(printdata, true, 1);  // partial ticket if open tab
            }

            String tmpOrderId = jsonGetter2(JSONtmp, "orderid").toString();
            tmpTableId = (Integer) jsonGetter2(JSONtmp, "currenttableid");
            updatePrintStatus(1, 0, tmpTableId);
            String tmpTableName = jsonGetter2(JSONtmp, "tablename").toString();
            String tmpGuests = jsonGetter2(JSONtmp, "guests").toString();
            String tmpSendTime = jsonGetter2(JSONtmp, "sendtime").toString();
            Integer tmpTABSTATE = (Integer) jsonGetter2(JSONtmp, "tabstate");
            Integer tmpsrc = (Integer) jsonGetter2(JSONtmp, "source");
            String tmpTicketNum = jsonGetter2(JSONtmp, "ticketnum").toString();
            String tmpDeliveryNumber = jsonGetter2(JSONtmp, "deliverynumber").toString();
            String tmpDeliveryAddress = jsonGetter2(JSONtmp, "deliveryaddress").toString();
            String tmpDeliveryAddress2 = jsonGetter2(JSONtmp, "deliveryaddress2").toString();
            String formatTicket1Tot = "RMB " + jsonGetter2(JSONtmp, "ordertotal").toString();
            //log("ordertotal=" + formatTicket1Tot);

            // Set up the Sale Type to print after the order total
            String tmpsaletype = jsonGetter2(JSONtmp, "saletype").toString();
            // Validate check ... set to cash
            //if (tmpsaletype.length() == 0) tmpsaletype ="0";
            int tmpint = Integer.valueOf(tmpsaletype);
            JSONArray tmp = new JSONArray(Global.saletypes.get(tmpint));
            String saletypestr = "";
            //if (isChinese()) saletypestr = jsonGetter2(tmp,"displayalt").toString(); else saletypestr = jsonGetter2(tmp,"display").toString();
            //don't use the device locale, use the app setting
            if (Global.EnglishLang) saletypestr = jsonGetter2(tmp, "display").toString();
            else saletypestr = jsonGetter2(tmp, "displayalt").toString();

            if (Global.POS1Enable) {
                err = POS1Dev.openDevice();
                while (err == ERROR_CODE.SUCCESS) {
                    // Loop over the number of copies
                    int copies = 1;
                    if ((tmpTABSTATE == 2) || (tmpTABSTATE == 0)) {
                        copies = Global.Printer1Copy;
                        if ((tmpsrc == 1) || (tmpsrc == 5)) {
                            copies = Global.Printer1CopyTakeOut;
                        }
                        //log("Setting P1 copies=" + Global.Printer1Copy);
                    }
                    if (reprint) copies = 1;
                    for (int i = 0; i < copies; i++) {
                        err = POS1Dev.selectAlignment(ALIGNMENT.CENTER);
                        // Only print logo or text if the tab is not open
                        if ((tmpTABSTATE != 1) || (reprint)) {
                            if (Global.POS1Logo) {
                                err = POS1Dev.sendCommand("FS p 1 0");        // print the lilys logo
                                //err = POS1Dev.sendCommand("FS p 2 0");	// print the 3sum logo
                            } else {
                                err = POS1Dev.printString(Global.CustomerName, FONT.FONT_A, true, false, true, true);
                            }
                        }
                        err = POS1Dev.sendCommand("ESC d 1");
                        err = POS1Dev.printString(tmpOrderId, FONT.FONT_A, true, false, false, false);
                        err = POS1Dev.printString("Table: " + tmpTableName, FONT.FONT_A, true, false, true, true);
                        err = POS1Dev.printString("Guests: " + tmpGuests, FONT.FONT_A, true, false, false, false);

                        if (tmpDeliveryNumber.length() > 0)
                            err = POS1Dev.printString(tmpDeliveryNumber, FONT.FONT_A, true, false, true, true);
                        //if (tmpDeliveryAddress.length() > 0) err = POS1Dev.printString(tmpDeliveryAddress, FONT.FONT_A, true, false, true, true);
                        if (tmpDeliveryAddress.length() > 0)
                            err = printVector(tmpDeliveryAddress, POS1Dev);
                        //if (tmpDeliveryAddress2.length() > 0) err = POSDev.printString(tmpDeliveryAddress2, FONT.FONT_A, true, false, true, true);

                        //if (reprint) err = POS1Dev.printString("REPRINT FULL TICKET", FONT.FONT_A, true, false, true, true);
                        if (tmpTABSTATE == 1) {
                            err = POS1Dev.printString("Open Tab", FONT.FONT_A, true, false, false, false);
                        }
                        if (tmpTABSTATE == 2) {
                            err = POS1Dev.printString("Closed Tab. Ticket Number: " + tmpTicketNum, FONT.FONT_A, true, false, false, false);
                        }
                        // print out the time sent
                        if (Global.P1PrintSentTime)
                            err = POS1Dev.printString("Sent: " + tmpSendTime, FONT.FONT_A, true, false, false, false);
                        err = POS1Dev.sendCommand("ESC d 1");

                        err = POS1Dev.selectAlignment(ALIGNMENT.LEFT);

                        // Use Vector mode so Chinese in Special instructions can print correctly
                        err = printVector(formatTicket1, POS1Dev);

                        err = POS1Dev.selectAlignment(ALIGNMENT.RIGHT);
                        // Print order total
                        err = POS1Dev.printString(formatTicket1Tot, FONT.FONT_A, true, false, true, true);

                        // Blank line
                        err = POS1Dev.sendCommand("ESC d 1");

                        if (tmpTABSTATE == 2) {
                            // Print sales type
                            //err = POS1Dev.printString(saletypestr, FONT.FONT_A, true, false, true, true);
                            err = printVector(saletypestr, POS1Dev);
                        }

                        err = POS1Dev.sendCommand("ESC d 1");
                        if ((tmpTABSTATE != 1) || (reprint)) {
                            err = POS1Dev.selectAlignment(ALIGNMENT.CENTER);
                            err = POS1Dev.printString("Thanks for visiting " + Global.CustomerNameBrief, FONT.FONT_A, true, false, false, false);
                            err = POS1Dev.sendCommand("ESC d 1");
                            err = POS1Dev.printString(Global.StoreAddress, FONT.FONT_A, true, false, false, false);
                        }
                        err = POS1Dev.cutPaper();
                    }
                    if (!Global.Printer1Type)
                        err = POS1Dev.sendCommand("ESC B 1 4"); // Gprinter, 1 beep 300mils

                    POS1Dev.closeDevice();
                    // Open the register Drawer unless the tab is open
                    if ((tmpTABSTATE != 1) && (!reprint)) {
                        if (Global.AutoOpenDrawer) openDrawer();
                    }
                    break;
                }
                if (err != ERROR_CODE.SUCCESS) {
                    String errorString = "";
                    if (err != null) errorString = EpsonCom.getErrorText(err);
                    //log("p1 1,2," + tmpTableId);
                    updatePrintStatus(1, 2, tmpTableId);
                } else {
                    //log("p1 1,1," + tmpTableId);
                    updatePrintStatus(1, 1, tmpTableId);
                }
            } else {
                log("p1 not enabled");
                // not enabled so just return blank status
                updatePrintStatus(1, 0, tmpTableId);
            }
        } catch (JSONException ex) {
            log("p1 fail ex=" + ex + " printdata=" + printdata);
            if (tmpTableId >= 0) updatePrintStatus(1, 2, tmpTableId);
        }
    }

    private ERROR_CODE printVector(String str, EpsonComDevice pr) {
        // The following code took so long to figure out, it was ridiculous
        byte[] bytesOut = null;
        Vector<Byte> sendit = null;
        try {
            //String sendString = editTextPrintString.getText().toString();
            String sendString = str;
            String sendStringEncoded = new String(sendString.getBytes("UTF-8"));
            bytesOut = sendStringEncoded.getBytes("CP936");
            sendit = new Vector<Byte>();
            for (int i = 0; i < bytesOut.length; i++) {
                sendit.add(bytesOut[i]);
            }
        } catch (UnsupportedEncodingException e) {
            log("Unsupported Encoding Exception");
        }
        err = pr.sendData(sendit);
        err = pr.sendCommand("ESC d 1");
        return err;
    }

    private void openDrawer() {
        if (isOnline()) {
            try {
                err = POS1Dev.openDevice();
                // ready to print
                err = POS1Dev.sendCommand("ESC p 0 2 2");    // open the money kick pin2 4ms on 2ms off
                err = POS1Dev.sendCommand("ESC p 1 2 2");    // open the money kick pin5 4ms on 2ms off
                err = POS1Dev.closeDevice();
            } catch (Exception ex) {
                String errorString = "";
                if (err != null) errorString = EpsonCom.getErrorText(err);
            }
        } else {
        }
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return (ni != null && ni.isAvailable() && ni.isConnected());
    }

    // send back an update on the print status
    private void updatePrintStatus(int prid, int prstatus, int tabid) {
        Intent i = new Intent(PRINT_STATUS);
        i.putExtra("printerid", prid);
        i.putExtra("printerstatus", prstatus);
        i.putExtra("tableid", tabid);
        //log("Service: UPS: prid=" + prid + " prstatus=" + prstatus + " tableid=" + tabid);
        // Send back a result if they are "Round Trippers"
        if (Global.PrintRoundTrip) sendBroadcast(i);
    }

    public static String GetTime() {
        Date dt = new Date();
        Integer hours = dt.getHours();
        String formathr = String.format("%02d", hours);
        Integer minutes = dt.getMinutes();
        String formatmin = String.format("%02d", minutes);
        Integer seconds = dt.getSeconds();
        String formatsec = String.format("%02d", seconds);
        String curTime = formathr + ":" + formatmin + ":" + formatsec;
        return curTime;
    }

    private String formatTicket(String jsonorder, Boolean partial, Integer pnum) {
        // String    The json order we want to build printer tickets for
        // Boolean   True=partial ticket so include only non-printed items using ORDERPRINTED[currentTableID]
        // Integer   The printer number 1,2 or 3
        // returns   String representation of the formatted ticket

        ArrayList<JSONArray> JSONOrderList = new ArrayList<JSONArray>();
        // First, build an ArrayList of the dishes in this JSON order
        try {
            JSONArray JSONOrderAry = new JSONArray(jsonorder);
            JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
            JSONArray jda = JSONdishObj.getJSONArray("dishes");
            numDish = jda.length();
            //log("#####: Number of dishes=" + numDish + " pnum=" + pnum);
            JSONOrderList.clear();
            if (numDish > 0) {
                JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");
                for (int i = 0; i < JSONdishesAry.length(); i++) {
                    JSONArray jd = JSONdishesAry.getJSONArray(i);
                    // Build a list of the dishes:
                    // Check if it passes the filter first, then
                    // if the partial flag is TRUE, then only include the dish if it is not already printed
                    Integer dishcatid = (Integer) jsonGetter2(jd, "categoryId");
                    // Also dont include the dish on Kitchen Printer P2 if they override it with the flag
                    boolean counterOnly = false;
                    try {
                        counterOnly = (Boolean) jsonGetter2(jd, "counterOnly");
                    } catch (Exception e) {
                        counterOnly = false;
                    }
                    Boolean includeDish = true;
                    if (pnum == 2) if (!QuickActivity.P2Filter.get(dishcatid)) includeDish = false;
                    if (pnum == 2) if (true == counterOnly) includeDish = false;
                    if (pnum == 3) if (!QuickActivity.P3Filter.get(dishcatid)) includeDish = false;
                    if (pnum == 3) if (true == counterOnly) includeDish = true;
                    if (includeDish) {
                        // Passed the filter. Now check if already printed if partial
                        if (partial) {
                            Boolean printed = (Boolean) jsonGetter2(jd, "dishPrinted");
                            if (!printed) {
                                JSONOrderList.add(jd);
                            }
                        } else {
                            JSONOrderList.add(jd);
                        }
                    }
                }
            }
        } catch (JSONException e) {
            log("json formatTicket Exception=" + e);
        }

        // Printer 1 will not use Kitchen Codes
        // Printer 2 will check Global.P2KitchenCodes
        // Printer 3 will check Global.P3KitchenCodes
        String formattedTicket = "";
        Boolean kitchenFormat = false;
        if ((pnum == 2) && (Global.P2KitchenCodes)) kitchenFormat = true;
        if ((pnum == 3) && (Global.P3KitchenCodes)) kitchenFormat = true;

        int dishCount = JSONOrderList.size();

        // Loop over each dish and build the printer strings
        //
        // COUNTER TICKET FORMAT:
        // 123456789-123456789-123456789-123456789-12
        // Id Name                      unt  Q qtytot
        // 99 aaaaaaaaaaaaaaaaaaaaaaaaa 999 x9 999.00
        //    priceOpt+options
        //    extras
        //    Special:aaaaaaaaaaaaaaaaa
        //    Discount: 99%
        //
        // Update, if no Seq Num, so the Name can be longer (28 chars)
        // aaaaaaaaaaaaaaaaaaaaaaaaaaaa
        //
        // KITCHEN TICKET FORMAT (double horizontal font size)
        // 123456789-123456789-1
        // Substituted(DishName + PriceOpt + Options + Extras) + Special:aaaaaa /r/n
        // Append "#9999" for Quantity > 1

        for (int i = 0; i < dishCount; i++) {
            try {
                JSONArray o = JSONOrderList.get(i);
                if (o != null) {
                    // Grab the print strings from JSON dish and format
                    String dishtext;

                    if (Global.EnglishLang) dishtext = jsonGetter2(o, "dishName").toString();
                    else dishtext = jsonGetter2(o, "dishName").toString();
                    if (kitchenFormat)
                        formattedTicket = formattedTicket + kitchenSubstitute(dishtext) + " ";

                    if (Global.PrintDishID) {
                        if (dishtext.length() > 25) dishtext = dishtext.substring(0, 24);
                        dishtext = addPadNum(25, dishtext);
                    } else {
                        if (dishtext.length() > 28) dishtext = dishtext.substring(0, 27);
                        dishtext = addPadNum(28, dishtext);
                    }

                    String quantity = jsonGetter2(o, "qty").toString();
                    String up = jsonGetter2(o, "priceUnitTotal").toString();
                    String qp = jsonGetter2(o, "priceQtyTotal").toString();

                    String formatid = String.format("%2d", Integer.valueOf(i + 1));
                    String formatqty = String.format("%1d", Integer.valueOf(quantity));
                    String formatup = String.format("%3d", Integer.valueOf(up));
                    String formatqp = String.format("%3d", Integer.valueOf(qp)) + ".00";

                    if (!kitchenFormat) {
                        if (Global.PrintDishID) {
                            formattedTicket = formattedTicket + formatid + "." +
                                    dishtext + " " +
                                    formatup + " x" +
                                    formatqty + " " +
                                    formatqp;
                        } else {
                            formattedTicket = formattedTicket + dishtext + " " +
                                    formatup + " x" +
                                    formatqty + " " +
                                    formatqp;
                        }
                    }

                    // Grab the Price Option + Options for the first line
                    String dishsub1a = "";
                    // Handle price option
                    String priceopt;
                    if (Global.EnglishLang) priceopt = jsonGetter2(o, "priceOptionName").toString();
                    else priceopt = jsonGetter2(o, "priceOptionName").toString();
                    if (priceopt.length() > 0) {
                        if (kitchenFormat)
                            formattedTicket = formattedTicket + kitchenSubstitute(priceopt) + " ";
                        dishsub1a = priceopt + " ";
                    }
                    // Add all the Option choices
                    String options = "";
                    String option = "";
                    JSONObject dishopt = new JSONObject();
                    dishopt = o.getJSONObject(jsonGetter3(o, "options"));
                    JSONArray dishoptAry = dishopt.getJSONArray("options");
                    if (dishoptAry.length() > 0) {
                        for (int j = 0; j < dishoptAry.length(); j++) {
                            // Grab just the optionName
                            if (Global.EnglishLang)
                                option = jsonGetter2(dishoptAry.getJSONArray(j), "optionName").toString();
                            else
                                option = jsonGetter2(dishoptAry.getJSONArray(j), "optionName").toString();
                            //if (j!=dishoptAry.length()-1) dishsubtext = dishsubtext + " ";
                            if (kitchenFormat)
                                formattedTicket = formattedTicket + kitchenSubstitute(option) + " ";
                            options = options + option + " ";
                        }
                        dishsub1a = dishsub1a + options;
                    }
                    //if ( dishsub1a.length() > 39 ) dishsub1a = dishsub1a.substring(0, 38);	// truncate for 1 line only
                    if (!kitchenFormat)
                        if (dishsub1a.length() > 0)
                            formattedTicket = formattedTicket + addPad("   " + dishsub1a);

                    // Add selected Extra choices for the next line
                    String extras = "";
                    String extra = "";
                    JSONObject dishext = new JSONObject();
                    dishext = o.getJSONObject(jsonGetter3(o, "extras"));
                    JSONArray dishextAry = dishext.getJSONArray("extras");
                    if (dishextAry.length() > 0) {
                        for (int j = 0; j < dishextAry.length(); j++) {
                            // Grab just the extraName
                            if (Global.EnglishLang)
                                extra = jsonGetter2(dishextAry.getJSONArray(j), "extraName").toString();
                            else
                                extra = jsonGetter2(dishextAry.getJSONArray(j), "extraName").toString();
                            //if (j!=dishextAry.length()-1) dishsubtext = dishsubtext + " ";
                            if (kitchenFormat)
                                formattedTicket = formattedTicket + kitchenSubstitute(extra) + " ";
                            extras = extras + extra + " ";
                        }
                        //if ( extras.length() > 39 ) extras = extras.substring(0, 38);	// truncate for 1 line only
                        if (!kitchenFormat)
                            formattedTicket = formattedTicket + addPad("   " + extras);
                    }

                    if (kitchenFormat) formattedTicket = formattedTicket.trim();

                    // Handle special Instructions on a new line
                    String specins = jsonGetter2(o, "specIns").toString();
                    if (specins.length() > 0) {
                        if (specins.length() > 31) specins = specins.substring(0, 30);
                        if (kitchenFormat)
                            formattedTicket = addPadKitc(formattedTicket) + addPadKitc("Spec:" + specins);
                        if (!kitchenFormat)
                            formattedTicket = formattedTicket + addPad("   Special: " + specins);
                    }
                    //formattedKitchenTicket = formattedKitchenTicket.trim();

                    // Handle discount information on a new line
                    String discount = jsonGetter2(o, "priceDiscount").toString();
                    if (discount.equalsIgnoreCase("100")) {
                        // No discount so don't do anything
                    } else if (discount.equalsIgnoreCase("0")) {
                        if (!kitchenFormat)
                            formattedTicket = formattedTicket + addPad("   Discount: Free Dish");
                    } else {
                        if (!kitchenFormat)
                            formattedTicket = formattedTicket + addPad("   Discount: " + discount + "%");
                    }

                    // If QTY > 1 then add indicator to the Kitchen ticket
                    if (kitchenFormat && Integer.valueOf(quantity) > 1) {
                        formattedTicket = formattedTicket + " #" + quantity + "#";
                    }

                    // Add a blank line or dash line between dishes
                    //if (i < dishCount-1) {    // in between
                    if (i < dishCount) {        // after each
                        //formattedCounterTicket = addBlankLine(formattedCounterTicket);
                        if (!kitchenFormat) formattedTicket = addDashLine(formattedTicket);
                        // pad out and then double space the kitchen ticket
                        if (kitchenFormat) formattedTicket = addPadKitc(formattedTicket);
                        if (kitchenFormat) formattedTicket = addBlankLineKitc(formattedTicket);
                    }
                }
            } catch (JSONException e) {
                log("Building Tickets Exception=" + e + " string=" + jsonorder);
                return "Print Error 1";
            }
        }
        return formattedTicket;
    }

    private String kitchenSubstitute(String str) {
        // Loop over each kitchen file entry
        int kitcCount = kitchenLines.length;
        String resultStr = str;
        for (int i = 0; i < kitcCount; i++) {
            // parse each line into columns using the divider character "|"
            String[] kitchenColumns = kitchenLines[i].split("\\|");
            // make sure the codes are valid length
            if (kitchenColumns.length == 2) {
                String s1 = kitchenColumns[0].trim();
                String s2 = kitchenColumns[1].trim();
                //log("HMMM: str=" + str + " s1=" + s1 + " s2=" + s2);
                //resultStr = str.replaceAll(s1,s2);
                if (str.equalsIgnoreCase(s1)) {
                    resultStr = s2;
                    break;
                }
            }
        }
        return resultStr;
    }

    // Some string padding functions for the printers
    private String addDashLine(String str) {
        String strDash = "";
        for (int k = 1; k <= Global.TicketCharWidth; k++) {
            strDash = strDash + "-";
        }
        str = str + strDash;
        return str;
    }

    private String addBlankLine(String str) {
        String strSpace = "";
        for (int k = 1; k <= Global.TicketCharWidth; k++) {
            strSpace = strSpace + " ";
        }
        str = str + strSpace;
        return str;
    }

    private String addBlankLineKitc(String str) {
        String strSpace = "";
        for (int k = 1; k <= Global.KitcTicketCharWidth; k++) {
            strSpace = strSpace + " ";
        }
        str = str + strSpace;
        return str;
    }

    private String addPad(String str) {
        //int addPad = Global.TicketCharWidth - str.length() + 1;
        // find the len of the str
        int length = str.length();
        int chars = 0;
        for (int i = 0; i < length; i++) {
            char ch = str.charAt(i);
            Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
            if (Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS.equals(block) ||
                    Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS.equals(block) ||
                    Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A.equals(block)) {
                chars = chars + 2;    // Double wide
            } else {
                chars = chars + 1;
            }
        }
        int intRem = ((chars - 1) % Global.TicketCharWidth);
        int intSpaces = Global.TicketCharWidth - intRem;
        for (int k = 1; k < intSpaces; k++) {
            str = str + " ";
        }
        //str = str + "\\r\\n";
        return str;
    }

    private String addPadKitc(String str) {
        // find the len of the str
        int length = str.length();
        int chars = 0;
        for (int i = 0; i < length; i++) {
            char ch = str.charAt(i);
            Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
            if (Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS.equals(block) ||
                    Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS.equals(block) ||
                    Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A.equals(block)) {
                chars = chars + 2;    // Double wide
            } else {
                chars = chars + 1;
            }
        }
        int intRem = ((chars - 1) % Global.KitcTicketCharWidth);
        int intSpaces = Global.KitcTicketCharWidth - intRem;
        for (int k = 1; k < intSpaces; k++) {
            str = str + " ";
        }
        return str;
    }

    private String addPadNum(int num, String str) {
        int addPad = num - str.length() + 1;
        for (int k = 1; k < addPad; k++) {
            str = str + " ";
        }
        //str = str + "\\r\\n";
        return str;
    }

    private boolean isChinese() {
        boolean usingAltLang = false;
        // Determine if we doing Chinese
        lc = Locale.getDefault();
        String ll = lc.getLanguage().substring(0, 2).toLowerCase();
        ;
        if ("zh".equalsIgnoreCase(ll)) {
            usingAltLang = true;
        }
        return usingAltLang;
    }

    private String getHashStr(String str) {
        String strHash = "";
        try {
            JSONArray tmp = new JSONArray(str);
            String tmptt = jsonGetter2(tmp, "tabletime").toString();
            String tmpdish = jsonGetter2(tmp, "dishes").toString();
            strHash = tmptt + tmpdish;
        } catch (Exception e) {
            log("getHashStr Exception=" + e);
        }
        return strHash;
    }

}