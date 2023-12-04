# DDNR: Data-Driven Norm Revision #
### A data-driven approach to norm revision that synthesises revised norms with respect to a dataset of traces describing the behavior of the autonomous agents in a Multi-Agent System ###

This repository contains
- A Java implementation of DDNR, a data-driven approach to norm revision in Normative Multi-Agent Systems (NMASs). DDNR synthesises revised norms with respect to a dataset of traces describing the behavior of the
  agents in the system. 
- A Java-SUMO implementation of a highway scenario where vehicles (cars and trucks) are autonomous agents which reason about the enforced norms (speed limit or minimum safety distance) when driving though the highway section. The behavior of vehicles (agents) though the highway section is monitored and collected in the form of execution traces.
- Code and results from experimentation with DDNR to revise the enforced norms based on the traces (data) generated by the agents.

### How do I get set up? ###
The repository relies on the [```Traas```](https://sumo.dlr.de/docs/TraCI/TraaS.html) 
java library for working with ```TraCI``` to interface with an installation of [```SUMO```](https://sumo.dlr.de/docs/index.html).

To execute the code, therefore, it is necessary to install ```Java 13+``` and [```SUMO```](https://sumo.dlr.de/docs/index.html) on the running machine.
The ```Traas``` jar is provided in folder ```lib```.

After installing the required dependencies,
to run experiments it is sufficient to set the parameters in the ```src/Experiment.java``` file and run the same file.


#### Repository structure ####
```
Data-Driven Norm Revision
│   README.md                               
│
└───lib                 # Required libraries
│       TraaS.jar       # The TraaS library
│
└───output              # Folder that will contain the output of the experiments
│   └───results         # Results reported in the paper                     
│   
└───src                 # The source code of the highway SUMO simulation, and of DNR
│   Experiment.java     # Main class to run experiments
│   └───dnr             # Package containing the class of the DNR (Data-Driven Norm Revision) module
│   └───masobjeval      # Package containing the class of the MAS Objectives Evaluator
│   └───simulation      # Package containing the classes to run the highway SUMO simulation with norm-aware agents
└───utils               # Utilities for the experiments
```

### Who do I talk to? ###

* Dr. D. Dell'Anna [d.dellanna@uu.nl](mailto:d.dellanna@uu.nl)
