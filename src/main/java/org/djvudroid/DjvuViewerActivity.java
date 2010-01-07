package org.djvudroid;

import android.app.Activity;
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

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //Mb already running decoding threads in this process so let's GC'em
        System.gc();
        final DjvuDocumentView documentView = new DjvuDocumentView(this);
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
        documentView.showDocument();
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
