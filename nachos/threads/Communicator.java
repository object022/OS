package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
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
    private Condition condSpeak2 = new Condition(lock);
    private Condition condListen2 = new Condition(lock);
    private int waitingS = 0, waitingL = 0, waitingS2 = 0, waitingL2 = 0, lastWord = 0;
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
    	while (waitingL == 0) {
    		waitingS++;
    		condSpeak.sleep();
    		waitingS--;
    	}
    	condListen.wake();
    	lastWord = word;
    	while (waitingL2 == 0) {
    		waitingS2++;
    		condSpeak2.sleep();
    		waitingS2--;
    	}
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
    	while (waitingS == 0) {
    		waitingL++;
    		condListen.sleep();
    		waitingL--;
    	}
    	condSpeak.wake();
    	while (waitingS2 == 0) {
    		waitingL2++;
    		condListen2.sleep();
    		waitingL2--;
    	}
    	condSpeak2.wake();
    	int ret = lastWord;
    	lastWord = 0;
    	lock.release();
    	return ret;
    }
}
