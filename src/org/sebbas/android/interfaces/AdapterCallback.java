package org.sebbas.android.interfaces;

public interface AdapterCallback {
    public void refreshAdapter();
    public void updateAdapterInstanceVariables();
    public void reloadAdapterContent(boolean hiddenFolders);
}
