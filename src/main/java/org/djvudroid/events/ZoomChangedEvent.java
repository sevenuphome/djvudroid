package org.djvudroid.events;

public class ZoomChangedEvent extends SafeEvent<ZoomListener>
{
    private final float newZoom;

    public ZoomChangedEvent(float newZoom)
    {
        this.newZoom = newZoom;
    }

    @Override
    public void dispatchSafely(ZoomListener listener)
    {
        listener.zoomChanged(newZoom);
    }
}
