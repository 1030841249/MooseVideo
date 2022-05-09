package com.zdh.live;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * author: ZDH
 * Date: 2022/4/3
 * Description:
 */
public class FileWriter {
    static File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "codec.h264");

    public  static  void writeBytes(byte[] array) {
        FileOutputStream writer = null;
        try {
            // 打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
            writer = new FileOutputStream(file,true);
            writer.write(array);
//            writer.write('\n');


        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
