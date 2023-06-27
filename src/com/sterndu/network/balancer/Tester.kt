@file:JvmName("Tester")
package com.sterndu.network.balancer

import java.util.*

object Tester {

	@JvmStatic
	fun func() {
		val i = 1000000
		var a: Long = 1
		var b: Long = 1
		for (l in 0 until i) {
			val t = a + b
			a = b
			b = t
		}
	}

	@JvmStatic
	fun func2(m: Long) {
		for (x in 0..1999) {
			var zy = 0.0
			var zx = zy
			val zoom = 2050.0
			val cX = (x - 750) / zoom + -.73
			val cY = (500 - 499) / zoom + .185
			var iter = m
			while (zx * zx + zy * zy < 4 && iter > 0) {
				val tmp = zx * zx - zy * zy + cX
				zy = 2.0 * zx * zy + cY
				zx = tmp
				iter--
			}
			// val r = (16777215 * (iter / m.toDouble())).toInt();
		}
	}

	@Throws(InterruptedException::class)
	@JvmStatic
	fun main(args: Array<String>) {
		val st = System.currentTimeMillis()
		Tester.func2(15000)
		val et = System.currentTimeMillis()
		println((et - st).toString() + " ms")
	}
}

fun main() {
	println(Runtime.getRuntime().totalMemory())
	println(Runtime.getRuntime().maxMemory())
	println(Runtime.getRuntime().freeMemory())
	val cores = Runtime.getRuntime().availableProcessors()
	val value: Long = 15000
	val r = Runnable { Tester.func2(value) }
	val ds = Balancer.longDurationTest(r, 50000, cores, 4)
	println(ds.contentToString())
	val b2 = Balancer()
	println("The test is to calculate a sliver of Mandelbrot with a max Iteration count of : $value")
	println("The test is running on $cores Threads/Cores simultaneously")
	val data = Balancer.runSelfTest(
		r, 80000L, 200L, 20000L, cores,
		2000, System.out
	)
	println("S means Singlecore Score; M means Multicore Score")
	println(
		"B means Score is determined by performance in the first test;\nB2 means Score is determined by performance in the second Test"
	)
	println("s-b:" + data[0])
	println("s-b2:" + data[1])
	println("m-b:" + data[2])
	println("m-b2:" + data[3])
	//b2.runNetworkTest(new byte[] {5, 8, 2, 9}, new AddressPortTuple("localhost",
	// 25555));
}
