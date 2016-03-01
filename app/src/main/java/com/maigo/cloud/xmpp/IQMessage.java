package com.maigo.cloud.xmpp;

import org.jivesoftware.smack.packet.IQ;

public class IQMessage extends IQ
{
    public final static String ELEMENT_NAME = "message";
    public final static String NAMESPACE = "maigo.cloud.service:message";

    private String title;
    private String content;

    public IQMessage()
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
