package org.djvudroid;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.io.FileFilter;

public class DjvuBrowserActivity extends Activity
{
    private File currentDirectory;
    private ArrayAdapter<String> adapter;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.browser);
        initListView();
        setCurrentDir(new File("/"));
    }

    private void initListView()
    {
        final ListView listView = (ListView) findViewById(R.id.browserList);
        adapter = new ArrayAdapter<String>(this, R.layout.browsertextview, R.id.browserTextView);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
            {
                final File file = new File(currentDirectory, adapter.getItem(i));
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
        final File[] files = newDir.listFiles(new FileFilter()
        {
            public boolean accept(File pathname)
            {
                return pathname.isDirectory() || pathname.getName().endsWith(".djvu") || pathname.getName().endsWith(".djv");
            }
        });
        adapter.clear();
        if (newDir.getParent() != null)
        {
            adapter.add("..");
        }
        for (File file : files)
        {
            adapter.add(file.getName());
        }
        currentDirectory = newDir;
    }
}
