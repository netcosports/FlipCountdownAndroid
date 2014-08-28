package com.netcosports.flip;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AnimationUtils;

/**
 * Created by stephane on 11/28/13.
 */
public class FlipCountdownView extends View{

    private int roundRectRadius = 4;
    private int middleLineHeight = 2;

    public FlipCountdownView(Context context) {
        super(context);
        init(null);
    }

    public FlipCountdownView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public FlipCountdownView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }


    private Paint mPaint;
    private Paint mPaintBg;
    private Paint mPaintGradientTop;
    private Paint mPaintMiddle;
    private int numberOffset = 0;
    private int colorGradientTop;
    private void init(AttributeSet attrs)
    {
        Resources res = getContext().getResources();

        int textColor, bgColor;
        float textSize;

        if(attrs != null)
        {
            TypedArray a = res.obtainAttributes(attrs, R.styleable.FlipCountdownView);
            textColor = a.getColor(R.styleable.FlipCountdownView_flipTextColor, res.getColor(R.color.flip_countdown_text));
            bgColor = a.getColor(R.styleable.FlipCountdownView_flipBackgroundColor, res.getColor(R.color.flip_countdown_background));
            textSize = a.getDimension(R.styleable.FlipCountdownView_flipTextSize, res.getDimension(R.dimen.flip_countdown_text_size));
            a.recycle();

        }
        else
        {
            textColor = res.getColor(R.color.flip_countdown_text);
            bgColor = res.getColor(R.color.flip_countdown_background);
            textSize = res.getDimension(R.dimen.flip_countdown_text_size);
        }


        roundRectRadius = res.getDimensionPixelSize(R.dimen.flip_countdown_round_rect_radius);
        middleLineHeight = res.getDimensionPixelSize(R.dimen.flip_countdown_middle_line_height);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(textColor);
        mPaint.setTextSize(textSize);

        mPaintBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintBg.setColor(bgColor);
        mPaintBg.setStyle(Paint.Style.FILL);

        mPaintMiddle = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaintMiddle.setColor(Color.BLACK);
        mPaintMiddle.setAlpha(30);
        mPaintMiddle.setStrokeWidth(middleLineHeight);
        mPaintMiddle.setStyle(Paint.Style.STROKE);

        mPaintGradientTop = new Paint(Paint.ANTI_ALIAS_FLAG);
        colorGradientTop = res.getColor(R.color.flip_countdown_gradient);

        duration = res.getInteger(R.integer.flip_duration);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mPaintGradientTop.setShader(new LinearGradient(0,
                0,
                0,
                getMeasuredHeight() / 2,
                new int[]{Color.TRANSPARENT, colorGradientTop},
                new float[]{0, 1},
                Shader.TileMode.REPEAT
        ));
    }

    private int mCurrentValue = -1;
    private int mValue = -1;
    public void setValue(int value) {
        if(value < 10 && value >= 0)
        {
            mValue = value;
            invalidate();
        }
    }

    private float duration;
    public void setDuration(int duration)
    {
        this.duration = duration;
    }

    private long startAnimTimestamp = 0;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawBgRect(canvas);

        if(mValue >= 0)
        {
            if(mCurrentValue == -1 || mCurrentValue == mValue)
            {
                mCurrentValue = mValue;
                drawNumber(canvas, mValue);
            }
            else if(mCurrentValue != mValue)
            {
                //animation in progress

                if(startAnimTimestamp == 0)
                {
                    //start animation
                    startAnimTimestamp = AnimationUtils.currentAnimationTimeMillis();
                    ViewCompat.postInvalidateOnAnimation(this);
                }
                else
                {
                    float currentPercentage = (AnimationUtils.currentAnimationTimeMillis() - startAnimTimestamp) / duration;
                    if(currentPercentage >= 1)
                    {
                        //animation finished
                        mCurrentValue = mValue;
                        startAnimTimestamp = 0;
                        drawNumber(canvas, mCurrentValue);
                    }
                    else
                    {
                        //draw current state
                        drawHalfUpperNumber(canvas, mValue);
                        drawHalfUpperNumber(canvas, mCurrentValue, 1 -  2 * currentPercentage);
                        drawHalfBottomNumber(canvas, mCurrentValue);
                        drawHalfBottomNumber(canvas, mValue, 2 * (currentPercentage - 0.5f));

                        ViewCompat.postInvalidateOnAnimation(this);
                    }
                }
            }
        }

        canvas.drawLine(0, getMeasuredHeight()/2, getMeasuredWidth(), getMeasuredHeight()/2, mPaintMiddle);

    }

    private Rect bounds = new Rect();
    private void drawNumber(Canvas canvas, int value) {
        drawHalfBottomNumber(canvas, value);
        drawHalfUpperNumber(canvas, value);
    }

    private RectF rectF = new RectF();
    private void drawHalfBottomNumber(Canvas canvas, int value) {
        drawHalfBottomNumber(canvas, value, 1);
    }

    private void drawHalfBottomNumber(Canvas canvas, int value, float percent) {
        String text = String.valueOf(value);
        mPaint.getTextBounds(text, 0, text.length(), bounds);
        canvas.save();
        canvas.clipRect(0,getMeasuredHeight() / 2 + numberOffset /2,getMeasuredWidth(), getMeasuredHeight());

        percent = getRealPercent(percent);

        canvas.scale(1, percent, 0, getMeasuredHeight() / 2);
        setColorFilter(percent);
        drawBgRect(canvas);

        float widthText = mPaint.measureText(text);
        canvas.drawText(text, (int)(getMeasuredWidth() / 2 - widthText / 2), getMeasuredHeight() / 2 + bounds.height() / 2, mPaint);
        canvas.restore();
    }

    private void setColorFilter(float percent) {
        if(percent != 1)
        {
            PorterDuffColorFilter filter = new PorterDuffColorFilter(addAlphaPercentToColor((int) (80 - 80 * percent), Color.BLACK), PorterDuff.Mode.DARKEN);
            mPaintBg.setColorFilter(filter);
        }
        else
        {
            mPaintBg.setColorFilter(null);
        }
    }

    public static int addAlphaPercentToColor(int alphaPercent, int color)
    {
        return Color.argb(alphaPercent * 255 / 100, Color.red(color), Color.green(color),
                Color.blue(color));
    }

    private void drawHalfUpperNumber(Canvas canvas, int value) {
        drawHalfUpperNumber(canvas, value, 1);
    }

    private void drawHalfUpperNumber(Canvas canvas, int value, float percent) {
        String text = String.valueOf(value);
        mPaint.getTextBounds(text, 0, text.length(), bounds);
        canvas.save();
        canvas.clipRect(0,0,getMeasuredWidth(), getMeasuredHeight() / 2 - numberOffset / 2);

        percent = getRealPercent(percent);

        canvas.scale(1, percent, 0, getMeasuredHeight()/2);
        setColorFilter(percent);
        drawBgRect(canvas);

        float widthText = mPaint.measureText(text);
        canvas.drawText(text, (int)(getMeasuredWidth() / 2 - widthText / 2), getMeasuredHeight() / 2 + bounds.height() / 2, mPaint);
        canvas.restore();
    }

    private void drawBgRect(Canvas canvas) {
        rectF.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
        canvas.drawRoundRect(rectF, roundRectRadius, roundRectRadius, mPaintBg);
        rectF.set(0,0,getMeasuredWidth(), getMeasuredHeight()/2);
        canvas.drawRoundRect(rectF, roundRectRadius, roundRectRadius, mPaintGradientTop);
    }

    private float getRealPercent(float percent) {
        if(percent > 1)
            percent = 1;
        else if(percent < 0)
            percent = 0;
        return percent;
    }
}
