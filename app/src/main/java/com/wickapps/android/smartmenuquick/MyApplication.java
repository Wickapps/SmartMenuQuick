/*
 * Copyright (C) 2019 Mark Wickham
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wickapps.android.smartmenuquick;

import android.app.Application;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

@ReportsCrashes(formKey = "",

        customReportContent = {ReportField.REPORT_ID,
                ReportField.APP_VERSION_CODE,
                ReportField.APP_VERSION_NAME,
                ReportField.PACKAGE_NAME,
                ReportField.PHONE_MODEL,
                ReportField.ANDROID_VERSION,
                ReportField.STACK_TRACE,
                ReportField.TOTAL_MEM_SIZE,
                ReportField.AVAILABLE_MEM_SIZE,
                ReportField.USER_APP_START_DATE,
                ReportField.USER_CRASH_DATE,
                ReportField.LOGCAT,
                ReportField.SHARED_PREFERENCES},

        formUri = "http://order.lilysbeijing.com/phpcommon/crashed1118.php",
        httpMethod = org.acra.sender.HttpSender.Method.POST,
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.msg_crash_text,
        resDialogText = R.string.msg_crash_text,
        resDialogIcon = android.R.drawable.ic_dialog_info,
        resDialogTitle = R.string.msg_crash_title)

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        ACRA.init(this);
        super.onCreate();
    }
}