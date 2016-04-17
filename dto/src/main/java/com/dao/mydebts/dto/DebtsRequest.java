package com.dao.mydebts.dto;

import com.dao.mydebts.entities.Actor;

/**
 * This is the first DTO to be sent in conversation.
 * It is a subject to subsequent additions, so created as class instead of reusing Actor.
 *
 * @author Oleg Chernovskiy on 29.03.16.
 */
public class DebtsRequest {

    private Actor me;

    public Actor getMe() {
        return me;
    }

    public void setMe(Actor me) {
        this.me = me;
    }
}
