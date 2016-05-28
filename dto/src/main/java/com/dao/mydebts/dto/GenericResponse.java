package com.dao.mydebts.dto;

/**
 * Generic server response used in various calls
 *
 * @author Oleg Chernovskiy on 25.05.16.
 */
public class GenericResponse {

    private String result;
    private String newId;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getNewId() {
        return newId;
    }

    public void setNewId(String newId) {
        this.newId = newId;
    }
}
