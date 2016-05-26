package com.dao.mydebts.dto;

/**
 * Generic server response used in various calls
 *
 * @author Oleg Chernovskiy on 25.05.16.
 */
public class GenericResponse {

    private String result;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
