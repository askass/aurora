package tech.aurorafin.aurora;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;


public class CategoryDialog extends DialogFragment {

    RecyclerView select_dialog_rv;

    public CategoryDialog() {
        super();
    }

    public void setAdapter(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter){
        select_dialog_rv.setAdapter(adapter);
    }



    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View view = inflater.inflate(R.layout.category_dialog, null);
        select_dialog_rv = view.findViewById(R.id.select_dialog_rv);

        builder.setView(view)

                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        CategoryDialog.this.getDialog().cancel();
                    }
                });
        return builder.create();
    }
}
