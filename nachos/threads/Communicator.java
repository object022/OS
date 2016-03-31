package nachos.threads;

import java.util.LinkedList;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 * 
 * Implementation Notes:
 * We use a simple one-round shake-hand protocol. Before the protocol starts,
 * speaker push his word onto a queue (this is to deal with the chaos when
 * there are multiple pairs), and after the protocol, listener grab the word from the queue.
 * 
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
    }
    private Lock lock = new Lock();
    private Condition condSpeak = new Condition(lock);
    private Condition condListen = new Condition(lock);
    private Condition condRet = new Condition(lock);
    private int msg = 0;
    int status = s_waitS; 
    int waitingL = 0, waitingS = 0;
    private static final int s_waitS = 0; // Waiting a speaker that has not appeared
    private static final int s_waitSq = 1; // Waiting a speaker that was summoned from the queue
    private static final int s_waitL = 2; // Waiting a listener that has not appeared
    private static final int s_waitLq = 3; // Waiting a listener that was summoned from the queue
    private static final int s_waitR = 4; // Waiting a speaker to confirm return
    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * NOTE: In this implementation, we can't ensure both thread returns at the same time.
     * However, we guarantee that when a speaker returns, a listener have been ready, and vice versa.
     * This pair does not necessarily have the same message, also; We just make sure they paired up.
     * 
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
    	lock.acquire();
    	if (status != s_waitS) {
    		waitingS++;
    		//System.out.println(KThread.currentThread() + " Speaker slept, state = " + status);
    		condSpeak.sleep();
    		//System.out.println(KThread.currentThread() + " Speaker arrives");
    		waitingS--;
    	}
    	else ;//System.out.println(KThread.currentThread() + " Speaker arrives without sleep");
    	Lib.assertTrue(status == s_waitS || status == s_waitSq);
   	
    	msg = word;
    	if (waitingL > 0) {
    		//System.out.println(KThread.currentThread() + " Wake up a listener");
    		condListen.wake();
    		status = s_waitLq;
    	}
    	else
    		status = s_waitL;
    	
    	condRet.sleep();
    	Lib.assertTrue(status == s_waitR);
    	
    	//System.out.println(KThread.currentThread() + " Speaker returns");
    	
    	if (waitingS > 0) {
        	//System.out.println(KThread.currentThread() + " Wake up a speaker");
    		status = s_waitSq;
        	condSpeak.wake();
    	}
    	else status = s_waitS;
    	lock.release();
   }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
    	lock.acquire();
    	if (status!= s_waitL) {
    		waitingL++;
    		//System.out.println(KThread.currentThread() + " Listener slept, state = " + status);
    		condListen.sleep();
    		//System.out.println(KThread.currentThread() + " Listener arrives");
    		waitingL--;
    	}
    	else ;//System.out.println(KThread.currentThread() + " Listener arrives without sleep");
    	
    	
    	
    	Lib.assertTrue(status == s_waitL || status == s_waitLq);
    	
    	//System.out.println(KThread.currentThread() + " Wake up a returner");
    	
    	int ret = msg;
    	condRet.wake();
    	status = s_waitR;
    	lock.release();
    	return ret;
    }
    public static void selfTest() {
    	System.out.println(new Tests().testComm1(20));
    	System.out.println(new Tests().testComm2(20));
    	System.out.println(new Tests().testComm3(20));
    }
}
