package com.example.banking.util;

import android.text.Editable;
import android.text.TextWatcher;
import java.util.function.Consumer;

public class SimpleTextWatcher implements TextWatcher {

    private final Consumer<String> after;

    private SimpleTextWatcher(Consumer<String> after) {
        this.after = after;
    }

    public static SimpleTextWatcher after(Consumer<String> after) {
        return new SimpleTextWatcher(after);
    }

    public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
    public void onTextChanged(CharSequence s, int a, int b, int c) {}
    public void afterTextChanged(Editable s) {
        after.accept(s.toString());
    }
}
