package com.maigo.cloud.xmpp;

import org.jivesoftware.smack.packet.IQ;

public class IQAck extends IQ
{
    public final static String ELEMENT_NAME = "ack";
    public final static String NAMESPACE = "maigo.cloud.service:ack";

    public boolean isSuccess = true;

    public IQAck()
    {
        super(ELEMENT_NAME, NAMESPACE);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml)
    {
        xml.rightAngleBracket();
        xml.openElement("success");
        xml.append(String.valueOf(isSuccess));
        xml.closeElement("success");

        return xml;
    }

    public boolean isSuccess()
    {
        return isSuccess;
    }

    public void setIsSuccess(boolean isSuccess)
    {
        this.isSuccess = isSuccess;
    }
}
