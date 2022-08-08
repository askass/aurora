package tech.aurorafin.aurora;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;


public class BalanceRow extends LinearLayout implements View.OnClickListener{
    public int index;
    TextView label, sum;
    BalanceRowCallback balanceRowCallback;

    public BalanceRow(Context context, BalanceRowCallback balanceRowCallback, String name, String sumT, int index, int Dp2, int Dp10, int Dp40, int Dp110, int labelTxtColor) {
        super(context);
        this.index = index;
        this.balanceRowCallback = balanceRowCallback;

        setOrientation(LinearLayout.HORIZONTAL);
        this.setClickable(true);
        this.setFocusable(false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Dp40);
        setLayoutParams(params);
        setBackground(ContextCompat.getDrawable(context, R.drawable.custom_ripple));
        setOnClickListener(this);

        label = new TextView(context);
        label.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Dp110, LinearLayout.LayoutParams.MATCH_PARENT);
        label.setLayoutParams(lp);
        label.setTextColor(labelTxtColor);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setSingleLine(true);
        label.setPadding(Dp10, 0, 0, 0 );
        label.setText(name);

        sum = new TextView(context);
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(0, Dp40, 1f);
        sum.setLayoutParams(lp1);
        sum.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        sum.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        sum.setTextColor(labelTxtColor);
        sum.setPadding(0, 0, Dp2, 0);
        sum.setText(sumT);

        addView(label);
        addView(sum);



    }

    public void updateLabel(String val) {
        sum.setText(val);
    }

    @Override
    public void onClick(View view) {
        balanceRowCallback.balanceRowClicked(this.index);
    }


    interface BalanceRowCallback{
        void balanceRowClicked(int index);
    }
}

