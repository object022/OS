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
	 * Expected concurrency, so use synchronized keyword in this file.
	 * As it won't run in the final test, even getting grep'd is fine.
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
		List<KThread> joinList = new LinkedList<KThread> ();
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
			KThread curThread = new KThread(new Runnable() {
				@Override
				public void run() {
					toJoin.join();
					synchronized(msg) {
					msg.add(-thisId - 1);}
				}
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
					synchronized(msg) {
						msg.add(1);
					}
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
					synchronized(msg) {
						msg.add(-1);
					}
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
		synchronized(msg) {
			int len = msg.size();
			for (int i = 0; i < len; i++) {
				prefix += msg.get(i);
				if (prefix < 0) return "Too many threads awoken from wake calls";
			}
		}
		return "Condition Variable Test 1 Succeed N = " + Integer.toString(n)
			+ " # of threads not woke up = " + prefix;
	}
	/**
	 * Testing the wait() and notify() for Condition2, Part 2.
	 * In this part we generate a lot of calls in the following pattern:
	 * Condition[i].sleep()
	 * Condition[i+1].wake()
	 * All processes should wake up in ascending order under proper synch conditions.
	 * This is currently scrapped; Wait until we make sure WaitUntil() works.
	 */
	public String testCond2(int n) {
		//return "Conditonal Variable Test 2 currently disabled";
		//TBD: if the RRS is in Chaos, this auto returns
		//if () return "(cond2) No test under this condition";
		return "Cond2 test currently disabled";
		/*
		LinkedList<Lock> locks = new LinkedList<Lock> ();
		LinkedList<Condition2> conds = new LinkedList<Condition2> ();
		LinkedList<KThread> threads = new LinkedList<KThread> ();
		for (int i = 0; i < n; i++) {
			locks.add(new Lock());
			conds.add(new Condition2(locks.get(i)));
			final int thisId = i;
			threads.add(new KThread(new Runnable() {
				@Override
				public void run() {
					if (thisId != 0) {
						int x = thisId - 1;
						ThreadedKernel.alarm.waitUntil(10000 * x);
						locks.get(x).acquire();
						conds.get(x).sleep();
						locks.get(x).release();
					}
					int y = thisId;
					locks.get(y).acquire();
					conds.get(y).wake();
					synchronized(msg) {
						msg.add(thisId);
					}
					locks.get(y).release();
				}
			}).setName("{cond2) Thread #" + Integer.toString(i)));
		}
		for (int i = 0; i < n; i++) {
			Lib.assertTrue(threads.get(i).getTCB() != null);
			threads.get(i).fork();
		}
		//locks.get(n-1).acquire();
		//conds.get(n-1).sleep();
		//locks.get(n-1).release();
		ThreadedKernel.alarm.waitUntil(100000);
		synchronized(msg) {
			//if (msg.size() != n) return "Wrong message queue size";
			for (int i = 0; i < msg.size(); i++)
				if (msg.get(i) != i) return "Wrong order at " + Integer.toString(i);
			return "Conditional Variable Test 2 passed, Returned Message size = " + Integer.toString(msg.size())
			+ ", N = " + Integer.toString(n);
		}
		*/
	}
	/**
	 * Testing the wait() and notifyAll() for Condition2.
	 * Just a small check as notifyAll() is basically a notify() repeated many times.
	 * We generate a lot of calls to wait() and some call to notifyAll().
	 */
	public String testCond3() {
		return "Conditional Variable Test 3 is not implemented yet.";
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
	 * Testing the Communicator class.
	 * Try generating listens first then speaks; then do it another way.
	 */
	public String testComm() {
		return null;
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
