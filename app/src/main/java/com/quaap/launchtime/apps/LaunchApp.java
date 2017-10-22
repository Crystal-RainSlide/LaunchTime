package com.quaap.launchtime.apps;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.quaap.launchtime.GlobState;
import com.quaap.launchtime.db.DB;

import java.util.List;

/**
 * Copyright (C) 2017   Tom Kliethermes
 *
 * This file is part of LaunchTime and is is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

public class LaunchApp {


    private static String TAG = "LT LaunchApp";

    private Activity activity;

    public LaunchApp(Activity activity) {
        this.activity = activity;
    }

    private DB db() {
        return GlobState.getGlobState(activity).getDB();
    }
    //Run/open the thing that was clicked
    public void launchApp(String activityname, String pkgname) {
        try {
            launchApp(db().getApp(new ComponentName(pkgname, activityname)));
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public void launchApp(final AppLauncher app) {
        String activityname = app.getLinkBaseActivityName();

        try {

            //needed to place in the open apps list
            Intent intent = getAppIntent(app);

            Log.d(TAG, "Launching " + app.getComponentName());
            if (isValidActivity(intent)) {
                // actually start it
                activity.startActivity(intent);
            } else {
                Toast.makeText(activity, "Could not launch item", Toast.LENGTH_LONG).show();
            }

            //log the launch
            if (app.isAppLink()) {
                db().appLaunched(new ComponentName(app.getPackageName(), app.getLinkBaseActivityName()));
            } else {
                db().appLaunched(app.getComponentName());
            }
        } catch (Exception e) {
            Log.d(TAG, "Could not launch " + activityname, e);
            Toast.makeText(activity, "Could not launch item: " + e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
        }
        //showButtonBar(false, true);
    }

    public static Intent getAppIntent(final AppLauncher app) {
        String activityname = app.getLinkBaseActivityName();
        String packagename = app.getPackageName();

        String uristr = app.getLinkUri();
        Uri uri = null;
        Intent intent;
        if (uristr != null && !uristr.equals("")) {
            uri = Uri.parse(uristr);
        }

        if (app.isActionLink()) {
            //Change "CALL" to "DIAL" so we can avoid needing the
            // android.permission.CALL_PHONE permission
            if (activityname.startsWith("android.intent.action.CALL")) {
                activityname = "android.intent.action.DIAL";
            }

            if (uri==null) {
                intent = new Intent(activityname);
            } else {
                intent = new Intent(activityname, uri);
            }
        } else {
            intent = new Intent();
            intent.setClassName(packagename, activityname);
            if (uri!=null) {
                intent.setAction(Intent.ACTION_VIEW);
                intent.setData(uri);
            } else {
                intent.setAction(Intent.ACTION_MAIN);
            }
        }
       // Log.d("launch", activityname + "  " + uristr);
        //needed to place in the open apps list
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return intent;

    }

    public boolean isValidActivity(AppLauncher app) {
        return isValidActivity(getAppIntent(app));
    }

    public boolean isValidActivity(Intent intent) {
        List<ResolveInfo> list = activity.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

}
