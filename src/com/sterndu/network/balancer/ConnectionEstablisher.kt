@file:JvmName("ConnectionEstablisher")
package com.sterndu.network.balancer

import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.*

object ConnectionEstablisher {
	private val gateways: MutableList<(String, Int) -> Connection> = ArrayList()
	private val hosts: MutableList<(Int) -> Host> = ArrayList()

	init {
		addGateway { address: String, port: Int ->
			ImplConnection(
				address, port
			)
		}
		addHost { port: Int -> ImplHost(port) }
	}

	fun addGateway(gateway: (String, Int) -> Connection) {
		gateways.add(gateway)
	}

	fun addGateway(gateway: (String, Int) -> Connection, index: Int) {
		gateways.add(index, gateway)
	}

	fun addHost(host: (Int) -> Host) {
		hosts.add(host)
	}

	fun addHost(host: (Int) -> Host, index: Int) {
		hosts.add(index, host)
	}

	@Throws(IOException::class)
	fun establishConnection(address: String, port: Int): Connection? {
		for (bi in gateways) try {
			return bi(address, port)
		} catch (e: IOException) {
			throw IOException("Could not connect:$address:$port")
		}
		return null
	}

	@Throws(IOException::class)
	fun establishConnection(address: String, port: Int, index: Int): Connection {
		return try {
			gateways[index](address, port)
		} catch (e: IOException) {
			throw IOException("Could not connect:$address:$port")
		}
	}

	@Throws(IOException::class)
	fun hostConnection(port: Int): Host? {
		for (bi in hosts) try {
			return bi(port)
		} catch (e: IOException) {
			throw IOException("Could not create:$port")
		}
		return null
	}

	@Throws(IOException::class)
	fun hostConnection(port: Int, index: Int): Host {
		return try {
			hosts[index](port)
		} catch (e: IOException) {
			throw IOException("Could not create:$port")
		}
	}

	interface Connection : Closeable {
		@Throws(IOException::class)
		fun bind(address: InetAddress, port: Int)

		@Throws(IOException::class)
		fun bind(sa: SocketAddress)

		@Throws(IOException::class)
		fun bind(address: String, port: Int)

		@Throws(IOException::class)
		fun connect(address: InetAddress, port: Int)

		@Throws(IOException::class)
		fun connect(address: InetAddress, port: Int, timeout: Int)

		@Throws(IOException::class)
		fun connect(sa: SocketAddress)

		@Throws(IOException::class)
		fun connect(sa: SocketAddress, timeout: Int)

		@Throws(IOException::class)
		fun connect(address: String, port: Int)

		@Throws(IOException::class)
		fun connect(address: String, port: Int, timeout: Int)
		fun getAddress(): InetAddress?

		@Throws(IOException::class)
		fun getInputStream(): InputStream?
		fun  getLocalAddress(): InetAddress
		fun  getLocalPort(): Int

		@Throws(IOException::class)
		fun getOutputStream(): OutputStream?
		fun  getPort(): Int
		fun  isBound(): Boolean
		fun  isClosed(): Boolean
		fun  isConnected(): Boolean

		companion object {
			val s = Socket()
		}
	}

	interface Host : Closeable {
		@Throws(IOException::class)
		fun acceptConnection(): Connection

		@Throws(IOException::class)
		fun bind(address: InetAddress, port: Int)

		@Throws(IOException::class)
		fun bind(sa: SocketAddress)

		@Throws(IOException::class)
		fun bind(address: String, port: Int)
		fun getInetAddress(): InetAddress?
		fun getLocalPort(): Int
		fun getLocalSocketAddress(): SocketAddress?
		fun isBound(): Boolean
		fun isClosed(): Boolean
	}

	class ImplConnection : Socket, Connection {
		constructor()
		constructor(address: InetAddress, port: Int) : super(address, port)
		constructor(sa: InetSocketAddress) : super(sa.hostString, sa.port)
		constructor(s: Socket) {
			super.bind(s.localSocketAddress)
		}

		constructor(address: String, port: Int) : super(address, port)

		@Throws(IOException::class)
		override fun bind(address: InetAddress, port: Int) {
			super.bind(InetSocketAddress(address, port))
		}

		@Throws(IOException::class)
		override fun bind(sa: SocketAddress) {
			super.bind(sa)
		}

		@Throws(IOException::class)
		override fun bind(address: String, port: Int) {
			super.bind(InetSocketAddress(address, port))
		}

		@Throws(IOException::class)
		override fun connect(address: InetAddress, port: Int) {
			super.connect(InetSocketAddress(address, port))
		}

		@Throws(IOException::class)
		override fun connect(address: InetAddress, port: Int, timeout: Int) {
			super.connect(InetSocketAddress(address, port), timeout)
		}

		@Throws(IOException::class)
		override fun connect(sa: SocketAddress) {
			super.connect(sa)
		}

		@Throws(IOException::class)
		override fun connect(sa: SocketAddress, timeout: Int) {
			super.connect(sa, timeout)
		}

		@Throws(IOException::class)
		override fun connect(address: String, port: Int) {
			super.connect(InetSocketAddress(address, port))
		}

		@Throws(IOException::class)
		override fun connect(address: String, port: Int, timeout: Int) {
			super.connect(InetSocketAddress(address, port), timeout)
		}

		override fun getAddress(): InetAddress? = super.getInetAddress()

		override fun getInputStream(): InputStream {
			return super.getInputStream()
		}

		override fun getLocalAddress(): InetAddress = super.getLocalAddress()
		override fun getLocalPort(): Int = super.getLocalPort()
		override fun getOutputStream(): OutputStream? {
			return super.getOutputStream()
		}
		override fun getPort(): Int = super.getPort()
		override fun isBound(): Boolean = super.isBound()
		override fun isClosed(): Boolean = super.isClosed()
		override fun isConnected(): Boolean = super.isConnected()

	}

	class ImplHost : ServerSocket, Host {
		constructor()
		constructor(port: Int) : super(port)
		constructor(port: Int, backlog: Int) : super(port, backlog)
		constructor(port: Int, backlog: Int, bindAddr: InetAddress) : super(port, backlog, bindAddr)

		@Throws(IOException::class)
		override fun acceptConnection(): Connection {
			val c = ImplConnection()
			implAccept(c)
			return c
		}

		@Throws(IOException::class)
		override fun bind(address: InetAddress, port: Int) {
			super.bind(InetSocketAddress(address, port))
		}

		@Throws(IOException::class)
		override fun bind(address: String, port: Int) {
			super.bind(InetSocketAddress(address, port))
		}

		override fun getInetAddress(): InetAddress? = super.getInetAddress()
		override fun getLocalPort(): Int = super.getLocalPort()
		override fun getLocalSocketAddress(): SocketAddress? = super.getLocalSocketAddress()
		override fun isBound(): Boolean = super.isBound()
		override fun isClosed(): Boolean = super.isClosed()
	}
}
