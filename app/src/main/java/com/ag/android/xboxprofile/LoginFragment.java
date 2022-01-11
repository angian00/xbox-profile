package com.ag.android.xboxprofile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class LoginFragment extends Fragment {
    public static LoginFragment newInstance() {
        return new LoginFragment();
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_login, container, false);
        EditText emailEdit = v.findViewById(R.id.edit_email);
        EditText passwordEdit = v.findViewById(R.id.edit_password);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String email = prefs.getString("email", null);
        if (email != null)
            emailEdit.setText(email);
        String password = prefs.getString("password", null);
        if (password != null)
            passwordEdit.setText(password);

        Button loginButton = v.findViewById(R.id.button_login);
        loginButton.setOnClickListener(view -> {
            String emailNew = emailEdit.getText().toString();
            String passwordNew = passwordEdit.getText().toString();
            prefs.edit()
                .putString("email", emailNew)
                .putString("password", passwordNew)
                .apply();

            Intent intent = new Intent(getContext(), XboxProfileActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return v;
    }
}
