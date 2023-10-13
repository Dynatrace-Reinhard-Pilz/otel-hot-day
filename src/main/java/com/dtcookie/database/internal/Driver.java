package com.dtcookie.database.internal;

import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

public class Driver implements java.sql.Driver {
	
	private ConnectionListener connectionListener = null;
	
	public static interface ConnectionListener {
		void onConnectionClosed(Connection con);
	}
	
	public Driver setConnectionListener(ConnectionListener listener) {
		synchronized (this) {
			this.connectionListener = listener;	
		}
		return this;
	}

	@Override
	public java.sql.Connection connect(String url, Properties info) throws SQLException {
		return new Connection(url, info, this);
	}
	
	void onConnectionClosed(Connection con) {
		if (con == null) {
			return;
		}
		synchronized (this) {
			if (this.connectionListener != null) {
				this.connectionListener.onConnectionClosed(con);
			}
		}
	}

	@Override
	public boolean acceptsURL(String url) throws SQLException {
		return true;
	}

	@Override
	public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
		return new DriverPropertyInfo[] { new DriverPropertyInfo("driver-property-info-name", "http://localhost:28888") };
	}

	@Override
	public int getMajorVersion() {
		return 1;
	}

	@Override
	public int getMinorVersion() {
		return 0;
	}

	@Override
	public boolean jdbcCompliant() {
		return true;
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		throw new SQLFeatureNotSupportedException("getParentLogger not supported");
	}

}
