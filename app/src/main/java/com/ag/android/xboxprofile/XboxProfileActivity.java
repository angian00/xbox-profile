package com.ag.android.xboxprofile;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;

public class XboxProfileActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return XboxProfileFragment.newInstance();
    }
}