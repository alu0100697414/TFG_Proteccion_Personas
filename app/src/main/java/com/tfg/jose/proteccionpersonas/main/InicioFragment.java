package com.tfg.jose.proteccionpersonas.main;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tfg.jose.proteccionpersonas.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class InicioFragment extends Fragment {

    public InicioFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_inicio, container, false);
    }
}
