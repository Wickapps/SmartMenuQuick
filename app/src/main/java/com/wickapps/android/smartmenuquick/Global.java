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

import org.json.JSONArray;

import java.util.ArrayList;

public class Global {
    // SERVER SETTINGS -----------------------------------------------------------------------------------------
    public static String PosSaveOrderJsonURL = "/phpcommon/pos-saveorder-json.php";
    public static String PosSaveAdHocURL = "/phpcommon/pos-saveadhoc.php";
    public static String PosSaveActivityURL = "/phpcommon/pos-saveactivity.php";
    public static String ServerReturn204 = "/return204.php";
    public static String PosShowDailyURL = "/phpcommon/pos-showdaily-json.php";

    // The following 4 settings are needed for initial boot
    public static String ServerIP = "";
    public static String SMID = "";
    public static String StoreID = "";
    public static String ActiveMenuID = "";

    public static String AppName = "SmartMenu Quick";
    // END OF SERVER SETTINGS ---------------------------------------------------------------------------------

    // CUSTOMER SPECIFC SETTINGS ------------------------------------------------------------------------------
    //
    public static String CustomerName = "";
    public static String CustomerNameBrief = "";
    //
    public static String StoreAddress1 = "";
    public static String StoreAddress2 = "";
    public static String StoreAddress3 = "";
    public static String StoreAddress4 = "";
    public static String StoreAddress5 = "";
    //
    // END OF CUSTOMER SPECIFIC SETTINGS ----------------------------------------------------------------------

    public static JSONArray Settings = null;

    public static Boolean PayTypeEnabled = true;

    public static Boolean PausedOrder = false;
    public static Boolean LoggedIn = false;

    public static Boolean TicketToCloud = true;    // true if we want to use HTTP to submit the ticket to public cloud
    public static Boolean PublicCloud = true;    // true = public , false = private
    public static Boolean AutoMenuReload = false;    // loaded from pref

    public static String ServerName = "";

    public static String MasterDeviceId = "";
    public static String POSIp = "";
    public static int POSSocket = 8080;

    public static int TOSendOrderMode = 1;        // 1=direct print 2=POS

    public static ArrayList<String> userList = new ArrayList<String>();
    public static ArrayList<String> topicList = new ArrayList<String>();
    public static ArrayList<String> modifiers = new ArrayList<String>();
    public static ArrayList<String> saletypes = new ArrayList<String>();
    public static ArrayList<String> welcome = new ArrayList<String>();
    public static ArrayList<String> tablenames = new ArrayList<String>();

    public static String CheckedPicName = "";
    public static int CheckedPicID = 0;

    public static String FileSource = "";    // Info string for source files: PUBLIC CLOUD or PRIVATE CLOUD or LOCAL

    public static Boolean Printer1Type = null;    // loaded from pref, true = epson, false = GPrinter
    public static Boolean Printer2Type = null;    // loaded from pref
    public static Boolean Printer3Type = null;    // loaded from pref
    public static Boolean StartEnglish = null;    // loaded from pref
    public static Boolean SetWallpaper = null;    // loaded from pref
    public static Boolean CheckWifi = null;    // loaded from pref
    public static Boolean ShowPics = null;    // loaded from pref
    public static Boolean AutoTakeOutTable = null;    // loaded from pref
    public static Boolean POS1Logo = null;    // Print .bmp logo on ticket?
    public static Boolean POS1Enable = null;    // Do we have POS printers
    public static Boolean POS2Enable = null;
    public static Boolean POS3Enable = null;
    public static Boolean P2KitchenCodes = null;
    public static Boolean P3KitchenCodes = null;
    public static String POS1Ip = null;    // printer IP Addresses
    public static String POS2Ip = null;
    public static String POS3Ip = null;
    public static Boolean PrintRoundTrip = null;
    public static Boolean PrintDishID = null;

    public static String P2FilterCats = null;
    public static String P3FilterCats = null;

    public static Boolean P1PrintSentTime = null;    // loaded from pref
    public static Boolean P2PrintSentTime = null;    // loaded from pref
    public static Boolean P3PrintSentTime = null;    // loaded from pref

    public static int Printer1Copy;                    // loaded from pref
    public static Boolean AutoOpenDrawer = null;    // loaded from pref

    public static int TicketCharWidth = 42;
    public static int KitcTicketCharWidth = 21;        // Print Double Wide on P2/P3

    public static String AdminPin = "";
    public static int UserLevel = 2;        // 0=staff, 1=manager, 2=admin

    public static int ConnectTimeout = 6000;
    public static int ReadTimeout = 6000;
    public static int MaxBuffer = 25000;

    public static int SocketRetry = 1;
    public static int SocketRetrySleep = 500;

    public static Boolean GuaranteeDelivery = false;    // MQTT Clean Session setting

    public static Boolean EnglishLang = true;    // keeps track of current language state

    public static String MenuVersion = "";        // menu version

    public static String MENUTXT = "menu text will download into here";
    public static String CATEGORYTXT = "category text will download into here";
    public static String KITCHENTXT = "kitchen codes will download into here";
    public static String SETTINGSTXT = "settings will download into here";
    public static String OPTIONSTXT = "dish options will download into here";
    public static String EXTRASTXT = "dish extras will download into here";
    public static String PICLISTTXT = "list of pics from the server will download into here";

    public static String TodayDate = "";

    public static int NumSpecials = 0;        // This will hold the number of specials
    public static int MenuMaxItems = 0;        // This will hold the number of menu items
    public static int NumCategory = 0;        // This will hold the number of cats

    public static String LoginTime = "";
    public static String LogoutTime = "";

    public static Boolean QuickOpen = false;    // true to accept incoming orders
}