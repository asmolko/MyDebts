package com.dao.mydebts;

import okhttp3.MediaType;

/**
 * @author Oleg Chernovskiy on 05.04.16.
 */
public class Constants {

    // preferences-related
    // @see xml/*_prefs.xml
    public static final  String PREF_SERVER_ADDRESS = "server.address";

    // network-related
    public static final String DEFAULT_SERVER_ENDPOINT = "sorseg.ru:1337";

    // derived from server REST-controllers
    public static final String SERVICE_DEBTS = "debt";
    public static final String SERVICE_AUDIT = "audit";

    // derived from debt REST-controller methods
    public static final String PATH_DEBTS = "debts";
    public static final String PATH_CREATE = "createDebt";
    public static final String PATH_APPROVE = "approve";
    public static final String PATH_DELETE = "delete";

    // derived from audit REST-controller methods
    public static final String PATH_FOR_DEBT = "forDebt";
    public static final String PATH_FOR_USER = "forUser";
    public static final String PATH_FOR_GROUP = "forGroup";

    public static final MediaType JSON_MIME_TYPE = MediaType.parse("application/json");

    public static final int DEBT_REQUEST_LOADER = 0;

    private Constants() {
    }


}
