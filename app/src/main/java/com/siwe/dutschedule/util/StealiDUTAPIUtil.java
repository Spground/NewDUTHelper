package com.siwe.dutschedule.util;

import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by asus on 2016/5/2.
 */
public class StealiDUTAPIUtil {
    private static JSONObject jsonObject;
    static {
        try {
            jsonObject = new JSONObject("{\"imei\":\"\",\"appid\":\"dlut_xntzgl\"}");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static String buildParams(String key, String value) {
        try {
            jsonObject = new JSONObject("{\"imei\":\"\",\"appid\":\"dlut_xntzgl\"}");
            jsonObject.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return jsonObject.toString();
    }

}
