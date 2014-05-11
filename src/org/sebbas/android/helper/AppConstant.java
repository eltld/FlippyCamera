package org.sebbas.android.helper;

import java.util.Arrays;
import java.util.List;

import android.os.Environment;

public class AppConstant {

    public static final int NUM_OF_COLUMNS_FOLDERVIEW = 2; // Number of columns of Grid View
    public static final int NUM_OF_COLUMNS_GALLERYVIEW = 3; // Number of columns of Grid View
    public static final int GRID_PADDING = 7; // Gridview image padding (in dp)
    public static final String PHOTO_ALBUM = Environment.DIRECTORY_PICTURES + "/FlickCam"; // SD card image directory
    public static final List<String> FILE_EXTN = Arrays.asList("jpg", "jpeg", "png", "gif"); // supported file formats
    public static final String DEFAULT_FOLDER_NAME = "Untitled Folder";

}
