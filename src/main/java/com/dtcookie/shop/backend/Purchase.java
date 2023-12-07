package com.dtcookie.shop.backend;

import java.util.Collections;
import java.util.concurrent.Callable;

import com.dtcookie.shop.frontend.FrontendServer;
import com.dtcookie.util.Http;

public class Purchase {

    private final String id;

    public Purchase(String id) {
        this.id = id;
    }

    public static Callable<String> confirm(String id) {
        return new Purchase(id).confirm();
    }
    
    public Callable<String> confirm() {
        return new Callable<String>() {
            @Override
            public String call() throws Exception {
                return Http.Jodd.GET("http://localhost:" + FrontendServer.LISTEN_PORT + "/purchase-confirmed", Collections.singletonMap("product.id",  id));
            }
        };        
    }
}
