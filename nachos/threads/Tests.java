package nachos.threads;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import nachos.machine.Lib;
import nachos.threads.KThread;
public class Tests {
	public List<Integer> msg = Collections.synchronizedList(new LinkedList<Integer> ());
	public boolean testJoin() {
		final int n = 0;
		List<KThread> tlist = new LinkedList<KThread> ();
		for (int i = 0; i < n; i++) {
			final int thisId = i;
			tlist.add(new KThread(new Runnable() {
				@Override
				public void run() {
					synchronized(msg) {
					msg.add(thisId + 1);
					}
				}
			}));
		}
		for (int i = 0; i < n; i++) {
			final int thisId = i;
			tlist.add(new KThread(new Runnable() {
				@Override
				public void run() {
					KThread toJoin = tlist.get(thisId);
					Lib.assertTrue(toJoin != null);
					toJoin.join();
					synchronized(msg) {
					msg.add(-thisId - 1);}
				}
			}));
		}
		
		Collections.shuffle(tlist);
		for (int i = 0; i < 2 * n; i++) 
			tlist.get(i).fork();
		for (int i = n; i < 2 * n; i++)
			tlist.get(i).join();
		synchronized(msg) {
			if (msg.size() != 2 * n) return false;
			for (int i = 0; i < 2 * n; i++)
				System.out.print(msg.get(i).toString() + " ");
			boolean[] used = new boolean[n + 1];
			for (int i = 0; i < 2 * n; i++) {
				int num = msg.get(i);
				if (num > 0)
					if (used[num]) return false; else used[num] = true;
				if (num < 0)
					if (!used[-num]) return false; else used[-num] = false;
			}
		}
		return true;
	}
}
