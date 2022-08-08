package tech.aurorafin.aurora;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;

public class WfBar extends View {

    Paint barPaint;
    Paint labelPaint;

    int totalColor;
    int positiveColor;
    int negativeColor;
    int textSize;
    int textNegativeColor;
    int Dp1;


    float dataSetWidth = 0;
    float start = 0;
    float end = 0;
    boolean total = false;
    boolean positive = false;
    String label = "";
    float labelWidth = 0;


    public WfBar(Context context, int totalColor, int positiveColor, int negativeColor, int textSize, int textNegativeColor, int Dp1, int Dp40) {
        super(context);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Dp40);
        setLayoutParams(lp);

        barPaint = new Paint();
        labelPaint = new Paint();

        barPaint.setStyle(Paint.Style.FILL);
        labelPaint.setTextSize(textSize);
        labelPaint.setAntiAlias(true);

        this.totalColor  = totalColor;
        this.positiveColor = positiveColor;
        this.negativeColor = negativeColor;
        this.textSize = textSize;
        this.textNegativeColor = textNegativeColor;
        this.Dp1 = Dp1;
    }

    public void updateBar(float dataSetWidth, float start, float end, boolean total, boolean positive, String label, float labelWidth){

        if (dataSetWidth != 0){
            this.dataSetWidth = dataSetWidth;
        }else {
            this.dataSetWidth = 1;
        }


        if(end>start){
            this.start = start;
            this.end = end;
        }else {
            this.end = start;
            this.start = end;
        }
        if(total){
            barPaint.setColor(totalColor);
            labelPaint.setColor(totalColor);
        }else if(positive) {
            barPaint.setColor(positiveColor);
            labelPaint.setColor(positiveColor);
        }else {
            barPaint.setColor(negativeColor);
            labelPaint.setColor(textNegativeColor);
        }
        this.label = label;
        this.labelWidth =labelWidth;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //super.onDraw(canvas);
        float scaler = ((float)getWidth() - labelWidth)/dataSetWidth;
        float left = start*scaler;
        float right = end*scaler;
        if(/*right-left != 0 && */right-left < 1){
            right = left+1f;
        }
        canvas.drawRect(left, Dp1, right, getHeight()-Dp1, barPaint);
        canvas.drawText(label, right + Dp1*3, (int)(getHeight()/2 + textSize/3), labelPaint);
    }
}
