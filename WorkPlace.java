import java.util.LinkedList;
import java.util.Iterator;

/**
 * WorkPlaces are occupied by employees
 * @author Thomas McDowell
 * @author Douglas Jones
 * @version 11/2/2020
 * Status: Broken off from MP8 solution; it works, but see BUG notices
 * @see Place
 * @see Employee
 */
public class WorkPlace extends Place {
    private final LinkedList <Employee> employees = new LinkedList <Employee>();

    // transmissivity median and scatter for workplaces
    // BUG --  These should come from model description file, not be hard coded
    private static final double transMed = 0.02 * Simulator.hour;
    private static final double transScat = 0.25 * Simulator.hour;

    // need a source of random numbers
    private static final MyRandom rand = MyRandom.stream();

    /** The only constructor for WorkPlace
     *  WorkPlaces are constructed with no residents
     */
    public WorkPlace() {
	super(); // initialize the underlying place
	super.transmissivity = rand.nextLogNormal( transMed, transScat );

	// make the workplace open at 8 AM
	open( 8*Simulator.hour );
    }

    /** Add an employee to a WorkPlace
     *  Should only be called from the person constructor
     *  @param r an Employee, the new worker
     */
    public void addEmployee( Employee r ) {
	employees.add( r );
	// no need to check to see if the person already works there?
    }

    /** Primarily for debugging
     * @return textual name and employees of the workplace
     */
    public String toString() {
	String res = name;
	// DEBUG for (Employee p: employees) { res = res + " " + p.name; }
	return res;
    }

    // simulation methods

    /** open the workplace for business
     *  @param t the time of day
     *  Note that this workplace will close itself 8 hours later, and
     *  opening plus closing should create a 24-hour cycle.
     *  @see close
     */
    private void open( double t ) {
	// BUG -- we should probably do something useful too
	
	class OpenWorkplace extends Simulator.Event {
	    OpenWorkplace() { super( t ); }
	    public void trigger() {
		//System.out.println( this.toString() + " opened at time " + t );
	        close( this.time + 8*Simulator.hour );
	    } 
	}

	// close this workplace 8 hours later
	Simulator.schedule( new OpenWorkplace() );
    }

    /** close the workplace for the day
     *  @param t the time of day
     *  note that this workplace will reopen 16 hours later, and
     *  opening plus closing should create a 24-hour cycle.
     *  @see open
     */
    private void close( double t ) {

	// open this workplace 16 hours later, with no attention to weekends
	class CloseWorkplace extends Simulator.Event {
	    CloseWorkplace() { super( t ); }
	    public void trigger() {
		//System.out.println( this.toString() + " closed at time " + t );
	        open( this.time + 16*Simulator.hour );
	    }
	}

	Simulator.schedule( new CloseWorkplace() );

	// send everyone home
	for (Person p : occupants) {
	    // schedule it for now in order to avoid modifying list inside loop
	    // not doing this gives risk of ConcurrentModificationException
	    p.goHome( t );
	}
    }
}
