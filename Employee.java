/**
 * Employees are People who work
 * @author Douglas Jones
 * @author Thomas McDowell
 * @version 11/2/2020
 * Status: broken off of MP8 solution; it works, but see BUG notices
 * @see Person
 * @see WorkPlace
 */
public class Employee extends Person {
    // instance variables
    private WorkPlace job;  // employees have WorkPlaces
			    // can't be final because set post constructor

    // need a source of random numbers
    private static final MyRandom rand = MyRandom.stream();

    /** The only constructor
     *  @param h the HomePlace of the newly constructed Employee
     *  Note that employees are created without well-defined workplaces
     */
    public Employee( HomePlace h ) {
	super( h ); // construct the base person
	job = null;

	// go to work every day at 25 minutes before 8 AM
	goToWork( (8*Simulator.hour) - (25*Simulator.minute) );
    }

    /** Set workplace of employee
     *  @param w the workPlace of the newly constructed Employee
     *  No employee's workplace may be set more than once
     */
    public void setWorkplace( WorkPlace w ) {
	assert job == null;
	job = w;
	w.addEmployee( this );
    }

    /** Primarily for debugging
     * @return textual name home and employer of this person
     */
    public String toString() {
	return super.toString() ;// DEBUG + " " + job.name;
    }

    // simulation methods

    /** Simulate the daily trip to work
     * @param time of departure
     */
    private void goToWork( double t ) {
	if (infectionState == States.dead) return; // finish killing the dead!

	// people only leave home if feeling OK
	if (infectionState != States.bedridden) {
	    double travelTime = rand.nextLogNormal(
		20 * Simulator.minute, // mean travel time
		3 * Simulator.minute   // scatter in travel time
	    );

	    // go to work every day at the same time
            class WorkTravel extends Simulator.Event {
	        Employee employee;
	        WorkTravel( Employee employee ) { 
		    super( t );
		    this.employee = employee;
	        }
	        public void trigger() {
		    employee.place.depart( employee, time );
		    employee.travelTo( employee.job, this.time + travelTime );
	            goToWork( this.time + Simulator.day );
	        }
	    }
	    Simulator.schedule( new WorkTravel( this ) );
	}
    }
}
