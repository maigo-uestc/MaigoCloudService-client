package com.maigo.cloud.xmpp;

import org.jivesoftware.smack.packet.IQ;

public class IQAlias extends IQ
{
    public final static String ELEMENT_NAME = "alias";
    public final static String NAMESPACE = "maigo.cloud.service:alias";

    private String alias;

    public IQAlias()
    {
        super(ELEMENT_NAME, NAMESPACE);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml)
    {
        xml.rightAngleBracket();
        xml.append(alias);

        return xml;
    }

    public String getAlias()
    {
        return alias;
    }

    public void setAlias(String alias)
    {
        this.alias = alias;
    }
}
