package org.mini.gui;

public interface GuiScriptEnvVarAccessor {

    String getEnv(String key);

    void setEnv(String key, String value);
}
