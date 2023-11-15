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
            "Parmesan cheese",
            "coconut oil",
            "truffles",
            "brown rice",
            "blueberries",
            "Tabasco sauce",
            "angelica",
            "navy beans",
            "ginger ale",
            "ale",
            "soy sauce",
            "chaurice sausage",
            "red chile powder",
            "radishes",
            "eggplants",
            "basil",
            "cumin",
            "ham",
            "tomato juice",
            "pea beans",
            "Irish cream liqueur",
            "corn flour",
            "alligator",
            "vinegar",
            "mackerel",
            "jelly beans",
            "vegemite",
            "Kahlua",
            "poultry seasoning",
            "sazon",
            "bok choy",
            "salsa",
            "crabs",
            "hash browns",
            "white chocolate",
            "pink beans",
            "dumpling",
            "chocolate",
            "coconut milk",
            "spinach",
            "gorgonzola",
            "snow peas",
            "hazelnuts",
            "mesclun greens",
            "clams",
            "mussels",
            "Romano cheese",
            "anchovy paste",
            "bananas",
            "prunes"
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
