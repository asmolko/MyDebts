package com.dao.mydebts.entities;

/**
 * Represents a unique contact person.
 * Persons are independent from matrices and can be shared
 * between them.
 *
 * @author Oleg Chernovskiy on 23.03.16.
 */
public class Person {

    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}