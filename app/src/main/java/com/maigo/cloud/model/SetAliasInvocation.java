package com.maigo.cloud.model;

import com.maigo.cloud.listener.SetAliasCompleteListener;

public class SetAliasInvocation
{
    private String alias;
    private SetAliasCompleteListener listener;

    public String getAlias()
    {
        return alias;
    }

    public void setAlias(String alias)
    {
        this.alias = alias;
    }

    public SetAliasCompleteListener getSetAliasCompleteListener()
    {
        return listener;
    }

    public void setSetAliasCompleteListener(SetAliasCompleteListener listener)
    {
        this.listener = listener;
    }
}
