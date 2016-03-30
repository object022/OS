package nachos.ag;

public class BoatGrader {

    /**
     * BoatGrader consists of functions to be called to show that
     * your solution is properly synchronized. This version simply
     * prints messages to standard out, so that you can watch it.
     * You cannot submit this file, as we will be using our own
     * version of it during grading.

     * Note that this file includes all possible variants of how
     * someone can get from one island to another. Inclusion in
     * this class does not imply that any of the indicated actions
     * are a good idea or even allowed.
     */
	int childOnOahu = 0, childOnMolokai = 0, 
		adultOnOahu = 0, adultOnMolokai = 0,
		peopleOnBoat = 0;
	int verbosity = 0;
	boolean boatPos = false, hasError = false; 
	//False at Oahu, True at Molokai
	//NEW ADDITION FOR 2014
	//MUST BE CALLED AT THE START OF CHILDITINERARY!
	public void error(String note) {
		if (hasError) return;
		System.out.println("Error: " + note);
		hasError = true;
	}
	public synchronized void initializeChild(){
		if (verbosity > 0)
		System.out.println("A child has forked.");
		childOnOahu += 1;
	}
	
	//NEW ADDITION FOR 2014
	//MUST BE CALLED AT THE START OF ADULTITINERARY!
	public synchronized void initializeAdult(){
		if (verbosity > 0)
		System.out.println("An adult as forked.");
		adultOnOahu += 1;
	}

    /* ChildRowToMolokai should be called when a child pilots the boat
       from Oahu to Molokai */
    public synchronized void ChildRowToMolokai() {
    	if (verbosity > 0)
    	System.out.println("**Child rowing to Molokai.");
    	childOnOahu -= 1;
    	childOnMolokai += 1;
    	if (boatPos) {
    		// Different Side, Someone must be on the boat
    		if (peopleOnBoat == 0) error("Boat on Molokai when trying to start at Oahu");
    		peopleOnBoat = 1;
    		boatPos = false;
    	} else {
    	if (peopleOnBoat != 0) error("Row after others on boat");
    	peopleOnBoat = 1;
    	}
    }

    /* ChildRowToOahu should be called when a child pilots the boat
       from Molokai to Oahu*/
    public synchronized void ChildRowToOahu() {
	System.out.println("**Child rowing to Oahu.");
	childOnMolokai -= 1;
	childOnOahu += 1;
	if (!boatPos) {
		// Different Side, Someone must be on the boat
		if (peopleOnBoat == 0) error("Boat on Molokai when trying to start at Oahu");
		peopleOnBoat = 1;
		boatPos = true;
	} else {
	if (peopleOnBoat != 0) error("Row after others on boat");
	peopleOnBoat = 1;
	}
    }

    /* ChildRideToMolokai should be called when a child not piloting
       the boat disembarks on Molokai */
    public synchronized void ChildRideToMolokai() {
	if (verbosity > 0)
    	System.out.println("**Child arrived on Molokai as a passenger.");
	childOnOahu -= 1;
	childOnMolokai += 1;
	if (boatPos) {
		// Different Side, throw error
		error("Can't ride to Molokai, boat is already there");
	} else {
	if (peopleOnBoat == 0) error("First ride before anyone on boat");
	if (peopleOnBoat == 2) error("Not enough space");
	peopleOnBoat = 2;
	}
	//Check if simulation ends
	if (childOnOahu == 0)
		if (adultOnOahu == 0)
			System.out.println("All people landed on Molokai - Process may end");
    }

    /* ChildRideToOahu should be called when a child not piloting
       the boat disembarks on Oahu */
    public synchronized void ChildRideToOahu() {
	if (verbosity > 0)
    	System.out.println("**Child arrived on Oahu as a passenger.");
	childOnMolokai -= 1;
	childOnOahu += 1;
	if (!boatPos) {
		// Different Side, throw error
		error("Can't ride to Oahu, boat is already there");
	} else {
	if (peopleOnBoat == 0) error("First ride before anyone on boat");
	if (peopleOnBoat == 2) error("Not enough space");
	peopleOnBoat = 2;
	}
	if (childOnOahu == 0)
		if (adultOnOahu == 0)
			System.out.println("All people landed on Molokai - Process may end");

    }

    /* AdultRowToMolokai should be called when a adult pilots the boat
       from Oahu to Molokai */
    public synchronized void AdultRowToMolokai() {
	if (verbosity > 0)
    	System.out.println("**Adult rowing to Molokai.");
	adultOnOahu -= 1;
	adultOnMolokai += 1;
	if (boatPos) {
		// Different Side, Someone must be on the boat
		if (peopleOnBoat == 0) error("Boat on Molokai when trying to start at Oahu");
		peopleOnBoat = 2;
		boatPos = false;
	} else {
	if (peopleOnBoat != 0) error("Row after others on boat");
	peopleOnBoat = 2;
	}
    }

    /* AdultRowToOahu should be called when a adult pilots the boat
       from Molokai to Oahu */
    public void AdultRowToOahu() {
	System.out.println("**Adult rowing to Oahu.");
	error("Prohibited Action");
    }

    /* AdultRideToMolokai should be called when an adult not piloting
       the boat disembarks on Molokai */
    public void AdultRideToMolokai() {
	System.out.println("**Adult arrived on Molokai as a passenger.");
	error("Prohibited Action");
    }

    /* AdultRideToOahu should be called when an adult not piloting
       the boat disembarks on Oahu */
    public void AdultRideToOahu() {
	System.out.println("**Adult arrived on Oahu as a passenger.");
	error("Prohibited Action");
    }
}




