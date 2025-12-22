package com.example.banking;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
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
    private TextView tvInstruction;
    private ImageButton btnBack;

    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private String userId = SessionManager.getInstance().getUserId();

    private boolean isCountingDown = false;
    private CountDownTimer countDownTimer;
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ekyc); // Đảm bảo tên file xml đúng

        // Ánh xạ View theo ID trong XML mới
        viewFinder = findViewById(R.id.viewFinder);
        tvInstruction = findViewById(R.id.tvInstruction);
        btnBack = findViewById(R.id.btnBack);

        cameraExecutor = Executors.newSingleThreadExecutor();

        btnBack.setOnClickListener(v -> finish());

        Intent getIntent = getIntent();
        type = getIntent.getStringExtra("type");

        // Kiểm tra quyền Camera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        } else {
            startCamera();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Chọn Camera trước
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();

                // Cấu hình ML Kit phát hiện khuôn mặt
                FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                        .build();
                FaceDetector detector = FaceDetection.getClient(options);

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    @SuppressLint("UnsafeOptInUsageError")
                    android.media.Image mediaImage = imageProxy.getImage();
                    if (mediaImage != null) {
                        InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

                        detector.process(image)
                                .addOnSuccessListener(faces -> {
                                    if (faces.size() > 0) {
                                        Face face = faces.get(0);
                                        // Logic kiểm tra khuôn mặt hợp lệ
                                        checkFaceAndCountDown(face, imageProxy.getWidth(), imageProxy.getHeight());
                                    } else {
                                        resetCountdown("Không tìm thấy khuôn mặt");
                                    }
                                })
                                .addOnFailureListener(e -> Log.e("EKYC", "Lỗi AI: " + e.getMessage()))
                                .addOnCompleteListener(task -> imageProxy.close());
                    } else {
                        imageProxy.close();
                    }
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, imageAnalysis);

            } catch (Exception e) {
                Log.e("Ekyc", "Lỗi mở camera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // Kiểm tra khuôn mặt có nằm giữa và đủ lớn không
    private void checkFaceAndCountDown(Face face, int width, int height) {
        Rect bounds = face.getBoundingBox();
        int centerX = bounds.centerX();
        int centerY = bounds.centerY();

        // 1. Kiểm tra độ lớn khuôn mặt (tránh đứng quá xa)
        if (bounds.width() < 150 || bounds.height() < 150) {
            resetCountdown("Vui lòng đưa mặt lại gần hơn");
            return;
        }

        // 2. Kiểm tra căn giữa (tương đối)
        // Lưu ý: Hệ tọa độ ImageAnalysis có thể xoay, nên check tương đối
        // Nếu muốn chính xác tuyệt đối cần map coordinate, nhưng ở đây check đơn giản
        boolean isCentered = (centerX > width * 0.2 && centerX < width * 0.8) &&
                (centerY > height * 0.2 && centerY < height * 0.8);

        if (isCentered) {
            startCountdown();
        } else {
            resetCountdown("Vui lòng giữ mặt ở giữa khung hình");
        }
    }

    private void startCountdown() {
        if (isCountingDown) return; // Đang đếm rồi thì thôi

        runOnUiThread(() -> {
            isCountingDown = true;
            if (countDownTimer != null) countDownTimer.cancel();

            countDownTimer = new CountDownTimer(3000, 1000) {
                public void onTick(long millisUntilFinished) {
                    long seconds = millisUntilFinished / 1000 + 1;
                    tvInstruction.setText("Giữ nguyên... " + seconds);
                    tvInstruction.setTextColor(Color.GREEN);
                }

                public void onFinish() {
                    tvInstruction.setText("Đang xử lý...");
                    tvInstruction.setTextColor(Color.YELLOW);
                    captureFace();
                }
            }.start();
        });
    }

    private void resetCountdown(String msg) {
        if (!isCountingDown) return;

        runOnUiThread(() -> {
            isCountingDown = false;
            if (countDownTimer != null) countDownTimer.cancel();
            tvInstruction.setText(msg);
            tvInstruction.setTextColor(Color.WHITE);
        });
    }

    private void captureFace() {
        File photoFile = new File(getExternalFilesDir(null), "ekyc_face_" + System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions options =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(options, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        processCapturedImage(photoFile.getAbsolutePath());
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        resetCountdown("Lỗi chụp ảnh. Thử lại!");
                    }
                });
    }

    private void processCapturedImage(String path) {
        if ("create".equalsIgnoreCase(type)) {
            // Trường hợp ĐĂNG KÝ: Trả về đường dẫn ảnh
            Intent resultIntent = new Intent();
            resultIntent.putExtra("faceImagePath", path);
            setResult(RESULT_OK, resultIntent);
            finish();
        }
        else if ("confirm".equalsIgnoreCase(type)) {
            // Trường hợp XÁC THỰC: So sánh vector trong background thread
            cameraExecutor.execute(() -> {
                try {
                    // Trích xuất Vector từ ảnh vừa chụp
                    final List<Float> newFaceEmbedding = extractFaceEmbedding(this, path);

                    // So sánh với Firebase
                    runOnUiThread(() -> compareWithDatabase(newFaceEmbedding));

                } catch (IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(ekyc.this, "Lỗi xử lý ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        resetCountdown("Vui lòng thử lại");
                    });
                }
            });
        }
    }

    private void compareWithDatabase(List<Float> newFaceEmbedding) {
        // Lấy ID cần so sánh (nếu admin check user khác thì lấy từ Intent, mặc định lấy user hiện tại)
        String targetUser = getIntent().hasExtra("user_id_check") ?
                getIntent().getStringExtra("user_id_check") : userId;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("faceId").document(targetUser).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<Double> stored = (List<Double>) documentSnapshot.get("faceEmbedding");
                        if (stored != null) {
                            // Convert Double (Firestore) -> Float
                            List<Float> oldEmbedding = new ArrayList<>();
                            for (Double d : stored) oldEmbedding.add(d.floatValue());

                            // Tính toán độ tương đồng
                            double distance = euclideanDistance(newFaceEmbedding, oldEmbedding);
                            double cosine = cosineSimilarity(newFaceEmbedding, oldEmbedding);

                            // Ngưỡng so sánh (Threshold)
                            // Distance càng nhỏ càng giống ( < 1.0 )
                            // Cosine càng lớn càng giống ( > 0.8 )
                            if (distance < 1.1 && cosine > 0.75) {
                                Toast.makeText(this, "Xác thực khuôn mặt thành công!", Toast.LENGTH_SHORT).show();
                                Intent resultIntent = new Intent();
                                resultIntent.putExtra("result_key", "OK");
                                setResult(RESULT_OK, resultIntent);
                                finish();
                            } else {
                                tvInstruction.setText("Khuôn mặt không khớp!");
                                tvInstruction.setTextColor(Color.RED);
                                isCountingDown = false; // Cho phép thử lại sau vài giây
                            }
                        }
                    } else {
                        Toast.makeText(this, "Tài khoản chưa đăng ký sinh trắc học", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi kết nối Server", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    // --- CÁC HÀM XỬ LÝ VECTOR (GIỮ NGUYÊN) ---
    private List<Float> normalizeEmbedding(float[] embeddingArray) {
        double norm = 0.0;
        for (float v : embeddingArray) norm += v * v;
        norm = Math.sqrt(norm);
        List<Float> normalized = new ArrayList<>(embeddingArray.length);
        for (float v : embeddingArray) normalized.add((float)(v / norm));
        return normalized;
    }

    private List<Float> extractFaceEmbedding(android.content.Context context, String imagePath) throws IOException {
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        if (bitmap == null) throw new IOException("Không đọc được ảnh");

        FaceEmbeddingExtractor extractor = new FaceEmbeddingExtractor(context);
        float[] embeddingArray = extractor.getEmbedding(bitmap);
        extractor.close();

        if (embeddingArray == null) throw new IOException("Không tìm thấy khuôn mặt trong ảnh để trích xuất");
        return normalizeEmbedding(embeddingArray);
    }

    private double euclideanDistance(List<Float> v1, List<Float> v2) {
        if (v1.size() != v2.size()) return 100.0;
        double sum = 0.0;
        for (int i = 0; i < v1.size(); i++) {
            double diff = v1.get(i) - v2.get(i);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    private double cosineSimilarity(List<Float> v1, List<Float> v2) {
        if (v1.size() != v2.size()) return 0.0;
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < v1.size(); i++) {
            dot += v1.get(i) * v2.get(i);
            normA += v1.get(i) * v1.get(i);
            normB += v2.get(i) * v2.get(i);
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}