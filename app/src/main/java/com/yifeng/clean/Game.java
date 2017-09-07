package  com.yifeng.clean;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;
import com.yifeng.clean.utils.AlarmFactory;
import com.yifeng.clean.utils.BroadcastReceiverAlarm;
import com.yifeng.clean.utils.CommonDialog;
import com.yifeng.clean.utils.GameNotification;
import com.yifeng.clean.utils.googleInAppBilling.IabBroadcastReceiver;
import com.yifeng.clean.utils.googleInAppBilling.IabHelper;
import com.yifeng.clean.utils.googleInAppBilling.IabResult;
import com.yifeng.clean.utils.googleInAppBilling.Inventory;
import com.yifeng.clean.utils.googleInAppBilling.Purchase;
import com.yifeng.clean.utils.googleInAppBilling.SkuDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by ywc on 17/4/12.
 */

public class Game extends UnityPlayerActivity implements com.yifeng.clean.utils.googleInAppBilling.IabBroadcastReceiver.IabBroadcastListener {

    public static final int s_fIStartActivityTypeNotify = 1;

    public class PurchaseItem
    {
        String Sku;
        boolean consumable;
        public PurchaseItem(String sku, boolean consumable)
        {
            this.Sku = sku;
            this.consumable = consumable;
        }

        public  String getSku()
        {
            return Sku;
        }

        public boolean isConsumable()
        {
            return  consumable;
        }
    }

    public static List<PurchaseItem> s_ListProductList = new ArrayList<PurchaseItem>(6);

    //broad cast receiver
    private GameBroadCastReceiver m_CBroadReceiver;

    // The helper object
    IabHelper mHelper;

    // Provides purchase notification while this app is running
    IabBroadcastReceiver mBroadcastReceiver;

    private Inventory m_CInventory;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        //registerReceiver to listen network status changed
        m_CBroadReceiver = new GameBroadCastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(m_CBroadReceiver,filter);
        m_CInventory = null;

        //check if start by notification
        Intent in = getIntent();
        if(in != null)
        {
            Bundle bn = in.getExtras();
            if (bn != null)
            {
                int StartTypeOfActivity = bn.getInt(GameNotification.g_StrNotifyActivity);
            }
        }
        else
        {
            //direct start up
        }

    }

    @Override
    protected void onDestroy() {

        //unregister receiver
        if (mBroadcastReceiver != null) {
            unregisterReceiver(m_CBroadReceiver);
        }
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
        }

        // very important:
