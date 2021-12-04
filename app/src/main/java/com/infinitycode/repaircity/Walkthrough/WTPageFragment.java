package com.infinitycode.repaircity.walkthrough;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.infinitycode.repaircity.R;

/**
 * Created by Infinity Code on 14/08/15.
 */

public class WTPageFragment extends Fragment {

    private int page_number;

    public WTPageFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        switch (page_number) {
            case 0:
                return inflater.inflate(R.layout.fragment_wt_page1, container, false);
            case 1:
                return inflater.inflate(R.layout.fragment_wt_page2, container, false);
            case 2:
                return inflater.inflate(R.layout.fragment_wt_page3, container, false);
            case 3:
                return inflater.inflate(R.layout.fragment_wt_page4, container, false);
            case 4:
                return inflater.inflate(R.layout.fragment_wt_page5, container, false);
            case 5:
                return inflater.inflate(R.layout.fragment_wt_page6, container, false);

        }
        return null;
    }

    public void setPage(int page_number) {
        this.page_number = page_number;
    }
}
