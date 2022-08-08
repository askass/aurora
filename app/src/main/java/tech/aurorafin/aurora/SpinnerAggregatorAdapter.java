package tech.aurorafin.aurora;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import tech.aurorafin.aurora.dbRoom.Aggregator;

import java.util.ArrayList;
import java.util.List;

public class SpinnerAggregatorAdapter extends ArrayAdapter {

    private Context mContext;
    private ArrayList<Aggregator> aggregators = new ArrayList<>();


    public SpinnerAggregatorAdapter(@NonNull Context context, int resource, ArrayList<Aggregator> aggregators) {
        super(context, resource, aggregators);
        mContext = context;
        this.aggregators = aggregators;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView label = (TextView) super.getDropDownView(position, convertView, parent);
        label.setText(aggregators.get(position).name);
        return label;

    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TextView label = (TextView) super.getView(position, convertView, parent);
        label.setText(aggregators.get(position).name);
        return label;
    }

    public SpinnerAggregatorAdapter(@NonNull Context context, int resource) {
        super(context, resource);
    }

    public SpinnerAggregatorAdapter(@NonNull Context context, int resource, int textViewResourceId) {
        super(context, resource, textViewResourceId);
    }

    public SpinnerAggregatorAdapter(@NonNull Context context, int resource, @NonNull Object[] objects) {
        super(context, resource, objects);
    }

    public SpinnerAggregatorAdapter(@NonNull Context context, int resource, int textViewResourceId, @NonNull Object[] objects) {
        super(context, resource, textViewResourceId, objects);
    }

    public SpinnerAggregatorAdapter(@NonNull Context context, int resource, @NonNull List objects) {
        super(context, resource, objects);
    }

    public SpinnerAggregatorAdapter(@NonNull Context context, int resource, int textViewResourceId, @NonNull List objects) {
        super(context, resource, textViewResourceId, objects);
    }
}
