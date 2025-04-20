    package com.example.imageeditor.utils;

    import android.content.Context;
    import android.os.Environment;

    import java.io.File;
    import java.io.IOException;
    import java.text.SimpleDateFormat;
    import java.util.Date;
    import java.util.Locale;

    public class FileUtils {

        public static File createImageFile(Context context) throws IOException {
            // Создает временный файл для сохранения фото с камеры
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            return File.createTempFile(
                    imageFileName,
                    ".jpg",
                    storageDir
            );
        }

        public static File getOutputMediaFile(Context context) {
            // Создает директорию для сохранения отредактированных изображений
            File mediaStorageDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "ImageEditor");

            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    return null;
                }
            }

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            return new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
        }
    }