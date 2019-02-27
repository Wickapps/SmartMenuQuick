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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.RawContacts;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ScaleXSpan;
import android.text.style.StyleSpan;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.epson.EpsonCom.EpsonCom;
import com.epson.EpsonCom.EpsonCom.ERROR_CODE;
import com.epson.EpsonCom.EpsonComDevice;
import com.epson.EpsonCom.EpsonComDeviceParameters;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class QuickActivity extends Activity {
    private Button newButton, closeButton, clearButton, p1Button, p2Button, p3Button, ODButton;
    private TextView tvcc, tvautoprint;

    private ImageView ivpf1, ivpf2, ivpf3;

    private Integer txtSize0, txtSize1, txtSize2, txtSize3, txtSize4;

    EditText specET;
    TextView txt2;
    Dialog dialog;
    private String incomingTableName;
    Locale lc, locale;
    Boolean autoPrint = true;
    Boolean CallStateBusy = false;

    private EditText etPassword1, etPassword2, etPassword3, etPassword4;
    private GenericTextWatcher watcher1, watcher2, watcher3, watcher4;

    SharedPreferences prefs;
    Editor prefEdit;

    //Status Thread
    Thread m_statusThread;
    boolean m_bStatusThreadStop;

    // Order submit return codes
    public static final String SM_ACK = "SMACK";  // The order was accepted
    public static final String SM_NACK = "SMNACK"; // The order was not accepted
    public static final String SM_INV = "SMINV";  // The JSON validation failed

    private static ConnectionLog mLog;

    public static ViewFlipper vfMenuTable; // (menu/tables)

    private File ordersDir;
    private File retryDir;
    private File logsDir;

    private String infoWifi;
    private String formatTicket;

    private String lastIncomingMsgData;

    ListView listOrder, listUnsent, listCalls;
    OrderAdapter orderAdapter;

    GridView gridview, gridReload;
    GridAdapter gridAdapter, reloadAdapter;

    GridView ticketview;
    TicketAdapter ticketAdapter;

    ArrayAdapter<String> unsentAdapter, last10Adapter;

    private static int MaxTickets = 20; // maximum number of tickets

    // Only one table's dishes can be displayed at any time. This ArrayList used to display the DISHES of currentTicketID in ListAdapter
    // It can be set with the dish list of any table by calling setJSONOrderList
    private static ArrayList<JSONArray> JSONOrderList = new ArrayList<JSONArray>();

    // In the future, this can be removed...
    // The following string array will hold each of the tables' orders including all dishes (represented with a JSON structure)
    private static String[] JSONOrderStr = new String[MaxTickets];

    // The ArrayList of tickets
    private static ArrayList<String> JSONTickets = new ArrayList<String>();

    //The ID of the currently selected ticket (0-relative), -1 indicates no tickets are selected
    private static int currentTicketID;

    // The print status for each of 3 printers is stored here, not in JSON for performance reasons
    // 0=Not available (gray)
    // 1=Success (green)
    // 2=Failed (red)
    private static int[] printStatus = new int[3];

    private StateListDrawable states;

    String[] rmbItem = new String[]{};
    String[] rmbItemAlt = new String[]{};
    String[] optionsItem = new String[]{};
    String[] extrasItem = new String[]{};
    String[] menuItem = new String[]{};
    String[] optionsAll = new String[]{};
    String[] extrasAll = new String[]{};
    String[] categoryAll = new String[]{};

    Button[] rbM = new Button[25];
    Button[][] rbE = new Button[5][25];
    String[][] rbEEng = new String[5][25];
    String[][] rbEAlt = new String[5][25];
    Button[][] rbO = new Button[5][25];
    Button[] butOIP = new Button[10];
    Button[] butPT = new Button[10];

    private static ArrayList<String> CategoryEng = new ArrayList<String>();
    private static ArrayList<String> CategoryAlt = new ArrayList<String>();

    public static ArrayList<Boolean> P2Filter = new ArrayList<Boolean>();
    public static ArrayList<Boolean> P3Filter = new ArrayList<Boolean>();

    private static String[] colors = new String[]{
            "#db35d5", "#81baff", "#cd5067", "#00cf70", "#fe7f3d", "#ff9a9a", "#247fca", "#00b49c", "#888888", "#83c748",
            "#9553c5", "#d8994d", "#fad666", "#f6adcd", "#3e8872", "#6f2c91", "#d24a85", "#fad666", "#fe7f3d", "#e3495a",
            "#d24a85", "#ff0000", "#aaaaaa", "#000000", "#5d9356", "#3a6e52"};

    private static String[] menubutcolors = new String[]{
            "#ffffff", "#111111", "#ffffff", "#111111", "#ffffff", "#111111", "#ffffff", "#ffffff", "#ffffff", "#111111",
            "#ffffff", "#ffffff", "#111111", "#111111", "#ffffff", "#ffffff", "#ffffff", "#ffffff", "#ffffff", "#ffffff",
            "#ffffff", "#ffffff", "#ffffff", "#ffffff", "#ffffff", "#ffffff"};

    private static String[] textColors = new String[]{
            "#ffffff", "#3a6e52", "#10000000", "#fad666", "#fe7f3d", "#3a6e52", "#247fca", "#000000", "#bbbbbb", "#111111", "#cd5067"};
    // white    green     clear       yellow    orange    ??        blue      black     gray      gray      red

    private static String[] orderSource = new String[]{"POS App", "Phone Order", "Order App", "Mobile App", "Internet Order", "Direct Entry"};
    private static String[] orderSourceColor = new String[]{"#247fca", "#5d9356", "#db35d5", "#fe7f3d", "#ff0000", "#247fca"};
    //                                                         blue       ??        ??        orange    red       blue

    ArrayList<String> dishArrayList = new ArrayList<String>();
    ArrayList<String> unsentItemList = new ArrayList<String>();
    ArrayList<String> reloadItemList = new ArrayList<String>();
    ArrayList<String> last10Calls = new ArrayList<String>();

    String OrderItem, OrderItemAlt, OrderDesc, ItemCat, ItemCatAlt;
    int Position, ItemCatId;
    boolean ItemCounterOnly;

    //EpsonCom Objects
    private static EpsonComDevice POS1Dev;
    private static EpsonComDeviceParameters POS1Params;
    private static ERROR_CODE err;

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

    private int numOptions;
    private int numExtras;

    //	Need handler for callbacks to the UI thread
    final Handler mHandler = new Handler();

    //	Create runnable for posting
    final Runnable mUpdateResults = new Runnable() {
        public void run() {
            // Grid
            menuItem = Global.MENUTXT.split("\\n");
            optionsAll = Global.OPTIONSTXT.split("\\n");
            extrasAll = Global.EXTRASTXT.split("\\n");
            categoryAll = Global.CATEGORYTXT.split("\\n");
            Global.MenuMaxItems = menuItem.length;
            Global.NumCategory = categoryAll.length;

            dishArrayList.clear();
            for (int i = 0; i < menuItem.length; i++) {
                String line = menuItem[i];
                String[] menuColumns = line.split("\\|");
                String[] menuLang = menuColumns[2].split("\\\\");
                // just keep the first 30 characters for the array list dish name
                int lngth = menuLang[0].length();
                if (lngth > 30) menuLang[0] = menuLang[0].substring(0, 30);
                dishArrayList.add(menuLang[0]);
            }

            // update the gridView
            gridview = (GridView) findViewById(R.id.gridView1);
            gridAdapter = new GridAdapter(QuickActivity.this, R.layout.array_list_item, dishArrayList);
            gridview.setAdapter(gridAdapter);

            // update the ticketsView
            ticketview = (GridView) findViewById(R.id.gridViewTickets);
            ticketAdapter = new TicketAdapter(QuickActivity.this, R.layout.ticket_item, JSONTickets);
            ticketview.setAdapter(ticketAdapter);
            ticketview.setSelection(currentTicketID);

            // Update the Order Items
            JSONOrderList.clear();
            setJSONOrderList(currentTicketID);

            listOrder = (ListView) findViewById(R.id.listOrder);
            orderAdapter = new OrderAdapter(QuickActivity.this, R.layout.list_item, JSONOrderList);
            listOrder.setAdapter(orderAdapter);

            // set the headers TextView for the Order from the JSON
            try {
                if (currentTicketID != -1) {
                    JSONArray JSONtmp = new JSONArray(JSONOrderStr[currentTicketID]);

                    String sendtype = jsonGetter2(JSONtmp, "sendtype").toString();

                    TextView tName = (TextView) findViewById(R.id.textheaderTicket);
                    tName.setText(jsonGetter2(JSONtmp, "orderid").toString());
                    tName.setTextSize(txtSize0);
                    if (sendtype.equalsIgnoreCase("1")) {
                        tName.setBackgroundColor(Color.parseColor(textColors[3]));
                    } else {
                        tName.setBackgroundColor(Color.parseColor(textColors[4]));
                    }
                    tName.setTextColor(Color.parseColor(textColors[7]));

                    // update the text for the Order Total RMB
                    TextView text = (TextView) findViewById(R.id.textTotal);
                    text.setTextSize(txtSize1);
                    text.setText(getString(R.string.tab3_rmb) + " " + Integer.toString(updateOrderTotalRMB(currentTicketID)));
                } else {
                    // No ticket is selected
                    TextView tName = (TextView) findViewById(R.id.textheaderTicket);
                    tName.setText(getString(R.string.msg_no_ticket_selected));
                    tName.setTextSize(txtSize0);
                    tName.setBackgroundColor(Color.parseColor(textColors[9]));
                    tName.setTextColor(Color.parseColor(textColors[0]));

                    // update the text for the Order Total RMB
                    TextView text = (TextView) findViewById(R.id.textTotal);
                    text.setTextSize(txtSize1);
                    text.setText("");
                }
            } catch (JSONException e) {
                log("JSON Exception setting header, table=" + currentTicketID + ", e=" + e);
            }

            updateTableButtons();
            invalidateOptionsMenu();
        }
    };

    final Runnable mOrderArrived = new Runnable() {
        public void run() {
            if (!autoPrint) {
                //Toast.makeText(PlaceOrder.this, "\n\n\nNew Order Arrived\n\n\n", Toast.LENGTH_LONG).show();
                LayoutInflater factory = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View textEntryView = factory.inflate(R.layout.new_order_dialog, null);
                final CustomDialog customDialog = new CustomDialog(QuickActivity.this);
                customDialog.setContentView(textEntryView);
                customDialog.show();
                customDialog.setCancelable(true);
                customDialog.setCanceledOnTouchOutside(true);
                //log("No autoSave- IncomingTableID=" + incomingTableID);
                TextView tv = (TextView) customDialog.findViewById(R.id.newOrderTableTxt);
                tv.setText("Table: " + incomingTableName);
            } else {
                //log("autoSave- IncomingTableID=" + incomingTableID);
            }
            updateTableButtons();
            invalidateOptionsMenu();
        }
    };

    final Runnable mMsgArrived = new Runnable() {
        public void run() {
            LayoutInflater factory = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View textEntryView = factory.inflate(R.layout.new_msg_dialog, null);
            final CustomDialog customDialog = new CustomDialog(QuickActivity.this);
            customDialog.setContentView(textEntryView);
            customDialog.show();
            customDialog.setCancelable(true);
            customDialog.setCanceledOnTouchOutside(true);
            TextView tv1 = (TextView) customDialog.findViewById(R.id.newMsgTxt);
            tv1.setText(lastIncomingMsgData);
        }
    };

    final Runnable mUpdateNetworkNotSent = new Runnable() {
        public void run() {
            LayoutInflater factory = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View textEntryView = factory.inflate(R.layout.network_send_fail_dialog, null);
            final CustomDialog customDialog = new CustomDialog(QuickActivity.this);
            customDialog.setContentView(textEntryView);
            customDialog.show();
            customDialog.setCancelable(true);
            customDialog.setCanceledOnTouchOutside(true);
        }
    };

    final Runnable mUpdateCantClose = new Runnable() {
        public void run() {
            LayoutInflater factory = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View textEntryView = factory.inflate(R.layout.cant_close_dialog, null);
            final CustomDialog customDialog = new CustomDialog(QuickActivity.this);
            customDialog.setContentView(textEntryView);
            customDialog.show();
            customDialog.setCancelable(true);
            customDialog.setCanceledOnTouchOutside(true);
        }
    };

    final Runnable mUpdatePrinters = new Runnable() {
        public void run() {
            //Toast.makeText(PlaceOrder.this, "\n\n\nPlease Print the Order\n\n\n", Toast.LENGTH_LONG).show();
            updatePrinters(currentTicketID);
        }
    };

    final Runnable mClearSelectedTicket = new Runnable() {
        public void run() {
            // clear the selected ticket
            try {
                JSONArray Jtmp = getInitialJSONOrder(currentTicketID);
                JSONTickets.remove(currentTicketID);
                removeJsonTable(currentTicketID, Jtmp);
                reorderTicketIDs();
                // Reset payment type
                JSONArray tmp = new JSONArray(Global.saletypes.get(0));
                String nam = "";
                if (isChinese()) nam = jsonGetter2(tmp, "displayalt").toString();
                else nam = jsonGetter2(tmp, "display").toString();
                String color = jsonGetter2(tmp, "color").toString();
                tvcc.setText(nam);
                tvcc.setTextColor(Color.parseColor(textColors[0]));
                tvcc.setBackgroundColor(Color.parseColor(color));
                // No ticket selected
                currentTicketID = -1;
                // back to tickets View
                vfMenuTable = (ViewFlipper) findViewById(R.id.vfMenuTable);
                vfMenuTable.setDisplayedChild(1);
                invalidateOptionsMenu();
                mHandler.post(mUpdateResults);
            } catch (Exception e) {
                log("handler mClearSelectedTicket e=" + e);
            }
        }
    };


    @Override
    public void onPause() {
        Global.PausedOrder = true;

        this.unregisterReceiver(wifiStatusReceiver);
        this.unregisterReceiver(incomingCallReceiver);
        this.unregisterReceiver(messageReceiver);


        // write out the currentTicketID
        prefEdit.putInt("currenttableid", currentTicketID);

        // write out state info for all the orders
        for (int i = 0; i < MaxTickets; i++) {
            if (tabIsOpen(i)) {
                prefEdit.putString("jsonorderstr" + i, JSONOrderStr[i]);
                //log("onPause: has ticket ID=" + i);
            } else {
                prefEdit.remove("jsonorderstr" + i);
            }
        }
        prefEdit.commit();
        log("onPause");
        super.onPause();
    }

    protected void onResume() {
        log("onResume");
        super.onResume();
        IntentFilter filter1 = new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        this.registerReceiver(wifiStatusReceiver, filter1);

        IntentFilter filter2 = new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        filter2.setPriority(99999);
        this.registerReceiver(incomingCallReceiver, filter2);

        this.registerReceiver(messageReceiver, new IntentFilter(SmartMenuService.TICKET_BUMPED));
        this.registerReceiver(messageReceiver, new IntentFilter(SmartMenuService.NEW_ORDER));
        this.registerReceiver(messageReceiver, new IntentFilter(SmartMenuService.PRINT_STATUS));

        // read in the tickets
        for (int i = 0; i < MaxTickets; i++) {
            try {
                // load the JSON strings
                String tmp = prefs.getString("jsonorderstr" + i, "");
                if (tmp.length() > 0) {
                    JSONOrderStr[i] = tmp;
                    log("onResume: setJSON ticket=" + i);
                } else {
                    JSONArray JSONtmp = getInitialJSONOrder(i);
                    JSONOrderStr[i] = JSONtmp.toString();
                }
            } catch (Exception e) {
                log("onResume Exception= " + e);
            }
        }

        Global.PausedOrder = false;
        mHandler.post(mUpdateResults);
        invalidateOptionsMenu();
    }

    @Override
    protected void onDestroy() {
        log("onDestroy");
        super.onDestroy();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log("onCreate");

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefEdit = prefs.edit();

        Global.LoggedIn = true;

        // Start the Service
        SmartMenuService.actionStart(getApplicationContext());

        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);

        // For Opening of register Drawer and printing of summarys, we need to set up printer params
        // This should be moved into the service so that it handles ALL printer activity
        POS1Dev = new EpsonComDevice();
        POS1Params = new EpsonComDeviceParameters();
        POS1Params.PortType = EpsonCom.PORT_TYPE.ETHERNET;
        POS1Params.IPAddress = Global.POS1Ip;
        POS1Params.PortNumber = 9100;
        POS1Dev.setDeviceParameters(POS1Params);

        setContentView(R.layout.placeorder);

        txtSize0 = (Utils.getFontSize(QuickActivity.this));
        txtSize1 = (int) (Utils.getFontSize(QuickActivity.this) / 1.2);
        txtSize2 = (int) (Utils.getFontSize(QuickActivity.this) / 1.4);
        txtSize3 = (int) (Utils.getFontSize(QuickActivity.this) / 1.1);
        txtSize4 = (int) (Utils.getFontSize(QuickActivity.this) * 1.2);

        // setup the button references
        setupButtons();

        // set up the View (menu/tables)
        vfMenuTable = (ViewFlipper) findViewById(R.id.vfMenuTable);
        vfMenuTable.setDisplayedChild(1);

        // grab the directory where orders, retrys and logs will be stored
        ordersDir = new File(getFilesDir(), "SmartMenuOrders");
        if (!ordersDir.exists()) ordersDir.mkdirs();
        retryDir = new File(getFilesDir(), "SmartMenuRetry");
        if (!retryDir.exists()) retryDir.mkdirs();
        logsDir = getExternalFilesDir("SmartMenuLogs");
        if (!logsDir.exists()) logsDir.mkdirs();


        try {
            mLog = new ConnectionLog(this);
        } catch (Exception e) {
        }

        // Using broadcast receiver instead of below
        //PhoneCallListener phoneListener = new PhoneCallListener();
        //TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        //telephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);

        // Setup the ActionBar
        getActionBar().setDisplayShowTitleEnabled(true);
        getActionBar().setTitle(Global.AppNameA);
        getActionBar().setSubtitle(Global.AppNameB);
        getActionBar().setDisplayUseLogoEnabled(false);
        getActionBar().setDisplayShowHomeEnabled(false);

        // setup the arrayList of menu items from the OrderItem dish name
        menuItem = Global.MENUTXT.split("\\n");
        optionsAll = Global.OPTIONSTXT.split("\\n");
        extrasAll = Global.EXTRASTXT.split("\\n");
        categoryAll = Global.CATEGORYTXT.split("\\n");

        // For each table, setup a new blank order structure if it is empty
        for (int i = 0; i < MaxTickets; i++) {
            try {
                // set up the initial order
                JSONArray JSONtmp = getInitialJSONOrder(i);
                JSONOrderStr[i] = JSONtmp.toString();

                // see if the preferences has any info to update
                String tmp = prefs.getString("jsonorderstr" + i, "");
                if (tmp.length() > 0) {
                    JSONOrderStr[i] = tmp;
                }
            } catch (JSONException e) {
                log("JSONtmp Initialize exception=" + e);
            }
            // clear all the response codes
            //responseCode[i] = 0;
        }
        // Initial print status states
        clearPrinterStatus();
        ivpf1.setBackgroundResource(R.drawable.presence_invisible);
        ivpf2.setBackgroundResource(R.drawable.presence_invisible);
        ivpf3.setBackgroundResource(R.drawable.presence_invisible);

        // set up the Categories List
        setupCatList();

        // setup the Menu Grid
        dishArrayList.clear();
        for (int i = 0; i < menuItem.length; i++) {
            String line = menuItem[i];
            String[] menuColumns = line.split("\\|");
            String[] menuLang = menuColumns[2].split("\\\\");
            // just keep the first 30 characters for the array list dish name
            int lngth = menuLang[0].length();
            if (lngth > 30) menuLang[0] = menuLang[0].substring(0, 30);
            dishArrayList.add(menuLang[0]);
        }

        GridView gridview = (GridView) findViewById(R.id.gridView1);
        gridAdapter = new GridAdapter(QuickActivity.this, R.layout.array_list_item, dishArrayList);
        gridview.setAdapter(gridAdapter);

        // update the ticketsView
        GridView ticketview = (GridView) findViewById(R.id.gridViewTickets);
        ticketAdapter = new TicketAdapter(QuickActivity.this, R.layout.ticket_item, JSONTickets);
        ticketview.setAdapter(ticketAdapter);

        // set up for the Order List
        // set the Order List to be 18% of screen width
        LinearLayout llOrderList = (LinearLayout) findViewById(R.id.col2);
        final float WIDE = this.getResources().getDisplayMetrics().widthPixels;
        int valueWide = (int) (WIDE * 0.22f);
        llOrderList.setLayoutParams(new LinearLayout.LayoutParams(valueWide, LayoutParams.FILL_PARENT));

        listOrder = (ListView) findViewById(R.id.listOrder);
        listOrder.setItemsCanFocus(true);
        orderAdapter = new OrderAdapter(QuickActivity.this, R.layout.list_item, JSONOrderList);
        listOrder.setAdapter(orderAdapter);

        listOrder.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listOrder.setMultiChoiceModeListener(new MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                // Total checked items
                final int checkedCount = listOrder.getCheckedItemCount();
                // Set the CAB title according to total checked items
                mode.setTitle(checkedCount + " Selected");
                // Calls toggleSelection method from ListViewAdapter Class
                orderAdapter.toggleSelection(position);
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                SparseBooleanArray selected = orderAdapter.getSelectedIds();
                if (item.getItemId() == R.id.delete) {
                    if (Global.UserLevel > 0) {
                        // Delete items operation
                        for (int i = (selected.size() - 1); i >= 0; i--) {
                            if (selected.valueAt(i)) {
                                int position = selected.keyAt(i);
                                deleteDishAtPosition(position);
                            }
                        }
                    } else {
                        Toast.makeText(QuickActivity.this, getString(R.string.msg_operation_not_allowed), Toast.LENGTH_LONG).show();
                    }
                    // Close CAB
                    mode.finish();
                    return true;
                }
                return false;
            }

            private void deleteDishAtPosition(int position) {
                // delete the item
                try {
                    JSONArray JSONOrderAry = new JSONArray(JSONOrderStr[currentTicketID]);
                    JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
                    JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");
                    // Remove the selected item
                    // Check for SDK version to see if we can use the JSON function directly
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        JSONdishesAry.remove(position);
                    } else {
                        // Do it the old-school way
                        JSONdishesAry = RemoveJSONArray(JSONdishesAry, position);
                    }
                    // replace it
                    JSONObject ary = new JSONObject();    // new object to store the new dishes
                    ary.put("dishes", JSONdishesAry);
                    JSONOrderAry.put(jsonGetter3(JSONOrderAry, "dishes"), ary);
                    saveJsonTable(currentTicketID, JSONOrderAry);

                    // update the total price of the order
                    ary = new JSONObject();
                    ary.put("ordertotal", updateOrderTotalRMB(currentTicketID));
                    JSONOrderAry.put(jsonGetter3(JSONOrderAry, "ordertotal"), ary);
                    saveJsonTable(currentTicketID, JSONOrderAry);
                } catch (JSONException e) {
                    log("JSON Delete Dish Exception=" + e);
                }
                mHandler.post(mUpdateResults);
                //}
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.multiselect_menu, menu);
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                orderAdapter.removeSelection();
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }
        });

        gridview.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                Position = position;
                setUpScreen();

                // check if popup or quick access
                if (dishHasChoices(position)) {
                    // Show the popup and allow for all the selections to be made
                    showThePopup();
                } else {
                    // This dish has no choices to make, so just directly add the main dish to the orderlist
                    // Create the initial JSON order and Options and Extras holders
                    JSONArray JSONOptionsAry = new JSONArray();
                    JSONArray JSONExtrasAry = new JSONArray();

                    // Reset the dish price
                    int priceUnitTotal = 0;
                    int priceUnitTotalFull = 0;
                    int priceUnitBase = 0;
                    int priceDiscount = 100;
                    int priceQtyTotal = 0;
                    int dishQty = 1;

                    // until we have the menu in a JSON structure, we need to use the following functions to get our names and pricing numbers
                    String priceOptionName = removeRMBnumber(rmbItem[0]);
                    String priceOptionNameAlt = removeRMBnumber(rmbItemAlt[0]);
                    priceUnitBase = getRMBnumber(rmbItem[0]);
                    priceUnitTotal = priceUnitTotal + priceUnitBase;
                    priceUnitTotalFull = priceUnitTotal;    // Undiscounted price for future discount calculations
                    priceQtyTotal = priceUnitTotal * dishQty;

                    // add it to the order list, new items placed at the top of the list
                    try {
                        // update the changed values
                        JSONArray JSONtmp = new JSONArray(JSONOrderStr[currentTicketID]);

                        // if first dish, then set a new tabletime
                        if (JSONOrderList.isEmpty()) {
                            jsonSetter(JSONtmp, "orderid", Utils.GetDateTime() + "-" + jsonGetter2(JSONtmp, "tablename").toString());
                            jsonSetter(JSONtmp, "tabletime", Utils.GetTime());
                        }
                        jsonSetter(JSONtmp, "tabstate", 1);
                        jsonSetter(JSONtmp, "sendtime", Utils.GetTime());
                        saveJsonTable(currentTicketID, JSONtmp);

                        // update the JSON with this item
                        JSONArray JSONOrderAry = new JSONArray(JSONOrderStr[currentTicketID]);
                        JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
                        JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");

                        JSONArray JSONDishAry = new JSONArray();
                        JSONDishAry.put(createInt("dishId", Position));
                        JSONDishAry.put(createStr("dishName", OrderItem));
                        JSONDishAry.put(createStr("dishNameAlt", OrderItemAlt));
                        JSONDishAry.put(createStr("categoryName", ItemCat));
                        JSONDishAry.put(createStr("categoryNameAlt", ItemCatAlt));
                        JSONDishAry.put(createInt("categoryId", ItemCatId));
                        JSONDishAry.put(createInt("priceOptionId", 0));
                        JSONDishAry.put(createStr("priceOptionName", priceOptionName));
                        JSONDishAry.put(createStr("priceOptionNameAlt", priceOptionNameAlt));
                        JSONDishAry.put(createInt("qty", dishQty));
                        JSONDishAry.put(createInt("priceUnitBase", priceUnitBase));
                        JSONDishAry.put(createInt("priceUnitTotal", priceUnitTotal));
                        JSONDishAry.put(createInt("priceUnitTotalFull", priceUnitTotalFull));
                        JSONDishAry.put(createInt("priceDiscount", priceDiscount));
                        JSONDishAry.put(createInt("priceQtyTotal", priceQtyTotal));
                        JSONDishAry.put(createStr("specIns", ""));
                        JSONDishAry.put(createBoolean("dishPrinted", false));
                        JSONDishAry.put(createBoolean("counterOnly", ItemCounterOnly));

                        // Add the dish Options which were built when they were selected ...
                        JSONObject aryO = new JSONObject();
                        aryO.put("options", JSONOptionsAry);
                        JSONDishAry.put(aryO);

                        // Add the dish Extras which were built when they were selected ...
                        JSONObject aryE = new JSONObject();
                        aryE.put("extras", JSONExtrasAry);
                        JSONDishAry.put(aryE);

                        JSONdishesAry.put(JSONDishAry);    // append this dish to the JSON dishes
                        JSONObject ary = new JSONObject(); // new object to store the new dishes
                        ary.put("dishes", JSONdishesAry);
                        JSONOrderAry.put(jsonGetter3(JSONOrderAry, "dishes"), ary);
                        saveJsonTable(currentTicketID, JSONOrderAry);

                        // update the total price of the order
                        ary = new JSONObject();
                        ary.put("ordertotal", updateOrderTotalRMB(currentTicketID));
                        JSONOrderAry.put(jsonGetter3(JSONOrderAry, "ordertotal"), ary);
                        saveJsonTable(currentTicketID, JSONOrderAry);

                    } catch (JSONException e) {
                        log("JSON Add Dish Exception=" + e);
                    }
                    listOrder.scrollTo(0, 0);
                    mHandler.post(mUpdateResults);
                }
            }
        });

        ticketview.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                currentTicketID = position;
                JSONOrderStr[position] = JSONTickets.get(position);
                clearPrinterStatus();
                // Display the SaleType whenever they press a ticket
                try {
                    JSONArray JSONtmp = new JSONArray(JSONOrderStr[currentTicketID]);
                    String tmpsaletype = jsonGetter2(JSONtmp, "saletype").toString();
                    int SaleType = Integer.valueOf(tmpsaletype);

                    JSONArray tmp = new JSONArray(Global.saletypes.get(SaleType));
                    String nam = "";
                    if (isChinese()) nam = jsonGetter2(tmp, "displayalt").toString();
                    else nam = jsonGetter2(tmp, "display").toString();
                    String color = jsonGetter2(tmp, "color").toString();
                    tvcc.setText(nam);
                    tvcc.setTextColor(Color.parseColor(textColors[0]));
                    tvcc.setBackgroundColor(Color.parseColor(color));
                } catch (JSONException e) {
                    log("ticketView Exception=" + e);
                }
                mHandler.post(mUpdateResults);
            }
        });

        if (Global.PayTypeEnabled) {
            try {
                JSONArray JSONtmp = new JSONArray(JSONOrderStr[currentTicketID]);
                String tmpsaletype = jsonGetter2(JSONtmp, "saletype").toString();
                int SaleType = Integer.valueOf(tmpsaletype);

                JSONArray tmp = new JSONArray(Global.saletypes.get(SaleType));
                String nam = "";
                if (isChinese()) nam = jsonGetter2(tmp, "displayalt").toString();
                else nam = jsonGetter2(tmp, "display").toString();
                String color = jsonGetter2(tmp, "color").toString();
                tvcc.setText(nam);
                tvcc.setTextColor(Color.parseColor(textColors[0]));
                tvcc.setBackgroundColor(Color.parseColor(color));
                //tvcc.setVisibility(View.VISIBLE);
                tvcc.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        try {
                            // Clicked on the payment type button, so bump it to the next type
                            JSONArray JSONtmp = new JSONArray(JSONOrderStr[currentTicketID]);
                            String tmpsaletype = jsonGetter2(JSONtmp, "saletype").toString();

                            int SaleType = Integer.valueOf(tmpsaletype);
                            int maxvalue = Global.saletypes.size();
                            int nextid = SaleType + 1;
                            if (nextid == maxvalue) nextid = 0;
                            // Set the new button color and SaleType
                            JSONArray tmp = new JSONArray(Global.saletypes.get(nextid));
                            String nam = "";
                            if (isChinese()) nam = jsonGetter2(tmp, "displayalt").toString();
                            else nam = jsonGetter2(tmp, "display").toString();
                            String color = jsonGetter2(tmp, "color").toString();
                            tvcc.setText(nam);
                            tvcc.setTextColor(Color.parseColor(textColors[0]));
                            tvcc.setBackgroundColor(Color.parseColor(color));
                            SaleType = nextid;
                            // update the json for the new SaleType
                            jsonSetter(JSONtmp, "saletype", Integer.toString(SaleType));
                            // re-save it
                            saveJsonTable(currentTicketID, JSONtmp);
                        } catch (JSONException e) {
                            log("JSONOtmp Exception saletype=" + e);
                        }
                        // update the UI
                        mHandler.post(mUpdateResults);
                    }
                });
            } catch (Exception e) {
                log("PayType Button Exception=" + e);
            }
        } else {
            tvcc.setVisibility(View.GONE);
        }

        tvautoprint.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (autoPrint) {
                    tvautoprint.setTextColor(Color.parseColor(textColors[0]));
                    tvautoprint.setBackgroundColor(Color.parseColor(textColors[9]));
                    tvautoprint.setText(getString(R.string.tab3_auto));
                    autoPrint = false;
                } else {
                    tvautoprint.setTextColor(Color.parseColor(textColors[0]));
                    tvautoprint.setBackgroundColor(Color.parseColor(textColors[6]));
                    tvautoprint.setText(getString(R.string.tab3_auto));
                    autoPrint = true;
                }
            }
        });

        clearButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // only clear tabs with printed dishes if allowed
                try {
                    // clear the selected ticket
                    JSONArray JSONtmp = getInitialJSONOrder(currentTicketID);
                    JSONTickets.remove(currentTicketID);
                    removeJsonTable(currentTicketID, JSONtmp);
                    // need to reorder the currenttableid in the tickets
                    reorderTicketIDs();
                    clearPrinterStatus();
                    // No ticket selected
                    currentTicketID = -1;
                    // back to tickets View
                    vfMenuTable = (ViewFlipper) findViewById(R.id.vfMenuTable);
                    vfMenuTable.setDisplayedChild(1);
                    invalidateOptionsMenu();
                    mHandler.post(mUpdateResults);
                } catch (JSONException e) {
                    log("JSONOtmp ClearButton Exception=" + e);
                }
            }
        });

        newButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    // bump to the next ticket
                    int numTickets = JSONTickets.size();
                    if (numTickets < MaxTickets) {
                        currentTicketID = numTickets;
                        clearPrinterStatus();
                        JSONArray JSONtmp = getInitialJSONOrder(currentTicketID);
                        JSONTickets.add(JSONtmp.toString());
                        JSONOrderStr[currentTicketID] = JSONtmp.toString();
                        saveJsonTable(currentTicketID, JSONtmp);
                        // reset the payment type
                        JSONArray tmp = new JSONArray(Global.saletypes.get(0));
                        String nam = "";
                        if (isChinese()) nam = jsonGetter2(tmp, "displayalt").toString();
                        else nam = jsonGetter2(tmp, "display").toString();
                        String color = jsonGetter2(tmp, "color").toString();
                        tvcc.setText(nam);
                        tvcc.setTextColor(Color.parseColor(textColors[0]));
                        tvcc.setBackgroundColor(Color.parseColor(color));
                    } else {
                        Toast.makeText(QuickActivity.this, getString(R.string.msg_no_tickets), Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    log("JSON newButton Ticket=" + currentTicketID + " e=" + e);
                }
                // update UI
                invalidateOptionsMenu();
                mHandler.post(mUpdateResults);
            }
        });

        closeButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (JSONOrderList.isEmpty()) {
                    AlertDialog alertDialog = new AlertDialog.Builder(QuickActivity.this).create();
                    alertDialog.setTitle(getString(R.string.tab3_empty_title));
                    alertDialog.setMessage(getString(R.string.tab3_empty_text));
                    alertDialog.setButton(getString(R.string.tab3_back), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    alertDialog.show();
                } else {
                    // Popup a payment type dialog
                    dialog = new Dialog(QuickActivity.this);
                    dialog.setContentView(R.layout.payment_type_popup);
                    // Title for the popup modify item box
                    String tit = getString(R.string.msg_saletype_choose);
                    dialog.setTitle(tit);
                    dialog.setCancelable(true);
                    dialog.setCanceledOnTouchOutside(true);

                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    layoutParams.setMargins(5, 0, 5, 5);
                    layoutParams.gravity = Gravity.LEFT;

                    LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(120, 80);
                    layoutParams3.setMargins(5, 5, 5, 5); // left,top,right.bottom
                    layoutParams3.gravity = Gravity.CENTER;

                    // set up the Headers and Buttons under this main LinearLayout
                    LinearLayout PTPMain = (LinearLayout) dialog.findViewById(R.id.PaymentTypePopupMain);
                    PTPMain.removeAllViews();

                    // New VERTICAL Linear Layout for each TextView header and BUTTON row
                    LinearLayout newLLV;
                    newLLV = new LinearLayout(QuickActivity.this);
                    newLLV.setLayoutParams(layoutParams);
                    newLLV.setOrientation(LinearLayout.VERTICAL);
                    newLLV.setHorizontalGravity(Gravity.LEFT);
                    // New HORIZONTAL Linear Layout for the buttons in the group
                    LinearLayout newLLH;
                    newLLH = new LinearLayout(QuickActivity.this);
                    newLLH.setLayoutParams(layoutParams);
                    newLLH.setOrientation(LinearLayout.HORIZONTAL);
                    newLLH.setHorizontalGravity(Gravity.CENTER);

                    // Loop through the Sales Types add a button for each one
                    for (int i = 0; i < Global.saletypes.size(); i++) {
                        try {
                            JSONArray tmp = new JSONArray(Global.saletypes.get(i));
                            String nam = "";
                            if (isChinese()) nam = jsonGetter2(tmp, "displayalt").toString();
                            else nam = jsonGetter2(tmp, "display").toString();

                            String st = String.valueOf(i);

                            butPT[i] = new Button(QuickActivity.this);
                            butPT[i].setTag(st); // put the Saletype=i in the button TAG

                            butPT[i].setText(nam);
                            butPT[i].setTextColor(Color.parseColor(textColors[0]));
                            butPT[i].setTextSize(txtSize1);
                            butPT[i].setBackgroundResource(R.drawable.border_yellow_tight);
                            butPT[i].setPadding(5, 5, 5, 5);
                            butPT[i].setGravity(Gravity.CENTER);
                            // set up the clickers which do the ADD TO ORDER function
                            butPT[i].setOnClickListener(new OnClickListener() {
                                public void onClick(View v) {
                                    String tmpSaleType = v.getTag().toString();
                                    try {
                                        JSONArray JSONtmp = new JSONArray(JSONOrderStr[currentTicketID]);
                                        jsonSetter(JSONtmp, "tabstate", 2);
                                        // update the time on the ORDER ID
                                        String tmpSendTime = Utils.GetTime();
                                        jsonSetter(JSONtmp, "sendtime", tmpSendTime);
                                        jsonSetter(JSONtmp, "saletype", tmpSaleType);

                                        // Get/Update the order total
                                        int tot = updateOrderTotalRMB(currentTicketID);
                                        // update the total price
                                        JSONObject ary = new JSONObject();
                                        ary.put("ordertotal", tot);
                                        JSONtmp.put(jsonGetter3(JSONtmp, "ordertotal"), ary);

                                        // re-save it
                                        saveJsonTable(currentTicketID, JSONtmp);

                                        // if sendOrderMode = 1 do local print, otherwise pass it up to the POS app
                                        if (Global.TOSendOrderMode == 1) {
                                            // Do printing and close out the order
                                            printTheTab(JSONtmp);
                                            // send up the order. The order will be sent up to the server by the Service
                                            SmartMenuService.actionSave(getApplicationContext(), JSONOrderStr[currentTicketID], "0");
                                            // clear the selected ticket and update
                                            mHandler.post(mClearSelectedTicket);
                                            mHandler.post(mUpdateResults);
                                        } else {
                                            // Send the tab to POS
                                            sendTheTab(JSONtmp);
                                            // send up the order, even for sendordermode = 2
                                            // No need to send it to the server cause POS app will do it
                                            //SmartMenuService.actionSave(getApplicationContext(),JSONOrderStr[currentTicketID],"0");
                                        }
                                        dialog.dismiss();
                                    } catch (JSONException e) {
                                        log("JSONOtmp CloseButton Exception curTable=" + currentTicketID + " tabstate2=" + e);
                                    }
                                }
                            });
                            newLLH.addView(butPT[i], i, layoutParams3);

                        } catch (Exception e) {
                            log("PaymentTypePopup Exception=" + e);
                        }
                    }
                    // Add the header - NOT NEEDED, THE POPUP ALREADY HAS A TITLE
                    //TextView tvtitle = new TextView(PlaceOrder.this);
                    //tvtitle.setText("Payment Type");
                    //tvtitle.setLayoutParams(layoutParams);
                    //newLLV.addView(tvtitle);
                    // Add the button row
                    newLLV.addView(newLLH);
                    // Update the main view
                    PTPMain.addView(newLLV, 0);
                    dialog.show();
                }
            }
        });

        p1Button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                reprint1();
            }
        });

        p2Button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                reprint2();
            }
        });

        p3Button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                reprint3();
            }
        });

        ODButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Open the register Drawer
                openDrawer();
            }
        });

        // No Initial Ticket
        currentTicketID = -1;
    }

    private void printTheTab(final JSONArray JSONtmp) {
        // build the tickets
        try {
            // Thread safe - we need to do everything from the passed in JSONtmp
            final ProgressDialog pd = ProgressDialog.show(QuickActivity.this, getString(R.string.tab3_sending_title), getString(R.string.tab3_sending), true, false);
            new Thread(new Runnable() {
                public void run() {
                    // Do the printing.
                    // Grab the table from the JSON
                    int table = (Integer) jsonGetter2(JSONtmp, "currenttableid");
                    // Print via the background service
                    SmartMenuService.actionPrintTicket1(getApplicationContext(), JSONtmp.toString(), false);
                    SmartMenuService.actionPrintTicket2(getApplicationContext(), JSONtmp.toString(), false);
                    SmartMenuService.actionPrintTicket3(getApplicationContext(), JSONtmp.toString(), false);
                    if (!Global.PrintRoundTrip) {
                        if (Global.POS2Enable)
                            printStatus[1] = 1;    // mark as successfully printed
                        if (Global.POS3Enable)
                            printStatus[2] = 1;    // mark as successfully printed
                    }
                    if (!Global.PrintRoundTrip) {
                        if (Global.POS1Enable)
                            printStatus[0] = 1;    // mark as successfully printed
                        int size = numberOfDishes(table);
                        for (int i = 0; i < size; i++) {
                            // mark as printed
                            setDishPrinted(table, i);
                        }
                    }
                    pd.dismiss();
                } // thread close
            }).start();
        } catch (Exception e) {
            log("SendTheTab Exception=" + e);
        }
    }

    private void sendTheTab(final JSONArray JSONtmp) {
        try {
            // Thread safe - we need to do everything from the passed in JSONtmp
            final ProgressDialog pd = ProgressDialog.show(QuickActivity.this, getString(R.string.tab3_sending_title), getString(R.string.tab3_sending), true, false);
            // Network Send
            new Thread(new Runnable() {
                public void run() {
                    int count = 0;
                    while (true) {
                        Socket s = null;
                        try {
                            JSONArray JSONtmp = new JSONArray(JSONOrderStr[currentTicketID]);
                            s = new Socket(Global.POSIp, Global.POSSocket);
                            s.setSoTimeout(Global.ConnectTimeout);
                            s.setKeepAlive(false);
                            PrintWriter output = new PrintWriter(s.getOutputStream(), true);
                            //log("Socket send=" + JSONOrderStr);
                            output.println(JSONtmp);
                            //output.flush();

                            // Check if we have the ACK from POS or TO
                            //log("After Flush, b4 input");
                            BufferedReader input = new BufferedReader(new InputStreamReader(s.getInputStream()));
                            //log("b4 readLine");
                            String st = input.readLine();
                            //log("Aftr readLine");

                            if (st.equalsIgnoreCase(SM_ACK)) {
                                //log("Recieved expected SMACK");
                                // clear the selected ticket and update
                                pd.dismiss();
                                mHandler.post(mClearSelectedTicket);
                                mHandler.post(mUpdateResults);
                                // Cleanup the socket before we break
                                if (s != null) {
                                    try {
                                        s.close();
                                    } catch (IOException eeee) {
                                        log("Close exception ee=" + eeee);
                                    }
                                }
                                break;
                            } else {
                                log("expected SMACK not received");
                                pd.dismiss();
                                mHandler.post(mUpdateNetworkNotSent);
                                // Clean up the socket before the Break
                                if (s != null) {
                                    try {
                                        s.close();
                                    } catch (IOException ee) {
                                        log("Close exception ee=" + ee);
                                    }
                                }
                                break;
                            }
                        } catch (Exception e) {
                            count++;
                            if (count < Global.SocketRetry) {
                                log("Socket2 Retry count=" + count + " e=" + e);
                                try {
                                    Thread.sleep(Global.SocketRetrySleep);
                                } catch (InterruptedException e1) {
                                    log("Sleep exception2");
                                }
                            } else {
                                log("Network send failed after retrys=" + count + " e=" + e);
                                // Cleanup the socket before we break
                                if (s != null) {
                                    try {
                                        s.close();
                                    } catch (IOException ee) {
                                        log("Close exception ee=" + ee);
                                    }
                                }
                                break;
                            }
                        }
                    }
                    pd.dismiss();
                } // thread close
            }).start();
        } catch (Exception e) {
            log("SendTheTab Exception=" + e);
        }
    }

    private void reprint1() {
        SmartMenuService.actionPrintTicket1(getApplicationContext(), JSONOrderStr[currentTicketID], true);
        if (!Global.PrintRoundTrip) {
            if (Global.POS1Enable) printStatus[0] = 1;    // mark as successfully printed
        }
    }

    private void reprint2() {
        SmartMenuService.actionPrintTicket2(getApplicationContext(), JSONOrderStr[currentTicketID], true);
        if (!Global.PrintRoundTrip) {
            if (Global.POS2Enable) printStatus[1] = 1;    // mark as successfully printed
        }
    }

    private void reprint3() {
        SmartMenuService.actionPrintTicket3(getApplicationContext(), JSONOrderStr[currentTicketID], true);
        if (!Global.PrintRoundTrip) {
            if (Global.POS3Enable) printStatus[2] = 1;    // mark as successfully printed
        }
    }

    private void setUpScreen() {
        // setup the Global Strings so the popup options have what they need
        String[] menuItem = Global.MENUTXT.split("\\n");
        String line = menuItem[Position].trim();
        String[] menuColumns = line.split("\\|");

        // If they want to over ride the category filters to select printer, store the flag
        String typeFlags = menuColumns[0];
        if (typeFlags.substring(5, 6).equals("1")) ItemCounterOnly = true;
        else ItemCounterOnly = false;

        // we have our array of columns for the selected line, no set up the language specific fields using the divider "\"
        String[] itemColumns = menuColumns[2].split("\\\\");
        String[] descColumns = menuColumns[4].split("\\\\");
        String[] rmbColumns = menuColumns[5].split("\\\\");

        // grab the category information for this dish
        String catColumns = menuColumns[1];
        ItemCatId = categoryGetIndex(catColumns);
        ItemCat = CategoryEng.get(ItemCatId).trim();
        ItemCatAlt = CategoryAlt.get(ItemCatId).trim();

        if (isChinese()) {
            OrderDesc = descColumns[1];
        } else {
            OrderDesc = descColumns[0];
        }
        OrderItem = itemColumns[0];
        OrderItemAlt = itemColumns[1];
        rmbItem = rmbColumns[0].split("%");
        rmbItemAlt = rmbColumns[1].split("%");

        String optionColumns = menuColumns[7];
        String extraColumns = menuColumns[8];

        optionsItem = optionColumns.split("%");
        extrasItem = extraColumns.split("%");
    }

    public void showThePopup() {

        dialog = new Dialog(this);

        // Now ready to display the popup chooser

        dialog.setContentView(R.layout.bigpic_popup);

        // lets scale the title on the popup box
        String tit = OrderItem;
        if (isChinese()) tit = OrderItemAlt;

        dialog.setTitle(tit);

        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        txt2 = (TextView) dialog.findViewById(R.id.Text1b);
        txt2.setText(OrderDesc);
        txt2.setTextSize(txtSize0);
        txt2.setVisibility(View.GONE);

        // set up all the elements for the dish popup dialog: 1)main 2)options 3)extras 4)special instructions
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(5, 0, 5, 5);
        layoutParams.gravity = Gravity.LEFT;

        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(150, 100);
        layoutParams3.setMargins(5, 5, 5, 5); // left,top,right.bottom
        layoutParams3.gravity = Gravity.CENTER;

        // set up the main dish Buttons
        LinearLayout llMain = (LinearLayout) dialog.findViewById(R.id.llMain);
        // new HOR lin Lay
        LinearLayout newLL;
        newLL = new LinearLayout(QuickActivity.this);
        newLL.setLayoutParams(layoutParams);
        newLL.setOrientation(LinearLayout.HORIZONTAL);
        newLL.setHorizontalGravity(Gravity.RIGHT);
        int lineLL = 0;
        int itemLL = 0;
        llMain.addView(newLL, lineLL);
        // work through the items
        for (int i = 0; i < rmbItem.length; i++) {
            rbM[i] = new Button(QuickActivity.this);
            //rbM[i].setId(i);
            rbM[i].setTag(i);
            if (isChinese()) {
                String s = rmbItemAlt[i];
                s = removeRMBnumber(s);
                if (s.length() == 0) s = tit;
                rbM[i].setText(s);
            } else {
                String s = rmbItem[i];
                s = removeRMBnumber(s);
                if (s.length() == 0) s = tit;
                rbM[i].setText(s);
            }
            rbM[i].setTextColor(Color.parseColor(textColors[0]));
            rbM[i].setTextSize(txtSize0);
            rbM[i].setBackgroundResource(R.drawable.border_yellow_tight);
            rbM[i].setPadding(5, 5, 5, 5);
            rbM[i].setGravity(Gravity.CENTER);
            // set up the clickers which do the ADD TO ORDER function
            rbM[i].setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    butMainClick(v);
                }
            });
            // new row every 5 items
            newLL.addView(rbM[i], itemLL % 5, layoutParams3);
            itemLL = itemLL + 1;
            int remainder = itemLL % 5;
            if (remainder == 0) {
                // start a new LL Horizontal
                lineLL = lineLL + 1;
                newLL = new LinearLayout(QuickActivity.this);
                newLL.setLayoutParams(layoutParams);
                newLL.setOrientation(LinearLayout.HORIZONTAL);
                llMain.addView(newLL, lineLL);
            }
        }

        // set up the Options
        numOptions = 0;        // support multiple option groups per dish
        if (!optionsItem[0].equalsIgnoreCase("none")) {
            numOptions = optionsItem.length;
            LinearLayout llOption = (LinearLayout) dialog.findViewById(R.id.llOptions);
            // set up array of BUTTONS for each group
            for (int j = 0; j < numOptions; j++) {
                // new HOR lin Lay
                newLL = new LinearLayout(QuickActivity.this);
                newLL.setLayoutParams(layoutParams);
                newLL.setOrientation(LinearLayout.HORIZONTAL);
                lineLL = 0;
                itemLL = 0;
                llOption.addView(newLL, lineLL);
                // get the index of the option
                int oo = optionsGetIndex(optionsItem[j]);
                // get the options into an array parsing by the %
                String line = optionsAll[oo];
                String[] optColumns = line.split("\\|");
                String[] Opt = optColumns[1].split("\\\\");
                String[] OptDetail = Opt[0].split("%");        // english
                String[] OptDetailAlt = Opt[1].split("%");    // alt language
                // set up the buttons
                for (int i = 0; i < OptDetail.length; i++) {
                    rbO[j][i] = new Button(QuickActivity.this);
                    rbO[j][i].setTag(j);
                    if (isChinese()) {
                        String s = OptDetailAlt[i];
                        s = removeRMBnumber(s);
                        rbO[j][i].setText(s);
                    } else {
                        String s = OptDetail[i];
                        s = removeRMBnumber(s);
                        rbO[j][i].setText(s);
                    }
                    rbO[j][i].setTextColor(Color.parseColor(textColors[3]));
                    rbO[j][i].setTextSize(txtSize0);
                    rbO[j][i].setBackgroundResource(R.drawable.border_grey2_tight);
                    rbO[j][i].setPadding(5, 5, 5, 5);
                    rbO[j][i].setGravity(Gravity.CENTER);
                    // set up the clickers which do the ADD TO ORDER function
                    rbO[j][i].setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            butOptionClick(v);
                        }
                    });
                    // new row every 5 items
                    newLL.addView(rbO[j][i], itemLL % 5, layoutParams3);
                    itemLL = itemLL + 1;
                    int remainder = itemLL % 5;
                    if (remainder == 0) {
                        // start a new LL Horizontal
                        lineLL = lineLL + 1;
                        newLL = new LinearLayout(QuickActivity.this);
                        newLL.setLayoutParams(layoutParams);
                        newLL.setOrientation(LinearLayout.HORIZONTAL);
                        llOption.addView(newLL, lineLL);
                    }
                }
                rbO[j][0].setBackgroundResource(R.drawable.border_grey_tight);
                rbO[j][0].setTag(1000 + (Integer) rbO[j][0].getTag());
            }
            addDividerGap(llOption);
        }

        // set up the EXTRAS check boxes
        numExtras = 0;

        //Toast.makeText(PlaceOrder.this, "B4Inside=" + extrasItem[0] + " Len=" + extrasItem[0].length(), Toast.LENGTH_SHORT).show();
        if (!extrasItem[0].equalsIgnoreCase("none")) {
            //Toast.makeText(PlaceOrder.this, "Inside=" + extrasItem[0] + " Len=" + extrasItem[0].length(), Toast.LENGTH_SHORT).show();
            numExtras = extrasItem.length;
            LinearLayout llExtra = (LinearLayout) dialog.findViewById(R.id.llExtras);
            // set up array of BUTTONS for each group
            for (int j = 0; j < numExtras; j++) {
                // new HOR lin Lay
                newLL = new LinearLayout(QuickActivity.this);
                newLL.setLayoutParams(layoutParams);
                newLL.setOrientation(LinearLayout.HORIZONTAL);
                lineLL = 0;
                itemLL = 0;
                llExtra.addView(newLL, lineLL);
                // get the index of the extra
                int ee = extrasGetIndex(extrasItem[j]);
                // get the options into an array parsing by the %
                String line = extrasAll[ee];
                String[] extColumns = line.split("\\|");
                String[] Ext = extColumns[1].split("\\\\");
                String[] ExtDetail = Ext[0].split("%");        // english
                String[] ExtDetailAlt = Ext[1].split("%");    // alt language

                // set up the buttons
                for (int i = 0; i < ExtDetail.length; i++) {
                    rbE[j][i] = new Button(QuickActivity.this);
                    final Integer btnID = 100 * j + i;
                    rbE[j][i].setId(btnID);
                    rbE[j][i].setTag(j);

                    String s = ExtDetail[i];
                    s = removeRMBnumber(s);
                    rbEEng[j][i] = ExtDetail[i];
                    rbEAlt[j][i] = ExtDetailAlt[i];
                    if (isChinese()) {
                        s = ExtDetailAlt[i];
                        s = removeRMBnumber(s);
                        rbE[j][i].setText(s);
                    } else {
                        rbE[j][i].setText(s);

                    }
                    rbE[j][i].setTextColor(Color.parseColor(textColors[4]));
                    rbE[j][i].setTextSize(txtSize0);
                    rbE[j][i].setBackgroundResource(R.drawable.border_grey2_tight);
                    rbE[j][i].setPadding(5, 5, 5, 5);
                    rbE[j][i].setGravity(Gravity.CENTER);
                    // set up the clickers which do the ADD TO ORDER function
                    rbE[j][i].setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            butExtraClick(v);
                        }
                    });

                    rbE[j][i].setOnLongClickListener(new View.OnLongClickListener() {
                        public boolean onLongClick(View v) {
                            butExtraLongClick(v);
                            return true;
                        }
                    });
                    // new row every 5 items
                    newLL.addView(rbE[j][i], itemLL % 5, layoutParams3);
                    itemLL = itemLL + 1;
                    int remainder = itemLL % 5;
                    if (remainder == 0) {
                        // start a new LL Horizontal
                        lineLL = lineLL + 1;
                        newLL = new LinearLayout(QuickActivity.this);
                        newLL.setLayoutParams(layoutParams);
                        newLL.setOrientation(LinearLayout.HORIZONTAL);
                        llExtra.addView(newLL, lineLL);
                    }
                }
            }
        }
        //now that the dialog is set up, it's time to show it
        dialog.show();
    }

    public void butOptionClick(View v) {
        // clear all buttons in the group
        int j = (Integer) v.getTag();
        j = j % 1000;
        int oo = optionsGetIndex(optionsItem[j]);
        String line = optionsAll[oo];
        String[] optColumns = line.split("\\|");
        String[] Opt = optColumns[1].split("\\\\");
        String[] OptDetail = Opt[0].split("%");        // english only for the ticket
        for (int i = 0; i < OptDetail.length; i++) {
            rbO[j][i].setBackgroundResource(R.drawable.border_grey2_tight);
            rbO[j][i].setTag(j);
        }
        // set the selected
        v.setBackgroundResource(R.drawable.border_grey_tight);
        v.setTag(1000 + (Integer) v.getTag());
    }

    private void butExtraClick(View v) {
        // set the selected
        v.setBackgroundResource(R.drawable.border_grey_tight);
        v.setTag(1000 + (Integer) v.getTag());
    }

    public void butExtraLongClick(View v) {
        int j = (Integer) v.getTag() % 1000;
        // set the selected
        v.setBackgroundResource(R.drawable.border_grey2_tight);
        v.setTag(j);
    }

    // This routine is called when a Price selector is pressed, this will add the dish and its
    // selections (options/extras/qty/specins) to the order ticket
    public void butMainClick(View v) {
        int value = (Integer) v.getTag();
        // load up the dish main option (price selector)

        // Create the initial JSON order and Options and Extras holders
        JSONArray JSONOptionsAry = new JSONArray();
        JSONArray JSONExtrasAry = new JSONArray();

        // Reset the dish price
        int priceUnitTotal = 0;
        int priceUnitTotalFull = 0;
        int priceUnitBase = 0;
        int priceDiscount = 100;
        int priceQtyTotal = 0;
        int dishQty = 1;

        // until we have a JSON menu, we need to use the following functions to get our pricing numbers
        String priceOptionName = removeRMBnumber(rmbItem[value]);
        String priceOptionNameAlt = removeRMBnumber(rmbItemAlt[value]);

        // load up the dish options (non-price) if available
        if (numOptions > 0) {
            for (int j = 0; j < numOptions; j++) {
                int oo = optionsGetIndex(optionsItem[j]);
                String line = optionsAll[oo];
                String[] optColumns = line.split("\\|");
                String[] Opt = optColumns[1].split("\\\\");
                String[] OptDetail = Opt[0].split("%");        // english only for the ticket
                String[] OptDetailAlt = Opt[1].split("%");    // alt language

                for (int i = 0; i < OptDetail.length; i++) {
                    if ((Integer) (rbO[j][i].getTag()) >= 1000) {
                        String OptDet = OptDetail[i];        // english only for the ticket
                        try {
                            // Append each Dish Option to the global array
                            JSONArray aryO = new JSONArray();
                            aryO.put(createInt("optionId", j));

                            String orderSecondaryTxt = OptDetail[i].trim();
                            String orderSecondaryTxtAlt = OptDetailAlt[i].trim();

                            aryO.put(createInt("optionPrice", getRMBnumber(orderSecondaryTxt)));
                            priceUnitTotal = priceUnitTotal + getRMBnumber(orderSecondaryTxt);

                            aryO.put(createStr("optionName", removeRMBnumber(orderSecondaryTxt)));
                            aryO.put(createStr("optionNameAlt", removeRMBnumber(orderSecondaryTxtAlt)));
                            JSONOptionsAry.put(aryO);    // append the dish options
                        } catch (JSONException e) {
                            log("JSON Add Dish Options Exception=" + e);
                        }
                    }
                }
            }
        }

        // load up the dish extras
        if (numExtras > 0) {
            for (int j = 0; j < numExtras; j++) {
                int ee = extrasGetIndex(extrasItem[j]);
                String line = extrasAll[ee];
                String[] extColumns = line.split("\\|");
                String[] Ext = extColumns[1].split("\\\\");
                String[] ExtDetail = Ext[0].split("%");        // english
                String[] ExtDetailAlt = Ext[1].split("%");    // alt language

                for (int i = 0; i < ExtDetail.length; i++) {
                    if ((Integer) (rbE[j][i].getTag()) >= 1000) {
                        //get the modified text from the button
                        ////int k = (Integer)rbE[j][i].getId();
                        ////Button but = (Button) dialog.findViewById(k);
                        ////String ExtDet = but.getText().toString();
                        String ExtDet = rbEEng[j][i];
                        String ExtDetAlt = rbEAlt[j][i];

                        try {
                            // Append selected Dish Extras to the global array
                            JSONArray aryE = new JSONArray();
                            aryE.put(createInt("extraId", j));
                            aryE.put(createStr("extraItem", extrasItem[j]));
                            aryE.put(createInt("extraIndex", ee));
                            aryE.put(createInt("extraPrice", getRMBnumber(ExtDet)));
                            priceUnitTotal = priceUnitTotal + getRMBnumber(ExtDet);
                            aryE.put(createStr("extraName", removeRMBnumber(ExtDet)));
                            aryE.put(createStr("extraNameAlt", removeRMBnumber(ExtDetAlt)));
                            JSONExtrasAry.put(aryE);    // append the dish extras
                        } catch (JSONException e) {
                            log("JSON Add Dish Extras Exception=" + e);
                        }
                    }
                }
            }
        }

        // note that priceUnitTotal may already contain value from the Options and Extras processing
        priceUnitBase = getRMBnumber(rmbItem[value]);
        priceUnitTotal = priceUnitTotal + priceUnitBase;
        priceUnitTotalFull = priceUnitTotal;    // Undiscounted price for future discount calculations
        priceQtyTotal = priceUnitTotal * dishQty;

        // add it to the order list, new items placed at the top of the list
        try {
            // update the JSON with this item
            JSONArray JSONOrderAry = new JSONArray(JSONOrderStr[currentTicketID]);
            JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
            JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");

            JSONArray JSONDishAry = new JSONArray();
            JSONDishAry.put(createInt("dishId", Position));
            JSONDishAry.put(createStr("dishName", OrderItem));
            JSONDishAry.put(createStr("dishNameAlt", OrderItemAlt));
            JSONDishAry.put(createStr("categoryName", ItemCat));
            JSONDishAry.put(createStr("categoryNameAlt", ItemCatAlt));
            JSONDishAry.put(createInt("categoryId", ItemCatId));
            JSONDishAry.put(createInt("priceOptionId", value));
            JSONDishAry.put(createStr("priceOptionName", priceOptionName));
            JSONDishAry.put(createStr("priceOptionNameAlt", priceOptionNameAlt));
            JSONDishAry.put(createInt("qty", dishQty));
            JSONDishAry.put(createInt("priceUnitBase", priceUnitBase));
            JSONDishAry.put(createInt("priceUnitTotal", priceUnitTotal));
            JSONDishAry.put(createInt("priceUnitTotalFull", priceUnitTotalFull));
            JSONDishAry.put(createInt("priceDiscount", priceDiscount));
            JSONDishAry.put(createInt("priceQtyTotal", priceQtyTotal));
            JSONDishAry.put(createStr("specIns", ""));
            JSONDishAry.put(createBoolean("dishPrinted", false));
            JSONDishAry.put(createBoolean("counterOnly", ItemCounterOnly));

            // Add the dish Options which were built when they were selected ...
            JSONObject aryO = new JSONObject();
            aryO.put("options", JSONOptionsAry);
            JSONDishAry.put(aryO);

            // Add the dish Extras which were built when they were selected ...
            JSONObject aryE = new JSONObject();
            aryE.put("extras", JSONExtrasAry);
            JSONDishAry.put(aryE);

            // Update the order dishes
            JSONdishesAry.put(JSONDishAry);     // append this dish to the JSON dishes
            JSONObject ary = new JSONObject(); // new object to store the new dishes
            ary.put("dishes", JSONdishesAry);
            JSONOrderAry.put(jsonGetter3(JSONOrderAry, "dishes"), ary);
            saveJsonTable(currentTicketID, JSONOrderAry);

            // update the tabstate
            ary = new JSONObject();
            ary.put("tabstate", 1);
            JSONOrderAry.put(jsonGetter3(JSONOrderAry, "tabstate"), ary);
            saveJsonTable(currentTicketID, JSONOrderAry);

            // update the total price of the order
            ary = new JSONObject();
            ary.put("ordertotal", updateOrderTotalRMB(currentTicketID));
            JSONOrderAry.put(jsonGetter3(JSONOrderAry, "ordertotal"), ary);
            saveJsonTable(currentTicketID, JSONOrderAry);

        } catch (JSONException e) {
            log("JSON Add Dish Exception=" + e);
        }
        listOrder.scrollTo(0, 0);
        dialog.dismiss();
        mHandler.post(mUpdateResults);
    }

    public void showOrderPopup(final int position) {
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.order_popup);
        // Title for the popup modify item box
        String tit = getString(R.string.msg_modify_item);
        dialog.setTitle(tit);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(5, 0, 5, 5);
        layoutParams.gravity = Gravity.LEFT;

        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(120, 80);
        layoutParams3.setMargins(5, 5, 5, 5); // left,top,right.bottom
        layoutParams3.gravity = Gravity.CENTER;

        // set up the Headers and Buttons under this main LinearLayout
        LinearLayout OIPMain = (LinearLayout) dialog.findViewById(R.id.OrderItemPopupMain);
        OIPMain.removeAllViews();

        // Walk through the modifier ArrayList, which contains JSON entries for 3 types of modifiers:
        // Type 0 - apply discounts to the dish
        // Type 1 - change the quantity of a dish
        // Type 2 - add special instructions to the dish
        // Lastly, Special Instructions will also have a CUSTOM option --->> Need to port to the POS app.

        // Work from last to first so they appear in the order as they are listed in the JSON modifiers file
        for (int i = Global.modifiers.size() - 1; i >= 0; i--) {
            try {
                JSONArray tmp = new JSONArray(Global.modifiers.get(i));
                String nam = "";
                if (isChinese()) nam = jsonGetter2(tmp, "namealt").toString();
                else nam = jsonGetter2(tmp, "name").toString();
                JSONObject JSONdishObj = tmp.getJSONObject(jsonGetter3(tmp, "items"));
                JSONArray JSONitems = JSONdishObj.getJSONArray("items");

                // New VERTICAL Linear Layout for each TextView header and BUTTON row
                LinearLayout newLLV;
                newLLV = new LinearLayout(QuickActivity.this);
                newLLV.setLayoutParams(layoutParams);
                newLLV.setOrientation(LinearLayout.VERTICAL);
                newLLV.setHorizontalGravity(Gravity.LEFT);
                // New HORIZONTAL Linear Layout for the buttons in the group
                LinearLayout newLLH;
                newLLH = new LinearLayout(QuickActivity.this);
                newLLH.setLayoutParams(layoutParams);
                newLLH.setOrientation(LinearLayout.HORIZONTAL);
                newLLH.setHorizontalGravity(Gravity.CENTER);

                for (int j = 0; j < JSONitems.length(); j++) {
                    JSONArray ji = JSONitems.getJSONArray(j);
                    butOIP[j] = new Button(QuickActivity.this);
                    butOIP[j].setTag(ji.toString()); // put the whole JSON item into the tag so we know what to do when it gets pressed
                    if (isChinese()) butOIP[j].setText(jsonGetter2(ji, "titlealt").toString());
                    else butOIP[j].setText(jsonGetter2(ji, "title").toString());
                    butOIP[j].setTextColor(Color.parseColor(textColors[0]));
                    butOIP[j].setTextSize(txtSize1);
                    butOIP[j].setBackgroundResource(R.drawable.border_yellow_tight);
                    butOIP[j].setPadding(5, 5, 5, 5);
                    butOIP[j].setGravity(Gravity.CENTER);
                    // set up the clickers which do the ADD TO ORDER function
                    butOIP[j].setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            butUpdateClick(v, position);
                        }
                    });
                    newLLH.addView(butOIP[j], j, layoutParams3);
                }
                // Add the header
                TextView tvtitle = new TextView(QuickActivity.this);
                tvtitle.setText(nam);
                tvtitle.setLayoutParams(layoutParams);
                newLLV.addView(tvtitle);
                // Add the button row
                newLLV.addView(newLLH);
                // Update the main view
                OIPMain.addView(newLLV, 0);
            } catch (Exception e) {
                log("orderItemPopup Exception=" + e);
            }
        }
        // Add the final custom spec ins button
        // New VERTICAL Linear Layout for each TextView header and BUTTON row
        LinearLayout newLLV;
        newLLV = new LinearLayout(QuickActivity.this);
        newLLV.setLayoutParams(layoutParams);
        newLLV.setOrientation(LinearLayout.VERTICAL);
        newLLV.setHorizontalGravity(Gravity.LEFT);
        // New HORIZONTAL Linear Layout for the buttons in the group
        LinearLayout newLLH;
        newLLH = new LinearLayout(QuickActivity.this);
        newLLH.setLayoutParams(layoutParams);
        newLLH.setOrientation(LinearLayout.HORIZONTAL);
        newLLH.setHorizontalGravity(Gravity.CENTER);
        // Add the button
        Button butCustSI = new Button(QuickActivity.this);
        butCustSI.setText(getString(R.string.special_ins_custom));
        butCustSI.setTextColor(Color.parseColor(textColors[0]));
        butCustSI.setTextSize(txtSize1);
        butCustSI.setBackgroundResource(R.drawable.border_yellow_tight);
        butCustSI.setPadding(5, 5, 5, 5);
        butCustSI.setGravity(Gravity.CENTER);
        // set up the clickers which do the ADD TO ORDER function
        butCustSI.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                try {
                    // Custom dialog needed to get the instructions. position=the dish index
                    final Dialog dialogAS = new Dialog(QuickActivity.this);

                    dialogAS.setContentView(R.layout.special_instruction);
                    dialogAS.setCancelable(true);
                    dialogAS.setCanceledOnTouchOutside(true);

                    final JSONArray JSONOrderAry = new JSONArray(JSONOrderStr[currentTicketID]);
                    JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
                    JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");
                    final JSONArray jd = JSONdishesAry.getJSONArray(position);

                    // lets scale the title on the popup box
                    String tit = getString(R.string.special_ins_title);
                    dialogAS.setTitle(tit);

                    TextView AStext = (TextView) dialogAS.findViewById(R.id.SItext);
                    AStext.setText(getString(R.string.special_ins_text1));
                    AStext.setTextSize(txtSize0);
                    AStext.setTextColor(Color.parseColor(textColors[0]));
                    // edit text box is next
                    Button AScancel = (Button) dialogAS.findViewById(R.id.SIcancel);
                    AScancel.setTextSize(txtSize1);
                    AScancel.setText(getString(R.string.tab2_si_cancel));
                    AScancel.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            dialogAS.dismiss();
                        }
                    });
                    Button ASsave = (Button) dialogAS.findViewById(R.id.SIadd);
                    ASsave.setTextSize(txtSize1);
                    ASsave.setText(getString(R.string.tab2_si_save));
                    ASsave.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            specET = (EditText) dialogAS.findViewById(R.id.SIedit);
                            String specins = specET.getText().toString();
                            specins = specins.replaceAll("[^\\p{L}\\p{N}\\s]", "");
                            if (specins.length() > 0) specins = specins + " ";
                            // Save it
                            jsonSetter(jd, "specIns", specins);
                            // update everything
                            saveJsonTable(currentTicketID, JSONOrderAry);
                            mHandler.post(mUpdateResults);
                            // Clear soft keyboard
                            specET.clearFocus();
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm != null)
                                imm.hideSoftInputFromWindow(specET.getWindowToken(), 0);
                            dialogAS.dismiss();
                        }
                    });
                    // Set the initial value of the ET box
                    specET = (EditText) dialogAS.findViewById(R.id.SIedit);
                    String specins = jsonGetter2(jd, "specIns").toString();
                    specET.setText(specins);

                    dialogAS.show();
                } catch (Exception e) {
                    log("orderItemPopup2 Exception=" + e);
                }
            }
        });
        newLLH.addView(butCustSI, 0, layoutParams3);
        // Add the header
        TextView tvtitle = new TextView(QuickActivity.this);
        tvtitle.setText(getString(R.string.special_ins_title));
        tvtitle.setLayoutParams(layoutParams);
        newLLV.addView(tvtitle);
        // Add the button row
        newLLV.addView(newLLH);
        // Update the main view
        OIPMain.addView(newLLV, 0);

        dialog.show();
    }

    public void butUpdateClick(View v, int position) {
        String tmp = v.getTag().toString();
        try {
            // The button tag has all the needed information encoded in JSON, so grab the type to get started
            JSONArray ji = new JSONArray(tmp);
            int type = (Integer) jsonGetter2(ji, "type");

            JSONArray JSONOrderAry = new JSONArray(JSONOrderStr[currentTicketID]);
            JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
            JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");
            int dishnum = position;
            JSONArray jd = JSONdishesAry.getJSONArray(dishnum);
            String specins = jsonGetter2(jd, "specIns").toString();

            // Handle discounts
            if (type == 0) {
                if (Global.UserLevel > 0) {
                    int discount = (Integer) jsonGetter2(ji, "discount");
                    int putf = (Integer) jsonGetter2(jd, "priceUnitTotalFull");
                    int qty = (Integer) jsonGetter2(jd, "qty");
                    float ratio = ((float) discount / (float) 100.0);
                    int newput = (int) ((float) putf * ratio);
                    jsonSetter(jd, "priceUnitTotal", newput);
                    jsonSetter(jd, "priceDiscount", discount);
                    jsonSetter(jd, "priceQtyTotal", newput * qty);
                    // update everything
                    saveJsonTable(currentTicketID, JSONOrderAry);
                    // and then update the total price of the order
                    JSONObject ary = new JSONObject();
                    ary.put("ordertotal", updateOrderTotalRMB(currentTicketID));
                    JSONOrderAry.put(jsonGetter3(JSONOrderAry, "ordertotal"), ary);
                    saveJsonTable(currentTicketID, JSONOrderAry);
                    mHandler.post(mUpdateResults);
                } else {
                    Toast.makeText(QuickActivity.this, getString(R.string.msg_operation_not_allowed), Toast.LENGTH_LONG).show();
                }
            }
            // Handle Quantities
            if (type == 1) {
                int newqty = (Integer) jsonGetter2(ji, "qty");
                int put = (Integer) jsonGetter2(jd, "priceUnitTotal");
                jsonSetter(jd, "qty", newqty);
                jsonSetter(jd, "priceQtyTotal", put * newqty);
            }
            // handle the MODIFIERS for the Special Instructions area
            if (type == 2) {
                String spec = jsonGetter2(ji, "title").toString();
                if (specins.indexOf(spec) < 0) specins = specins + spec + " ";
                jsonSetter(jd, "specIns", specins);
            }
            dialog.dismiss();
            // update everything
            saveJsonTable(currentTicketID, JSONOrderAry);
            // and then update the total price of the order
            JSONObject ary = new JSONObject();
            ary.put("ordertotal", updateOrderTotalRMB(currentTicketID));
            JSONOrderAry.put(jsonGetter3(JSONOrderAry, "ordertotal"), ary);
            saveJsonTable(currentTicketID, JSONOrderAry);
        } catch (JSONException e) {
            log("JSON Add Modifier Exception=" + e);
        }
        mHandler.post(mUpdateResults);
    }

    private class GridAdapter extends ArrayAdapter {
        Context ctxt;
        private ArrayList<String> data;
        String[] menuItem;
        String[] categoryAll;
        int textSize;

        GridAdapter(Context ctxt, int resource, ArrayList<String> items) {
            super(ctxt, resource, items);
            this.ctxt = ctxt;
            data = items;
            menuItem = Global.MENUTXT.split("\\n");
            categoryAll = Global.CATEGORYTXT.split("\\n");
            textSize = (txtSize3);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            TextView label = (TextView) convertView;
            if (convertView == null) {
                convertView = new TextView(ctxt);
                label = (TextView) convertView;
            }
            String tempString = data.get(position);
            SpannableString spanString = new SpannableString(tempString);
            spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
            label.setText(spanString);

            String line = menuItem[position];
            String[] menuColumns = line.split("\\|");
            if (isChinese()) {
                String[] menuLang = menuColumns[2].split("\\\\");
                tempString = menuLang[1];
                spanString = new SpannableString(tempString);
                spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
                label.setText(spanString);
            }
            label.setTextSize(textSize);
            label.setHeight((int) (Utils.getWindowButtonHeight(QuickActivity.this)));

            String catColumns = menuColumns[1];
            // look up the category and set the language
            int qq = categoryGetIndex(catColumns);

            states = new StateListDrawable();
            states.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(Color.parseColor(textColors[2])));
            states.addState(new int[]{}, new ColorDrawable(Color.parseColor(colors[qq])));
            label.setBackgroundDrawable(states);
            //label.setTextColor(Color.parseColor(textColors[0]));		// just white text
            label.setTextColor(Color.parseColor(menubutcolors[qq]));    // black or white text
            if (menubutcolors[qq].equalsIgnoreCase("#ffffff")) {
                label.setShadowLayer((float) 0.01, 1, 2, Color.parseColor(textColors[7]));    // dark shadow
            }
            label.setPadding(4, 2, 4, 2);    // l,t,r,b
            label.setGravity(Gravity.CENTER);
            return (convertView);
        }
    }

    private class TicketAdapter extends ArrayAdapter {
        Context ctxt;
        private ArrayList<String> data;
        int textSize;
        String telnum;

        TicketAdapter(Context ctxt, int resource, ArrayList<String> items) {
            super(ctxt, resource, items);
            this.ctxt = ctxt;
            data = items;
        }

        public View getView(final int position, View convertView, ViewGroup parent) {

            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.ticket_item, null);
            }
            try {
                JSONArray o = new JSONArray(data.get(position));
                if (o != null) {
                    TextView t1 = (TextView) v.findViewById(R.id.ticket_item_orderid);
                    //String oid = jsonGetter2(o,"orderid").toString();
                    String tt = jsonGetter2(o, "tabletime").toString();
                    String tid = jsonGetter2(o, "tablename").toString();
                    t1.setText(tt + "-" + tid);
                    t1.setTextSize(txtSize4);

                    t1 = (TextView) v.findViewById(R.id.ticket_item_source);
                    int src = (Integer) jsonGetter2(o, "source");
                    t1.setTextSize(txtSize0);
                    t1.setText(orderSource[src]);
                    t1.setBackgroundColor(Color.parseColor(orderSourceColor[src]));
                    t1.setTextColor(Color.parseColor(textColors[0]));
                    t1.setTextSize(txtSize4);

                    t1 = (TextView) v.findViewById(R.id.ticket_item_phone);
                    telnum = jsonGetter2(o, "deliverynumber").toString();
                    t1.setText("Number: " + telnum);
                    t1.setTextSize(txtSize3);

                    t1 = (TextView) v.findViewById(R.id.ticket_item_name);
                    String cn = jsonGetter2(o, "customername").toString();
                    t1.setText("Name: " + cn);
                    t1.setTextSize(txtSize3);

                    t1 = (TextView) v.findViewById(R.id.ticket_item_addr1);
                    String da = jsonGetter2(o, "deliveryaddress").toString();
                    t1.setText("Addr1: " + da);
                    t1.setTextSize(txtSize3);

                    t1 = (TextView) v.findViewById(R.id.ticket_item_addr2);
                    String da2 = jsonGetter2(o, "deliveryaddress2").toString();
                    t1.setText("Addr2: " + da2);
                    t1.setTextSize(txtSize3);

                    t1 = (TextView) v.findViewById(R.id.ticket_item_time);
                    //String tt = jsonGetter2(o,"tabletime").toString();
                    t1.setText("Time: " + tt.substring(0, 2) + ":" + tt.substring(2, 4) + ":" + tt.substring(4, 6));
                    t1.setTextSize(txtSize3);

                    t1 = (TextView) v.findViewById(R.id.ticket_item_ticketid);
                    tid = jsonGetter2(o, "currenttableid").toString();
                    t1.setText("Ticket ID: " + tid);
                    t1.setTextSize(txtSize3);

                    t1 = (TextView) v.findViewById(R.id.ticket_item_deviceid);
                    final String did = jsonGetter2(o, "waiter").toString();
                    String s = "User ID:";
                    //if (src == 0) s = "Login ID:";
                    t1.setText(s + " " + did);
                    t1.setTextSize(txtSize3);

                    t1 = (TextView) v.findViewById(R.id.ticket_item_response);
                    final String resp = jsonGetter2(o, "response").toString();
                    t1.setText("Response: " + resp);
                    t1.setTextSize(txtSize3);
                    if (resp.equalsIgnoreCase("Accepted")) {
                        t1.setBackgroundColor(Color.parseColor(textColors[1]));
                        t1.setTextColor(Color.parseColor(textColors[0]));
                        t1.setVisibility(View.VISIBLE);
                    } else if (resp.equalsIgnoreCase("Reject")) {
                        t1.setBackgroundColor(Color.parseColor(textColors[10]));
                        t1.setTextColor(Color.parseColor(textColors[0]));
                        t1.setVisibility(View.VISIBLE);
                    } else if (resp.equalsIgnoreCase("Reject-Range")) {
                        t1.setBackgroundColor(Color.parseColor(textColors[10]));
                        t1.setTextColor(Color.parseColor(textColors[0]));
                        t1.setVisibility(View.VISIBLE);
                    } else if (resp.equalsIgnoreCase("Call")) {
                        t1.setBackgroundColor(Color.parseColor(textColors[4]));
                        t1.setTextColor(Color.parseColor(textColors[0]));
                        t1.setVisibility(View.VISIBLE);
                    } else t1.setVisibility(View.INVISIBLE);

                    t1 = (TextView) v.findViewById(R.id.ticket_item_dishes);
                    int dish = numberOfDishes(position);
                    t1.setText("Dishes: " + dish);
                    t1.setTextSize(txtSize4);

                    t1 = (TextView) v.findViewById(R.id.ticket_item_total);
                    String tot = jsonGetter2(o, "ordertotal").toString();
                    t1.setText("Order Total: " + tot);
                    t1.setTextSize(txtSize4);

                    Button bCall = (Button) v.findViewById(R.id.TicketCall);
                    bCall.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            boolean isTelephony = getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
                            if (isTelephony) {
                                // call the telephone number
                                if (telnum.length() > 0) {
                                    Intent makeCall = new Intent();
                                    makeCall.setAction(Intent.ACTION_DIAL);
                                    makeCall.setData(Uri.parse("tel:" + Uri.encode(telnum)));
                                    startActivity(makeCall);
                                } else {
                                    Toast.makeText(QuickActivity.this, "\nNo telephone number available.\n", Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Toast.makeText(QuickActivity.this, "\nTelephone calls not available on this device.\n", Toast.LENGTH_LONG).show();
                            }
                        }
                    });

                    // Response buttons are next, only show them for Mobile orders
                    LinearLayout llrb = (LinearLayout) v.findViewById(R.id.TicketResponseButtons);
                    if (src == 3) llrb.setVisibility(View.VISIBLE);    // Show for mobile orders
                    else llrb.setVisibility(View.INVISIBLE);

                    // When a mobile order ticket is displayed the following 3 buttons allow the user to control communication with the user
                    // through push messages. The code below is commented out and needs to be refactored for Firebase Cloud Messaging (FCM)
                    // The old method relied on MQTT which is no longer supported by SmartMenu.

                    Button bAccept = (Button) v.findViewById(R.id.TicketAccept);
                    bAccept.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            /*
                            String sendMsg = "Accepted";
                            SmartMenuService.actionPushItOut(getApplicationContext(), did, sendMsg);
                            // update the response
                            try {
                                JSONArray JSONtmp = new JSONArray(JSONOrderStr[currentTicketID]);
                                jsonSetter(JSONtmp, "response", sendMsg);
                                // re-save it
                                saveJsonTable(currentTicketID, JSONtmp);
                                mHandler.post(mUpdateResults);
                            } catch (JSONException e) {
                                log("JSONOtmp bAccept=" + e);
                            }
                            */
                        }
                    });

                    Button bCallUs = (Button) v.findViewById(R.id.TicketCallUs);
                    bCallUs.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            /*
                            String sendMsg = "Call";
                            SmartMenuService.actionPushItOut(getApplicationContext(), did, sendMsg);
                            // update the response
                            try {
                                JSONArray JSONtmp = new JSONArray(JSONOrderStr[currentTicketID]);
                                jsonSetter(JSONtmp, "response", sendMsg);
                                // re-save it
                                saveJsonTable(currentTicketID, JSONtmp);
                                mHandler.post(mUpdateResults);
                            } catch (JSONException e) {
                                log("JSONOtmp bAccept=" + e);
                            }
                            */
                        }
                    });

                    Button bReject = (Button) v.findViewById(R.id.TicketReject);
                    final CheckBox cboor = (CheckBox) v.findViewById(R.id.OutOfRange);
                    bReject.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            /*
                            String sendMsg = "Reject";
                            if (cboor.isChecked()) sendMsg = "Reject-Range";
                            SmartMenuService.actionPushItOut(getApplicationContext(), did, sendMsg);
                            // update the response
                            try {
                                JSONArray JSONtmp = new JSONArray(JSONOrderStr[currentTicketID]);
                                jsonSetter(JSONtmp, "response", sendMsg);
                                // re-save it
                                saveJsonTable(currentTicketID, JSONtmp);
                                mHandler.post(mUpdateResults);
                            } catch (JSONException e) {
                                log("JSONOtmp bAccept=" + e);
                            }
                            */
                        }
                    });

                    Button bEdit = (Button) v.findViewById(R.id.TicketEdit);
                    bEdit.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            // Handle the phone details update if they click
                            final CustomDialog customDialog = new CustomDialog(QuickActivity.this);
                            LayoutInflater factory = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                            final View textEntryView = factory.inflate(R.layout.caller_detail, null);

                            customDialog.setContentView(textEntryView);
                            customDialog.show();
                            customDialog.setCancelable(true);
                            customDialog.setCanceledOnTouchOutside(true);

                            EditText etNum = (EditText) customDialog.findViewById(R.id.etNumber);
                            etNum.setTextSize(txtSize1);

                            try {
                                JSONArray JSONtmp = new JSONArray(JSONOrderStr[currentTicketID]);
                                etNum.setText(jsonGetter2(JSONtmp, "deliverynumber").toString());
                                EditText etAddr = (EditText) customDialog.findViewById(R.id.etAddr);
                                etAddr.setTextSize(txtSize1);
                                etAddr.setText(jsonGetter2(JSONtmp, "deliveryaddress").toString());
                            } catch (JSONException e) {
                                log("JSONtmp Exception llcaller=" + e);
                            }

                            last10Calls.clear();

                            ContentResolver cr = getContentResolver();
                            Cursor callLogCursor = cr.query(CallLog.Calls.CONTENT_URI, /*uri*/
                                    null, /*projection*/
                                    null, /*selection*/
                                    null, /*selection arguments*/
                                    CallLog.Calls.DEFAULT_SORT_ORDER /*sort by*/);
                            if (callLogCursor != null) {
                                int cnt = 0;
                                while ((callLogCursor.moveToNext()) && (cnt < 100)) {
                                    String id = callLogCursor.getString(callLogCursor.getColumnIndex(CallLog.Calls._ID)); /*Get ID of call*/
                                    String addr = callLogCursor.getString(callLogCursor.getColumnIndex(CallLog.Calls.CACHED_NAME)); /*Get Contact Name*/
                                    String addr2 = "";

                                    String cacheNumber = callLogCursor.getString(callLogCursor.getColumnIndex(CallLog.Calls.CACHED_NUMBER_LABEL)); /*Get Contact Cache Number*/
                                    String number = callLogCursor.getString(callLogCursor.getColumnIndex(CallLog.Calls.NUMBER)); /*Get Contact Number*/

                                    long dateTimeMillis = callLogCursor.getLong(callLogCursor.getColumnIndex(CallLog.Calls.DATE)); /*Get Date and time information*/
                                    long durationMillis = callLogCursor.getLong(callLogCursor.getColumnIndex(CallLog.Calls.DURATION));
                                    int callType = callLogCursor.getInt(callLogCursor.getColumnIndex(CallLog.Calls.TYPE)); /*Get Call Type*/

                                    if (addr == null) addr = "";
                                    if (addr2 == null) addr2 = "";

                                    //SimpleDateFormat datePattern = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");
                                    SimpleDateFormat datePattern = new SimpleDateFormat("MM-dd HH:mm:ss");
                                    String date_str = datePattern.format(new Date(dateTimeMillis));

                                    if (callType == CallLog.Calls.OUTGOING_TYPE) {
                                        last10Calls.add(number + "," + addr + "," + addr2 + "," + date_str + ",Out");
                                    } else if (callType == CallLog.Calls.INCOMING_TYPE) {
                                        last10Calls.add(number + "," + addr + "," + addr2 + "," + date_str + ",In");
                                    } else if (callType == CallLog.Calls.MISSED_TYPE) {
                                        last10Calls.add(number + "," + addr + "," + addr2 + "," + date_str + ",Missed");
                                    }
                                    cnt = cnt + 1;
                                }
                                callLogCursor.close();
                            }

                            listCalls = (ListView) customDialog.findViewById(R.id.last10List);
                            last10Adapter = new ArrayAdapter<String>(QuickActivity.this, android.R.layout.simple_list_item_1, last10Calls);
                            listCalls.setAdapter(last10Adapter);
                            listCalls.setOnItemClickListener(new OnItemClickListener() {
                                public void onItemClick(AdapterView parent, View v, final int position, long id) {
                                    final int pos = position;
                                    String str[] = last10Calls.get(pos).split(",");

                                    String tmpDeliveryNumber = str[0];
                                    String tmpDeliveryAddress = str[1];
                                    String tmpDeliveryAddress2 = str[2];
                                    customDialog.dismiss();
                                    // update the json
                                    try {
                                        JSONArray JSONtmp = new JSONArray(JSONOrderStr[currentTicketID]);
                                        jsonSetter(JSONtmp, "deliverynumber", tmpDeliveryNumber);
                                        jsonSetter(JSONtmp, "deliveryaddress", tmpDeliveryAddress);
                                        jsonSetter(JSONtmp, "deliveryaddress2", tmpDeliveryAddress2);
                                        // re-save it
                                        saveJsonTable(currentTicketID, JSONtmp);
                                        //
                                        mHandler.post(mUpdateResults);
                                    } catch (JSONException e) {
                                        log("JSONOtmp Exception listcalls=" + e);
                                    }
                                }
                            });
                            listCalls.setOnItemLongClickListener(new OnItemLongClickListener() {
                                public boolean onItemLongClick(AdapterView parent, View v, final int position, long id) {
                                    final int pos = position;

                                    String str[] = last10Calls.get(pos).split(",");
                                    String tmpDeliveryNumber = str[0];
                                    String tmpDeliveryAddress = str[1];
                                    String tmpDeliveryAddress2 = str[2];

                                    // update the edit text boxes
                                    EditText etNum = (EditText) customDialog.findViewById(R.id.etNumber);
                                    etNum.setTextSize(txtSize1);
                                    etNum.setText(tmpDeliveryNumber);

                                    EditText etAddr = (EditText) customDialog.findViewById(R.id.etAddr);
                                    etAddr.setTextSize(txtSize1);
                                    etAddr.setText(tmpDeliveryAddress);

                                    EditText etAddr2 = (EditText) customDialog.findViewById(R.id.etAddr2);
                                    etAddr2.setTextSize(txtSize1);
                                    etAddr2.setText(tmpDeliveryAddress2);

                                    // update the json
                                    try {
                                        JSONArray JSONtmp = new JSONArray(JSONOrderStr[currentTicketID]);
                                        jsonSetter(JSONtmp, "deliverynumber", tmpDeliveryNumber);
                                        jsonSetter(JSONtmp, "deliveryaddress", tmpDeliveryAddress);
                                        jsonSetter(JSONtmp, "deliveryaddress2", tmpDeliveryAddress2);
                                        // re-save it
                                        saveJsonTable(currentTicketID, JSONtmp);
                                        // update ui
                                        mHandler.post(mUpdateResults);
                                    } catch (JSONException e) {
                                        log("JSONOtmp Exception listcalls2=" + e);
                                    }

                                    Button butsav = (Button) customDialog.findViewById(R.id.butSave);
                                    etAddr2 = (EditText) customDialog.findViewById(R.id.etAddr2);
                                    Cursor cursor = getContentResolver().query(
                                            Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
                                                    Uri.decode(tmpDeliveryNumber)),
                                            new String[]{PhoneLookup._ID}, null, null, null);
                                    if (cursor.moveToFirst()) {
                                        butsav.setEnabled(false);
                                        etAddr2.setVisibility(View.INVISIBLE);
                                    } else {
                                        butsav.setEnabled(true);
                                        butsav.setText("Save");
                                        etAddr2.setVisibility(View.VISIBLE);
                                        etAddr2.setTextSize(txtSize1);
                                    }
                                    return true;
                                }
                            });

                            Button butsav = (Button) customDialog.findViewById(R.id.butSave);
                            butsav.setEnabled(false);
                            EditText etAddr2 = (EditText) customDialog.findViewById(R.id.etAddr2);
                            etAddr2.setVisibility(View.INVISIBLE);
                            butsav.setOnClickListener(new OnClickListener() {
                                public void onClick(View v) {
                                    // update from the edit text boxes
                                    EditText etNum = (EditText) customDialog.findViewById(R.id.etNumber);
                                    String tmpDeliveryNumber = etNum.getText().toString();
                                    EditText etAddr = (EditText) customDialog.findViewById(R.id.etAddr);
                                    String tmpDeliveryAddress = etAddr.getText().toString();
                                    EditText etAddr2 = (EditText) customDialog.findViewById(R.id.etAddr2);
                                    String tmpDeliveryAddress2 = etAddr2.getText().toString();

                                    // update the json
                                    try {
                                        JSONArray JSONtmp = new JSONArray(JSONOrderStr[currentTicketID]);
                                        jsonSetter(JSONtmp, "deliverynumber", tmpDeliveryNumber);
                                        jsonSetter(JSONtmp, "deliveryaddress", tmpDeliveryAddress);
                                        jsonSetter(JSONtmp, "deliveryaddress2", tmpDeliveryAddress2);
                                        // re-save it
                                        saveJsonTable(currentTicketID, JSONtmp);
                                        // update ui
                                        mHandler.post(mUpdateResults);
                                    } catch (JSONException e) {
                                        log("JSONOtmp Exception listcalls3=" + e);
                                    }

                                    // insert the new contact
                                    ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
                                    int rawContactInsertIndex = ops.size();

                                    ops.add(ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                                            .withValue(RawContacts.ACCOUNT_TYPE, null)
                                            .withValue(RawContacts.ACCOUNT_NAME, null).build());
                                    ops.add(ContentProviderOperation
                                            .newInsert(Data.CONTENT_URI)
                                            .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
                                            .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                                            .withValue(StructuredName.DISPLAY_NAME, tmpDeliveryAddress).build());
                                    ops.add(ContentProviderOperation
                                            .newInsert(Data.CONTENT_URI)
                                            .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
                                            .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                                            .withValue(Phone.NUMBER, tmpDeliveryNumber)
                                            .withValue(Phone.TYPE, Phone.TYPE_MOBILE).build());

                                    // Insert Address2
                                    ops.add(ContentProviderOperation
                                            .newInsert(Data.CONTENT_URI)
                                            .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
                                            .withValue(Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                                            .withValue(ContactsContract.CommonDataKinds.StructuredPostal.STREET, tmpDeliveryAddress2).build());
                                    try {
                                        getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
                                    } catch (RemoteException e) {
                                        e.printStackTrace();
                                    } catch (OperationApplicationException e) {
                                        e.printStackTrace();
                                    }

                                    customDialog.dismiss();
                                }
                            });
                        }
                    });

                    // Just highlight the currently selected table with YELLOW border
                    LinearLayout ll1 = (LinearLayout) v.findViewById(R.id.ticket_item_ll);
                    if (currentTicketID == position) {
                        ll1.setBackgroundResource(R.drawable.border_yellow3);
                        bCall.setEnabled(true);
                        bEdit.setEnabled(true);
                        bAccept.setEnabled(true);
                        bCallUs.setEnabled(true);
                        bReject.setEnabled(true);
                    } else {
                        ll1.setBackgroundResource(R.drawable.border_gray3);
                        bCall.setEnabled(false);
                        bEdit.setEnabled(false);
                        bAccept.setEnabled(false);
                        bCallUs.setEnabled(false);
                        bReject.setEnabled(false);
                    }


                }
            } catch (JSONException e) {
                log("JSON TicketGetView Exception=" + e);
            }

            return (v);
        }
    }

    private class OrderAdapter extends ArrayAdapter<JSONArray> {

        private ArrayList<JSONArray> items;
        private int orderFontSize;
        private int orderFontSizeSmall;
        private SparseBooleanArray mSelectedItemsIds;

        public OrderAdapter(QuickActivity quickActivity, int textViewResourceId, ArrayList<JSONArray> items) {
            super(getBaseContext(), textViewResourceId, items);
            this.items = items;
            orderFontSize = (Utils.getFontSize(QuickActivity.this));
            orderFontSizeSmall = (int) (Utils.getFontSize(QuickActivity.this) / 1.2);
            mSelectedItemsIds = new SparseBooleanArray();
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.list_item, null);
            }

            JSONArray o = items.get(position);
            if (o != null) {
                TextView qt = (TextView) v.findViewById(R.id.list_item_qty);
                String quantity = jsonGetter2(o, "qty").toString();
                String discount = jsonGetter2(o, "priceDiscount").toString();

                String discountstr = "";
                if (discount.equalsIgnoreCase("100")) {
                    discountstr = "";
                } else if (discount.equalsIgnoreCase("0")) {
                    discountstr = "(free)";
                } else {
                    discountstr = "(" + discount + "%)";
                }
                qt.setText("x " + quantity);
                qt.setTextSize(orderFontSizeSmall);

                TextView up = (TextView) v.findViewById(R.id.list_item_unitprice);
                up.setText(jsonGetter2(o, "priceUnitTotal").toString() + discountstr);
                up.setTextSize(orderFontSizeSmall);

                TextView qp = (TextView) v.findViewById(R.id.list_item_qtyprice);
                qp.setText(jsonGetter2(o, "priceQtyTotal").toString() + ".00");
                qp.setTextSize(orderFontSizeSmall);

                // Set the main dish title with price option + options + extras + special instructions
                TextView tt = (TextView) v.findViewById(R.id.list_item_title);
                TextView st = (TextView) v.findViewById(R.id.list_item_subtitle);
                CheckBox cb = (CheckBox) v.findViewById(R.id.sentPrintersCB);

                try {
                    tt.setTextSize(orderFontSizeSmall);
                    st.setTextSize(orderFontSizeSmall);

                    // Start with dish name
                    String dishtext;
                    if (Global.EnglishLang) dishtext = jsonGetter2(o, "dishName").toString();
                    else dishtext = jsonGetter2(o, "dishNameAlt").toString();
                    tt.setText(dishtext.trim());

                    // Handle price option
                    String dishsubtext = "";
                    String priceopt;
                    if (Global.EnglishLang) priceopt = jsonGetter2(o, "priceOptionName").toString();
                    else priceopt = jsonGetter2(o, "priceOptionNameAlt").toString();
                    if (priceopt.length() > 0) {
                        dishsubtext = dishsubtext + priceopt;
                    }

                    // Add all the Option choices
                    JSONObject dishopt = new JSONObject();
                    dishopt = o.getJSONObject(jsonGetter3(o, "options"));
                    JSONArray dishoptAry = dishopt.getJSONArray("options");
                    //log("opt=" + dishoptAry.toString(1));
                    if (dishoptAry.length() > 0) {
                        dishsubtext = dishsubtext + "\n";
                        // Loop print
                        for (int i = 0; i < dishoptAry.length(); i++) {
                            //dishtext = dishtext + dishoptAry.getString(i);
                            // Grab just the optionName
                            if (Global.EnglishLang)
                                dishsubtext = dishsubtext + jsonGetter2(dishoptAry.getJSONArray(i), "optionName").toString();
                            else
                                dishsubtext = dishsubtext + jsonGetter2(dishoptAry.getJSONArray(i), "optionNameAlt").toString();
                            if (i != dishoptAry.length() - 1) dishsubtext = dishsubtext + ", ";
                        }
                    }
                    // Add selected Extra choices
                    JSONObject dishext = new JSONObject();
                    dishext = o.getJSONObject(jsonGetter3(o, "extras"));
                    JSONArray dishextAry = dishext.getJSONArray("extras");
                    if (dishextAry.length() > 0) {
                        dishsubtext = dishsubtext + "\n";
                        // Loop print
                        for (int i = 0; i < dishextAry.length(); i++) {
                            // Grab just the extraName
                            if (Global.EnglishLang)
                                dishsubtext = dishsubtext + jsonGetter2(dishextAry.getJSONArray(i), "extraName").toString();
                            else
                                dishsubtext = dishsubtext + jsonGetter2(dishextAry.getJSONArray(i), "extraNameAlt").toString();
                            if (i != dishextAry.length() - 1) dishsubtext = dishsubtext + ", ";
                        }
                    }

                    // Handle special Instructions
                    String specins = jsonGetter2(o, "specIns").toString();
                    if (specins.length() > 0) {
                        dishsubtext = dishsubtext + "\n";
                        dishsubtext = dishsubtext + getString(R.string.special_string) + specins;
                    }
                    if (dishsubtext.length() == 0) {
                        st.setVisibility(View.GONE);
                    } else {
                        st.setText(dishsubtext.trim());
                    }

                    // Set up the checkboxes for printed status
                    JSONArray JSONtmp = new JSONArray(JSONOrderStr[currentTicketID]);

                    int tabstate = (Integer) jsonGetter2(JSONtmp, "tabstate");
                    // always show the checkmarks for printing status...
                    //if (tabstate == 0) {
                    //	cb.setVisibility(View.GONE);
                    //} else {

                    // .. No Tabs in Quick app so make the checkmarks invisible
                    cb.setVisibility(View.GONE);
                    /*
                    	if (numberOfDishes(currentTicketID) > 0) {
                    		if (dishHasBeenPrinted(currentTicketID,position)) {
                    			cb.setVisibility(View.VISIBLE);
                    			cb.setChecked(true);
                    		} else {
                    			cb.setVisibility(View.VISIBLE);
                    			cb.setChecked(false);
                    		}
                    	}
                    */

                    //}
                    listOrder.setOnItemClickListener(new OnItemClickListener() {
                        public void onItemClick(AdapterView parent, View v, final int position, long id) {
                            showOrderPopup(position);
                            mHandler.post(mUpdateResults);
                        }
                    });
                    /*
                    // The following gets replaced by the Modal Action Bar Multi-Deleter
                    listOrder.setOnItemLongClickListener(new OnItemLongClickListener() {
                        public boolean onItemLongClick(AdapterView parent, View v, final int position, long id)
                        {
                        	// Only delete orders that have not been printed
                           	if (!dishHasBeenPrinted(currentTicketID,position) || Global.PrintedAllowDelete) {
                           	//if (true) {
                           		//log("Gonna try to delete dish=" + position + " on Table=" + currentTicketID);
                				// delete the item
                				try {
                					JSONArray JSONOrderAry = new JSONArray(JSONOrderStr[currentTicketID]);
                		    		JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry,"dishes"));
                		    		JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");
                		    		// Remove the selected item
                		    		// Check for SDK version to see if we can use the JSON function directly
                		            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                		            	JSONdishesAry.remove(position);
                		            } else {
                		            	// Do it the old-school way
                		            	JSONdishesAry = RemoveJSONArray(JSONdishesAry,position);
                		            }
                					// replace it
                					JSONObject ary=new JSONObject();	// new object to store the new dishes
                					ary.put("dishes",JSONdishesAry);
                		            JSONOrderAry.put(jsonGetter3(JSONOrderAry,"dishes"), ary);
                		            saveJsonTable(currentTicketID,JSONOrderAry);

                		      	  	// update the total price of the order
                		            ary=new JSONObject();
                		            ary.put("ordertotal",updateOrderTotalRMB(currentTicketID));
                		      	  	JSONOrderAry.put(jsonGetter3(JSONOrderAry,"ordertotal"), ary);
                		      	  	saveJsonTable(currentTicketID,JSONOrderAry);

                				} catch (JSONException e) {
                	  				log("JSON Delete Dish Exception=" + e);
                	            }
                           		mHandler.post(mUpdateResults);
                           	}
                            return true;
                        }
                    });
                    */
                } catch (JSONException e) {
                    log("JSON Opt+Ext Exception=" + e);
                }
            }
            return v;
        }

        public void removeSelection() {
            mSelectedItemsIds = new SparseBooleanArray();
            notifyDataSetChanged();
        }

        public void selectView(int position, boolean value) {
            if (value)
                mSelectedItemsIds.put(position, value);
            else
                mSelectedItemsIds.delete(position);
            notifyDataSetChanged();
        }

        public int getSelectedCount() {
            return mSelectedItemsIds.size();
        }

        public SparseBooleanArray getSelectedIds() {
            return mSelectedItemsIds;
        }

        public void toggleSelection(int position) {
            selectView(position, !mSelectedItemsIds.get(position));
        }
    }

    private class TicketGridAdapter extends GridAdapter {
        Context ctxt;
        private ArrayList<String> data;
        int textSize;
        String telnum;

        TicketGridAdapter(Context ctxt, int resource, ArrayList<String> items) {
            super(ctxt, resource, items);
            this.ctxt = ctxt;
            data = items;
        }

        public View getView(int position, View convertView, ViewGroup parent) {

            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.ticket_grid_item, null);
            }
            try {
                // read the file to get some key info, need to check in ORDERS and UNSENT
                String fname = data.get(position);
                File readFileO = new File(ordersDir, fname);
                File readFileR = new File(retryDir, fname);
                String order = Utils.ReadLocalFile(readFileO);
                if (order.length() == 0)
                    // Must be UNSENT order
                    order = Utils.ReadLocalFile(readFileR);
                JSONArray o = new JSONArray(order);
                //JSONArray o = new JSONArray(data.get(position));

                if (o != null) {
                    TextView t1 = (TextView) v.findViewById(R.id.ticket_item_orderid);

                    String tt = jsonGetter2(o, "tabletime").toString();
                    String tid = jsonGetter2(o, "tablename").toString();
                    String tphonenum = jsonGetter2(o, "deliverynumber").toString();
                    String taddr = jsonGetter2(o, "deliveryaddress").toString();
                    t1.setText(tt + "-" + tid);
                    t1.setTextSize(txtSize2);

                    t1 = (TextView) v.findViewById(R.id.ticket_item_time);
                    t1.setText("Time: " + tt.substring(0, 2) + ":" + tt.substring(2, 4) + ":" + tt.substring(4, 6));
                    t1.setTextSize(txtSize2);

                    t1 = (TextView) v.findViewById(R.id.ticket_item_ticketid);
                    t1.setText("Table: " + tid);
                    t1.setTextSize(txtSize2);

                    t1 = (TextView) v.findViewById(R.id.ticket_item_phonenum);
                    t1.setText("Number: " + tphonenum);
                    t1.setTextSize(txtSize2);

                    t1 = (TextView) v.findViewById(R.id.ticket_item_addr);
                    t1.setText("Addr1: " + taddr);
                    t1.setTextSize(txtSize2);

                    t1 = (TextView) v.findViewById(R.id.ticket_item_dishes);

                    JSONObject JSONdishObj = o.getJSONObject(jsonGetter3(o, "dishes"));
                    JSONArray jda = JSONdishObj.getJSONArray("dishes");
                    int dish = 0;
                    if (jda != null) dish = jda.length();
                    t1.setText("Dishes: " + dish);
                    t1.setTextSize(txtSize2);

                    t1 = (TextView) v.findViewById(R.id.ticket_item_total);
                    String tot = jsonGetter2(o, "ordertotal").toString();
                    t1.setText("Total: " + tot);
                    t1.setTextSize(txtSize2);
                }
            } catch (Exception e) {
                log("JSON TicketGridView Exception=" + e);
            }

            return (v);
        }
    }

    public boolean dishHasChoices(int itemPosition) {
        boolean choices1 = true;
        boolean choices2 = true;
        boolean choices3 = true;

        // see if the dish has empty popup, then we can save some clicks
        if (rmbItem.length == 1) choices1 = false;
        if (extrasItem[0].equalsIgnoreCase("none")) choices2 = false;
        if (optionsItem[0].equalsIgnoreCase("none")) choices3 = false;

        return choices1 || choices2 || choices3;
    }

    public boolean checkInternetConnection() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        // test for connection
        if (cm.getActiveNetworkInfo() != null
                && cm.getActiveNetworkInfo().isAvailable()
                && cm.getActiveNetworkInfo().isConnected()) {
            return true;
        } else {
            // Log.v("KIOSK", "Internet Connection Not Present");
            return false;
        }
    }

    public void lostConnection() {
        AlertDialog alertDialog = new AlertDialog.Builder(QuickActivity.this).create();
        alertDialog.setTitle("Connection");
        alertDialog.setIcon(android.R.drawable.stat_sys_warning);
        alertDialog.setMessage("Data connection not available. Please restart.");
        alertDialog.setCancelable(false);
        alertDialog.setButton("Exit", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        alertDialog.show();
    }

    private void addDividerLine(LinearLayout ll) {
        LinearLayout mll;
        mll = ll;
        // add divider line
        ImageView imageLine = new ImageView(QuickActivity.this);
        imageLine.setBackgroundResource(R.drawable.bar_white);
        mll.addView(imageLine);
    }

    private void addDividerGap(LinearLayout ll) {
        LinearLayout mll;
        mll = ll;
        // add divider line
        ImageView imageLine = new ImageView(QuickActivity.this);
        imageLine.setBackgroundResource(R.drawable.gap);
        mll.addView(imageLine);
    }

    // Scan though the optionsItem array, find the str, return the location index
    private int optionsGetIndex(String str) {
        int found = 0;
        for (int i = 0; i < optionsAll.length; i++) {
            if (str.equalsIgnoreCase(optionsAll[i].substring(0, str.length()))) {
                found = i;
                break;
            }
        }
        return found;
    }

    // Scan though the extrasItem array, find the str, return the location index
    private int extrasGetIndex(String str) {
        int found = 0;
        for (int i = 0; i < extrasAll.length; i++) {
            if (str.equalsIgnoreCase(extrasAll[i].substring(0, str.length()))) {
                found = i;
                break;
            }
        }
        return found;
    }

    // Scan though the Category array, find the str, return the location index
    private int categoryGetIndex(String str) {
        int found = 0;
        for (int i = 0; i < categoryAll.length; i++) {
            if (str.equalsIgnoreCase(categoryAll[i].substring(0, str.length()))) {
                found = i;
                break;
            }
        }
        return found;
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return (ni != null && ni.isAvailable() && ni.isConnected());
    }

    private boolean haveNetworkConnection() {
        boolean HaveConnectedWifi = false;
        boolean HaveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    HaveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    HaveConnectedMobile = true;
        }
        return HaveConnectedWifi || HaveConnectedMobile;
    }

    public void messageBox(final Context context, final String message, final String title) {
        this.runOnUiThread(
                new Runnable() {
                    public void run() {
                        final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
                        alertDialog.setTitle(title);
                        alertDialog.setIcon(android.R.drawable.stat_sys_warning);
                        alertDialog.setMessage(message);
                        alertDialog.setCancelable(false);
                        alertDialog.setButton("Back", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                alertDialog.cancel();
                                //finish();
                            }
                        });
                        alertDialog.show();
                    }
                }
        );
    }

    BroadcastReceiver wifiStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            infoWifi = "Checking";
            SupplicantState supState;
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            supState = wifiInfo.getSupplicantState();
            infoWifi = "" + supState;
            if (supState.equals(SupplicantState.COMPLETED)) {
                // wifi is up so set the title bar
                infoWifi = "OK";
            } else {
                // no wifi so give an update
                if (supState.equals(SupplicantState.SCANNING)) {
                    infoWifi = "Scanning";
                } else if (supState.equals(SupplicantState.DISCONNECTED)) {
                    infoWifi = "Not Available";
                } else {
                    infoWifi = "Connecting";
                }
            }
        }
    };

    BroadcastReceiver incomingCallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Bundle bundle = intent.getExtras();
            if (bundle == null) return;
            // Incoming call
            // Issue with 2 incoming tickets getting generated on single incoming call, so update the following line...
            // Annoying bug in Android 4.1.2
            // http://stackoverflow.com/questions/23425417/broadcast-receiver-calls-twice-extra-state-ringing-state-causing-invariant-data

            // Get the state
            String state = bundle.getString(TelephonyManager.EXTRA_STATE);

            // Clear the flag if they went idle
            if ((state != null) && (state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_IDLE))) {
                CallStateBusy = false;
            }

            // Process incoming call
            // Issue with 2 incoming tickets getting generated on single incoming call, so also update the following line...
            ///if ((state != null) &&  (state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_RINGING))) {
            if ((state != null) && (state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_RINGING)) && (!CallStateBusy)) {
                CallStateBusy = true;
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        try {
                            // bump to the next ticket
                            int numTickets = JSONTickets.size();
                            if (numTickets < MaxTickets) {
                                int newTicketID = numTickets;
                                clearPrinterStatus();
                                JSONArray JSONtmp = getInitialJSONOrder(newTicketID);

                                String tmpDeliveryNumber = bundle.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
                                String tmpDeliveryAddress = "";
                                String tmpDeliveryAddress2 = "";
                                Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(tmpDeliveryNumber));
                                Cursor cursor = getContentResolver().query(uri, new String[]{PhoneLookup.DISPLAY_NAME}, tmpDeliveryNumber, null, null);
                                if (cursor.moveToFirst()) {
                                    tmpDeliveryAddress = cursor.getString(cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME));
                                }
                                cursor.close();
                                tmpDeliveryAddress2 = "";

                                String tmpTableName = "Call";
                                String tmpTableTime = Utils.GetTime();
                                String tmpOrderId = Utils.GetDateTime() + "-" + tmpTableName;

                                // save the new data for the incoming call

                                jsonSetter(JSONtmp, "source", 1);    // type 1 is phone call
                                jsonSetter(JSONtmp, "tabstate", 2);
                                jsonSetter(JSONtmp, "orderid", tmpOrderId);
                                jsonSetter(JSONtmp, "currenttableid", newTicketID);
                                jsonSetter(JSONtmp, "tabletime", tmpTableTime);
                                jsonSetter(JSONtmp, "tablename", tmpTableName);
                                jsonSetter(JSONtmp, "deliverynumber", tmpDeliveryNumber);
                                jsonSetter(JSONtmp, "deliveryaddress", tmpDeliveryAddress);
                                jsonSetter(JSONtmp, "deliveryaddress2", tmpDeliveryAddress2);

                                JSONTickets.add(JSONtmp.toString());
                                JSONOrderStr[newTicketID] = JSONtmp.toString();
                                saveJsonTable(newTicketID, JSONtmp);
                            } else {
                                Toast.makeText(QuickActivity.this, "\n\n\nNo Tickets Available\n\n\n", Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            log("JSONOtmp incomingCallReceiver e=" + e);
                        }
                        // update UI
                        invalidateOptionsMenu();
                        mHandler.post(mUpdatePrinters);
                        mHandler.post(mUpdateResults);
                    }
                }, 250);
            }
        }
    };

    private void getPassword(String js, String expectedpw, int returnID, int dishPosition, Dialog dialogNames) {
        final Dialog dialogPW;
        dialogPW = new Dialog(QuickActivity.this);
        dialogPW.setContentView(R.layout.password);
        dialogPW.setCancelable(true);
        dialogPW.setCanceledOnTouchOutside(true);

        etPassword1 = (EditText) dialogPW.findViewById(R.id.etPassword1);
        etPassword1.setRawInputType(Configuration.KEYBOARD_12KEY);
        etPassword2 = (EditText) dialogPW.findViewById(R.id.etPassword2);
        etPassword2.setRawInputType(Configuration.KEYBOARD_12KEY);
        etPassword3 = (EditText) dialogPW.findViewById(R.id.etPassword3);
        etPassword3.setRawInputType(Configuration.KEYBOARD_12KEY);
        etPassword4 = (EditText) dialogPW.findViewById(R.id.etPassword4);
        etPassword4.setRawInputType(Configuration.KEYBOARD_12KEY);

        // set the starting selected to et1
        etPassword1.requestFocus();

        // turn on keyboard
        //getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        //InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        //if (imm != null) imm.toggleSoftInput(0, InputMethodManager.SHOW_IMPLICIT);

        // setup the text watchers
        watcher1 = new GenericTextWatcher(etPassword1, js, expectedpw, returnID, dialogPW, dialogNames, dishPosition);
        etPassword1.addTextChangedListener(watcher1);
        watcher2 = new GenericTextWatcher(etPassword2, js, expectedpw, returnID, dialogPW, dialogNames, dishPosition);
        etPassword2.addTextChangedListener(watcher2);
        watcher3 = new GenericTextWatcher(etPassword3, js, expectedpw, returnID, dialogPW, dialogNames, dishPosition);
        etPassword3.addTextChangedListener(watcher3);
        watcher4 = new GenericTextWatcher(etPassword4, js, expectedpw, returnID, dialogPW, dialogNames, dishPosition);
        etPassword4.addTextChangedListener(watcher4);

        // setup the title
        String tit = getString(R.string.msg_password);
        SpannableStringBuilder ssBuilser = new SpannableStringBuilder(tit);
        StyleSpan span = new StyleSpan(Typeface.NORMAL);
        ScaleXSpan span1 = new ScaleXSpan(2);
        ssBuilser.setSpan(span, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        ssBuilser.setSpan(span1, 0, tit.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        dialogPW.setTitle(ssBuilser);
        dialogPW.show();
    }

    private class GenericTextWatcher implements TextWatcher {
        private View view;
        private String js;
        private String expectedpw;
        private int returnID;
        private Dialog dialogPW;
        private Dialog dialogNames;
        private int dishPosition;

        private GenericTextWatcher(View view, String js, String expectedpw, int returnID, Dialog dialogPW, Dialog dialogNames, int position) {
            this.view = view;
            this.js = js;
            this.expectedpw = expectedpw;
            this.returnID = returnID;
            this.dialogPW = dialogPW;
            this.dialogNames = dialogNames;
            this.dishPosition = dishPosition;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void afterTextChanged(Editable editable) {
            switch (view.getId()) {
                case R.id.etPassword1:
                    etPassword2.requestFocus();
                    break;
                case R.id.etPassword2:
                    etPassword3.requestFocus();
                    break;
                case R.id.etPassword3:
                    etPassword4.requestFocus();
                    break;
                case R.id.etPassword4:
                    dialogPW.dismiss();
                    dialogNames.dismiss();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        // In the order app, only the the first line works. Here, both seem to work
                        // stackoverflow.com/questions/1109022/close-hide-the-android-soft-keyboard
                        imm.toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY);
                        imm.hideSoftInputFromWindow(etPassword4.getWindowToken(), 0);
                    }
                    if (pwMatch(expectedpw)) {
                        // turn off keyboard
                        //InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                        //if (imm != null) imm.toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY);
                        switch (returnID) {
                            case 2:
                                try {
                                    // Insert handler for password success
                                    // Case 2 for Change User
                                    JSONArray ja = new JSONArray(js);
                                    int level = (Integer) jsonGetter2(ja, "userlevel");
                                    String name = jsonGetter2(ja, "name").toString();

                                    Global.CheckedPicName = name;
                                    Global.LoginTime = Utils.GetDateTime();
                                    Global.ServerName = Global.CheckedPicName;
                                    Global.UserLevel = level;
                                    // save new user logged in
                                    String sendserver = "3," + Utils.GetDateTime() + "," + Global.ServerName;
                                    activityLogger(sendserver);
                                    mHandler.post(mUpdateResults);
                                    break;
                                } catch (Exception e) {
                                    log("Discount getPassword Exception e=" + e);
                                }
                        }
                    }
                    break;
            }
        }
    }

    private boolean pwMatch(String pw2) {
        Boolean result = false;
        String pw = etPassword1.getText().toString();
        pw = pw + etPassword2.getText().toString();
        pw = pw + etPassword3.getText().toString();
        pw = pw + etPassword4.getText().toString();
        if (pw.equals(pw2)) result = true;
        return (result);
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
                messageBox(QuickActivity.this,
                        "Sorry, Cash Drawer cannot be opened. " + errorString,
                        "Connection problem 1");
            }
        } else {
            String errorString = "Sorry, Cash Drawer cannot be opened. ";
            messageBox(QuickActivity.this, errorString, "Connection problem 1b");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        menu.add(0, 0, menu.NONE, "Store");
        MenuItem item0 = menu.getItem(0);
        item0.setIcon(null);
        item0.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        String state = "Closed";
        if (Global.QuickOpen) state = "Open";
        item0.setTitle(" STORE " + "\n" + " " + state + " ");

        menu.add(0, 1, menu.NONE, "Printers");
        MenuItem item1 = menu.getItem(1);
        item1.setIcon(null);
        item1.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        String char1 = "\u2460";
        String char2 = "\u2461";
        String char3 = "\u2462";
        if (Global.POS1Enable) char1 = "\u2776";
        if (Global.POS2Enable) char2 = "\u2777";
        if (Global.POS3Enable) char3 = "\u2778";

        item1.setTitle(" PRINT " + "\n" + " " + char1 + char2 + char3 + " ");

        menu.add(0, 2, menu.NONE, "Server");
        MenuItem item2 = menu.getItem(2);
        item2.setIcon(null);
        item2.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        item2.setTitle(" SERVER " + "\n" + " " + Global.ServerName + " ");

        SubMenu subMenu3 = menu.addSubMenu(0, 3, menu.NONE, "LANG");
        subMenu3.add(0, 10, menu.NONE, "English");
        subMenu3.add(0, 11, menu.NONE, "Chinese");
        MenuItem subMenu3Item = subMenu3.getItem();
        subMenu3Item.setIcon(null);
        subMenu3Item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        if (Global.EnglishLang) {
            subMenu3Item.setTitle("LANG\n" +
                    " ENG ");
        } else {
            subMenu3Item.setTitle("LANG\n" +
                    "  CH  ");
        }

        menu.add(0, 4, menu.NONE, "Menu");
        MenuItem item4 = menu.getItem(4);
        item4.setIcon(null);
        item4.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        if (vfMenuTable.getDisplayedChild() == 1) {
            item4.setTitle("   MENU" + "     \n" +
                    "TICKETS" + " \u25FC");
        } else {
            item4.setTitle("   MENU" + " \u25FC\n" +
                    "TICKETS    ");
        }

        SubMenu subMenu8 = menu.addSubMenu(0, 8, menu.NONE, "Tools");
        subMenu8.setIcon(android.R.drawable.ic_menu_preferences);
        subMenu8.add(0, 12, menu.NONE, "Status");
        subMenu8.add(0, 13, menu.NONE, "Open Cash Drawer");
        subMenu8.add(0, 15, menu.NONE, "Unsent Orders");
        subMenu8.add(0, 19, menu.NONE, "Reload Order");
        subMenu8.add(0, 20, menu.NONE, "Register Logout");
        MenuItem subMenu8Item = subMenu8.getItem();
        subMenu8Item.setIcon(android.R.drawable.ic_menu_preferences);
        subMenu8Item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return (super.onCreateOptionsMenu(menu));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 12) {
            LayoutInflater factory = LayoutInflater.from(this);
            final View textEntryView = factory.inflate(R.layout.info_dialog, null);

            final CustomDialog customDialog = new CustomDialog(this);
            customDialog.setContentView(textEntryView);
            customDialog.show();
            customDialog.setCancelable(true);
            customDialog.setCanceledOnTouchOutside(true);

            TextView tv0 = (TextView) customDialog.findViewById(R.id.AboutAppName);
            Map<String, String> map0 = new LinkedHashMap<String, String>();
            map0.put(getString(R.string.msg_about_app_name), Global.AppName);
            populateField(map0, tv0);

            TextView tv1 = (TextView) customDialog.findViewById(R.id.AboutVersion);
            Map<String, String> map1 = new LinkedHashMap<String, String>();
            map1.put(getString(R.string.msg_about_version_name), getVersionName());
            populateField(map1, tv1);

            TextView tv2 = (TextView) customDialog.findViewById(R.id.AboutFileSource);
            Map<String, String> map2 = new LinkedHashMap<String, String>();
            map2.put(getString(R.string.msg_about_filesource), Global.FileSource);
            populateField(map2, tv2);

            TextView tv5 = (TextView) customDialog.findViewById(R.id.AboutSmartMenuID);
            Map<String, String> map5 = new LinkedHashMap<String, String>();
            map5.put(getString(R.string.msg_about_smartmenuid), Global.SMID);
            populateField(map5, tv5);

            TextView tv6 = (TextView) customDialog.findViewById(R.id.AboutMenuVersion);
            Map<String, String> map6 = new LinkedHashMap<String, String>();
            map6.put(getString(R.string.msg_about_menuver), Global.MenuVersion);
            populateField(map6, tv6);

            TextView tv7 = (TextView) customDialog.findViewById(R.id.AboutDeviceId);
            Map<String, String> map7 = new LinkedHashMap<String, String>();
            map7.put(getString(R.string.msg_about_deviceid), Global.MasterDeviceId);
            populateField(map7, tv7);

            TextView tv7a = (TextView) customDialog.findViewById(R.id.AboutDeviceIP);
            Map<String, String> map7a = new LinkedHashMap<String, String>();
            map7a.put(getString(R.string.msg_about_deviceip), Utils.getIpAddress(true));
            populateField(map7a, tv7a);

            TextView tv8 = (TextView) customDialog.findViewById(R.id.AboutTicketnum);
            Map<String, String> map8 = new LinkedHashMap<String, String>();
            map8.put(getString(R.string.msg_about_ticketnum), "N/A");
            populateField(map8, tv8);

            TextView tv9 = (TextView) customDialog.findViewById(R.id.AboutWifi);
            Map<String, String> map9 = new LinkedHashMap<String, String>();
            map9.put(getString(R.string.msg_about_wifistatus), infoWifi);
            populateField(map9, tv9);

            // Include the actual IP addresses do they can see them with the status indicators
            TextView tvServerIP = (TextView) customDialog.findViewById(R.id.label1a2);
            tvServerIP.setText(Global.ServerIP);
            TextView tvPOS1IP = (TextView) customDialog.findViewById(R.id.label2a);
            tvPOS1IP.setText(Global.POS1Ip);
            TextView tvPOS2IP = (TextView) customDialog.findViewById(R.id.label13a);
            tvPOS2IP.setText(Global.POS2Ip);
            TextView tvPOS3IP = (TextView) customDialog.findViewById(R.id.label14a);
            tvPOS3IP.setText(Global.POS3Ip);

            //Create and run status thread for continuous updates
            //createAndRunStatusThread(this,customDialog);
            // Just update once, no need to keep refreshing it.
            updateConnectionStatus(customDialog);
            return (true);
        }
        if (item.getItemId() == 13) {
            // save record on server
            String sendserver = "0," + Utils.GetDateTime() + "," + Global.ServerName;
            activityLogger(sendserver);
            openDrawer();
            return (true);
        }
        if (item.getItemId() == 15) {
            LayoutInflater factory = LayoutInflater.from(this);
            final View textEntryView = factory.inflate(R.layout.queue_dialog, null);

            final CustomDialog customDialog = new CustomDialog(this);
            customDialog.setContentView(textEntryView);
            customDialog.show();
            customDialog.setCancelable(true);
            customDialog.setCanceledOnTouchOutside(true);

            File[] files = retryDir.listFiles();
            unsentItemList.clear();

            for (File f : files)
                unsentItemList.add(f.getName());

            listUnsent = (ListView) customDialog.findViewById(R.id.unsentItemList);
            unsentAdapter = new ArrayAdapter<String>(QuickActivity.this, android.R.layout.simple_list_item_1, unsentItemList);
            listUnsent.setAdapter(unsentAdapter);

            // set up a button, when they click, resend all the items
            Button butSnd = (Button) customDialog.findViewById(R.id.butSndAll);
            butSnd.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    final ProgressDialog pd = ProgressDialog.show(QuickActivity.this, "Sending", "Sending order(s) to the server...", true, false);
                    new Thread(new Runnable() {
                        public void run() {
                            if ((pingIP()) & (Global.TicketToCloud)) {
                                for (String fname : unsentItemList) {

                                    String postURL = "https://" + Global.ServerIP + Global.PosSaveOrderJsonURL;
                                    try {
                                        File readFile = new File(retryDir, fname);
                                        JSONArray JSONOrder = new JSONArray(Utils.ReadLocalFile(readFile));
                                        String orderid = jsonGetter2(JSONOrder, "orderid").toString();

                                        // update the sendtype so resend=2
                                        JSONObject obj = new JSONObject();
                                        obj.put("sendtpye", "2");
                                        JSONOrder.put(jsonGetter3(JSONOrder, "sendtype"), obj);

                                        int sc = Utils.SendMultipartJsonOrder(postURL, JSONOrder.toString(1), Global.SMID);
                                        log("Resent=" + orderid + " status code=" + sc);
                                        if (sc == 200) {
                                            if (readFile.delete()) {
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
                            pd.dismiss();
                            customDialog.dismiss();
                        }
                    }).start();
                }
            });
            // set up a button, when they click, send all the items
            Button butDel = (Button) customDialog.findViewById(R.id.butDelAll);
            butDel.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    File[] files = retryDir.listFiles();
                    if (files != null) {
                        for (int i = 0; i < files.length; i++) {
                            files[i].delete();
                        }
                    }
                    customDialog.dismiss();
                }
            });
            return (true);
        }
        if (item.getItemId() == 0) {
            if (Global.QuickOpen) {
                Global.QuickOpen = false;
                //IntentFilter filter2 = new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
                //filter2.setPriority(99999);
                //this.registerReceiver(incomingCallReceiver, filter2);
            } else {
                Global.QuickOpen = true;
                //this.unregisterReceiver(incomingCallReceiver);
            }
            invalidateOptionsMenu();
        }
        if (item.getItemId() == 20) {
            if (JSONTickets.size() > 0) {
                AlertDialog alertDialog = new AlertDialog.Builder(QuickActivity.this).create();
                alertDialog.setTitle(getString(R.string.tab3_opentabs_title));
                alertDialog.setMessage(getString(R.string.tab3_opentabs_text));
                alertDialog.setButton(getString(R.string.tab3_continue), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        registerLogout();
                    }
                });
                alertDialog.setButton2(getString(R.string.tab3_back), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                alertDialog.show();
            } else {
                registerLogout();
            }
            return (true);
        }
        if (item.getItemId() == 1) {
            LayoutInflater factory = LayoutInflater.from(this);
            final View textEntryView = factory.inflate(R.layout.printers_dialog, null);

            final CustomDialog customDialog = new CustomDialog(this);
            customDialog.setContentView(textEntryView);
            customDialog.show();
            customDialog.setCancelable(true);
            customDialog.setCanceledOnTouchOutside(true);

            final CheckBox cb1 = (CheckBox) customDialog.findViewById(R.id.cb1);
            final CheckBox cb2 = (CheckBox) customDialog.findViewById(R.id.cb2);
            final CheckBox cb3 = (CheckBox) customDialog.findViewById(R.id.cb3);
            if (Global.POS1Enable) cb1.setChecked(true);
            else cb1.setChecked(false);
            if (Global.POS2Enable) cb2.setChecked(true);
            else cb2.setChecked(false);
            if (Global.POS3Enable) cb3.setChecked(true);
            else cb3.setChecked(false);

            cb1.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    if (cb1.isChecked()) {
                        Global.POS1Enable = true;
                        prefEdit.putBoolean("pos1enable", true);
                        prefEdit.commit();
                        String sendserver = "6," + Utils.GetDateTime() + "," + Global.ServerName + " Enable 1";
                        activityLogger(sendserver);
                    } else {
                        Global.POS1Enable = false;
                        prefEdit.putBoolean("pos1enable", false);
                        prefEdit.commit();
                        String sendserver = "6," + Utils.GetDateTime() + "," + Global.ServerName + " Disable 1";
                        activityLogger(sendserver);
                    }
                    printStatus[0] = 0;
                    mHandler.post(mUpdatePrinters);
                    invalidateOptionsMenu();
                }
            });
            cb2.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    if (cb2.isChecked()) {
                        Global.POS2Enable = true;
                        prefEdit.putBoolean("pos2enable", true);
                        prefEdit.commit();
                        String sendserver = "6," + Utils.GetDateTime() + "," + Global.ServerName + " Enable 2";
                        activityLogger(sendserver);
                    } else {
                        Global.POS2Enable = false;
                        prefEdit.putBoolean("pos2enable", false);
                        prefEdit.commit();
                        String sendserver = "6," + Utils.GetDateTime() + "," + Global.ServerName + " Disable 2";
                        activityLogger(sendserver);
                    }
                    printStatus[1] = 0;
                    mHandler.post(mUpdatePrinters);
                    invalidateOptionsMenu();
                }
            });
            cb3.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    if (cb3.isChecked()) {
                        Global.POS3Enable = true;
                        prefEdit.putBoolean("pos3enable", true);
                        prefEdit.commit();
                        String sendserver = "6," + Utils.GetDateTime() + "," + Global.ServerName + " Enable 3";
                        activityLogger(sendserver);
                    } else {
                        Global.POS3Enable = false;
                        prefEdit.putBoolean("pos3enable", false);
                        prefEdit.commit();
                        String sendserver = "6," + Utils.GetDateTime() + "," + Global.ServerName + " Disable 3";
                        activityLogger(sendserver);
                    }
                    printStatus[2] = 0;
                    mHandler.post(mUpdatePrinters);
                    invalidateOptionsMenu();
                }
            });
            return (true);
        }
        if (item.getItemId() == 2) {
            // Handle a click on the Server, allow change of User in case a manager or admin password is required for an operation
            AlertDialog.Builder builder = new AlertDialog.Builder(QuickActivity.this);
            builder.setTitle(getString(R.string.login_person_name));
            builder.setCancelable(true);
            String[] tmpArr = new String[Global.userList.size()];
            try {
                for (int i = 0; i < Global.userList.size(); i++) {
                    JSONArray tmp = new JSONArray(Global.userList.get(i));
                    tmpArr[i] = jsonGetter2(tmp, "name").toString();
                }
                DialogInterface.OnClickListener picDialogListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogint, int which) {
                        try {
                            // turn off keyboard
                            //InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                            //if (imm != null) imm.toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY);

                            Global.CheckedPicID = which;
                            JSONArray tmp = new JSONArray(Global.userList.get(which));
                            String nam = jsonGetter2(tmp, "name").toString();
                            String pin = jsonGetter2(tmp, "pin").toString();
                            //int level = (Integer) jsonGetter2(tmp,"userlevel");
                            // Ask for password
                            getPassword(tmp.toString(), pin, 2, 0, dialog);
                        } catch (JSONException e) {
                            log("Change User Json1 e=" + e);
                        }
                    }
                };
                builder.setSingleChoiceItems(tmpArr, Global.CheckedPicID, picDialogListener);
                dialog = builder.create();
                dialog.show();
            } catch (JSONException e) {
                log("Change User Json2 e=" + e);
            }
            return (true);
        }
        if (item.getItemId() == 10) {
            // Switch lang to English
            Configuration config = getBaseContext().getResources().getConfiguration();
            locale = new Locale("en");
            Locale.setDefault(locale);
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
            Global.EnglishLang = true;
            GridView gridview = (GridView) findViewById(R.id.gridView1);
            gridview.setAdapter(new GridAdapter(this, R.layout.array_list_item, dishArrayList));
            listOrder = (ListView) findViewById(R.id.listOrder);
            listOrder.setAdapter(new OrderAdapter(QuickActivity.this, R.layout.list_item, JSONOrderList));
            invalidateOptionsMenu();
            setupButtons();
            return (true);
        }
        if (item.getItemId() == 11) {
            Configuration config = getBaseContext().getResources().getConfiguration();
            locale = new Locale("zh");
            Locale.setDefault(locale);
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
            Global.EnglishLang = false;
            GridView gridview = (GridView) findViewById(R.id.gridView1);
            gridview.setAdapter(new GridAdapter(this, R.layout.array_list_item, dishArrayList));
            listOrder = (ListView) findViewById(R.id.listOrder);
            listOrder.setAdapter(new OrderAdapter(QuickActivity.this, R.layout.list_item, JSONOrderList));
            invalidateOptionsMenu();
            setupButtons();
            return (true);
        }
        if (item.getItemId() == 19) {
            // Reload a table from Local Storage

            // bump to the next ticket
            int numTickets = JSONTickets.size();
            if (numTickets < MaxTickets) {

                final int newTicketID = numTickets;
                clearPrinterStatus();

                // Give them a layout so they can choose the file to reload
                LayoutInflater factory = LayoutInflater.from(this);
                final View textEntryView = factory.inflate(R.layout.reload_dialog, null);

                final CustomDialog customDialog = new CustomDialog(this);
                customDialog.setContentView(textEntryView);
                customDialog.show();
                customDialog.setCancelable(true);
                customDialog.setCanceledOnTouchOutside(true);

                final Button reloadDate = (Button) customDialog.findViewById(R.id.reloadDate);
                reloadDate.setText(getString(R.string.reload_date));

                // Let them choose a date
                final Calendar myCalendar = Calendar.getInstance();
                final DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear,
                                          int dayOfMonth) {
                        // TODO Auto-generated method stub
                        myCalendar.set(Calendar.YEAR, year);
                        myCalendar.set(Calendar.MONTH, monthOfYear);
                        myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        String myFormat = "yyMMdd"; //In which you need put here
                        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
                        reloadDate.setText(sdf.format(myCalendar.getTime()));

                        // update the tickets shown
                        FileFilter filter = new FileFilter() {
                            @Override
                            public boolean accept(File arg0) {
        						/* get current date
        						Date dt = new Date();
        						Integer month = dt.getMonth() + 1;
        						String formatmon = String.format("%02d", month);
        						Integer day = dt.getDate();
        						String formatdy = String.format("%02d", day);
        						Integer yr = dt.getYear() - 100;
        						String formatyr = String.format("%02d", yr);
        						*/
                                final Button reloadDate = (Button) customDialog.findViewById(R.id.reloadDate);
                                String dat = reloadDate.getText().toString();
                                //return arg0.getName().startsWith(formatyr+formatmon+formatdy);
                                return arg0.getName().startsWith(dat);
                                //return arg0.getName().endsWith(".jpg") || arg0.getName().endsWith(".bmp")|| arg0.getName().endsWith(".png") || arg0.isDirectory();
                            }
                        };
                        // Allow them to reload either Saved or Unsent orders for Today
                        File[] filesO = ordersDir.listFiles(filter);
                        File[] filesR = retryDir.listFiles(filter);
                        reloadItemList.clear();
                        for (File f : filesO) reloadItemList.add(f.getName());
                        for (File f : filesR) reloadItemList.add(f.getName());

                        gridReload = (GridView) customDialog.findViewById(R.id.reloadItemGrid);
                        reloadAdapter = new TicketGridAdapter(QuickActivity.this, R.layout.ticket_grid_item, reloadItemList);
                        gridReload.setAdapter(reloadAdapter);

                        gridReload.setOnItemClickListener(new OnItemClickListener() {
                            public void onItemClick(AdapterView parent, View v, final int position, long id) {
                                String fname = reloadItemList.get(position);
                                try {
                                    // Find out if this is a Saved or Unsent order
                                    File readFileO = new File(ordersDir, fname);
                                    File readFileR = new File(retryDir, fname);

                                    String order = Utils.ReadLocalFile(readFileO);
                                    if (order.length() == 0)
                                        // Must be UNSENT
                                        order = Utils.ReadLocalFile(readFileR);
                                    JSONArray JSONtmp = new JSONArray(order);

                                    // couple of updates
                                    jsonSetter(JSONtmp, "orderid", Utils.GetDateTime() + "-" + jsonGetter2(JSONtmp, "tablename").toString());
                                    jsonSetter(JSONtmp, "tabletime", Utils.GetTime());
                                    jsonSetter(JSONtmp, "tabstate", 2);
                                    jsonSetter(JSONtmp, "currenttableid", newTicketID);

                                    // Reloaded order so update the sendtype so resend=2
                                    jsonSetter(JSONtmp, "sendtype", "2");

                                    JSONTickets.add(JSONtmp.toString());
                                    JSONOrderStr[newTicketID] = JSONtmp.toString();

                                    // re-save it
                                    currentTicketID = newTicketID;
                                    saveJsonTable(currentTicketID, JSONtmp);
                                    // update UI
                                    invalidateOptionsMenu();
                                    mHandler.post(mUpdatePrinters);
                                    mHandler.post(mUpdateResults);

                                    customDialog.dismiss();
                                } catch (Exception e) {
                                    log("JSONOtmp Reloading Ticket=" + newTicketID + " e=" + e);
                                }
                            }
                        });
                    }
                };
                reloadDate.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // TODO Auto-generated method stub
                        new DatePickerDialog(QuickActivity.this, date, myCalendar
                                .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                                myCalendar.get(Calendar.DAY_OF_MONTH)).show();
                    }
                });
            } else {
                Toast.makeText(QuickActivity.this, "\n\n\nNo Tickets Available\n\n\n", Toast.LENGTH_LONG).show();
            }
            return (true);
        }
        if (item.getItemId() == 4) {
            // start with the TABLES View (menu/sent/tables/specials)
            vfMenuTable = (ViewFlipper) findViewById(R.id.vfMenuTable);
            if (vfMenuTable.getDisplayedChild() == 0) {
                vfMenuTable.setDisplayedChild(1);
            } else {
                if (currentTicketID != -1) {
                    vfMenuTable.setDisplayedChild(0);
                } else {
                    Toast.makeText(QuickActivity.this, "\n\n\nPlease Select a Ticket\n\n\n", Toast.LENGTH_LONG).show();
                }
            }
            invalidateOptionsMenu();
            updateTableButtons();
            return (true);
        }
        // end of all the menu items

        return (super.onOptionsItemSelected(item));
    }

    private void registerLogout() {
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.logout, null);
        final CustomDialog customDialog = new CustomDialog(this);
        customDialog.setContentView(textEntryView);
        customDialog.setCancelable(true);
        customDialog.setCanceledOnTouchOutside(true);
        customDialog.show();
        // check for confirm
        Button butLogout = (Button) customDialog.findViewById(R.id.butLogout);
        butLogout.setText(getString(R.string.register_logout));
        butLogout.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Global.LogoutTime = Utils.GetDateTime();
                String logOut = "Login:" + Global.LoginTime + " " +
                        "Logout: " + Global.LogoutTime + " " +
                        "Server: " + Global.ServerName + " ";
                String sendserver = "2," + Utils.GetDateTime() + "," + logOut;
                activityLogger(sendserver);
                // Stop the service
                log("Register Logging Out...");
                SmartMenuService.actionStop(getApplicationContext());
                finish();
                Global.LoggedIn = false;
                Intent kintent = new Intent(getApplicationContext(), LoginActivity.class);
                kintent.setFlags((Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                startActivity(kintent);
            }
        });
    }

    private void activityLogger(final String sendServer) {
        // Save to a file
        String fname = "activity.txt";
        File writeFile = new File(logsDir, fname);
        FileWriter writer;

        try {
            writer = new FileWriter(writeFile, true);
            writer.write(sendServer + "\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
        }

        // send to server
        new Thread(new Runnable() {
            public void run() {
                if ((pingIP()) & (Global.TicketToCloud)) {
                    String postURL = "https://" + Global.ServerIP + Global.PosSaveActivityURL;
                    Utils.SendMultipartAdhoc(postURL,
                            sendServer,
                            Global.SMID);
                } else {
                    log("PlaceOrder: activityLogger failed");
                }
            }
        }).start();
    }

    private String getVersionName() {
        String version = "";
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            version = "Package name not found";
        }
        return version;
    }

    private int getVersionCode() {
        int version = -1;
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pi.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
        }
        return version;
    }

    // below is the over ride that will disable the back button
    public void onBackPressed() {
    }

    private int getOrderTotalRMB(int table) {
        // Grab the ordertotal from the JSON
        int TOTALRMB = 0;
        try {
            JSONArray JSONOrderAry = new JSONArray(JSONOrderStr[table]);
            if (JSONOrderAry != null) {
                String ordertotal = "0";
                ordertotal = jsonGetter2(JSONOrderAry, "ordertotal").toString();
                TOTALRMB = Integer.parseInt(ordertotal);
            }
        } catch (JSONException e) {
            log("JSON getOrderTotalRMB Exception=" + e);
        }
        return TOTALRMB;
    }

    private int updateOrderTotalRMB(int table) {
        int TOTALRMB = 0;
        try {
            JSONArray JSONOrderAry = new JSONArray(JSONOrderStr[table]);
            if (JSONOrderAry != null) {
                JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
                JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");

                // Loop through each dish and add the Qty Total for the dish to the Order Total
                for (int i = 0; i < JSONdishesAry.length(); i++) {
                    JSONArray jd = JSONdishesAry.getJSONArray(i);
                    // Grab the PriceQty from the dish
                    int priceqty = Integer.parseInt(jsonGetter2(jd, "priceQtyTotal").toString());
                    // Running total ...
                    TOTALRMB = TOTALRMB + priceqty;
                }
                //log("dish cnt=" + JSONdishesAry.length());
                //log("new dish price=" + TOTALRMB);

                // update total price
                JSONObject ary = new JSONObject();
                ary.put("ordertotal", TOTALRMB);
                JSONOrderAry.put(jsonGetter3(JSONOrderAry, "ordertotal"), ary);

                // replace it
                ary = new JSONObject();    // new object to store the new dishes
                ary.put("dishes", JSONdishesAry);
                // Replace the JSON dishes Object in the JSON order
                JSONOrderAry.put(jsonGetter3(JSONOrderAry, "dishes"), ary);
            }
        } catch (JSONException e) {
            log("JSON updateOrderTotalRMB Exception=" + e);
        }
        return TOTALRMB;
    }

    // Until we have JSON menu, we need to extract the price from the dish name
    private static int getRMBnumber(String in) {
        int newRMB = 0;
        String foundRMB = "none";
        Pattern p = Pattern.compile("(RMB )(\\d*)");
        Matcher m = p.matcher(in);
        while (m.find()) {
            foundRMB = m.group(2).trim();    // load up the money
            newRMB = newRMB + Integer.valueOf(foundRMB);
        }
        return newRMB;
    }

    // Until we have JSON menu, we need to remove the price from the dish name
    private static String removeRMBnumber(String in) {
        String newName = in;
        Pattern p = Pattern.compile("(.*)(RMB )(\\d*)");
        Matcher m = p.matcher(in);
        while (m.find()) {
            newName = m.group(1).trim();    // load up the dish name before the RMB 99
        }
        return newName;
    }

    // Some string padding functions for the printers
    private String addPad(String str) {
        int addPad = Global.TicketCharWidth - str.length() + 1;
        for (int k = 1; k < addPad; k++) {
            str = str + " ";
        }
        //str = str + "\\r\\n";
        return str;
    }

    private String addPadKitc(String str) {
        int addPad = Global.KitcTicketCharWidth - str.length() + 1;
        //for(int k=1; k<addPad; k++)
        //{
        //	str = str + " ";
        //}
        str = str + "\\r\\n";
        return str;
    }

    private String addDots(String str) {
        int addPad = Global.TicketCharWidth - str.length() - 1;
        str = str + " ";
        for (int k = 1; k < addPad; k++) {
            str = str + ".";
        }
        str = str + " ";
        return str;
    }

    private String addDashLine(String str) {
        String strDash = "";
        for (int k = 1; k <= Global.TicketCharWidth; k++) {
            strDash = strDash + "-";
        }
        str = str + strDash;
        return str;
    }

    private String addBlankLineB4(String str) {
        String strBlanks = "";
        for (int k = 1; k <= Global.TicketCharWidth; k++) {
            strBlanks = strBlanks + " ";
        }
        str = strBlanks + str;
        return str;
    }

    private String addBlankLineAfter(String str) {
        String strBlanks = "";
        for (int k = 1; k <= Global.TicketCharWidth; k++) {
            strBlanks = strBlanks + " ";
        }
        str = str + strBlanks;
        return str;
    }

    private String addBlankLineAfterKitc(String str) {
        //String strBlanks = "";
        //for(int k=1; k<=Global.KitcTicketCharWidth; k++)
        //{
        //	strBlanks = strBlanks + " ";
        //}
        //str = str + strBlanks;
        str = str + "\\r\\n";
        return str;
    }

	/*
	// Not needed since we are using Broadcast receiver????
	private class PhoneCallListener extends PhoneStateListener {
	    private boolean isPhoneCalling = false;
	    @Override
	    public void onCallStateChanged(int state, String incomingNumber) {
	        if (TelephonyManager.CALL_STATE_RINGING == state) {
	            // phone ringing
	        }
	        if (TelephonyManager.CALL_STATE_OFFHOOK == state) {
	            // active
	            isPhoneCalling = true;
	        }
	        if (TelephonyManager.CALL_STATE_IDLE == state) {
	            // run when class initial and phone call ended, need detect flag
	            // from CALL_STATE_OFFHOOK
	            if (isPhoneCalling) {
	                Handler handler = new Handler();
	                //Put in delay because call log is not updated immediately when state changed
	                // The dialer takes a little bit of time to write to it 500ms seems to be enough
	                handler.postDelayed(new Runnable() {
	                    @Override
	                    public void run() {
	                        // get start of cursor
	                            String[] projection = new String[]{Calls.NUMBER};
	                            Cursor cur = getContentResolver().query(Calls.CONTENT_URI, projection, null, null, Calls.DATE +" desc");
	                            cur.moveToFirst();
	                            String lastCallnumber = cur.getString(0);
	                            Global.TableName = lastCallnumber;
	                    }
	                },200);
	                isPhoneCalling = false;
	            }
	        }
	    }
	}
	*/

    BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equalsIgnoreCase(SmartMenuService.TICKET_BUMPED)) {
                invalidateOptionsMenu();
            }
            if (action.equalsIgnoreCase(SmartMenuService.PRINT_STATUS)) {
                Bundle extras = intent.getExtras();
                Integer prid = extras.getInt("printerid");
                prid = prid - 1; // need zero relative printer #
                Integer prstatus = extras.getInt("printerstatus");
                Integer tabid = extras.getInt("tableid");
                //log("onReceive: Print_Status: prid=" + prid + " prstatus=" + prstatus + " tableid=" + tabid);
                // Update the printer status here from the service result IF we are in print RoundTrip mode or it FAILED
                //
                // Update the PR status that gets sent back---
                // 0=blank, no update
                // 1=success
                // 2=failure
                printStatus[prid] = prstatus;

                try {
                    if (numberOfDishes(tabid) > 0) {
                        // load the order from json
                        JSONArray JSONtmp = new JSONArray(JSONOrderStr[tabid]);
                        // Mark all dishes as printed if PR1 comes back successful
                        if (prid == 0 && prstatus == 1) {
                            if (Global.PrintRoundTrip) {
                                int size = numberOfDishes(tabid);
                                for (int i = 0; i < size; i++) {
                                    // mark as printed
                                    setDishPrinted(tabid, i);
                                }
                                // update the jsonstr
                                try {
                                    JSONtmp = new JSONArray(JSONOrderStr[tabid]);
                                    // re-save it
                                    saveJsonTable(tabid, JSONtmp);
                                } catch (JSONException e) {
                                    log("json except opn tab=" + e);
                                }
                            }
                        }
                    }
                } catch (Exception e1) {
                    log("Ex PrintStatus=" + e1);
                }
                mHandler.post(mUpdatePrinters);
                mHandler.post(mUpdateResults);
            }
            if (action.equalsIgnoreCase(SmartMenuService.NEW_MSG)) {
                Bundle extra = intent.getExtras();
                String newMsg = extra.getString("msgdata");
                log("PlaceOrder: MQTT Msg Received= " + newMsg);
                lastIncomingMsgData = newMsg;
                mHandler.post(mMsgArrived);
            }
            if (action.equalsIgnoreCase(SmartMenuService.NEW_ORDER)) {
                Bundle extras = intent.getExtras();
                String value = "";
                value = extras.getString("orderdata");
                log("PlaceOrder: Order Received");
                try {
                    JSONArray JSONtmp = new JSONArray(value);

                    // bump to the next ticket
                    int numTickets = JSONTickets.size();
                    if (numTickets < MaxTickets) {
                        // No duplicate mobile orders
                        String src = jsonGetter2(JSONtmp, "waiter").toString();
                        int newTicketID = mobileDeviceExists(src);    // replace existing ticket if match found
                        if (newTicketID == -1) {
                            // Add a new ticket if no match found
                            newTicketID = numTickets;
                            clearPrinterStatus();
                            // updates
                            jsonSetter(JSONtmp, "tabstate", 2);
                            jsonSetter(JSONtmp, "currenttableid", newTicketID);
                            // save it
                            JSONTickets.add(JSONtmp.toString());
                            JSONOrderStr[newTicketID] = JSONtmp.toString();
                            saveJsonTable(newTicketID, JSONtmp);
                        } else {
                            // Replace the existing mobile ticker
                            clearPrinterStatus();
                            // updates
                            jsonSetter(JSONtmp, "tabstate", 2);
                            jsonSetter(JSONtmp, "currenttableid", newTicketID);
                            // save it
                            JSONTickets.set(newTicketID, JSONtmp.toString());
                            JSONOrderStr[newTicketID] = JSONtmp.toString();
                            saveJsonTable(newTicketID, JSONtmp);
                        }
                        incomingTableName = Global.tablenames.get(newTicketID);
                    } else {
                        Toast.makeText(QuickActivity.this, getString(R.string.msg_no_tickets), Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    log("json execption processing network order");
                }
                mHandler.post(mOrderArrived);
                mHandler.post(mUpdateResults);
            }
        }
    };

    private void updateTableButtons() {
        if (currentTicketID != -1) {
            clearButton.setVisibility(View.VISIBLE);

            ivpf1.setVisibility(View.VISIBLE);
            ivpf2.setVisibility(View.VISIBLE);
            ivpf3.setVisibility(View.VISIBLE);

            if (JSONOrderList.isEmpty()) {
                closeButton.setVisibility(View.GONE);
                tvcc.setVisibility(View.GONE);
                p1Button.setVisibility(View.GONE);
                p2Button.setVisibility(View.GONE);
                p3Button.setVisibility(View.GONE);
            } else {
                closeButton.setVisibility(View.VISIBLE);
                //tvcc.setVisibility(View.VISIBLE);
                p1Button.setVisibility(View.VISIBLE);
                p2Button.setVisibility(View.VISIBLE);
                p3Button.setVisibility(View.VISIBLE);
            }
        } else {
            closeButton.setVisibility(View.GONE);
            clearButton.setVisibility(View.GONE);
            tvcc.setVisibility(View.GONE);
            p1Button.setVisibility(View.GONE);
            p2Button.setVisibility(View.GONE);
            p3Button.setVisibility(View.GONE);

            // ..... lets make them always visible
            ivpf1.setVisibility(View.VISIBLE);
            ivpf2.setVisibility(View.VISIBLE);
            ivpf3.setVisibility(View.VISIBLE);
        }
        newButton.setVisibility(View.VISIBLE);
        tvautoprint.setVisibility(View.GONE);
        ODButton.setVisibility(View.GONE);
        LinearLayout llprintstatus = (LinearLayout) findViewById(R.id.printHeaderLL);
        llprintstatus.setVisibility(View.VISIBLE);
    }

    private void updatePrinters(int table) {
        if (table != -1) {
            if (printStatus[0] == 0) {
                ivpf1.setBackgroundResource(R.drawable.presence_invisible);
            } else if (printStatus[0] == 1) {
                ivpf1.setBackgroundResource(R.drawable.presence_online);
            } else if (printStatus[0] == 2) {
                ivpf1.setBackgroundResource(R.drawable.presence_busy);
            }

            if (printStatus[1] == 0) {
                ivpf2.setBackgroundResource(R.drawable.presence_invisible);
            } else if (printStatus[1] == 1) {
                ivpf2.setBackgroundResource(R.drawable.presence_online);
            } else if (printStatus[1] == 2) {
                ivpf2.setBackgroundResource(R.drawable.presence_busy);
            }

            if (printStatus[2] == 0) {
                ivpf3.setBackgroundResource(R.drawable.presence_invisible);
            } else if (printStatus[2] == 1) {
                ivpf3.setBackgroundResource(R.drawable.presence_online);
            } else if (printStatus[2] == 2) {
                ivpf3.setBackgroundResource(R.drawable.presence_busy);
            }
        }
    }

    private void setupButtons() {
        int widthL = (int) (Utils.getWidth(QuickActivity.this) / 15.0);
        int widthM = (int) (Utils.getWidth(QuickActivity.this) / 20.0);
        int widthS = (int) (Utils.getWidth(QuickActivity.this) / 30.0);
        int widthfull = txtSize0;

        closeButton = (Button) findViewById(R.id.closeButton);
        closeButton.setTextSize(widthfull);
        closeButton.setMinWidth(widthL);
        closeButton.setMaxWidth(widthL);
        closeButton.setMinHeight(widthL);
        closeButton.setMaxHeight(widthL);
        closeButton.setText(getString(R.string.tab3_close));

        clearButton = (Button) findViewById(R.id.clearButton);
        clearButton.setTextSize(widthfull);
        clearButton.setMinWidth(widthL);
        clearButton.setMaxWidth(widthL);
        clearButton.setMinHeight(widthL);
        clearButton.setMaxHeight(widthL);
        clearButton.setText(getString(R.string.tab3_clear));

        newButton = (Button) findViewById(R.id.newButton);
        newButton.setTextSize(widthfull);
        newButton.setMinWidth(widthL);
        newButton.setMaxWidth(widthL);
        newButton.setMinHeight(widthL);
        newButton.setMaxHeight(widthL);
        newButton.setText(getString(R.string.tab3_new));

        p1Button = (Button) findViewById(R.id.printP1);
        p1Button.setTextSize(widthfull);
        p1Button.setMinWidth(widthL);
        p1Button.setMaxWidth(widthL);
        p1Button.setMinHeight(widthS);
        p1Button.setMaxHeight(widthS);

        p2Button = (Button) findViewById(R.id.printP2);
        p2Button.setTextSize(widthfull);
        p2Button.setMinWidth(widthL);
        p2Button.setMaxWidth(widthL);
        p2Button.setMinHeight(widthS);
        p2Button.setMaxHeight(widthS);

        p3Button = (Button) findViewById(R.id.printP3);
        p3Button.setTextSize(widthfull);
        p3Button.setMinWidth(widthL);
        p3Button.setMaxWidth(widthL);
        p3Button.setMinHeight(widthS);
        p3Button.setMaxHeight(widthS);

        ODButton = (Button) findViewById(R.id.openDrawer);
        ODButton.setTextSize(widthfull);
        ODButton.setMinWidth(widthL);
        ODButton.setMaxWidth(widthL);
        ODButton.setMinHeight(widthS);
        ODButton.setMaxHeight(widthS);
        ODButton.setText(getString(R.string.tab3_open));

        tvcc = (TextView) findViewById(R.id.textCredit);
        tvcc.setTextSize((float) (widthfull / 1.25));
        tvcc.setMinWidth(widthL);
        tvcc.setMaxWidth(widthL);
        tvcc.setMinHeight(widthM);
        tvcc.setMaxHeight(widthM);

        tvautoprint = (TextView) findViewById(R.id.textAutoPrint);
        tvautoprint.setTextSize(widthfull);
        tvautoprint.setMinWidth(widthL);
        tvautoprint.setMaxWidth(widthL);
        tvautoprint.setMinHeight(widthS);
        tvautoprint.setMaxHeight(widthS);
        if (autoPrint) {
            tvautoprint.setTextColor(Color.parseColor(textColors[0]));
            tvautoprint.setBackgroundColor(Color.parseColor(textColors[6]));
            tvautoprint.setText(getString(R.string.tab3_auto));
        } else {
            tvautoprint.setTextColor(Color.parseColor(textColors[0]));
            tvautoprint.setBackgroundColor(Color.parseColor(textColors[9]));
            tvautoprint.setText(getString(R.string.tab3_auto));
        }

        ivpf1 = (ImageView) findViewById(R.id.printfail1);
        ivpf2 = (ImageView) findViewById(R.id.printfail2);
        ivpf3 = (ImageView) findViewById(R.id.printfail3);
    }

    private void writeOutFile(File fildir, String fname, String fcontent) {
        File writeFile = new File(fildir, fname);
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(writeFile, false), "UTF-8"));
            writer.write(fcontent);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            log("PlaceOrder: WriteOutFile Exception: Dir=" + fildir + " fname=" + fname);
        }
    }

    // Set up the intial blank JSON representation of the order.
    public JSONArray getInitialJSONOrder(int table) throws JSONException {
        JSONArray orderArr = new JSONArray();

        // compute the:   orderid, tabletime
        String tmpTableName = "TO";
        String tmpTableTime = Utils.GetTime();
        String tmpOrderId = Utils.GetDateTime() + "-" + tmpTableName;

        orderArr.put(createStr("orderid", tmpOrderId));
        orderArr.put(createInt("source", 5));        // see the orderSource[] for values, type 5 is TO App Direct
        orderArr.put(createStr("storeid", Global.SMID));
        orderArr.put(createStr("customername", ""));
        orderArr.put(createStr("response", ""));
        orderArr.put(createStr("tablename", tmpTableName));
        orderArr.put(createStr("waiter", Global.ServerName));    // Name from Login Activity
        orderArr.put(createStr("guests", ""));
        orderArr.put(createStr("saletype", "0"));
        orderArr.put(createStr("date", ""));
        orderArr.put(createStr("tabletime", tmpTableTime));
        orderArr.put(createStr("sendtime", ""));
        orderArr.put(createStr("servertime", ""));    // set on the server side
        orderArr.put(createStr("sendtype", "1"));    // 1=normal, 2=resent
        orderArr.put(createStr("ticketnum", "N/A"));
        orderArr.put(createInt("ordertotal", 0));
        orderArr.put(createStr("deliverynumber", ""));
        orderArr.put(createStr("deliveryaddress", ""));
        orderArr.put(createStr("deliveryaddress2", ""));
        orderArr.put(createInt("currenttableid", table));
        orderArr.put(createInt("tabstate", 2));

        // Add the dish to the order ...
        // The dish information will be left blank and updated as the order is built
        orderArr.put(createArrayDishes());

        return orderArr;
    }

    public JSONObject createArrayDishes() throws JSONException {
        JSONArray JSONDishAry = new JSONArray();

        JSONObject ary = new JSONObject();

        /* Each of the dishes looks like this in JSON
        {  "dishes": [ { "dishId":  99 },
                       { "dishName":  aaa },
                       { "dishNameAlt":  aaa },
                       { "categoryName":  aaa },
                       { "categoryNameAlt":  aaa },
                       { "priceOptionId": 99 },
                       { "priceOptionName": aaa },
                       { "priceOptionNameAlt": aaa },

                       { "options": [ { "optionId": 99 },
                                      { "optionPrice": 99 },
                                      { "optionNameEn": aaa },
                                      { "optionNameCh": aaa } ]

                                    ,[ ... ]

                                    }

                       { "extras": [ { "extraId": 99 },
                                     { "extraPrice": 99 },
                                     { "extraNameEn": aaa },
                                     { "extraNameCh": aaa } ]

                                   ,[ ... ]

                                   }

                       { "qty":  99 },
                       { "priceUnitBase": 99 },
                       { "priceUnitTotal": 99 },
                       { "priceQtyTotal": 99 },
                       { "specIns":  aaa },
                       { "dishPrinted":  boolean }
        */

        ary.put("dishes", JSONDishAry);

        return ary;
    }

    public JSONObject createStr(String nam, String val) throws JSONException {
        JSONObject ary = new JSONObject();
        ary.put(nam, val);
        return ary;
    }

    public JSONObject createInt(String nam, Integer val) throws JSONException {
        JSONObject ary = new JSONObject();
        ary.put(nam, val);
        return ary;
    }

    public JSONObject createBoolean(String nam, Boolean val) throws JSONException {
        JSONObject ary = new JSONObject();
        ary.put(nam, val);
        return ary;
    }

    // Log helper function
    public static void log(String message) {
        log(message, null);
    }

    public static void log(String message, Throwable e) {
        if (mLog != null) {
            try {
                mLog.println(message);
            } catch (IOException ex) {
            }
        }
    }

    private Object jsonGetter(JSONArray json, String key) {
        Object value = null;
        for (int i = 0; i < json.length(); i++) {
            try {
                JSONObject obj = json.getJSONObject(i);
                String name = obj.getString("name");
                if (name.equalsIgnoreCase(key)) {
                    value = obj.get("value");
                }
            } catch (JSONException e) {
            }
        }
        return value;
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
                //log("i=" + i + " obj=" + obj.toString());
                if (obj.has(key)) {
                    v = i;
                    //log("obj has key=" + key);
                }
            } catch (JSONException e) {
                log("jsonGetter3 Exception=" + e);
            }
        }
        return v;
    }

    private void jsonSetter(JSONArray array, String key, Object replace) {
        for (int i = 0; i < array.length(); i++) {
            try {
                JSONObject obj = array.getJSONObject(i);
                if (obj.has(key)) {
                    obj.putOpt(key, replace);
                }
            } catch (JSONException e) {
                log("jsonSetter exception");
            }
        }
    }

    public static JSONArray RemoveJSONArray(JSONArray jarray, int pos) {
        JSONArray Njarray = new JSONArray();
        try {
            for (int i = 0; i < jarray.length(); i++) {
                if (i != pos)
                    Njarray.put(jarray.get(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Njarray;
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
                // reachable server
                downloadSuccess = true;
            } else {
                downloadSuccess = false;
            }
        } catch (Exception e) {
            downloadSuccess = false;
            log("Place Order:PingIP failed e=" + e);
        }
        return downloadSuccess;
    }

    private void populateField(Map<String, String> values, TextView view) {
        StringBuilder sb = new StringBuilder();
        for (Entry<String, String> entry : values.entrySet()) {
            String fieldName = entry.getKey();
            String fieldValue = entry.getValue();
            sb.append(fieldName)
                    .append(": ")
                    .append("<b>").append(fieldValue).append("</b>");
        }
        view.setText(Html.fromHtml(sb.toString()));
    }

    private void updateConnectionStatus(CustomDialog customDialog) {
        // update the wi-fi status
        ImageView img = (ImageView) customDialog.findViewById(R.id.lit0a);
        img.setBackgroundResource(R.drawable.presence_invisible);
        if (checkInternetConnection()) {
            img.setBackgroundResource(R.drawable.presence_online);
        } else {
            img.setBackgroundResource(R.drawable.presence_busy);
        }

        // update the SERVER connection status
        img = (ImageView) customDialog.findViewById(R.id.lit1aServer);
        img.setBackgroundResource(R.drawable.presence_invisible);
        if (Global.CheckAvailability) {
            new ping204(img).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        // update the Printer1 status
        if (Global.POS1Enable) {
            img = (ImageView) customDialog.findViewById(R.id.lit2a);
            img.setBackgroundResource(R.drawable.presence_invisible);
            new pingFetch(Global.POS1Ip, img).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        // update the Printer2 status
        if (Global.POS2Enable) {
            img = (ImageView) customDialog.findViewById(R.id.lit3a);
            img.setBackgroundResource(R.drawable.presence_invisible);
            new pingFetch(Global.POS2Ip, img).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        // update the Printer3 status
        if (Global.POS3Enable) {
            img = (ImageView) customDialog.findViewById(R.id.lit4a);
            img.setBackgroundResource(R.drawable.presence_invisible);
            new pingFetch(Global.POS3Ip, img).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    // check for ping connectivity
    private class pingFetch extends AsyncTask<Void, String, Integer> {
        private String ip1;
        private Integer code;
        private InetAddress in2;
        private ImageView img;

        public pingFetch(String ip, ImageView imgv) {
            ip1 = ip;
            in2 = null;
            img = imgv;
            code = 0;
        }

        protected void onPreExecute(Void... params) {
        }

        protected Integer doInBackground(Void... params) {
            try {
                in2 = InetAddress.getByName(ip1);
            } catch (Exception e) {
                e.printStackTrace();
                code = 2;
            }
            try {
                if (in2.isReachable(Global.ConnectTimeout)) {
                    code = 1;
                } else {
                    code = 2;
                }
            } catch (Exception e) {
                e.printStackTrace();
                code = 2;
            }
            return 1;
        }

        protected void onProgressUpdate(String msg) {
        }

        protected void onPostExecute(Integer result) {
            if (code == 1) {
                img.setBackgroundResource(R.drawable.presence_online);
            }
            if (code == 2) {
                img.setBackgroundResource(R.drawable.presence_busy);
            }
        }
    }

    // Check for connectivity hitting the 204 script and update the UI
    public class ping204 extends AsyncTask<Void, String, Integer> {
        private Boolean code;
        private ImageView img;

        public ping204(ImageView imgv) {
            code = false;
            img = imgv;
        }

        protected void onPreExecute(Void... params) {
        }

        protected Integer doInBackground(Void... params) {
            try {
                String ip1 = Global.ProtocolPrefix + Global.ServerIP + Global.ServerReturn204;
                int status = -1;
                code = false;
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
                    if (status == 204) code = true;
                } catch (Exception e) {
                    code = false;
                }
            } catch (Exception e) {
                code = false;
            }
            return 1;
        }

        protected void onProgressUpdate(String msg) {
        }

        protected void onPostExecute(Integer result) {
            if (code == true) {
                img.setBackgroundResource(R.drawable.presence_online);
            } else {
                img.setBackgroundResource(R.drawable.presence_busy);
            }
        }
    }

    private void createAndRunStatusThread(final Activity act, final CustomDialog cd) {
        m_bStatusThreadStop = false;
        m_statusThread = new Thread(new Runnable() {
            public void run() {
                while (m_bStatusThreadStop == false) {
                    try {
                        //anything touching the GUI has to run on the Ui thread
                        act.runOnUiThread(new Runnable() {
                            public void run() {
                                updateConnectionStatus(cd);
                            }
                        });
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        m_bStatusThreadStop = true;
                    }
                }
            }
        });
        m_statusThread.start();
    }

    private void setupCatList() {
        // Loop through each line of cat file and populate both the Category String Arrays
        CategoryEng.clear();
        CategoryAlt.clear();
        // also populate the printer filter arrays
        P2Filter.clear();
        P3Filter.clear();
        for (int i = 0; i < categoryAll.length; i++) {
            String line = categoryAll[i];
            String[] linColumns = line.split("\\|");
            String[] linLang = linColumns[1].split("\\\\");
            // if there are no special, then we dont want to add the special to the category selector
            // even though the specials category still always resides in the TXT file
            if ((Global.NumSpecials > 0) || !(linLang[0].equalsIgnoreCase("Specials"))) {
                CategoryEng.add(linLang[0]);
                CategoryAlt.add(linLang[1]);
                // print filters arrays
                if (Global.P2FilterCats.contains(linColumns[0])) {
                    P2Filter.add(i, true);
                } else {
                    P2Filter.add(i, false);
                }
                if (Global.P3FilterCats.contains(linColumns[0])) {
                    P3Filter.add(i, true);
                } else {
                    P3Filter.add(i, false);
                }
            }
        }
        //log("P2Filter=" + P2Filter.toString());
        //log("P3Filter=" + P3Filter.toString());
    }

    // Build an ArrayList for the orderAdapter from the Json dishes in table tabid
    private void setJSONOrderList(int tabid) {
        try {
            if (tabid != -1) {
                JSONArray JSONOrderAry = new JSONArray(JSONOrderStr[tabid]);
                JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
                JSONArray jda = JSONdishObj.getJSONArray("dishes");
                int numdish = jda.length();
                //log("Number of dishes=" + numdish);
                JSONOrderList.clear();
                if (numdish > 0) {
                    JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");
                    for (int i = 0; i < JSONdishesAry.length(); i++) {
                        JSONArray jd = JSONdishesAry.getJSONArray(i);
                        JSONOrderList.add(jd);
                    }
                }
            } else {
                // No selected ticket
                JSONOrderList.clear();
            }
        } catch (Exception e) {
            log("json setJSONOrderList Table=" + tabid + " Exception=" + e);
        }
    }

    // Returns the number of Dishes currently on a table
    private int numberOfDishes(int tabid) {
        int numdish = -1;
        try {
            JSONArray JSONOrderAry = new JSONArray(JSONOrderStr[tabid]);
            JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
            JSONArray jda = JSONdishObj.getJSONArray("dishes");
            if (jda != null) {
                numdish = jda.length();
            } else {
                log("numdish=-1 due to NULL");
            }
        } catch (Exception e) {
            log("json numberOfDishes Table=" + tabid + " Exception=" + e);
        }
        //log("Table=" + tabid +" Dish count=" + numdish);
        return numdish;
    }

    // Set a specific dish as printed
    private void setDishPrinted(int tabid, int dishid) {
        try {
            JSONArray JSONOrderAry = new JSONArray(JSONOrderStr[tabid]);
            JSONObject JSONdishObj = JSONOrderAry.getJSONObject(jsonGetter3(JSONOrderAry, "dishes"));
            //JSONArray jda = JSONdishObj.getJSONArray("dishes");
            JSONArray JSONdishesAry = JSONdishObj.getJSONArray("dishes");
            JSONArray jd = JSONdishesAry.getJSONArray(dishid);
            jsonSetter(jd, "dishPrinted", true);
            //jd.put(createBoolean("dishPrinted",false));
            saveJsonTable(tabid, JSONOrderAry);
            //log("aaa=" + JSONOrderAry);
        } catch (Exception e) {
            log("json setDishPrinted Table=" + tabid + " dishID=" + dishid + " Exception=" + e);
        }
        //log("setDishPrinted(" + tabid + "," + dishid + ")");
    }

    // Return true is a Table has TabState=1 or TabState=2
    private boolean tabIsOpen(int tabid) {
        boolean hastab = false;
        try {
            //JSONArray JSONtmp = new JSONArray(JSONOrderStr[tabid]);
            //int tabstate = (Integer) jsonGetter2(JSONtmp,"tabstate");
            //if (tabstate > 0) {
            if (numberOfDishes(tabid) > 0) hastab = true;
            //log("tabIsOpen(" + tabid + ")= " + tabstate);
            //}
        } catch (Exception e) {
            log("json tabIsOpen Table=" + tabid + " Exception=" + e);
        }
        //log("hasOrderID(" + tabid + ")= " + hasorderid);
        return hastab;
    }

    // Save a JSON table for persistence
    private void saveJsonTable(int tabid, JSONArray json) {
        try {
            JSONOrderStr[tabid] = json.toString();
            JSONTickets.set(tabid, json.toString());
            prefEdit.putString("jsonorderstr" + tabid, JSONOrderStr[tabid]);
            prefEdit.commit();
        } catch (Exception e) {
            log("saveJsonTable Table=" + tabid + " Exception=" + e);
        }
    }

    // Remove a JSON table from persistence
    private void removeJsonTable(int tabid, JSONArray json) {
        try {
            JSONOrderStr[tabid] = json.toString();
            prefEdit.remove("jsonorderstr" + tabid);
            prefEdit.commit();
        } catch (Exception e) {
            log("removeJsonTable Table=" + tabid + " Exception=" + e);
        }
        // clear the response code for the ticket
        //responseCode[tabid] = 0;
    }

    // Clear all the printers status (x3) for a table
    private void clearPrinterStatus() {
        printStatus[0] = 0;
        printStatus[1] = 0;
        printStatus[2] = 0;
        mHandler.post(mUpdatePrinters);
    }

    // Re-order Ticket IDs
    // Inside the JSON Tickets, there is a currentTableID
    private void reorderTicketIDs() {
        try {
            int numTickets = JSONTickets.size();
            for (int i = 0; i < numTickets; i++) {
                JSONArray JSONtmp = new JSONArray(JSONTickets.get(i));
                jsonSetter(JSONtmp, "currenttableid", i);
                JSONTickets.set(i, JSONtmp.toString());
                JSONOrderStr[i] = JSONtmp.toString();
                saveJsonTable(i, JSONtmp);
            }
        } catch (JSONException e) {
            log("json reorder Tickets= e");
        }
    }

    // See if there is an existing ticket for a device id (did) and return its ticket number or -1 if no matches
    private int mobileDeviceExists(String did) {
        int id = -1;
        try {
            int numTickets = JSONTickets.size();
            for (int i = 0; i < numTickets; i++) {
                JSONArray JSONtmp = new JSONArray(JSONTickets.get(i));
                int src = (Integer) jsonGetter2(JSONtmp, "source");
                String did2 = jsonGetter2(JSONtmp, "waiter").toString();
                if ((did2.equalsIgnoreCase(did)) && (src == 3)) id = i;
            }
        } catch (Exception e) {
            log("json deviceExist Exception=" + e);
        }
        return id;
    }

}