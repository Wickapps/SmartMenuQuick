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
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;

public class SyncActivity extends Activity {

    Button bDel, bBack, bSave;
    private File dir;
    private ImageView img;

    private SharedPreferences prefs;
    private static Boolean downloadSuccess = false;
    private String serverMv;

    private File textDir;
    private File logsDir;

    //Status Thread
    Thread m_statusThread;
    boolean m_bStatusThreadStop;

    private static String[] filesLocalName = new String[]{"menufile.txt", "category.txt", "kitchen.txt", "settings.txt", "options.txt", "extras.txt"};
    private static String[] filesSourceName = new String[]{"", "", "", "", "", ""};
    private static String[] filesText = new String[]{"", "", "", "", "", ""};

    private ArrayList<String> catList = new ArrayList<String>();
    protected ArrayList<CharSequence> selectedP2Cat = new ArrayList<CharSequence>();
    protected ArrayList<CharSequence> selectedP3Cat = new ArrayList<CharSequence>();
    protected Button p2CatButton, p3CatButton;

    //	Need handler for callbacks to the UI thread
    final Handler mHandler = new Handler();

    final Runnable noConnection = new Runnable() {
        public void run() {
            failedAuth0();
        }
    };
    final Runnable exceptionConnection = new Runnable() {
        public void run() {
            failedAuth2();
        }
    };
    final Runnable exceptionReload = new Runnable() {
        public void run() {
            TextView txt = (TextView) findViewById(R.id.textLoad);
            txt.setText("Failed");
            failedAuth3();
        }
    };
    final Runnable reloaded = new Runnable() {
        public void run() {
            TextView txt = (TextView) findViewById(R.id.textLoad);
            txt.setText("Success");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        textDir = new File(android.os.Environment.getExternalStorageDirectory(), "SmartMenuFiles");
        if (!textDir.exists()) textDir.mkdirs();
        logsDir = new File(android.os.Environment.getExternalStorageDirectory(), "SmartMenuLogs");
        if (!logsDir.exists()) logsDir.mkdirs();

        setContentView(R.layout.sync_layout);

        // Setup the ActionBar
        Context context = getActionBar().getThemedContext();
        getActionBar().setDisplayShowTitleEnabled(true);
        getActionBar().setSubtitle(Global.AppName);
        getActionBar().setTitle(Global.CustomerName + " " + Global.StoreID);
        getActionBar().setDisplayUseLogoEnabled(false);
        getActionBar().setDisplayShowHomeEnabled(false);

        //create and run status thread
        createAndRunStatusThread(this);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Global.PublicCloud = (new Boolean(prefs.getBoolean("publiccloud", true)));
        RadioButton rb1 = (RadioButton) findViewById(R.id.ctBut1);
        RadioButton rb2 = (RadioButton) findViewById(R.id.ctBut2);
        if (Global.PublicCloud) {
            rb1.setChecked(false);
            rb2.setChecked(true);
        } else {
            rb1.setChecked(true);
            rb2.setChecked(false);
        }

        Global.StoreID = (new String(prefs.getString("storeid", "3")));
        RadioButton rbsi1 = (RadioButton) findViewById(R.id.siBut1);
        RadioButton rbsi2 = (RadioButton) findViewById(R.id.siBut2);
        RadioButton rbsi3 = (RadioButton) findViewById(R.id.siBut3);
        RadioButton rbsi4 = (RadioButton) findViewById(R.id.siBut4);
        RadioButton rbsi5 = (RadioButton) findViewById(R.id.siBut5);
        if (Global.StoreID.equalsIgnoreCase("1")) {
            rbsi1.setChecked(true);
            rbsi2.setChecked(false);
            rbsi3.setChecked(false);
            rbsi4.setChecked(false);
            rbsi5.setChecked(false);
        } else if (Global.StoreID.equalsIgnoreCase("2")) {
            rbsi1.setChecked(false);
            rbsi2.setChecked(true);
            rbsi3.setChecked(false);
            rbsi4.setChecked(false);
            rbsi5.setChecked(false);
        } else if (Global.StoreID.equalsIgnoreCase("3")) {
            rbsi1.setChecked(false);
            rbsi2.setChecked(false);
            rbsi3.setChecked(true);
            rbsi4.setChecked(false);
            rbsi5.setChecked(false);
        } else if (Global.StoreID.equalsIgnoreCase("4")) {
            rbsi1.setChecked(false);
            rbsi2.setChecked(false);
            rbsi3.setChecked(false);
            rbsi4.setChecked(true);
            rbsi5.setChecked(false);
        } else {
            rbsi1.setChecked(false);
            rbsi2.setChecked(false);
            rbsi3.setChecked(false);
            rbsi4.setChecked(false);
            rbsi5.setChecked(true);
        }

        Global.ActiveMenuID = (new String(prefs.getString("activemenuid", "3")));
        RadioButton rbami1 = (RadioButton) findViewById(R.id.amiBut1);
        RadioButton rbami2 = (RadioButton) findViewById(R.id.amiBut2);
        RadioButton rbami3 = (RadioButton) findViewById(R.id.amiBut3);
        RadioButton rbami4 = (RadioButton) findViewById(R.id.amiBut4);
        RadioButton rbami5 = (RadioButton) findViewById(R.id.amiBut5);
        if (Global.ActiveMenuID.equalsIgnoreCase("1")) {
            rbami1.setChecked(true);
            rbami2.setChecked(false);
            rbami3.setChecked(false);
            rbami4.setChecked(false);
            rbami5.setChecked(false);
        } else if (Global.ActiveMenuID.equalsIgnoreCase("2")) {
            rbami1.setChecked(false);
            rbami2.setChecked(true);
            rbami3.setChecked(false);
            rbami4.setChecked(false);
            rbami5.setChecked(false);
        } else if (Global.ActiveMenuID.equalsIgnoreCase("3")) {
            rbami1.setChecked(false);
            rbami2.setChecked(false);
            rbami3.setChecked(true);
            rbami4.setChecked(false);
            rbami5.setChecked(false);
        } else if (Global.ActiveMenuID.equalsIgnoreCase("4")) {
            rbami1.setChecked(false);
            rbami2.setChecked(false);
            rbami3.setChecked(false);
            rbami4.setChecked(true);
            rbami5.setChecked(false);
        } else {
            rbami1.setChecked(false);
            rbami2.setChecked(false);
            rbami3.setChecked(false);
            rbami4.setChecked(false);
            rbami5.setChecked(true);
        }

        Global.Printer1Type = (new Boolean(prefs.getBoolean("printer1type", false)));
        RadioButton p1rbpt1 = (RadioButton) findViewById(R.id.p1ptBut1);
        RadioButton p1rbpt2 = (RadioButton) findViewById(R.id.p1ptBut2);
        if (Global.Printer1Type) {
            p1rbpt1.setChecked(true);
            p1rbpt2.setChecked(false);
        } else {
            p1rbpt1.setChecked(false);
            p1rbpt2.setChecked(true);
        }

        Global.Printer2Type = (new Boolean(prefs.getBoolean("printer2type", false)));
        RadioButton p2rbpt1 = (RadioButton) findViewById(R.id.p2ptBut1);
        RadioButton p2rbpt2 = (RadioButton) findViewById(R.id.p2ptBut2);
        if (Global.Printer2Type) {
            p2rbpt1.setChecked(true);
            p2rbpt2.setChecked(false);
        } else {
            p2rbpt1.setChecked(false);
            p2rbpt2.setChecked(true);
        }

        Global.Printer3Type = (new Boolean(prefs.getBoolean("printer3type", false)));
        RadioButton p3rbpt1 = (RadioButton) findViewById(R.id.p3ptBut1);
        RadioButton p3rbpt2 = (RadioButton) findViewById(R.id.p3ptBut2);
        if (Global.Printer3Type) {
            p3rbpt1.setChecked(true);
            p3rbpt2.setChecked(false);
        } else {
            p3rbpt1.setChecked(false);
            p3rbpt2.setChecked(true);
        }

        Global.AutoMenuReload = (new Boolean(prefs.getBoolean("automenureload", true)));
        CheckBox cb9 = (CheckBox) findViewById(R.id.autoMenuReload);
        if (Global.AutoMenuReload) {
            cb9.setChecked(true);
        } else {
            cb9.setChecked(false);
        }

        Global.POS1Logo = (new Boolean(prefs.getBoolean("pos1logo", false)));
        CheckBox cb19 = (CheckBox) findViewById(R.id.pos1logo);
        if (Global.POS1Logo) {
            cb19.setChecked(true);
        } else {
            cb19.setChecked(false);
        }

        Global.POS1Enable = (new Boolean(prefs.getBoolean("pos1enable", false)));
        CheckBox cb20 = (CheckBox) findViewById(R.id.pos1enable);
        if (Global.POS1Enable) {
            cb20.setChecked(true);
        } else {
            cb20.setChecked(false);
        }

        Global.POS2Enable = (new Boolean(prefs.getBoolean("pos2enable", false)));
        CheckBox cb21 = (CheckBox) findViewById(R.id.pos2enable);
        if (Global.POS2Enable) {
            cb21.setChecked(true);
        } else {
            cb21.setChecked(false);
        }

        Global.POS3Enable = (new Boolean(prefs.getBoolean("pos3enable", false)));
        CheckBox cb22 = (CheckBox) findViewById(R.id.pos3enable);
        if (Global.POS3Enable) {
            cb22.setChecked(true);
        } else {
            cb22.setChecked(false);
        }

        Global.P2KitchenCodes = (new Boolean(prefs.getBoolean("p2kitchencodes", true)));
        CheckBox cb23 = (CheckBox) findViewById(R.id.p2KitchenCodes);
        if (Global.P2KitchenCodes) {
            cb23.setChecked(true);
        } else {
            cb23.setChecked(false);
        }
        Global.P3KitchenCodes = (new Boolean(prefs.getBoolean("p3kitchencodes", true)));
        CheckBox cb24 = (CheckBox) findViewById(R.id.p3KitchenCodes);
        if (Global.P3KitchenCodes) {
            cb24.setChecked(true);
        } else {
            cb24.setChecked(false);
        }

        Global.P1PrintSentTime = (new Boolean(prefs.getBoolean("p1printsenttime", true)));
        CheckBox cbp1pst = (CheckBox) findViewById(R.id.p1PrintSentTime);
        if (Global.P1PrintSentTime) {
            cbp1pst.setChecked(true);
        } else {
            cbp1pst.setChecked(false);
        }

        Global.P2PrintSentTime = (new Boolean(prefs.getBoolean("p2printsenttime", true)));
        CheckBox cbp2pst = (CheckBox) findViewById(R.id.p2PrintSentTime);
        if (Global.P2PrintSentTime) {
            cbp2pst.setChecked(true);
        } else {
            cbp2pst.setChecked(false);
        }

        Global.P3PrintSentTime = (new Boolean(prefs.getBoolean("p3printsenttime", true)));
        CheckBox cbp3pst = (CheckBox) findViewById(R.id.p3PrintSentTime);
        if (Global.P3PrintSentTime) {
            cbp3pst.setChecked(true);
        } else {
            cbp3pst.setChecked(false);
        }

        Global.AutoOpenDrawer = (new Boolean(prefs.getBoolean("autoopendrawer", false)));
        CheckBox cbaod = (CheckBox) findViewById(R.id.autoOpenDrawer);
        if (Global.AutoOpenDrawer) {
            cbaod.setChecked(true);
        } else {
            cbaod.setChecked(false);
        }

        Global.PrintRoundTrip = (new Boolean(prefs.getBoolean("printroundtrip", true)));
        CheckBox cbprt = (CheckBox) findViewById(R.id.printRoundTrip);
        if (Global.PrintRoundTrip) {
            cbprt.setChecked(true);
        } else {
            cbprt.setChecked(false);
        }

        Global.PrintDishID = (new Boolean(prefs.getBoolean("printdishid", true)));
        CheckBox cbprdid = (CheckBox) findViewById(R.id.printDishID);
        if (Global.PrintDishID) {
            cbprdid.setChecked(true);
        } else {
            cbprdid.setChecked(false);
        }
	
	/*
    Global.PrintedAllowDelete = (new Boolean(prefs.getBoolean("printedallowdelete", false)));
	CheckBox cbpad = (CheckBox) findViewById(R.id.printedAllowDelete);
	if (Global.PrintedAllowDelete) {
		cbpad.setChecked(true);
	} else {
		cbpad.setChecked(false);
	}
	
    Global.PrintedAllowClear = (new Boolean(prefs.getBoolean("printedallowclear", true)));
	CheckBox cbpac = (CheckBox) findViewById(R.id.printedAllowClear);
	if (Global.PrintedAllowClear) {
		cbpac.setChecked(true);
	} else {
		cbpac.setChecked(false);
	}
	*/

        Global.P2FilterCats = (new String(prefs.getString("p2filtercats", "")));
        Button fc2 = (Button) findViewById(R.id.butP2FilterCats);
        fc2.setText(Global.P2FilterCats);

        Global.P3FilterCats = (new String(prefs.getString("p3filtercats", "")));
        Button fc3 = (Button) findViewById(R.id.butP3FilterCats);
        fc3.setText(Global.P3FilterCats);


        // set the selected filter for P2
        String[] eCat = Global.P2FilterCats.split(",");
        selectedP2Cat.clear();
        for (int i = 0; i < eCat.length; i++) {
            selectedP2Cat.add(eCat[i]);
        }

        eCat = Global.P3FilterCats.split(",");
        selectedP3Cat.clear();
        for (int i = 0; i < eCat.length; i++) {
            selectedP3Cat.add(eCat[i]);
        }

        Global.POS1Ip = (new String(prefs.getString("pos1ip", "192.168.1.30")));
        EditText ip1 = (EditText) findViewById(R.id.ip1);
        ip1.setText(Global.POS1Ip);
        Global.POS2Ip = (new String(prefs.getString("pos2ip", "192.168.1.31")));
        EditText ip2 = (EditText) findViewById(R.id.ip2);
        ip2.setText(Global.POS2Ip);
        Global.POS3Ip = (new String(prefs.getString("pos3ip", "192.168.1.32")));
        EditText ip3 = (EditText) findViewById(R.id.ip3);
        ip3.setText(Global.POS3Ip);

        Global.POSIp = (new String(prefs.getString("posmasterip", "192.168.1.71")));
        EditText ip4 = (EditText) findViewById(R.id.ipmd);
        ip4.setText(Global.POSIp);

        Global.ServerIP = (new String(prefs.getString("serverip", "")));
        EditText ip5 = (EditText) findViewById(R.id.serverIP);
        ip5.setText(Global.ServerIP);

        Global.TOSendOrderMode = (new Integer(prefs.getInt("tosendordermode", 1)));
        EditText som = (EditText) findViewById(R.id.sendOrderMode);
        som.setText(String.valueOf(Global.TOSendOrderMode));

        Global.Printer1Copy = (new Integer(prefs.getInt("printer1copy", 1)));
        EditText p1c = (EditText) findViewById(R.id.etP1C);
        p1c.setText(String.valueOf(Global.Printer1Copy));

        TextView tv0 = (TextView) findViewById(R.id.AboutAppName);
        Map<String, String> map0 = new LinkedHashMap<String, String>();
        map0.put(getString(R.string.msg_about_app_name), Global.AppName);
        populateField(map0, tv0);

        TextView tv1 = (TextView) findViewById(R.id.AboutVersion);
        Map<String, String> map1 = new LinkedHashMap<String, String>();
        map1.put(getString(R.string.msg_about_version_name), getVersionName());
        populateField(map1, tv1);

        TextView tv2 = (TextView) findViewById(R.id.AboutFileSource);
        Map<String, String> map2 = new LinkedHashMap<String, String>();
        map2.put(getString(R.string.msg_about_filesource), Global.FileSource);
        populateField(map2, tv2);

        TextView tv3 = (TextView) findViewById(R.id.AboutServerIP);
        Map<String, String> map3 = new LinkedHashMap<String, String>();
        map3.put(getString(R.string.msg_about_cloudIP), Global.ServerIP);
        populateField(map3, tv3);

        TextView tv5 = (TextView) findViewById(R.id.AboutSMID);
        Map<String, String> map5 = new LinkedHashMap<String, String>();
        map5.put(getString(R.string.msg_about_smartmenuid), Global.SMID);
        populateField(map5, tv5);

        TextView tv6 = (TextView) findViewById(R.id.AboutMenuVersion);
        Map<String, String> map6 = new LinkedHashMap<String, String>();
        map6.put(getString(R.string.msg_about_menuver), Global.MenuVersion);
        populateField(map6, tv6);

        TextView tv7 = (TextView) findViewById(R.id.AboutDeviceId);
        Map<String, String> map7 = new LinkedHashMap<String, String>();
        map7.put(getString(R.string.msg_about_deviceid), Global.MasterDeviceId);
        populateField(map7, tv7);

        Button bDel = (Button) findViewById(R.id.butDeleteP);
        bDel.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                TextView txt = (TextView) findViewById(R.id.textLoad);
                txt.setText("Deleting Pictures ...");
                dir = new File(android.os.Environment.getExternalStorageDirectory(), "SmartMenuPics200");
                Utils.deleteDirectory(dir);
                Toast.makeText(SyncActivity.this, getString(R.string.cacheDeleted), Toast.LENGTH_SHORT).show();
                txt.setText("");
            }
        });

        Button bReload = (Button) findViewById(R.id.butReload);
        bReload.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                TextView txt = (TextView) findViewById(R.id.textLoad);
                txt.setText("Reloading files ...");
                // reload the files from the server
                final ProgressDialog pd = ProgressDialog.show(SyncActivity.this, "Reloading", "Loading files from the server...", true, false);
                new Thread(new Runnable() {
                    public void run() {
                        // see if we can ping the server first
                        try {
                            if (pingIP()) {
                                reloadTheFiles();
                                // success
                                mHandler.post(reloaded);
                            } else {
                                // failed to upload
                                mHandler.post(exceptionReload);
                            }
                        } catch (Exception e) {
                            // failed to upload
                            mHandler.post(exceptionReload);
                        }
                        pd.dismiss();
                    }
                }).start();
            }
        });

        Button bStartService = (Button) findViewById(R.id.butStartService);
        bStartService.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Start the Service
                SmartMenuService.actionStart(getApplicationContext());
            }
        });

        Button bStopService = (Button) findViewById(R.id.butStopService);
        bStopService.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Stop the Service
                SmartMenuService.actionStop(getApplicationContext());
            }
        });

        newCatList();

        p2CatButton = (Button) findViewById(R.id.butP2FilterCats);
        p2CatButton.setText(Global.P2FilterCats);
        p2CatButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showP2CatDialog();
            }
        });

        p3CatButton = (Button) findViewById(R.id.butP3FilterCats);
        p3CatButton.setText(Global.P3FilterCats);
        p3CatButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showP3CatDialog();
            }
        });


        Button bSave = (Button) findViewById(R.id.butSave);
        bSave.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                String sendserver = "7," + Utils.GetDateTime() + "," + Global.ServerName;
                activityLogger(sendserver);
                // Update the Prefs
                RadioButton rb1 = (RadioButton) findViewById(R.id.ctBut1);
                RadioButton rb2 = (RadioButton) findViewById(R.id.ctBut2);

                Editor prefEdit = prefs.edit();
                if (rb1.isChecked()) {
                    prefEdit.putBoolean("publiccloud", false);
                    prefEdit.commit();
                }
                if (rb2.isChecked()) {
                    prefEdit.putBoolean("publiccloud", true);
                    prefEdit.commit();
                }

                RadioButton p1rbpt1 = (RadioButton) findViewById(R.id.p1ptBut1);
                RadioButton p1rbpt2 = (RadioButton) findViewById(R.id.p1ptBut2);
                prefEdit = prefs.edit();
                if (p1rbpt1.isChecked()) {
                    prefEdit.putBoolean("printer1type", true);
                    prefEdit.commit();
                }
                if (p1rbpt2.isChecked()) {
                    prefEdit.putBoolean("printer1type", false);
                    prefEdit.commit();
                }

                RadioButton p2rbpt1 = (RadioButton) findViewById(R.id.p2ptBut1);
                RadioButton p2rbpt2 = (RadioButton) findViewById(R.id.p2ptBut2);
                prefEdit = prefs.edit();
                if (p2rbpt1.isChecked()) {
                    prefEdit.putBoolean("printer2type", true);
                    prefEdit.commit();
                }
                if (p2rbpt2.isChecked()) {
                    prefEdit.putBoolean("printer2type", false);
                    prefEdit.commit();
                }

                RadioButton p3rbpt1 = (RadioButton) findViewById(R.id.p3ptBut1);
                RadioButton p3rbpt2 = (RadioButton) findViewById(R.id.p3ptBut2);
                prefEdit = prefs.edit();
                if (p3rbpt1.isChecked()) {
                    prefEdit.putBoolean("printer3type", true);
                    prefEdit.commit();
                }
                if (p3rbpt2.isChecked()) {
                    prefEdit.putBoolean("printer3type", false);
                    prefEdit.commit();
                }

                RadioButton rbsi1 = (RadioButton) findViewById(R.id.siBut1);
                RadioButton rbsi2 = (RadioButton) findViewById(R.id.siBut2);
                RadioButton rbsi3 = (RadioButton) findViewById(R.id.siBut3);
                RadioButton rbsi4 = (RadioButton) findViewById(R.id.siBut4);
                RadioButton rbsi5 = (RadioButton) findViewById(R.id.siBut5);
                prefEdit = prefs.edit();
                if (rbsi1.isChecked()) {
                    prefEdit.putString("storeid", "1");
                    prefEdit.commit();
                }
                if (rbsi2.isChecked()) {
                    prefEdit.putString("storeid", "2");
                    prefEdit.commit();
                }
                if (rbsi3.isChecked()) {
                    prefEdit.putString("storeid", "3");
                    prefEdit.commit();
                }
                if (rbsi4.isChecked()) {
                    prefEdit.putString("storeid", "4");
                    prefEdit.commit();
                }
                if (rbsi5.isChecked()) {
                    prefEdit.putString("storeid", "5");
                    prefEdit.commit();
                }

                RadioButton rbami1 = (RadioButton) findViewById(R.id.amiBut1);
                RadioButton rbami2 = (RadioButton) findViewById(R.id.amiBut2);
                RadioButton rbami3 = (RadioButton) findViewById(R.id.amiBut3);
                RadioButton rbami4 = (RadioButton) findViewById(R.id.amiBut4);
                RadioButton rbami5 = (RadioButton) findViewById(R.id.amiBut5);
                if (rbami1.isChecked()) {
                    prefEdit.putString("activemenuid", "1");
                    prefEdit.commit();
                }
                if (rbami2.isChecked()) {
                    prefEdit.putString("activemenuid", "2");
                    prefEdit.commit();
                }
                if (rbami3.isChecked()) {
                    prefEdit.putString("activemenuid", "3");
                    prefEdit.commit();
                }
                if (rbami4.isChecked()) {
                    prefEdit.putString("activemenuid", "4");
                    prefEdit.commit();
                }
                if (rbami5.isChecked()) {
                    prefEdit.putString("activemenuid", "5");
                    prefEdit.commit();
                }

                CheckBox cb4 = (CheckBox) findViewById(R.id.checkEng);
                CheckBox cb5 = (CheckBox) findViewById(R.id.checkPaper);
                CheckBox cb6 = (CheckBox) findViewById(R.id.checkWifi);
                CheckBox cb7 = (CheckBox) findViewById(R.id.showPics);
                CheckBox cb9 = (CheckBox) findViewById(R.id.autoMenuReload);

                CheckBox cb19 = (CheckBox) findViewById(R.id.pos1logo);
                CheckBox cb20 = (CheckBox) findViewById(R.id.pos1enable);
                CheckBox cb21 = (CheckBox) findViewById(R.id.pos2enable);
                CheckBox cb22 = (CheckBox) findViewById(R.id.pos3enable);
                CheckBox cb23 = (CheckBox) findViewById(R.id.p2KitchenCodes);
                CheckBox cb24 = (CheckBox) findViewById(R.id.p3KitchenCodes);
                CheckBox cbp1pst = (CheckBox) findViewById(R.id.p1PrintSentTime);
                CheckBox cbp2pst = (CheckBox) findViewById(R.id.p2PrintSentTime);
                CheckBox cbp3pst = (CheckBox) findViewById(R.id.p3PrintSentTime);
                CheckBox cbaod = (CheckBox) findViewById(R.id.autoOpenDrawer);
                CheckBox cbprt = (CheckBox) findViewById(R.id.printRoundTrip);
                CheckBox cbpad = (CheckBox) findViewById(R.id.printedAllowDelete);
                CheckBox cbpac = (CheckBox) findViewById(R.id.printedAllowClear);
                CheckBox cbprdid = (CheckBox) findViewById(R.id.printDishID);

                prefEdit = prefs.edit();
                if (cb4.isChecked()) {
                    prefEdit.putBoolean("startenglish", true);
                    prefEdit.commit();
                } else {
                    prefEdit.putBoolean("startenglish", false);
                    prefEdit.commit();
                }
                if (cb5.isChecked()) {
                    prefEdit.putBoolean("setwallpaper", true);
                    prefEdit.commit();
                } else {
                    prefEdit.putBoolean("setwallpaper", false);
                    prefEdit.commit();
                }
                if (cb6.isChecked()) {
                    prefEdit.putBoolean("checkwifi", true);
                    prefEdit.commit();
                } else {
                    prefEdit.putBoolean("checkwifi", false);
                    prefEdit.commit();
                }
                if (cb7.isChecked()) {
                    prefEdit.putBoolean("showpics", true);
                    prefEdit.commit();
                } else {
                    prefEdit.putBoolean("showpics", false);
                    prefEdit.commit();
                }
                if (cb9.isChecked()) {
                    prefEdit.putBoolean("automenureload", true);
                    prefEdit.commit();
                } else {
                    prefEdit.putBoolean("automenureload", false);
                    prefEdit.commit();
                }
                if (cb19.isChecked()) {
                    Global.POS1Logo = true;
                    prefEdit.putBoolean("pos1logo", true);
                    prefEdit.commit();
                } else {
                    Global.POS1Logo = false;
                    prefEdit.putBoolean("pos1logo", false);
                    prefEdit.commit();
                }
                if (cb20.isChecked()) {
                    Global.POS1Enable = true;
                    prefEdit.putBoolean("pos1enable", true);
                    prefEdit.commit();
                } else {
                    Global.POS1Enable = false;
                    prefEdit.putBoolean("pos1enable", false);
                    prefEdit.commit();
                }
                if (cb21.isChecked()) {
                    Global.POS2Enable = true;
                    prefEdit.putBoolean("pos2enable", true);
                    prefEdit.commit();
                } else {
                    Global.POS2Enable = false;
                    prefEdit.putBoolean("pos2enable", false);
                    prefEdit.commit();
                }
                if (cb22.isChecked()) {
                    Global.POS3Enable = true;
                    prefEdit.putBoolean("pos3enable", true);
                    prefEdit.commit();
                } else {
                    Global.POS3Enable = false;
                    prefEdit.putBoolean("pos3enable", false);
                    prefEdit.commit();
                }
                if (cb23.isChecked()) {
                    Global.P2KitchenCodes = true;
                    prefEdit.putBoolean("p2kitchencodes", true);
                    prefEdit.commit();
                } else {
                    Global.P2KitchenCodes = false;
                    prefEdit.putBoolean("p2kitchencodes", false);
                    prefEdit.commit();
                }
                if (cb24.isChecked()) {
                    Global.P3KitchenCodes = true;
                    prefEdit.putBoolean("p3kitchencodes", true);
                    prefEdit.commit();
                } else {
                    Global.P3KitchenCodes = false;
                    prefEdit.putBoolean("p3kitchencodes", false);
                    prefEdit.commit();
                }
                if (cbp1pst.isChecked()) {
                    Global.P1PrintSentTime = true;
                    prefEdit.putBoolean("p1printsenttime", true);
                    prefEdit.commit();
                } else {
                    Global.P1PrintSentTime = false;
                    prefEdit.putBoolean("p1printsenttime", false);
                    prefEdit.commit();
                }
                if (cbp2pst.isChecked()) {
                    Global.P2PrintSentTime = true;
                    prefEdit.putBoolean("p2printsenttime", true);
                    prefEdit.commit();
                } else {
                    Global.P2PrintSentTime = false;
                    prefEdit.putBoolean("p2printsenttime", false);
                    prefEdit.commit();
                }
                if (cbp3pst.isChecked()) {
                    Global.P3PrintSentTime = true;
                    prefEdit.putBoolean("p3printsenttime", true);
                    prefEdit.commit();
                } else {
                    Global.P3PrintSentTime = false;
                    prefEdit.putBoolean("p3printsenttime", false);
                    prefEdit.commit();
                }
                if (cbaod.isChecked()) {
                    Global.AutoOpenDrawer = true;
                    prefEdit.putBoolean("autoopendrawer", true);
                    prefEdit.commit();
                } else {
                    Global.AutoOpenDrawer = false;
                    prefEdit.putBoolean("autoopendrawer", false);
                    prefEdit.commit();
                }
                if (cbprt.isChecked()) {
                    Global.PrintRoundTrip = true;
                    prefEdit.putBoolean("printroundtrip", true);
                    prefEdit.commit();
                } else {
                    Global.PrintRoundTrip = false;
                    prefEdit.putBoolean("printroundtrip", false);
                    prefEdit.commit();
                }
                if (cbprdid.isChecked()) {
                    Global.PrintDishID = true;
                    prefEdit.putBoolean("printdishid", true);
                    prefEdit.commit();
                } else {
                    Global.PrintDishID = false;
                    prefEdit.putBoolean("printdishid", false);
                    prefEdit.commit();
                }
    		/*
    		if (cbpad.isChecked()) {
    			Global.PrintedAllowDelete = true;
    			prefEdit.putBoolean("printedallowdelete", true);
    			prefEdit.commit();
    		} else {
    			Global.PrintedAllowDelete = false;
    			prefEdit.putBoolean("printedallowdelete", false);
    			prefEdit.commit();
    		}
    		if (cbpac.isChecked()) {
    			Global.PrintedAllowClear = true;
    			prefEdit.putBoolean("printedallowclear", true);
    			prefEdit.commit();
    		} else {
    			Global.PrintedAllowClear = false;
    			prefEdit.putBoolean("printedallowclear", false);
    			prefEdit.commit();
    		}
    		*/
                Button fc2 = (Button) findViewById(R.id.butP2FilterCats);
                Global.P2FilterCats = fc2.getText().toString();
                prefEdit.putString("p2filtercats", Global.P2FilterCats);

                Button fc3 = (Button) findViewById(R.id.butP3FilterCats);
                Global.P3FilterCats = fc3.getText().toString();
                prefEdit.putString("p3filtercats", Global.P3FilterCats);

                EditText et = (EditText) findViewById(R.id.sendOrderMode);
                int tmp = Integer.parseInt(et.getText().toString());
                if (tmp < 1) tmp = 1;
                if (tmp > 2) tmp = 2;
                Global.TOSendOrderMode = tmp;
                prefEdit.putInt("tosendordermode", tmp);

                et = (EditText) findViewById(R.id.serverIP);
                Global.ServerIP = et.getText().toString();
                prefEdit.putString("serverip", Global.ServerIP);

                et = (EditText) findViewById(R.id.ip1);
                Global.POS1Ip = et.getText().toString();
                prefEdit.putString("pos1ip", Global.POS1Ip);

                et = (EditText) findViewById(R.id.ip2);
                Global.POS2Ip = et.getText().toString();
                prefEdit.putString("pos2ip", Global.POS2Ip);

                et = (EditText) findViewById(R.id.ip3);
                Global.POS3Ip = et.getText().toString();
                prefEdit.putString("pos3ip", Global.POS3Ip);

                et = (EditText) findViewById(R.id.ipmd);
                Global.POSIp = et.getText().toString();
                prefEdit.putString("posmasterip", Global.POSIp);

                et = (EditText) findViewById(R.id.serverIP);
                Global.ServerIP = et.getText().toString();
                prefEdit.putString("serverip", Global.ServerIP);

                et = (EditText) findViewById(R.id.etP1C);
                tmp = Integer.parseInt(et.getText().toString());
                if (tmp < 1) tmp = 1;
                if (tmp > 3) tmp = 3;
                Global.Printer1Copy = tmp;
                prefEdit.putInt("printer1copy", Global.Printer1Copy);

                prefEdit.commit();
            }
        });

    }

    protected void showP2CatDialog() {
        boolean[] checkedP2Cat = new boolean[catList.size()];
        int count = catList.size();
        for (int i = 0; i < count; i++) {
            checkedP2Cat[i] = selectedP2Cat.contains(catList.get(i));
        }
        DialogInterface.OnMultiChoiceClickListener p2DialogListener = new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                if (isChecked) selectedP2Cat.add(catList.get(which));
                else selectedP2Cat.remove(catList.get(which));
                StringBuilder stringBuilder = new StringBuilder();
                for (CharSequence ext : selectedP2Cat) {
                    // There is a '0 length' item at the beginning of the arraylist and I dont know why, so don't let it through...
                    if (ext.length() > 0) {
                        stringBuilder.append(ext.toString() + ",");
                    }
                }
                // Kill the last comma...
                String ss = stringBuilder.toString();
                if (ss.length() > 0) ss = ss.substring(0, ss.length() - 1);
                p2CatButton.setText(ss);
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Print Categories");
        // need the opts in a string [] to pass in multi
        String[] tmpArr = new String[catList.size()];
        for (int i = 0; i < catList.size(); i++) {
            tmpArr[i] = catList.get(i);
        }
        builder.setMultiChoiceItems(tmpArr, checkedP2Cat, p2DialogListener);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    protected void showP3CatDialog() {
        boolean[] checkedP3Cat = new boolean[catList.size()];
        int count = catList.size();
        for (int i = 0; i < count; i++)
            checkedP3Cat[i] = selectedP3Cat.contains(catList.get(i));
        DialogInterface.OnMultiChoiceClickListener p3DialogListener = new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                if (isChecked) selectedP3Cat.add(catList.get(which));
                else selectedP3Cat.remove(catList.get(which));
                StringBuilder stringBuilder = new StringBuilder();
                for (CharSequence ext : selectedP3Cat) {
                    // There is a '0 length' item at the beginning of the arraylist and I dont know why, so don't let it through...
                    if (ext.length() > 0) {
                        stringBuilder.append(ext.toString() + ",");
                    }
                }
                // remove last ","
                String ss = stringBuilder.toString();
                if (ss.length() > 0) ss = ss.substring(0, ss.length() - 1);
                p3CatButton.setText(ss);
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Print Categories");
        // need the opts in a string [] to pass in multi
        String[] tmpArr = new String[catList.size()];
        for (int i = 0; i < catList.size(); i++) {
            tmpArr[i] = catList.get(i);
        }
        builder.setMultiChoiceItems(tmpArr, checkedP3Cat, p3DialogListener);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void newCatList() {
        // get the extra list for the button
        catList.clear();
        String[] lines = Global.CATEGORYTXT.split("\\n");
        for (int i = 0; i < lines.length; i++) {
            int start = 0;
            int end = lines[i].indexOf("|");
            catList.add(lines[i].substring(start, end));
        }
    }

    private void updateConnectionStatus() {
        // update the wi-fi status
        ImageView img = (ImageView) findViewById(R.id.lit0a);
        img.setBackgroundResource(R.drawable.presence_invisible);
        if (checkInternetConnection()) {
            img.setBackgroundResource(R.drawable.presence_online);
        } else {
            img.setBackgroundResource(R.drawable.presence_busy);
        }

        // update the SERVER connection status
        img = (ImageView) findViewById(R.id.lit1aServer);
        img.setBackgroundResource(R.drawable.presence_invisible);
        // AWS doesnt seem to like ICMP so try a 204 check on the server
        //new pingFetch(Global.ServerIP, img).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        new ping204().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        // update the Printer1 status
        if (Global.POS1Enable) {
            img = (ImageView) findViewById(R.id.lit2a);
            img.setBackgroundResource(R.drawable.presence_invisible);
            new pingFetch(Global.POS1Ip, img).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        // update the Printer2 status
        if (Global.POS2Enable) {
            img = (ImageView) findViewById(R.id.lit3a);
            img.setBackgroundResource(R.drawable.presence_invisible);
            new pingFetch(Global.POS2Ip, img).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        // update the Printer3 status
        if (Global.POS3Enable) {
            img = (ImageView) findViewById(R.id.lit4a);
            img.setBackgroundResource(R.drawable.presence_invisible);
            new pingFetch(Global.POS3Ip, img).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        // update the Master status
        img = (ImageView) findViewById(R.id.litMaster);
        img.setBackgroundResource(R.drawable.presence_invisible);
        new pingFetch(Global.POSIp, img).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);


        // update the POS service status
		/*
		SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(this);
		if (prefs.getBoolean("isstarted", false)) {
			//if (isMyServiceRunning()) {
			img = (ImageView) findViewById(R.id.litPOSService); 
			img.setBackgroundResource(R.drawable.presence_online);
		} else {
			img = (ImageView) findViewById(R.id.litPOSService); 
			img.setBackgroundResource(R.drawable.presence_busy);	
		}
		*/
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
    private boolean pingIP() {
        String ip1 = "https://" + Global.ServerIP + Global.ServerReturn204;
        int status = -1;
        downloadSuccess = false;
        try {
            InputStream in = null;
            URL url = new URL(ip1);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setConnectTimeout(Global.ConnectTimeout);
            conn.setReadTimeout(Global.ReadTimeout);
            conn.setUseCaches(false);
            conn.setAllowUserInteraction(false);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("HEAD");
            in = conn.getInputStream();
            status = conn.getResponseCode();
            in.close();
            conn.disconnect();
            if (status == 204) {
                // reachable server
                downloadSuccess = true;
            } else {
                downloadSuccess = false;
            }
        } catch (Exception e) {
            downloadSuccess = false;
        }
        return downloadSuccess;
    }

    private void populateField(Map<String, String> values, TextView view) {
        StringBuilder sb = new StringBuilder();
        for (Entry<String, String> entry : values.entrySet()) {
            String fieldName = entry.getKey();
            String fieldValue = entry.getValue();
            sb.append(fieldName)
                    .append("<br /><b>").append(fieldValue).append("</b>");
        }
        view.setText(Html.fromHtml(sb.toString()));
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

        public ping204() {
            code = false;
        }

        protected void onPreExecute(Void... params) {
        }

        protected Integer doInBackground(Void... params) {
            try {
                String ip1 = "https://" + Global.ServerIP + Global.ServerReturn204;
                int status = -1;
                code = false;
                try {
                    InputStream in = null;
                    URL url = new URL(ip1);
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    conn.setConnectTimeout(Global.ConnectTimeout);
                    conn.setReadTimeout(Global.ReadTimeout);
                    conn.setUseCaches(false);
                    conn.setAllowUserInteraction(false);
                    conn.setInstanceFollowRedirects(false);
                    conn.setRequestMethod("HEAD");
                    in = conn.getInputStream();
                    status = conn.getResponseCode();
                    in.close();
                    conn.disconnect();
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
                img = (ImageView) findViewById(R.id.lit1aServer);
                img.setBackgroundResource(R.drawable.presence_online);
            } else {
                img = (ImageView) findViewById(R.id.lit1aServer);
                img.setBackgroundResource(R.drawable.presence_busy);
            }
        }
    }

    private void reloadTheFiles() {
        if (pingIP()) {
            // blow away the existing local files
            textDir = new File(android.os.Environment.getExternalStorageDirectory(), "SmartMenuFiles");
            Utils.deleteDirectory(textDir);
            textDir.mkdirs();

            //setup file source path+name
            filesSourceName[0] = "https://" + Global.ServerIP + "/" + Global.SMID + Global.ActiveMenuID + "/" + "menufile.txt";
            filesSourceName[1] = "https://" + Global.ServerIP + "/" + Global.SMID + Global.ActiveMenuID + "/" + "category.txt";
            filesSourceName[2] = "https://" + Global.ServerIP + "/" + Global.SMID + Global.ActiveMenuID + "/" + "kitchen.txt";
            filesSourceName[3] = "https://" + Global.ServerIP + "/" + Global.SMID + Global.StoreID + "/" + "settings.txt";
            filesSourceName[4] = "https://" + Global.ServerIP + "/" + Global.SMID + Global.ActiveMenuID + "/" + "options.txt";
            filesSourceName[5] = "https://" + Global.ServerIP + "/" + Global.SMID + Global.ActiveMenuID + "/" + "extras.txt";

            HttpDownload2();
        } else {
            TextView txt = (TextView) findViewById(R.id.textLoad);
            txt.setText("Reload Failed.");
        }
    }

    private void HttpDownload2() {
        try {
            Global.FileSource = "Public Cloud";
            downloadSuccess = true;
            //Loop through the downloads here
            for (int i = 0; i < filesSourceName.length; i++) {
                filesText[i] = Utils.DownloadText(filesSourceName[i]);
                if (filesText[i].length() > 0) {
                    filesText[i] = Utils.removeBOMchar(filesText[i]);
                    filesText[i] = Utils.removeCommentLines(filesText[i]);
                    if (i == 0) {
                        filesText[i] = Utils.removeUnAvailable(filesText[i]);
                    }
                    writeOutFile(filesText[i], filesLocalName[i]);
                } else {
                    mHandler.post(exceptionReload);
                    break;
                }
            }
            Global.MENUTXT = filesText[0];
            Global.CATEGORYTXT = filesText[1];
            processMenu();
            Global.KITCHENTXT = filesText[2];
            Global.SETTINGSTXT = filesText[3];
            Global.Settings = new JSONArray(Global.SETTINGSTXT);
            Global.OPTIONSTXT = filesText[4];
            Global.EXTRASTXT = filesText[5];

            Global.SMID = jsonGetter(Global.Settings, "smid").toString();
            Global.MenuVersion = jsonGetter(Global.Settings, "menuversion").toString();
            serverMv = jsonGetter(Global.Settings, "menuversion").toString();
            prefs = PreferenceManager.getDefaultSharedPreferences(this);
            Editor prefEdit = prefs.edit();
            prefEdit.putString("menuversion", serverMv);
            prefEdit.commit();
            // Reload the JSON settings we need
            loadJSONSettings();
        } catch (Exception e) {
            mHandler.post(exceptionReload);
        }
    }

    private void processMenu() {
        // We have read in the Menu file
        String[] menuItem = Global.MENUTXT.split("\\n");
        String[] categoryItem = Global.CATEGORYTXT.split("\\n");
        Global.MenuMaxItems = menuItem.length;
        Global.NumCategory = categoryItem.length;
        Global.NumSpecials = 0;

        // Loop through each line
        for (int i = 0; i < menuItem.length; i++) {
            // parse each line into columns using the divider character "|"
            String[] menuColumns = menuItem[i].split("\\|");
            // if it is a special, then bump the counter
            if (menuColumns[1].equals("specials")) Global.NumSpecials++;
        }
    }

    public void loadJSONSettings() {
        // load some settings from JSON
        Global.CustomerName = jsonGetter(Global.Settings, "customername").toString();
        Global.CustomerNameBrief = jsonGetter(Global.Settings, "customernamebrief").toString();
        Global.StoreAddress1 = jsonGetter(Global.Settings, "storeaddress1").toString();
        Global.StoreAddress2 = jsonGetter(Global.Settings, "storeaddress2").toString();
        Global.StoreAddress3 = jsonGetter(Global.Settings, "storeaddress3").toString();
        Global.StoreAddress4 = jsonGetter(Global.Settings, "storeaddress4").toString();
        Global.StoreAddress5 = jsonGetter(Global.Settings, "storeaddress5").toString();

        Global.AdminPin = jsonGetter(Global.Settings, "adminpin").toString();
        //Global.ManagerPin = jsonGetter(Global.Settings,"managerpin").toString();

        // Build the userlist- a string array of JSON entries
        Global.userList.clear();
        try {
            String tmp = jsonGetter(Global.Settings, "userlist").toString();
            JSONArray JSONusers = new JSONArray(tmp);
            // Loop through each user and add it to the userlist string array
            for (int i = 0; i < JSONusers.length(); i++) {
                JSONArray ju = JSONusers.getJSONArray(i);
                Global.userList.add(i, ju.toString());
            }
        } catch (Exception e) {
        }

        // Build the modifiers list- a string array of JSON entries
        Global.modifiers.clear();
        try {
            String tmp = jsonGetter(Global.Settings, "modifiers").toString();
            JSONArray JSONmods = new JSONArray(tmp);
            // Loop through each modifier and add it to the modifier string array
            for (int i = 0; i < JSONmods.length(); i++) {
                JSONArray ju = JSONmods.getJSONArray(i);
                Global.modifiers.add(i, ju.toString());
                //log("mod i=" + i + " json=" + ju.toString());
            }
        } catch (Exception e) {
        }

        // Build the Welcome ArrayList
        // Note that the Welcome info is only actually used in the ORDER app
        Global.welcome.clear();
        try {
            String tmp = jsonGetter(Global.Settings, "welcome").toString();
            JSONArray JSONwelc = new JSONArray(tmp);
            // Loop through each modifier and add it to the modifier string array
            for (int i = 0; i < JSONwelc.length(); i++) {
                JSONArray ju = JSONwelc.getJSONArray(i);
                Global.welcome.add(i, ju.toString());
            }
        } catch (Exception e) {
        }

        // Build the Table Names ArrayList
        Global.tablenames.clear();
        try {
            String tmp = jsonGetter(Global.Settings, "tablenames").toString();
            JSONArray JSONtabn = new JSONArray(tmp);
            // Loop through each modifier and add it to the modifier string array
            for (int i = 0; i < JSONtabn.length(); i++) {
                JSONArray ju = JSONtabn.getJSONArray(i);
                Global.tablenames.add(i, ju.toString());
            }
        } catch (Exception e) {
        }

        // Build the saletpyes- a string array of JSON entries
        Global.saletypes.clear();
        try {
            String tmp = jsonGetter(Global.Settings, "saletypes").toString();
            JSONArray JSONsaletypes = new JSONArray(tmp);
            // Loop through each user and add it to the saletype string array
            for (int i = 0; i < JSONsaletypes.length(); i++) {
                JSONArray ju = JSONsaletypes.getJSONArray(i);
                Global.saletypes.add(i, ju.toString());
            }
        } catch (Exception e) {
        }
    }

    private void createAndRunStatusThread(final Activity act) {
        m_bStatusThreadStop = false;
        m_statusThread = new Thread(new Runnable() {
            public void run() {
                while (m_bStatusThreadStop == false) {
                    try {
                        //anything touching the GUI has to run on the Ui thread
                        act.runOnUiThread(new Runnable() {
                            public void run() {
                                updateConnectionStatus();
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

    private void failedAuth0() {
        AlertDialog alertDialog = new AlertDialog.Builder(SyncActivity.this).create();
        alertDialog.setTitle("Connection");
        alertDialog.setIcon(android.R.drawable.stat_sys_warning);
        alertDialog.setMessage("Data connection not available. Files cannot be reloaded.");
        alertDialog.setCancelable(false);
        alertDialog.setButton("Back", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alertDialog.show();
    }

    private void failedAuth2() {
        AlertDialog alertDialog = new AlertDialog.Builder(SyncActivity.this).create();
        alertDialog.setTitle("Uploading");
        alertDialog.setIcon(android.R.drawable.stat_sys_warning);
        alertDialog.setMessage("Uploading not successful. Please try again.");
        alertDialog.setCancelable(false);
        alertDialog.setButton("Back", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alertDialog.show();
    }

    private void failedAuth3() {
        AlertDialog alertDialog = new AlertDialog.Builder(SyncActivity.this).create();
        alertDialog.setTitle("Reloading");
        alertDialog.setIcon(android.R.drawable.stat_sys_warning);
        alertDialog.setMessage("Reload not successful. Please try again.");
        alertDialog.setCancelable(false);
        alertDialog.setButton("Back", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        alertDialog.show();
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
                if ((checkInternetConnection()) & (Global.TicketToCloud)) {
                    String postURL = "https://" + Global.ServerIP + Global.PosSaveActivityURL;
                    Utils.SendMultipartAdhoc(postURL,
                            sendServer,
                            Global.SMID,
                            Global.StoreID);
                } else {
                    // add it to the retry directory
                    String fname = "retry-activity-" + Utils.GetDateTime() + ".txt";
                    File writeFile = new File(logsDir, fname);
                    FileWriter writer;
                    try {
                        writer = new FileWriter(writeFile, true);
                        writer.write(sendServer);
                        writer.flush();
                        writer.close();
                    } catch (IOException e) {
                    }
                }
            }
        }).start();
    }

    public void onBackPressed() {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, menu.NONE, "Exit");
        MenuItem item0 = menu.getItem(0);
        item0.setIcon(null);
        item0.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        item0.setTitle("Exit    ");
        return (super.onCreateOptionsMenu(menu));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 0) {
            finish();
            return (true);
        }
        return (super.onOptionsItemSelected(item));
    }

    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.smartmenu.android.stafforder.SmartMenuService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
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

    private void writeOutFile(String fcontent, String fname) {
        File writeFile = new File(textDir, fname);
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(writeFile, false), "UTF-8"));
            writer.write(fcontent);
            writer.flush();
            writer.close();
        } catch (IOException e) {
        }
    }

}