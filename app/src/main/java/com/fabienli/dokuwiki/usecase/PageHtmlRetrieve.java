package com.fabienli.dokuwiki.usecase;

import android.os.AsyncTask;
import android.util.Log;

import com.fabienli.dokuwiki.db.AppDatabase;
import com.fabienli.dokuwiki.db.Page;
import com.fabienli.dokuwiki.db.PageUpdateHtml;
import com.fabienli.dokuwiki.db.SyncAction;
import com.fabienli.dokuwiki.sync.PageHtmlDownloader;
import com.fabienli.dokuwiki.sync.PageInfoRetriever;
import com.fabienli.dokuwiki.sync.XmlRpcAdapter;
import com.fabienli.dokuwiki.tools.Logs;
import com.fabienli.dokuwiki.usecase.callback.PageHtmlRetrieveCallback;

import java.util.List;

public class PageHtmlRetrieve extends PoolAsyncTask {
    String TAG = "PageHtmlRetrieve";
    AppDatabase _db = null;
    PageHtmlRetrieveCallback _pageHtmlRetrieveCallback = null;
    String _pageContent = "";
    XmlRpcAdapter _xmlRpcAdapter;

    public PageHtmlRetrieve(AppDatabase db, XmlRpcAdapter xmlRpcAdapter) {
        _db = db;
        _xmlRpcAdapter = xmlRpcAdapter;
    }

    public String retrievePage(String pagename) {
        Log.d(TAG, "retrievePage: "+pagename);
        String pageContent = "";
        String pageVersion = "";
        SyncAction syncActionRelated = null;
        // check it in DB
        Page dbPage = _db.pageDao().findByName(pagename);
        if(dbPage != null) {
            Log.d(TAG, "page "+pagename+" loaded from local db" );
            Logs.getInstance().add("page "+pagename+" loaded from local db" );
            if(!dbPage.isHtmlEmpty()) pageContent = dbPage.html;
            pageVersion = dbPage.rev;
            // Check if a more recent version exists:
            List<SyncAction> synActionItems = _db.syncActionDao().getAll();
            for (SyncAction sa : synActionItems ){
                if(sa.name.compareTo(pagename) == 0 && sa.verb.compareTo("GET") == 0 && sa.priority.compareTo(SyncAction.LEVEL_GET_DYNAMICS) != 0)
                {
                    syncActionRelated = sa;
                    pageVersion = sa.rev;
                }
            }
        }
        // get it from server if not there
        if(pageContent.length() == 0 || syncActionRelated != null)
        {
            Log.d(TAG,"page "+pagename+" not in local db, get it from server" );
            Logs.getInstance().add("page "+pagename+" not in local db, get it from server" );
            PageHtmlDownloader pageHtmlDownloader = new PageHtmlDownloader(_xmlRpcAdapter);
            pageContent = pageHtmlDownloader.retrievePageHTML(pagename);

            if(pageContent == null || pageContent.length()==0) {
                // new page, propose to create it
                StaticPagesDisplay staticPagesDisplay = new StaticPagesDisplay(_db, "");
                pageContent = staticPagesDisplay.getProposeCreatePageHtml(pagename);
            }
            else {
                // get the version if we need
                if (pageVersion == null || pageVersion.length() == 0) {
                    // need to update from server
                    PageInfoRetriever pageInfoRetriever = new PageInfoRetriever(_xmlRpcAdapter);
                    pageVersion = pageInfoRetriever.retrievePageVersion(pagename);
                }
                // store it in DB cache
                PageUpdateHtml pageUpdateHtml = new PageUpdateHtml(_db, pagename, pageContent, pageVersion);
                pageUpdateHtml.doSync();

                if (syncActionRelated != null) {
                    _db.syncActionDao().deleteAll(syncActionRelated);
                }
            }
        }
        return pageContent;
    }

    public void retrievePageAsync(String pagename, PageHtmlRetrieveCallback pageHtmlRetrieveCallback) {
        Log.d(TAG, "retrievePageAsync: "+pagename);
        _pageHtmlRetrieveCallback = pageHtmlRetrieveCallback;
        //execute(pagename);
        executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, pagename);
    }

    @Override
    protected String doInBackground(String... pagename) {
        if(pagename.length == 1){
            Log.d(TAG, "doInBackground: "+pagename[0]);
            _pageContent = retrievePage(pagename[0]);
        }
        return "ok";
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if(_pageHtmlRetrieveCallback!=null)
            _pageHtmlRetrieveCallback.pageRetrieved(_pageContent);
    }
}
