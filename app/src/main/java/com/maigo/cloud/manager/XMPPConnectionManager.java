package com.maigo.cloud.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.maigo.cloud.client.R;
import com.maigo.cloud.listener.CloudServiceListener;
import com.maigo.cloud.listener.SetAliasCompleteListener;
import com.maigo.cloud.model.NotificationInfo;
import com.maigo.cloud.model.SetAliasInvocation;
import com.maigo.cloud.service.ConnectService;
import com.maigo.cloud.service.NotificationService;
import com.maigo.cloud.xmpp.IQAck;
import com.maigo.cloud.xmpp.IQAckProvider;
import com.maigo.cloud.xmpp.IQAlias;
import com.maigo.cloud.xmpp.IQMessage;
import com.maigo.cloud.xmpp.IQMessageProvider;
import com.maigo.cloud.xmpp.IQNotification;
import com.maigo.cloud.xmpp.IQNotificationProvider;

import org.jivesoftware.smack.AbstractConnectionListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.iqrequest.IQRequestHandler;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;

import java.security.KeyStore;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

public class XMPPConnectionManager
{
    public final static String SERVICE_NAME = "MaigoCloudService";
    public final static String SERVICE_HOST = "192.168.1.106";
    public final static int SERVICE_PORT = 5222;
    public final static String XMPP_RESOURCE = "Android";

    public final static String KEYSTORE_TYPE = "BKS";
    private final static String CLIENT_KEY_PASSWORD = "5201314";
    private final static String CLIENT_TRUST_PASSWORD = "5201314";

    private String apiKey;
    private String username = "";
    private String password = "";
    private boolean needToRegister;
    private Context context;

    private XMPPTCPConnection xmpptcpConnection;
    private CloudServiceListener cloudServiceListener;

    //service loop and try to connect until success
    private ConnectService connectService;

    //service publish the receiving push notification info
    private NotificationService notificationService;

    //handle the heart beat, ping server one time in one minute
    private PingManager pingManager;

    //only process the <notification> <message> or <ack>
    private StanzaFilter stanzaFilter;

    //if the connection is connecting with the server
    private boolean isConnecting = false;

    /**
     * @key IQAlias stanzaId
     * @value SetAliasCompleteListener
     */
    private Map<String, SetAliasInvocation> setAliasInvocationMap = new ConcurrentHashMap<String, SetAliasInvocation>();

    public XMPPConnectionManager(Context context, String apiKey)
    {
        this.context = context;
        this.apiKey = apiKey;

        notificationService = new NotificationService(context, R.mipmap.ic_launcher);
        stanzaFilter = new OrFilter(new StanzaTypeFilter(IQNotification.class), new StanzaTypeFilter(IQAck.class));
    }

    public synchronized void start()
    {
        config();
    }

    private synchronized void config()
    {
        ConfigAsyncTask configAsyncTask = new ConfigAsyncTask(context);
        configAsyncTask.setConfigListener(new ConfigAsyncTask.ConfigListener() {
            @Override
            public void onConfigComplete(XMPPTCPConnection connection) {
                xmpptcpConnection = connection;

                registerStanzaProviders();
                registerStanzaHandlers();
                connect();
            }
        });
        configAsyncTask.execute();
    }

    private synchronized void connect()
    {
        needToRegister = false;
        SharedPreferences sharedPreferences = context.getSharedPreferences(SERVICE_NAME, Context.MODE_PRIVATE);
        username = sharedPreferences.getString("username", "");
        password = sharedPreferences.getString("password", "");
        if (username.equals("") || password.equals(""))
        {
            username = UUID.randomUUID().toString().replace("-", "");
            password = UUID.randomUUID().toString().replace("-", "");
            needToRegister = true;
        }

        connectService = new ConnectService(apiKey, username, password, needToRegister);
        connectService.setApiKey(apiKey);
        connectService.setXmppConnection(xmpptcpConnection);
        connectService.start();

        xmpptcpConnection.addConnectionListener(new AbstractConnectionListener() {
            @Override
            public void authenticated(XMPPConnection connection, boolean resumed) {
                if (cloudServiceListener != null)
                    cloudServiceListener.onCloudServiceConnect();

                isConnecting = true;
                sendUnconfirmAlias();
                startHeartBeat();
            }
        });
    }

