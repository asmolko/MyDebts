package com.dao.mydebts

import com.google.gson.GsonBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.GsonHttpMessageConverter

/**
 * @author Oleg Chernovskiy
 */
@Configuration
class AppConfig {

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
