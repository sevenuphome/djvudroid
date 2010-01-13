package org.djvudroid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class BitmapsCacheService
{
    private final Context context;
    private final MessageDigest md5;

    public BitmapsCacheService(Context context)
    {
        this.context = context;
        try
        {
            md5 = MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException(e);
        }
    }

    public Bitmap cachedBitmapFor(Uri djvuUri, int pageNum, int targetWidth)
    {
        final String s = composeBitmapFileName(djvuUri, pageNum, targetWidth);
        FileInputStream inputStream = null;
        try
        {
            inputStream = context.openFileInput(s);
            return BitmapFactory.decodeStream(inputStream);
        }
        catch (FileNotFoundException e)
        {
            return null;
        }
        finally
        {
            if (inputStream != null)
            {
                try
                {
                    inputStream.close();
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private String composeBitmapFileName(Uri djvuUri, int pageNum, int targetWidth)
    {
        final String keyString = djvuUri + "_" + pageNum + "_" + targetWidth;
        final byte[] bytes = md5.digest(keyString.getBytes());
        final StringBuilder builder = new StringBuilder();
        for (byte aByte : bytes)
        {
            builder.append(Integer.toString((int)aByte, 16)).append("_");
        }
        return builder.toString();
    }

    public void cacheBitmapFor(Uri djvuUri, int pageNum, int targetWidth, Bitmap bitmap)
    {
        FileOutputStream fileOutputStream = null;
        try
        {
            fileOutputStream = context.openFileOutput(composeBitmapFileName(djvuUri, pageNum, targetWidth), Context.MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
        }
        catch (FileNotFoundException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            if (fileOutputStream != null)
            {
                try
                {
                    fileOutputStream.close();
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
