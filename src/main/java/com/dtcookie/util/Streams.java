package com.dtcookie.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Supplier;

public interface Streams {

	public static void close(InputStream in) {
		if (in == null) {
			return;
		}
		try {
			in.close();
		} catch (IOException e) {
			// ignore
		}
	}

	public static void close(OutputStream out) {
		if (out == null) {
			return;
		}
		try {
			out.close();
		} catch (IOException e) {
			// ignore
		}
	}	

	public static byte[] readAll(InputStream in) throws IOException {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			copy(in, out);
			return out.toByteArray();
		}
	}

	public static byte[] readAll(Supplier<InputStream> in) throws IOException {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			copy(in, out);
			return out.toByteArray();
		}
	}
	
	public static void copy(byte[] in, Supplier<OutputStream> provider) throws IOException {
		try (ByteArrayInputStream bin = new ByteArrayInputStream(in)) {
			copy(in, provider.get());
		}		
	}
	
	public static void copy(byte[] in, OutputStream out) throws IOException {
		try (ByteArrayInputStream bin = new ByteArrayInputStream(in)) {
			copy(bin, out);
		}		
	}

	public static void copy(Supplier<InputStream> in, Supplier<OutputStream> out) throws IOException {
		copy(in.get(), out.get());
	}

	public static void copy(Supplier<InputStream> in, OutputStream out) throws IOException {
		copy(in.get(), out);
	}
	
	public static void copy(InputStream in, OutputStream out) throws IOException {
		byte[] buf = new byte[4096];
		int len = in.read(buf);
		while (len > 0) {
			out.write(buf, 0, len);
			len = in.read(buf);
		}
		out.flush();
	}

	public static void drain(Supplier<InputStream> in) throws IOException {
		drain(in.get());
	}

	public static void drain(InputStream in) throws IOException {
		byte[] buf = new byte[4096];
		int len = in.read(buf);
		while (len > 0) {
			len = in.read(buf);
		}
	}
	
}
