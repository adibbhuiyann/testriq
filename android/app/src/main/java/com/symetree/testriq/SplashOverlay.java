package com.symetree.testriq;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * The opening screen. It is drawn by Android itself (not by the page), so it appears on the
 * very first frame and animates smoothly while the app's screens load underneath it.
 * Layout, colours and motion match the original opening screen of the app.
 */
final class SplashOverlay extends FrameLayout {
    static final int BG = 0xFF0B1230;
    private static final int GLOW_CENTER = 0xFF1C2A5C;

    /** cubic-bezier(.34,1.56,.64,1): the gentle overshoot used for the logo. */
    private static final Interpolator SPRING = new PathInterpolator(0.34f, 1.56f, 0.64f, 1f);
    /** cubic-bezier(.2,.8,.2,1): the app's standard easing. */
    private static final Interpolator EASE = new PathInterpolator(0.2f, 0.8f, 0.2f, 1f);
    /** CSS "ease", used for the fade into the app. */
    private static final Interpolator FADE = new PathInterpolator(0.25f, 0.1f, 0.25f, 1f);

    private final float dp;
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private int bgAlpha;
    private final LogoView logo;
    private final View[] rising;
    private final View loading;
    private final ArrayList<Animator> running = new ArrayList<>();
    private boolean leaving;

    SplashOverlay(Context c) {
        super(c);
        dp = getResources().getDisplayMetrics().density;
        setWillNotDraw(false);
        setClickable(true); // nothing underneath can be tapped until the app is ready
        setClipChildren(false);
        setContentDescription("Testriq is opening");

        LinearLayout column = new LinearLayout(c);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = px(25);
        column.setPadding(pad, pad, pad, pad);
        column.setClipChildren(false);
        column.setClipToPadding(false);
        column.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        addView(column, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        // Wordmark: height clamp(64dp, 17% of the screen width, 104dp), like the web version.
        float screenWidthDp = getResources().getDisplayMetrics().widthPixels / dp;
        float hDp = Math.max(64f, Math.min(104f, screenWidthDp * 0.17f));
        int h = px(hDp);
        int w = Math.round(h * (334.3f / 113.0f));
        int glowPad = px(70);
        logo = new LogoView(c, w, h, glowPad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(w + 2 * glowPad, h + 2 * glowPad);
        lp.setMargins(-glowPad, -glowPad, -glowPad, -glowPad + px(22));
        column.addView(logo, lp);

        TextView tag = text(c, "IELTS · SAT · PTE", 12.48f, 0xFF8F9CCC, 0.42f, true);
        column.addView(tag, rowParams(18.72f, 6f));
        TextView by = text(c, "BY SYMETREE", 11.2f, 0xFFB8C6E4, 0.3f, false);
        column.addView(by, rowParams(16.8f, 45f));

        loading = new View(c);
        GradientDrawable line = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0x00CBD6FF, 0xFFCBD6FF, 0x00CBD6FF});
        loading.setBackground(line);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(px(54), px(2));
        llp.bottomMargin = px(22);
        column.addView(loading, llp);

        TextView small = text(c, "Preparing your next step", 10.56f, 0xFF9CABC9, 0f, false);
        column.addView(small, rowParams(15.84f, 0f));

        rising = new View[]{tag, by, loading, small};

        // starting state (the first frame matches the system launch colour exactly)
        logo.setAlpha(0f);
        logo.setScaleX(0.6f);
        logo.setScaleY(0.6f);
        logo.setRotation(-8f);
        for (View v : rising) {
            v.setAlpha(0f);
            v.setTranslationY(px(12));
        }
    }

    /** Plays the opening animation. */
    void start() {
        ValueAnimator bg = ValueAnimator.ofInt(0, 255);
        bg.setDuration(320);
        bg.setInterpolator(EASE);
        bg.addUpdateListener(a -> {
            bgAlpha = (int) a.getAnimatedValue();
            invalidate();
        });
        play(bg);

        logo.setLayerType(LAYER_TYPE_HARDWARE, null);
        ValueAnimator in = ValueAnimator.ofFloat(0f, 1f);
        in.setDuration(900);
        in.setInterpolator(SPRING);
        in.addUpdateListener(a -> {
            float v = (float) a.getAnimatedValue();
            logo.setAlpha(Math.max(0f, Math.min(1f, v)));
            float s = 0.6f + 0.4f * v;
            logo.setScaleX(s);
            logo.setScaleY(s);
            logo.setRotation(-8f * (1f - v));
        });
        in.addListener(new EndListener(() -> logo.setLayerType(LAYER_TYPE_NONE, null)));
        play(in);

        for (View v : rising) {
            v.animate().alpha(1f).translationY(0f)
                    .setStartDelay(300).setDuration(700).setInterpolator(EASE)
                    .withLayer().start();
        }

        // a soft pulse on the loading line while the app gets ready
        ValueAnimator pulse = ValueAnimator.ofFloat(1f, 0.35f);
        pulse.setStartDelay(1050);
        pulse.setDuration(650);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setInterpolator(FADE);
        pulse.addUpdateListener(a -> loading.setAlpha((float) a.getAnimatedValue()));
        play(pulse);
    }

