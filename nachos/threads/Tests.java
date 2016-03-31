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
	 * Expected concurrency - Use SynchList primitive for this
	 */
	public SynchList msg = new SynchList();
	/**
	 * Tester for KThread.join().
	 * Generates 2*n threads, n of them joining another n threads
	 * Shuffle all threads and fork them, check the resulting message queue
	 * @return A string indicating the result of the test
	 */
	public String testJoin(final int n) {
		List<KThread> tlist = new LinkedList<KThread> ();
		List<KThread> joinList = new LinkedList<KThread> ();
		for (int i = 0; i < n; i++) {
			final int thisId = i;
			tlist.add(new KThread(new Runnable() {
				@Override
				public void run() {
					msg.add(thisId + 1);
				}
			}).setName("Starter #" + Integer.toString(thisId)));
		}
		for (int i = 0; i < n; i++) {
			final int thisId = i;
			final KThread toJoin = tlist.get(thisId);
			KThread curThread = new KThread(new Runnable() {
				@Override
				public void run() {
					toJoin.join();
					msg.add(-thisId - 1);}
			}).setName("Joiner #" + Integer.toString(thisId));
			tlist.add(curThread);
			joinList.add(curThread);
		}
		
		Collections.shuffle(tlist);
		for (int i = 0; i < 2 * n; i++) {
			Lib.assertTrue(tlist.get(i).getTCB() != null);
			tlist.get(i).fork();
		}
		for (int i = 0; i < n; i++)
			joinList.get(i).join();
			
		boolean[] used = new boolean[n + 1];
		int num = 0;
		for (int i = 0; i < 2 * n; i++) {
				Object ret = msg.removeFirstNoWait();
				Lib.assertTrue(ret != null);
				num = (Integer) ret;
				if (num > 0)
					if (used[num]) return "Duplicate Entry"; else used[num] = true;
				if (num < 0)
					if (!used[-num]) return "Join before thread stops"; else used[-num] = false;
			
		}
		return "KThread.Join Succeed N = " + Integer.toString(n);
	}
	/**
	 * Testing the wait() and notify() for Condition2, Part 1.
	 * In this part we generate a lot of calls to Sleep() and Wake(), to one conditional variable.
	 * @param size of the test. Same numbers of sleeps and wakes will be generated.
	 * @return The string indicating the result.
	 */
	
	public String testCond1(int n) {
		Lock lock = new Lock();
		Condition2 cond = new Condition2(lock);
		LinkedList<KThread> tlist = new LinkedList<KThread> ();
		LinkedList<KThread> joinList = new LinkedList<KThread> ();
		for (int i = 0; i < n; i++) {
			final int thisId = i+1;
			KThread t = new KThread(new Runnable() {
				@Override
				public void run() {
					lock.acquire();
					cond.wake();
					msg.add(1);
					lock.release();
				}
			}).setName("(cond1) sleeper #" + Integer.toString(thisId));
			tlist.add(t);
			joinList.add(t);
			tlist.add(new KThread(new Runnable() {
				@Override
				public void run() {
					lock.acquire();
					cond.sleep();
					msg.add(-1);
					lock.release();
				}
			}).setName("(cond1) waker #" + Integer.toString(thisId)));
		}
		Collections.shuffle(tlist);
		for (int i = 0; i < 2 * n; i++) {
			Lib.assertTrue(tlist.get(i).getTCB() != null);
			tlist.get(i).fork();
		}
		for (int i = 0; i < n; i++)
			joinList.get(i).join();
		int prefix = 0;
			while (true) {
				Object o = msg.removeFirstNoWait();
				if (o == null) break;
				prefix += (Integer) o;
				if (prefix < 0) return "Too many threads awoken from wake calls";
			}
		
		return "Condition Variable Test 1 Succeed N = " + Integer.toString(n)
			+ " # of threads not woke up = " + prefix;
	}
	/**
	 * Testing the wait() and notifyAll() for Condition2.
	 * The original second part to test notify() is now moved to Communicator.
	 * N threads are forked sharing a common variable. They wait each other until the 
	 * last one arrives, then all are awoken up to finish the process.
	 **/
	Integer counter = 0; // For testCond2 only
	public String testCond2(int n) {
		LinkedList<Lock> locks = new LinkedList<Lock> ();
		LinkedList<Condition2> conds = new LinkedList<Condition2> ();
		LinkedList<KThread> threads = new LinkedList<KThread> ();
		Lock count_lock = new Lock();
		Condition2 count_cond = new Condition2(count_lock);
		for (int i = 0; i < n; i++) {
			locks.add(new Lock());
			conds.add(new Condition2(locks.get(i)));
			final int thisId = i;
			threads.add(new KThread(new Runnable() {
				@Override
				public void run() {
						count_lock.acquire();
						msg.add(counter);
						counter++;
						if (counter == n) count_cond.wakeAll();
						else count_cond.sleep();
						msg.add(counter);
						counter--;
						count_lock.release();
				}
			}).setName("{cond2) Thread #" + Integer.toString(i)));
		}
		for (int i = 0; i < n; i++) {
			Lib.assertTrue(threads.get(i).getTCB() != null);
			threads.get(i).fork();
		}
		for (int i = 0; i < n; i++) threads.get(i).join();
		for (int i = 0; i < n; i++)
			if ((Integer) msg.removeFirstNoWait() != i) 
				return "Wrong counting order at " + Integer.toString(i);
		for (int i = 0; i < n; i++)
			if ((Integer) msg.removeFirstNoWait() != n - i) 
				return "Wrong joining order at " + Integer.toString(i);

		return "Conditional Variable Test 2 passed, N = " + Integer.toString(n);
	}

	/**
	 * Testing the Alarm class.
	 * We generate a lot of calls to waitUntil() and see if the condition specified in the 
	 * problem statement is fulfilled. Also see if it acts correctly if the thread is woke 
	 * up by another conditional variable before time up.
	 * Requires using the autograder event handler to test the optimality constraint.
	 */
	public String testAlarm() {
		return null;
	}
	/**
	 * Testing the Conditon2 synch as well as the Communicator class.
	 * Generate N speakers and listeners and they should form a queue.
	 */
	public String testComm1(int n) {
		LinkedList<Communicator> comm = new LinkedList<Communicator> ();
		LinkedList<KThread> tlist = new LinkedList<KThread> ();
		for (int i = 0; i < n; i++) comm.add(new Communicator());
		for (int i = 0; i < n; i++) {
			final int thisId = i;
			tlist.add(new KThread(new Runnable() {
				@Override
				public void run() {
					if (thisId != 0) {
						int res = comm.get(thisId - 1).listen();
						//System.out.println(KThread.currentThread() + " Listened " + res);
						if (res != thisId - 1) msg.add(-thisId-1); 
					}
					//System.out.println(KThread.currentThread() +" Speaking " + thisId);
					msg.add(thisId);
					comm.get(thisId).speak(thisId); 
					//System.out.println(KThread.currentThread() +" Speaked " + thisId);
					
				}
			}).setName("(comm1) Person #" + Integer.toString(thisId)));
		}
		Collections.shuffle(tlist);
		for (int i = 0; i < n; i++) tlist.get(i).fork();
		comm.get(n-1).listen();
		for (int i = 0; i < n; i++) tlist.get(i).join();
		for (int i = 0; i < n; i++) {
			Object o = msg.removeFirstNoWait();
			if (o == null) return "Queue is too small";
			if ((Integer) o < 0) return "Get incorrect word at " + Integer.toString((Integer) o - 1);
			if ((Integer) o != i) return "Incorrect order at " +  Integer.toString((Integer) o);
		}
		return "Communicator Test 1 passed, N = " + Integer.toString(n);
	}
	/**
	 * Testing the Communicator class.
	 * Known as the Ping-Pong test. This process is also a key component in Boat.
	 */
	public String testComm2(int n) {
		Communicator comm1 = new Communicator(), comm2 = new Communicator();
		KThread ping = new KThread(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < n; i++) {
					if (i % 2 == 0) {
						//System.out.println("speaking " + i);
						comm1.speak(i);
						//System.out.println("spoken " + i);
					} else {
						//System.out.println("listening " + i);
						msg.add((Integer) comm1.listen());
						//System.out.println("listened " + i);
					}
				}
			}
		}).setName("(comm2) ping");
		KThread pong = new KThread(new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < n; i++) {
					if (i % 2 == 1) {
						//System.out.println("speaking " + i);
						comm1.speak(i);
						//System.out.println("spoken " + i);
					} else {
						//System.out.println("listening " + i);
						msg.add((Integer) comm1.listen());
						//System.out.println("listened " + i);
					}
				}
			}
		}).setName("(comm2) pong");
		ping.fork();
		pong.fork();
		ping.join();
		pong.join();
		while (true) {
			Object o = msg.removeFirstNoWait();
			if (o == null) break;
			System.out.println(o);
		}
		return "Communicator Test 2 passed, N = " + Integer.toString(n);
	}
	/**
	 * Testing the Boat class.
	 * There are seemingly not much thing to do, except shuffling the starting orders.
	 * Requires changes to RoundRobinScheduler(current default) to work with this class.
	 * Requires changes to BoatGrader to work with this class.
	 */
	public String testBoat() {
		return null;
	}
}
