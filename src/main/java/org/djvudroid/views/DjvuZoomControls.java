package org.djvudroid.views;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ToggleButton;
import android.widget.ZoomControls;
import org.djvudroid.events.BringUpZoomControlsListener;
import org.djvudroid.models.ZoomModel;

public class DjvuZoomControls extends ZoomControls implements BringUpZoomControlsListener
{
    private int atomicShowCounter = 0;
    public DjvuZoomControls(Context context, final ZoomModel zoomModel)
    {
        super(context);
        hide();
        setOnZoomInClickListener(new OnClickListener()
        {
            public void onClick(View view)
            {
                zoomModel.increaseZoom();
            }
        });
        setOnZoomOutClickListener(new OnClickListener()
        {
            public void onClick(View view)
            {
                zoomModel.decreaseZoom();
            }
        });
        final ToggleButton button = new ToggleButton(context);
        button.setTextOn("Scroll On");
        final String s = "Scroll Off";
        button.setTextOff(s);
        button.setText(s);
        button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            public void onCheckedChanged(CompoundButton compoundButton, boolean b)
            {
                zoomModel.setHorizontalScrollEnabled(b);
            }
        });
        addView(button, 0);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
        final boolean inControlsArea = ev.getX() > getLeft() && ev.getX() < getRight() &&
                ev.getY() > getTop() && ev.getY() < getBottom();
        if (inControlsArea)
        {
            bringUpZoomControls();
        }
        return !inControlsArea;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        return false;
    }

    public void bringUpZoomControls()
    {
        if (atomicShowCounter == 0)
        {
            show();
        }
        final int currentCounter = ++atomicShowCounter;
        postDelayed(new Runnable()
        {
            public void run()
            {
                if (currentCounter != atomicShowCounter)
                {
                    return;
                }
                hide();
                atomicShowCounter = 0;
            }
        }, 2000);
    }
}
