package com.example.imageeditor;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.imageeditor.utils.FileUtils;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSIONS_REQUEST = 100;

    private String currentPhotoPath;
    private boolean pendingTakePhoto = false;
    private boolean pendingChoosePhoto = false;

    // Лаунчеры для результатов активностей
    private ActivityResultLauncher<Intent> takePictureLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<String[]> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация кнопок
        Button btnTakePhoto = findViewById(R.id.btnTakePhoto);
        Button btnChoosePhoto = findViewById(R.id.btnChoosePhoto);

        // Инициализация лаунчеров
        initializeActivityResultLaunchers();

        // Обработчики кнопок
        btnTakePhoto.setOnClickListener(v -> {
            Log.d(TAG, "Кнопка 'Сделать фото' нажата");
            pendingTakePhoto = true;
            pendingChoosePhoto = false;
            if (checkPermissions()) {
                dispatchTakePictureIntent();
            }
        });

        btnChoosePhoto.setOnClickListener(v -> {
            Log.d(TAG, "Кнопка 'Выбрать из галереи' нажата");
            pendingChoosePhoto = true;
            pendingTakePhoto = false;
            if (checkPermissions()) {
                openGallery();
            }
        });
    }

    private void initializeActivityResultLaunchers() {
        // Лаунчер для съемки фото
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Log.d(TAG, "Фото успешно снято, путь: " + currentPhotoPath);
                        Uri imageUri = Uri.fromFile(new File(currentPhotoPath));
                        startEditorActivity(imageUri);
                    } else {
                        Log.w(TAG, "Съемка фото отменена или не удалась");
                    }
                });

        // Лаунчер для выбора изображения
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        Log.d(TAG, "Изображение выбрано: " + imageUri);
                        startEditorActivity(imageUri);
                    } else {
                        Log.w(TAG, "Выбор изображения отменен или не удался");
                    }
                });

        // Лаунчер для запроса разрешений
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean allGranted = true;
                    for (Boolean granted : permissions.values()) {
                        if (!granted) {
                            allGranted = false;
                            break;
                        }
                    }
                    if (allGranted) {
                        Log.d(TAG, "Все разрешения предоставлены");
                        // Повторяем действие, которое требовало разрешений
                        if (pendingTakePhoto) {
                            dispatchTakePictureIntent();
                        } else if (pendingChoosePhoto) {
                            openGallery();
                        }
                    } else {
                        Log.w(TAG, "Некоторые разрешения отклонены");
                        Toast.makeText(this, "Для работы приложения нужны разрешения", Toast.LENGTH_LONG).show();
                    }
                    pendingTakePhoto = false;
                    pendingChoosePhoto = false;
                });
    }

    private boolean checkPermissions() {
        String[] permissions;
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            permissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES
            };
        } else {
            permissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            Log.d(TAG, "Запрашиваем разрешения");
            requestPermissionLauncher.launch(permissions);
            return false;
        }
        return true;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = FileUtils.createImageFile(this);
                currentPhotoPath = photoFile.getAbsolutePath();
                Log.d(TAG, "Создан файл для фото: " + currentPhotoPath);
            } catch (IOException ex) {
                Log.e(TAG, "Ошибка создания файла изображения", ex);
                Toast.makeText(this, "Ошибка создания файла изображения", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri photoURI = FileProvider.getUriForFile(this,
                    "com.example.imageeditor.fileprovider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            takePictureLauncher.launch(takePictureIntent);
        } else {
            Log.w(TAG, "Нет приложения для камеры");
            Toast.makeText(this, "Камера недоступна", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void startEditorActivity(Uri imageUri) {
        if (imageUri != null) {
            Intent editorIntent = new Intent(this, EditorActivity.class);
            editorIntent.putExtra("imageUri", imageUri.toString());
            startActivity(editorIntent);
        } else {
            Log.e(TAG, "URI изображения null");
            Toast.makeText(this, "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show();
        }
    }
}