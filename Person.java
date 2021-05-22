import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * People occupy places
 * @author Douglas Jones
 * @author Thomas McDowell
 * @version 12/7/2020
 * Status: MP12 Solution
 * @see Place
 * @see Employee
 */
public class Person {
    // private stuff needed for instances

    protected enum States {
	uninfected, latent, infectious, bedridden, recovered, dead
	// the order of the above is significant: >= uninfected is infected
    }

    // static attributes describing progression of infection
    // BUG --  These should come from model description file, not be hard coded
    double latentMedT = 2 * Simulator.day;
    double latentScatT = 1 * Simulator.day;
    double bedriddenProb = 0.7;
    double infectRecMedT = 1 * Simulator.week;
    double infectRecScatT = 6 * Simulator.day;
    double infectBedMedT = 3 * Simulator.day;
    double infectBedScatT = 5 * Simulator.day;
    double deathProb = 0.2;
    double bedRecMedT = 2 * Simulator.week;
    double bedRecScatT = 1 * Simulator.week;
    double bedDeadMedT = 1.5 * Simulator.week;
    double bedDeadScatT = 1 * Simulator.week;

    // static counts of infection progress
    static int numUninfected = 0;
    static int numLatent = 0;
    static int numInfectious = 0;
    static int numBedridden = 0;
    static int numRecovered = 0;
    static int numDead = 0;

    // fixed attributes of each instance
    final HomePlace home;  // all people have homes
    public final String name;      // all people have names

    // instance variables
    protected Place place;         // when not in transit, where the person is
    public States infectionState;  // all people have infection states

    // the collection of all instances
    private static final LinkedList <Person> allPeople =
	new LinkedList <Person> ();

    // need a source of random numbers
    private static final MyRandom rand = MyRandom.stream();

    /** The only constructor
     *  @param h the home of the newly constructed person
     */
    public Person( HomePlace h ) {
	name = super.toString();
	home = h;
	place = h; // all people start out at home
	infectionState = States.uninfected;
	numUninfected = numUninfected + 1;
	h.addResident( this );

	allPeople.add( this ); // this is the only place items are added!
    }

    /** Predicate to test person for infectiousness
     *  @return true if the person can transmit infection
     */
    public boolean isInfectious() {
	return (infectionState == States.infectious)
	    || (infectionState == States.bedridden);
    }

    /** Primarily for debugging
     *  @return textual name and home of this person
     */
    public String toString() {
	return name ;// DEBUG  + " " + home.name + " " + infectionState;
    }

    /** Shuffle the population
     *  This allows correlations between attributes of people to be broken
     */
    public static void shuffle() {
	Collections.shuffle( allPeople, rand );
    }

    /** Allow outsiders to iterate over all people
     *  @return an iterator over people
     */
    public static Iterator <Person> iterator() {
	return allPeople.iterator();
    }

    // simulation methods relating to infection process

    /** Infect a person
     *  @param t the time at which the person is infected (latent)
     *  called when circumstances call for a person to become infected
     */
    public void infect( double t ) {
	if (infectionState == States.uninfected) {
	    // infecting an already infected person has no effect

	    double delay = rand.nextLogNormal( latentMedT, latentScatT );

	    numUninfected = numUninfected - 1;
	    infectionState = States.latent;
	    numLatent = numLatent + 1;

	    Simulator.schedule( new BecomeInfectious( t, delay, this ) );
	}
    }

    /** An infected but latent person becomes infectous
     *  scheduled by infect() to make a latent person infectious
     *  @param t the time at which the person becomes infectious
     */
    void beInfectious( double t ) {
	numLatent = numLatent - 1;
	infectionState = States.infectious;
	numInfectious = numInfectious + 1;

	double recDelay = rand.nextLogNormal( infectRecMedT, infectBedScatT );
	double bedDelay = rand.nextLogNormal( infectBedMedT, infectBedScatT );


	if (place != null) place.oneMoreInfectious( t );

	if ( rand.nextFloat() > bedriddenProb ) {
	    Simulator.schedule( new BecomeRecoveredInf( t, recDelay, this ) );
	} else {
	    Simulator.schedule( new BecomeBedridden( t, bedDelay, this ) );
	}
    }

