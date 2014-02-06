package org.sebbas.android.interfaces;

import java.util.ArrayList;

import android.support.v4.app.Fragment;

public interface ProgressListener {
    public void onCompletion(ArrayList<Fragment> fragmentList);
    public void onProgressUpdate(int value);
}
