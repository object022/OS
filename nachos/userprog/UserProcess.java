package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    public UserProcess(boolean _isDebug) {
    isDebug = _isDebug;
    if (!isDebug) {
    counterLock.acquire();
	pid = totalProcess++;
	counterLock.release();
    }
    else {
    	//Allocate 8 pages for testing purposes?
    	numPages = 8;
    	pageTable = new TranslationEntry[8];
    	for (int i=0; i<8; i++){
    		int ppn = UserKernel.requestPP();
    		Lib.assertTrue(ppn != -1);
    	    pageTable[i] = new TranslationEntry(i, ppn, true,false,false,false);
    	}
    }
	status = -1;
    }
    
    public UserProcess() {
    	this(false);
    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
	if (!load(name, args))
	    return false;
	
	
	
	//Allocate descriptors for stdin & stdout
	OpenFile stdin = UserKernel.console.openForReading();
	OpenFile stdout = UserKernel.console.openForWriting();
	FileDesc descin = new FileDesc(stdin);
	FileDesc descout = new FileDesc(stdout);
	Lib.assertTrue(descin.id == 0);
	Lib.assertTrue(descout.id == 1);
	
	//User Process Counter
	if (!isDebug) {
    counterLock.acquire();
	runningProcess++;
	counterLock.release();
	}
	
	thread = new UThread(this);
	thread.setName(name).fork();

	
	return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
	Lib.assertTrue(maxLength >= 0);

	byte[] bytes = new byte[maxLength+1];

	int bytesRead = readVirtualMemory(vaddr, bytes);

	for (int length=0; length<bytesRead; length++) {
	    if (bytes[length] == 0)
		return new String(bytes, 0, length);
	}

	return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    // Copying codes from writeVirtualMemory; If this is modified, modify the other function in the same way.
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
				 int length) {
    	if (vaddr < 0) return 0;
    	Lib.debug(dbgProcess, pid + "  \t\t vaddr = " + po(vaddr) + " len = " + length);
    	int page = Processor.pageFromAddress(vaddr), amount = 0; // first page in the virtual address
    	while (length > 0) {
    		if (page >= numPages) break;
    		TranslationEntry entry = pageTable[page];
    		if (!entry.valid) break;
    		int paddr_s = entry.ppn * pageSize + Processor.offsetFromAddress(vaddr);
    		int psize = Math.min(length, pageSize - Processor.offsetFromAddress(vaddr));
    		Lib.assertTrue(entry.ppn == Processor.pageFromAddress(psize + paddr_s - 1));
    		int res = readPhysicalMemory(paddr_s, data, offset, psize);
    		offset += res;
    		vaddr += res;
    		length -= res;
    		amount += res;
    		if (res < psize) break; // Not enough to read now
    		page++;
    	};
    	return amount;
    }
    /**
     * Read from Physical Memory. Have the same interface as readVirtualMemory.
     * Note: We don't set Entry.dirty NOR Entry.used for no cache have been introduced at this time.
     */
    public int readPhysicalMemory(int paddr, byte[] data, int offset, int length) {
    	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

    	byte[] memory = Machine.processor().getMemory();

    	if (paddr < 0 || paddr >= memory.length)
    	    return 0;

    	int amount = Math.min(length, memory.length-paddr);
    	System.arraycopy(memory, paddr, data, offset, amount);

    	return amount;
    }
    
    
    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
	return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public static String po(int addr) {
    	return Processor.pageFromAddress(addr) + "P+" +Processor.offsetFromAddress(addr);
    }
    // Copying codes from readVirtualMemory; If this is modified, modify the other function in the same way.
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
			  int length) {
    	if (vaddr < 0) return 0;
    	int page = Processor.pageFromAddress(vaddr), amount = 0; // first page in the virtual address
    	if (length > 0)
    	Lib.debug(dbgProcess, pid + "  \t\t vaddr = " + po(vaddr) + " len = " + length);
    	while (length > 0) {
    		if (page >= numPages) break;
    		TranslationEntry entry = pageTable[page];
    		//Lib.debug(dbgProcess, pid + "  \t\t\t Block #" + page + " PPN" + entry.ppn + " RO:" + entry.readOnly);
    		if (!entry.valid || entry.readOnly) break;
    		int paddr_s = entry.ppn * pageSize + Processor.offsetFromAddress(vaddr);
    		int psize = Math.min(length, pageSize - Processor.offsetFromAddress(vaddr));
    		Lib.assertTrue(entry.ppn == Processor.pageFromAddress(psize + paddr_s - 1));
    		//Lib.debug(dbgProcess, pid + "  \t\t\t\t From:" + po(paddr_s) + " len:" + psize);
    		int res = writePhysicalMemory(paddr_s, data, offset, psize);
    		offset += res;
    		vaddr += res;
    		length -= res;
    		amount += res;
    		if (res < psize) break; // Not enough to write now
    		page++;
    		};
    	return amount;
    }
    /**
     * Interface for writing to Physical memory. Same argument and returns with writeVirtualMemory.
     * Note: We don't set Entry.dirty NOR Entry.used for no cache have been introduced at this time.
     */
    public int writePhysicalMemory(int paddr, byte[] data, int offset,
				  int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	
	if (paddr < 0 || paddr >= memory.length)
	    return 0;

	int amount = Math.min(length, memory.length-paddr);
	System.arraycopy(data, offset, memory, paddr, amount);

	return amount;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
	Lib.debug(dbgProcess, pid + "  UserProcess.load(\"" + name + "\")");
	
	
	boolean res = incRef(name);
	if (!res) return false;
	OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
	if (executable == null) {
	    Lib.debug(dbgProcess, pid + "  \topen failed");
	    res = decRef(name);
	    return false;
	}

	try {
	    coff = new Coff(executable);
	}
	catch (EOFException e) {
	    executable.close();
		res = decRef(name);
		if (!res) {
			Lib.debug(dbgProcess, pid + "  \tclosing source file failed");
			return false;
		}
	    Lib.debug(dbgProcess, pid + "  \tcoff load failed");
	    return false;
	}

	res = decRef(name);
	if (!res) {
		Lib.debug(dbgProcess, pid + "  \tclosing source file failed");
		return false;
	}
	
	// make sure the sections are contiguous and start at page 0
	numPages = 0;
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    if (section.getFirstVPN() != numPages) {
		coff.close();
		Lib.debug(dbgProcess, pid + "  \tfragmented executable");
		return false;
	    }
	    numPages += section.getLength();
	}

	// make sure the argv array will fit in one page
	byte[][] argv = new byte[args.length][];
	int argsSize = 0;
	for (int i=0; i<args.length; i++) {
	    argv[i] = args[i].getBytes();
	    // 4 bytes for argv[] pointer; then string plus one for null byte
	    argsSize += 4 + argv[i].length + 1;
	}
	if (argsSize > pageSize) {
	    coff.close();
	    Lib.debug(dbgProcess, pid + "  \targuments too long");
	    return false;
	}

	// program counter initially points at the program entry point
	initialPC = coff.getEntryPoint();	

	
	
	Lib.debug(dbgProcess, pid + 
			"  \tInitial PC = " + Processor.pageFromAddress(initialPC) + "P + " + Processor.offsetFromAddress(initialPC));

	
	// next comes the stack; stack pointer initially points to top of it
	numPages += stackPages;
	initialSP = numPages*pageSize;

	// and finally reserve 1 page for arguments
	numPages++;

	if (!loadSections())
	    return false;

	// store arguments in last page
	int entryOffset = (numPages-1)*pageSize;
	int stringOffset = entryOffset + args.length*4;

	this.argc = args.length;
	this.argv = entryOffset;
	
	for (int i=0; i<argv.length; i++) {
	    byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
	    Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
	    entryOffset += 4;
	    Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
		       argv[i].length);
	    stringOffset += argv[i].length;
	    Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
	    stringOffset += 1;
	}

	return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
	if (numPages > Machine.processor().getNumPhysPages()) {
	    coff.close();
	    Lib.debug(dbgProcess, pid + "  \tinsufficient physical memory");
	    return false;
	}

	// load sections
	// Init pageTables
	pageTable = new TranslationEntry[numPages];
	for (int i=0; i<numPages; i++){
		int ppn = UserKernel.requestPP();
		if (ppn == -1) {
			//for (int j = 0; j < i; j++) {
			//	pageTable[j].valid = false;
			//	UserKernel.releasePP(pageTable[j].ppn);
			//}
			coff.close();
			return false;
		}
	    pageTable[i] = new TranslationEntry(i, ppn, true ,false,false,false);
	}
	
	
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    
	    Lib.debug(dbgProcess, pid + "  \tinitializing " + section.getName()
		      + " section (" + section.getLength() + " pages)");

	    for (int i=0; i<section.getLength(); i++) {
		int vpn = section.getFirstVPN()+i;

		section.loadPage(i, pageTable[vpn].ppn);

		if (section.isReadOnly()) pageTable[vpn].readOnly = true;
		Lib.debug(dbgProcess, pid + 
				"  \tLoad Page " + i + "-> VPN" + vpn + " PPN" + pageTable[vpn].ppn + " RO:" + pageTable[vpn].readOnly);

	
	    }
	}
	
	coff.close();
	return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
    	for (int i = 0; i < numPages; i++) 
    		if (pageTable[i].valid){
    			pageTable[i].valid = false;
    			UserKernel.releasePP(pageTable[i].ppn);
    		}
    }
    protected boolean closeFiles() {
    	boolean res = true;
    	for (FileDesc fd : desc) {
    		if (fd != null)
    		if (!fd.remove()) res = false;
    	}
    	return res;
    }
    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
	Processor processor = Machine.processor();

	// by default, everything's 0
	for (int i=0; i<processor.numUserRegisters; i++)
	    processor.writeRegister(i, 0);

	// initialize PC and SP according
	processor.writeRegister(Processor.regPC, initialPC);
	processor.writeRegister(Processor.regSP, initialSP);

	// initialize the first two argument registers to argc and argv
	processor.writeRegister(Processor.regA0, argc);
	processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {
    Lib.debug(dbgProcess, pid + "  \tEntered halt call with pid = " + pid);
    if (pid == 0)
	Machine.halt();
	return 0;
    }

    /*
     * Manipulates File Descriptor && Global counters for Unlink() operations
     */
    protected Vector<FileDesc> desc = new Vector<FileDesc> ();
    protected static HashMap<String, RefCounter> ref = new HashMap<String, RefCounter> ();
    protected static final int maxDesc = 16;
    protected class RefCounter {
    	public RefCounter() {
    		count = 0;
    		toUnlink = false;
    		lock = new Lock();
    	}
    	
		public int count;
		public boolean toUnlink;
		public Lock lock; // To avoid multiple changes to counter
    }
    // Returns false, if the file is bound to delete.
    public boolean incRef(String name) {
    	RefCounter counter = ref.get(name);
    	if (counter == null) {
    		counter = new RefCounter();
    		ref.put(name, counter);
    	}
    	counter.lock.acquire();
    	if (counter.toUnlink) {
    		counter.lock.release();
    		return false;
    	}
    	counter.count++;
    	counter.lock.release();
    	Lib.debug(dbgProcess, pid + "  \tReference counter: " + name +  " " + counter.count);
    	return true;
    }
    // Returns false if the file should be deleted now, but fails the deletion
    protected boolean decRef(String name) {
    	RefCounter counter = ref.get(name);
    	if (counter == null) {
    		Lib.debug(dbgProcess, pid + "  \tCan't Find reference counter for " + name);
    		return false;
    		//Lib.assertNotReached(); // Should NOT get here?? Now just call error
    	}
    	counter.lock.acquire();
    	
    	counter.count--;
    	if (counter.count == 0) {
    		RefCounter removed = ref.remove(name);
    		Lib.assertTrue(removed != null);
    		if (counter.toUnlink) {
    			boolean res = ThreadedKernel.fileSystem.remove(name);
    			if (!res) Lib.debug(dbgProcess, pid + "  \tCan't Delete the file " + name);
    			else Lib.debug(dbgProcess, pid + "  \tSuccessfully deleted file " + name);
    			return res;
    		}
    	}
    	//There is no need to explicitly deallocate the memory for counter; Garbage collector does it.
    	counter.lock.release();
    	Lib.debug(dbgProcess, pid + "  \tReference counter: " + name +  " " + counter.count);
    	
    	return true;
    }
    
    /*
     * File Descriptors
     */
    private class FileDesc {
    	public FileDesc(OpenFile file) { 
			// failing to construct the descriptor will make id == -1, so returning it yields error
    		this.file = file;
    		this.id = -1;
    		if (file == null) {
    			Lib.debug(dbgProcess, pid + "  \t Failed to create desc because file is null");
    			return;
    		}
    		// Max file quota reached
    		if (desc.size() == maxDesc) {
    			Lib.debug(dbgProcess, pid + "  \t Failed to create desc because max descriptors reached");
    			return;
    		}
    		for (int i = 0; i < desc.size(); i++)
    			if (desc.get(i) == null){ // empty descriptor slot
    				if (!UserProcess.this.incRef(file.getName())) { // try to increase reference
    					Lib.debug(dbgProcess, pid + "  \tFailed to create desc because can't add reference");
    					return;
    				}
    				id = i;
    				desc.set(i, this);
    				Lib.debug(dbgProcess, pid + "  \tReplace desc at " + id);
    				return;
    			}
    		//new descriptor slot
    		if (!UserProcess.this.incRef(file.getName())){ // try to increase reference
    			Lib.debug(dbgProcess, pid + "  \tFailed to create desc because can't add reference");
    			return;
    		}
    		id = desc.size();
    		Lib.debug(dbgProcess, pid + "  \tNew desc at " + id);
    		desc.add(this);
    		
    	}
    	public boolean remove() {
    		Lib.debug(dbgProcess, pid + "  \tRemove desc at " + id);
    		desc.set(id, null);
    		file.close();
    		boolean res = UserProcess.this.decRef(file.getName());
    		this.file = null;
    		this.id = -1;
    		return res;
    	}
    	@Override
    	public String toString() {
    		return "[fd" + id + "] : " + file.getName();
    	}
    	public int id;
    	public OpenFile file;
    }
    private int handleCreate(int a0) {
    	String filename = readVirtualMemoryString(a0, 256);
    	if (filename == null) return -1;
    	Lib.debug(dbgProcess, pid + "  \tCreating File " + filename);
    	OpenFile file = ThreadedKernel.fileSystem.open(filename, true);
    	if (file != null) Lib.debug(dbgProcess, pid + "  \tObtained file pointer");
    	return new FileDesc(file).id;
    }
	private int handleOpen(int a0) {
    	String filename = readVirtualMemoryString(a0, 256);
    	if (filename == null) return -1;
    	Lib.debug(dbgProcess, pid + "  \tOpening File " + filename);
    	OpenFile file = ThreadedKernel.fileSystem.open(filename, false);
    	if (file != null) Lib.debug(dbgProcess, pid + "  \tObtained file pointer");
    	return new FileDesc(file).id;
    }
    private int handleRead(int a0, int a1, int a2) {
    	// Boundary Checks
    	if (a0 >= desc.size()) return -1;
    	if (a0 < 0) return -1;
    	if (desc.get(a0) == null) return -1;
    	if (a2 < 0) return -1;
    	if (a0 >= 2)
    	Lib.debug(dbgProcess, pid + "  Entered read call with fd = " + desc.get(a0) + ", length = " + a2);
    	OpenFile file = desc.get(a0).file;
    	//Lib.debug(dbgProcess, pid + "  File Name = " + file.getName() + ", length = " + file.length());
    	//We need to avoid creating too large buffer here!
    	if (a2 > pageSize * numPages) return -1;
    	byte[] buffer = new byte[a2];
    	int len = file.read(buffer, 0, a2);
    	if (a0 >= 2)
    	Lib.debug(dbgProcess, pid + "  \tByte Read = " + len);
    	if (len == -1) return -1; // Fail to read; Break now.
    	int actuals = writeVirtualMemory(a1, buffer, 0, len);
    	if (a0 >= 2)
    	Lib.debug(dbgProcess, pid + "  \tActual Write = " + actuals);
    	if (actuals < len) return -1; // Fail to write; Report error as required.
    	// Note that as specified in syscall.h, if we don't have sufficient space for writing the results back to memory
    	// it would be identified as error
    	if (a0 >= 2)
    	Lib.debug(dbgProcess, pid + "  Quit read call with bytes read = " + len);
    	return len;
    }
    private int handleWrite(int a0, int a1, int a2) {
    	// Boundary Checks
    	if (a0 >= desc.size()) return -1;
    	if (a0 < 0) return -1;
    	if (desc.get(a0) == null) return -1;
    	if (a2 < 0) return -1;
    	
    	if (a0 >= 2)
        	Lib.debug(dbgProcess, pid + "  Entered write call with fd = " + a0 + ", length = " + a2);
    	OpenFile file = desc.get(a0).file;
    	//We need to avoid creating too large buffer here!
    	if (a2 > pageSize * numPages) return -1;
    	byte[] buffer = new byte[a2];
    	int actuals = readVirtualMemory(a1, buffer);
    	// Note that as specified in syscall.h, if we don't have sufficient bytes read from memory
    	// it would be identified as error
    	if (actuals < a2) return -1;
    	// Currently only a one-round attempt; Can easily change to read-until-blocked.
    	int len = file.write(buffer, 0, a2);
    	// Note that as specified in syscall.h, if we don't manage to write sufficient bytes to the file
    	// it would be identified as error, while in reading it wouldn't.
    	if (a0 >= 2)
    	Lib.debug(dbgProcess, pid + "  Quit write call with bytes written = " + len);
    	//if (len < a2) return -1;
    	//else return a2;
    	return len;
    }
    private int handleClose(int a0) {
    	// Boundary Checks
    	if (a0 >= desc.size()) return -1;
    	if (a0 < 0) return -1;
    	if (desc.get(a0) == null) return -1;
    	if (desc.get(a0).remove()) return 0;
    	return -1;
    }
    //When the file is still referenced, the deletion is deferred until last deference. 
    //In this case the function returns 0 always
    private int handleUnlink(int a0) {
    	String filename = readVirtualMemoryString(a0, 256);
    	if (filename == null) return -1;
    	RefCounter counter = ref.get(filename);
    	if (counter != null) {
    		//No lock is used here; Modification to toUnlink function is one-directional
    		//EDGE CASE???
    		if (counter.toUnlink) return -1; // Multiple unlinking attempts
    		counter.toUnlink = true;
    		Lib.debug(dbgProcess, pid + "  Unlink: suspended deletion of " + filename + ", ref = " + counter.count);
    		return 0; // This file is scheduled to be deleted once deferenced by all
    	} else {
    	boolean res = ThreadedKernel.fileSystem.remove(filename);
    	if (!res) Lib.debug(dbgProcess, pid + "  Unlink: direct removal fails on " + filename);
    	else Lib.debug(dbgProcess, pid + "  Unlink: directly removed " + filename);
    	return res ? 0 : -1;
    	}
    }
    
    /*
     * Keeping track of forked childs in order to handle join calls
     */
    HashMap<Integer, UserProcess> childMap = new HashMap<Integer, UserProcess> ();
    
    // File, Argc, Argv
    private int handleExec(int a0, int a1, int a2) {
    	String filename = readVirtualMemoryString(a0, 256);
    	if (filename == null) return -1;
    	if (!filename.endsWith(".coff")) return -1;
    	if (a1 > pageSize) return -1;
    	if (a1 < 0) return -1;
    	String[] args = new String[a1];
    	for (int i = 0; i < a1; i++) {
    		byte[] addr = new byte[4];
    		if (readVirtualMemory(a2, addr, 0, 4) < 4) return -1; // Fail to read address to argument
    		String arg = readVirtualMemoryString(Lib.bytesToInt(addr, 0), 256);
    		Lib.debug(dbgProcess, pid + "  \tArgument #" + i + " : " + arg);
    		if (arg == null) return -1;
    		args[i] = arg;
    		a2 += 4;
    	}
    	UserProcess toExec = newUserProcess();
    	if (!toExec.execute(filename, args))
    		return -1;
    	UserProcess putRet = childMap.put(toExec.pid, toExec);
    	Lib.assertTrue(putRet == null);
    	return toExec.pid;
    }
    
    private int handleJoin(int a0, int a1) {
    	UserProcess toJoin = childMap.get(a0);
    	if (toJoin == null) return -1; // Can't find the desired process
    	KThread thread = toJoin.thread;
    	if (!thread.isFinished()) 
    		thread.join();
    	childMap.remove(a0);
    	if (toJoin.status == -1) 
    	return 0;
    	else {
    		int cs = toJoin.status;
    		if (writeVirtualMemory(a1, Lib.bytesFromInt(cs), 0, 4) < 4) return -1;
    		else return 1;
    	}
    }
    private int handleExit(int a0) {
    	Lib.debug(dbgProcess, pid + "  \tEntered exit call with pid = " + pid + ", stat = " + a0);
    	status = a0;
    	unloadSections();
    	if (!closeFiles()) status = -1; // Error at closing files
    	//User Process Counter
    	if (!isDebug) {
        counterLock.acquire();
    	runningProcess--;
    	counterLock.release();
    	if (runningProcess == 0) Kernel.kernel.terminate();
    	}
    	KThread.finish();
    	Lib.assertNotReached();
    	return 0;
    }
    private static final int
        syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    
    
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
    if (syscall != 6 && syscall != 7)
    	Lib.debug(dbgProcess, pid + "  Entered system call with handler " + syscall);
    
	switch (syscall) {
	case syscallHalt:
	    return handleHalt();
	case syscallCreate:
		return handleCreate(a0);
	case syscallOpen:
		return handleOpen(a0);
	case syscallRead:
		return handleRead(a0, a1, a2);
	case syscallWrite:
		return handleWrite(a0, a1, a2);
	case syscallClose:
		return handleClose(a0);
	case syscallUnlink:
		return handleUnlink(a0);
	case syscallExec:
		return handleExec(a0, a1, a2);
	case syscallJoin:
		return handleJoin(a0, a1);
	case syscallExit:
		return handleExit(a0);
	default:
	    Lib.debug(dbgProcess, pid + "  Unknown syscall " + syscall);
	    Lib.assertNotReached("Unknown system call!");
	}
	return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	Processor processor = Machine.processor();

	switch (cause) {
	case Processor.exceptionSyscall:
	    int result = handleSyscall(processor.readRegister(Processor.regV0),
				       processor.readRegister(Processor.regA0),
				       processor.readRegister(Processor.regA1),
				       processor.readRegister(Processor.regA2),
				       processor.readRegister(Processor.regA3)
				       );
	    processor.writeRegister(Processor.regV0, result);
	    processor.advancePC();
	    break;				       
		
	default:
	    Lib.debug(dbgProcess, pid + "  Unexpected exception: " +
		      Processor.exceptionNames[cause]);
	    handleExit(-1);  
	}
    }

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    private int initialPC, initialSP;
    private int argc, argv;
    
    public UThread thread;
    public int status;
    
    private int pid;
    private boolean isDebug;
    private static int totalProcess = 0;
    private static int runningProcess = 0;
	private static Lock counterLock = new Lock();
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
}