    /** An infectious person becomes bedridden
     *  scheduled by beInfectious() to make an infectious person bedridden
     *  @param t the time the person becomes bedridden
     */
    void beBedridden( double t ) {
	numInfectious = numInfectious - 1;
	infectionState = States.bedridden;
	numBedridden = numBedridden + 1;

	double recDelay = rand.nextLogNormal( bedRecMedT, bedRecScatT );
	double deadDelay = rand.nextLogNormal( bedDeadMedT, bedDeadScatT );

	// if in a place (not in transit) that is not home, go home now!
	if ((place != null) && (place != home)) goHome( t );

	if ( rand.nextFloat() > deathProb ) {
	    Simulator.schedule( new BecomeRecoveredBed( t, recDelay, this) );
	} else {
	    Simulator.schedule( new BecomeDead( t, deadDelay, this ) );
	}
    }

    /** A infectious or bedridden person recovers
     *  scheduled by beInfectious() or beBedridden to make a person recover
     *  @param t the time the person recovers
     */
    void beRecovered( double t ) {
	if (infectionState == States.infectious) {
	    numInfectious = numInfectious - 1;
	} else {
	    numBedridden = numBedridden - 1;
	}
	infectionState = States.recovered;
	numRecovered = numRecovered + 1;

	if (place != null) place.oneLessInfectious( t );
    }

    /** A bedridden person dies
     *  scheduled by beInfectious() to make a bedridden person die
     *  @param t the time the person dies
     */
    void beDead( double t ) {
	numBedridden = numBedridden - 1;
	infectionState = States.dead; // needed to prevent resurrection
	numDead = numDead + 1;

	// if the person died in a place, make them leave it!
	if (place != null) place.depart( this, t );

	// BUG: leaves them in the directory of residents and perhaps employees
    }

    // simulation methods relating to daily reporting

    /** Make the daily midnight report
     *  @param t the current time
     */
    public static void report( double t ) {

	Simulator.schedule( new ReportEvent( t ) );

    }

    // simulation methods relating to personal movement

    /** Make a person arrive at a new place
     *  @param p new place
     *  @param t the current time
     *  scheduled
     */
    void arriveAt( double t, Place p ) {
	if ((infectionState == States.bedridden) && (p != home)) {
	    // go straight home if you arrive at work while sick
	    goHome( t );

	} else if (infectionState == States.dead) { // died on the way to work
	    // allow this person to be forgotten

	} else { // only really arrive if not sick
	    p.arrive( this, t );
	    this.place = p;
	    //System.out.println( this.name + " arrived at "
		//	                  + p.name + " at time " + t );
	}
    }

    /** Move a person to a new place
     *  @param p the place where the person travels
     *  @param t the time at which the move will be completed
     *  BUG -- if time was the time the trip started:
     *  travelTo could do the call to this.place.depart()
     *  and it could compute the travel time
     */
    public void travelTo( Place p, double t ) {

	Simulator.schedule( new TravelEvent( t, this, p ) );
    }

    /** Simulate the trip home from wherever
     * @param t time of departure
     */
    public void goHome( double t ) {
	double travelTime = rand.nextLogNormal(
	    20 * Simulator.minute, // mean travel time
	    3 * Simulator.minute   // scatter in travel time
	);

	Simulator.schedule( new TravelHome( t, travelTime, this ) );
    }
}

/** Event where a person becomes infected
 *  @author Thomas McDowell
 *  @version 12/7/2020
 *  @see Simulator
 *  @see Person
 */
class BecomeInfectious extends Simulator.Event {
    Person p;
    /** Event constructor
     *  @param t the time the person is infected
     *  @param delay the amount of time the person will spend latent
     *  @param p the person who will become infectious
     */
    BecomeInfectious( double t, double delay, Person p ) {
        super( t + delay );
        this.p = p;
    }
    /** trigger() calls be infectious on this event's person
     */
    public void trigger() { p.beInfectious( this.time ); }
}

/** Event where a person becomes recovered after being infected
 *  @author Thomas McDowell
 *  @version 12/7/2020
 *  @see Simulator
 *  @see Person
 */
class BecomeRecoveredInf extends Simulator.Event {
    Person p;
    /** Event constructor
     *  @param t the time the person becomes infectious
     *  @param delay the amount of time the person will spend infectious
     *  @param p the person who will become recovered
     */
    BecomeRecoveredInf( double t, double delay, Person p ) {
        super( t + delay );
	this.p = p;
    }
    /** trigger() calls beRecovered on the person
     */
    public void trigger() { p.beRecovered( this.time ); }
}

