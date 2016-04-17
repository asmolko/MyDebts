package com.dao.mydebts.entities;

import com.google.android.gms.plus.model.people.*;

/**
 * Just a wrapper around {@link Person} to be able to access it locally
 * once the buffer is closed.
 *
 * @author Oleg Chernovskiy on 17.04.16.
 */
public class Contact {

    private final String imageUrl;
    private final String displayName;
    private final String id;

    public Contact(Person person) {
        // persons are loaded with image, id and display name so no need in null-checks
        this.imageUrl = person.getImage().getUrl();
        this.displayName = person.getDisplayName();
        this.id = person.getId();
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getId() {
        return id;
    }

    public Actor toActor() {
        return new Actor(id);
    }
}
