package com.example.banking;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ekyc extends AppCompatActivity {

    private String type;

    private PreviewView viewFinder;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    String userId = SessionManager.getInstance().getUserId();
    private boolean hasCaptured = false; // tránh chụp nhiều lần

    private static final int REQUEST_CAMERA_PERMISSION = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ekyc);

        viewFinder = findViewById(R.id.viewFinder);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Kiểm tra quyền CAMERA
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        } else {
            startCamera();
        }

        Intent getIntent = getIntent();
        type = getIntent.getStringExtra("type");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this,
                        "Ứng dụng cần quyền CAMERA để quét khuôn mặt",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();

                // ML Kit Face Detection
                FaceDetectorOptions options =
                        new FaceDetectorOptions.Builder()
                                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                                .build();
                FaceDetector detector = FaceDetection.getClient(options);

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    @SuppressLint("UnsafeOptInUsageError")
                    android.media.Image mediaImage = imageProxy.getImage();
                    if (mediaImage != null) {
                        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                        InputImage image = InputImage.fromMediaImage(mediaImage, rotationDegrees);

                        detector.process(image)
                                .addOnSuccessListener(faces -> {
                                    for (Face face : faces) {
                                        if (face.getBoundingBox().width() > 100 && face.getBoundingBox().height() > 100) {
                                            if (!hasCaptured) {
                                                hasCaptured = true;
                                                // Delay 3 giây trước khi chụp
                                                new android.os.Handler(Looper.getMainLooper()).postDelayed(() -> {
                                                    captureFace();
                                                }, 3000);
                                            }
                                        }
                                    }

                                })
                                .addOnFailureListener(e ->
                                        Log.e("EKYC", "Lỗi nhận diện: " + e.getMessage()))
                                .addOnCompleteListener(task -> imageProxy.close());
                    } else {
                        imageProxy.close();
                    }
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);

            } catch (Exception e) {
                Log.e("Ekyc", "Không thể mở camera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void captureFace() {
        File photoFile = new File(getExternalFilesDir(null), "face.jpg");
        ImageCapture.OutputFileOptions options =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(options, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        if("create".equalsIgnoreCase(type)){
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("faceImagePath", photoFile.getAbsolutePath());
                            setResult(RESULT_OK, resultIntent);
                            finish();
                        }
                        else if("confirm".equalsIgnoreCase(type)){
                            try {
                                final List<Float> newFaceEmbedding = extractFaceEmbedding(ekyc.this, photoFile.getAbsolutePath());
                                confirm_face(newFaceEmbedding);
                            } catch (IOException e) {
                                Toast.makeText(ekyc.this, "Lỗi trích xuất embedding: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }

                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(ekyc.this, "Lỗi chụp ảnh", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Hàm so sánh bằng Euclidean distance
    private double euclideanDistance(List<Float> v1, List<Float> v2) {
        if (v1.size() != v2.size()) throw new IllegalArgumentException("Vector size mismatch");
        double sum = 0.0;
        for (int i = 0; i < v1.size(); i++) {
            double diff = v1.get(i) - v2.get(i);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    // Hàm so sánh bằng Cosine similarity
    private double cosineSimilarity(List<Float> v1, List<Float> v2) {
        if (v1.size() != v2.size()) throw new IllegalArgumentException("Vector size mismatch");
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < v1.size(); i++) {
            dot += v1.get(i) * v2.get(i);
            normA += v1.get(i) * v1.get(i);
            normB += v2.get(i) * v2.get(i);
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private void confirm_face(List<Float> newFaceEmbedding){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("faceId").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<Double> stored = (List<Double>) documentSnapshot.get("faceEmbedding");

                        List<Float> oldEmbedding = new ArrayList<>();
                        for (Double d : stored) oldEmbedding.add(d.floatValue());

                        // So sánh
                        double distance = euclideanDistance(newFaceEmbedding, oldEmbedding);
                        double cosine = cosineSimilarity(newFaceEmbedding, oldEmbedding);

                        if (distance < 1.0 || cosine > 0.8) {
                            Toast.makeText(this, "Khuôn mặt trùng khớp", Toast.LENGTH_SHORT).show();
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("result_key", "OK");
                            setResult(RESULT_OK, resultIntent);
                            finish();
                        } else {
                            Toast.makeText(this, "Khuôn mặt không khớp", Toast.LENGTH_SHORT).show();
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("result_key", "CANCELED");
                            setResult(RESULT_CANCELED, resultIntent);
                            finish();
                        }
                    }
                });

    }

    private List<Float> normalizeEmbedding(float[] embeddingArray) {
        double norm = 0.0;
        for (float v : embeddingArray) norm += v * v;
        norm = Math.sqrt(norm);

        List<Float> normalized = new ArrayList<>(embeddingArray.length);
        for (float v : embeddingArray) normalized.add((float)(v / norm));
        return normalized;
    }


    private List<Float> extractFaceEmbedding(Context context, String imagePath) throws IOException {
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        if (bitmap == null) {
            throw new IOException("Không thể đọc ảnh từ đường dẫn: " + imagePath);
        }

        FaceEmbeddingExtractor extractor = new FaceEmbeddingExtractor(context);
        float[] embeddingArray = extractor.getEmbedding(bitmap);
        extractor.close();

        if (embeddingArray == null || embeddingArray.length == 0) {
            throw new IOException("Không thể trích xuất embedding từ ảnh");
        }

        return normalizeEmbedding(embeddingArray);
    }


}
