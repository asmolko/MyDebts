package com.dao.mydebts.dto;

import com.dao.mydebts.entities.Person;

/**
 * This is the first DTO to be sent in conversation.
 * It is a subject to subsequent additions, so created as class instead of reusing Person.
 *
 * @author Oleg Chernovskiy on 29.03.16.
 */
public class DebtsRequest {

    private Person me;

    public Person getMe() {
        return me;
    }

    public void setMe(Person me) {
        this.me = me;
    }
}
