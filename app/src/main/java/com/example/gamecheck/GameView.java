package com.example.gamecheck;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;

public class GameView extends View {

    private enum Screen {
        SPLASH,
        HOME,
        GAME
    }

    private enum StartEdges {
        LEFT_AND_RIGHT,
        TOP_AND_BOTTOM
    }

    private static final long SPLASH_DURATION = 2400L;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF playButton = new RectF();
    private final RectF exitButton = new RectF();
    private final RectF homeButton = new RectF();

    private final Bitmap logo;
    private final Bitmap homeBackground;
    private final Bitmap adventurer;
    private final Bitmap enemy;
    private final AnimatedBackground animatedBackground;
    private final ArrayList<Projectile> projectiles = new ArrayList<>();

    private Screen currentScreen = Screen.SPLASH;
    private long splashStartedAt = SystemClock.uptimeMillis();
    private long gameStartedAt;
    private long lastFrameAt;
    private boolean animationRunning = true;
    private boolean useVerticalStartNext;
    private StartEdges currentStartEdges = StartEdges.LEFT_AND_RIGHT;
    private MovingGameObject objectA;
    private MovingGameObject objectB;

    public GameView(Context context) {
        super(context);
        setFocusable(true);

        logo = BitmapFactory.decodeResource(getResources(), R.drawable.game_logo);
        homeBackground = BitmapFactory.decodeResource(getResources(), R.drawable.home_background);
        adventurer = BitmapFactory.decodeResource(getResources(), R.drawable.adventurer);
        enemy = BitmapFactory.decodeResource(getResources(), R.drawable.enemy_b);
        animatedBackground = new AnimatedBackground(context, R.drawable.forest_background);

        textPaint.setTypeface(android.graphics.Typeface.create(
                android.graphics.Typeface.SANS_SERIF,
                android.graphics.Typeface.BOLD
        ));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        switch (currentScreen) {
            case SPLASH -> drawSplash(canvas);
            case HOME -> drawHome(canvas);
            case GAME -> drawGame(canvas);
        }

        if (animationRunning && currentScreen != Screen.HOME) {
            postInvalidateOnAnimation();
        }
    }

    private void drawSplash(Canvas canvas) {
        long elapsed = SystemClock.uptimeMillis() - splashStartedAt;
        float progress = Math.min(1f, elapsed / (float) SPLASH_DURATION);
        float scale = getUiScale(canvas);

        paint.setShader(new LinearGradient(
                0, 0, canvas.getWidth(), canvas.getHeight(),
                Color.rgb(8, 17, 36), Color.rgb(41, 28, 74),
                Shader.TileMode.CLAMP
        ));
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);
        paint.setShader(null);

        float logoSize = 230f * scale;
        float logoCenterX = canvas.getWidth() / 2f;
        float logoCenterY = canvas.getHeight() * 0.39f;
        drawRoundImage(canvas, logo, logoCenterX, logoCenterY, logoSize);

        drawCenteredText(canvas, "JOURNEY OF THE MONSTER SLAYER",
                canvas.getHeight() * 0.67f, 42f * scale, Color.WHITE);
        drawCenteredText(canvas, "Preparing your adventure...",
                canvas.getHeight() * 0.74f, 22f * scale, 0xFFCBD5E1);

        float barWidth = 560f * scale;
        float barHeight = 22f * scale;
        float left = (canvas.getWidth() - barWidth) / 2f;
        float top = canvas.getHeight() * 0.82f;
        RectF bar = new RectF(left, top, left + barWidth, top + barHeight);

        paint.setColor(0x55334155);
        canvas.drawRoundRect(bar, barHeight / 2f, barHeight / 2f, paint);
        paint.setColor(0xFFF4C95D);
        canvas.drawRoundRect(left, top, left + barWidth * progress,
                top + barHeight, barHeight / 2f, barHeight / 2f, paint);

