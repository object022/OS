package nachos.threads;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import nachos.machine.Lib;
import nachos.threads.KThread;
/**
 * A tester class for various tasks of this project.
 */
public class Tests {
	/**
	 * This message list is used among all test cases.
	 * It's like the 
	 */
	public List<Integer> msg = Collections.synchronizedList(new LinkedList<Integer> ());
	/**
	 * Tester for KThread.join().
	 * Generates 2*n threads, n of them joining another n threads
	 * Shuffle all threads and fork them, check the resulting message queue
	 * @return A string indicating the result of the test
	 */
	public String testJoin(final int n) {
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
			}).setName("Starter #" + Integer.toString(thisId)));
		}
		for (int i = 0; i < n; i++) {
			final int thisId = i;
			KThread toJoin = tlist.get(thisId);
			tlist.add(new KThread(new Runnable() {
				@Override
				public void run() {
					toJoin.join();
					synchronized(msg) {
					msg.add(-thisId - 1);}
				}
			}).setName("Joiner #" + Integer.toString(thisId)));
		}
		
		Collections.shuffle(tlist);
		for (int i = 0; i < 2 * n; i++) 
			tlist.get(i).fork();
		for (int i = 0; i < 2 * n; i++)
			tlist.get(i).join();
		synchronized(msg) {
			int len = msg.size();
			boolean[] used = new boolean[n + 1];
			for (int i = 0; i < len; i++) {
				int num = msg.get(i);
				if (num > 0)
					if (used[num]) return "Duplicate Entry"; else used[num] = true;
				if (num < 0)
					if (!used[-num]) return "Join before thread stops"; else used[-num] = false;
			}
		}
		return "KThread.Join Succeed N = " + Integer.toString(n);
	}
}
