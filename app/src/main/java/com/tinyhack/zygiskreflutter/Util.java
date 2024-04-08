package com.tinyhack.zygiskreflutter;

import java.io.FileInputStream;
import java.io.FileOutputStream;

public class Util {
    public static  void saveProxyIPToFile(String filesdir, String ip) {
        String path = filesdir + "/proxyip.txt";
        try {
            FileOutputStream fos = new FileOutputStream(path);
            //reformat IP so that it will have 3 digits for each part
            String[] parts = ip.split("\\.");
            for (int i = 0; i < parts.length; i++) {
                int val = Integer.parseInt(parts[i]);
                if (val < 100) {
                    //represent as octal
                    parts[i] = String.format("%03o", val);
                }
            }
            ip = String.join(".", parts);
            fos.write(ip.getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getPoxyIPFromFile(String filesdir) {
        String path = filesdir + "/proxyip.txt";
        try {
            FileInputStream fis = new FileInputStream(path);
            byte[] data = new byte[(int) fis.available()];
            fis.read(data);
            fis.close();
            String ip = new String(data);
            //reformat IP so that it will have normal decimal
            String[] parts = ip.split("\\.");
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].startsWith("0")) {
                    //represent as octal
                    parts[i] = String.valueOf(Integer.parseInt(parts[i], 8));
                }
            }
            ip = String.join(".", parts);
            return ip;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
