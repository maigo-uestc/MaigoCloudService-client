package com.maigo.cloud.xmpp;

import org.jivesoftware.smack.packet.IQ;

public class IQNotification extends IQ
{
    public final static String ELEMENT_NAME = "notification";
    public final static String NAMESPACE = "maigo.cloud.service:notification";

    private String title;
    private String content;

    public IQNotification()
    {
        super(ELEMENT_NAME, NAMESPACE);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml)
    {
        return xml;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }
}
