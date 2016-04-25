package nachos.userprog;

import java.util.LinkedList;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
    /**
     * Allocate a new user kernel.
     */
    public UserKernel() {
	super();
    }

    /**
     * Initialize this kernel. Creates a synchronized console and sets the
     * processor's exception handler.
     */
    public void initialize(String[] args) {
	super.initialize(args);
	//Assume this calls only once...
	fpLock = new Lock();
	freePages = new LinkedList<Integer> ();
	fpLock.acquire();
	for (int i = 0; i < Machine.processor().getNumPhysPages(); i++)
		freePages.add(i);
	fpLock.release();
	console = new SynchConsole(Machine.console());
	
	Machine.processor().setExceptionHandler(new Runnable() {
		public void run() { exceptionHandler(); }
	    });
    }

    /**
     * Synch'd fetch a free page number.
     * As the # of free page requests are not a lot in each process(8 only), we don't implement bulk request.
     * Also this is to intentionally make fragments in physical page addresses to test robustness.
     */
    public static int requestPP() {
    	fpLock.acquire();
    	int res = -1;
    	if (!freePages.isEmpty()) res = freePages.removeFirst();
    	fpLock.release();
    	return res;
    }
    /**
     * Synch'd release a page number.
     */
    public static void releasePP(int num) {
    	fpLock.acquire();
    	freePages.addFirst(num); // Re-usage for pages with smaller IDs
    	fpLock.release();
    }
    /**
     * Test the console device.
     */	
    public void selfTest() {
	super.selfTest();

	/*System.out.println("Testing the console device. Typed characters");
	System.out.println("will be echoed until q is typed.");

	char c;

	do {
	    c = (char) console.readByte(true);
	    console.writeByte(c);
	}
	while (c != 'q');

	System.out.println("");
	*/
	Tests.VMTest();
    }

    /**
     * Returns the current process.
     *
     * @return	the current process, or <tt>null</tt> if no process is current.
     */
    public static UserProcess currentProcess() {
	if (!(KThread.currentThread() instanceof UThread))
	    return null;
	
	return ((UThread) KThread.currentThread()).process;
    }

    /**
     * The exception handler. This handler is called by the processor whenever
     * a user instruction causes a processor exception.
     *
     * <p>
     * When the exception handler is invoked, interrupts are enabled, and the
     * processor's cause register contains an integer identifying the cause of
     * the exception (see the <tt>exceptionZZZ</tt> constants in the
     * <tt>Processor</tt> class). If the exception involves a bad virtual
     * address (e.g. page fault, TLB miss, read-only, bus error, or address
     * error), the processor's BadVAddr register identifies the virtual address
     * that caused the exception.
     */
    public void exceptionHandler() {
	Lib.assertTrue(KThread.currentThread() instanceof UThread);

	UserProcess process = ((UThread) KThread.currentThread()).process;
	int cause = Machine.processor().readRegister(Processor.regCause);
	process.handleException(cause);
    }

    /**
     * Start running user programs, by creating a process and running a shell
     * program in it. The name of the shell program it must run is returned by
     * <tt>Machine.getShellProgramName()</tt>.
     *
     * @see	nachos.machine.Machine#getShellProgramName
     */
    public void run() {
	super.run();

	UserProcess process = UserProcess.newUserProcess();
	
	String shellProgram = Machine.getShellProgramName();	
	Lib.assertTrue(process.execute(shellProgram, new String[] { }));

	KThread.finish();
    }

    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
	super.terminate();
    }
    
    /** Globally accessible reference to the synchronized console. */
    public static SynchConsole console;

    private static LinkedList<Integer> freePages = null;
    private static Lock fpLock = null;
    // dummy variables to make javac smarter
    private static Coff dummy1 = null;
}
