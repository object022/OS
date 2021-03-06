package nachos.threads;

import nachos.machine.*;

import java.util.TreeSet;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }
    
    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
        return new PriorityQueue(transferPriority);
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
		       
        Lib.assertTrue(priority >= priorityMinimum &&
		   priority <= priorityMaximum);
	
        getThreadState(thread).setPriority(priority);
    }

    public boolean increasePriority() {
        boolean intStatus = Machine.interrupt().disable();
		       
        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == priorityMaximum)
            return false;

        setPriority(thread, priority+1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    public boolean decreasePriority() {
        boolean intStatus = Machine.interrupt().disable();
		       
        KThread thread = KThread.currentThread();

        int priority = getPriority(thread);
        if (priority == priorityMinimum)
            return false;

        setPriority(thread, priority-1);

        Machine.interrupt().restore(intStatus);
        return true;
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;    

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
    	PriorityQueue queue = new PriorityQueue (true);
    	LinkedList<KThread> tlist = new LinkedList<KThread> ();
    	
    	for (int i = 0; i < 20; ++ i){
    		KThread t = new KThread ( new Runnable () {
    			public void run (){}
    		}).setName("test #" + i);
    		tlist.add(t);
    		if (i < 10){
    			boolean b = Machine.interrupt().disable();
    			queue.waitForAccess(t);
    			Machine.interrupt().restore(b);
    		}
    		getThreadState(t).setPriority(i % priorityMaximum);
    	}
    	for (int i = 0; i < 5; ++ i){
    		getThreadState(tlist.get(i + 10)).addPrev(
    				getThreadState(tlist.get(i)));
    	}
    	
    	
    	for (int i = 0; i < 10; ++ i){
    		boolean b = Machine.interrupt().disable();
    		KThread t = queue.nextThread();
    		Machine.interrupt().restore(b);
    		System.out.println(t.toString());
    	}
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
    	
        ThreadState node;
    
        PriorityQueue(boolean transferPriority) {
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
            /*if (this.transferPriority)
                return node.maxPrev();
            else {
            	ThreadState ret = node.pr.getFirst();
                for (Iterator<ThreadState> iter = node.pr.iterator();iter.hasNext();) {
                    ThreadState cur = iter.next();
                    if (cur.priority > ret.priority) ret = cur;
                }
                return ret;
            }*/
        	return node.maxPrev();
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
    
    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class ThreadState implements Comparable<ThreadState>{
	/**
	 * Allocate a new <tt>ThreadState</tt> object and associate it with the
	 * specified thread.
	 *
	 * @param	thread	the thread this state belongs to.
	 */
    	public boolean donatePriority;
        public ThreadState(KThread thread) {
            this.thread = thread;
            donatePriority = true;
            setPriority(priorityDefault);
            nodeId = totalNodes++;
        }
        public ThreadState(PriorityQueue queue) {
            this.queue = queue;
            setPriority(priorityMinimum - 1);
            donatePriority = queue.transferPriority;
        }

        public int currentMax() {
            int ret = priority;
            if (!pr.isEmpty()) ret = Math.max(ret, maxPrev().current);
                return ret;
        }
        
        public ThreadState maxPrev() {
        	if (pr.isEmpty()) return null;
            ThreadState ret = pr.getFirst();
            for (Iterator<ThreadState> iter = pr.iterator();iter.hasNext();) {
                ThreadState cur = iter.next();
                if (cur.current > ret.current)
                    ret = cur;
                else if (cur.current == ret.current){
                	if (ret.ne.contains(cur))
                		ret = cur;
                }
            }
            return ret;
        }
        
        @Override
        public int compareTo(ThreadState o) {
            if (current != o.current)
                return current - o.current;
            return nodeId - o.nodeId;
        }
        
        public void update(ThreadState prev) {
        	if (!donatePriority) return;
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
        	if (!donatePriority) return;
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
        	System.out.println("Adding link:" + node + "->" + this);
            pr.add(node);
            node.ne.add(this);
            update_local();
        }
        
        public void delPrev(ThreadState node) {
        	System.out.println("Removing link:" + node + "->" + this);
            
            pr.remove(node);
            node.ne.remove(this);
            update_local();
        }
        @Override
        public String toString() {
        	return "[" + nodeId + " c:" + current + " i:" + priority + "]"  + thread + " " + queue;
        }
	
	/**
	 * Return the priority of the associated thread.
	 *
	 * @return	the priority of the associated thread.
	 */
        public int getPriority() {
            return priority;
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
        public void setPriority(int priority) {
            if (this.priority == priority)
                return;
	    
            this.priority = priority;
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
        public void waitForAccess(PriorityQueue waitQueue) {
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
        public void acquire(PriorityQueue waitQueue) {
        	
            ThreadState tar = waitQueue.node;
            tar.delPrev(this);
            Lib.assertTrue(tar.ne.isEmpty());
            this.addPrev(tar);
           
        }

        int nodeId, current;
        protected LinkedList<ThreadState> pr = new LinkedList<ThreadState> ();
        protected LinkedList<ThreadState> ne = new LinkedList<ThreadState> ();
	/** The thread with which this object is associated. */	   
        protected KThread thread;
	/** The queue associated with this node. */
        protected PriorityQueue queue;
	/** The priority of the associated thread. */
        protected int priority;
    }
}
