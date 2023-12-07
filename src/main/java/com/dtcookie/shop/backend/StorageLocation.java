package com.dtcookie.shop.backend;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class StorageLocation {

    private static final Random RAND = new Random(System.currentTimeMillis());

    private static final String[] LOCATION_NAMES = new String[] {
        "New York",
        "Helsinki",
        "Canberra",
    };

    private static String current = LOCATION_NAMES[0];
    private static final Object lock = new Object();

    private final String name;

    public StorageLocation(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public boolean available(String productName, int quantity) {
        synchronized (lock) {
            if (!this.name.equals(current)) {
                return false;
            }
            current = LOCATION_NAMES[RAND.nextInt(LOCATION_NAMES.length)];
            return true;
        }        
    }

    public void deduct(String productName, int quantity) {
        try {
            Thread.sleep(50);
        } catch (Throwable t) {}
    }

    public static StorageLocation[] getAll() {
        if (RAND.nextInt(5) == 0) {
            return new StorageLocation[0];
        }
      return LOCATIONS_BY_NAME.values().toArray(new StorageLocation[LOCATIONS_BY_NAME.size()]);        
    }

    public static Map<String, StorageLocation> LOCATIONS_BY_NAME = make();

    private static Map<String, StorageLocation> make() {
        Map<String, StorageLocation> locations = new HashMap<>();
        for (String name : LOCATION_NAMES) {
            StorageLocation location = new StorageLocation(name);
            locations.put(name, location);
        }
        return locations;
    }     
}
