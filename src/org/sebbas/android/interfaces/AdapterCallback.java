package org.sebbas.android.interfaces;

import java.util.ArrayList;

public interface AdapterCallback<T> {
    public void refreshAdapter();
    public void updateAdapterContent(ArrayList<T> list);
}
