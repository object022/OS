package nachos.threads;

import java.util.PriorityQueue;

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
	public class AlarmItem implements Comparable<AlarmItem> {

		public KThread thread;
		public long time;
		public AlarmItem(KThread thread, long time) {
			this.thread = thread;
			this.time = time;
		}
		@Override
		public int compareTo(AlarmItem o) {
			if (this.time < o.time) return 1;
			if (this.time > o.time) return -1;
			return 0;
		}
	}
	PriorityQueue<AlarmItem> alarmList = new PriorityQueue<AlarmItem> ();
	
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
    	
    boolean intStatus = Machine.interrupt().disable();
    while (!alarmList.isEmpty()) {
    	AlarmItem toWake = alarmList.peek();
    	if (toWake.time > Machine.timer().getTime()) break;
    	toWake.thread.ready();
    	alarmList.remove();
    }
    Machine.interrupt().restore(intStatus);
	KThread.yield();
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
        long wakeTime = Machine.timer().getTime() + x;
        boolean intStatus = Machine.interrupt().disable();
        alarmList.add(new AlarmItem(KThread.currentThread(), wakeTime));
        KThread.sleep();
        Machine.interrupt().restore(intStatus);
	}
}
