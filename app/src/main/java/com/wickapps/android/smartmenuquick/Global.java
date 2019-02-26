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
    // SETTINGS -----------------------------------------------------------------------------------------
    public static String PosSaveActivityURL = "/phpcommon/pos-saveactivity.php";
    public static String PosSaveOrderJsonURL = "/phpcommon/saveorder1118.php";
    public static String ServerReturn204 = "/phpcommon/return204.php";
    public static String Uploader = "/phpcommon/uploadfile0413.php";

    public static String ProtocolPrefix = "http://";  // Default to non SSL
    public static String ServerIP = "";
    public static String ServerIPHint = "order.lilysbeijing.com";
    public static String SMID = "";
    public static Boolean CheckAvailability = false;

    public static String AppName = "SmartMenu Quick";
    public static String AppNameA = "SmartMenu";
    public static String AppNameB = "Quick";

    public static String CustomerName = "";
    public static String CustomerNameBrief = "";
    public static String StoreAddress = "";

    public static JSONArray Settings = null;

    public static Boolean PayTypeEnabled = true;

    public static Boolean PausedOrder = false;
    public static Boolean LoggedIn = false;

    public static Boolean TicketToCloud = true; // true if we want to use HTTP to submit the ticket to public cloud
    public static Boolean PublicCloud = true; // true = public , false = private
    public static Boolean AutoMenuReload = false; // loaded from pref

    public static String ServerName = "";

    public static String MasterDeviceId = "";
    public static String POSIp = "";
    public static int POSSocket = 8080;

    public static int TOSendOrderMode = 1; // 1=direct print 2=POS

    public static ArrayList<String> userList = new ArrayList<String>();
    public static ArrayList<String> topicList = new ArrayList<String>();
    public static ArrayList<String> modifiers = new ArrayList<String>();
    public static ArrayList<String> saletypes = new ArrayList<String>();
    public static ArrayList<String> tablenames = new ArrayList<String>();

    public static String CheckedPicName = "";
    public static int CheckedPicID = 0;

    public static String FileSource = ""; // Info string for source files: PUBLIC CLOUD or PRIVATE CLOUD or LOCAL

    public static Boolean Printer1Type = null; // true = epson, false = GPrinter
    public static Boolean Printer2Type = null;
    public static Boolean Printer3Type = null;
    public static Boolean StartEnglish = null;
    public static Boolean POS1Logo = null; // Print .bmp logo on ticket?
    public static Boolean POS1Enable = null; // Do we have POS printers
    public static Boolean POS2Enable = null;
    public static Boolean POS3Enable = null;
    public static Boolean P2KitchenCodes = null;
    public static Boolean P3KitchenCodes = null;
    public static String POS1Ip = null; // printer IP Addresses
    public static String POS2Ip = null;
    public static String POS3Ip = null;
    public static Boolean PrintRoundTrip = null;
    public static Boolean PrintDishID = null;

    public static String P2FilterCats = null;
    public static String P3FilterCats = null;

    public static Boolean P1PrintSentTime = null;
    public static Boolean P2PrintSentTime = null;
    public static Boolean P3PrintSentTime = null;

    public static int Printer1Copy;
    public static Integer Printer1CopyTakeOut;
    public static Boolean AutoOpenDrawer = null;

    public static int TicketCharWidth = 42;
    public static int KitcTicketCharWidth = 21; // Print Double Wide on P2/P3

    public static String AdminPin = "";
    public static int UserLevel = 2; // 0=staff, 1=manager, 2=admin


    public static int ConnectTimeout = 15000;
    public static int ReadTimeout = 15000;
    public static int MaxBuffer = 25000;

    public static int SocketRetry = 1;
    public static int SocketRetrySleep = 500;

    public static Boolean GuaranteeDelivery = false; // MQTT Clean Session setting

    public static Boolean EnglishLang = true; // keeps track of current language state

    public static String MenuVersion = ""; // menu version

    public static String MENUTXT = "menu text will download into here";
    public static String CATEGORYTXT = "category text will download into here";
    public static String KITCHENTXT = "kitchen codes will download into here";
    public static String SETTINGSTXT = "settings will download into here";
    public static String OPTIONSTXT = "dish options will download into here";
    public static String EXTRASTXT = "dish extras will download into here";

    public static String TodayDate = "";

    public static int NumSpecials = 0; // This will hold the number of specials
    public static int MenuMaxItems = 0; // This will hold the number of menu items
    public static int NumCategory = 0; // This will hold the number of cats

    public static String LoginTime = "";
    public static String LogoutTime = "";

    public static Boolean QuickOpen = false; // true to accept incoming orders
}