/** Event where a person becomes bedridden
 *  @author Thomas McDowell
 *  @version 12/7/2020
 *  @see Simulator
 *  @see Person
 */
class BecomeBedridden extends Simulator.Event {
    Person p;
    /** Event constructor
     *  @param t the time the person becomes infectious
     *  @param delay the amount of time the person will spend infectious
     *  @param p the person who will become bedridden
     */
    BecomeBedridden( double t, double delay, Person p ) {
        super( t + delay);
	this.p = p;
    }
    public void trigger() { p.beBedridden( this.time ); }
}

/** Event where a person becomes recovered after being bedridden
 *  @author Thomas McDowell
 *  @version 12/7/2020
 *  @see Simulator
 *  @see Person
 */
class BecomeRecoveredBed extends Simulator.Event {
    Person p;
    /** Event constructor
     *  @param t the time the person becomes bedridden
     *  @param delay the amount of time the person will spend bedridden
     *  @param p the person who will become recovered
     */
    BecomeRecoveredBed( double t, double delay, Person p ) {
        super( t + delay );
        this.p = p;
    }
    /** trigger() calls beRecovered on the person
     */
    public void trigger() { p.beRecovered( this.time ); }
}

/** Event where a person dies
 *  @author Thomas McDowell
 *  @version 12/7/2020
 *  @see Simulator
 *  @see Person
 */
class BecomeDead extends Simulator.Event {
    Person p;
    /** Event constructor
     *  @param t the time the person becomes bedridden
     *  @param delay the amount of time the person will spend bedridden
     *  @param p the person who will die
     */
    BecomeDead( double t, double delay, Person p ) {
        super( t + delay );
	this.p = p;
    }
    /** trigger() calls beDead on the person
     */
    public void trigger() { p.beDead( this.time ); }
}

/** Event where a person travels to a place
 *  @author Thomas McDowell
 *  @version 12/7/2020
 *  @see Simulator
 *  @see Person
 *  @see Place
 */
class TravelEvent extends Simulator.Event {
    Person person;
    Place place;
    /** Event constructor
     *  @param t the time the person arrives at the place
     *  @param person the person traveling
     *  @param place the place being traveled to
     */
    TravelEvent( double t, Person person, Place place ) {
        super( t );
        this.person = person;
	this.place = place;
    }
    /** trigger() sets a persons place to null and makes them arrive
     *  at their destination
     */
    public void trigger() {
        this.person.place = null;
        this.person.arriveAt( this.time, this.place );
    }
}

/** Event where a person travels to their home
 *  @author Thomas McDowell
 *  @version 12/7/2020
 *  @see Simulator
 *  @see Person
 *  @see Place
 *  @see HomePlace
 */
class TravelHome extends Simulator.Event {
    Person person;
    double travelTime;
    /** Event constructor
     *  @param t the time the person departs their current location
     *  @param travelTime the travel time
     *  @param p the person traveling home
     */
    TravelHome( double t, double travelTime, Person person ) {
        super( t );
        this.person = person;
    }
    /** trigger() makes a person travel to their HomePlace
     */
    public void trigger() {
        // required due to case of arriving at work after falling ill
        if (this.person.place != null) {
	    this.person.place.depart( this.person, this.time );
	}
        this.person.travelTo( this.person.home, this.time + this.travelTime );
    }
}

/** Event to report simulation data to user
 *  @author Thomas McDowell
 *  @version 12/7/2020
 *  @see Simulator
 *  @see Person
 */
class ReportEvent extends Simulator.Event {
    /** Event constructor
     *  @param t the time the daily reports are made
     */
    ReportEvent( double t ) {
        super( t );
    }
    /** trigger() prints out the simulation report and schedules the next one
     */
    public void trigger() {
        System.out.println(
            "at " + this.time
            + ", un = " + Person.numUninfected
            + ", lat = " + Person.numLatent
            + ", inf = " + Person.numInfectious
            + ", bed = " + Person.numBedridden
            + ", rec = " + Person.numRecovered
            + ", dead = " + Person.numDead
                );

        Person.report( this.time + Simulator.day );

            }
}
