package tech.aurorafin.aurora;

import android.content.Context;
import android.os.Build;
import android.text.method.KeyListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;

import androidx.annotation.RequiresApi;

public class SpecEditText extends androidx.appcompat.widget.AppCompatEditText {


    public SpecEditText(Context context) {
        super(context);
    }

    public SpecEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SpecEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d("MyTag", "onKeyDown " + keyCode);
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        Log.d("MyTag", "onKeyPreIme " + keyCode);
        return super.onKeyPreIme(keyCode, event);
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        Log.d("MyTag", "onKeyMultiple " + keyCode);
        return super.onKeyMultiple(keyCode, repeatCount, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d("MyTag", "onKeyUp " + keyCode);
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        Log.d("onKeyShortcut", "dispatchKeyEvent ");
        return super.onKeyShortcut(keyCode, event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.d("MyTag", "dispatchKeyEvent ");
        return super.dispatchKeyEvent(event);
    }
}
