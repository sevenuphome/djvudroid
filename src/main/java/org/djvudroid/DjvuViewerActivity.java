package org.djvudroid;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import java.io.FileNotFoundException;

public class DjvuViewerActivity extends Activity
{
    //Reuse decodeService in process cause it holds decode caches
    private final static DecodeService decodeService = new DecodeService();
    private static final int MENU_EXIT = 0;
    private DjvuDocumentView documentView;
    private static final String DOCUMENT_VIEW_STATE_PREFERENCES = "DjvuDocumentViewState";

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //Mb already running decoding threads in this process so let's GC'em
        System.gc();
        documentView = new DjvuDocumentView(this);
        documentView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        decodeService.setContainerView(documentView);
        documentView.setDecodeService(decodeService);
        try
        {
            decodeService.open(getContentResolver().openInputStream(getIntent().getData()));
        }
        catch (FileNotFoundException e)
        {
            throw new RuntimeException(e);
        }
        setContentView(documentView);
        final SharedPreferences sharedPreferences = getSharedPreferences(DOCUMENT_VIEW_STATE_PREFERENCES, 0);
        documentView.goToPage(sharedPreferences.getInt(getIntent().getData().toString(), 0));
        documentView.showDocument();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        final SharedPreferences sharedPreferences = getSharedPreferences(DOCUMENT_VIEW_STATE_PREFERENCES, 0);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(getIntent().getData().toString(), documentView.getCurrentPage());
        editor.commit();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0, MENU_EXIT, 0, "Exit");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case MENU_EXIT:
                System.exit(0);
                return true;
        }
        return false;
    }
}
