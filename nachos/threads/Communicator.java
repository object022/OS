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
    private Condition2 condSpeak = new Condition2(lock);
    private Condition2 condListen = new Condition2(lock);
    private Condition2 condSpeak2 = new Condition2(lock);
    private Condition2 condListen2 = new Condition2(lock);
    private int waitingS = 0, waitingL = 0, waitingS2 = 0, waitingL2 = 0;
    private LinkedList<Integer> messages = new LinkedList<Integer> ();
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
    	messages.add(word);
    	waitingS++;
    	while (waitingL == 0) 
    		condSpeak.sleep();
    	condListen.wake();
    	waitingL--;
    	lock.release();
    	
    	
    	//Second synch
    	lock.acquire();
    	waitingS2++;
    	while (waitingL2 == 0) {
    		
    		condSpeak2.sleep();
    		
    	}
    	waitingL2--;
    	condListen2.wake();
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
    	waitingL++;
    	while (waitingS == 0) condListen.sleep();
    	condSpeak.wake();
    	Lib.assertTrue(!messages.isEmpty());
    	int ret = messages.removeFirst();
    	waitingS--;
    	lock.release();
    	
    	lock.acquire();
    	waitingL2++;
    	while (waitingS2 == 0) condListen2.sleep();
    	waitingS2--;
    	condSpeak2.wake();
    	lock.release();
    	
    	return ret;
    }
    public static void selfTest() {
    	System.out.println(new Tests().testComm1(20));
    	System.out.println(new Tests().testComm2(20));
    	System.out.println(new Tests().testComm3(20));
    }
}
