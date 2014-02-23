package org.sebbas.android.flickcam;

import java.util.ArrayList;

import org.sebbas.android.interfaces.ProgressListener;


import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.util.Log;

public class CameraLoaderFragment extends Fragment {

    private static final String TAG = "camera_loader_fragment";

    private ProgressListener mProgressListener;
    private boolean mResult = false;
    private LoadingTask mTask;

    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log.d(TAG, "ON ATTACH");
        setRetainInstance(true);
    }

    public boolean hasLoaded() {
        return mResult;
    }

    public void removeProgressListener() {
        mProgressListener = null;
    }

    public void setProgressListener(ProgressListener listener) {
        mProgressListener = listener;
    }

    public void startLoading(Context context) {
        mTask = new LoadingTask();
        mTask.execute(context);
    }
    
    private class LoadingTask extends AsyncTask<Context, Integer, ArrayList<Fragment>> {

        private static final String TAG = "loading_task";

        @Override
        protected ArrayList<Fragment> doInBackground(Context... context) {
            // Background work ...
            ArrayList<Fragment> fragmentList = new ArrayList<Fragment>();
            try {
                fragmentList.add(Fragment.instantiate(context[0], CameraFragment.class.getName()));
                fragmentList.add(Fragment.instantiate(context[0], GalleryFragment.class.getName()));
                Thread.sleep(100); // Only to show the startup fragment a little longer
            } catch (Exception e) {
                Log.e(TAG, "Failed to instantiate fragments");
                e.printStackTrace();
            }
            return fragmentList;
        }

        @Override
        protected void onPostExecute(ArrayList<Fragment> fragmentList) {
            mTask = null;
            if (mProgressListener != null) {
                mProgressListener.onCompletion(fragmentList);
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (mProgressListener != null) {
                mProgressListener.onProgressUpdate(values[0]);
            }
        }
    }
}