//        Log.d(TAG, "Destroying helper.");
        if (mHelper != null) {
            mHelper.disposeWhenFinished();
            mHelper = null;
        }

        super.onDestroy();
    }

    public static void setNotificationConfig(int identifier, int id, int[] days, int time, String content)
    {
        if (GameHelper.s_Context == null)
            return;
        //start service when time up.
        Intent wakeUpIntent = new Intent(GameHelper.s_Context, BroadcastReceiverAlarm.class);
        //set action
        wakeUpIntent.setAction(BroadcastReceiverAlarm.g_StrAlarmAction);
        //identifier
        wakeUpIntent.putExtra(BroadcastReceiverAlarm.g_StrNotifyidentifier, GameNotification.g_INotifyBeginIdentifier + identifier);
        //day in week
        wakeUpIntent.putExtra(BroadcastReceiverAlarm.g_StrNotifyDay, days);
        //content
        wakeUpIntent.putExtra(BroadcastReceiverAlarm.g_StrNotifyContent, content);
        //id
        wakeUpIntent.putExtra(BroadcastReceiverAlarm.g_StrNotifyId, id);
        //time
        wakeUpIntent.putExtra(BroadcastReceiverAlarm.g_StrNotifyTime, time);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(GameHelper.s_Context, 0, wakeUpIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        //awake data config

        AlarmFactory.AlarmInformation info = new AlarmFactory().new AlarmInformation();
        info.m_IAlarmType = AlarmFactory.G_ALARM_TYPE_WAKE_SERVICE_NOTIFICATION;
        info.m_CContext = GameHelper.s_Context;
        info.m_CIntent = pendingIntent;
        info.m_IAlarmHour = time;
        info.m_ListDays = days;
//        info.m_SContent = content;
//        info.m_SExtra = extra;
        //send
        AlarmFactory.GenerateAlarm(info);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null)
        {
            Bundle bun = intent.getExtras();
            if (bun != null)
            {
                int StartTypeOfActivity = bun.getInt(GameNotification.g_StrNotifyActivity);
            }
            else
            {

            }
        }
    }

    public void setProductListID(String[] list, String[] consumeAble)
    {
        s_ListProductList.clear();
        for (int i = 0; i < list.length; ++i)
        {
            boolean consume = consumeAble[i].equals("true")  ? true : false;
            PurchaseItem item = new PurchaseItem(list[i], consume);
            s_ListProductList.add(item);
        }
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if(i == KeyEvent.KEYCODE_BACK)
        {
            exitBy2Click();
        }
        return false;
    }

    private static Boolean isExit = false;

    private void exitBy2Click() {
        Timer tExit = null;
        if (isExit == false) {
            isExit = true;
            Toast.makeText(this, "Press again to exit the game", Toast.LENGTH_SHORT).show();
            tExit = new Timer();
            tExit.schedule(new TimerTask() {
                @Override
                public void run() {
                    isExit = false;
                }
            }, 2000);

        } else {
            finish();
            System.exit(0);
        }
    }

    //==========================================================google in app billing==========================================================================
    //init billing
    public void initBilling()
    {
        //start google in app billing
        String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvOYAhPciaewpPE8dbc9RcOkb+eCnaw7tVnKVb31l4Q86mPPFloZOAJGl+XdcyHTyyEB3RlT9RCKVrrnWd8unTkZibP4F1SeZflaSYdzsPBv1YKrwJ9Wt/dzamw0Up+1rAQ7mF/OC+fKY9FoEZhEl4vBBtJI+3jrj6kbG539FsbqWlwvTx1lDDKybrCtBRpdOLKxCUm8AHhY4jo5kFbX7vOJKEnwWcRhxXHUxHgWw7h2TwWh8DnPQXhEKKs3OjpdcpKR9crNG4kOZeGZwb5/iz5GrlWpkNrwjdRCAmRVT84a7fbrH6TcC4m0DOYsSZH8+xCTYlQ78JKIjfMH8R5yVDwIDAQAB";

        // Create the helper, passing it our context and the public key to verify signatures with
        mHelper = new IabHelper(this, base64EncodedPublicKey);

        // enable debug logging (for a production application, you should set this to false).
        mHelper.enableDebugLogging(true);

        // Start setup. This is asynchronous and the specified listener
        // will be called once setup completes.
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {

                if (result.isSuccess()) {
                    // Have we been disposed of in the meantime? If so, quit.
                    if (mHelper == null)
                    {
                        // Important: Dynamically register for broadcast messages about updated purchases.
                        // We register the receiver here instead of as a <receiver> in the Manifest
                        // because we always call getPurchases() at startup, so therefore we can ignore
                        // any broadcasts sent while the app isn't running.
                        // Note: registering this listener in an Activity is a bad idea, but is done here
                        // because this is a SAMPLE. Regardless, the receiver must be registered after
                        // IabHelper is setup, but before first call to getPurchases().
                        mBroadcastReceiver = new IabBroadcastReceiver(Game.this);
                        IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
                        registerReceiver(mBroadcastReceiver, broadcastFilter);
                    }
                }
                else
                {
                    mHelper = null;
                }

                String resultStr = "fail";
                if (result.isSuccess())
                {
                    resultStr = "success";
                }

                UnityPlayer.UnitySendMessage("GameMain","NotifyInitBillingResultEvent", resultStr);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (mHelper == null) return;

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        }
        else {
//            Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }

    @Override
    public void receivedBroadcast() {

        runOnUiThread(new Runnable() {
            public void run()
            {
                // Received a broadcast notification that the inventory of items has changed
                try {
                    if (mHelper != null) mHelper.queryInventoryAsync(mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    UnityPlayer.UnitySendMessage("GameMain","NotifyRequestProductListFailureEvent", "");
                }
            }
        });

    }

    // Called when consumption is complete
    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
        // if we were disposed of in the meantime, quit.
        if (mHelper == null || result.isFailure()) {
            UnityPlayer.UnitySendMessage("GameMain","NotifyBillingConsumResultEvent", "fail");
        }

            UnityPlayer.UnitySendMessage("GameMain","NotifyBillingConsumResultEvent", "success");

        }
    };

    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            //Log.d(TAG, "Query inventory finished.");

            // failure, notify game
            if (mHelper == null || result.isFailure()) {
                UnityPlayer.UnitySendMessage("GameMain","NotifyRequestProductListFailureEvent", "");
            }
            else
            {
                m_CInventory = inventory;

                List<Purchase> consumeList = new ArrayList<>();

                StringBuffer sb = new StringBuffer();
                sb.append("");
                for (int i = 0; i < s_ListProductList.size(); ++i)
                {
                    PurchaseItem item = s_ListProductList.get(i);
                    SkuDetails detail = inventory.getSkuDetails(item.getSku());
                    Purchase pur = inventory.getPurchase(item.getSku());

                    if (detail != null)
                    {
                        sb.append(detail.getPrice() + "|");

                        //consume
                        if (item.isConsumable() && pur != null)
                        {
                            try {
                                mHelper.consumeAsync(pur, mConsumeFinishedListener);
                            } catch (IabHelper.IabAsyncInProgressException e) {
                                UnityPlayer.UnitySendMessage("GameMain","NotifyBillingConsumResultEvent", "fail");
                            }
                        }
                    }
                }

                UnityPlayer.UnitySendMessage("GameMain","NotifyRequestProductListSuccessEvent", sb.toString());
            }
        }
    };

    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
