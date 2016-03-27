package com.dao.mydebts;

import com.dao.mydebts.entities.DebtInfo;
import com.dao.mydebts.entities.Person;
import com.thoughtworks.xstream.XStream;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.UUID;

import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import static org.junit.Assert.*;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {

    private static final String MARSHALLED = "<debt>\n"+
            "  <from>\n"+
            "    <id>ebb558f9-3c16-4889-9603-d97dce150c15</id>\n"+
            "  </from>\n"+
            "  <to>\n"+
            "    <id>aef06198-f7be-4886-bf00-19045b2d48ea</id>\n"+
            "  </to>\n"+
            "  <amount>500</amount>\n"+
            "</debt>";
    
    @Test
    public void xstreamTest() throws Exception {
        Person orderer = new Person();
        orderer.setId("ebb558f9-3c16-4889-9603-d97dce150c15");
        
        Person cParty = new Person();
        cParty.setId("aef06198-f7be-4886-bf00-19045b2d48ea");

        DebtInfo dInfo = new DebtInfo();
        dInfo.setFrom(orderer);
        dInfo.setTo(cParty);
        dInfo.setAmount(new BigDecimal(500));

        XStream marshaller = new XStream();
        marshaller.autodetectAnnotations(true);
        String xml = marshaller.toXML(dInfo);
        
        assertEquals(xml, MARSHALLED);
    }
}