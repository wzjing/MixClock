package com.infinitytech.classicalmix;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;


public class Oval extends AppCompatImageView {


    public Oval(Context context) {
        super(context);
    }

    public Oval(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {

//        canvas.drawArc();

        super.onDraw(canvas);
    }
}
