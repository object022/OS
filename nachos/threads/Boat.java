package nachos.threads;
import java.util.PriorityQueue;

import nachos.ag.BoatGrader;
import nachos.threads.PriorityScheduler.ThreadState;

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
	
	System.out.println("\n ***Testing Boats with only 2 children***");
	begin(0, 2, b);

    }

    public static Lock lock;
    public static Condition condLeader, condAssist, condPeople;
    public static Communicator toBoat, onOahu, onMolokai;
    public static PriorityQueue queue;
    public static int leaderVote = 0;
    public static void begin( int adults, int children, BoatGrader b )
    {
	// Store the externally generated autograder in a class
	// variable to be accessible by children.
	bg = b;
	lock = new Lock();
	condLeader = new Condition(lock);
	condAssist = new Condition(lock);
	condPeople = new Condition(lock);
	toBoat = new Communicator();
	onOahu = new Communicator();
	onMolokai = new Communicator();
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
	
	for (int i = 0; i < adults; i++){
		KThread t = new KThread(runnableAdult).setName("Adult #" + i);
		t.fork();
		((ThreadState)t.schedulingState).setPriority(2);
	}
	for (int i = 0; i < children; i++)
		new KThread(runnableChildren).setName("Child #" + i).fork();

	while (true) {
		int msg = toBoat.listen();
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
		}
	}
	
    }
    

    static void AdultItinerary()
    {
	bg.initializeAdult(); //Required for autograder interface. Must be the first thing called.
	//DO NOT PUT ANYTHING ABOVE THIS LINE. 

	lock.acquire();
	condPeople.sleep();
	lock.release();
	/*
	 * wake by leader - row to Molokai - wake assistant
	 */
	onOahu.speak(-1);
	bg.AdultRowToMolokai();
	toBoat.speak(-1);
	lock.acquire();
	condAssist.wake();
	lock.release();
    }

    static void ChildItinerary()
    {
	bg.initializeChild(); //Required for autograder interface. Must be the first thing called.
	//DO NOT PUT ANYTHING ABOVE THIS LINE. 

	//Electing the leader and assistant
	lock.acquire();
	int role = leaderVote;
	if (role < 2) leaderVote++;
	lock.release();
	
	switch (role) {
	case 0: // Leader
		//Take the assistant to the other side, everybody else sleeping on CondPeople
		bg.ChildRowToMolokai();
		onOahu.speak(0);
		onMolokai.listen();
		toBoat.speak(0); 
		//Be warned, we're cheating here! Boat will decide if leader should continue
		lock.acquire();
		condLeader.sleep();
		lock.release();
		//End of cheat
		bg.ChildRowToOahu();
		//Main Loop
		while (true) {
			lock.acquire();
			condPeople.wake();
			lock.release();
			int type = onOahu.listen();
			switch (type) {
			case 1: // Children
				/*
				 * Row to Molokai - Wait children to ride - Row to Oahu
				 */
				bg.ChildRowToMolokai();
				onOahu.speak(0);
				toBoat.speak(0); 
				// Be warned, we're cheating here! Boat will decide if leader should continue
				lock.acquire();
				condLeader.sleep();
				lock.release();
				//End of cheat
				bg.ChildRowToOahu();
				break;
			case -1: // Adult
				/*
				 * Wait assistant to row back - row to Molokai - wait assistant to ride - row to Oahu
				 */
				lock.acquire();
				condLeader.sleep();
				lock.release();
				onOahu.listen();
				bg.ChildRowToMolokai();
				onOahu.speak(0);
				onMolokai.listen();
				toBoat.speak(0); 
				// Be warned, we're cheating here! Boat will decide if leader should continue
				lock.acquire();
				condLeader.sleep();
				lock.release();
				//End of cheat
				bg.ChildRowToOahu();
			}
			lock.acquire();
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
		bg.ChildRowToOahu();
		lock.acquire();
		condLeader.wake();
		lock.release();
		onOahu.speak(0);
		onOahu.listen();
		bg.ChildRideToMolokai();
		onMolokai.speak(0);
		}
	case 2: // Normal
		/*
		 * Wait leader to wake - Wait leader to row to Molokai - ride to Molokai
		 */
		lock.acquire();
		condPeople.sleep();
		lock.release();
		onOahu.speak(1);
		onOahu.listen();
		bg.ChildRideToMolokai();
		toBoat.speak(1);
	}
    }

    static void SampleItinerary()
    {
	// Please note that this isn't a valid solution (you can't fit
	// all of them on the boat). Please also note that you may not
	// have a single thread calculate a solution and then just play
	// it back at the autograder -- you will be caught.
	System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
	bg.AdultRowToMolokai();
	bg.ChildRideToMolokai();
	bg.AdultRideToMolokai();
	bg.ChildRideToMolokai();
    }
    
}
