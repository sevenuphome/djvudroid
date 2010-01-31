package org.djvudroid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.*;
import android.widget.*;
import org.djvudroid.events.ZoomListener;
import org.djvudroid.models.ZoomModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DjvuDocumentView extends ScrollView implements ZoomListener
{
    private final ZoomModel zoomModel;
    private DecodeService decodeService;
    private final Map<Integer, FrameLayout> pages = new HashMap<Integer, FrameLayout>();
    private final Map<Integer, Bitmap> visiblePageNumToBitmap = new HashMap<Integer, Bitmap>();
    private final Set<Integer> decodingPageNums = new HashSet<Integer>();
    private boolean isInitialized = false;
    private int pageToGoTo;
    private float lastX;
    private VelocityTracker velocityTracker;
    private final Scroller scroller;

    public DjvuDocumentView(Context context, ZoomModel zoomModel)
    {
        super(context);
        this.zoomModel = zoomModel;
        initLayout();
        setKeepScreenOn(true);
        scroller = new Scroller(getContext());
    }

    public void setDecodeService(DecodeService decodeService)
    {
        this.decodeService = decodeService;
    }

    private void init()
    {
        if (isInitialized)
        {
            return;
        }
        final LinearLayout linearLayout = getMainLayout();
        final int width = decodeService.getEffectivePagesWidth();
        final int height = decodeService.getEffectivePagesHeight();
        for (int i = 0; i < decodeService.getPageCount(); i++)
        {
            addPageToMainLayoutIfNotAvailable(linearLayout, width, height, i);
        }
        goToPageImpl(pageToGoTo);
        isInitialized = true;
    }

    private void addPageToMainLayoutIfNotAvailable(LinearLayout mainLayout, int width, int height, int pageIndex)
    {
        if (pages.containsKey(pageIndex))
        {
            return;
        }
        final FrameLayout frameLayout = new FrameLayout(getContext());
        frameLayout.setLayoutParams(new LayoutParams(width, height));
        frameLayout.addView(createPageNumView(pageIndex));
        pages.put(pageIndex, frameLayout);
        mainLayout.addView(frameLayout);
    }

    private LinearLayout getMainLayout()
    {
        return (LinearLayout) findViewWithTag(LinearLayout.class);
    }

    private LinearLayout initLayout()
    {
        final LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        linearLayout.setTag(LinearLayout.class);
        addView(linearLayout);
        return linearLayout;
    }

    private void goToPageImpl(final int toPage)
    {
        post(new Runnable()
        {
            public void run()
            {
                scrollTo(0, pages.get(toPage).getTop());
            }
        });
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt)
    {
        super.onScrollChanged(l, t, oldl, oldt);
        stopDecodingInvisiblePages();
        removeImageFromInvisiblePages();
        startDecodingVisiblePages();
    }

    private void startDecodingVisiblePages()
    {
        startDecodingVisiblePages(false);
    }

    private void startDecodingVisiblePages(boolean invalidate)
    {
        for (final Map.Entry<Integer, FrameLayout> pageNumToPage : pages.entrySet())
        {
            final FrameLayout page = pageNumToPage.getValue();
            if (isPageVisible(page))
            {
                final Integer pageNum = pageNumToPage.getKey();
                if (visiblePageNumToBitmap.containsKey(pageNum) && !invalidate)
                {
                    continue;
                }
                decodePage(pageNum);
            }
        }
    }

    private void removeImageFromInvisiblePages()
    {
        for (Integer visiblePageNum : new HashMap<Integer, Bitmap>(visiblePageNumToBitmap).keySet())
        {
            if (!isPageVisible(pages.get(visiblePageNum)))
            {
                removeImageFromPage(visiblePageNum);
            }
        }
    }

    private void stopDecodingInvisiblePages()
    {
        for (Integer decodingPageNum : new HashSet<Integer>(decodingPageNums))
        {
            if (!isPageVisible(pages.get(decodingPageNum)))
            {
                stopDecodingPage(decodingPageNum);
            }
        }
    }

    private void stopDecodingAllPages()
    {
        for (Integer decodingPageNum : new HashSet<Integer>(decodingPageNums))
        {
            stopDecodingPage(decodingPageNum);
        }
    }

    private void stopDecodingPage(Integer decodingPageNum)
    {
        decodeService.stopDecoding(decodingPageNum);
        removeDecodingStatus(decodingPageNum);
    }

    private void decodePage(final Integer pageNum)
    {
        if (decodingPageNums.contains(pageNum))
        {
            return;
        }
        addPageToMainLayoutIfNotAvailable(getMainLayout(), getWidth(), getHeight(), pageNum);
        setDecodingStatus(pageNum);
        decodeService.decodePage(pageNum, new DecodeService.DecodeCallback()
        {
            public void decodeComplete(final Bitmap bitmap)
            {
                post(new Runnable()
                {
                    public void run()
                    {
                        submitBitmap(pageNum, bitmap);
                    }
                });
            }
        }, zoomModel.getZoom());
    }

    private void setDecodingStatus(Integer pageNum)
    {
        if (!decodingPageNums.contains(pageNum) && pages.containsKey(pageNum))
        {
            final ProgressBar bar = new ProgressBar(getContext());
            bar.setIndeterminate(true);
            bar.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
            bar.setTag(ProgressBar.class);
            pages.get(pageNum).addView(bar);
        }
        decodingPageNums.add(pageNum);
    }

    private void removeDecodingStatus(Integer decodingPageNum)
    {
        if (decodingPageNums.contains(decodingPageNum) && pages.containsKey(decodingPageNum))
        {
            final FrameLayout page = pages.get(decodingPageNum);
            page.removeView(page.findViewWithTag(ProgressBar.class));
        }
        decodingPageNums.remove(decodingPageNum);
    }

    private boolean isPageVisible(FrameLayout page)
    {
        return page.getGlobalVisibleRect(new Rect());
    }

    private void submitBitmap(Integer pageNum, Bitmap bitmap)
    {
        removeImageFromPage(pageNum);
        addImageToPage(pageNum, bitmap);
        removeDecodingStatus(pageNum);
    }

    private void addImageToPage(Integer pageNum, Bitmap bitmap)
    {
        init();
        final ImageView imageView = createImageView(bitmap);
        final FrameLayout page = pages.get(pageNum);
        page.addView(imageView);
        page.setLayoutParams(new LinearLayout.LayoutParams(bitmap.getWidth(), bitmap.getHeight()));
        visiblePageNumToBitmap.put(pageNum, bitmap);
    }

    private void removeImageFromPage(Integer fromPage)
    {
        final FrameLayout page = pages.get(fromPage);
        final View imageView = page.findViewWithTag(ImageView.class);
        if (imageView == null)
        {
            return;
        }
        page.removeView(imageView);
        final Bitmap bitmap = visiblePageNumToBitmap.remove(fromPage);
        bitmap.recycle();
    }

    private ImageView createImageView(Bitmap bitmap)
    {
        final ImageView imageView = new ImageView(getContext());
        imageView.setImageBitmap(bitmap);
        imageView.setTag(ImageView.class);
        return imageView;
    }

    private TextView createPageNumView(int i)
    {
        TextView pageNumTextView = new TextView(getContext());
        pageNumTextView.setText("Page " + (i + 1));
        pageNumTextView.setTextSize(32);
        pageNumTextView.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        pageNumTextView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        return pageNumTextView;
    }

    public void showDocument()
    {
        // use post to ensure that document view has width and height before decoding begin
        post(new Runnable()
        {
            public void run()
            {
                decodePage(0);
            }
        });
    }

    public void goToPage(int toPage)
    {
        if (isInitialized)
        {
            goToPageImpl(toPage);
        }
        else
        {
            pageToGoTo = toPage;
        }
    }

    public int getCurrentPage()
    {
        for (Map.Entry<Integer, FrameLayout> entry : pages.entrySet())
        {
            if (isPageVisible(entry.getValue()))
            {
                return entry.getKey();
            }
        }
        return 0;
    }

    public void zoomChanged(float newZoom, float oldZoom)
    {
        stopDecodingAllPages();
        startDecodingVisiblePages(true);
        final float ratio = newZoom / oldZoom;
        final int halfWidth = getWidth() / 2;
        scrollTo((int) (ratio * (getScrollX() + halfWidth) - halfWidth), getScrollY());
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev)
    {
        zoomModel.bringUpZoomControls();

        final boolean b = super.onTouchEvent(ev);
        if (!zoomModel.isHorizontalScrollEnabled())
        {
            return b;
        }

        if (velocityTracker == null)
        {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(ev);

        switch (ev.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                if (!scroller.isFinished())
                {
                    scroller.abortAnimation();
                }
                lastX = ev.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                final int delta = (int) (lastX - ev.getX());
                scrollBy(delta, 0);
                lastX = ev.getX();
                break;
            case MotionEvent.ACTION_UP:
                velocityTracker.computeCurrentVelocity(1000);
                scroller.fling(getScrollX(), 0, (int) -velocityTracker.getXVelocity(), 0, 0, getMainLayout().getWidth(), 0, 0);
                velocityTracker.recycle();
                velocityTracker = null;
                break;
        }
        return true;
    }

    @Override
    public void computeScroll()
    {
        // save scrollX as it killed by scroller inside ScrollView.computeScroll()
        final int scrollX = getScrollX();
        super.computeScroll();
        if (scroller.computeScrollOffset())
        {
            scrollTo(scroller.getCurrX(), getScrollY());
        }
        else
        {
            scrollTo(scrollX, getScrollY());
        }
    }

    @Override
    protected void measureChild(View child, int parentWidthMeasureSpec, int parentHeightMeasureSpec)
    {
        super.measureChild(child, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), parentHeightMeasureSpec);
    }

    @Override
    protected void measureChildWithMargins(View child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed)
    {
        super.measureChildWithMargins(child, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), widthUsed, parentHeightMeasureSpec, heightUsed);
    }
}
