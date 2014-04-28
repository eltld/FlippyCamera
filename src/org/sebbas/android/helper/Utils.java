package org.sebbas.android.helper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
 
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Environment;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;
 
public class Utils {
 
    private Context mContext;

    public Utils(Context context) {
        this.mContext = context;
    }
 
    // Reading file paths from SDCard
    public ArrayList<String> getFilePaths() {
        ArrayList<String> filePaths = new ArrayList<String>();

        File directory = new File(android.os.Environment.getExternalStorageDirectory()
                        + File.separator + AppConstant.PHOTO_ALBUM);

        // Check for directory
        if (directory.isDirectory()) {
            // getting list of file paths
            File[] listFiles = directory.listFiles();

            // Check for count
            if (listFiles.length > 0) {

                // Loop through all files
                for (int i = 0; i < listFiles.length; i++) {

                    // Get the file path
                    String filePath = listFiles[i].getAbsolutePath();

                    // Check for supported file extension
                    if (IsSupportedFile(filePath)) {
                        // Add image path to array list
                        filePaths.add(filePath);
                    }
                }
            } else {
                // Image directory is empty
                Toast.makeText(mContext, AppConstant.PHOTO_ALBUM + " is empty. Please load some images in it !",
                        Toast.LENGTH_LONG).show();
            }

        } else {
            AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
            alert.setTitle("Error!");
            alert.setMessage(AppConstant.PHOTO_ALBUM
                    + " directory path is not valid! Please set the image directory name AppConstant.java class");
            alert.setPositiveButton("OK", null);
            alert.show();
        }
        return filePaths;
    }
    
    // Check supported file extensions
    private boolean IsSupportedFile(String filePath) {
        String ext = filePath.substring((filePath.lastIndexOf(".") + 1),
                filePath.length());

        if (AppConstant.FILE_EXTN.contains(ext.toLowerCase(Locale.getDefault()))) {
            return true;
        } else {
            return false;
        }
    }
 
    /*
     * Getting the screen width
     */
    @SuppressLint("NewApi")
    public int getScreenWidth() {
        int columnWidth;
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
 
        final Point point = new Point();
        try {
            display.getSize(point);
        } catch (java.lang.NoSuchMethodError ignore) { // Older device
            point.x = display.getWidth();
            point.y = display.getHeight();
        }
        columnWidth = point.x;
        return columnWidth;
    }
    
    // This method should be used in a threaded context as it uses a LOT of resources
    // Returns a list of lists. A list in this list contains all image paths of a folder with images.
    public ArrayList<List<String>> getImagePaths(final boolean alsoHiddenImages) {
        File externalStorage = Environment.getExternalStorageDirectory();
        return loopOverFiles(externalStorage, new ArrayList<List<String>>(), alsoHiddenImages);
    }
    
    private ArrayList<List<String>> loopOverFiles(File folder, ArrayList<List<String>> fileList, boolean alsoHiddenImages) {
        File[] allFiles = folder.listFiles();
        if (allFiles != null) {
            ArrayList<String> imagePaths = new ArrayList<String>();
            
            // Only enter the loop if the current directory does not contain a '.nomedia' file
            if (!containsNomediaFile(allFiles) || alsoHiddenImages) {
                for (File file : allFiles) {
                    if (file.isDirectory()) {
                        if (alsoHiddenImages && !file.getName().equals("Android")) {
                            loopOverFiles(file, fileList, alsoHiddenImages);
                        } else if (!file.isHidden() && !file.getName().equals("Android")){
                            loopOverFiles(file, fileList, alsoHiddenImages);
                        }
                    } else if (fileIsImage(file)) {
                        imagePaths.add(file.getAbsolutePath());
                    } 
                    
                }
                if (imagePaths.size() != 0) {
                    fileList.add(imagePaths);
                }
            }
        }
        return fileList;
    }
    
    private boolean containsNomediaFile(File[] allFiles) {
        boolean result = false;
        for (File file : allFiles) {
            if (file.getName().equals(".nomedia")) {
                result = true;
            }
        }
        return result;
    }
    
    private boolean fileIsImage(File file) {
        for (String ext : AppConstant.FILE_EXTN) {
            if (file.getAbsolutePath().endsWith("." + ext)) return true; 
        }
        return false;
    }
}
