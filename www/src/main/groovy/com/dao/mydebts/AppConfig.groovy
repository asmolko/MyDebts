package com.dao.mydebts

import com.google.gson.GsonBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.GsonHttpMessageConverter

/**
 * Application-wide config
 *
 * @author Oleg Chernovskiy
 */
@Configuration
class AppConfig {

    /**
     * Overrides default gson message converter with alternate one. Don't change this method's
     * name as it masks Spring Boot's auto-configured bean.
     * @return custom gson message converter
     */
    @Bean
    @Qualifier("for-gson")
    GsonHttpMessageConverter gsonHttpMessageConverter() {
        def cv = new GsonHttpMessageConverter();
        cv.gson = new GsonBuilder() // like in android app
            .setDateFormat("yyyy-MM-dd'T'hh:mm:ssZ") // ISO 8601
            .create();
        return cv;
    }


}
