package mydebts;

import com.dao.mydebts.dto.DebtsRequest;
import com.dao.mydebts.entities.Actor;
import com.dao.mydebts.entities.Debt;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.*;

import java.math.BigDecimal;
import java.util.Date;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {

    private static final String DEBT_INFO_EXPECTED = "{\n" +
            "  \"src\": {\n" +
            "    \"id\": \"ebb558f9-3c16-4889-9603-d97dce150c15\"\n" +
            "  },\n" +
            "  \"dest\": {\n" +
            "    \"id\": \"aef06198-f7be-4886-bf00-19045b2d48ea\"\n" +
            "  },\n" +
            "  \"amount\": 500,\n" +
            "  \"created\": \"1970-01-01T03:00:00 +0300\",\n" +
            "  \"approvedBySrc\": true,\n" +
            "  \"approvedByDest\": false\n" +
            "}";

    @Test
    @Ignore
    public void gsonTest() throws Exception {
        Actor orderer = new Actor();
        orderer.setId("ebb558f9-3c16-4889-9603-d97dce150c15");

        Actor cParty = new Actor();
        cParty.setId("aef06198-f7be-4886-bf00-19045b2d48ea");

        Debt dInfo = new Debt();
        dInfo.setSrc(orderer);
        dInfo.setDest(cParty);
        dInfo.setCreated(new Date(0));
        dInfo.setAmount(new BigDecimal(500));

        Gson marshaller = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss Z")
                .setPrettyPrinting().create();
        String json = marshaller.toJson(dInfo);

        Assert.assertEquals(json, DEBT_INFO_EXPECTED);
    }

    private static final String DATA_REQUEST_EXPECTED = "{\n" +
            "  \"me\": {\n" +
            "    \"id\": \"ebb558f9-3c16-4889-9603-d97dce150c15\"\n" +
            "  }\n" +
            "}";

    @Test
    @Ignore
    public void drequestTest() throws Exception {
        Actor me = new Actor();
        me.setId("ebb558f9-3c16-4889-9603-d97dce150c15");

        DebtsRequest dr = new DebtsRequest();
        dr.setMe(me);

        Gson marshaller = new GsonBuilder().setPrettyPrinting().create();
        String json = marshaller.toJson(dr);

        Assert.assertEquals(json, DATA_REQUEST_EXPECTED);
    }
}