package com.maigo.cloud.xmpp;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.Element;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
public class IQNotificationProvider extends IQProvider
{
    public IQNotificationProvider()
    {
        super();
    }

    private String title;
    private String content;

    @Override
    public Element parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException, SmackException
    {
        boolean isFinish = false;
        while (!isFinish)
        {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG)
            {
                if ("title".equals(parser.getName()))
                {
                    title = parser.nextText();
                }
                else if ("content".equals(parser.getName()))
                {
                    content = parser.nextText();
                }
            }
            else if (eventType == XmlPullParser.END_TAG && "notification".equals(parser.getName()))
            {
                isFinish = true;
            }
        }

        IQNotification iqNotification = new IQNotification();
        iqNotification.setTitle(title);
        iqNotification.setContent(content);
        return iqNotification;
    }
}
