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

import android.content.Context;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ConnectionLogService {
    private String mPath;
    private Writer mWriter;
    private static final SimpleDateFormat TIMESTAMP_FMT = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss] ");

    public ConnectionLogService(Context context) throws IOException {
        // Set the directory to save text files
        File logDir = context.getExternalFilesDir("SmartMenuLogs");
        if (!logDir.exists()) {
            logDir.mkdirs();
            // do not allow media scan
            new File(logDir, ".nomedia").createNewFile();
        }
        open(logDir.getAbsolutePath() + "/log");
    }

    public ConnectionLogService(String basePath) throws IOException {
        open(basePath);
    }

    protected void open(String basePath) throws IOException {
        File f = new File(basePath + "-" + getTodayString());
        mPath = f.getAbsolutePath();
        mWriter = new BufferedWriter(new FileWriter(mPath, true), 2048);
    }

    private static String getTodayString() {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd-hhmmss");
        return "smartmenu-service.txt";
    }

    public String getPath() {
        return mPath;
    }

    public void println(String message) throws IOException {
        mWriter.write(TIMESTAMP_FMT.format(new Date()));
        mWriter.write(message);
        mWriter.write('\n');
        mWriter.flush();
    }

    public void close() throws IOException {
        mWriter.close();
    }
}