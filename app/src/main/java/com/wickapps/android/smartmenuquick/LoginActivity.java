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
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.StateListDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ScaleXSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.epson.EpsonCom.EpsonCom;
import com.epson.EpsonCom.EpsonCom.ERROR_CODE;
import com.epson.EpsonCom.EpsonComDevice;
import com.epson.EpsonCom.EpsonComDeviceParameters;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LoginActivity extends Activity {

    EditText et1;
    private String infoWifi;

    private Button nameButton;

    private StateListDrawable states;
    private static Integer ButColor;
    private static Integer SelButColor;

    private ListView listUsers;
    private ListView listTopics;
    private ArrayAdapter<?> adapterUA, adapterBA;

    private SharedPreferences prefs;
    private Editor prefEdit;

    private File logsDir;

    //Status Thread
    Thread m_statusThread;
    boolean m_bStatusThreadStop;

    private EditText etPassword1, etPassword2, etPassword3, etPassword4;
    private GenericTextWatcher watcher1, watcher2, watcher3, watcher4;

    //EpsonCom Objects
    private EpsonComDevice POSDev;
    private EpsonComDeviceParameters POSParams;

    private static String[] colors = new String[]{
            "#ffffff", "#fad666", "#fe7f3d", "#dddddd", "#3a6e52", "#7753a9", "#247fca", "#00b49c", "#d24a85",
            "#83c748", "#d8994d", "#947c4b", "#9da6a2", "#f6adcd", "#3e8872", "#6f2c91", "#ffffff", "#fad666", "#fe7f3d", "#e3495a", "#d24a85"
    };

    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        this.registerReceiver(wifiStatusReceiver, filter);
        Global.PausedOrder = true;
    }

    @Override
    public void onPause() {
        this.unregisterReceiver(wifiStatusReceiver);
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.login);

        Global.PausedOrder = true;
        Global.LoggedIn = false;

        // Setup the Action bar
        getActionBar().setDisplayShowTitleEnabled(true);
        getActionBar().setTitle(Global.AppNameA);
        getActionBar().setSubtitle(Global.AppNameB);

        // grab the directory where logs will be stored
        File logsDir = getExternalFilesDir("SmartMenuLogs");
        if (!logsDir.exists()) logsDir.mkdirs();


        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefEdit = prefs.edit();

        int smallFontSize = (int) (Utils.getFontSize(LoginActivity.this) / 1.15);
        int largeFontSize = (int) (Utils.getFontSize(LoginActivity.this));

        ButColor = Color.parseColor("#44222222");
        SelButColor = Color.parseColor("#44eeeeee");

        String txt = Utils.FancyDate() + "   " + Utils.GetTime();
        TextView tv = (TextView) findViewById(R.id.textDT);
        tv.setText(txt);
        tv.setTextColor(Color.parseColor(colors[1]));

        tv = (TextView) findViewById(R.id.textTitle);
        tv.setText(getString(R.string.login_title));
        tv.setTextColor(Color.parseColor("#ffffff"));

        tv = (TextView) findViewById(R.id.textFloat);
        tv.setText(getString(R.string.login_float));
        tv.setTextColor(Color.parseColor("#eeeeee"));

        Button but1 = (Button) findViewById(R.id.butLogin);
        but1.setText(getString(R.string.login_start));
        but1.setTextColor(Color.parseColor("#eeeeee"));
        //but1.setTextSize(smallFontSize);

        et1 = (EditText) findViewById(R.id.etFloat);
        et1.setRawInputType(Configuration.KEYBOARD_12KEY);
        et1.setText("0");
        //et1.requestFocus();

        nameButton = (Button) findViewById(R.id.spinnerName);
        nameButton.setText(getString(R.string.login_select_name));
        nameButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showNameDialog();
            }
        });

        // check for start button press
        Button butLogin = (Button) findViewById(R.id.butLogin);
        butLogin.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                //Toast.makeText(LoginActivity.this, "checkedPic=" + checkedPic, Toast.LENGTH_SHORT).show();
                if ((et1.getText().toString().length() > 0) && (!nameButton.getText().toString().equalsIgnoreCase(getString(R.string.login_select_name)))) {
                    // set the vars
                    Global.LoginTime = Utils.GetDateTime();
                    Global.ServerName = Global.CheckedPicName;
                    // carry on
                    finish();

                    // save new user logged in
                    String sendserver = "3," + Utils.GetDateTime() + "," + Global.ServerName;
                    activityLogger(sendserver);

                    Intent kintent = new Intent(getApplicationContext(), QuickActivity.class);
                    kintent.setFlags((Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                    startActivity(kintent);
                }
            }
        });
    }

    protected void showNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.login_person_name));
        // need the pics in a string [] to pass in multi
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
                        Global.CheckedPicID = which;
                        JSONArray tmp = new JSONArray(Global.userList.get(which));
                        String nam = jsonGetter2(tmp, "name").toString();
                        String pin = jsonGetter2(tmp, "pin").toString();
                        int level = (Integer) jsonGetter2(tmp, "userlevel");
                        // Ask for password
                        getPassword(nam, pin, level, 1);
                    } catch (JSONException e) {
                    }
                }
            };
            builder.setSingleChoiceItems(tmpArr, Global.CheckedPicID, picDialogListener);
            AlertDialog dialog = builder.create();
            dialog.show();
        } catch (JSONException e) {
        }
    }

    private void getPassword(String nam, String pw, int lev, int returnID) {
        final Dialog dialogPW;
        dialogPW = new Dialog(LoginActivity.this);
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

        // setup the text watchers
        watcher1 = new GenericTextWatcher(etPassword1, nam, pw, lev, returnID, dialogPW);
        etPassword1.addTextChangedListener(watcher1);
        watcher2 = new GenericTextWatcher(etPassword2, nam, pw, lev, returnID, dialogPW);
        etPassword2.addTextChangedListener(watcher2);
        watcher3 = new GenericTextWatcher(etPassword3, nam, pw, lev, returnID, dialogPW);
        etPassword3.addTextChangedListener(watcher3);
        watcher4 = new GenericTextWatcher(etPassword4, nam, pw, lev, returnID, dialogPW);
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
        private String nam;
        private String pw;
        private int level;
        private int returnID;
        private Dialog dialogPW;

        private GenericTextWatcher(View view, String nam, String pw, int level, int returnID, Dialog dialogPW) {
            this.view = view;
            this.nam = nam;
            this.pw = pw;
            this.level = level;
            this.returnID = returnID;
            this.dialogPW = dialogPW;
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
                    // turn off keyboard
                    etPassword4.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        // In the order app, only the the first line works. Here, both seem to work
                        // stackoverflow.com/questions/1109022/close-hide-the-android-soft-keyboard
                        //imm.toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY);
                        imm.hideSoftInputFromWindow(etPassword4.getWindowToken(), 0);
                    }
                    if (pwMatch(pw)) {
                        switch (returnID) {
                            case 1:
                                // Handle Password access for the Register Login
                                TextView txt = (TextView) findViewById(R.id.spinnerName);
                                txt.setText(nam);
                                Global.CheckedPicName = nam;
                                Global.LoginTime = Utils.GetDateTime();
                                Global.ServerName = Global.CheckedPicName;
                                Global.UserLevel = level;
                                // save new user logged in
                                String sendserver = "3," + Utils.GetDateTime() + "," + Global.ServerName;
                                activityLogger(sendserver);
                                finish();
                                Intent kintent = new Intent(getApplicationContext(), QuickActivity.class);
                                kintent.setFlags((Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                                startActivity(kintent);
                                break;
                            case 2:
                                kintent = new Intent(getApplicationContext(), SettingsActivity.class);
                                kintent.setFlags((Intent.FLAG_ACTIVITY_NO_HISTORY));
                                startActivity(kintent);
                                break;
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
                            }
                        });
                        alertDialog.show();
                    }
                }
        );
    }

    // below is the over ride that will disable the back button
    public void onBackPressed() {
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return (ni != null && ni.isAvailable() && ni.isConnected());
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

    private void openDrawer() {
        if (isOnline()) {
            POSDev = new EpsonComDevice();
            POSParams = new EpsonComDeviceParameters();

            POSParams.PortType = EpsonCom.PORT_TYPE.ETHERNET;
            POSParams.IPAddress = Global.POS1Ip;
            POSParams.PortNumber = 9100;
            POSDev.setDeviceParameters(POSParams);

            ERROR_CODE err = null;
            err = POSDev.openDevice();

            if (err == ERROR_CODE.SUCCESS) {
                err = POSDev.sendCommand("ESC p 0 2 2");    // open the money kick pin2 4ms on 2ms off
                err = POSDev.sendCommand("ESC p 1 2 2");    // open the money kick pin5 4ms on 2ms off
                POSDev.closeDevice();
                String sendserver = "0," + Utils.GetDateTime() + "," + "No user currently logged in";
                activityLogger(sendserver);
            } else {
                String errorString = EpsonCom.getErrorText(err);
                messageBox(LoginActivity.this,
                        "Sorry, Cash Drawer cannot be opened. " +
                                errorString,
                        "Connection problem 1");
            }
        } else {
            String errorString = "Sorry, Cash Drawer cannot be opened. ";
            messageBox(LoginActivity.this, errorString, "Connection problem 1b");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        SubMenu subMenu0 = menu.addSubMenu(0, 0, menu.NONE, "Tools");
        subMenu0.setIcon(android.R.drawable.ic_menu_preferences);
        subMenu0.add(0, 11, menu.NONE, "Status");
        subMenu0.add(0, 12, menu.NONE, "Settings");
        subMenu0.add(0, 13, menu.NONE, "Open Cash Drawer");
        subMenu0.add(0, 16, menu.NONE, "Staff Management");
        MenuItem subMenu0Item = subMenu0.getItem();
        subMenu0Item.setIcon(android.R.drawable.ic_menu_preferences);
        subMenu0Item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        menu.add(0, 1, menu.NONE, "Exit");
        MenuItem item1 = menu.getItem(1);
        item1.setIcon(null);
        item1.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        item1.setTitle("Exit");
        return (super.onCreateOptionsMenu(menu));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == 11) {
            // Popup the status window
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

            /*
            // Not supporting TickerNumbers in the Quick Service App.
            TextView tv8 = (TextView) customDialog.findViewById(R.id.AboutTicketnum);
            Map<String, String> map8 = new LinkedHashMap<String, String>();
            map8.put(getString(R.string.msg_about_ticketnum), Global.TicketNum.toString());
            populateField(map8, tv8);
            */

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

            //create and run status thread
            //createAndRunStatusThread(this,customDialog);
            // Just update the status indicators once
            updateConnectionStatus(customDialog);

            return (true);
        }

        if (item.getItemId() == 12) {
            // Settings
            // Ask for password
            getPassword("None", Global.AdminPin, Global.UserLevel, 2);
            return (true);
        }
        if (item.getItemId() == 13) {
            openDrawer();
            return (true);
        }
        if (item.getItemId() == 16) {
            LayoutInflater factory = LayoutInflater.from(this);
            final View textEntryView = factory.inflate(R.layout.user_admin, null);

            final CustomDialog customDialog = new CustomDialog(this);
            customDialog.setContentView(textEntryView);
            customDialog.show();
            customDialog.setCancelable(true);
            customDialog.setCanceledOnTouchOutside(true);

            listUsers = (ListView) customDialog.findViewById(R.id.listUsers);
            listUsers.setLongClickable(true);
            listUsers.setPadding(2, 2, 2, 2);
            adapterUA = new UserListAdapter(LoginActivity.this, R.layout.cat_item, Global.userList);
            listUsers.setAdapter(adapterUA);

            Button but1 = (Button) customDialog.findViewById(R.id.butNewUser);
            but1.setText(getString(R.string.login_new_user));
            but1.setTextColor(Color.parseColor("#eeeeee"));
            but1.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (Global.userList.size() <= 9) {
                        final Dialog dialogAU = new Dialog(LoginActivity.this);
                        dialogAU.setContentView(R.layout.add_user);
                        dialogAU.setCancelable(true);
                        dialogAU.setCanceledOnTouchOutside(true);
                        String tit = getString(R.string.login_person_name);
                        dialogAU.setTitle(tit);
                        TextView AUtext = (TextView) dialogAU.findViewById(R.id.AUtext);
                        AUtext.setText(getString(R.string.login_new_user));
                        AUtext.setTextColor(Color.parseColor("#EEEEEE"));
                        // edit text box is next
                        Button AUsave = (Button) dialogAU.findViewById(R.id.AUadd);
                        AUsave.setText(getString(R.string.login_save));
                        AUsave.setOnClickListener(new OnClickListener() {
                            public void onClick(View v) {
                                EditText specET = (EditText) dialogAU.findViewById(R.id.AUedit);
                                String specins = specET.getText().toString();
                                specins = specins.replaceAll("[^\\p{L}\\p{N}-\\s]", "");
                                if ((specins.length() > 0) && (!Global.userList.contains(specins))) {

                                    Global.userList.add(specins);
                                    Collections.sort(Global.userList);
                                    adapterUA.notifyDataSetChanged();
                                    dialogAU.dismiss();

                                    Editor prefEdit = prefs.edit();
                                    Set<String> set = new HashSet<String>();
                                    set.addAll(Global.userList);
                                    prefEdit.putStringSet("userlist", set);
                                    prefEdit.commit();
                                }
                            }
                        });
                        dialogAU.show();
                    }
                }
            });
            return (true);
        }
        return (super.onOptionsItemSelected(item));
    }

    private class UserListAdapter extends ArrayAdapter<String> {
        private ArrayList<String> items;

        public UserListAdapter(LoginActivity loginActivity, int textViewResourceId, ArrayList<String> items) {
            super(getBaseContext(), textViewResourceId, items);
            this.items = items;
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.user_item, null);
            }
            String o = items.get(position);
            if (o != null) {
                TextView tt = (TextView) v.findViewById(R.id.user_item_title);
                if (tt != null) {
                    tt.setText(o);
                    tt.setTextSize((float) (Utils.getFontSize(LoginActivity.this) / 1.00));
                    tt.setPadding(2, 2, 2, 2);
                    tt.setSingleLine();
                }
            }
            // Button to delete a User
            Button del = (Button) v.findViewById(R.id.UserDeleteBut);
            del.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    AlertDialog alertDialog = new AlertDialog.Builder(LoginActivity.this).create();
                    alertDialog.setTitle(getString(R.string.login_delete_title));
                    alertDialog.setMessage(getString(R.string.login_delete_text));
                    alertDialog.setButton2(getString(R.string.tab3_delete), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // delete the item
                            Global.userList.remove(position);
                            adapterUA.notifyDataSetChanged();

                            Editor prefEdit = prefs.edit();
                            Set<String> set = new HashSet<String>();
                            set.addAll(Global.userList);
                            prefEdit.putStringSet("userlist", set);
                            prefEdit.commit();
                        }
                    });
                    alertDialog.setButton(getString(R.string.tab3_back), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // back do nothing
                        }
                    });
                    alertDialog.show();
                }
            });
            return v;
        }
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
                if ((isOnline()) & (Global.TicketToCloud)) {
                    String postURL = "https://" + Global.ServerIP + Global.PosSaveActivityURL;
                    Utils.SendMultipartAdhoc(postURL,
                            sendServer,
                            Global.SMID);
                } else {
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

    private boolean checkInternetConnection() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        // test for connection
        if (cm.getActiveNetworkInfo() != null
                && cm.getActiveNetworkInfo().isAvailable()
                && cm.getActiveNetworkInfo().isConnected()) {
            return true;
        } else {
            return false;
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
        // AWS doesnt seem to like ICMP so try a 204 check on the server
        //new pingFetch(Global.ServerIP, img).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        new ping204(img).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

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
                    InputStream in = null;
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

    private Object jsonGetter2(JSONArray json, String key) {
        Object value = null;
        for (int i = 0; i < json.length(); i++) {
            try {
                JSONObject obj = json.getJSONObject(i);
                if (obj.has(key)) {
                    value = obj.get(key);
                }
            } catch (JSONException e) {
            }
        }
        return value;
    }

}