package com.example.imageeditor.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;

public class BitmapUtils {

    public static Bitmap getBitmapFromUri(Context context, Uri uri) throws IOException {
        InputStream input = context.getContentResolver().openInputStream(uri);

        // Определяем размеры изображения без загрузки в память
        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();

        // Рассчитываем inSampleSize для экономии памяти
        int originalWidth = onlyBoundsOptions.outWidth;
        int originalHeight = onlyBoundsOptions.outHeight;

        // Максимально допустимый размер
        int maxWidth = 2048;
        int maxHeight = 2048;

        int inSampleSize = 1;
        if (originalHeight > maxHeight || originalWidth > maxWidth) {
            final int halfHeight = originalHeight / 2;
            final int halfWidth = originalWidth / 2;

            // Рассчитываем наибольший inSampleSize, который является степенью 2 и сохраняет
            // высоту и ширину больше или равной запрашиваемой высоте и ширине
            while ((halfHeight / inSampleSize) >= maxHeight || (halfWidth / inSampleSize) >= maxWidth) {
                inSampleSize *= 2;
            }
        }

        // Загружаем с оптимальным размером
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = inSampleSize;
        input = context.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
        input.close();

        return bitmap;
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Bitmap flipBitmap(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.preScale(-1.0f, 1.0f);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Bitmap cropBitmap(Bitmap bitmap, int x, int y, int width, int height) {
        return Bitmap.createBitmap(bitmap, x, y, width, height);
    }

    public static Bitmap overlayBitmaps(Bitmap base, Bitmap overlay) {
        Bitmap result = Bitmap.createBitmap(base.getWidth(), base.getHeight(), base.getConfig());
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(base, 0, 0, null);
        canvas.drawBitmap(overlay, 0, 0, null);
        return result;
    }
}