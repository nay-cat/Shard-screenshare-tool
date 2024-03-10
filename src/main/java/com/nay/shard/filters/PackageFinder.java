package com.nay.shard.filters;

import android.os.Environment;

import com.nay.shard.MainActivity;

import java.io.File;

public class PackageFinder {

    public static boolean isCheating;

    public static void checkAutoclicker(MainActivity mainActivity){
        isCheating = checkAutoclickPackge(new File(Environment.getExternalStorageDirectory() +
                "/Android/data/"));
        if (isCheating){
            mainActivity.setCheating(true);
        }
    }

    public static boolean checkAutoclickPackge(File directory) {
        if (directory.exists() && directory.isDirectory()) {
            File[] filesasd = directory.listFiles();
            if (filesasd != null) {
                for (File file : filesasd) {
                    if (file.getName().toLowerCase().contains("autoclick")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
