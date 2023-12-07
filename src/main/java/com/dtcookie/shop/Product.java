package com.dtcookie.shop;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class Product {

    private static final Random RAND = new Random(System.currentTimeMillis());

    private final String ID = UUID.randomUUID().toString();
    private final String name;
    private int quantity = 0;

    public Product(final String name) {
        this.name = name;
    }

    public String getID() {
        return this.ID;
    }

    public String getName() {
        return this.name;
    }

    public int getPrice() {
        return this.name.length() * 15;
    }

    @Override
    public String toString() {
        return this.getName();
    }

    public int getQuantity() {
        return this.quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public static Product random() {
        return PRODUCTS_BY_NAME.get(PRODUCT_NAMES[RAND.nextInt(PRODUCT_NAMES.length)]);
    }

    public static Product get(String name) {
        return PRODUCTS_BY_NAME.get(name);
    }

    public static Product getByID(String id) {
        return PRODUCTS_BY_ID.get(id);
    }    

    private static final String[] PRODUCT_NAMES = new String[] {
        "Alcohol",
        "Ayahuasca",
        "Weed",
        "Benzos",
        "Cocaine",
        "GHB",
        "Hallucinogens",
        "Heroin",
        "Inhalants",
        "Ketamine",
        "Khat",
        "Kratom",
        "LSD",
        "Ecstasy",
        "Mescaline",
        "Methamphetamine",
        "Dextromethorphan",
        "Loperamide",
        "Angel Dust",
        "Oxy",
        "Speed",
        "Magic Mushrooms",
        "Roofies",
        "Salvia",
        "Steroids",
        "Spice",
        "Flakka",
        "Tobacco",        
    };

    private static Map<String, Product> PRODUCTS_BY_ID = new HashMap<>();
    private static Map<String, Product> PRODUCTS_BY_NAME = makeProductsByName();

    private static Map<String, Product> makeProductsByName() {
        Map<String, Product> products = new HashMap<>();
        for (String name : PRODUCT_NAMES) {
            Product product = new Product(name);
            products.put(name, product);
            if (PRODUCTS_BY_ID == null) {
                PRODUCTS_BY_ID = new HashMap<>();
            }
            PRODUCTS_BY_ID.put(product.getID(), product);
        }
        return products;
    }    
}
