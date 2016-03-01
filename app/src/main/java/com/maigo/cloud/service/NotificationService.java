package com.maigo.cloud.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import com.maigo.cloud.model.NotificationInfo;

import de.greenrobot.event.EventBus;

public class NotificationService
{
    public final static int NOTIFICATION_ID = 998;

    private NotificationManager notificationManager;
    private Notification.Builder notificationBuilder;
    private int smallIcon;

    public NotificationService(Context context, int smallIcon)
    {
        this.notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.notificationBuilder = new Notification.Builder(context);
        this.smallIcon = smallIcon;
    }

    public void publishNotificationInfo(NotificationInfo notificationInfo)
    {
        notificationBuilder.setContentTitle(notificationInfo.getTitle());
        notificationBuilder.setContentText(notificationInfo.getContent());
        notificationBuilder.setSmallIcon(smallIcon);

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }
}
