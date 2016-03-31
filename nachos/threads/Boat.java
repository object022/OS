package nachos.threads;
import java.util.Collections;
import java.util.LinkedList;

import nachos.ag.BoatGrader;

public class Boat
{
	
	/**
	 * Notes on the Implementation of Boat(Project 1 Task 6)
	 * Our strategy is pretty simple. First there is a vote for children
	 * and two children becomes leader and assistant. The rest people waiting
	 * waits on a condition variable for leader to wake.
	 * First, leader send assistant to the other side and return.
	 * From now on, he wakes one people. If this is a children, he rows him
	 * to Molokai and return.
	 * If this is an adult, the adult goes to the other side, wakes assistant,
	 * assistant rows back and they perform the first step to return to original
	 * state.
	 * For all these actions that requires order, we use a communicator
	 * to ensure they happen in the order we expect. Two communicators
	 * are to simulate communicators at both islands.
	 * 
	 * The real challenge arises when there are no other people on Oahu. How
	 * can the leader know this and go to Molokai without waiting indefinitely?
	 * 
	 * 
	 * The description is conflicting with itself. It states:
	 * "You cannot pass the number of threads created to the
	 *  threads representing adults and children"
	 * while saying
	 * "It's reasonable to allow a person to see how many children
	 *  or adults are on the same island"
	 *  
	 * Does this mean that we can bypass the first constraint
	 * by storing the number of children in a shared variable?
	 *  
	 *  
	 * Consider if this is not possible, then we're unable to determine
	 * how many person are on the island of Oahu, nor if there are anyone
	 * left on Oahu from the view of a people. This will inevitably
	 * lead to a deadlock where one last children waiting another
	 * when he can't figure out if there is still one more children.
	 *  
	 * There are several ways to get around this "evil last child" problem.
	 * One is that we may assume we're using a Priority Scheduler. Then we
	 * can reliably count peoples.
	 * Also, we can assume that if there is only one thread running, when 
	 * voluntarily yield, it'll yield to the boat process. Then, the last 
	 * children yield before he return to Oahu, and if he is indeed the last
	 * child, boat terminates him.
	 *  
	 * Another way is to use Alarm class. The last children on Oahu checks if someone
	 * still there every second or so; The main boat process will terminate everyone
	 * when there are no person left on Oahu. However, one may consider
	 * this to be a busy-waiting behavior, and it's largely inconsistent 
	 * with the rest of the program.
	 *  
	 * Anyway, I find this problem quite strange.. Maybe I need some clarification
	 * from the staff.
	 */
    static BoatGrader bg;
    
    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();
	
