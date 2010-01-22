package org.djvudroid.codec;

import android.content.Context;
import org.djvudroid.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipInputStream;

public class LibraryExtracter
{
    private static final String LIBDJVUDROID_SO = "libdjvudroid.so";
    private static boolean extractedToday = false;

    public static void extractCodecLibrary(Context context)
    {
        if (extractedToday)
        {
            return;
        }
        FileOutputStream fileOutputStream = null;
        ZipInputStream zipInputStream = null;
        try
        {
            fileOutputStream = context.openFileOutput(LIBDJVUDROID_SO, Context.MODE_WORLD_READABLE);
            zipInputStream = new ZipInputStream(context.getResources().openRawResource(R.raw.libdjvudroid));
            zipInputStream.getNextEntry();
            final byte[] bytes = new byte[4096];
            int c;
            while (zipInputStream.available() > 0)
            {
                c = zipInputStream.read(bytes);
                if (c < 1)
                {
                    break;
                }
                fileOutputStream.write(bytes, 0, c);
            }
            zipInputStream.closeEntry();
        }
        catch (FileNotFoundException e)
        {
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            try
            {
                if (zipInputStream != null)
                {
                    zipInputStream.close();
                }
                if (fileOutputStream != null)
                {
                    fileOutputStream.close();
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
        System.load(new File(context.getFilesDir(), LIBDJVUDROID_SO).getAbsolutePath());
        extractedToday = true;
    }
}
