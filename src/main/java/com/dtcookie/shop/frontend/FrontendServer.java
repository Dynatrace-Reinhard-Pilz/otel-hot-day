package com.dtcookie.shop.frontend;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dtcookie.database.Database;
import com.dtcookie.shop.Product;
import com.dtcookie.shop.backend.BackendServer;
import com.dtcookie.util.Http;
import com.dtcookie.util.Otel;
import com.sun.net.httpserver.HttpExchange;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;

public class FrontendServer {
	
	private static final Logger log = LogManager.getLogger(FrontendServer.class);

	public static final int LISTEN_PORT = 54039;

	private static final OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
	private static final Meter meter = openTelemetry.meterBuilder("manual-instrumentation").setInstrumentationVersion("1.0.0").build();
    private static final LongCounter confirmedPurchasesCounter = meter.counterBuilder("shop.purchases.confirmed").setDescription("Number of confirmed purchases").build();
    private static final LongCounter expectedRevenueCounter = meter.counterBuilder("shop.revenue.expected").setDescription("Expected revenue in dollar").build();
	private static final LongCounter actualRevenueCounter = meter.counterBuilder("shop.revenue.actual").setDescription("Actual revenue in dollar").build();


	public static void submain(String[] args) throws Exception {
		Otel.init();
		log.info("Launching Frontend Server on port " + LISTEN_PORT);
		Http.serve(
			LISTEN_PORT,
			Http.handler("/place-order", FrontendServer::handlePlaceOrder)
			.add("/purchase-confirmed", FrontendServer::handlePurchaseConfirmed)
		);		
	}

	public static String handlePlaceOrder(HttpExchange exchange) throws Exception {
		// log.info("Frontend received request: " + exchange.getRequestURI().toString());
		Product product = Product.random();
		String productID = product.getID();
		reportExpectedRevenue(product);
		try (Connection con = Database.getConnection(10, TimeUnit.SECONDS)) {
			try (Statement stmt = con.createStatement()) {
				stmt.executeUpdate("INSERT INTO orders VALUES (" + productID + ")");
			}
		}
		Http.JDK.GET("http://localhost:" + BackendServer.CREDIT_CARD_LISTEN_PORT + "/validate-credit-card/"+productID);

		return Http.JDK.GET("http://localhost:" + BackendServer.INVENTORY_LISTEN_PORT + "/check-inventory/" + URLEncoder.encode(product.getName(), StandardCharsets.UTF_8));
	}

	public static String handlePurchaseConfirmed(HttpExchange exchange) throws Exception {
		String requestURI = exchange.getRequestURI().toString();
		String productID = requestURI.substring(requestURI.lastIndexOf("/") + 1);
		Product product = Product.getByID(productID);

		reportPurchases(product);
		reportActualRevenue(product);

		return "confirmed";
	}

	private static void reportPurchases(Product product) {
        Attributes attributes = Attributes.of(AttributeKey.stringKey("product"), product.getName());
        confirmedPurchasesCounter.add(1, attributes);
	}


	private static void reportExpectedRevenue(Product product) {
        Attributes attributes = Attributes.of(AttributeKey.stringKey("product"), product.getName());
        expectedRevenueCounter.add(product.getPrice(), attributes);
	}	

	private static void reportActualRevenue(Product product) {
        Attributes attributes = Attributes.of(AttributeKey.stringKey("product"), product.getName());
        actualRevenueCounter.add(product.getPrice(), attributes);
	}	

}
