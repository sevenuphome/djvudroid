package org.djvudroid.events;

public class BringUpZoomControlsEvent extends SafeEvent<BringUpZoomControlsListener>
{
    @Override
    public void dispatchSafely(BringUpZoomControlsListener listener)
    {
        listener.bringUpZoomControls();
    }
}
