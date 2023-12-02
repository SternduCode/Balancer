@file:JvmName("Balancer")
package com.sterndu.network.balancer

import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.Channels
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToLong

class Balancer(private val lat: Int = 100, private val bandwidth: Int = 100, private val power: Int = 100) {
	data class AddressPortTuple(val address: String, val port: Int)

	@Throws(IOException::class)
	private fun clientSideNetworkTest(data: ByteArray, c: ConnectionEstablisher.Connection) {
		val latencyNanosShort: List<Long> = ArrayList()
		val latencyNanosBig: List<Long> = ArrayList()
		val returnData: ByteArray
		val inputStream = c.getInputStream()
		val os = c.getOutputStream()
		val out = Channels.newChannel(os)
		val input = Channels.newChannel(inputStream)
		os?.write(data)
		if (inputStream != null) {
			while (inputStream.available() == 0) try {
				Thread.sleep(1)
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
		val baos = ByteArrayOutputStream()
		if (inputStream != null) {
			while (inputStream.available() > 0) {
				val b = ByteArray(4096)
				val i = inputStream.read(b)
				baos.write(b, 0, i)
			}
		}
		returnData = baos.toByteArray()
		var i = 0
		while (i in 0..19) try {
			val st = System.currentTimeMillis()
			out.write(ByteBuffer.allocate(java.lang.Long.BYTES).order(ByteOrder.BIG_ENDIAN).putLong(st).flip())
			if (inputStream != null) {
				while (inputStream.available() == 0) try {
					Thread.sleep(1)
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
			val sr = System.currentTimeMillis()
			val baos1 = ByteArrayOutputStream()
			if (inputStream != null) {
				while (inputStream.available() > 0) {
					val b1 = ByteArray(4096)
					val i1 = inputStream.read(b1)
					baos1.write(b1, 0, i1)
				}
			}
			val b = baos1.toByteArray()
			val input = ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN)
			println("CS:size:" + b.size)
			println(input.remaining())
			for (j in 0..7) print(input.getLong().toString() + " ")
			println()
			val er = System.currentTimeMillis()
			println("$st\t$sr\t$er")
			i++
		} catch (e: Exception) {
			System.err.println(e)
			e.stackTrace.forEach { e4: StackTraceElement ->
				System.err.println("	$e4")
			}
			i--
		}
		val lostFromAllRuns: MutableList<Double> = LinkedList()
		val random = Random()
		for (i in 0..29) {
			val st = System.currentTimeMillis()
			val bb = ByteBuffer.allocate(java.lang.Long.BYTES * 32).order(ByteOrder.BIG_ENDIAN)
			random.longs(32).sequential().forEach { value: Long -> bb.putLong(value) }
			out.write(bb.flip())
			if (inputStream != null) {
				while (inputStream.available() == 0) try {
					Thread.sleep(1)
				} catch (e: InterruptedException) {
					e.printStackTrace()
				}
			}
			val sr = System.currentTimeMillis()
			val baos1 = ByteArrayOutputStream()
			if (inputStream != null) {
				while (inputStream.available() > 0) {
					val b1 = ByteArray(128)
					val i1 = inputStream.read(b1)
					baos1.write(b1, 0, i1)
				}
			}
			val er = System.currentTimeMillis()
			val b = baos1.toByteArray()
			println("CS:size:" + b.size)
			val lost: Double =
				if (32 * 4096 * 8 + 10 > b.size) (32 * 4096 * 8 + 10 - b.size).toDouble() / (32 * 4096 * 8 + 10) else 0.0
			lostFromAllRuns.add(lost)
			println("$st $sr $er")
		}
		val max = lostFromAllRuns.max()
		val min = lostFromAllRuns.min()
		val av = lostFromAllRuns.average()
		println("$max\t$min\t$av")
	}

	@Throws(IOException::class)
	private fun serverSideNetworkTest(data: ByteArray, c: ConnectionEstablisher.Connection) {
		val `is` = c.getInputStream()
		val os = c.getOutputStream()
		val `in` = Channels.newChannel(`is`)
		val out = Channels.newChannel(os)
		val returnData: ByteArray
		if (`is` != null) {
			while (`is`.available() == 0) try {
				Thread.sleep(1)
			} catch (e: Exception) {
				e.printStackTrace()
			}
		}
		val baos = ByteArrayOutputStream()
		if (`is` != null) {
			while (`is`.available() > 0) {
				val b = ByteArray(256)
				val i = `is`.read(b)
				baos.write(b, 0, i)
			}
		}
		returnData = baos.toByteArray()
		os?.write(data)
		for (i in 0..19) try {
			if (`is` != null) {
				while (`is`.available() == 0) try {
					Thread.sleep(1)
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
			val sr = System.currentTimeMillis()
			val baos1 = ByteArrayOutputStream()
			if (`is` != null) {
				while (`is`.available() > 0) {
					val b1 = ByteArray(4096)
					val i1 = `is`.read(b1)
					baos1.write(b1, 0, i1)
				}
			}
			println("SS:size:" + baos1.size())
			val input = ByteBuffer.wrap(baos1.toByteArray()).order(ByteOrder.BIG_ENDIAN)
			val output = ByteBuffer.allocate(java.lang.Long.BYTES * 8).order(ByteOrder.BIG_ENDIAN)
			output.putLong(input.getLong())
			output.putLong(sr)
			val r = Random()
			output.putLong(r.nextLong())
			output.putLong(r.nextLong())
			output.putLong(r.nextLong())
			output.putLong(r.nextLong())
			output.putLong(r.nextLong())
			output.putLong(System.currentTimeMillis())
			out.write(output.flip())
		} catch (e: Exception) {
			System.err.println(e)
			e.stackTrace.forEach { e4: StackTraceElement? -> System.err.println(e4) }
		}
		for (i in 0..29) {
			if (`is` != null) {
				while (`is`.available() == 0) try {
					Thread.sleep(1)
				} catch (e: Exception) {
					e.printStackTrace()
				}
			}
			val sr = System.currentTimeMillis()
			val baos1 = ByteArrayOutputStream()
			if (`is` != null) {
				while (`is`.available() > 0) {
					val b1 = ByteArray(4096)
					val i1 = `is`.read(b1)
					baos1.write(b1, 0, i1)
				}
			}
			println("SS:size:" + baos1.size())
			val input = ByteBuffer.allocate(32 * 8).order(ByteOrder.BIG_ENDIAN)
			input.put(baos1.toByteArray())
			val output = ByteBuffer.allocate(java.lang.Long.BYTES * 32 * 4096 + 10).order(ByteOrder.BIG_ENDIAN)
			output.putShort(baos1.size().toShort())
			output.putLong(sr)
			for (ii in 0..31) Random(input.getLong(ii * 8)).longs(4096).sequential()
				.forEach { l: Long -> output.putLong(l) }
			out.write(output.flip())
		}
	}

	fun runNetworkTest(data: ByteArray, vararg apt: AddressPortTuple) {
		val ab = AtomicBoolean(false)
		val tg = ThreadGroup("Connections")
		val ss = Thread {
			try {
				val h = ConnectionEstablisher.hostConnection(25555)
				var i = 0
				while ((i < apt.size) and !ab.get()) {
					val c = h?.acceptConnection()
					Thread(tg) {
						try {
							if (c != null) {
								serverSideNetworkTest(data, c)
							}
						} catch (e: IOException) {
							e.printStackTrace()
						}
					}.start()
					i++
				}
			} catch (e: IOException) {
				e.printStackTrace()
			}
		}
		ss.start()
		for (ap in apt) try {
			val c = ConnectionEstablisher.establishConnection(ap.address, ap.port)
			if (c != null) {
				clientSideNetworkTest(data, c)
			}
		} catch (e2: IOException) {
			e2.printStackTrace()
		}
		if (ss.isAlive) {
			var lt: Long = 10000
			var st = System.currentTimeMillis()
			var nt: Long = 0
			while (lt > 0) {
				try {
					Thread.sleep(1)
				} catch (e: InterruptedException) {
					e.printStackTrace()
				}
				lt -= System.currentTimeMillis().also { nt = it } - st
				st = nt
			}
		}
		if (tg.activeCount() > 0) tg.interrupt()
	}

	companion object {
		fun longDurationTest(r: Runnable, millis: Long, cores: Int, checksOverWholeTimeFrame: Int): DoubleArray {
			val ds = DoubleArray(checksOverWholeTimeFrame)
			val counts = IntArray(cores)
			val ths = arrayOfNulls<Thread>(cores)
			for (i in 0 until cores) ths[i] = object : Thread() {
				override fun run() {
					val st = System.currentTimeMillis()
					while (System.currentTimeMillis() < st + millis) {
						r.run()
						counts[i]++
					}
				}
			}
			try {
				var idx = 0
				val checkMillis = (millis / checksOverWholeTimeFrame.toDouble()).roundToLong()
				for (i in 0 until cores) ths[i]!!.start()
				var curAdd = checkMillis
				val st = System.currentTimeMillis()
				var time: Long
				while (System.currentTimeMillis()
						.also { time = it } < st + millis
				) if ((time >= st + curAdd) and (idx < ds.size)) {
					var count = 0.0
					for (i in counts) count += i.toDouble()
					count /= cores.toDouble()
					ds[idx++] = count
					curAdd += checkMillis
				}
				if (idx < ds.size) {
					println(counts[0])
					var count = 0.0
					for (i in counts) count += i.toDouble()
					count /= cores.toDouble()
					ds[idx] = count
				}
			} catch (e: Throwable) {
				e.printStackTrace()
			}
			for (i in ds.indices.reversed()) if (i != 0) ds[i] -= ds[i - 1]
			return ds
		}

		@Throws(InterruptedException::class)
		fun runSelfTest(
			r: Runnable, timeOutMillis: Long, cycles: Long, millis: Long, cores: Int, wait: Long,
			sb: PrintStream
		): DoubleArray {
			val data = DoubleArray(4)
			val counts: MutableList<Long> = ArrayList(cores)
			val r2 = Runnable {
				val st = System.currentTimeMillis()
				var i: Long = 0
				while (System.currentTimeMillis() < st + millis) {
					r.run()
					i++
				}
				synchronized(counts) {
					counts.add(i)
				}
			}
			val ths = arrayOfNulls<Thread>(cores)
			for (i in 0 until cores) ths[i] = Thread(r2).also { it.start() }
			var lastSize = counts.size
			var st1 = System.currentTimeMillis()
			var time: Long = 0
			var index = 0
			while (counts.size < cores && time < timeOutMillis) try {
				if (lastSize != counts.size) {
					lastSize = counts.size
					time = 0
					st1 = System.currentTimeMillis()
				} else time = System.currentTimeMillis() - st1
				ths[index]!!.join(timeOutMillis - time)
				if (!ths[index]!!.isAlive) {
					index++
				}
			} catch (e: InterruptedException) {
				e.printStackTrace()
			}
			sb.print("${millis}ms to get as many Cycles done as possible\n")
			sb.print("counts:$counts\n")
			data[2] = (counts.sum().toDouble() / millis) * 100.0
			data[0] = (counts.average() / millis) * 100.0
			try {
				Thread.sleep(wait)
			} catch (e: Exception) {
				e.printStackTrace()
			}
			val times: MutableList<Long> = ArrayList(cores)
			val r3 = Runnable {
				val st2 = System.currentTimeMillis()
				for (i in 0 until cycles) r.run()
				val et = System.currentTimeMillis()
				times.add(et - st2)
			}
			for (i in 0 until cores) Thread(r3).start()
			lastSize = times.size
			st1 = System.currentTimeMillis()
			time = 0
			while (times.size < cores && time < timeOutMillis) {
				if (lastSize != times.size) {
					lastSize = times.size
					time = 0
					st1 = System.currentTimeMillis()
				} else time = System.currentTimeMillis() - st1
				try {
					Thread.sleep(1)
				} catch (e: InterruptedException) {
					e.printStackTrace()
				}
			}
			sb.print("$cycles Cycles to get done as fast as possible\n")
			sb.print("times:$times\n")
			if (times.isNotEmpty()) {
				data[3] = times.sumOf { cycles / it.toDouble() * 100.0 }
				data[1] = (cycles / times.average()) * 100.0
			}
			return data
		}
	}
}
