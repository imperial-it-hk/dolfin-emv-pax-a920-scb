package th.co.bkkps.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;

public class BitmapUtils {
    public static Bitmap clearBlank(Bitmap bp, int blank, int backColor) {
        int height = bp.getHeight();
        int width = bp.getWidth();

        int newBlank = blank < 0 ? 0 : blank;

        int top = getTopBorder(bp, width, height, newBlank, backColor);
        int bottom = getBottomBorder(bp, width, height, newBlank, backColor);
        int left = getLeftBorder(bp, width, height, newBlank, backColor);
        int right = getRightBorder(bp, width, height, newBlank, backColor);

        return Bitmap.createBitmap(bp, left, top, right - left, bottom - top);
    }


    private static int getTopBorder(Bitmap bp, int width, int height, int blank, int backColor) {
        int[] pixels = new int[width];
        for (int y = 0; y < height; y++) {
            bp.getPixels(pixels, 0, width, 0, y, width, 1);
            for (int pix : pixels) {
                if (pix != backColor) {
                    return y - blank > 0 ? y - blank : 0;
                }
            }
        }
        return 0;
    }

    private static int getBottomBorder(Bitmap bp, int width, int height, int blank, int backColor) {
        int[] pixels = new int[width];
        for (int y = height - 1; y >= 0; y--) {
            bp.getPixels(pixels, 0, width, 0, y, width, 1);
            for (int pix : pixels) {
                if (pix != backColor) {
                    return y + blank > height - 1 ? height - 1 : y + blank;
                }
            }
        }
        return height - 1;
    }

    private static int getLeftBorder(Bitmap bp, int width, int height, int blank, int backColor) {
        int[] pixels = new int[height];
        for (int x = 0; x < width; x++) {
            bp.getPixels(pixels, 0, 1, x, 0, 1, height);
            for (int pix : pixels) {
                if (pix != backColor) {
                    return x - blank > 0 ? x - blank : 0;
                }
            }
        }
        return 0;
    }

    private static int getRightBorder(Bitmap bp, int width, int height, int blank, int backColor) {
        int[] pixels = new int[height];
        for (int x = width - 1; x > 0; x--) {
            bp.getPixels(pixels, 0, 1, x, 0, 1, height);
            for (int pix : pixels) {
                if (pix != backColor) {
                    return x + blank > width - 1 ? width - 1 : x + blank;
                }
            }
        }
        return width - 1;
    }

    public static Bitmap addOverlayToCenter(Bitmap overlayBitmap, Bitmap baseBitmap) {

        int bitmap2Width = overlayBitmap.getWidth();
        int bitmap2Height = overlayBitmap.getHeight();
        float marginLeft = (float)(baseBitmap.getWidth() * 0.5 - bitmap2Width * 0.5);
        float marginTop = (float)(baseBitmap.getHeight() * 0.5 - bitmap2Height * 0.5);
        Canvas canvas = new Canvas(baseBitmap);
        canvas.drawBitmap(baseBitmap, new Matrix(), null);
        canvas.drawBitmap(overlayBitmap, marginLeft, marginTop, null);
        return baseBitmap;
    }
}
