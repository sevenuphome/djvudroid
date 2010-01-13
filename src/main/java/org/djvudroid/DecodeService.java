package org.djvudroid;

import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import com.lizardtech.djvu.DjVuPage;
import com.lizardtech.djvu.Document;
import com.lizardtech.djvu.GMap;
import com.lizardtech.djvu.GRect;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DecodeService
{
    private View containerView;
    private Document document;
    private GMap map;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    public static final String DJVU_DROID = "DjvuDroid";
    private final Map<Integer, Future<?>> decodingFutures = new ConcurrentHashMap<Integer, Future<?>>();
    private BitmapsCacheService bitmapsCacheService;
    private Uri documentUri;

    public void setBitmapsCacheService(BitmapsCacheService bitmapsCacheService)
    {
        this.bitmapsCacheService = bitmapsCacheService;
    }

    public void setContainerView(View containerView)
    {
        this.containerView = containerView;
    }

    public void open(InputStream inputStream, Uri fileUri)
    {
        documentUri = fileUri;
        document = new Document();
        try
        {
            document.read(inputStream);
            document.setAsync(true);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void decodePage(int pageNum, final ImageView imageView)
    {
        decodePage(pageNum, new DecodeCallback()
        {
            public void decodeComplete(final Bitmap bitmap)
            {
                imageView.post(new Runnable()
                {
                    public void run()
                    {
                        imageView.setImageBitmap(bitmap);
                    }
                });
            }
        });
    }

    public void decodePage(int pageNum, final DecodeCallback decodeCallback)
    {
        final DecodeTask decodeTask = new DecodeTask(pageNum, decodeCallback);
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
        final Bitmap cachedBitmap = bitmapsCacheService.cachedBitmapFor(documentUri, currentDecodeTask.pageNumber, getTargetWidth());
        if (cachedBitmap != null)
        {
            Log.d(DJVU_DROID, "Found cached bitmap for " + currentDecodeTask.pageNumber);
            finishDecoding(currentDecodeTask, cachedBitmap);
            return;
        }
        DjVuPage vuPage = document.getPage(currentDecodeTask.pageNumber, Document.MAX_PRIORITY, false);
        preloadNextPage(currentDecodeTask.pageNumber);

        while (vuPage.isDecoding())
        {
            if (isTaskDead(currentDecodeTask))
            {
                break;
            }
            waitForDecode(vuPage);
        }
        Log.d(DJVU_DROID, "Starting map update");
        updateMap(vuPage);
        Log.d(DJVU_DROID, "Map update finished");
        if (map == null)
        {
            return;
        }
        if (isTaskDead(currentDecodeTask))
        {
            return;
        }
        Log.d(DJVU_DROID, "Starting converting map to bitmap");
        final Bitmap bitmap = convertMapToBitmap();
        Log.d(DJVU_DROID, "Converting map to bitmap finished");
        if (isTaskDead(currentDecodeTask))
        {
            return;
        }
        bitmapsCacheService.cacheBitmapFor(documentUri, currentDecodeTask.pageNumber, getTargetWidth(), bitmap);
        finishDecoding(currentDecodeTask, bitmap);
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
        document.getPage(nextPage, Document.MIN_PRIORITY, false);
    }

    private void waitForDecode(DjVuPage vuPage)
    {
        vuPage.waitForCodec(vuPage.progressiveLock, 200);
    }

    private void updateMap(DjVuPage vuPage)
    {
        if (vuPage.getInfo() == null)
        {
            return;
        }
        int subsample = calculateSubsample(vuPage);
        GRect rect = calculateRect(vuPage, subsample);
        map = vuPage.getMap(rect, subsample, map);
    }

    private int calculateSubsample(DjVuPage vuPage)
    {
        return calculateSubsample(vuPage, getTargetWidth());
    }

    private int getTargetWidth()
    {
        return containerView.getWidth();
    }

    public GRect getTargetRect()
    {
        try
        {
            return calculateRect(document.getPage(0, Document.MAX_PRIORITY, false));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private GRect calculateRect(DjVuPage vuPage)
    {
        return calculateRect(vuPage, calculateSubsample(vuPage));
    }

    private GRect calculateRect(DjVuPage vuPage, int subsample)
    {
        return new GRect(0, 0, vuPage.getInfo().width / subsample, vuPage.getInfo().height / subsample);
    }

    private int calculateSubsample(DjVuPage vuPage, int targetWidth)
    {
        return (int) Math.ceil(vuPage.getInfo().width * 1.0 / targetWidth);
    }

    private void updateImage(final DecodeTask currentDecodeTask, Bitmap bitmap)
    {
        currentDecodeTask.decodeCallback.decodeComplete(bitmap);
    }

    private Bitmap convertMapToBitmap()
    {
        return GMapToBitmap.convert(map, null);
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
        return document.size();
    }

    public void freeBitmap(Bitmap bitmap)
    {
        bitmap.recycle();
        Log.d(DJVU_DROID, "Bitmap freed: " + bitmap);
    }

    private class DecodeTask
    {
        private final int pageNumber;
        private final DecodeCallback decodeCallback;

        private DecodeTask(int pageNumber, DecodeCallback decodeCallback)
        {
            this.pageNumber = pageNumber;
            this.decodeCallback = decodeCallback;
        }
    }

    public interface DecodeCallback
    {
        void decodeComplete(Bitmap bitmap);
    }
}
