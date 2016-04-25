package nachos.userprog;

import java.util.Random;

import nachos.machine.Lib;

public class Tests {
	private Tests() { // Can't instantiate this class.
		
	};
	public static UserProcess process;
	public static Random rand = new Random();
	public static void VMTest() {
		//process = new UserProcess(true);
		// So this doesn't mess up the later process
		/*int len = 7000, start = 1111;
		byte[] data = new byte[len];
		byte[] verf = new byte[len];
		rand.nextBytes(data); // Thank you RNG
		int res = process.writeVirtualMemory(start, data);
		Lib.debug('a', res + " bytes written to memory");
		Lib.assertTrue(res == len);
		res = process.readVirtualMemory(start, verf);
		for (int i = 0; i < len; i++)
			if (data[i] != verf[i]) Lib.debug('a', "Error at position" + i);
		*/
		}
}
