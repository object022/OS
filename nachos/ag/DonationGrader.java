package nachos.ag;

import nachos.machine.Machine;
import nachos.threads.KThread;
import nachos.threads.Lock;
import nachos.threads.PriorityScheduler;
import nachos.threads.ThreadedKernel;

/**
 * A grader for testing priority donation.
 * 
 * @author Xiangru Chen
 * 
 */
public class DonationGrader extends BasicTestGrader {

	@Override
	public void run() {
		assertTrue(
				ThreadedKernel.scheduler.getClass().getSimpleName()
						.equals("PriorityScheduler"),
				"This grader needs priority scheduler.");

		testLock();

		testJoin();

		done();
	}

	private void testLock() {
		lock = new Lock();
		lock.acquire();

		boolean insStatus = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(lowPriority);

		forkNewThread(new Runnable() {
			@Override
			public void run() {
				lock.acquire();
				lock.release();
				System.out.println("High priority thread completes");
			}
		}, highPriority);

		forkNewThread(new Runnable() {
			@Override
			public void run() {
				boolean intStatus = Machine.interrupt().disable();
				System.out.println("Mid priority thread yields "  
				+ ThreadedKernel.scheduler.getEffectivePriority()
				+ " Intrinstic "+ ThreadedKernel.scheduler.getPriority());
				Machine.interrupt().restore(intStatus);
				
				for (int i = 0; i < 10; ++i) {
					System.out.println("Mid priority thread yields");
					KThread.yield();
				}
				System.out.println("Mid priority thread returns");
				assertTrue(false, "Maybe error in your priority donation.");
			}
		}, midPriority);
		System.out.println("Low priority thread yields " + ThreadedKernel.scheduler.getEffectivePriority());
		Machine.interrupt().restore(insStatus);
		for (int i = 0; i < 10; ++i) {
			System.out.println("Low priority thread yields");
			KThread.yield();
		}
		System.out.println("Low priority thread returns");
		lock.release();
	}

	private void testJoin() {
		boolean insStatus = Machine.interrupt().disable();
		ThreadedKernel.scheduler.setPriority(highPriority);

		ThreadHandler low = forkNewThread(new Runnable() {
			@Override
			public void run() {
				alwaysYield();
			}
		}, lowPriority);

		forkNewThread(new Runnable() {
			@Override
			public void run() {
				alwaysYield();
				assertTrue(false, "Maybe error in your priority donation.");
			}
		}, midPriority);

		Machine.interrupt().restore(insStatus);
		low.thread.join();
	}

	private void alwaysYield() {
		for (int i = 0; i < 10; ++i) {
			KThread.yield();
		}
	}

	private Lock lock = null;

	public static final int highPriority = PriorityScheduler.priorityMaximum;
	public static final int midPriority = PriorityScheduler.priorityMaximum - 1;
	public static final int lowPriority = PriorityScheduler.priorityMaximum - 2;

}
