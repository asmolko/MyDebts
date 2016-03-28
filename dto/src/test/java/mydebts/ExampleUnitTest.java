package mydebts;

import com.dao.mydebts.dto.DebtsRequest;
import com.dao.mydebts.entities.DebtInfo;
import com.dao.mydebts.entities.Person;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {

    private static final String DEBT_INFO_EXPECTED = "{\n" +
            "  \"from\": {\n" +
            "    \"id\": \"ebb558f9-3c16-4889-9603-d97dce150c15\"\n" +
            "  },\n" +
            "  \"to\": {\n" +
            "    \"id\": \"aef06198-f7be-4886-bf00-19045b2d48ea\"\n" +
            "  },\n" +
            "  \"amount\": 500\n" +
            "}";
    
    @Test
    public void gsonTest() throws Exception {
        Person orderer = new Person();
        orderer.setId("ebb558f9-3c16-4889-9603-d97dce150c15");
        
        Person cParty = new Person();
        cParty.setId("aef06198-f7be-4886-bf00-19045b2d48ea");

        DebtInfo dInfo = new DebtInfo();
        dInfo.setFrom(orderer);
        dInfo.setTo(cParty);
        dInfo.setAmount(new BigDecimal(500));

        Gson marshaller = new GsonBuilder().setPrettyPrinting().create();
        String json = marshaller.toJson(dInfo);
        
        Assert.assertEquals(json, DEBT_INFO_EXPECTED);
    }
    
    private static final String DATA_REQUEST_EXPECTED = "{\n" +
            "  \"me\": {\n" +
            "    \"id\": \"ebb558f9-3c16-4889-9603-d97dce150c15\"\n" +
            "  }\n" +
            "}";
    
    @Test
    public void drequestTest() throws Exception {
        Person me = new Person();
        me.setId("ebb558f9-3c16-4889-9603-d97dce150c15");

        DebtsRequest dr = new DebtsRequest();
        dr.setMe(me);

        Gson marshaller = new GsonBuilder().setPrettyPrinting().create();
        String json = marshaller.toJson(dr);

        Assert.assertEquals(json, DATA_REQUEST_EXPECTED);
    }
}