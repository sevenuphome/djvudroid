package org.djvudroid.models;

import org.djvudroid.events.BringUpZoomControlsEvent;
import org.djvudroid.events.EventDispatcher;
import org.djvudroid.events.ZoomChangedEvent;

public class ZoomModel extends EventDispatcher
{
    private float zoom = 1.0f;
    private static final float INCREMENT_DELTA = 0.05f;

    public void setZoom(float zoom)
    {
        if (this.zoom != zoom)
        {
            this.zoom = zoom;
            dispatch(new ZoomChangedEvent(zoom));
        }
    }

    public float getZoom()
    {
        return zoom;
    }

    public void increaseZoom()
    {
        setZoom(getZoom() + INCREMENT_DELTA);
    }

    public void decreaseZoom()
    {
        setZoom(getZoom() - INCREMENT_DELTA);
    }

    public void bringUpZoomControls()
    {
        dispatch(new BringUpZoomControlsEvent());
    }
}
