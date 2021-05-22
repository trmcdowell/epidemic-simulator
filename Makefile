# Makefile
#     Make commands for Epidemic Simulator
#     Status: MP11 Solution
#     Author: Thomas McDowell

# Example Uses
# The Following primary make commands are supported
#    make                -- equivalent of make Epidemic.class
#    make Epidemic.class -- Makes Epidemic.class and all subsidiaries
#
# The Following secondary make commands are supported
#    make clean          -- delete all automatically generated files
#    make html           -- make HTML documentation using javadoc
#    make test           -- test the simulation


# source files by category

support = Error.java MyRandom.java MyScanner.java Simulator.java
model = Person.java Employee.java Place.java HomePlace.java WorkPlace.java
main = Epidemic.java

EpidemicFiles = $(support) $(model) $(main)

#########################################
# Primary make target

Epidemic.class: $(EpidemicFiles)
	javac $(EpidemicFiles)

make: $(EpidemicFiles)
	javac $(EpidemicFiles)

#########################################
# Secondary make targets

clean:
	rm -f *.class *.html package-list script.js stylesheet.css

index.html:
	javadoc *.java
html: index.html

test: $(EpidemicFiles)
	javac $(EpidemicFiles)
	java Epidemic testepi

#########################################
# Subsidiary targets -- Support classes

Error.class: Error.java
	javac Error.java

MyRandom.class: MyRandom.java
	javac MyRandom.java

Simulator.class: Simulator.java
	javac Simulator.java

MyScanner.class: MyScanner.java
	javac MyScanner.java

# Subsidiary targets -- Model classes

Person.class: Person.java
	javac Person.java

Employee.class: Person.class Employee.java
	javac Employee.java

Place.class: Place.java
	javac Place.java

HomePlace.class: Place.class HomePlace.java
	javac HomePlace.java

WorkPlace.class: Place.class WorkPlace.java
	javac WorkPlace.java