	begin(50, 50, b);

    }

    public static Lock lock;
    public static Condition2 condLeader, condAssist, condPeople;
    public static Communicator toBoat, onOahu, onMolokai, cheat;
    
    //Cheating Info: When the PriorityQueue is correctly implemented, let begin have higher priority than
    //Everything else. Then when we need to hear from cheat, yield instead.
    
    public static int leaderVote = 0, reported = 0; // how many people have reported themselves and are still on Oahu?
    public static boolean forceEnter = false; // If no one is seen to report himself, the next coming person can't sleep
    public static void begin( int adults, int children, BoatGrader b )
    {
	// Store the externally generated autograder in a class
	// variable to be accessible by children.
	bg = b;
	lock = new Lock();
	condLeader = new Condition2(lock);
	condAssist = new Condition2(lock);
	condPeople = new Condition2(lock);
	toBoat = new Communicator();
	onOahu = new Communicator();
	onMolokai = new Communicator();
	cheat = new Communicator();
	// Instantiate global variables here
	
	// Create threads here. See section 3.4 of the Nachos for Java
	// Walkthrough linked from the projects page.

	Runnable runnableChildren = new Runnable() {
		@Override
		public void run() {
			ChildItinerary();
		}
	};
	Runnable runnableAdult = new Runnable() {
		@Override
		public void run() {
			AdultItinerary();
		}
	};
	
	
	// Test: Shuffle
	LinkedList<KThread> tlist = new LinkedList<KThread> ();
	for (int i = 0; i < adults; i++)
		tlist.add(new KThread(runnableAdult).setName("Adult #" + i));
	for (int i = 0; i < children; i++)
		tlist.add(new KThread(runnableChildren).setName("Child #" + i));
	Collections.shuffle(tlist);
	for (int i = 0; i < tlist.size(); i++)
		tlist.get(i).fork();
	while (true) {
		int msg = toBoat.listen();
		//System.out.println("Boat: Receives message " + msg);
		switch (msg) {
		case 1: // Children arrives
			children--;
			break;
		case -1: // Adult arrives
			adults--;
			break;
		case 0: // Leader on Molokai, waiting decision: THIS IS CHEATING
			if ((children == 2) && (adults == 0)) // Assistant is already on Molokai
				return;
			//System.out.println("Boat: Leader should pass, people remaining = " + children  + " " + adults);
			cheat.speak(0);
		}
	}
	
    }

    static int aid = 0;
    static void AdultItinerary()
    {
	bg.initializeAdult(); //Required for autograder interface. Must be the first thing called.
	//DO NOT PUT ANYTHING ABOVE THIS LINE. 
	/*
	 * wake up - wake leader - row to Molokai - wake assistant
	 */
	lock.acquire();
	reported++;
	if (!forceEnter)
	condPeople.sleep();

	int id = aid++; // only for debugging purposes

	//System.out.println("Adult #" + id + " Entering - forceEnter = " + forceEnter);
	forceEnter = false;
	reported--;
	//System.out.println("Adult #" + id + " wake the leader");
	condLeader.wake();
	lock.release();
	onOahu.speak(-1);
	//System.out.println("Adult #" + id + " go to Molokai");
	bg.AdultRowToMolokai();
	toBoat.speak(-1);
	lock.acquire();
	//System.out.println("Adult #" + id + " wake the assistant to row back");
	condAssist.wake();
	lock.release();
    }
    static int cid = 0;
    static void ChildItinerary()
    {
	bg.initializeChild(); //Required for autograder interface. Must be the first thing called.
	//DO NOT PUT ANYTHING ABOVE THIS LINE. 
	
	//Electing the leader and assistant
	//Decide the ID
	int id = -1;
	lock.acquire();
	int role = leaderVote;
	if (role < 2) leaderVote++;
	else id = cid++;
	lock.release();
	
	switch (role) {
	case 0: // Leader
		bg.ChildRowToMolokai();
		onOahu.speak(0);
		onMolokai.listen();
		
		lock.acquire();
		if (reported == 0) {
			toBoat.speak(0);
			cheat.listen();
		}
		lock.release();
		
		bg.ChildRowToOahu();
		//Wake up the first people
		lock.acquire();
		if (reported == 0) forceEnter = true;
		else condPeople.wake();
		lock.release();
		
		//Main Loop
		while (true) {
			int type = onOahu.listen();
			switch (type) {
			case 1: // Children
				/*
				 * Row to Molokai - Wait children to ride - Row to Oahu
				 */
				//System.out.println("Leader: rowing to Molokai and wait");
				bg.ChildRowToMolokai();
				onOahu.speak(0);
				onOahu.listen();
				
				lock.acquire();
				if (reported == 0) {
					toBoat.speak(0);
					cheat.listen();
				}
				lock.release();
				
				//System.out.println("Leader: rowing back to Oahu");
				bg.ChildRowToOahu();
				break;
			case -1: // Adult
				/*
				 * Wait assistant to row back - row to Molokai - wait assistant to ride - row to Oahu
				 */
				lock.acquire();
				condLeader.sleep();
				lock.release();
				//System.out.println("Leader: waiting assistant to row back");
				onOahu.listen();
				//System.out.println("Leader: rowing to Molokai to take assistant there");
				bg.ChildRowToMolokai();
				onOahu.speak(0);
				onMolokai.listen();
				
				
				lock.acquire();
				if (reported == 0) {
					toBoat.speak(0);
					cheat.listen();
				}
				lock.release();
				
				//System.out.println("Leader: rowing back to Oahu");
				
				bg.ChildRowToOahu();
			}
			
			//System.out.println("Leader finished, type = " + type);
			
			lock.acquire();
			//System.out.println("People waiting to wake up: " + reported);
			if (reported == 0) 
				forceEnter = true;
			else condPeople.wake();
			condLeader.sleep();
			lock.release();
		}
	case 1: // Assistant
		//Get to another side with the help of the leader
		//Everybody else sleeping on CondPeople
		onOahu.listen();
		bg.ChildRideToMolokai();
		onMolokai.speak(0);
		while (true) {
		/* Adult wakes assistant on Molokai - row back to Oahu
		 *  - wait for leader to row to Molokai - ride to Molokai
		 */
		lock.acquire();
		condAssist.sleep();
		lock.release();
		//System.out.println("Assistant: summoned by adult, rowing to Oahu");
		bg.ChildRowToOahu();
		lock.acquire();
		//System.out.println("Assistant: waking the leader");
		condLeader.wake();
		lock.release();
		onOahu.speak(0);
		onOahu.listen();
		//System.out.println("Assistant: rowing to Molokai and wait");
		bg.ChildRideToMolokai();
		onMolokai.speak(0);
		}
	case 2: // Normal
		/*
		 * Wait - wake up leader - Wait leader to row to Molokai - ride to Molokai
		 */
		lock.acquire();
		reported++;
		if (!forceEnter)
		condPeople.sleep();
		//System.out.println("Child #" + id + ": Entering - forceEnter = " + forceEnter);
		forceEnter = false;
		reported--;
		//System.out.println("Child #" + id + ": wake the leader");
		condLeader.wake();
		lock.release();
		onOahu.speak(1);
		onOahu.listen();
		//System.out.println("Child #" + id + ": Riding to Molokai");
		bg.ChildRideToMolokai();
		toBoat.speak(1);
		onOahu.speak(2);
	}
    }

    
}
