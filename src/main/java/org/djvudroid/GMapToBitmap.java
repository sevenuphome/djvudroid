package org.djvudroid;

import android.graphics.Bitmap;
import com.lizardtech.djvu.GMap;

public class GMapToBitmap
{
    public static Bitmap convert(GMap map, Bitmap target)
    {
        int height = map.rows();
        int width = map.columns();
        if (target == null || target.getWidth() != width || target.getHeight() != height)
        {
            target = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        }
        int[] colors = new int[width];
        for (int y = 0; y < height; y++)
        {
            map.fillRGBPixels(0, y, width, 1, colors, 0, width);
            target.setPixels(colors, 0, width, 0, y, width, 1);
        }
        return target;
    }
}
