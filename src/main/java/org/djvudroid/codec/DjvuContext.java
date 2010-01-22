package org.djvudroid.codec;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

public class DjvuContext implements Runnable
{
    private final long contextHandle;
    private ContentResolver contentResolver;
    private static final int BUFFER_SIZE = 32768;
    private static final String DJVU_DROID_CODEC_LIBRARY = "DjvuDroidCodecLibrary";
    private final HashMap<String, Semaphore> urlToSemaphore = new HashMap<String, Semaphore>();
    private final Object waitObject = new Object();

    public DjvuContext()
    {
        this.contextHandle = create();
        new Thread(this).start();
    }

    public DjvuDocument openDocument(Uri uri)
    {
        final Semaphore semaphore = new Semaphore(0);
        urlToSemaphore.put(uri.toString(), semaphore);
        return DjvuDocument.openDocument(uri, this, semaphore, waitObject);
    }

    long getContextHandle()
    {
        return contextHandle;
    }

    public void run()
    {
        for(;;)
        {
            try
            {
                handleMessage(contextHandle);
                synchronized (waitObject)
                {
                    waitObject.notifyAll();
                }
            }
            catch (Exception e)
            {
                Log.e(DJVU_DROID_CODEC_LIBRARY, "Codec error", e);
            }
        }
    }

    /**
     * Called from JNI
     * @param uri uri to load from
     * @param streamId inner stream id
     * @param docHandle document handle to submit data to
     */
    @SuppressWarnings({"UnusedDeclaration"})
    private void handleNewStream(final String uri, final int streamId, final long docHandle)
    {
        Log.d(DJVU_DROID_CODEC_LIBRARY, "Starting data submit for: " + uri);
        InputStream inputStream = null;
        try
        {
            final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
            inputStream = contentResolver.openInputStream(Uri.parse(uri));
            if (inputStream instanceof FileInputStream)
            {
                fileStreamWrite(streamId, docHandle, buffer, (FileInputStream) inputStream);
            }
            else
            {
                genericStreamWrite(streamId, docHandle, inputStream, buffer);
            }
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
            if (inputStream != null)
            {
                try
                {
                    streamClose(docHandle, streamId, false);
                    inputStream.close();
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
        Log.d(DJVU_DROID_CODEC_LIBRARY, "Data submit finished for: " + uri);
        urlToSemaphore.remove(uri).release();
    }

    private void fileStreamWrite(int streamId, long docHandle, ByteBuffer buffer, FileInputStream fileInputStream)
            throws IOException
    {
        final FileChannel channel = fileInputStream.getChannel();
        int c;
        while ((c = channel.read(buffer)) != -1)
        {
            streamWrite(docHandle, streamId, buffer, c);
            buffer.rewind();
        }
    }

    private void genericStreamWrite(int streamId, long docHandle, InputStream inputStream, ByteBuffer buffer)
            throws IOException
    {
        int c;
        final byte[] bytes = new byte[BUFFER_SIZE];
        while ((c = inputStream.read(bytes)) != -1)
        {
            buffer.rewind();
            buffer.put(bytes, 0, c);
            streamWrite(docHandle, streamId, buffer, c);
        }
    }

    public void setContentResolver(ContentResolver contentResolver)
    {
        this.contentResolver = contentResolver;
    }

    @Override
    protected void finalize() throws Throwable
    {
        free(contextHandle);
        super.finalize();
    }

    private static native long create();
    private static native void free(long contextHandle);
    private native void handleMessage(long contextHandle);
    private static native void streamWrite(long docHandle, int streamId, Buffer buffer, int dataLen);
    private static native void streamClose(long docHandle, int streamId, boolean stop);
}
