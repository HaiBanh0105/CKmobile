package com.example.banking;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class FaceEmbeddingExtractor {
    private Interpreter interpreter;

    public FaceEmbeddingExtractor(Context context) throws IOException {
        MappedByteBuffer modelBuffer = loadModelFile(context, "facenet.tflite");
        interpreter = new Interpreter(modelBuffer);
    }

    private MappedByteBuffer loadModelFile(Context context, String modelName) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY,
                fileDescriptor.getStartOffset(), fileDescriptor.getDeclaredLength());
    }

    public float[] getEmbedding(Bitmap faceBitmap) {
        Bitmap resized = Bitmap.createScaledBitmap(faceBitmap, 160, 160, true);
        float[][][][] input = new float[1][160][160][3];

        for (int y = 0; y < 160; y++) {
            for (int x = 0; x < 160; x++) {
                int pixel = resized.getPixel(x, y);
                input[0][y][x][0] = ((pixel >> 16) & 0xFF) / 255.0f;
                input[0][y][x][1] = ((pixel >> 8) & 0xFF) / 255.0f;
                input[0][y][x][2] = (pixel & 0xFF) / 255.0f;
            }
        }

        float[][] output = new float[1][128]; // vector 128 chiều
        interpreter.run(input, output);
        return output[0];
    }
}
