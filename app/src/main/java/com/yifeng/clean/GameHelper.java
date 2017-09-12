package com.yifeng.clean;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;

import java.io.File;

import static android.os.Environment.MEDIA_MOUNTED;

/**
 * Created by ywc on 17/3/20.
 */

public class GameHelper {

    public static Activity s_GameActivity = null;
    public static Context s_Context = null;

    public static void initGameHelper(Context paramContext){
        s_Context = paramContext.getApplicationContext();
        s_GameActivity = (Activity) paramContext;

        DeviceUuidFactory uuidFac = new DeviceUuidFactory(s_Context);
    }

    //get user id
    public static String getUserUUID()
    {
        String uuid = DeviceUuidFactory.uuid.toString();
        return uuid;
    }

    //go to google market
    public static void GotoMarket()
    {
        Uri uri = Uri.parse("market://details?id=" + s_Context.getPackageName());
        Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);

        try {
            s_GameActivity.startActivity(myAppLinkToMarket);
        } catch (ActivityNotFoundException e) {
            s_GameActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + s_Context.getPackageName())));
        }
    }

    //open a web site
    public static void GotoURL(String url)
    {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        Uri content_url = Uri.parse(url);
        intent.setData(content_url);
        s_GameActivity.startActivity(intent);
    }

    public static long getDeviceStorage()
    {
        long totalSize = getDataStorage() + getExternalStorage();
        return totalSize;
    }

    private static long getExternalStorage()
    {
        if (Environment.getExternalStorageState().equals(MEDIA_MOUNTED))
        {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSize();
            long availableBlocks = stat.getAvailableBlocks();
            return blockSize * availableBlocks;
        }
        else
        {
            return 0;
        }
    }

    private static long getDataStorage()
    {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return blockSize * availableBlocks;
    }

    public static int getLaunchStatus()
    {
        return ((Game)s_GameActivity).getLaunchStatus();
    }

}
