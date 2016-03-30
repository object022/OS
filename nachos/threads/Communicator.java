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
    private int waitingS = 0, waitingL = 0;
    private LinkedList<Integer> messages = new LinkedList<Integer> ();
    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
    	lock.acquire();
    	messages.add(word);
    	while (waitingL == 0) {
    		waitingS++;
    		condSpeak.sleep();
    		waitingS--;
    	}
    	condListen.wake();
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
    	while (waitingS == 0) {
    		waitingL++;
    		condListen.sleep();
    		waitingL--;
    	}
    	condSpeak.wake();
    	int ret = messages.removeFirst();
    	lock.release();
    	return ret;
    }
}
