package com.maigo.cloud.xmpp;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.Element;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class IQAckProvider extends IQProvider
{
    private boolean isSuccess;

    @Override
    public Element parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException, SmackException
    {
        boolean isFinish = false;
        while (!isFinish)
        {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG)
            {
                if ("success".equals(parser.getName()))
                {
                    isSuccess = parser.nextText().equals("true");
                }
            }
            else if (eventType == XmlPullParser.END_TAG && "ack".equals(parser.getName()))
            {
                isFinish = true;
            }
        }

        IQAck iqAck = new IQAck();
        iqAck.setIsSuccess(isSuccess);
        return iqAck;
    }
}
