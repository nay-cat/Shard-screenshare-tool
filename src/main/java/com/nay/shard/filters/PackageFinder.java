package com.nay.shard.filters;

import android.os.Environment;

import com.nay.shard.MainActivity;

import java.io.File;

public class PackageFinder {

    public static boolean isCheating;

    public static void checkAutoclicker(MainActivity mainActivity) {
        isCheating = checkAutoclickPackage(new File(Environment.getExternalStorageDirectory(), "/Android/data/"));
        if (isCheating) {
            mainActivity.setCheating(true);
        }
    }

    public static boolean checkAutoclickPackage(File directory) {
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().toLowerCase().contains("autoclick")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
