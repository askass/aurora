package tech.aurorafin.aurora.about;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;

import tech.aurorafin.aurora.R;

public class AboutActivity extends AppCompatActivity implements View.OnClickListener{
    AppCompatImageButton about_back_btn;
    TextView helpView, privacyView, usageView, rank_app;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        about_back_btn = findViewById(R.id.about_back_btn);
        about_back_btn.setOnClickListener(this);

        helpView = findViewById(R.id.helpTv);
        helpView.setMovementMethod(LinkMovementMethod.getInstance());

        privacyView = findViewById(R.id.privacyTv);
        privacyView.setMovementMethod(LinkMovementMethod.getInstance());

        usageView = findViewById(R.id.usageTv);
        usageView.setMovementMethod(LinkMovementMethod.getInstance());

        //rank_app = findViewById(R.id.rank_app);
        //rank_app.setMovementMethod(LinkMovementMethod.getInstance());


    }

    @Override
    public void onClick(View view) {
        onBackPressed();
    }
}
