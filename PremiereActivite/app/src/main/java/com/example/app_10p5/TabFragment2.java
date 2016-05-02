package com.example.app_10p5;

import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.app.Fragment;

/**
 * Created by beaus on 24/04/2016.
 */

public class TabFragment2 extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        View v = inflater.inflate(R.layout.tab_fragment_2, container, false);;

        EditText et = (EditText) v.findViewById(R.id.commande_prix);
        et.setFilters(new InputFilter[]{new DecimalDigitsInputFilter(3, 2)});

        return v;
    }
}
