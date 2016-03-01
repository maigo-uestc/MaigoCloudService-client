package com.maigo.cloud.context;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;

import com.maigo.cloud.listener.CloudServiceListener;
import com.maigo.cloud.listener.SetAliasCompleteListener;
import com.maigo.cloud.manager.XMPPConnectionManager;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MaigoCloudService extends Service
{
    public final static String SERVICE_NAME = "MaigoCloudService";

    private String apiKey;

    private boolean isInited = false;
    private boolean isStarted = false;

    private static MaigoCloudService instance;

    private XMPPConnectionManager xmppConnectionManager;
    private static CloudServiceListener cloudServiceListener;

    /**
     * store the set alias when the service is not start. All the alias will be auto send to xmppConnectionManager
     * when startCloudService() call.
     * @key alias
     * @value setAliasCompleteListener
     */
    private static Map<String, SetAliasCompleteListener> unstartedSetAliasMap = new ConcurrentHashMap<String, SetAliasCompleteListener>();

    public MaigoCloudService()
    {

    }

    public static void start(Context context)
    {
        Intent intent = new Intent(context, MaigoCloudService.class);
        context.startService(intent);
    }

    public static void stop()
    {
        if(instance != null)
            instance.stopService();
    }

    public static String getLocalUsername()
    {
        String username = null;
        if(instance != null)
            username = instance.xmppConnectionManager.getLocalUsername();

        if(username.equals(""))
            username = null;

        return username;
    }

    public static void setAlias(String alias, SetAliasCompleteListener listener)
    {
        if(instance == null || !instance.isStarted)
        {
            unstartedSetAliasMap.put(alias, listener);
            return;
        }

        instance.xmppConnectionManager.setAlias(alias, listener);
    }

    public static void setCloudServiceListener(CloudServiceListener listener)
    {
        cloudServiceListener = listener;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        init();
        startCloudService();

        return super.onStartCommand(intent, flags, startId);
    }

    private synchronized void init()
    {
        if(isInited)
            return;

        try
        {
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            apiKey = appInfo.metaData.getString("MaigoCloudService.API_KEY");
        }
        catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }

        if(apiKey == null)
            throw new IllegalArgumentException("MaigoCloudService.API_KEY should be set in <meta-data/> tag in <application/>");

        instance = this;
        isInited = true;
    }

    private synchronized void startCloudService()
    {
        if(isStarted)
            return;

        xmppConnectionManager = new XMPPConnectionManager(this, apiKey);
        xmppConnectionManager.setCloudServiceListener(new CloudServiceListener()
        {
            @Override
            public void onCloudServiceConnect()
            {
                if(cloudServiceListener != null)
                    cloudServiceListener.onCloudServiceConnect();
            }

            @Override
            public void onReceivePushNotification(String title, String content)
            {
                if(cloudServiceListener != null)
                    cloudServiceListener.onReceivePushNotification(title, content);
            }

            @Override
            public void onReceiveTransparentMessage(String title, String content)
            {
                if(cloudServiceListener != null)
                    cloudServiceListener.onReceiveTransparentMessage(title, content);
            }
        });
        xmppConnectionManager.start();
        doUnstartedSetAlias();

        isStarted = true;
    }

    private void stopService()
    {
        isInited = false;
        isStarted = false;

        xmppConnectionManager.stop();

        stopSelf();
    }

    private void doUnstartedSetAlias()
    {
        ConcurrentHashMap<String, SetAliasCompleteListener> copyHashMap = new ConcurrentHashMap<String, SetAliasCompleteListener>(unstartedSetAliasMap);
        unstartedSetAliasMap.clear();

        Set<Map.Entry<String, SetAliasCompleteListener>> entrySet = copyHashMap.entrySet();
        for(Map.Entry<String, SetAliasCompleteListener> entry : entrySet)
        {
            xmppConnectionManager.setAlias(entry.getKey(), entry.getValue());
        }
    }
}
