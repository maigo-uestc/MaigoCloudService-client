package com.maigo.cloud.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.Tag;
import android.util.Log;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.iqregister.AccountManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConnectService extends Thread
{
    private final static String TAG = "ConnectService";

    private Object notifyObject = new Object();
    private AbstractXMPPConnection xmppConnection;
    private int failTimes = 0;

    private String apiKey;
    private String username;
    private String password;
    private boolean needToRegister;

    private volatile boolean isCancel = false;

    public ConnectService(String apiKey, String username, String password, boolean needToRegister)
    {
        this.apiKey = apiKey;
        this.username = username;
        this.password = password;
        this.needToRegister = needToRegister;
    }

    public void setXmppConnection(AbstractXMPPConnection xmppConnection)
    {
        this.xmppConnection = xmppConnection;
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

    @Override
    public void run()
    {
        Log.e(TAG, "service start.");

        while(true)
        {
            Log.e(TAG, "try to connect.");
            synchronized (notifyObject)
            {
                if(isCancel)
                    break;

                try
                {
                    if(!xmppConnection.isConnected())
                    {
                        xmppConnection.connect();
                    }

                    if (needToRegister)
                    {
                        AccountManager accountManager = AccountManager.getInstance(xmppConnection);
                        Map<String, String> attr = new HashMap<String, String>();
                        attr.put("apiKey", apiKey);
                        accountManager.createAccount(username, password, attr);
                    }

                    if(!xmppConnection.isAuthenticated())
                        xmppConnection.login(username, password);
                } catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                } catch (XMPPException e) {
                    e.printStackTrace();
                } catch (SmackException.NoResponseException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (SmackException e) {
                    e.printStackTrace();
                }

                try
                {
                    if(xmppConnection.isAuthenticated())
                    {
                        needToRegister = false;

                        failTimes = 0;
                        notifyObject.wait();
                    }
                    else
                    {
                        failTimes++;
                        notifyObject.wait(getWaitMillis());
                    }
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }

        Log.e(TAG, "cancel successfully.");
        return;
    }

    public void wakeUp()
    {
        if(isCancel)
            return;

        synchronized(notifyObject)
        {
            notifyObject.notify();
        }
    }

    /**
     * cancel the current connect service and stop the thread.
     */
    public void cancel()
    {
        this.isCancel = true;
        this.interrupt();
    }

    private int getWaitMillis()
    {
        if(failTimes <= 3)
            return 0;
        else if(failTimes <= 6)
            return 10 * 1000;
        else if(failTimes <= 9)
            return 30 * 1000;
        else if(failTimes <= 12)
            return 60 * 1000;
        else if(failTimes <= 15)
            return 300 * 1000;
        else if(failTimes <= 18)
            return 1800 * 1000;
        else
            return 3600 * 1000;
    }
}
