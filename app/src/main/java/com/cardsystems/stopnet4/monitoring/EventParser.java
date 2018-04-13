package com.cardsystems.stopnet4.monitoring;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Maxwell on 21.03.2018.
 */

public class EventParser {

    public static String separator = ";,;";

    public static String endOfAnswer = "!@e\n";

    public static int colCount = 8;

    private static String oldTime = null;

    public static EventData getEventData(String event){
        try {
            if (event.length() < 10) {
                return null;
            }

            //check if was changes in db
            String newTime = event.substring(0, event.indexOf(separator));
            //Log.e("PARSER",oldTime +" "+newTime);

            if(oldTime != null) {
                if( compareTime(oldTime, newTime) == false )
                {
                    //Log.e("PARSER", "old");
                    return null;
                }
            }

            //Log.e("monitor", "new");
            oldTime = newTime;

            int indexBeforeFoto = 0;

            for (int i = 0; i < colCount; i++) {
                indexBeforeFoto = event.indexOf(separator, indexBeforeFoto);
                indexBeforeFoto += separator.length();
            }

            if(indexBeforeFoto >= event.length() || indexBeforeFoto <= separator.length()){
                return null;
            }

            String info = event.substring(0, indexBeforeFoto - separator.length());
            info += event.substring(event.indexOf(separator, indexBeforeFoto));

            //SimpleDateFormat df = new SimpleDateFormat("mm:ss");
            return new EventData( getBitmap(event.substring(indexBeforeFoto, event.indexOf(separator, indexBeforeFoto))),
                    info.replaceAll(separator, "\n") /*+ df.format(Calendar.getInstance().getTime())*/);

        }catch (Exception e) {
            Log.e("PARSER ", "Error:", e);
            return null;
        }
    }

    private static Bitmap getBitmap(String foto){
        //  Log.e("img ",foto);

        foto = (foto.length()%2) != 0 ? "0"+foto : foto;
        int len = foto.length();
        byte[] data = new byte[len/2];

        for(int i = 0; i < len; i+=2){
            data[i/2] = (byte) ((Character.digit(foto.charAt(i), 16) << 4) + Character.digit(foto.charAt(i+1), 16));
        }

        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }

    private static boolean compareTime( String first, String second) {

        // Years
        String firstYear = first.substring(0, 4);
        String secondYear = second.substring(0, 4);
        if (Integer.parseInt(firstYear) < Integer.parseInt(secondYear)) {
            return true;
        }
        if (Integer.parseInt(firstYear) > Integer.parseInt(secondYear)) {
            return false;
        }

        // Month
        String firstMonth = first.substring(5, 7);
        String secondMonth = second.substring(5, 7);
        if (Integer.parseInt(firstMonth) < Integer.parseInt(secondMonth)) {
            return true;
        }
        if (Integer.parseInt(firstMonth) > Integer.parseInt(secondMonth)) {
            return false;
        }

        // Days
        String firstDay = first.substring(8, 10);
        String secondDay = second.substring(8, 10);
        if (Integer.parseInt(firstDay) < Integer.parseInt(secondDay)) {
            return true;
        }
        if (Integer.parseInt(firstDay) > Integer.parseInt(secondDay)) {
            return false;
        }

        // Hours
        String firstHours = first.substring(11, 13);
        String secondHours = second.substring(11, 13);
        if (Integer.parseInt(firstHours) < Integer.parseInt(secondHours)) {
            return true;
        }
        if (Integer.parseInt(firstHours) > Integer.parseInt(secondHours)) {
            return false;
        }

        // Minutes
        String firstMinutes = first.substring(14, 16);
        String secondMinutes = second.substring(14, 16);
        if (Integer.parseInt(firstMinutes) < Integer.parseInt(secondMinutes)) {
            return true;
        }
        if (Integer.parseInt(firstMinutes)
                > Integer.parseInt(secondMinutes)) {
            return false;
        }

        // Seconds
        String firstSeconds = first.substring(17, 19);
        String secondSeconds = second.substring(17, 19);
        if (Integer.parseInt(firstSeconds) < Integer.parseInt(secondSeconds)) {
            return true;
        }
        if (Integer.parseInt(firstSeconds) > Integer.parseInt(secondSeconds)) {
            return false;
        }

        // Milliseconds
        String firstMilliseconds = first.substring(20, 23);
        String secondMilliseconds = second.substring(20, 23);
        if (Integer.parseInt(firstMilliseconds) < Integer.parseInt(secondMilliseconds)) {
            return true;
        }
        if (Integer.parseInt(firstMilliseconds) > Integer.parseInt(secondMilliseconds)) {
            return false;
        }

        // if times are equal
        return false;
    }
}
