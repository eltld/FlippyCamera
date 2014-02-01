package org.sebbas.android.flickcam;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;

public class CameraLoaderFragment extends Fragment {

    /**
     * Classes wishing to be notified of loading progress/completion
     * implement this.
     */
    public interface ProgressListener {
        /**
         * Notifies that the task has completed
         *
         * @param result Double result of the task
         */
        public void onCompletion(boolean result);

        /**
         * Notifies of progress
         *
         * @param value int value from 0-100
         */
        public void onProgressUpdate(int value);
    }

    private ProgressListener mProgressListener;
    private boolean mResult = false;
    private LoadingTask mTask;
    private Object mCamera;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Keep this Fragment around even during config changes
        setRetainInstance(true);
    }

    public boolean getSetupStatus() {
        return mResult;
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

    public void startLoading() {
        mTask = new LoadingTask();
        mTask.execute();
    }
    
    private class LoadingTask extends AsyncTask<Context, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(Context... context) {
            // Background work ...
            try {
                Thread.sleep(2500);
                mResult = true;
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return mResult;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mResult = result;
            mTask = null;
            if (mProgressListener != null) {
                mProgressListener.onCompletion(mResult);
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
