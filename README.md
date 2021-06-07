Epidemic simulator

Author: Tom McDowell <br>
Author: Douglas Jones

Version: 11/11/2020 -- the 11/2/2020 version broken into multiple source files

This distribution contains:

-- README -- this file <br>
-- EpidemicSource -- the list of all Java source files included here <br>
-- testepi -- input to the simulator

Shell command to build the simulator:
```
$ javac @EpidemicSource
```
Shell command to build Javadoc web site:
```
$ javadoc @EpidemicSource
```
Shell command to test the simulator:
```
$ java Epidemic testepi
```

This test runs the simulator for one simulated month, using a community of
1000 people, 10 of which are initially infected with a COVID-like disease.
By the end of the month, on the order of 10 people will die and from 500 to 900
people will remain uninfected.  The rate of disease spread through the
community depends on a randomly constructed social network of family and
job relationships, with a 50% employment rate where both homes and workplaces
have random variations in their transmissivity.  No two runs are likely to
produce the same results.

The community is describe in the file testepi, but currently, the disease
characteristics are described by constants hard-coded into the simulator.
There are bug notices in the code, in the form of comments with a // BUG
header noting several places where it is clear that the code can be
improved.

The input community description file format needs a manual, but moving
the disease characteristics into this file is an even higher priority.

The logic of workplaces in the model could be replicated to support schools
with students and stores with customers.

The effect of mitigation strategies such as closing workplaces with more than
some number of employees when the number of beridden people exceeds some
threshold could be modeled by making people stay home from such places when
these criteria are met.
