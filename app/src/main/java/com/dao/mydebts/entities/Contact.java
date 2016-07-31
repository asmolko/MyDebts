package com.dao.mydebts.entities;

import com.google.android.gms.plus.model.people.Person;
import com.orm.dsl.Table;

/**
 * Just a wrapper around {@link Person} to be able to access it locally
 * once the buffer is closed.
 *
 * @author Oleg Chernovskiy on 17.04.16.
 */
@Table
public class Contact {

    private Long id; // for db

    private final String imageUrl;
    private final String displayName;
    private final String googleId;

    public Contact() {
        this.imageUrl = "";
        this.displayName = "";
        this.googleId = "";
    }

    public Contact(Person person) {
        // persons are loaded with image, id and display name so no need in null-checks
        this.imageUrl = person.getImage().getUrl();
        this.displayName = person.getDisplayName();
        this.googleId = person.getId();
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns native id that associates this Contact with external auth system
     * @return string representing native ID. Should not be null
     */
    public String getNativeId() {
        return googleId;
    }

    public Actor toActor() {
        return new Actor(googleId);
    }

    /**
     * Returns internal ID associated with app DB
     * @return long representing internal ID. Null if contact is not yet persisted.
     */
    public Long getId() {
        return id;
    }
}
