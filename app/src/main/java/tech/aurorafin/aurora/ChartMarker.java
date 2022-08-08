package tech.aurorafin.aurora;

import android.content.Context;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.utils.MPPointF;

public class ChartMarker extends MarkerView {

    MPPointF mOffset;


    public ChartMarker(Context context) {
        super(context, R.layout.marker_one);
    }

    @Override
    public MPPointF getOffset() {
        if(mOffset == null) {
            // center the marker horizontally and vertically
            mOffset = new MPPointF(-(getWidth() / 2f), -getHeight()/2f);
        }

        return mOffset;
    }
}
