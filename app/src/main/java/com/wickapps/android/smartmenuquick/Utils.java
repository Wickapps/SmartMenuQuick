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
import android.util.DisplayMetrics;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Utils {

    public static String DownloadText(String link) throws Exception {
        InputStream in = null;
        String responseData = "";

        try {
            final URL url = new URL(link);

            OkHttpClient.Builder b = new OkHttpClient.Builder();
            b.readTimeout(Global.ReadTimeout, TimeUnit.MILLISECONDS);
            b.writeTimeout(Global.ReadTimeout, TimeUnit.MILLISECONDS);
            b.connectTimeout(Global.ConnectTimeout, TimeUnit.MILLISECONDS);
            final OkHttpClient client = b.build();

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            responseData = response.body().string();
        } catch (Exception e1) {
            throw new Exception("Unexpected code " + e1);
        }
        return responseData;
    }

    public static String ReadLocalFile(File fname) throws UnsupportedEncodingException {
        int BUFFER_SIZE = 2000;
        FileInputStream fstream = null;
        try {
            fstream = new FileInputStream(fname);
        } catch (Exception e1) {
            return "";
        }
        BufferedReader bufreader;
        try {
            bufreader = new BufferedReader(new InputStreamReader(fstream, "UTF-8"));
        } catch (Exception e) {
            return "";
        }
        int charRead;
        String str = "";
        char[] inputBuffer = new char[BUFFER_SIZE];
        try {
            while ((charRead = bufreader.read(inputBuffer)) > 0) {
                String readString = String.copyValueOf(inputBuffer, 0, charRead);
                str += readString;
                inputBuffer = new char[BUFFER_SIZE];
            }
            fstream.close();
        } catch (Exception e) {
            return "";
        }
        return str;
    }

    public static String GetDateTime() {
        Date dt = new Date();
        Integer hours = dt.getHours();
        String formathr = String.format("%02d", hours);
        Integer minutes = dt.getMinutes();
        String formatmin = String.format("%02d", minutes);
        Integer secs = dt.getSeconds();
        String formatsec = String.format("%02d", secs);
        Integer month = dt.getMonth() + 1;
        String formatmon = String.format("%02d", month);
        Integer day = dt.getDate();
        String formatdy = String.format("%02d", day);
        Integer yr = dt.getYear() - 100;    // the function returns year since 1900 so need to offset for 20xx
        String formatyr = String.format("%02d", yr);
        String curTime = formatyr + formatmon + formatdy + "-" + formathr + formatmin + formatsec;
        return curTime;
    }

    public static String FancyDate() {
        String[] daysofweek = new String[]{"Sun", "Mon", "Tue", "Wed", "Thur", "Fri", "Sat"};
        String[] months = new String[]{"Jan", "Feb", "Mar", "April", "May", "June", "July", "Aug", "Sept", "Oct", "Nov", "Dec"};
        String[] daysuffix = new String[]{"st", "nd", "rd", "th", "th", "th", "th", "th", "th", "th", "th", "th", "th", "th", "th", "th", "th", "th", "th", "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th", "th", "st"};
        Date dt = new Date();
        Integer dow = dt.getDay();
        String formatdow = daysofweek[dow];
        Integer month = dt.getMonth();
        String formatmon = months[month];
        Integer day = dt.getDate();
        String formatdy = String.format("%2d", day);
        String curDate = formatdow + ", " + formatmon + " " + formatdy + daysuffix[day - 1];
        return curDate;
    }

    public static String GetTime() {
        Date dt = new Date();
        Integer hours = dt.getHours();
        String formathr = String.format("%02d", hours);
        Integer minutes = dt.getMinutes();
        String formatmin = String.format("%02d", minutes);
        Integer seconds = dt.getSeconds();
        String formatsec = String.format("%02d", seconds);
        String curTime = formathr + formatmin + formatsec;
        return curTime;
    }

    public static String GetDate() {
        Date dt = new Date();
        Integer month = dt.getMonth() + 1;
        String formatmon = String.format("%02d", month);
        Integer day = dt.getDate();
        String formatdy = String.format("%02d", day);
        Integer yr = dt.getYear() - 100;    // the functions returns years since 1900, so offset to get 20xx
        String formatyr = String.format("%02d", yr);
        String curDate = formatyr + formatmon + formatdy;
        return curDate;
    }

    /**
     * Generate a random integer in the range [lowEnd...highEnd].
     *
     * @param highEnd the high end of the range of possible number
     * @return a random integer in [0...highEnd]
     */
    public static int randomInt(int highEnd) {
        int theNum;
        // Pick a random number in the range
        // then truncate it to an integer
        Random r = new Random();
        theNum = r.nextInt(highEnd + 1);
        return theNum;
    }

    public static int getFontSize(Activity activity) {
        DisplayMetrics dMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dMetrics);

        switch (dMetrics.densityDpi) {
            case DisplayMetrics.DENSITY_HIGH:
                // Lenovo
                return 16;
            case DisplayMetrics.DENSITY_MEDIUM:
                // Cube, Kindle, Archos
                return 18;
            case DisplayMetrics.DENSITY_LOW:
                // ICS Buy-Now Newest
                return 20;
        }
        // Unknown
        return 18;
    }

    public static int getWindowButtonHeight(Activity activity) {
        DisplayMetrics dMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dMetrics);
        // return a fraction of the screen height
        final float HIGH = activity.getResources().getDisplayMetrics().heightPixels;
        int valueHigh = (int) (HIGH / 11.0f);
        return valueHigh;
    }

    public static int getWindowTicketHeight(Activity activity) {
        DisplayMetrics dMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dMetrics);
        // return a fraction of the screen height
        final float HIGH = activity.getResources().getDisplayMetrics().heightPixels;
        int valueHigh = (int) (HIGH / 2.8f);
        return valueHigh;
    }

    public static int getWidth(Activity activity) {
        final float WIDE = activity.getResources().getDisplayMetrics().widthPixels;
        int valueWide = (int) (WIDE);
        return (valueWide);
    }

    public static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files == null) {
                return true;
            }
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory()) {
                        deleteDirectory(files[i]);
                    } else {
                        files[i].delete();
                    }
                }
            }
        }
        return (path.delete());
    }

    public static String filenameFromURL(String str) {
        // URL comes in such as: 			http://www.smart.com/files/name.jpg
        // string s gets returned as: 		name.jpg

        //   http://www.lilysamericandiner.com/fetch200/name.jpg
        //      .*//                        .*/      .*/ (1).jpg

        String s = str;
        String name = str;

        //s = s.replaceAll("[^\\p{L}\\p{N}.\\/\\-\\+\\!\\$,\\s]", "");

        Pattern p = Pattern.compile("(.*//)(.*/)(.*/)(.*.jpg)");
        Matcher m = p.matcher(s);

        if (m.find()) {
            name = m.group(4).trim();
        }

        return (name);
    }

    public static String removeBOMchar(String tmp) {
        char[] UTF16LE = {0xFF, 0xFE};
        char[] UTF8 = {0xEF, 0xBB, 0xBF};
        String sTemp = tmp;
        //sTemp.replaceAll("^\\xEF\\xBB\\xBF", "");
        sTemp = sTemp.replace("\uFEFF", "");
        // sTemp = sTemp.substring(1, sTemp.length());
        return sTemp;
    }

    // This routine will remove all the lines in the files that begin with "//"
    // This allows for easy updating of the menufile.txt and others
    public static String removeCommentLines(String tmp) {
        String sTemp = tmp;
        sTemp = sTemp.replaceAll("\\/\\/.*\\r\\n", "");
        return sTemp;
    }

    // This routine will remove all the lines in the files that begin with "1"
    // This allows for removal of unavailable dishes
    public static String removeUnAvailable(String tmp) {
        String sTemp = tmp;
        //sTemp = sTemp.replaceAll("^1.*\\r\\n", "");
        //sTemp = sTemp.replaceAll("(?m)^1.*$", "");
        //sTemp = sTemp.replaceAll("^1(.*)\\n", "");
        // (?m) does multiline, and we need the ^ to match the beginning of each line
        sTemp = sTemp.replaceAll("(?m)^1.....\\|.*\\r\\n", "");
        sTemp = sTemp.replaceAll("(?m)^1.....\\|.*\\n", "");
        return sTemp;
    }

    public static String removeDiacriticalMarks(String string) {
        return Normalizer.normalize(string, Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private static void writeOutFile(File fildir, String fname, String fcontent) {
        File writeFile = new File(fildir, fname);
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(writeFile, false), "UTF-8"));
            writer.write(fcontent);
            writer.flush();
            writer.close();
        } catch (Exception e) {
        }
    }
}