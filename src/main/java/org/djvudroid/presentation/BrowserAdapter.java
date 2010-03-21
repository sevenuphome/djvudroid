package org.djvudroid.presentation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import org.djvudroid.R;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BrowserAdapter extends BaseAdapter
{
    private final Context context;
    private File currentDirectory;
    private List<File> files = Collections.emptyList();

    public BrowserAdapter(Context context)
    {
        this.context = context;
    }

    public int getCount()
    {
        return files.size();
    }

    public File getItem(int i)
    {
        return files.get(i);
    }

    public long getItemId(int i)
    {
        return i;
    }

    public View getView(int i, View view, ViewGroup viewGroup)
    {
        final View browserItem = LayoutInflater.from(context).inflate(R.layout.browseritem, viewGroup, false);
        final ImageView imageView = (ImageView) browserItem.findViewById(R.id.browserItemIcon);
        final File file = files.get(i);
        final TextView textView = (TextView) browserItem.findViewById(R.id.browserItemText);
        textView.setText(file.getName());
        if (file.equals(currentDirectory.getParentFile()))
        {
            imageView.setImageResource(R.drawable.arrowup);
            textView.setText(file.getAbsolutePath());
        }
        else if (file.isDirectory())
        {
            imageView.setImageResource(R.drawable.folderopen);
        }
        else
        {
            imageView.setImageResource(R.drawable.book);
        }
        return browserItem;
    }

    public void setCurrentDirectory(File currentDirectory)
    {
        this.currentDirectory = currentDirectory;
        ArrayList<File> files = new ArrayList<File>(Arrays.asList(currentDirectory.listFiles(new FileFilter()
        {
            public boolean accept(File pathname)
            {
                return pathname.isDirectory() || pathname.getName().endsWith(".djvu") || pathname.getName().endsWith(".djv");
            }
        })));
        if (currentDirectory.getParentFile() != null)
        {
            files.add(0, currentDirectory.getParentFile());
        }
        setFiles(files);
    }

    public void setFiles(List<File> files)
    {
        this.files = files;
        notifyDataSetInvalidated();
    }

    public File getCurrentDirectory()
    {
        return currentDirectory;
    }
}
