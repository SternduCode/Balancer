package com.sterndu.network.balancer;

import java.util.Arrays;

public class Tester {
	public static void func() {
		int i = 1000000;
		long a = 1, b = 1;
		for (long l = 0; l < i; l++) {
			long t = a + b;
			a = b;
			b = t;
		}
	}

	public static void func2(long m) {
		for (int x = 0; x < 1000; x++) {
			double zy;
			double zx = zy = 0;
			long MAX_ITER = m;
			double ZOOM = 64050.0;
			double cX = (x - 700) / ZOOM + -.73;
			double cY = (500 - 500) / ZOOM + .185;
			long iter = MAX_ITER;
			while (zx * zx + zy * zy < 4 && iter > 0) {
				double tmp = zx * zx - zy * zy + cX;
				zy = 2.0 * zx * zy + cY;
				zx = tmp;
				iter--;
			}
			int r = (int) (16777215 * (iter / (double) MAX_ITER));
		}
	}

	public static void main(String[] args) {
		System.out.println(Runtime.getRuntime().totalMemory());
		System.out.println(Runtime.getRuntime().maxMemory());
		System.out.println(Runtime.getRuntime().freeMemory());
		int cores = Runtime.getRuntime().availableProcessors();
		long val = 1507;
		Runnable r = () -> {
			func2(val);
		};
		double[] ds = Balancer.longDurationTest(r, 50000, cores, 4);
		System.out.println(Arrays.toString(ds));
		Balancer b2 = new Balancer(null, null, null);
		System.out.println("The test is to calculate a sliver of Mandelbrot with a max Iteration count of : " + val);
		System.out.println("The test is running on " + cores + " Threads/Cores simultaneously");
		double[] data = Balancer.runSelfTest(r, 60000l, 10000l, 20000l, cores,
				2000, System.out);
		System.out.println("S means Singlecore Score; M means Multicore Score");
		System.out.println(
				"B means Score is determined by performance in the first test;\nB2 means Score is determined by performance in the second Test");
		System.out.println("s-b:" + data[0]);
		System.out.println("s-b2:" + data[1]);
		System.out.println("m-b:" + data[2]);
		System.out.println("m-b2:" + data[3]);
		//b2.runNetworkTest(new byte[] {5, 8, 2, 9}, new AddressPortTuple("localhost",
		// 25555));
	}

}
