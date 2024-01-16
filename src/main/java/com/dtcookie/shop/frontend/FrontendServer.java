package com.dtcookie.shop.frontend;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;
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
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jodd.http.HttpRequest;
import jodd.http.HttpResponse;
import io.opentelemetry.context.Context;

public class FrontendServer {
	
	private static final Logger log = LogManager.getLogger(FrontendServer.class);

	public static final int LISTEN_PORT = 54039;

	private static final OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
	private static final Meter meter = openTelemetry.meterBuilder("manual-instrumentation").setInstrumentationVersion("1.0.0").build();
    private static final LongCounter confirmedPurchasesCounter = meter.counterBuilder("shop.purchases.confirmed").setDescription("Number of confirmed purchases").build();
    private static final LongCounter expectedRevenueCounter = meter.counterBuilder("shop.revenue.expected").setDescription("Expected revenue in dollar").build();
	private static final LongCounter actualRevenueCounter = meter.counterBuilder("shop.revenue.actual").setDescription("Actual revenue in dollar").build();
	private static final Tracer tracer = openTelemetry.getTracer("manual-instrumentation");


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
		// log.info("Placing order");
		Product product = Product.random();
		String productID = product.getID();
		reportExpectedRevenue(product);
		try (Connection con = Database.getConnection(10, TimeUnit.SECONDS)) {
			try (Statement stmt = con.createStatement()) {
				stmt.executeUpdate("INSERT INTO orders VALUES (" + productID + ")");
			}
		}
		validateCreditCard(product);
		return checkInventory(product);
	}

	public static void validateCreditCard(Product product) throws Exception {
		Span span = tracer.spanBuilder("validate-credit-card").setSpanKind(SpanKind.INTERNAL).startSpan();
		try (Scope scope = span.makeCurrent()) {
			Http.JDK.GET("http://localhost:" + BackendServer.CREDIT_CARD_LISTEN_PORT + "/validate-credit-card/"+product.getID(), null);
			Span childSpan = tracer.spanBuilder("cc-valid").setParent(Context.current().with(span)).startSpan();
			try {
				Thread.sleep(50);
			} finally {
				childSpan.end();
			}			
		} catch (Exception e) {
			span.recordException(e);
			span.setStatus(StatusCode.ERROR);
			throw e;
		} finally {
			span.end();
		}
	}

	public static String checkInventory(Product product) {
		Span span = tracer.spanBuilder("check-inventory").setSpanKind(SpanKind.INTERNAL).startSpan();
		try (Scope scope = span.makeCurrent()) {
			Span childSpan = tracer.spanBuilder("resolve-inventory-backend").setParent(Context.current().with(span)).startSpan();
			try {
				return Http.JDK.GET("http://localhost:" + BackendServer.INVENTORY_LISTEN_PORT + "/check-inventory/" + URLEncoder.encode(product.getName(), StandardCharsets.UTF_8), null);
			} finally {
				childSpan.end();
			}			
		} catch (Exception e) {
			span.recordException(e);
			span.setStatus(StatusCode.ERROR);
			throw e;
		} finally {
			span.end();
		}		
	}

	public static String handlePurchaseConfirmed(HttpExchange exchange) throws Exception {
		String productID = exchange.getRequestHeaders().get("product.id").get(0);
		Product product = Product.getByID(productID);
		Span span = tracer.spanBuilder("purchase-confirmed").setSpanKind(SpanKind.INTERNAL).startSpan();
		try (Scope scope = span.makeCurrent()) {
			span.setAttribute("product.name", product.getName());
			reportPurchases(product);
			reportActualRevenue(product);
			for (int i = 0; i < 50; i++) {
				Span childSpan = tracer.spanBuilder("persist-purchase-confirmation-" + i).setParent(Context.current().with(span)).startSpan();
				try {
					Thread.sleep(1);
				} finally {
					childSpan.end();
				}
			}
		} catch (Exception e) {
			span.recordException(e);
			span.setStatus(StatusCode.ERROR);
			throw e;
		} finally {
			span.end();
		}		

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
