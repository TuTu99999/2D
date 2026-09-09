package com.example.gamecheck;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

public class AnimatedBackground {

    private final Bitmap image;
    private final Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public AnimatedBackground(Context context, int imageResource) {
        image = BitmapFactory.decodeResource(context.getResources(), imageResource);
        imagePaint.setFilterBitmap(true);
        lightPaint.setColor(0x88FFF09A);
    }

    public void draw(Canvas canvas, long elapsedTime) {
        int viewWidth = canvas.getWidth();
        int viewHeight = canvas.getHeight();
        if (viewWidth == 0 || viewHeight == 0 || image == null) return;

        float viewRatio = (float) viewWidth / viewHeight;
        float imageRatio = (float) image.getWidth() / image.getHeight();

        float cropWidth;
        float cropHeight;
        if (imageRatio > viewRatio) {
            cropHeight = image.getHeight();
            cropWidth = cropHeight * viewRatio;
        } else {
            cropWidth = image.getWidth();
            cropHeight = cropWidth / viewRatio;
        }

        // Zoom in a little so the image has room to move in both directions.
        cropWidth *= 0.88f;
        cropHeight *= 0.88f;

        float maxLeft = image.getWidth() - cropWidth;
        float maxTop = image.getHeight() - cropHeight;
        float horizontalProgress = (float) ((Math.sin(elapsedTime / 2600.0) + 1.0) / 2.0);
        float verticalProgress = (float) ((Math.cos(elapsedTime / 3400.0) + 1.0) / 2.0);

        int left = Math.round(maxLeft * horizontalProgress);
        int top = Math.round(maxTop * verticalProgress);
        Rect source = new Rect(
                left,
                top,
                Math.min(image.getWidth(), left + Math.round(cropWidth)),
                Math.min(image.getHeight(), top + Math.round(cropHeight))
        );
        RectF destination = new RectF(0, 0, viewWidth, viewHeight);
        canvas.drawBitmap(image, source, destination, imagePaint);

        drawMovingLights(canvas, elapsedTime);
    }

    private void drawMovingLights(Canvas canvas, long elapsedTime) {
        float width = canvas.getWidth();
        float height = canvas.getHeight();

        for (int i = 0; i < 8; i++) {
            float x = (elapsedTime * (0.018f + i * 0.002f) + i * width / 8f) % width;
            float baseY = height * (0.25f + (i % 4) * 0.16f);
            float y = baseY + (float) Math.sin(elapsedTime / 500.0 + i) * 14f;
            float radius = 3f + (i % 3) * 1.5f;
            canvas.drawCircle(x, y, radius, lightPaint);
        }
    }
}

