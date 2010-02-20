package org.djvudroid;

import android.content.Context;
import android.content.SharedPreferences;

public class ViewerPreferences
{
    private SharedPreferences sharedPreferences;
    private static final String FULL_SCREEN = "FullScreen";

    public ViewerPreferences(Context context)
    {
        sharedPreferences = context.getSharedPreferences("ViewerPreferences", 0);
    }

    public void setFullScreen(boolean fullscreen)
    {
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(FULL_SCREEN, fullscreen);
        editor.commit();
    }

    public boolean isFullScreen()
    {
        return sharedPreferences.getBoolean(FULL_SCREEN, false);
    }
}
