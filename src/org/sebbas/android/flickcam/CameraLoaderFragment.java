package org.sebbas.android.flickcam;

import android.app.Activity;
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
        public void onCompletion(Double result);

        /**
         * Notifies of progress
         *
         * @param value int value from 0-100
         */
        public void onProgressUpdate(int value);
    }

    private ProgressListener mProgressListener;
    private Double mResult = Double.NaN;
    private LoadingTask mTask;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Keep this Fragment around even during config changes
        setRetainInstance(true);
    }

    /**
     * Returns the result or {@value Double#NaN}
     *
     * @return the result or {@value Double#NaN}
     */
    public Double getResult() {
        return mResult;
    }

    /**
     * Returns true if a result has already been calculated
     *
     * @return true if a result has already been calculated
     * @see #getResult()
     */
    public boolean hasResult() {
        return !Double.isNaN(mResult);
    }

    /**
     * Removes the ProgressListener
     *
     * @see #setProgressListener(ProgressListener)
     */
    public void removeProgressListener() {
        mProgressListener = null;
    }

    /**
     * Sets the ProgressListener to be notified of updates
     *
     * @param listener ProgressListener to notify
     * @see #removeProgressListener()
     */
    public void setProgressListener(ProgressListener listener) {
        mProgressListener = listener;
    }

    /**
     * Starts loading the data
     */
    public void startLoading() {
        mTask = new LoadingTask();
        mTask.execute();
    }

    private class LoadingTask extends AsyncTask<Void, Integer, Double> 
{

        @Override
        protected Double doInBackground(Void... params) {
            double result = 0;
            for (int i = 0; i < 100; i++) {
                try {
                    result += Math.sqrt(i);
                    Thread.sleep(50);
                    this.publishProgress(i);
                } catch (InterruptedException e) {
                    return null;
                }
            }
            return Double.valueOf(result);
        }

        @Override
        protected void onPostExecute(Double result) {
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
