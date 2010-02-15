package org.djvudroid;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import org.djvudroid.presentation.BrowserAdapter;

import java.io.File;

public class DjvuBrowserActivity extends Activity
{
    private BrowserAdapter adapter;
    private static final String CURRENT_DIRECTORY = "currentDirectory";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browser);
        initListView();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
        setCurrentDir(new File("/sdcard"));
        if (savedInstanceState != null)
        {
            final String absolutePath = savedInstanceState.getString(CURRENT_DIRECTORY);
            if (absolutePath != null)
            {
                setCurrentDir(new File(absolutePath));
            }
        }
    }

    private void initListView()
    {
        final ListView listView = (ListView) findViewById(R.id.browserList);
        adapter = new BrowserAdapter(this);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                final File file = adapter.getItem(i);
                if (file.isDirectory())
                {
                    setCurrentDir(file);
                }
                else
                {
                    showDjvuDocument(file);
                }
            }
        });
    }

    private void showDjvuDocument(File file)
    {
        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.fromFile(file));
        intent.setClass(this, DjvuViewerActivity.class);
        startActivity(intent);
    }

    private void setCurrentDir(File newDir)
    {
        adapter.setCurrentDirectory(newDir);
        getWindow().setTitle(newDir.getAbsolutePath());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putString(CURRENT_DIRECTORY, adapter.getCurrentDirectory().getAbsolutePath());
    }


}
