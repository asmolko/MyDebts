package com.dao.mydebts;

import com.dao.mydebts.dto.DebtsRequest;
import com.dao.mydebts.dto.DebtsResponse;
import com.dao.mydebts.entities.Actor;
import com.google.gson.Gson;

import junit.framework.Assert;

import org.junit.Test;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Simple debt request integration test.
 * Uses assumed test IDs.
 *
 * Created by Oleg Chernovskiy on 05.04.16.
 */
public class ServerAvailabilitySmokeTest {

    private static final Gson mJsonSerializer = new Gson();
    private static final OkHttpClient mHttpClient = new OkHttpClient();
    private static final String SERVER_URL = "http://sorseg.ru:1337/srv/dao.groovy";

    @Test
    public void testDebtRequest() throws IOException {
        DebtsRequest postData = new DebtsRequest();
        Actor me = new Actor();
        me.setId("TEST-ID");
        postData.setMe(me);

        Request postQuery = new Request.Builder()
                .url(SERVER_URL)
                .post(RequestBody.create(Constants.JSON_MIME_TYPE, mJsonSerializer.toJson(postData)))
                .build();

        // send request across the network
        Response response = mHttpClient.newCall(postQuery).execute();
        Assert.assertEquals("Should return HTTP OK", true, response.isSuccessful());

        String result = response.body().string();
        Assert.assertNotNull("Deserialized answer must not be empty", result);

        DebtsResponse answer = mJsonSerializer.fromJson(result, DebtsResponse.class);
        Assert.assertEquals("Answer must contain the same ID as requested", answer.getMe().getId(), me.getId());

        Assert.assertNotNull("Answer must contain debt list", answer.getDebts());
        Assert.assertFalse("There should be non-empty debt list", answer.getDebts().isEmpty());
    }

}