        if (progress >= 1f) {
            currentScreen = Screen.HOME;
            invalidate();
        }
    }

    private void drawHome(Canvas canvas) {
        drawCoverImage(canvas, homeBackground);

        paint.setColor(0x80030A18);
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);

        float scale = getUiScale(canvas);
        float logoSize = 150f * scale;
        drawRoundImage(canvas, logo, canvas.getWidth() * 0.16f,
                canvas.getHeight() * 0.22f, logoSize);

        drawCenteredText(canvas, "JOURNEY OF THE MONSTER SLAYER",
                canvas.getHeight() * 0.30f, 48f * scale, Color.WHITE);
        drawCenteredText(canvas, "The ancient forest is calling for a hero",
                canvas.getHeight() * 0.40f, 24f * scale, 0xFFE2E8F0);

        float buttonWidth = 380f * scale;
        float buttonHeight = 82f * scale;
        float centerX = canvas.getWidth() / 2f;
        float firstTop = canvas.getHeight() * 0.53f;

        playButton.set(centerX - buttonWidth / 2f, firstTop,
                centerX + buttonWidth / 2f, firstTop + buttonHeight);
        exitButton.set(centerX - buttonWidth / 2f,
                firstTop + buttonHeight + 24f * scale,
                centerX + buttonWidth / 2f,
                firstTop + buttonHeight * 2f + 24f * scale);

        drawButton(canvas, playButton, "PLAY", 0xFFE0A82E, scale);
        drawButton(canvas, exitButton, "EXIT", 0xAA23324A, scale);
    }

    private void drawGame(Canvas canvas) {
        long now = SystemClock.uptimeMillis();
        long elapsed = now - gameStartedAt;
        animatedBackground.draw(canvas, elapsed);

        float scale = getUiScale(canvas);
        ensureGameObjects(canvas, scale);
        updateGameObjects(canvas, now);

        paint.setColor(0x44000000);
        canvas.drawRect(0, 0, canvas.getWidth(), 100f * scale, paint);
        drawCenteredText(canvas, "OBJECT MOVEMENT DEMO",
                63f * scale, 30f * scale, Color.WHITE);

        String startPositionText = currentStartEdges == StartEdges.LEFT_AND_RIGHT
                ? "START: A LEFT  ↔  B RIGHT"
                : "START: A TOP  ↕  B BOTTOM";
        drawCenteredText(canvas, startPositionText,
                91f * scale, 17f * scale, 0xFFF4C95D);

        objectA.draw(canvas, scale);
        objectB.draw(canvas, scale);
        for (Projectile projectile : projectiles) {
            projectile.draw(canvas);
        }

        float margin = 30f * scale;
        homeButton.set(margin, margin, margin + 170f * scale, margin + 62f * scale);
        drawButton(canvas, homeButton, "HOME", 0xAA17253D, scale);

        drawCenteredText(canvas, "Tap anywhere: A fires object C",
                canvas.getHeight() - 35f * scale, 21f * scale, Color.WHITE);
    }

    private void ensureGameObjects(Canvas canvas, float scale) {
        if (objectA != null && objectB != null) return;

        float objectSize = 112f * scale;
        float halfSize = objectSize / 2f;
        float centerX = canvas.getWidth() / 2f;
        float centerY = canvas.getHeight() / 2f;

        boolean startsAtLeftAndRight =
                currentStartEdges == StartEdges.LEFT_AND_RIGHT;

        float objectAX = startsAtLeftAndRight ? halfSize : centerX;
        float objectAY = startsAtLeftAndRight ? centerY : halfSize;
        float objectBX = startsAtLeftAndRight
                ? canvas.getWidth() - halfSize
                : centerX;
        float objectBY = startsAtLeftAndRight
                ? centerY
                : canvas.getHeight() - halfSize;

        objectA = new MovingGameObject(
                adventurer,
                objectAX,
                objectAY,
                objectSize,
                190f * scale,
                105f * scale,
                "A",
                0xFF2563EB,
                MovingGameObject.MovementType.BOUNCE
        );

        objectB = new MovingGameObject(
                enemy,
                objectBX,
                objectBY,
                objectSize,
                -165f * scale,
                -115f * scale,
                "B",
                0xFFDC2626,
                MovingGameObject.MovementType.WRAP_LEFT_AND_TOP
        );
    }

    private void updateGameObjects(Canvas canvas, long now) {
        if (lastFrameAt == 0L) lastFrameAt = now;
        float deltaTime = Math.min(0.04f, (now - lastFrameAt) / 1000f);
        lastFrameAt = now;

        objectA.update(deltaTime, canvas.getWidth(), canvas.getHeight());
        objectB.update(deltaTime, canvas.getWidth(), canvas.getHeight());

        Iterator<Projectile> iterator = projectiles.iterator();
        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            projectile.update(deltaTime, canvas.getWidth(), canvas.getHeight());
            if (!projectile.isActive()) iterator.remove();
        }
    }

    private void startGame() {
        currentScreen = Screen.GAME;
        currentStartEdges = useVerticalStartNext
                ? StartEdges.TOP_AND_BOTTOM
                : StartEdges.LEFT_AND_RIGHT;
        useVerticalStartNext = !useVerticalStartNext;
        gameStartedAt = SystemClock.uptimeMillis();
        lastFrameAt = gameStartedAt;
        objectA = null;
        objectB = null;
        projectiles.clear();
        invalidate();
    }

    private void drawButton(Canvas canvas, RectF bounds, String label,
                            int backgroundColor, float scale) {
        paint.setColor(backgroundColor);
        canvas.drawRoundRect(bounds, 16f * scale, 16f * scale, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f * scale);
        paint.setColor(0xFFF4C95D);
        canvas.drawRoundRect(bounds, 16f * scale, 16f * scale, paint);
        paint.setStyle(Paint.Style.FILL);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(28f * scale);
        textPaint.setColor(Color.WHITE);
        float textY = bounds.centerY()
                - (textPaint.ascent() + textPaint.descent()) / 2f;
        canvas.drawText(label, bounds.centerX(), textY, textPaint);
    }

    private void drawRoundImage(Canvas canvas, Bitmap bitmap,
                                float centerX, float centerY, float size) {
        if (bitmap == null) return;

        canvas.save();
        Path circle = new Path();
        circle.addCircle(centerX, centerY, size / 2f, Path.Direction.CW);
        canvas.clipPath(circle);
        RectF destination = new RectF(
                centerX - size / 2f,
                centerY - size / 2f,
                centerX + size / 2f,
                centerY + size / 2f
        );
        canvas.drawBitmap(bitmap, null, destination, null);
        canvas.restore();

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(3f, size * 0.025f));
        paint.setColor(0xFFF4C95D);
        canvas.drawCircle(centerX, centerY, size / 2f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawCoverImage(Canvas canvas, Bitmap bitmap) {
        if (bitmap == null) {
            canvas.drawColor(Color.rgb(8, 17, 36));
            return;
        }

        float viewRatio = (float) canvas.getWidth() / canvas.getHeight();
        float imageRatio = (float) bitmap.getWidth() / bitmap.getHeight();
        Rect source;

        if (imageRatio > viewRatio) {
            int sourceWidth = Math.round(bitmap.getHeight() * viewRatio);
            int left = (bitmap.getWidth() - sourceWidth) / 2;
            source = new Rect(left, 0, left + sourceWidth, bitmap.getHeight());
        } else {
            int sourceHeight = Math.round(bitmap.getWidth() / viewRatio);
            int top = (bitmap.getHeight() - sourceHeight) / 2;
            source = new Rect(0, top, bitmap.getWidth(), top + sourceHeight);
        }

        canvas.drawBitmap(bitmap, source,
                new Rect(0, 0, canvas.getWidth(), canvas.getHeight()), paint);
    }

    private void drawCenteredText(Canvas canvas, String text, float y,
                                  float size, int color) {
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(size);
        textPaint.setColor(color);
        canvas.drawText(text, canvas.getWidth() / 2f, y, textPaint);
    }

    private float getUiScale(Canvas canvas) {
        return Math.min(canvas.getWidth() / 1280f, canvas.getHeight() / 720f);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) return true;

        float x = event.getX();
        float y = event.getY();

        if (currentScreen == Screen.HOME) {
            if (playButton.contains(x, y)) {
                startGame();
            } else if (exitButton.contains(x, y)) {
                ((Activity) getContext()).finish();
            }
        } else if (currentScreen == Screen.GAME) {
            if (homeButton.contains(x, y)) {
                currentScreen = Screen.HOME;
                projectiles.clear();
                invalidate();
            } else if (objectA != null) {
                projectiles.add(new Projectile(
                        objectA.getX(), objectA.getY(), x, y,
                        Math.min(getWidth() / 1280f, getHeight() / 720f)
                ));
            }
        }
        return true;
    }

    public void pauseAnimation() {
        animationRunning = false;
    }

    public void resumeAnimation() {
        animationRunning = true;
        if (currentScreen == Screen.SPLASH) {
            splashStartedAt = SystemClock.uptimeMillis();
        } else if (currentScreen == Screen.GAME) {
            gameStartedAt = SystemClock.uptimeMillis();
            lastFrameAt = gameStartedAt;
        }
        invalidate();
    }
}
