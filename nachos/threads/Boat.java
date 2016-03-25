package nachos.threads;
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

//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
//  	begin(1, 2, b);

//  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
//  	begin(3, 3, b);
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
	// Store the externally generated autograder in a class
	// variable to be accessible by children.
	bg = b;

	// Instantiate global variables here
	
	// Create threads here. See section 3.4 of the Nachos for Java
	// Walkthrough linked from the projects page.

	Runnable r = new Runnable() {
	    public void run() {
                SampleItinerary();
            }
        };
        KThread t = new KThread(r);
        t.setName("Sample Boat Thread");
        t.fork();

    }

    static void AdultItinerary()
    {
	bg.initializeAdult(); //Required for autograder interface. Must be the first thing called.
	//DO NOT PUT ANYTHING ABOVE THIS LINE. 

	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/
    }

    static void ChildItinerary()
    {
	bg.initializeChild(); //Required for autograder interface. Must be the first thing called.
	//DO NOT PUT ANYTHING ABOVE THIS LINE. 
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
