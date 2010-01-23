package org.djvudroid;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import org.djvudroid.codec.DjvuContext;
import org.djvudroid.codec.DjvuDocument;
import org.djvudroid.codec.DjvuPage;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DecodeService
{
    private final DjvuContext djvuContext;

    private View containerView;
    private DjvuDocument document;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    public static final String DJVU_DROID = "DjvuDroid";
    private final Map<Integer, Future<?>> decodingFutures = new ConcurrentHashMap<Integer, Future<?>>();

    public DecodeService()
    {
        djvuContext = new DjvuContext();
    }

    public void setContentResolver(ContentResolver contentResolver)
    {
        djvuContext.setContentResolver(contentResolver);
    }

    public void setContainerView(View containerView)
    {
        this.containerView = containerView;
    }

    public void open(Uri fileUri)
    {
        document = djvuContext.openDocument(fileUri);
    }

    public void decodePage(int pageNum, final DecodeCallback decodeCallback, float zoom)
    {
        final DecodeTask decodeTask = new DecodeTask(pageNum, decodeCallback, zoom);
        synchronized (decodingFutures)
        {
            final Future<?> future = executorService.submit(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        performDecode(decodeTask);
                    }
                    catch (IOException e)
                    {
                        Log.e(DJVU_DROID, "Decode fail", e);
                    }
                }
            });
            final Future<?> removed = decodingFutures.put(pageNum, future);
            if (removed != null)
            {
                removed.cancel(false);
            }
        }
    }

    public void stopDecoding(int pageNum)
    {
        final Future<?> future = decodingFutures.remove(pageNum);
        if (future != null)
        {
            future.cancel(false);
        }
    }

    private void performDecode(DecodeTask currentDecodeTask)
            throws IOException
    {
        if (isTaskDead(currentDecodeTask))
        {
            Log.d(DJVU_DROID, "Skipping decode task for page " + currentDecodeTask.pageNumber);
            return;
        }
        Log.d(DJVU_DROID, "Starting decode of page: " + currentDecodeTask.pageNumber);
        DjvuPage vuPage = document.getPage(currentDecodeTask.pageNumber);
        preloadNextPage(currentDecodeTask.pageNumber);

        while (vuPage.isDecoding())
        {
            if (isTaskDead(currentDecodeTask))
            {
                break;
            }
            waitForDecode(vuPage);
        }
        if (isTaskDead(currentDecodeTask))
        {
            return;
        }
        Log.d(DJVU_DROID, "Start converting map to bitmap");
        float scale = calculateScale(vuPage) * currentDecodeTask.zoom;
        final Bitmap bitmap = vuPage.renderBitmap(getScaledWidth(vuPage, scale), getScaledHeight(vuPage, scale));
        Log.d(DJVU_DROID, "Converting map to bitmap finished");
        if (isTaskDead(currentDecodeTask))
        {
            return;
        }
        finishDecoding(currentDecodeTask, bitmap);
    }

    private int getScaledHeight(DjvuPage vuPage, float scale)
    {
        return (int) (scale * vuPage.getHeight());
    }

    private int getScaledWidth(DjvuPage vuPage, float scale)
    {
        return (int) (scale * vuPage.getWidth());
    }

    private float calculateScale(DjvuPage djvuPage)
    {
        return 1.0f * getTargetWidth() / djvuPage.getWidth();
    }

    private void finishDecoding(DecodeTask currentDecodeTask, Bitmap bitmap)
    {
        updateImage(currentDecodeTask, bitmap);
        stopDecoding(currentDecodeTask.pageNumber);
    }

    private void preloadNextPage(int pageNumber) throws IOException
    {
        final int nextPage = pageNumber + 1;
        if (nextPage >= getPageCount())
        {
            return;
        }
        document.getPage(nextPage);
    }

    private void waitForDecode(DjvuPage vuPage)
    {
        vuPage.waitForDecode();
    }

    private int getTargetWidth()
    {
        return containerView.getWidth();
    }

    public int getEffectivePagesWidth()
    {
        final DjvuPage page = document.getPage(0);
        return getScaledWidth(page, calculateScale(page));
    }

    public int getEffectivePagesHeight()
    {
        final DjvuPage page = document.getPage(0);
        return getScaledHeight(page, calculateScale(page));
    }

    private void updateImage(final DecodeTask currentDecodeTask, Bitmap bitmap)
    {
        currentDecodeTask.decodeCallback.decodeComplete(bitmap);
    }

    private boolean isTaskDead(DecodeTask currentDecodeTask)
    {
        synchronized (decodingFutures)
        {
            return !decodingFutures.containsKey(currentDecodeTask.pageNumber);
        }
    }

    public int getPageCount()
    {
        return document.getPageCount();
    }

    private class DecodeTask
    {
        private final int pageNumber;
        private final float zoom;
        private final DecodeCallback decodeCallback;

        private DecodeTask(int pageNumber, DecodeCallback decodeCallback, float zoom)
        {
            this.pageNumber = pageNumber;
            this.decodeCallback = decodeCallback;
            this.zoom = zoom;
        }
    }

    public interface DecodeCallback
    {
        void decodeComplete(Bitmap bitmap);
    }
}
