package com.example.gamecheck;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import java.util.Random;

public class MovingGameObject {

    public enum MovementType {
        BOUNCE,
        WRAP_LEFT_AND_TOP
    }

    private final Bitmap image;
    private final float size;
    private final String name;
    private final MovementType movementType;
    private final Random random = new Random();
    private final Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float x;
    private float y;
    private float speedX;
    private float speedY;

    public MovingGameObject(Bitmap image, float x, float y, float size,
                            float speedX, float speedY, String name,
                            int labelColor, MovementType movementType) {
        this.image = image;
        this.x = x;
        this.y = y;
        this.size = size;
        this.speedX = speedX;
        this.speedY = speedY;
        this.name = name;
        this.movementType = movementType;

        imagePaint.setFilterBitmap(true);
        shadowPaint.setColor(0x66000000);
        bubblePaint.setColor(labelColor);
        labelPaint.setColor(Color.WHITE);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
    }

    public void update(float deltaTime, float screenWidth, float screenHeight) {
        x += speedX * deltaTime;
        y += speedY * deltaTime;

        if (movementType == MovementType.BOUNCE) {
            bounceAtAllEdges(screenWidth, screenHeight);
        } else {
            wrapAtLeftAndTop(screenWidth, screenHeight);
        }
    }

    private void bounceAtAllEdges(float screenWidth, float screenHeight) {
        float halfSize = size / 2f;

        if (x - halfSize <= 0f) {
            x = halfSize;
            speedX = Math.abs(speedX);
        } else if (x + halfSize >= screenWidth) {
            x = screenWidth - halfSize;
            speedX = -Math.abs(speedX);
        }

        if (y - halfSize <= 0f) {
            y = halfSize;
            speedY = Math.abs(speedY);
        } else if (y + halfSize >= screenHeight) {
            y = screenHeight - halfSize;
            speedY = -Math.abs(speedY);
        }
    }

    private void wrapAtLeftAndTop(float screenWidth, float screenHeight) {
        float halfSize = size / 2f;

        if (x - halfSize <= 0f) {
            // Touching the left edge makes B appear at the opposite edge.
            x = screenWidth - halfSize;
            y = randomBetween(halfSize, screenHeight - halfSize);
            speedX = -Math.abs(speedX);
            speedY = random.nextBoolean()
                    ? Math.abs(speedY)
                    : -Math.abs(speedY);
        }

        if (y - halfSize <= 0f) {
            // Touching the top edge makes B appear at the bottom edge.
            y = screenHeight - halfSize;
            x = randomBetween(halfSize, screenWidth - halfSize);
            speedY = -Math.abs(speedY);
            speedX = random.nextBoolean()
                    ? Math.abs(speedX)
                    : -Math.abs(speedX);
        }

        // The remaining two edges reflect B back into the screen.
        if (x + halfSize >= screenWidth && speedX > 0f) {
            x = screenWidth - halfSize;
            speedX = -Math.abs(speedX);
        }
        if (y + halfSize >= screenHeight && speedY > 0f) {
            y = screenHeight - halfSize;
            speedY = -Math.abs(speedY);
        }
    }

    private float randomBetween(float minimum, float maximum) {
        return minimum + random.nextFloat() * Math.max(0f, maximum - minimum);
    }

    public void draw(Canvas canvas, float uiScale) {
        float halfSize = size / 2f;
        RectF destination = new RectF(
                x - halfSize,
                y - halfSize,
                x + halfSize,
                y + halfSize
        );

        canvas.drawOval(
                x - size * 0.32f,
                y + size * 0.36f,
                x + size * 0.32f,
                y + size * 0.50f,
                shadowPaint
        );
        canvas.drawBitmap(image, null, destination, imagePaint);

        float bubbleRadius = 19f * uiScale;
        float bubbleY = y - halfSize - 15f * uiScale;
        canvas.drawCircle(x, bubbleY, bubbleRadius, bubblePaint);

        labelPaint.setTextSize(23f * uiScale);
        float textY = bubbleY
                - (labelPaint.ascent() + labelPaint.descent()) / 2f;
        canvas.drawText(name, x, textY, labelPaint);
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }
}
