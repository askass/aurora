package tech.aurorafin.aurora;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class SpinnerScaleAdapter extends ArrayAdapter {

    ArrayList<BalanceFragment.DividerLabel> dividerLabels;


    public SpinnerScaleAdapter(@NonNull Context context, int resource, ArrayList<BalanceFragment.DividerLabel> dividerLabels) {
        super(context, resource, dividerLabels);
        this.dividerLabels = dividerLabels;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView label = (TextView) super.getDropDownView(position, convertView, parent);
        label.setText(dividerLabels.get(position).dividerLabel);
        label.setGravity(Gravity.CENTER_VERTICAL);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        return label;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView label = (TextView) super.getView(position, convertView, parent);
        label.setText(dividerLabels.get(position).dividerLabel);
        label.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        return label;
    }

}
