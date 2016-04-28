package com.dao.mydebts;

import okhttp3.MediaType;

/**
 * @author Oleg Chernovskiy on 05.04.16.
 */
public class Constants {

    //private static final String SERVER_ENDPOINT = "sorseg.ru:8080/debt/";
    private static final String SERVER_ENDPOINT = "http://demoth.no-ip.org:8080/debt/";

    public static final String SERVER_ENDPOINT_DEBTS = SERVER_ENDPOINT + "debts";

    public static final MediaType JSON_MIME_TYPE = MediaType.parse("text/json");

    public static final int DEBT_REQUEST_LOADER = 0;

    private Constants() {
    }


}
