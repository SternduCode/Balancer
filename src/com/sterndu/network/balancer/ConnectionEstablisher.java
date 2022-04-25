package com.sterndu.network.balancer;

import java.io.*;
import java.net.*;
import java.util.*;
import com.sterndu.util.interfaces.*;

public class ConnectionEstablisher {

	public interface Connection extends Closeable {
		Socket s = new Socket();

		void bind(InetAddress address, Integer port) throws IOException;

		void bind(SocketAddress sa) throws IOException;

		void bind(String address, Integer port) throws IOException;

		void connect(InetAddress address, Integer port) throws IOException;

		void connect(InetAddress address, Integer port, int timeout) throws IOException;

		void connect(SocketAddress sa) throws IOException;

		void connect(SocketAddress sa, int timeout) throws IOException;

		void connect(String address, Integer port) throws IOException;

		void connect(String address, Integer port, int timeout) throws IOException;
		InetAddress getAddress();

		InputStream getInputStream() throws IOException;

		InetAddress getLocalAddress();

		int getLocalPort();

		OutputStream getOutputStream() throws IOException;

		int getPort();

		boolean isBound();

		boolean isClosed();

		boolean isConnected();
	}

	public interface Host extends Closeable {

		Connection acceptConnection() throws IOException;

		void bind(InetAddress address, int port) throws IOException;

		void bind(SocketAddress sa) throws IOException;

		void bind(String address, int port) throws IOException;

		InetAddress getInetAddress();

		int getLocalPort();

		SocketAddress getLocalSocketAddress();

		boolean isBound();

		boolean isClosed();

	}

	public static class Impl_Connection extends Socket implements Connection {
		public Impl_Connection() {

		}

		public Impl_Connection(InetAddress address, Integer port) throws IOException {
			super(address, port);
		}

		public Impl_Connection(InetSocketAddress sa) throws UnknownHostException, IOException {
			super(sa.getHostString(), sa.getPort());
		}

		public Impl_Connection(Socket s) throws IOException {
			super.bind(s.getLocalSocketAddress());
		}

		public Impl_Connection(String address, Integer port) throws UnknownHostException, IOException {
			super(address, port);
		}

		@Override
		public void bind(InetAddress address, Integer port) throws IOException {
			super.bind(new InetSocketAddress(address, port));
		}

		@Override
		public void bind(SocketAddress sa) throws IOException {
			super.bind(sa);
		}

		@Override
		public void bind(String address, Integer port) throws IOException {
			super.bind(new InetSocketAddress(address, port));
		}

		@Override
		public void connect(InetAddress address, Integer port) throws IOException {
			super.connect(new InetSocketAddress(address, port));
		}

		@Override
		public void connect(InetAddress address, Integer port, int timeout) throws IOException {
			super.connect(new InetSocketAddress(address, port), timeout);
		}

		@Override
		public void connect(SocketAddress sa) throws IOException {
			super.connect(sa);
		}

		@Override
		public void connect(SocketAddress sa, int timeout) throws IOException {
			super.connect(sa, timeout);
		}

		@Override
		public void connect(String address, Integer port) throws IOException {
			super.connect(new InetSocketAddress(address, port));
		}

		@Override
		public void connect(String address, Integer port, int timeout) throws IOException {
			super.connect(new InetSocketAddress(address, port), timeout);
		}

		@Override
		public InetAddress getAddress() { return super.getInetAddress(); }

		@Override
		public InetAddress getLocalAddress() { return super.getLocalAddress(); }
	}

	public static class Impl_Host extends ServerSocket implements Host {

		public Impl_Host() throws IOException {}

		public Impl_Host(int port) throws IOException {
			super(port);


		}

		public Impl_Host(int port, int backlog) throws IOException {
			super(port, backlog);
		}

		public Impl_Host(int port, int backlog, InetAddress bindAddr) throws IOException {
			super(port, backlog, bindAddr);
		}

		@Override
		public Connection acceptConnection() throws IOException {
			Impl_Connection c = new Impl_Connection();
			implAccept(c);
			return c;
		}

		@Override
		public void bind(InetAddress address, int port) throws IOException {
			super.bind(new InetSocketAddress(address, port));
		}

		@Override
		public void bind(String address, int port) throws IOException {
			super.bind(new InetSocketAddress(address, port));
		}

	}

	private static List<ThrowingBiFunction<String, Integer, Connection, IOException>> gateways = new ArrayList<>();

	private static List<ThrowingFunction<Integer, Host, IOException>> hosts = new ArrayList<>();

	static {
		addGateway(Impl_Connection::new);
		addHost(Impl_Host::new);
	}

	public static void addGateway(ThrowingBiFunction<String, Integer, Connection, IOException> gateway) {
		gateways.add(gateway);
	}

	public static void addGateway(ThrowingBiFunction<String, Integer, Connection, IOException> gateway,int index) {
		gateways.add(index,gateway);
	}

	public static void addHost(ThrowingFunction<Integer, Host, IOException> host) {
		hosts.add(host);
	}

	public static void addHost(ThrowingFunction<Integer, Host, IOException> host, int index) {
		hosts.add(index, host);
	}

	public static Connection establishConnection(String address, int port) throws IOException {
		for (ThrowingBiFunction<String, Integer, Connection, IOException> bi: gateways) try {
			return bi.apply(address, port);
		} catch (IOException e) {
		}
		throw new IOException("Could not connect:" + address + ":" + port);
	}

	public static Connection establishConnection(String address, int port, int index) throws IOException {
		try {
			return gateways.get(index).apply(address, port);
		} catch (IOException e) {
			throw new IOException("Could not connect:" + address + ":" + port);
		}
	}

	public static Host hostConnection(int port) throws IOException {
		for (ThrowingFunction<Integer, Host, IOException> bi: hosts) try {
			return bi.apply(port);
		} catch (IOException e) {
		}
		throw new IOException("Could not create:" + port);
	}

	public static Host hostConnection(int port, int index) throws IOException {
		try {
			return hosts.get(index).apply(port);
		} catch (IOException e) {
			throw new IOException("Could not create:" + port);
		}
	}

}
