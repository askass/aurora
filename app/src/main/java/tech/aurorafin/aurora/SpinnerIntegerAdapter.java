package tech.aurorafin.aurora;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class SpinnerIntegerAdapter extends ArrayAdapter {

    private ArrayList<Integer> years;


    public SpinnerIntegerAdapter(@NonNull Context context, int resource, ArrayList<Integer> years) {
        super(context, resource, years);
        this.years = years;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView label = (TextView) super.getDropDownView(position, convertView, parent);
        label.setText(Integer.toString(years.get(position)));
        label.setGravity(Gravity.CENTER_VERTICAL);
        return label;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView label = (TextView) super.getView(position, convertView, parent);
        label.setText(Integer.toString(years.get(position)));
        label.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        return label;
    }

}
