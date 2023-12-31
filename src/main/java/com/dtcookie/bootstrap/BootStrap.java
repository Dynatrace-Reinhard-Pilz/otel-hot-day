package com.dtcookie.bootstrap;

import java.io.File;

import com.dtcookie.shop.backend.BackendServer;
import com.dtcookie.shop.frontend.FrontendServer;

public class BootStrap {

    public static void main(String[] args) throws Throwable {
    	String purpose = System.getenv("DEMO_PURPOSE");
    	if (purpose != null) {
    		switch (purpose) {
    		case "FRONTEND":
    			FrontendServer.submain(args);
    			break;
    		case "BACKEND":
    			BackendServer.submain(args);
    			break;
    		case "LOAD":
    			LoadGenerator.submain(args);
    			break;
    		}
    		return;
    	}
		deleteDirectory(new File("./.tmp"));
        new SubProcess("load-generator", "LOAD", "load-generator", false).execute();
        new SubProcess("order-api", "FRONTEND", "order-api", true).execute();
        new SubProcess("order-backend", "BACKEND", "order-backend", false).execute();

        synchronized (BootStrap.class) {
            BootStrap.class.wait();
        }
    	
    }

	private static boolean deleteDirectory(File directoryToBeDeleted) {
		File[] allContents = directoryToBeDeleted.listFiles();
		if (allContents != null) {
			for (File file : allContents) {
				deleteDirectory(file);
			}
		}
		return directoryToBeDeleted.delete();
	}

}
