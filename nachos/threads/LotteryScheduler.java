package nachos.threads;

import nachos.machine.*;
import nachos.threads.PriorityScheduler.PriorityQueue;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends Scheduler {
	
    /**
     * Allocate a new lottery scheduler.
     */
    public LotteryScheduler() {
    }
    
    /**
     * Allocate a new lottery thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer tickets from waiting threads
     *					to the owning thread.
     * @return	a new lottery thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new LotteryQueue(transferPriority);
    }
    
    public int getPriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());
		       
        return getThreadState(thread).getPriority();
    }

    public int getEffectivePriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());
		       
        return getThreadState(thread).getEffectivePriority();
    }

    public void setPriority(KThread thread, int priority) {
        Lib.assertTrue(Machine.interrupt().disabled());
		       
        Lib.assertTrue(priority >= ticketMinimum &&
		   priority <= ticketMaximum);
	
        getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
        boolean intStatus = Machine.interrupt().disable();
		       
        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == ticketMaximum)
            return false;

        setPriority(thread, priority+1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    public boolean decreasePriority() {
        boolean intStatus = Machine.interrupt().disable();
		       
        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == ticketMinimum)
            return false;

        setPriority(thread, priority-1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int ticketDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int ticketMinimum = 1;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int ticketMaximum = Integer.MAX_VALUE;    

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
        if (thread.schedulingState == null)
            thread.schedulingState = new ThreadState(thread);
        return (ThreadState) thread.schedulingState;
    }
    
    public void selftest (){
    	LotteryQueue queue = new LotteryQueue (true);
    	LinkedList<KThread> tlist = new LinkedList<KThread> ();
    	
    	for (int i = 0; i < 20; ++ i){
    		KThread t = new KThread ( new Runnable () {
    			public void run (){}
    		}).setName("test #" + i);
    		tlist.add(t);
    		getThreadState(t).setPriority(i + 1);
    		if (i < 10){
    			boolean b = Machine.interrupt().disable();
    			queue.waitForAccess(t);
    			Machine.interrupt().restore(b);
    		}
    		Lib.assertTrue(((ThreadState)(t.schedulingState)).getEffectivePriority() == i + 1);
    	}
    	for (int i = 0; i < 10; ++ i){
    		getThreadState(tlist.get(i)).addPrev(
    				getThreadState(tlist.get(i + 10)));
    	}
    	
    	
    	for (int i = 0; i < 10; ++ i){
    		boolean b = Machine.interrupt().disable();
    		KThread t = queue.nextThread();
    		if (t == null){
    			System.out.println(i);
    			Lib.assertTrue(false);
    		}
    		Machine.interrupt().restore(b);
    		System.out.println(t.toString() + ":" + ((ThreadState)t.schedulingState).getEffectivePriority());
    	}
    }

    
    
    
    protected class LotteryQueue extends ThreadQueue {
    	
        ThreadState node;
    
        LotteryQueue(boolean transferPriority) {
            this.transferPriority = transferPriority;
            node = new ThreadState(this);
        }

        public void waitForAccess(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            if (thread == null)
                return;
            getThreadState(thread).waitForAccess(this);
            ++ totalNodes;
        }

        public void acquire(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            getThreadState(thread).acquire(this);
        }

        public KThread nextThread() {
            Lib.assertTrue(Machine.interrupt().disabled());
            //Remove any thread that is holding this. (?????)
            while (!node.ne.isEmpty()) {
                ThreadState curr = node.ne.getFirst();
                curr.delPrev(node);
            }
            if (node.pr.isEmpty())
                return null;
            ThreadState pick = pickNextThread();
            pick.acquire(this);
            return pick.thread;
        }

	/**
	 * Return the next thread that <tt>nextThread()</tt> would return,
	 * without modifying the state of this queue.
	 *
	 * @return	the next thread that <tt>nextThread()</tt> would
	 *		return.
	 */
        protected ThreadState pickNextThread() {
        	return node.lotteryChoose();
        }
	
        public void print() {
            Lib.assertTrue(Machine.interrupt().disabled());
	    // implement me (if you want)
	    // Waiting for debug purposes
        }

	/**
	 * <tt>true</tt> if this queue should transfer priority from waiting
	 * threads to the owning thread.
	 */
        public boolean transferPriority;
    }
    protected static int totalNodes = 0;
    
    // the only change from PriorityQueue is to change the content of currentMax(), 
    protected class ThreadState implements Comparable<ThreadState>{
    	/**
    	 * Allocate a new <tt>ThreadState</tt> object and associate it with the
    	 * specified thread.
    	 *
    	 * @param	thread	the thread this state belongs to.
    	 */
        	public boolean donateTicket;
        	
            public ThreadState(KThread thread) {
                this.thread = thread;
                donateTicket = true;
                setPriority(ticketDefault);
                nodeId = totalNodes++;
            }
            
            public ThreadState(LotteryQueue queue) {
                this.queue = queue;
                setPriority(ticketMinimum - 1);
                donateTicket = queue.transferPriority;
            }
            
            public int currentMax() {
                int ret = ticket;
                if (pr.isEmpty())
                    return ret;
                ThreadState cur;
                for (Iterator<ThreadState> iter = pr.iterator();iter.hasNext();){
                	cur = iter.next();
                	//System.out.println("	cur: " + cur.toString());
                	//System.out.println("	ret: " + ret + "+" + cur.current);
                    ret += cur.current;
                }
                return ret;
            }
            
            public ThreadState lotteryChoose () {
            	if (pr.isEmpty()) 
            		return null;
            	int totalticket = 0;
            	ThreadState cur;
                for (Iterator<ThreadState> iter = pr.iterator();iter.hasNext();){
                	cur = iter.next();
                    totalticket += cur.current;
                }
                
                int rand = (int) ( Math.random() * totalticket );
                
                totalticket = 0;
                for (Iterator<ThreadState> iter = pr.iterator();iter.hasNext();){
                	cur = iter.next();
                    totalticket += cur.current;
                    if (totalticket >= rand)
                    	return cur;
                }
                
                Lib.assertTrue(false);
                return null;
            }
            
            @Override
            public int compareTo(ThreadState o) {
                if (current != o.current)
                    return current - o.current;
                return nodeId - o.nodeId;
            }
            
            public void update(ThreadState prev) {
                if (!donateTicket)
                    return;
                int newMax = currentMax();
                if (newMax != current) {
                    current = newMax;
                	for (Iterator<ThreadState> iter = ne.iterator();iter.hasNext();) {
                        ThreadState cur = iter.next();
                         cur.update(this);
                    }
                }
            }
    	
            public void update_local() {
                if (!donateTicket)
                    return;
                int newMax = currentMax();
                if (newMax != current) {
                    current = newMax;
                    for (Iterator<ThreadState> iter = ne.iterator();iter.hasNext();) {
                        ThreadState cur = iter.next();
                        cur.update(this);
                    }
                }
            }
    	
            public void addPrev(ThreadState node) {
                pr.add(node);
                node.ne.add(this);
                update_local();
            }
            
            public void delPrev(ThreadState node) {
                pr.remove(node);
                node.ne.remove(this);
                update_local();
            }
            
            @Override
            public String toString() {
            	return "[" + nodeId + " c:" + current + " i:" + ticket + "]"  + thread + " " + queue;
            }
    	
    	/**
    	 * Return the priority of the associated thread.
    	 *
    	 * @return	the priority of the associated thread.
    	 */
            public int getPriority() {
                return ticket;
            }

    	/**
    	 * Return the effective priority of the associated thread.
    	 *
    	 * @return	the effective priority of the associated thread.
    	 */
            public int getEffectivePriority() {
                return current;
            }

    	/**
    	 * Set the priority of the associated thread to the specified value.
    	 *
    	 * @param	priority	the new priority.
    	 */
            public void setPriority(int ticket) {
                if (this.ticket == ticket)
                    return;
    	    
                this.ticket = ticket;
                update_local();
            }

    	/**
    	 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
    	 * the associated thread) is invoked on the specified priority queue.
    	 * The associated thread is therefore waiting for access to the
    	 * resource guarded by <tt>waitQueue</tt>. This method is only called
    	 * if the associated thread cannot immediately obtain access.
    	 *
    	 * @param	waitQueue	the queue that the associated thread is
    	 *				now waiting on.
    	 *
    	 * @see	nachos.threads.ThreadQueue#waitForAccess
    	 */
            public void waitForAccess(LotteryQueue waitQueue) {
                ThreadState tar = waitQueue.node;
                tar.addPrev(this);
            }

    	/**
    	 * Called when the associated thread has acquired access to whatever is
    	 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
    	 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
    	 * <tt>thread</tt> is the associated thread), or as a result of
    	 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
    	 *
    	 * @see	nachos.threads.ThreadQueue#acquire
    	 * @see	nachos.threads.ThreadQueue#nextThread
    	 */
            public void acquire(LotteryQueue waitQueue) {
                ThreadState tar = waitQueue.node;
                tar.delPrev(this);
                Lib.assertTrue(tar.ne.isEmpty());
                this.addPrev(tar);
          
            }

            public int nodeId, current;
            protected LinkedList<ThreadState> pr = new LinkedList<ThreadState> ();
            protected LinkedList<ThreadState> ne = new LinkedList<ThreadState> ();
    	/** The thread with which this object is associated. */	   
            protected KThread thread;
    	/** The queue associated with this node. */
            protected LotteryQueue queue;
    	/** The priority of the associated thread. */
            protected int ticket;
        }
}
