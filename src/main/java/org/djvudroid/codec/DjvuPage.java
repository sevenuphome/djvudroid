package org.djvudroid.codec;

import android.graphics.Bitmap;

import java.nio.Buffer;
import java.nio.ByteBuffer;

public class DjvuPage
{
    private final long pageHandle;
    private final Object waitObject;

    DjvuPage(long pageHandle, Object waitObject)
    {
        this.pageHandle = pageHandle;
        this.waitObject = waitObject;
    }

    public boolean isDecoding()
    {
        return !isDecodingDone(pageHandle);
    }

    private static native int getWidth(long pageHandle);

    private static native int getHeight(long pageHandle);

    private static native boolean isDecodingDone(long pageHandle);

    private static native boolean renderPage(long pageHandle, int targetWidth, int targetHeight, Buffer buffer);

    private static native void free(long pageHandle);

    public void waitForDecode()
    {
        synchronized (waitObject)
        {
            try
            {
                waitObject.wait(200);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public int getWidth()
    {
        return getWidth(pageHandle);
    }

    public int getHeight()
    {
        return getHeight(pageHandle);
    }

    public Bitmap renderBitmap(int width, int height)
    {
        final ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 2);
        renderPage(pageHandle, width, height, buffer);
        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        bitmap.copyPixelsFromBuffer(buffer);
        return bitmap;
    }

    @Override
    protected void finalize() throws Throwable
    {
        free(pageHandle);
        super.finalize();
    }
}
