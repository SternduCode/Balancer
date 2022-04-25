package com.sterndu.network.balancer;

import java.io.*;
import java.lang.invoke.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import com.sterndu.network.balancer.ConnectionEstablisher.*;

public class Balancer {
	public static class AddressPortTuple {
		private final String address;
		private final int port;

		public AddressPortTuple(String address, int port) {
			this.address = address;
			this.port = port;
		}

		public String getAddress() { return address; }

		public int getPort() { return port; }

	}

	private final int lat, bandwidth, power;

	public Balancer(Integer lat, Integer bandwidth, Integer power) {
		this.lat = lat != null ? lat : 100;
		this.bandwidth = bandwidth != null ? lat : 100;
		this.power = power != null ? lat : 100;
	}

	public static double[] longDurationTest(Runnable r,long milis,int cores,int checks) {
		double[] ds=new double[checks];
		Thread[] ths=new Thread[cores];
		for (int i=0;i<cores;i++) ths[i]=new Thread() {
			int i=0;
			@Override
			public void run() {
				long st=System.currentTimeMillis(),duratio=milis;
				while (System.currentTimeMillis()<st+duratio) {
					r.run();
					i++;
				}
			}
		};
		try {
			int idx = 0;
			MethodHandle mh=MethodHandles.lookup().findGetter(ths[0].getClass(), "i", int.class);
			long check_milis = Math.round(milis / (double) checks);
			for (int i = 0; i < cores; i++) ths[i].start();
			long cur_add = check_milis, st = System.currentTimeMillis(), time = st;
			while ((time = System.currentTimeMillis()) < st + milis) if (time >= st + cur_add & idx < ds.length) {
				double count=0;
				for (int i=0;i<cores;i++)count+=(int)mh.invoke(ths[i]);
				count/=cores;
				ds[idx++]=count;
				cur_add+=check_milis;
			}
			if (idx < ds.length) {
				System.out.println(mh.invoke(ths[0]));
				double count = 0;
				for (int i = 0; i < cores; i++) count += (int) mh.invoke(ths[i]);
				count /= cores;
				ds[idx++] = count;
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		for (int i = ds.length - 1; i >= 0; i--) if (i!=0)
			ds[i]-=ds[i-1];
		return ds;
	}

	public static double[] runSelfTest(Runnable r, long timeoutmillis, long cycles, long milis, int cores, long wait,
			PrintStream sb) {
		double[] data=new double[4];
		List<Long> counts = new ArrayList<>(cores);
		Runnable r2 = () -> {
			long st = System.currentTimeMillis(), duratio = milis;
			long i = 0;
			while (System.currentTimeMillis() < st + duratio) {
				r.run();
				i++;
			}
			counts.add(i);
		};
		for (int i = 0; i < cores; i++) new Thread(r2).start();
		int lastsize = counts.size();
		long st1 = System.currentTimeMillis();
		long time = 0;
		while (counts.size() < cores && time < timeoutmillis) try {
			if (lastsize != counts.size()) {
				lastsize = counts.size();
				time = 0;
				st1 = System.currentTimeMillis();
			} else time = System.currentTimeMillis() - st1;
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		sb.print(milis + "ms to get as many Cycles done as possible\n");
		sb.print("counts:" + counts + "\n");
		data[2] = (double) counts.parallelStream().mapToLong(l -> l).sum() / milis * 100.0d;
		data[0] = counts.parallelStream().mapToLong(l -> l).average().getAsDouble() / milis * 100.0d;

		try {
			Thread.sleep(wait);
		} catch (Exception e) {
			e.printStackTrace();
		}

		List<Long> times = new ArrayList<>(cores);
		Runnable r3 = () -> {
			long st2 = System.currentTimeMillis();
			for (int i = 0; i < cycles; i++) r.run();
			long et = System.currentTimeMillis();
			times.add(et - st2);
		};
		for (int i = 0; i < cores; i++) new Thread(r3).start();
		lastsize = times.size();
		st1 = System.currentTimeMillis();
		time = 0;
		while (times.size() < cores && time < timeoutmillis) {
			if (lastsize != times.size()) {
				lastsize = times.size();
				time = 0;
				st1 = System.currentTimeMillis();
			} else time = System.currentTimeMillis() - st1;
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		sb.print(cycles + " Cycles to get done as fast as possible\n");
		sb.print("times:" + times + "\n");
		data[3] = times.parallelStream().mapToDouble(l -> cycles / (double) l * 100.0d).sum();
		data[1] = cycles / times.parallelStream().mapToLong(l -> l).average().getAsDouble() * 100.0d;
		return data;
	}

	private void clientSideNetworkTest(byte[] data,Connection c) throws IOException {
		List<Long> latnanosshort = new ArrayList<>();
		List<Long> latnanosbig = new ArrayList<>();
		byte[] retdata;
		InputStream is=c.getInputStream();
		OutputStream os=c.getOutputStream();
		WritableByteChannel out = Channels.newChannel(os);
		ReadableByteChannel in = Channels.newChannel(is);
		os.write(data);
		while (is.available() == 0)
			try {
				Thread.sleep(1);
			} catch (Exception e) {
				e.printStackTrace();
			}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		while (is.available() > 0) {
			byte[] b = new byte[4096];
			int i = is.read(b);
			baos.write(b, 0, i);
		}
		retdata = baos.toByteArray();
		for (int i = 0; i < 20; i++) try {
			long st = System.currentTimeMillis();
			out.write(ByteBuffer.allocate(Long.BYTES).order(ByteOrder.BIG_ENDIAN).putLong(st).flip());
			while (is.available() == 0)
				try {
					Thread.sleep(1);
				} catch (Exception e) {
					e.printStackTrace();
				}
			long sr = System.currentTimeMillis();
			ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
			while (is.available() > 0) {
				byte[] b1 = new byte[4096];
				int i1 = is.read(b1);
				baos1.write(b1, 0, i1);
			}
			byte[] b = baos1.toByteArray();

			ByteBuffer input = ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN);
			System.out.println("CS:size:" + b.length);
			System.out.println(input.remaining());
			for (int j = 0; j < 8; j++) System.out.print(input.getLong() + " ");
			System.out.println();
			long er = System.currentTimeMillis();
			System.out.println(st + "	" + sr + "	" + er);
		}catch (Exception e) {
			System.err.println(e);
			Arrays.asList(e.getStackTrace()).forEach(e4 -> System.err.println("	" + e4));
			i--;
		}
		List<Double> losts = new LinkedList<>();
		for (int i = 0; i < 30; i++) {
			long st = System.currentTimeMillis();
			ByteBuffer bb = ByteBuffer.allocate(Long.BYTES * 32).order(ByteOrder.BIG_ENDIAN);
			new Random().longs(32).sequential().forEach(l -> bb.putLong(l));
			out.write(bb.flip());
			while (is.available() == 0)
				try {
					Thread.sleep(1);
				} catch (Exception e) {
					e.printStackTrace();
				}
			long sr = System.currentTimeMillis();
			ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
			while (is.available() > 0) {
				byte[] b1 = new byte[128];
				int i1 = is.read(b1);
				baos1.write(b1, 0, i1);
			}
			long er = System.currentTimeMillis();
			byte[] b = baos1.toByteArray();
			ByteBuffer bb1 = ByteBuffer.wrap(b);
			System.out.println("CS:size:" + b.length);
			double lost = 32 * 4096 * 8 + 10 > b.length ? (32 * 4096 * 8 + 10 - b.length) / (32 * 4096 * 8 + 10) : 0;
			losts.add(lost);
			System.out.println(st + "	" + sr + "	" + er);
		}
		double max = losts.parallelStream().mapToDouble(d -> d).max().getAsDouble();
		double min = losts.parallelStream().mapToDouble(d -> d).min().getAsDouble();
		double av = losts.parallelStream().mapToDouble(d -> d).average().getAsDouble();
		System.out.println(max + "	" + min + "	" + av);
	}

	private void serverSideNetworkTest(byte[] data,Connection c) throws IOException {
		InputStream is = c.getInputStream();
		OutputStream os = c.getOutputStream();
		ReadableByteChannel in = Channels.newChannel(is);
		WritableByteChannel out = Channels.newChannel(os);
		byte[] retdata;
		while (is.available() == 0)
			try {
				Thread.sleep(1);
			} catch (Exception e) {
				e.printStackTrace();
			}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		while (is.available() > 0) {
			byte[] b = new byte[256];
			int i = is.read(b);
			baos.write(b, 0, i);
		}
		retdata = baos.toByteArray();
		os.write(data);
		for (int i = 0; i < 20; i++) try {
			while (is.available() == 0)
				try {
					Thread.sleep(1);
				} catch (Exception e) {
					e.printStackTrace();
				}
			long sr = System.currentTimeMillis();
			ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
			while (is.available() > 0) {
				byte[] b1 = new byte[4096];
				int i1 = is.read(b1);
				baos1.write(b1, 0, i1);
			}
			System.out.println("SS:size:" + baos1.size());
			ByteBuffer input = ByteBuffer.wrap(baos1.toByteArray()).order(ByteOrder.BIG_ENDIAN);
			ByteBuffer output = ByteBuffer.allocate(Long.BYTES * 8).order(ByteOrder.BIG_ENDIAN);
			output.putLong(input.getLong());
			output.putLong(sr);
			Random r = new Random();
			output.putLong(r.nextLong());
			output.putLong(r.nextLong());
			output.putLong(r.nextLong());
			output.putLong(r.nextLong());
			output.putLong(r.nextLong());
			output.putLong(System.currentTimeMillis());
			out.write(output.flip());
		}catch (Exception e) {
			System.err.println(e);
			Arrays.asList(e.getStackTrace()).forEach(e4->System.err.println(e4));
		}
		for (int i = 0; i < 30; i++) {
			while (is.available() == 0)
				try {
					Thread.sleep(1);
				} catch (Exception e) {
					e.printStackTrace();
				}
			long sr = System.currentTimeMillis();
			ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
			while (is.available() > 0) {
				byte[] b1 = new byte[4096];
				int i1 = is.read(b1);
				baos1.write(b1, 0, i1);
			}
			System.out.println("SS:size:" + baos1.size());
			ByteBuffer input = ByteBuffer.allocate(32 * 8).order(ByteOrder.BIG_ENDIAN);
			input.put(baos1.toByteArray());
			ByteBuffer output = ByteBuffer.allocate(Long.BYTES * 32 * 4096 + 10).order(ByteOrder.BIG_ENDIAN);
			output.putShort((short) baos1.size());
			output.putLong(sr);
			for (int ii = 0; ii < 32; ii++)
				new Random(input.getLong(ii * 8)).longs(4096).sequential().forEach(l -> output.putLong(l));
			out.write(output.flip());
		}
	}

	public void runNetworkTest(byte[] data,AddressPortTuple... apt) {
		AtomicBoolean ab = new AtomicBoolean(false);
		ThreadGroup tg = new ThreadGroup("Conns");
		Thread ss = new Thread(() -> {
			try {
				Host h = ConnectionEstablisher.hostConnection(25555);
				for (int i = 0; i < apt.length & !ab.get(); i++) {
					Connection c = h.acceptConnection();
					new Thread(tg, () -> {
						try {
							serverSideNetworkTest(data, c);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}).start();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		ss.start();
		for (AddressPortTuple ap: apt) try {
			Connection c = ConnectionEstablisher.establishConnection(ap.getAddress(), ap.getPort());
			clientSideNetworkTest(data,c);
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		if (ss.isAlive())
			for (long lt = 10000, st = System.currentTimeMillis(),
			nt = 0; lt > 0; lt -= (nt = System.currentTimeMillis()) - st, st = nt)
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		if (tg.activeCount() > 0) tg.interrupt();
	}
}
