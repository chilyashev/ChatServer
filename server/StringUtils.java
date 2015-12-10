package server;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by Mihail Chilyashev on 12/7/15.
 * All rights reserved, unless otherwise noted.
 */
public class StringUtils {

    /**
     * Връща md5 hash по подаден низ
     * @param bytesToDigest подадения низ
     * @return md5 hash
     */
    public static String md5(byte[] bytesToDigest) {
        StringBuilder sb = new StringBuilder();
        MessageDigest md5 = null;
        try {
            // Нова инстанция на MessageDigest всеки път, защото не е thread-safe
            md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(bytesToDigest);
            // Конвертиране на digest-а към нормален низ
            for (byte aDigest : digest) {
                sb.append(Integer.toString((aDigest & 0xff) + 0x100, 16).substring(1));
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        
        return sb.toString();
    }
}
