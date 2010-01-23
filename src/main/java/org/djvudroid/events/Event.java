package org.djvudroid.events;

public interface Event<T>
{
    void dispatchOn(Object listener);
}
