package org.sebbas.android.interfaces;

import java.util.ArrayList;
import java.util.List;

public interface AdapterCallback<T> {
    public void refreshAdapter();
    public void updateAdapterContent(ArrayList<T> list);
    public void updateImagePaths(ArrayList<List <String>> imagePaths);
}
