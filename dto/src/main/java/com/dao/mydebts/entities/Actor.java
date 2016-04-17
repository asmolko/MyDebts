package com.dao.mydebts.entities;

/**
 * Represents a unique contact person.
 *
 * @author Oleg Chernovskiy on 23.03.16.
 */
public class Actor {

    public Actor() {
    }

    public Actor(String id) {
        this.id = id;
    }

    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