    private synchronized void startHeartBeat()
    {
        pingManager = PingManager.getInstanceFor(xmpptcpConnection);
        pingManager.setPingInterval(60);
        pingManager.registerPingFailedListener(new PingFailedListener() {
            @Override
            public void pingFailed() {
                isConnecting = false;
                reconnect();
            }
        });
    }

    private synchronized void reconnect()
    {
        xmpptcpConnection.disconnect();
        connectService.wakeUp();
        pingManager.setPingInterval(-1);    //disable the auto ping
    }

    public synchronized void stop()
    {
        Thread stopThread = new Thread()
        {
            @Override
            public void run()
            {
                connectService.cancel();
                xmpptcpConnection.disconnect();
                pingManager.setPingInterval(-1);
            }
        };
        stopThread.start();
    }

    public String getLocalUsername()
    {
        return username;
    }

    public void setCloudServiceListener(CloudServiceListener cloudServiceListener)
    {
        this.cloudServiceListener = cloudServiceListener;
    }

    private void registerStanzaProviders()
    {
        ProviderManager.addIQProvider(IQNotification.ELEMENT_NAME, IQNotification.NAMESPACE, new IQNotificationProvider());
        ProviderManager.addIQProvider(IQMessage.ELEMENT_NAME, IQMessage.NAMESPACE, new IQMessageProvider());
        ProviderManager.addIQProvider(IQAck.ELEMENT_NAME, IQAck.NAMESPACE, new IQAckProvider());
        //add more custom stanza providers here
    }

    private void registerStanzaHandlers()
    {
        //handle IQNotification
        xmpptcpConnection.registerIQRequestHandler(new AbstractIqRequestHandler(IQNotification.ELEMENT_NAME, IQNotification.NAMESPACE,
                IQ.Type.set, IQRequestHandler.Mode.async) {
            @Override
            public IQ handleIQRequest(IQ iqRequest) {
                //make up a NotificationInfo containing the info received from server
                IQNotification iqNotification = (IQNotification) iqRequest;
                NotificationInfo notificationInfo = new NotificationInfo();
                notificationInfo.setTitle(iqNotification.getTitle());
                notificationInfo.setContent(iqNotification.getContent());

                if (cloudServiceListener != null) {
                    cloudServiceListener.onReceivePushNotification(iqNotification.getTitle(),
                            iqNotification.getContent());
                }

                //publish the notification and show on the NotificationBar
                notificationService.publishNotificationInfo(notificationInfo);

                //make up a Ack containing the same stanzaId and reply
                IQAck iqAck = new IQAck();
                iqAck.setType(IQ.Type.set);
                iqAck.setStanzaId(iqNotification.getStanzaId());

                return iqAck;
            }
        });

        //handle IQMessage
        xmpptcpConnection.registerIQRequestHandler(new AbstractIqRequestHandler(IQMessage.ELEMENT_NAME, IQMessage.NAMESPACE,
                IQ.Type.set, IQRequestHandler.Mode.async)
        {
            @Override
            public IQ handleIQRequest(IQ iqRequest)
            {
                //make up a NotificationInfo containing the info received from server
                IQMessage iqMessage = (IQMessage) iqRequest;
                if(cloudServiceListener != null)
                {
                    cloudServiceListener.onReceiveTransparentMessage(iqMessage.getTitle(),
                            iqMessage.getContent());
                }

                //make up a Ack containing the same stanzaId and reply
                IQAck iqAck = new IQAck();
                iqAck.setType(IQ.Type.set);
                iqAck.setStanzaId(iqMessage.getStanzaId());

                return iqAck;
            }
        });

        //handle IQAck
        xmpptcpConnection.registerIQRequestHandler(new AbstractIqRequestHandler(IQAck.ELEMENT_NAME, IQAck.NAMESPACE,
                IQ.Type.set, IQRequestHandler.Mode.async)
        {
            @Override
            public IQ handleIQRequest(IQ iqRequest)
            {
                IQAck iqAck = (IQAck) iqRequest;
                SetAliasInvocation setAliasInvocation = setAliasInvocationMap.get(iqAck.getStanzaId());
                if(setAliasInvocation == null)
                    return null;

                SetAliasCompleteListener setAliasCompleteListener = setAliasInvocation.getSetAliasCompleteListener();
                if(setAliasCompleteListener == null)
                    return null;

                setAliasCompleteListener.onSetAliasComplete(iqAck.isSuccess);
                setAliasInvocationMap.remove(iqAck.getStanzaId());

                return null;
            }
        });
    }

