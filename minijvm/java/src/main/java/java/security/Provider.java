package java.security;

import java.util.Properties;

public abstract class Provider extends Properties {
    public String getName() {
        return (String) get("provider.name");
    }
}
