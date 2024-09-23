package org.mini.gui.gscript;

public interface EnvVarProvider {

    public String getEnvVar(String envName);

    public void setEnvVar(String envName, String envValue);
}
