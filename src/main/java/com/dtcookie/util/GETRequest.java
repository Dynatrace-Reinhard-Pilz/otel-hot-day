package com.dtcookie.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public final class GETRequest {

	private final String sURL;
	private final Map<String,String> headers = new HashMap<>();
	
	public GETRequest(String url) {
		this.sURL = url;
	}
	
	public final Map<String,String> headers() {
		return this.headers;
	}
	
	public String send() throws IOException {
		URL url = new URL(this.sURL);
		int port = url.getPort();
		if (port == -1) {
			port = 80;
		}
		String host = url.getHost();

        InetAddress addr = InetAddress.getByName(host);
        SocketAddress sockaddr = new InetSocketAddress(addr, port);
        try (Socket socket = new Socket()) {
            socket.connect(sockaddr, 30000);
            try (InputStream socketIn = socket.getInputStream()) {
            	try (InputStreamReader socketIsr = new InputStreamReader(socketIn)) {
                    try (BufferedReader br = new BufferedReader(socketIsr)) {
                    	try (OutputStream socketOut = socket.getOutputStream()) {
                            try (PrintWriter pw = new PrintWriter(socketOut, true)) {
                                pw.print("GET " + url.getPath() + " HTTP/1.1\r\n");
                                pw.print("Host: "+ host +"\r\n");
                                pw.print("Connection: close\r\n");
                        		for (Entry<String, String> entry : headers.entrySet()) {
                        			pw.print(entry.getKey() + ": " + entry.getValue() + "\r\n");
                        		}                                
                                pw.print("\r\n");
                                pw.flush();
                                
                                String response = "";
                                String line = null;
                                while((line = br.readLine())!=null){
                                	response = response + line + "\n";
                                }
                                return response;
                            }
                    	}
                    }
            	}
            }
        }
	}
}
