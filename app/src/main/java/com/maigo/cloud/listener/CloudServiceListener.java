package com.maigo.cloud.listener;

public interface CloudServiceListener
{
    public void onCloudServiceConnect();

    public void onReceivePushNotification(String title, String content);

    public void onReceiveTransparentMessage(String title, String content);
}
