package org.sebbas.android.helper;

import java.util.Arrays;
import java.util.List;

import android.os.Environment;

public class AppConstant {

    public static final int NUM_OF_COLUMNS_FOLDERVIEW = 2; // Number of columns of Grid View
    public static final int NUM_OF_COLUMNS_GALLERYVIEW = 3; // Number of columns of Grid View
    public static final int GRID_PADDING = 1; // Gridview image padding (in dp)
    public static final String PICTURES_DIRECTORY = Environment.DIRECTORY_PICTURES; // SD card image directory
    public static final String FOLDER_THUMBNAILS = "Thumbnails"; // SD card image directory
    public static final List<String> FILE_EXTN = Arrays.asList("jpg", "jpeg", "png", "gif"); // supported file formats
    public static final String DEFAULT_FOLDER_NAME = "Untitled Folder";
    public static final String ALBUM_NAME = "Flippy Camera";
    public static final int THUMBNAIL_WIDTH = 256;
    public static final int THUMBNAIL_HEIGHT = 256;
}
