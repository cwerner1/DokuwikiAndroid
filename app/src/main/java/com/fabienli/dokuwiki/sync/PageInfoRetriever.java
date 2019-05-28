package com.fabienli.dokuwiki.sync;

import android.util.Log;

import java.util.ArrayList;

public class PageInfoRetriever {
    protected String TAG = "PageInfoRetriever";
    protected XmlRpcAdapter _xmlRpcAdapter = null;

    public PageInfoRetriever(XmlRpcAdapter xmlRpcAdapter) {
        _xmlRpcAdapter = xmlRpcAdapter;
    }

    public String retrievePageInfo(String pagename){
        Log.d(TAG,"GetPage info "+pagename);
        ArrayList<String> resultList = _xmlRpcAdapter.callMethod("wiki.getPageInfo", pagename);

        String pageVersion = "";
        for (String r : resultList) {
            String pageinfo = r.replace("{", "").replace("}", "").replace(", ", ",");
            String[] parts = pageinfo.split(",");
            for (String a : parts) {
                if (a.startsWith("version=")) {
                    pageVersion = a.substring(8);
                }
            }
        }

        return pageVersion;
    }
}