//            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null || result.isFailure()) {
                CommonDialog.popCommonDialog("inform", "purchase failed");
                UnityPlayer.UnitySendMessage("GameMain","NotifyPurchaseFailureEvent", "");
                return;
            }

            UnityPlayer.UnitySendMessage("GameMain","NotifyPurchaseSuccessEvent", "");

            UnityPlayer.UnitySendMessage("GameMain","NotifyVerifyPurchaseEvent", purchase.getDeveloperPayload());

            for (int i = 0; i < s_ListProductList.size(); ++i)
            {
                if (s_ListProductList.get(i).getSku().equals(purchase.getSku()))
                {
                    if (s_ListProductList.get(i).isConsumable())
                    {
                        try {
                            mHelper.consumeAsync(purchase, mConsumeFinishedListener);
                        } catch (IabHelper.IabAsyncInProgressException e) {
                            UnityPlayer.UnitySendMessage("GameMain","NotifyBillingConsumResultEvent", "fail");
                        }
                        break;
                    }
                }
            }
//
            CommonDialog.popCommonDialog("inform", "purchase success");
        }
    };

    //request product list
    public void requestProductList()
    {
        if (mHelper == null)
        {
            UnityPlayer.UnitySendMessage("GameMain","NotifyRequestProductListFailureEvent", "");
            return;
        }

        List<String> additionalSkuList = new ArrayList<>();
        for (int i = 0; i < s_ListProductList.size(); ++i)
        {
            PurchaseItem item = s_ListProductList.get(i);
            additionalSkuList.add(i, item.getSku());
        }

        try {
            mHelper.queryInventoryAsync(true, additionalSkuList, null, mGotInventoryListener);
        }
        catch (IabHelper.IabAsyncInProgressException e)
        {
            UnityPlayer.UnitySendMessage("GameMain","NotifyRequestProductListFailureEvent", "");
        }

//        runOnUiThread(new Runnable() {
//            public void run()
//            {
//                try {
//                    mHelper.queryInventoryAsync(mGotInventoryListener);
//                } catch (IabHelper.IabAsyncInProgressException e) {
//                    UnityPlayer.UnitySendMessage("GameMain","NotifyRequestProductListFailureEvent", "");
//                }
//            }
//        });

    }

    //request purchase
    public void requestPurchase(final  String productId,final String strId)
    {
        if (mHelper == null)
        {
            UnityPlayer.UnitySendMessage("GameMain","NotifyPurchaseFailureEvent", "");
            return;
        }

        runOnUiThread(new Runnable() {
            public void run()
            {
                try {
                    SkuDetails detail = m_CInventory.getSkuDetails(productId);
                    //detail.getType()

                    mHelper.launchPurchaseFlow(GameHelper.s_GameActivity, productId, 0,
                            mPurchaseFinishedListener, strId);
                } catch (Exception e) {
                    UnityPlayer.UnitySendMessage("GameMain","NotifyPurchaseFailureEvent", "");
                }
            }
        });

    }

    //consume Product
    public void consumeProduct(final  String productId,final String strId)
    {
        if (mHelper == null)
        {
            UnityPlayer.UnitySendMessage("GameMain","NotifyBillingConsumResultEvent", "fail");
            return;
        }

        final Purchase purchase = m_CInventory.getPurchase(productId);

        if (purchase != null)
        {
           try {
                mHelper.consumeAsync(purchase, mConsumeFinishedListener);
            } catch (IabHelper.IabAsyncInProgressException e) {
                UnityPlayer.UnitySendMessage("GameMain","NotifyBillingConsumResultEvent", "fail");
            }
        }
        else
        {
            UnityPlayer.UnitySendMessage("GameMain","NotifyBillingConsumResultEvent", "fail");
        }

    }

    //==========================================================google in app billing==========================================================================
}
