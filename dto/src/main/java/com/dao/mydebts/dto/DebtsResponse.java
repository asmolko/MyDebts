package com.dao.mydebts.dto;

import com.dao.mydebts.entities.DebtInfo;
import com.dao.mydebts.entities.Person;

import java.util.List;

/**
 * The second DTO to be seen in conversation. The server sends this in
 * response to {@link DebtsRequest}
 * 
 * Created by Oleg Chernovskiy on 29.03.16.
 */
public class DebtsResponse {
    
    private Person me;
    
    private List<DebtInfo> debts;

    public Person getMe() {
        return me;
    }

    public void setMe(Person me) {
        this.me = me;
    }

    public List<DebtInfo> getDebts() {
        return debts;
    }

    public void setDebts(List<DebtInfo> debts) {
        this.debts = debts;
    }
}