    public boolean isConnecting()
    {
        return isConnecting;
    }

    public synchronized void setAlias(String alias, SetAliasCompleteListener setAliasCompleteListener)
    {
        IQAlias iqAlias = new IQAlias();
        iqAlias.setType(IQ.Type.set);
        iqAlias.setAlias(alias);

        String stanzaId = UUID.randomUUID().toString().replace("-", "");
        iqAlias.setStanzaId(stanzaId);

        SetAliasInvocation setAliasInvocation = new SetAliasInvocation();
        setAliasInvocation.setAlias(alias);
        setAliasInvocation.setSetAliasCompleteListener(setAliasCompleteListener);
        setAliasInvocationMap.put(stanzaId, setAliasInvocation);

        //if current state is connecting, send IQAlias immediately
        if(isConnecting)
        {
            try
            {
                xmpptcpConnection.sendStanza(iqAlias);
            }
            catch (SmackException.NotConnectedException e)
            {
                e.printStackTrace();
            }
        }
    }

    private synchronized void sendUnconfirmAlias()
    {
        ConcurrentHashMap<String, SetAliasInvocation> copyHashMap = new ConcurrentHashMap<String, SetAliasInvocation>(setAliasInvocationMap);
        setAliasInvocationMap.clear();

        for(SetAliasInvocation setAliasInvocation : copyHashMap.values())
        {
            setAlias(setAliasInvocation.getAlias(), setAliasInvocation.getSetAliasCompleteListener());
        }
    }

    private static class ConfigAsyncTask extends AsyncTask<Void, Void, XMPPTCPConnection>
    {
        private Context context;
        private XMPPTCPConnection xmpptcpConnection;
        private ConfigListener configListener;

        ConfigAsyncTask(Context context)
        {
            this.context = context;
        }

        public void setConfigListener(ConfigListener configListener)
        {
            this.configListener = configListener;
        }

        @Override
        protected XMPPTCPConnection doInBackground(Void... params)
        {
            XMPPTCPConnectionConfiguration.Builder connectionConfigurationBuilder = XMPPTCPConnectionConfiguration.builder();

            connectionConfigurationBuilder.setHost(SERVICE_HOST);
            connectionConfigurationBuilder.setPort(SERVICE_PORT);
            connectionConfigurationBuilder.setServiceName(SERVICE_NAME);
            connectionConfigurationBuilder.setSecurityMode(ConnectionConfiguration.SecurityMode.required);
            connectionConfigurationBuilder.setCompressionEnabled(false);
            connectionConfigurationBuilder.setDebuggerEnabled(true);
            connectionConfigurationBuilder.setResource(XMPP_RESOURCE);
            connectionConfigurationBuilder.setSendPresence(false);
            connectionConfigurationBuilder.allowEmptyOrNullUsernames();
            connectionConfigurationBuilder.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return hostname.equals(SERVICE_NAME);
                }
            });

            try
            {
                KeyStore keyStoreClientKey = KeyStore.getInstance(KEYSTORE_TYPE);
                KeyStore keyStoreClientTrust = KeyStore.getInstance(KEYSTORE_TYPE);

                keyStoreClientKey.load(context.getResources().openRawResource(R.raw.client_key),
                        CLIENT_KEY_PASSWORD.toCharArray());
                keyStoreClientTrust.load(context.getResources().openRawResource(R.raw.client_trust),
                        CLIENT_TRUST_PASSWORD.toCharArray());
                KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

                keyManagerFactory.init(keyStoreClientKey, "5201314".toCharArray());
                trustManagerFactory.init(keyStoreClientTrust);

                SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
                connectionConfigurationBuilder.setCustomSSLContext(sslContext);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            xmpptcpConnection = new XMPPTCPConnection(connectionConfigurationBuilder.build());

            return xmpptcpConnection;
        }

        @Override
        protected void onPostExecute(XMPPTCPConnection xmpptcpConnection)
        {
            if(configListener != null)
                configListener.onConfigComplete(xmpptcpConnection);
        }

        public interface ConfigListener
        {
            /**
             * return the XMPPTCPConnection build by config.
             * @param xmpptcpConnection
             */
            public void onConfigComplete(XMPPTCPConnection xmpptcpConnection);
        }
    };
}