    /** Fades into the app. {@code halfway} runs mid-fade, {@code done} once the screen is gone. */
    void finish(Runnable halfway, Runnable done) {
        if (leaving) return;
        leaving = true;
        setClickable(false);
        for (Animator a : running) a.cancel();
        running.clear();
        loading.animate().cancel();
        loading.setAlpha(1f);
        boolean motion = Build.VERSION.SDK_INT < 26 || ValueAnimator.areAnimatorsEnabled();
        if (!motion) {
            // animations are switched off on this phone: go straight to the app
            halfway.run();
            done.run();
            return;
        }
        postDelayed(halfway, 275);
        animate().alpha(0f).setDuration(550).setInterpolator(FADE).withLayer()
                .withEndAction(done).start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) return;
        // radial-gradient(circle at 50% 40%, #1c2a5c 0%, #0b1230 72%) — farthest-corner size
        float cx = w * 0.5f, cy = h * 0.4f;
        float far = (float) Math.hypot(cx, Math.max(cy, h - cy));
        bgPaint.setShader(new RadialGradient(cx, cy, Math.max(1f, far * 0.72f),
                GLOW_CENTER, BG, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(BG);
        if (bgAlpha > 0 && bgPaint.getShader() != null) {
            bgPaint.setAlpha(bgAlpha);
            canvas.drawRect(0, 0, getWidth(), getHeight(), bgPaint);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        for (Animator a : running) a.cancel();
        running.clear();
        super.onDetachedFromWindow();
    }

    // ------------------------------------------------------------------ helpers

    private void play(Animator a) {
        running.add(a);
        a.start();
    }

    private int px(float v) {
        return Math.round(v * dp);
    }

    private LinearLayout.LayoutParams rowParams(float heightDp, float marginBottomDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, px(heightDp));
        lp.bottomMargin = px(marginBottomDp);
        return lp;
    }

    private static TextView text(Context c, String s, float sizeDp, int color, float spacing, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sizeDp);
        t.setTextColor(color);
        t.setLetterSpacing(spacing);
        t.setIncludeFontPadding(false);
        t.setSingleLine(true);
        t.setGravity(Gravity.CENTER);
        if (bold) {
            t.setTypeface(Build.VERSION.SDK_INT >= 28
                    ? Typeface.create(Typeface.DEFAULT, 700, false)
                    : Typeface.DEFAULT_BOLD);
        }
        return t;
    }

    /** The wordmark with the same soft blue glow the web version draws beneath it. */
    private static final class LogoView extends View {
        private final Drawable mark;
        private final int markW, markH, pad;
        private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

        LogoView(Context c, int w, int h, int pad) {
            super(c);
            this.markW = w;
            this.markH = h;
            this.pad = pad;
            mark = c.getDrawable(R.drawable.splash_wordmark);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            float dp = getResources().getDisplayMetrics().density;
            // drop-shadow(0 18px 50px #3f5cf044): an oval glow slightly below the letters
            RadialGradient g = new RadialGradient(0f, 0f, 1f,
                    new int[]{0x2E3F5CF0, 0x183F5CF0, 0x003F5CF0},
                    new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP);
            Matrix m = new Matrix();
            m.setScale(markW / 2f + 50 * dp, markH / 2f + 50 * dp);
            m.postTranslate(w / 2f, h / 2f + 18 * dp);
            g.setLocalMatrix(m);
            glow.setShader(g);
            if (mark != null) mark.setBounds(pad, pad, pad + markW, pad + markH);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawRect(0, 0, getWidth(), getHeight(), glow);
            if (mark != null) mark.draw(canvas);
        }
    }

    private static final class EndListener implements Animator.AnimatorListener {
        private final Runnable end;

        EndListener(Runnable end) {
            this.end = end;
        }

        @Override public void onAnimationStart(Animator a) { }
        @Override public void onAnimationEnd(Animator a) { end.run(); }
        @Override public void onAnimationCancel(Animator a) { }
        @Override public void onAnimationRepeat(Animator a) { }
    }
}
