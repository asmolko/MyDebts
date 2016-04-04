package com.dao.mydebts;

import okhttp3.MediaType;

/**
 * Created by adonai on 05.04.16.
 */
public class Constants {

    public static final String DEFAULT_SERVER_URL = "http://sorseg.ru:1337";

    public static final MediaType JSON_MIME_TYPE = MediaType.parse("text/json");

    public static final int DEBT_REQUEST_LOADER = 0;

    private Constants() {
    }


}
