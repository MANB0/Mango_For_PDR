package com.example.mango;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class FileSave {
    public static void writeToFile(Context context, String fileName, String data) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss_");
        Date curDate = new Date(System.currentTimeMillis());
        String str  = formatter.format(curDate);
        Log.d("FileSave", str);

        String fileNameString = str + fileName + ".txt";
        Log.d("FileSave", fileNameString);

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), fileNameString);

        try {
            FileWriter writer = new FileWriter(file, true);
            writer.append(data).append("\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
