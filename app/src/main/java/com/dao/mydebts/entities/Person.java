package com.dao.mydebts.entities;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Represents a unique contact person.
 * Persons are independent from matrices and can be shared
 * between them.
 * 
 * Created by Oleg Chernovskiy on 23.03.16.
 */
@XStreamAlias("person")
public class Person {
    
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
