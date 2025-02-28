package org.mini.layout.xwebview;

import org.mini.gui.GForm;
import org.mini.http.MiniHttpClient;

import java.util.List;

public interface XuiBrowserHolder {
    XuiBrowser getBrowser();

    GForm getForm();

    List<MiniHttpClient> getHttpClients();
}
