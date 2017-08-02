

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Random;
import java.util.Scanner;
import java.text.SimpleDateFormat;

import jsc.independentsamples.MannWhitneyTest;
import org.apache.commons.math3.stat.inference.MannWhitneyUTest;




public class PSO {


	// ****************  MISCELLANEOUS	  ******************

	// for random numbers
	public static boolean useSeedForRand;
	public static int seed;   
	public static Random rand;

	// debugging toggles
	public static boolean regularDebug = false;
	public static boolean nBestDebug = false;

	public static final int DEBUG_RUN_NUM = 1;
	public static int debugParticleID = 1;

	// ****************  PSO   ******************

	public static int totalNumRuns;
	public static int currentRunNum;

	// iterations are counted in terms of function evaluations (FEs)
	// to facilitate runs where we are counting those instead of iterations
	public static int totalNumIters;
	public static int currentIterNum;
	public static int numItersPerOutputInterval;
	public static int numInitialFEsIgnored;

	// are we counting iterations or function evaluations?
	public static boolean useIterations;

	public static int totalNumFEs;
	public static int currentFENum;
	public static int numFEsPerOutputInterval;
	public static int numFEIntervals;

	public static int numIntervalsForDataOutput;

	//	public static DoubleVector cumMinDimValues;
	//	public static DoubleVector cumMaxDimValues;


	// topologies considered
	public static enum Topology {
		GBEST, RING, vonNEUMANN, MOORE, FLOCK
	}
	public static int[] numRowsVonNeumannAndMooreList;
	public static int[] numColsVonNeumannAndMooreList;

	// include self?
	public static enum SelfModel {
		INCLUDE_SELF, NOT_INCLUDE_SELF
	}

	// who has influence in the neighborhood?
	public static enum InfluenceModel {
		NEIGH_BEST, FIPS
	}


	// the usual PSO parameters
	//	public static double neighborhoodTheta;
	//	public static double personalTheta;
	public static double nBestTheta;
	public static double pBestTheta;
	public static double theta;  
	public static double constrictionFactor;
	
	private static double pValue = -999.0;


	// ****************  MG-PSO   ******************


	public static boolean usingSPSO = false;

	// data files
	// a summary of all the runs
	public static boolean summaryDataFileNeeded; 
	// min, max, and quartiles info for the intervals
	public static boolean intervalDataFileNeeded;
	// final function values for Mann-Whitney calculation
	public static boolean runsDataFileNeeded;


	//	public static int valueChangeWindow;




	//
	//	private static void setSPSOParameters() {
	//
	//		usingSingleMemory = false;
	//		singleMemoryTheta = 0.0;
	//
	//		usingGBest = false;			
	//		gBestTheta = 0.0;
	//
	//		usingGBestMemory = false;
	//		gBestMemoryTheta = 0.0;
	//
	//		usingNBest = true;			
	//		nBestTheta = 2.05;
	//
	//		usingNBestMemory = false;
	//		nBestMemoryTheta = 0.0;
	//
	//		usingPBest = true;
	//		pBestTheta = 2.05;
	//
	//		usingPBestMemory = false;
	//		pBestMemoryTheta = 0.0;
	//
	//		theta = 4.1;
	//		constrictionFactor = 2.0 / (theta - 2.0 + Math.sqrt(theta*theta - 4.0*theta));
	//
	//	}
	//
	//
	//
	//	private static void setMGPSOParameters() {
	//
	//		usingSingleMemory = false;
	//		singleMemoryTheta = 0.0;
	//
	//		usingGBest = false;			
	//		gBestTheta = 0.0;
	//
	//		usingGBestMemory = false;
	//		gBestMemoryTheta = 0.0;
	//
	//		usingNBest = true;			
	//		nBestTheta = 2.05;
	//
	//		usingNBestMemory = false;
	//		nBestMemoryTheta = 0.0;
	//
	//		usingPBest = true;
	//		pBestTheta = 1.025; 
	//
	//		usingPBestMemory = true;
	//		pBestMemoryTheta = 1.025; 
	//
	//		theta = 4.1;  
	//		constrictionFactor = 2.0 / (theta - 2.0 + Math.sqrt(theta*theta - 4.0*theta));
	//
	//
	//		// *****************************	
	//
	//		usingHybrid = false;
	//
	//		usingDiscounting = false;
	//		discountFactor = 1.0;
	//
	//
	//		//			fitnessProportionalSelectMinDistFromBestPosition = 0.0;//3.0;
	//		//			fitnessProportionalSelectMaxMultipleOfBestValue = Double.MAX_VALUE;
	//
	//		//			valueChangeWindow = 10;
	//
	//		// ********************************************************************************
	//
	//
	//	}

	private GPTree gpTree;

	public PSO(GPTree gpTree) {
		this.gpTree = gpTree;
	}
	

	public double evalGPTree(int numRuns) {
		
		try {

			// create a PrintWriter that writes to System.out so I can use methods that
			// write to a file to write to the output window; gets closed at the end of
			// this (main) method
			PrintWriter outputWindow = new PrintWriter(System.out);

			// create date string for screen output and output file names
			SimpleDateFormat dateformatter = new SimpleDateFormat("yyyy-MM-dd--hh-mm-ss-a");
			Calendar date = Calendar.getInstance();
			String dateString = dateformatter.format(date.getTime());

			// to output window
//			outputWindow.println("RUNNING CODE ON " + dateString + "\n");	


			// this has never been used!!
			useSeedForRand = false;
			seed = 8778;   
			rand = useSeedForRand? new Random(seed): new Random();


			// standard PSO parameters
			nBestTheta = 2.05;
			pBestTheta = 2.05;
			theta = nBestTheta + pBestTheta;  
			constrictionFactor = 2.0 / (theta - 2.0 + Math.sqrt(theta*theta - 4.0*theta));


			// how many runs?
			totalNumRuns = numRuns;  


			// FUNCTION
			// NOTE: the location of the optimum is shifted by a random amount that is a fraction of the 
			// distance from the optimum location to the edge of the search space.  in the case that the 
			// optimum location is not the same distance from both edges, it is a fraction of the shorter 
			// distance
			//			int numTestFunctions = TestFunctions.NUMBER_OPT_FUNCTIONS;
			boolean doSchwefel = false;
			boolean doRastrigin = true;  
			boolean doAckley = false;     
			boolean doGriewank =  false;   
			boolean doPenalFunc1 = false;   
			boolean doPenalFunc2 = false;   
			boolean doSphere = false;
			boolean doRosenbrock = false;    
			boolean[] doFunction = { doSchwefel, doRastrigin, doAckley, doGriewank, doPenalFunc1, doPenalFunc2, doSphere, doRosenbrock };


			// PSO TOPOLOGY
			Topology[] topologiesList = { Topology.GBEST, Topology.RING, Topology.vonNEUMANN, Topology.MOORE, Topology.FLOCK};
			boolean doGBEST = true;
			boolean doRING = false;
			boolean doVonNEUMANN = false;
			boolean doMOORE =  false;			
			boolean doFLOCK = true;		
			boolean[] doTopology = { doGBEST, doRING, doVonNEUMANN, doMOORE, doFLOCK };

			// SELF MODEL
			SelfModel[] selfModelsList = { SelfModel.INCLUDE_SELF, SelfModel.NOT_INCLUDE_SELF };
			boolean doINCLUDE_SELF = true;
			boolean doNOT_INCLUDE_SELF = false;
			boolean[] doSelfModel = { doINCLUDE_SELF, doNOT_INCLUDE_SELF };

			// INFLUENCE MODEL
			InfluenceModel[] influenceModelsList = { InfluenceModel.NEIGH_BEST, InfluenceModel.FIPS };
			boolean doNEIGH_BEST = true;
			boolean doFIPS = false;
			boolean[] doInfluenceModel = { doNEIGH_BEST, doFIPS };


//			// MAX SPEED
//			double[] maxSpeedList = { 5, 10, 15, 20, 25, 30, 35, 40, 45, 50 };
//			boolean doMaxSpeed1 = false;  
//			boolean doMaxSpeed2 = false;  
//			boolean doMaxSpeed3 = false;  
//			boolean doMaxSpeed4 = false;  
//			boolean doMaxSpeed5 = true;  
//			boolean doMaxSpeed6 = false;  
//			boolean doMaxSpeed7 = false;  
//			boolean doMaxSpeed8 = false;  
//			boolean doMaxSpeed9 = false;  
//			boolean doMaxSpeed10 = false;  
//			boolean[] doMaxSpeeds = { doMaxSpeed1, doMaxSpeed2, doMaxSpeed3, doMaxSpeed4, doMaxSpeed5,
//					doMaxSpeed6, doMaxSpeed7, doMaxSpeed8, doMaxSpeed9, doMaxSpeed10 };
//
//			// NORMAL SPEED
//			double[] normalSpeedList = { 5, 10, 15, 20, 25, 30, 35, 40, 45, 50 };
//			boolean doNormalSpeed1 = false;  
//			boolean doNormalSpeed2 = false;  
//			boolean doNormalSpeed3 = false;  
//			boolean doNormalSpeed4 = false;  
//			boolean doNormalSpeed5 = true;  
//			boolean doNormalSpeed6 = false;  
//			boolean doNormalSpeed7 = false;  
//			boolean doNormalSpeed8 = false;  
//			boolean doNormalSpeed9 = false;  
//			boolean doNormalSpeed10 = false;  
//			boolean[] doNormalSpeeds = { doNormalSpeed1, doNormalSpeed2, doNormalSpeed3, doNormalSpeed4, doNormalSpeed5,
//					doNormalSpeed6, doNormalSpeed7, doNormalSpeed8, doNormalSpeed9, doNormalSpeed10 };
//
//			// NEIGHBORHOOD RADIUS
//			double[] neighRadiusList = { 5, 10, 15, 20, 25, 30, 35, 40, 45, 50 };
//			boolean doNeighRadius1 = false;  
//			boolean doNeighRadius2 = false;  
//			boolean doNeighRadius3 = true;  
//			boolean doNeighRadius4 = false;  
//			boolean doNeighRadius5 = false;  
//			boolean doNeighRadius6 = false;  
//			boolean doNeighRadius7 = false;  
//			boolean doNeighRadius8 = false;  
//			boolean doNeighRadius9 = false;  
//			boolean doNeighRadius10 = false;  
//			boolean[] doNeighRadii = { doNeighRadius1, doNeighRadius2, doNeighRadius3, doNeighRadius4, doNeighRadius5,
//					doNeighRadius6, doNeighRadius7, doNeighRadius8, doNeighRadius9, doNeighRadius10 };
//
//			// SEPARATION
//			double[] separationList = { 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 };
//			boolean doSeparation1 = false;  
//			boolean doSeparation2 = false;  
//			boolean doSeparation3 = false;  
//			boolean doSeparation4 = false;  
//			boolean doSeparation5 = true;  
//			boolean doSeparation6 = false;  
//			boolean doSeparation7 = false;  
//			boolean doSeparation8 = false;  
//			boolean doSeparation9 = false;  
//			boolean doSeparation10 = false;  
//			boolean[] doSeparations = { doSeparation1, doSeparation2, doSeparation3, doSeparation4, doSeparation5,
//					doSeparation6, doSeparation7, doSeparation8, doSeparation9, doSeparation10 };
//
//			// COHESION
//			double[] cohesionList = { 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0 };
//			boolean doCohesion1 = false;  
//			boolean doCohesion2 = false;  
//			boolean doCohesion3 = false;  
//			boolean doCohesion4 = false;  
//			boolean doCohesion5 = true;  
//			boolean doCohesion6 = false;  
//			boolean doCohesion7 = false;  
//			boolean doCohesion8 = false;  
//			boolean doCohesion9 = false;  
//			boolean doCohesion10 = false;  
//			boolean[] doCohesions = { doCohesion1, doCohesion2, doCohesion3, doCohesion4, doCohesion5,
//					doCohesion6, doCohesion7, doCohesion8, doCohesion9, doCohesion10 };
//
//			// ALIGNMENT
//			double[] alignmentList = { 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0 };
//			boolean doAlign1 = false;  
//			boolean doAlign2 = false;  
//			boolean doAlign3 = false;  
//			boolean doAlign4 = false;  
//			boolean doAlign5 = true;  
//			boolean doAlign6 = false;  
//			boolean doAlign7 = false;  
//			boolean doAlign8 = false;  
//			boolean doAlign9 = false;  
//			boolean doAlign10 = false;  
//			boolean[] doAlignments = { doAlign1, doAlign2, doAlign3, doAlign4, doAlign5,
//					doAlign6, doAlign7, doAlign8, doAlign9, doAlign10 };
//
//			// PACEKEEPING
//			double[] pacekeepingList = { 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0 };
//			boolean doPace1 = false;  
//			boolean doPace2 = false;  
//			boolean doPace3 = false;  
//			boolean doPace4 = false;  
//			boolean doPace5 = true;  
//			boolean doPace6 = false;  
//			boolean doPace7 = false;  
//			boolean doPace8 = false;  
//			boolean doPace9 = false;  
//			boolean doPace10 = false;  
//			boolean[] doPacekeepings = { doPace1, doPace2, doPace3, doPace4, doPace5,
//					doPace6, doPace7, doPace8, doPace9, doPace10 };
//
//			// RANDOM MOTION PROBABILITY
//			double[] randomProbList = { 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0 };
//			boolean doRandProb1 = false;  
//			boolean doRandProb2 = false;  
//			boolean doRandProb3 = false;  
//			boolean doRandProb4 = false;  
//			boolean doRandProb5 = true;  
//			boolean doRandProb6 = false;  
//			boolean doRandProb7 = false;  
//			boolean doRandProb8 = false;  
//			boolean doRandProb9 = false;  
//			boolean doRandProb10 = false;  
//			boolean[] doRandProbs = { doRandProb1, doRandProb2, doRandProb3, doRandProb4, doRandProb5,
//					doRandProb6, doRandProb7, doRandProb8, doRandProb9, doRandProb10 };



			// DIMENSIONS
			int[] numDimsList = { 3, 10, 30, 50 };
			//			int numNumDims = numDimsList.length;
			boolean doNumDims1 = false;  
			boolean doNumDims2 = false;   
			boolean doNumDims3 = true;  
			boolean doNumDims4 = false;  
			boolean[] doNumDims = { doNumDims1, doNumDims2, doNumDims3, doNumDims4 };


			// NUM PARTICLES
			int[] numParticlesList = { 4, 10, 20, 30, 40, 50, 60, 70 }; 
			//			int numParticleLevels = numParticlesList.length;
			boolean doNumParticles1 = false;   
			boolean doNumParticles2 = false;   
			boolean doNumParticles3 = false;   
			boolean doNumParticles4 = true;    
			boolean doNumParticles5 = false;    
			boolean doNumParticles6 = false;    
			boolean doNumParticles7 = false;    
			boolean doNumParticles8 = false;    
			boolean[] doNumParticles = { doNumParticles1, doNumParticles2, doNumParticles3, doNumParticles4, doNumParticles5, 
					doNumParticles6, doNumParticles7, doNumParticles8 };

			// we need to specify the number of rows and columns for the von Neumann
			// and Moore neighborhoods for each number of particles
			numRowsVonNeumannAndMooreList = new int[numParticlesList.length];
			numColsVonNeumannAndMooreList = new int[numParticlesList.length];

			numRowsVonNeumannAndMooreList[0] = 2;
			numColsVonNeumannAndMooreList[0] = 2;

			numRowsVonNeumannAndMooreList[1] = 2;
			numColsVonNeumannAndMooreList[1] = 5;

			numRowsVonNeumannAndMooreList[2] = 4;
			numColsVonNeumannAndMooreList[2] = 5;

			numRowsVonNeumannAndMooreList[3] = 5;
			numColsVonNeumannAndMooreList[3] = 6;

			numRowsVonNeumannAndMooreList[4] = 5;
			numColsVonNeumannAndMooreList[4] = 8;

			numRowsVonNeumannAndMooreList[5] = 5;
			numColsVonNeumannAndMooreList[5] = 10;

			numRowsVonNeumannAndMooreList[6] = 6;
			numColsVonNeumannAndMooreList[6] = 10;

			numRowsVonNeumannAndMooreList[7] = 7;
			numColsVonNeumannAndMooreList[7] = 10;


			// NUM ITERATIONS AND NUM FUNCTION EVALUATIONS
			// this is were we start dealing with the fact that we might be counting iterations,
			// or we might be counting function evaluations; we need to have the ability to specify both
			useIterations = true;
			if (useIterations) {
				numItersPerOutputInterval = 100;
			}
			else {
				numFEsPerOutputInterval = 200;
			}

			// NUM ITERATIONS
			int[] numIterationsList = { 10000, 30000, 50000, 100000, 200000 }; 
			boolean doNumIterations1 = true;       //  10,000
			boolean doNumIterations2 = false;      //  30,000
			boolean doNumIterations3 = false;      //  50,000
			boolean doNumIterations4 = false;      // 100,000
			boolean doNumIterations5 = false;      // 200,000
			boolean[] doNumIterations = { doNumIterations1, doNumIterations2, doNumIterations3, doNumIterations4, doNumIterations5 };

			// NUM FUNCTION EVALUATIONS
			int[] numFEsList = { 10000, 30000, 50000, 100000, 200000 }; 
			boolean doNumFEs1 = false;      //  10,000
			boolean doNumFEs2 = false;      //  30,000
			boolean doNumFEs3 = false;      //  50,000
			boolean doNumFEs4 = false;      // 100,000
			boolean doNumFEs5 = false;      // 200,000
			boolean[] doNumFEs = { doNumFEs1, doNumFEs2, doNumFEs3, doNumFEs4, doNumFEs5 };


			// what files are needed
			summaryDataFileNeeded = true;    
			runsDataFileNeeded = true;    // for Mann Whitney tests
			intervalDataFileNeeded = true;
			PrintWriter intervalDataFile = null;
			// where to put them
			// NOTE: this folder must already exist!
			String folder = "results/test2/";

			//			// huge array to hold final function values for all runs for Mann Whitney tests
			//			double[][][][][][][][][][][][][] finalFuncValues = new double[0][0][0][0][0][0][0][0][0][0][0][0][0];
			//			if (runsDataFileNeeded) {
			//				finalFuncValues = 
			//						new double[numTestFunctions][numParticleLevels][numTopos][numSelfOptions][numInfluenceModels][numSwitchOverPoints]
			//								[numSingleMemSizes][numGBestMemSizes][numNBestMemSizes][numPBestMemSizes][numMemSelectMethods][numMemRemoveMethods][totalNumRuns];        
			//			}


			// Here's where we start all the nested for-loops to run all the parameter combinations specified above


			// TEST FUNCTIONS
			// ==============
			for (int functionNum = 0 ; functionNum < doFunction.length ; ++functionNum) {

				if (!doFunction[functionNum])
					continue;

				PrintWriter summaryDataFile = null;
				if (summaryDataFileNeeded) {
					String summaryDataFilename = folder + "SUMMARY-DATA-" + TestFunctions.getShortFunctionName(functionNum) + "-" + dateString + ".txt";
					summaryDataFile = new PrintWriter(new FileWriter(summaryDataFilename ));
					summaryDataFile.println(dateString);
					summaryDataFile.println();
				}


				PrintWriter runsDataFile = null;
				String runsDataFileName = folder + "ALL-RUN-DATA-" + TestFunctions.getShortFunctionName(functionNum) + "-" + dateString + ".txt";
				if (runsDataFileNeeded) {
					runsDataFile = new PrintWriter(new FileWriter(runsDataFileName));
					runsDataFile.println(dateString);
					runsDataFile.println();
				}


				// TOPOLOGY
				// ========
				for (int topoIndex = 0 ; topoIndex < doTopology.length ; ++topoIndex) {

					if (!doTopology[topoIndex])
						continue;

					Topology currentPSOTopology = topologiesList[topoIndex]; 

					if (isSPSO(currentPSOTopology)) {   
						usingSPSO = true;
					}
					else {
						usingSPSO = false;
					}



					// INCLUDING SELF?
					// ===============
					for (int selfIndex = 0 ; selfIndex < doSelfModel.length ; ++selfIndex) {

						if (!doSelfModel[selfIndex])
							continue;

						SelfModel currentPSOSelfModel =  selfModelsList[selfIndex]; 



						// INFLUENCE MODEL
						// ===============
						for (int influenceIndex = 0 ; influenceIndex < doInfluenceModel.length ; ++influenceIndex) {

							if (!doInfluenceModel[influenceIndex])
								continue;

							InfluenceModel currentPSOInfluenceModel = influenceModelsList[influenceIndex]; 



							// # DIMENSIONS
							// ============
							for (int numDimensionsIndex = 0 ; numDimensionsIndex < doNumDims.length ; ++numDimensionsIndex ) {

								if (!doNumDims[numDimensionsIndex])
									continue;

								int currentNumDimensions = numDimsList[numDimensionsIndex];



								// # PARTICLES
								// ===========
								for (int numParticlesIndex = 0 ; numParticlesIndex < doNumParticles.length ; ++numParticlesIndex ) {

									if (!doNumParticles[numParticlesIndex])
										continue;

									int currentNumParticles = numParticlesList[numParticlesIndex];



									// # ITERATIONS
									// ============
									int numIterationsIndex;
									int numFEsIndex;
									for (numIterationsIndex = 0, numFEsIndex = 0 ; 
											numIterationsIndex < doNumIterations.length && numFEsIndex < doNumFEs.length ; 
											++numIterationsIndex, ++numFEsIndex ) {
										// the second condition = if we're using iterations we don't need to do a run for every level of FEs  ??????????????????????????

										// if using iterations
										if (useIterations && !doNumIterations[numIterationsIndex])
											continue;

										// if using function evaluations
										if (!useIterations && !doNumFEs[numFEsIndex])
											continue;

										currentFENum = 0;
										currentIterNum = 0;

										// this puts everything into number of FEs terms so we can just use that for iteration control
										// and data output control
										if (useIterations) {
											totalNumIters = numIterationsList[numIterationsIndex];

											// to get that many iterations (not counting the numParticles FEs used to evaluate the
											// particles when the swarm is created, i.e. iteration 0), we need an additional numParticles FEs
											totalNumFEs = (totalNumIters * currentNumParticles) + currentNumParticles;

											// numFEsPerIntervalForOutput needs to be calculated based on numIterationsPerIntervalForOutput
											numFEsPerOutputInterval = currentNumParticles * numItersPerOutputInterval;
											// we need this because the first numParticles FEs are in iteration 0 and shouldn't be counted 
											// for *iterations* reporting
											numInitialFEsIgnored = currentNumParticles;

											numIntervalsForDataOutput = totalNumIters / numItersPerOutputInterval;

										}

										else {
											totalNumFEs = numFEsList[numFEsIndex];

											// numFEsPerIntervalForOutput does not need to be calculated since it is assigned
											// a value above where we indicate whether to use iterations or FEs

											// no initial FEs are ignored because, unlike iterations, where we don't start counting iterations
											// until after the swarm has been created and numParticles FEs have been used in the process,
											// here we start counting FEs right away
											numInitialFEsIgnored = 0;
											numIntervalsForDataOutput = totalNumFEs / numFEsPerOutputInterval;
											// would not expect the following to ever be an issue since we are setting both 
											// totalNumFunctionEvaluations and numFEsPerIntervalForOutput, and it is unlikely
											// that we would set them so that totalNumFunctionEvaluations is not a multiple of 
											// numFEsPerIntervalForOutput, but if that is the case we would need an extra interval
											// for data output to take care of the "extra" FEs
											if (totalNumFEs % numFEsPerOutputInterval != 0)
												++numIntervalsForDataOutput;
										}


										// create the DataOutput objects once and for all and
										// just copy the values/errors in the globalBests returned by update into these 
										// DataOutput objects
										//
										// NOTE: we need to add 1 to numIntervalsForDataOutput because: 
										//       whether we are doing output by # of iterations or output by # of FEs, 
										//       the output with index 0,
										//       which is right after the swarm is created if counting iterations, 
										//           or before the first FE if counting FEs 
										//       is not wanted
										// perhaps a better way to look at it is that output interval indices are *not* 
										// zero-based, so we need that extra (useless) item in the index 0 spot
										DataOutput[][] intervalData = new DataOutput[numIntervalsForDataOutput+1][totalNumRuns];
										for (int i = 0 ; i < intervalData.length ; ++i) {
											for (int r = 0 ; r < totalNumRuns ; ++r) {
												intervalData[i][r] = new DataOutput();
											}
										}
										// can create the IntervalSummaryData objects once and for all because we will just be
										// recalculating their contents after every series of runs
										IntervalSummaryData[] intervalSummaryData = new IntervalSummaryData[numIntervalsForDataOutput+1];
										for (int i = 0 ; i < intervalSummaryData.length ; ++i) {
											intervalSummaryData[i] = new IntervalSummaryData();
										}


//										// # MAXIMUM SPEED
//										// ===============
//										for (int maxSpeedIndex = 0 ; maxSpeedIndex < doMaxSpeeds.length ; ++maxSpeedIndex ) {
//
//											if (!doMaxSpeeds[maxSpeedIndex])
//												continue;
//
//											double currentMaxSpeed = maxSpeedList[maxSpeedIndex];
//
//
//											// # NORMAL SPEED
//											// ==============
//											for (int normalSpeedIndex = 0 ; normalSpeedIndex < doNormalSpeeds.length ; ++normalSpeedIndex ) {
//
//												if (!doNormalSpeeds[normalSpeedIndex])
//													continue;
//
//												double currentNormalSpeed = normalSpeedList[normalSpeedIndex];
//
//
//												// # NEIGHBORHOOD RADIUS
//												// =====================
//												for (int neighRadiusIndex = 0 ; neighRadiusIndex < doNeighRadii.length ; ++neighRadiusIndex ) {
//
//													if (!doNeighRadii[neighRadiusIndex])
//														continue;
//
//													double currentNeighRadius = neighRadiusList[neighRadiusIndex];
//
//
//													// # SEPARATION
//													// ============
//													for (int separationIndex = 0 ; separationIndex < doSeparations.length ; ++separationIndex ) {
//
//														if (!doSeparations[separationIndex])
//															continue;
//
//														double currentSeparationWeight = separationList[separationIndex];
//
//
//														// # COHESION
//														// ==========
//														for (int cohesionIndex = 0 ; cohesionIndex < doCohesions.length ; ++cohesionIndex ) {
//
//															if (!doCohesions[cohesionIndex])
//																continue;
//
//															double currentCohesionWeight = cohesionList[cohesionIndex];
//
//
//															// # ALIGNMENT
//															// ===========
//															for (int alignIndex = 0 ; alignIndex < doAlignments.length ; ++alignIndex ) {
//
//																if (!doAlignments[alignIndex])
//																	continue;
//
//																double currentAlignmentWeight = alignmentList[alignIndex];
//
//
//																// # PACEKEEPING
//																// =============
//																for (int paceIndex = 0 ; paceIndex < doPacekeepings.length ; ++paceIndex ) {
//
//																	if (!doPacekeepings[paceIndex])
//																		continue;
//
//																	double currentPacekeepingWeight = pacekeepingList[paceIndex];
//
//
//																	// # RANDOM MOTION PROBABILITY
//																	// ===========================
//																	for (int randProbIndex = 0 ; randProbIndex < doRandProbs.length ; ++randProbIndex ) {
//
//																		if (!doRandProbs[randProbIndex])
//																			continue;
//
//																		double currentRandMotionProb = randomProbList[randProbIndex];





																		if (runsDataFileNeeded) {
																			runsDataFile.println();
																			runsDataFile.println();
																			runsDataFile.println();

																			outputTypeOfTest(runsDataFile);	

//																			outputTestParameters(runsDataFile, currentPSOTopology, currentPSOSelfModel, currentPSOInfluenceModel, 
//																					currentMaxSpeed, currentNormalSpeed, currentNeighRadius, currentSeparationWeight,
//																					currentCohesionWeight, currentAlignmentWeight, currentPacekeepingWeight, currentRandMotionProb,
//																					functionNum, currentNumDimensions, currentNumParticles, 
//																					totalNumRuns, totalNumFEs, totalNumIters); 

																			outputTestParameters(runsDataFile, currentPSOTopology, currentPSOSelfModel, currentPSOInfluenceModel, 
																					functionNum, currentNumDimensions, currentNumParticles, 
																					totalNumRuns, totalNumFEs, totalNumIters); 
																			
																			runsDataFile.println();
																		}



																		// this will put EACH CASE:
																		//	 1) topology
																		//   2) self model
																		//   3) influence model
																		//   4) function
																		//   5) # dimensions 
																		//   6) # particles
																		//   7) # iterations
																		// in its own file
																		if (intervalDataFileNeeded) {

																			//																			String caseDescription = getCaseDescriptionStringNoInfluenceModel(currentPSOTopology, currentPSOSelfModel, 
																			//																					currentPSOInfluenceModel);																			
																			String caseDescription = getCaseDescriptionString(currentPSOTopology, currentPSOSelfModel, 
																					currentPSOInfluenceModel);

//																			String typeOfTest = getTypeOfTestString(functionNum, currentNumDimensions, currentNumParticles, totalNumRuns, totalNumIters, 
//																					currentMaxSpeed, currentNormalSpeed, currentNeighRadius, currentSeparationWeight,
//																					currentCohesionWeight, currentAlignmentWeight, currentPacekeepingWeight, currentRandMotionProb);

																			String typeOfTest = getTypeOfTestString(functionNum, currentNumDimensions, currentNumParticles, totalNumRuns, totalNumIters);
																			
																			String intervalDataFileName = folder + "INTERVAL-DATA-" + TestFunctions.getShortFunctionName(functionNum) + 
																					//																					"-d" + currentNumDimensions + 
																					//																					"-p" + currentNumParticles + 
																					//																					"-it" + totalNumIters + 
																					"-" + caseDescription + "-" + typeOfTest +
																					".txt";

																			intervalDataFile = new PrintWriter(new FileWriter(intervalDataFileName));
																			intervalDataFile.println("# " + dateString);
																			intervalDataFile.println("# ");


																			outputTypeOfTest(intervalDataFile);	

//																			outputTestParameters(intervalDataFile, currentPSOTopology, currentPSOSelfModel, currentPSOInfluenceModel, 
//																					currentMaxSpeed, currentNormalSpeed, currentNeighRadius, currentSeparationWeight,
//																					currentCohesionWeight, currentAlignmentWeight, currentPacekeepingWeight, currentRandMotionProb,
//																					functionNum, currentNumDimensions, currentNumParticles, 
//																					totalNumRuns, totalNumFEs, totalNumIters); 	
																			
																			outputTestParameters(intervalDataFile, currentPSOTopology, currentPSOSelfModel, currentPSOInfluenceModel, 
																					functionNum, currentNumDimensions, currentNumParticles, 
																					totalNumRuns, totalNumFEs, totalNumIters); 

																			//												iterationOutputFileNames[currentFunctionNum]
																			//														[numParticlesIndex]
																			//																[topoIndex]
																			//																		[selfIndex]
																			//																				[influenceIndex] = "iter-data-" + fileName;
																		}



																		// haven't been too interested in this yet....
																		long startTimeAllRuns = System.currentTimeMillis();  


																		double numSuccesses = 0.0;
																		double sumDistancesToOptimum = 0.0;


																		double[] finalFunctionValues = new double[totalNumRuns];
																		double[] finalDistanceValues = new double[totalNumRuns];


																		// for calculating distance from optimum
																		DoubleVector shiftedOptimumLocation = 
																				new DoubleVector(currentNumDimensions, TestFunctions.OPT_COORD[functionNum]);



																		//																		double sumCumBoundingBoxVolumes = 0.0;

																		// RUNS
																		// ====
																		for (currentRunNum = 0 ; currentRunNum < totalNumRuns ; ++currentRunNum) {

																			if (regularDebug || nBestDebug)
																				System.out.println("\n*** RUN NUM *** = " + currentRunNum);


																			//																			// reset the vectors keeping track of the min and max of the dimensions
																			//																			cumMinDimValues = new DoubleVector(currentNumDimensions, Double.MAX_VALUE);
																			//																			cumMaxDimValues = new DoubleVector(currentNumDimensions, -Double.MAX_VALUE);


																			// for each run generate a random shift of the location of the optimum in that function's search space
																			double shiftVectorAmount = TestFunctions.SHIFT_RANGE[functionNum] * rand.nextDouble();
																			if (rand.nextDouble() < 0.5) 
																				shiftVectorAmount *= -1.0;

																			// finally, we can set the function parameters because we know the shift for that run
																			//																			DoubleVector shiftVector = new DoubleVector(currentNumDimensions, shiftVectorAmount);
																			TestFunctions.setShiftVector(functionNum, currentNumDimensions, shiftVectorAmount);

																			shiftedOptimumLocation.setAll(TestFunctions.OPT_COORD[functionNum]);
																			shiftedOptimumLocation.addScalar(shiftVectorAmount);


																			// initialize currentFENum so that FEs that occur in creating the swarm will be counted
																			currentFENum = 0;
																			// also initialize currentIterNum
																			currentIterNum = 0;

																			// this can't be done before the shiftVectorAmount is set because that gets shown,
																			// but it can't be done after a run because, if we are doing a hybrid run, the 
																			// theta parameters (and possibly others) will have been reset to s-pso values,
																			// so show the type of test only on the DEBUG_RUN_NUM 
																			if (regularDebug && currentRunNum == DEBUG_RUN_NUM) {
//																				outputTypeOfTest(outputWindow);	
																			}


																			// create the swarm
																			// numParticlesIndex is sent because it needs to be used in the creation of the vonNeumann
																			// and Moore neighborhoods to access the arrays above that indicate the number of rows/cols
//																			Swarm swarm = new Swarm (currentNumParticles, numParticlesIndex, functionNum, currentNumDimensions, 
//																					currentPSOTopology, currentPSOSelfModel, currentPSOInfluenceModel, 
//																					currentMaxSpeed, currentNormalSpeed, currentNeighRadius, currentSeparationWeight,
//																					currentCohesionWeight, currentAlignmentWeight, currentPacekeepingWeight, currentRandMotionProb,
//																					intervalData);

																			Swarm swarm = new Swarm (currentNumParticles, numParticlesIndex, functionNum, currentNumDimensions, 
																					currentPSOTopology, currentPSOSelfModel, currentPSOInfluenceModel, 
																					intervalData, gpTree);
																			
																			
																			// ITERATIONS
																			// ==========
																			// - we are going solely by the number of FEs used;
																			// - we calculate the number of FEs we need to get the number of iterations we want, and stop when we have used those up;
																			// - so we are only keeping track of iterations for the purpose of things like deciding when to restructure a 
																			//		dynamic topology (which is not part of the Basic-PSO code, but will be added in subsequent projects);
																			// - NOTE: currentFunctionEvaluationNum is initialized to 0 above before the swarm is created because some FEs
																			//         will be used when the particles are created and evaluated, and it is incremented in TestFunctions 
																			//         when the function is evaluated in the evalWithError method

																			for ( ; currentFENum < totalNumFEs ; ) {

																				// one round of asynchronous updates is an iteration;
																				// currentIterationNum starts at 0, so needs to be incremented *before* the call to asynchronousUpdate
																				++currentIterNum;

																				//																				if (!usingSPSO && usingHybrid && currentIterNum >= switchOverIteration) {
																				//																					switchedToSPSO = true;
																				//																					setSPSOParameters();
																				//																				}

																				swarm.update(functionNum, currentPSOTopology, currentPSOSelfModel, currentPSOInfluenceModel, 
																						intervalData);

																			}  // END ITERATIONS FOR-LOOP 


																			//																			if (switchedToSPSO) {   
																			//																				setMGPSOParameters();
																			//																				usingSPSO = false;
																			//																			}


																			double finalFuncValue = Swarm.globalBest.getFunctionValue();

																			//																	System.out.println("currentFunctionNum = " + currentFunctionNum);
																			//																	System.out.println("numParticlesIndex = " + numParticlesIndex);
																			//																	System.out.println("topoIndex = " + topoIndex);
																			//																	System.out.println("selfIndex = " + selfIndex);
																			//																	System.out.println("influenceIndex = " + influenceIndex);
																			//																	System.out.println("singleMemSizeIndex = " + singleMemSizeIndex);
																			//																	System.out.println("gBestMemSizeIndex = " + gBestMemSizeIndex);
																			//																	System.out.println("nBestMemSizeIndex = " + nBestMemSizeIndex);
																			//																	System.out.println("pBestMemSizeIndex = " + pBestMemSizeIndex);
																			//																	System.out.println("memSelectIndex = " + memSelectIndex);
																			//																	System.out.println("memRemoveIndex = " + memRemoveIndex);
																			//																	System.out.println("currentRunNum = " + currentRunNum);

																			//																		if (runsDataFileNeeded) {
																			//
																			//																			finalFuncValues[functionNum]
																			//																					[numParticlesIndex]
																			//																							[topoIndex]
																			//																									[selfIndex]
																			//																											[influenceIndex]
																			//																													[switchOverIndex]
																			//																															[singleMemSizeIndex]
																			//																																	[gBestMemSizeIndex]
																			//																																			[nBestMemSizeIndex]
																			//																																					[pBestMemSizeIndex]
																			//																																							[memSelectIndex]
																			//																																									[memRemoveIndex]
																			//																																											[currentRunNum] = finalFuncValue;
																			//																		}

																			// final best function values
																			if (runsDataFileNeeded) {
																				finalFunctionValues[currentRunNum] = finalFuncValue;
																			}

																			// successful?
																			if (finalFuncValue <= TestFunctions.SUCCESS_CRITERION[functionNum]) {
																				++numSuccesses;
																			}

																			// distance from optimum
																			//																			DoubleVector actualOptimumLocation = 
																			//																					new DoubleVector(currentNumDimensions, TestFunctions.OPT_COORD[functionNum]);
																			//																			DoubleVector shiftVector = 
																			//																					new DoubleVector(currentNumDimensions, shiftVectorAmount);
																			//																			DoubleVector shiftedOptimumLocation = DoubleVector.add(actualOptimumLocation, shiftVector);
																			double distanceFromOptimum = 
																					Swarm.globalBest.getPosition().distance(shiftedOptimumLocation);
																			sumDistancesToOptimum += distanceFromOptimum;

																			// bounding box volume
																			//																			double cumBoundingBoxVolume = calcCumBoundingBoxVolume();
																			//																			System.out.printf("run %2d: bounding box vol = %e\n", currentRunNum, cumBoundingBoxVolume);
																			//																			sumCumBoundingBoxVolumes += cumBoundingBoxVolume;

																			if (runsDataFileNeeded) {
																				finalDistanceValues[currentRunNum] = distanceFromOptimum;
																			}

																		}  // END RUNS FOR-LOOP 


																		// TIMING STATS
																		long endTimeAllRuns = System.currentTimeMillis();
																		double secondsPerRun = ((endTimeAllRuns - startTimeAllRuns) / 1000.0) / totalNumRuns;


																		// Mann Whitney
																		if (runsDataFileNeeded) {
																			runsDataFile.println("GLOBAL BEST FUNCTION VALUES");
																			for (int r = 0; r < totalNumRuns; ++r)
																				runsDataFile.println(finalFunctionValues[r]);
																			runsDataFile.println();

																			runsDataFile.println("GLOBAL BEST DISTANCE FROM OPTIMUM");
																			for (int r = 0; r < totalNumRuns; ++r)
																				runsDataFile.println(finalDistanceValues[r]);
																			runsDataFile.println();
																		}



																		double successRate = numSuccesses * 100.0 / totalNumRuns;
																		double averageDistanceToOptimum = sumDistancesToOptimum / totalNumRuns;

																		// CALCULATIONS FOR OUTPUT
																		calculateSummaryDataOverRuns(intervalData, intervalSummaryData, functionNum, currentNumDimensions);



																		// SUMMARY DATA OUTPUT TO OUTPUT WINDOW
//																		outputTypeOfTest(outputWindow);	

//																		outputTestParameters(outputWindow, currentPSOTopology, currentPSOSelfModel, currentPSOInfluenceModel, 
//																				currentMaxSpeed, currentNormalSpeed, currentNeighRadius, currentSeparationWeight,
//																				currentCohesionWeight, currentAlignmentWeight, currentPacekeepingWeight, currentRandMotionProb,
//																				functionNum, currentNumDimensions, currentNumParticles, 
//																				totalNumRuns, totalNumFEs, totalNumIters); 		
																		
//																		outputTestParameters(outputWindow, currentPSOTopology, currentPSOSelfModel, currentPSOInfluenceModel, 
//																				functionNum, currentNumDimensions, currentNumParticles, 
//																				totalNumRuns, totalNumFEs, totalNumIters); 
//
//																		outputSummaryData(outputWindow, intervalSummaryData, secondsPerRun, successRate, 
//																				averageDistanceToOptimum, functionNum);								

																		//																		// average bounding box volume
																		//																		double averageCumBoundingBoxVolume = sumCumBoundingBoxVolumes / totalNumRuns;
																		//																		System.out.printf("average bounding box vol = %e\n\n", averageCumBoundingBoxVolume);


																		// SUMMARY DATA OUTPUT TO FILE
																		// NOTE:  the file is not closed here because the summary data for *all* cases is put in this file, 
																		//        so it's not closed until *all* loops are done
																		if (summaryDataFileNeeded) {

																			outputTypeOfTest(summaryDataFile);	

//																			outputTestParameters(summaryDataFile, currentPSOTopology, currentPSOSelfModel, currentPSOInfluenceModel, 
//																					currentMaxSpeed, currentNormalSpeed, currentNeighRadius, currentSeparationWeight,
//																					currentCohesionWeight, currentAlignmentWeight, currentPacekeepingWeight, currentRandMotionProb,
//																					functionNum, currentNumDimensions, currentNumParticles, 
//																					totalNumRuns, totalNumFEs, totalNumIters);  	
																			
																			outputTestParameters(summaryDataFile, currentPSOTopology, currentPSOSelfModel, currentPSOInfluenceModel, 
																					functionNum, currentNumDimensions, currentNumParticles, 
																					totalNumRuns, totalNumFEs, totalNumIters); 

																			outputSummaryData(summaryDataFile, intervalSummaryData, secondsPerRun, successRate, 
																					averageDistanceToOptimum, functionNum);								
																		}


																		if (runsDataFileNeeded) {
																			runsDataFile.println();
																			outputSummaryData(runsDataFile, intervalSummaryData, secondsPerRun, successRate, 
																					averageDistanceToOptimum, functionNum);								
																		}

																		// INTERVAL DATA OUTPUT TO FILE
																		if (intervalDataFileNeeded) {
																			outputIterationData(intervalDataFile, intervalSummaryData);
																			// close the file here because we create a separate file for every case (function, #dimensions, #particles, etc.)
																			intervalDataFile.close();
																		}


//																	}  // RANDOM MOTION PROBABILITY
//
//
//																}  // PACEKEEPING
//
//
//															}  // ALIGNMENT
//
//
//														}  // COHESION
//
//
//													}  // SEPARATION
//
//
//												}  // RADIUS
//
//
//											} // NORMAL SPEED
//
//
//										} // MAXIMUM SPEED


									}  // END NUM ITERATIONS


								}  // END NUM PARTICLES FOR-LOOP 


							}  // END NUM DIMENSIONS FOR-LOOP 		


						} // END INFLUENCE MODEL FOR-LOOP       


					} // INCLUDE SELF FOR-LOOP        


				} // END TOPOLOGY FOR-LOOP 


				if (summaryDataFileNeeded) {
					summaryDataFile.close();   
				}

				if (runsDataFileNeeded) {
					runsDataFile.close();
					pValue = mannWhitney(runsDataFileName, totalNumRuns, folder, functionNum, dateString);
				}


			} // END FUNCTION NUM FOR-LOOP 



			// close these files after *all* cases are done running (since 
			// output from all cases goes to these files)
			//			if (summaryDataFileNeeded) {
			//				summaryDataFile.close();   
			//			}


			//			if (runsDataFileNeeded) {
			//				runsDataFile.close();
			//				mannWhitney(runsDataFileName, totalNumRuns, folder, dateString);
			//			}


			// outputWindow is System.out
//			outputWindow.println("\n\nDONE");	
//			outputWindow.close();


		}  // try

		catch (Exception e) {
			e.printStackTrace();
		}
		
		return pValue;

	}


//	public static double calcCumBoundingBoxVolume() {
//	
//	double volume = 1;
//	for (int i = 0 ; i < cumMinDimValues.size() ; ++i) {
//		double dimensionSize = cumMaxDimValues.get(i) - cumMinDimValues.get(i);
//		volume *= dimensionSize;
//	}
//	
//	return volume;
//}
	

	public static boolean isSPSO(Topology topo) {
		return (topo == Topology.GBEST || topo == Topology.RING || topo ==Topology.vonNEUMANN || topo == Topology.MOORE);
	}



	public static String getCaseDescriptionString(Topology currentPSOTopology, SelfModel currentPSOSelfModel, InfluenceModel currentPSOInfluenceModel) {

		String topologyAndInfluence = "";

		if (currentPSOTopology == Topology.GBEST) {
			topologyAndInfluence += "   GB";
		}		
		else if (currentPSOTopology == Topology.RING) {
			topologyAndInfluence += "   RI";
		}
		else if (currentPSOTopology == Topology.vonNEUMANN) {
			topologyAndInfluence += "   vN";
		}
		else if (currentPSOTopology == Topology.MOORE) {
			topologyAndInfluence += "   MO";
		}
		else if (currentPSOTopology == Topology.FLOCK){
			topologyAndInfluence += "FL";	
		}	
		else {
			topologyAndInfluence += "UNKNOWN_PSO_TOPOLOGY";
		}


		if (currentPSOSelfModel == SelfModel.INCLUDE_SELF) {
			topologyAndInfluence += "-yesSELF";
		}
		else if (currentPSOSelfModel == SelfModel.NOT_INCLUDE_SELF) {
			topologyAndInfluence += "-noSELF";
		}
		else {
			topologyAndInfluence += "UNKNOWN_PSO_SELF_MODEL";
		}


		if (currentPSOInfluenceModel == InfluenceModel.NEIGH_BEST) {
			topologyAndInfluence += "-nBEST";
		}
		else if (currentPSOInfluenceModel == InfluenceModel.FIPS){
			topologyAndInfluence += "-FIPS";
		}
		else {
			topologyAndInfluence += "UNKNOWN_PSO_INFLUENCE_MODEL";
		}

		return topologyAndInfluence;

	}




//	public static String getTypeOfTestString(int currentFunctionNum, int numDimensions, int numParticles, int numRuns, int numIterations,
//			double currentMaxSpeed, double currentNormalSpeed, double currentNeighRadius, double currentSeparationWeight,
//			double currentCohesionWeight, double currentAlignmentWeight, double currentPacekeepingWeight, double currentRandMotionProb) {

	public static String getTypeOfTestString(int currentFunctionNum, int numDimensions, int numParticles, int numRuns, int numIterations) {
		
		String typeOfTest = "";

		if (usingSPSO) {
			typeOfTest += "S-PSO";
		}

		else { 
			typeOfTest += "FLOCK-PSO";	
//			typeOfTest += "ms" + currentMaxSpeed;
//			typeOfTest += "ns" + currentNormalSpeed;
//			typeOfTest += "ra" + currentNeighRadius;
//			typeOfTest += "se" + currentSeparationWeight;
//			typeOfTest += "co" + currentCohesionWeight;
//			typeOfTest += "al" + currentAlignmentWeight;
//			typeOfTest += "pa" + currentPacekeepingWeight;
//			typeOfTest += "rm" + currentRandMotionProb;
		}

//		// get rid of leading dash
//		return typeOfTest.substring(1);
		return typeOfTest;

	}



	public static void outputTypeOfTest(PrintWriter resultsOutputFile) {

		resultsOutputFile.print("# ");

		if (usingSPSO) {
			resultsOutputFile.println("STANDARD PSO");
		}

		else { 
			resultsOutputFile.println("FLOCK PSO");	
		}

		resultsOutputFile.println("# --------------------------------------------------------------------------------------------");


	}


//	public static void outputTestParameters(PrintWriter resultsOutputFile, Topology currentPSOTopology, SelfModel currentPSOSelfModel, 
//			InfluenceModel currentPSOInfluenceModel, 
//			double currentMaxSpeed, double currentNormalSpeed, double currentNeighRadius, double currentSeparationWeight,
//			double currentCohesionWeight, double currentAlignmentWeight, double currentPacekeepingWeight, double currentRandMotionProb,
//			int currentFunctionNum, int numDimensions, int numParticles, int numRuns, int totalNumFunctionEvaluations, int numIterations) {

	public static void outputTestParameters(PrintWriter resultsOutputFile, Topology currentPSOTopology, SelfModel currentPSOSelfModel, 
			InfluenceModel currentPSOInfluenceModel, 
			int currentFunctionNum, int numDimensions, int numParticles, int numRuns, int totalNumFunctionEvaluations, int numIterations) {

		resultsOutputFile.println("# " + TestFunctions.getFunctionName(currentFunctionNum) + ", location of optimum shifted randomly for each run ");

		resultsOutputFile.println("# "+ getCaseDescriptionString(currentPSOTopology, currentPSOSelfModel, currentPSOInfluenceModel));

		resultsOutputFile.println("# " + numDimensions + " dimensions");

		resultsOutputFile.println("# " + numParticles + " particles");

		resultsOutputFile.print("# ");
		if (useIterations)
			resultsOutputFile.println(numIterations + " iterations");
		else
			resultsOutputFile.println(totalNumFunctionEvaluations + " function evaluations");

		resultsOutputFile.println("# overall theta = " + theta + ", constriction = " + constrictionFactor);						

//		if (!usingSPSO) {
//			resultsOutputFile.println("# max speed = " + currentMaxSpeed);
//			resultsOutputFile.println("# normal speed = " + currentNormalSpeed);
//			resultsOutputFile.println("# neigh radius = " + currentNeighRadius);
//			resultsOutputFile.println("# separation = " + currentSeparationWeight);
//			resultsOutputFile.println("# cohesion = " + currentCohesionWeight);
//			resultsOutputFile.println("# alignment = " + currentAlignmentWeight);
//			resultsOutputFile.println("# pacekeeping = " + currentPacekeepingWeight);
//			resultsOutputFile.println("# rand motionprob = " + currentRandMotionProb);
//		}

		resultsOutputFile.print("# ");
		if (useSeedForRand)
			resultsOutputFile.println("random number generator seed = " + seed);			
		else
			resultsOutputFile.println("random number generator seed = no seed");

		resultsOutputFile.println("# " + numRuns + " runs");

		resultsOutputFile.println("# ");

		resultsOutputFile.flush();

	}




	// print out the function value summary data for the last iteration
	private static void outputSummaryData(PrintWriter summaryDataFile, IntervalSummaryData intervalSummaryData[], double secondsPerRun, 
			double successRate, double averageDistanceToOptimum, int currentFunctionNum) {

		summaryDataFile.printf("%s %.4e \n", "Average function value over runs = ", intervalSummaryData[intervalSummaryData.length-1].getAverageFunctionValue());
		summaryDataFile.printf("%s %.4e \n", "Standard deviation over runs     = ", intervalSummaryData[intervalSummaryData.length-1].getStdDevFunctionValue());
		summaryDataFile.printf("%s %.4e \n", "Minimum function value over runs = ", intervalSummaryData[intervalSummaryData.length-1].getMinimumFunctionValue());
		summaryDataFile.printf("%s %.4e \n", "Median function value over runs  = ", intervalSummaryData[intervalSummaryData.length-1].getMedianFunctionValue());
		summaryDataFile.printf("%s %.4e \n", "Maximum function value over runs = ", intervalSummaryData[intervalSummaryData.length-1].getMaximumFunctionValue());
		//	summaryDataFile.printf("%s %4.3e \n", "RMSE over runs                   = ", iterationSummaryData[iterationSummaryData.length-1].getRootMeanSqrErrFunctionValue());
		summaryDataFile.println();

		if (TestFunctions.SUCCESS_CRITERION[currentFunctionNum] >= 0.000001)
			summaryDataFile.printf("%s %8.6f    \n", "Function value success criterion = ", TestFunctions.SUCCESS_CRITERION[currentFunctionNum]);
		else
			summaryDataFile.printf("%s %.4e    \n", "Function value success criterion = ", TestFunctions.SUCCESS_CRITERION[currentFunctionNum]);
		summaryDataFile.printf("%s %6.2f%s \n", "Success rate over runs = ", successRate, "%");

		summaryDataFile.printf("%s %.4e    \n", "Average distance to optimum = ", averageDistanceToOptimum);
		summaryDataFile.println();	

		summaryDataFile.println("Time per run: " + secondsPerRun + " seconds");		
		summaryDataFile.println("--------------------------------------------------------------------------------------------------------------------------------------------------------------------");
		summaryDataFile.println();

		summaryDataFile.flush();

	}






	// print out the function and error summary data for each iteration at which it was collected
	private static void outputIterationData(PrintWriter intervalSummaryDataFile, IntervalSummaryData intervalSummaryData[]) {


		intervalSummaryDataFile.println("#  val = function value,   err = absolute value error,   bbv = axis-aligned bounding box volume");

		if (useIterations)
			intervalSummaryDataFile.print("# iter      ");
		else
			intervalSummaryDataFile.print("# #FEs      ");

		intervalSummaryDataFile.print("mean-val    std-dev-val     min-val        q1-val      median-val      q3-val       max-val");
		intervalSummaryDataFile.print("       mean-err      min-err        q1-err      median-err      q3-err       max-err");
		intervalSummaryDataFile.println("         mean-bbv        min-bbv          q1-bbv        median-bbv        q3-bbv         max-bbv");


		intervalSummaryDataFile.println("#-------------------------------------------------------------------------------------------" + 
				"-------------------------------------------------------------------------------------------------" +
				"-------------------------------------------------------------------------------------------------");

		// start at interval i = 1 because we have set it up so that interval numbers are *not* zero-based
		// NOTE:  the length of the intervalSummaryData array is (numIntervalsForDataOutput+1), so
		// the last intervalNum, i.e. intervalSummaryData.length - 1, is actually numIntervalsForDataOutput
		for (int i = 1 ; i < intervalSummaryData.length ; ++i) {	

			if (useIterations) 
				intervalSummaryDataFile.printf("%6d     ", i * numItersPerOutputInterval);
			else
				intervalSummaryDataFile.printf("%6d     ", i * numFEsPerOutputInterval);

			// mean function value
			intervalSummaryDataFile.printf("%.4e    ", 
					intervalSummaryData[i].getAverageFunctionValue());
			// std dev function value
			intervalSummaryDataFile.printf("%.4e    ", 
					intervalSummaryData[i].getStdDevFunctionValue());
			// min function value
			intervalSummaryDataFile.printf("%.4e    ", 
					intervalSummaryData[i].getMinimumFunctionValue());
			// q1 function value
			intervalSummaryDataFile.printf("%.4e    ", 
					intervalSummaryData[i].getFirstQuartileFunctionValue());
			// q2 function value
			intervalSummaryDataFile.printf("%.4e    ", 
					intervalSummaryData[i].getMedianFunctionValue());
			// q3 function value
			intervalSummaryDataFile.printf("%.4e    ", 
					intervalSummaryData[i].getThirdQuartileFunctionValue());
			// max function value
			intervalSummaryDataFile.printf("%.4e    ", 
					intervalSummaryData[i].getMaximumFunctionValue());


			// mean abs val error
			intervalSummaryDataFile.printf("%.4e    ", 
					intervalSummaryData[i].getAverageAbsValError());
			// min abs val error
			intervalSummaryDataFile.printf("%.4e    ", 
					intervalSummaryData[i].getMinimumAbsValError());
			// q1 abs val error
			intervalSummaryDataFile.printf("%.4e    ", 
					intervalSummaryData[i].getFirstQuartileAbsValError());
			// q2 abs val error
			intervalSummaryDataFile.printf("%.4e    ", 
					intervalSummaryData[i].getMedianAbsValError());
			// q3 abs val error
			intervalSummaryDataFile.printf("%.4e    ", 
					intervalSummaryData[i].getThirdQuartileAbsValError());
			// max abs val error
			intervalSummaryDataFile.printf("%.4e    ", 
					intervalSummaryData[i].getMaximumAbsValError());


			// mean bounding box volume
			intervalSummaryDataFile.printf("%12.4e    ", 
					intervalSummaryData[i].getAverageBoundingBoxVolume());
			// min bounding box volume
			intervalSummaryDataFile.printf("%12.4e    ", 
					intervalSummaryData[i].getMinimumBoundingBoxVolume());
			// q1 bounding box volume
			intervalSummaryDataFile.printf("%12.4e    ", 
					intervalSummaryData[i].getFirstQuartileBoundingBoxVolume());
			// q2 bounding box volume
			intervalSummaryDataFile.printf("%12.4e    ", 
					intervalSummaryData[i].getMedianBoundingBoxVolume());
			// q3 bounding box volume
			intervalSummaryDataFile.printf("%12.4e    ", 
					intervalSummaryData[i].getThirdQuartileBoundingBoxVolume());
			// max bounding box volume
			intervalSummaryDataFile.printf("%12.4e    ", 
					intervalSummaryData[i].getMaximumBoundingBoxVolume());

			intervalSummaryDataFile.println(); 

		}

		intervalSummaryDataFile.println("#-------------------------------------------------------------------------------------------" + 
				"-------------------------------------------------------------------------------------------------" +
				"-------------------------------------------------------------------------------------------------");

		if (useIterations)
			intervalSummaryDataFile.print("# iter      ");
		else
			intervalSummaryDataFile.print("# #FEs      ");

		intervalSummaryDataFile.print("mean-val    std-dev-val     min-val        q1-val      median-val      q3-val       max-val");
		intervalSummaryDataFile.print("       mean-err      min-err        q1-err      median-err      q3-err       max-err");
		intervalSummaryDataFile.println("         mean-bbv        min-bbv          q1-bbv        median-bbv        q3-bbv         max-bbv");

		intervalSummaryDataFile.flush();

	}



	// this has to be done before any summary statistics are printed to the screen or sent to a file
	// iterationData contains data for every run for every iteration
	// 
	private static void calculateSummaryDataOverRuns(DataOutput[][] intervalData, IntervalSummaryData[] intervalSummaryData, 
			int functionNum, int numDimensions) {

		for (int i = 0 ; i < intervalSummaryData.length ; ++i) {
			// we need to send iterationData[i], the data over all runs for iteration i, to the 
			// summarizeData method of intervalSummaryData[i], which is the IntervalSummaryData object 
			// for iteration i.  the method uses that data to calculate statistics (average, min, max, etc.) for that 
			// iteration over all the runs and set those values in the IntervalSummaryData object
			intervalSummaryData[i].summarizeData(intervalData[i], functionNum, numDimensions);
		}

	}





	public static double mannWhitney(String runsDataFileName, int numRuns, String folder, int functionNum, String dateString) {

		final int TOTAL_NUM_CASE_LINES = 50;
		double returnPValue = 0.0;

		try {

			String mannWhitneyFilename = folder + "MANN-WHITNEY-" + TestFunctions.getShortFunctionName(functionNum) + "-" + dateString + ".txt";
			PrintWriter mwOutputFile = new PrintWriter(new FileWriter(mannWhitneyFilename));
			mwOutputFile.println(dateString);
			mwOutputFile.println();
			mwOutputFile.println();
			mwOutputFile.println("=====================================================");
			mwOutputFile.println("FUNCTION: " + TestFunctions.getFunctionName(functionNum));
			mwOutputFile.println("=====================================================");
			mwOutputFile.println();
			mwOutputFile.println();
			mwOutputFile.println();



			File runsDataFile = new File(runsDataFileName);

			Scanner fScanner = new Scanner(runsDataFile);

			int numCases = 0;
			while (fScanner.hasNextLine()) {
				String line = fScanner.nextLine();
				if (line.contains("GLOBAL BEST FUNCTION VALUES"))
					++numCases;
			}
			fScanner.close();

			fScanner = new Scanner(runsDataFile);
			double[][] funcValData = new double[numCases][numRuns];
			double[][] distGlobalOptData = new double[numCases][numRuns];
			String[][] caseInfo = new String[numCases][TOTAL_NUM_CASE_LINES];		
			String line = "";

			for(int caseNum = 0; caseNum < numCases; ++caseNum) {

				int caseInfoLine = 0;

				while (!line.contains("STANDARD") && !line.contains("FLOCK")) {
					line = fScanner.nextLine();
				}
				caseInfo[caseNum][caseInfoLine++] = line;
				line = fScanner.nextLine();
				while (line.length() > 0 && line.charAt(0) == '#') {
					caseInfo[caseNum][caseInfoLine++] = line;
					line = fScanner.nextLine();
				}

				while (!line.contains("GLOBAL BEST FUNCTION VALUES")) {
					line = fScanner.nextLine();	
				}
				for (int run = 0; run < numRuns; ++run) {
					funcValData[caseNum][run] = fScanner.nextDouble();
				}

				while (!line.contains("GLOBAL BEST DISTANCE FROM OPTIMUM")) {
					line = fScanner.nextLine();	
				}
				for (int run = 0; run < numRuns; ++run) {
					distGlobalOptData[caseNum][run] = fScanner.nextDouble();
				}

				while (!line.contains("Average function value over runs")) {
					line = fScanner.nextLine();
				}
				caseInfo[caseNum][caseInfoLine++] = line;
				line = fScanner.nextLine();
				while (fScanner.hasNextLine() && !line.contains("---")) {
					caseInfo[caseNum][caseInfoLine++] = line;
					line = fScanner.nextLine();
				}

				caseInfo[caseNum][caseInfoLine++] = line;
			}

			fScanner.close();


			mwOutputFile.println("-----------------------------------------------------");
			mwOutputFile.println("FUNCTION VALUE MANN WHITNEY TESTS");
			mwOutputFile.println("-----------------------------------------------------");
			mwOutputFile.println();
			mwOutputFile.println();



			// Mann Whitney for function values
			for (int caseNum1 = 0; caseNum1 < numCases; ++caseNum1) {
				//				for (int caseNum2 = 0; caseNum2 < numCases; ++caseNum2) {
				for (int caseNum2 = caseNum1 + 1; caseNum2 < numCases; ++caseNum2) {

					if (caseNum1 == caseNum2)
						continue;

					//					if (caseInfo[caseNum1][0].contains("MEMORY")) //  && caseInfo[caseNum2][0].contains("MEMORY"))
					//						continue;

					double[] A = funcValData[caseNum1];
					double[] B = funcValData[caseNum2];

					// MannWhitneyUTest (Apache Commons)
					MannWhitneyUTest mw1 = new MannWhitneyUTest();
					double p1 = mw1.mannWhitneyUTest(A, B);
					double u1 = mw1.mannWhitneyU(A, B);	
				
					// MannWhitneyTest (jsc)
					MannWhitneyTest mw2 = new MannWhitneyTest(A, B);
					double p2a = mw2.getSP();
					//					double p2b = mw2.approxSP();
					//					double u2a = mw2.getStatistic();
					double u2b = mw2.getTestStatistic();
					double z2 = mw2.getZ();


					//					int commaIndex1 = caseInfo[caseNum1][0].indexOf(',');
					//					int commaIndex2 = caseInfo[caseNum2][0].indexOf(',');
					mwOutputFile.println("FUNCTION VALUE FOR " + TestFunctions.getFunctionName(functionNum));
					mwOutputFile.println("------------------------------------------------------------------");
					mwOutputFile.println("  Case 1: " + caseInfo[caseNum1][0]);
					if (caseInfo[caseNum1][0].contains("STANDARD")) {
						mwOutputFile.println("             " + caseInfo[caseNum1][3] + " -- " + caseInfo[caseNum1][5].substring(2) + " -- " + caseInfo[caseNum1][6].substring(2));
					}					
					if (caseInfo[caseNum1][0].contains("FLOCK")) {
						mwOutputFile.println("             " + caseInfo[caseNum1][9] + " -- " + caseInfo[caseNum1][11].substring(2) + " -- " + caseInfo[caseNum1][12].substring(2));
						mwOutputFile.println("             " + caseInfo[caseNum1][1]);
						mwOutputFile.println("             " + caseInfo[caseNum1][2]);
						mwOutputFile.println("             " + caseInfo[caseNum1][3]);
						mwOutputFile.println("             " + caseInfo[caseNum1][4]);
						mwOutputFile.println("             " + caseInfo[caseNum1][5]);
						mwOutputFile.println("             " + caseInfo[caseNum1][6]);
					}

					mwOutputFile.println("  Case 2: " + caseInfo[caseNum2][0]);
					if (caseInfo[caseNum2][0].contains("STANDARD")) {
						mwOutputFile.println("             " + caseInfo[caseNum2][3] + " -- " + caseInfo[caseNum2][5].substring(2) + " -- " + caseInfo[caseNum2][6].substring(2));
					}if (caseInfo[caseNum2][0].contains("FLOCK")) {
						mwOutputFile.println("             " + caseInfo[caseNum2][9] + " -- " + caseInfo[caseNum2][11].substring(2) + " -- " + caseInfo[caseNum2][12].substring(2));
						mwOutputFile.println("             " + caseInfo[caseNum2][1]);
						mwOutputFile.println("             " + caseInfo[caseNum2][2]);
						mwOutputFile.println("             " + caseInfo[caseNum2][3]);
						mwOutputFile.println("             " + caseInfo[caseNum2][4]);
						mwOutputFile.println("             " + caseInfo[caseNum2][5]);
						mwOutputFile.println("             " + caseInfo[caseNum2][6]);
					}


					//					mwOutputFile.println("FUNCTION VALUE  (First One: " + caseInfo[caseNum1][0].substring(2, commaIndex1) + 
					//							" , Second One: " + caseInfo[caseNum2][0].substring(2, commaIndex2) + ")");
					mwOutputFile.println();					
					//					mwOutputFile.println(caseInfo[caseNum1][1]);
					double case1MeanRank = mw2.getRankSumA()/numRuns;
					mwOutputFile.println("Case 1:");
					mwOutputFile.println("     mean rank (jsc) = " + case1MeanRank);
					//					mwOutputFile.println(caseInfo[caseNum2][1]);
					double case2MeanRank = mw2.getRankSumB()/numRuns;
					mwOutputFile.println("Case 2:");
					mwOutputFile.println("     mean rank (jsc) = " + case2MeanRank);
					mwOutputFile.println();


					// MannWhitneyUTest
					mwOutputFile.println("p (apache) = " + p1);
					mwOutputFile.println("u (apache) = " + u1);
					mwOutputFile.println();

					// MannWhitneyTest
					mwOutputFile.println("p (jsc) = " + p2a);
					//					allFinalFuncValsDataFile.println("p2b = " + p2b);
					//					allFinalFuncValsDataFile.println("u2a = " + u2a);
					mwOutputFile.println("u (jsc) = " + u2b);
					mwOutputFile.println("z (jsc) = " + z2);

					mwOutputFile.println();
					mwOutputFile.println();
					mwOutputFile.println();
					
					if (case1MeanRank < case2MeanRank) {
						returnPValue = -p2a;
					}
					else {
						returnPValue = p2a;
					}

				}
			}



			mwOutputFile.println();
			mwOutputFile.println();
			mwOutputFile.println("-----------------------------------------------------");
			mwOutputFile.println("DISTANCE FROM GLOBAL OPTIMUM MANN WHITNEY TESTS");
			mwOutputFile.println("-----------------------------------------------------");
			mwOutputFile.println();
			mwOutputFile.println();


			// Mann Whitney for distance from global opt values
			for (int caseNum1 = 0; caseNum1 < numCases; ++caseNum1) {
				//				for (int caseNum2 = 0; caseNum2 < numCases; ++caseNum2) {
				for (int caseNum2 = caseNum1 + 1; caseNum2 < numCases; ++caseNum2) {

					if (caseNum1 == caseNum2)
						continue;

					//					if (caseInfo[caseNum1][0].contains("MEMORY")) //  && caseInfo[caseNum2][0].contains("MEMORY"))
					//						continue;

					double[] A = distGlobalOptData[caseNum1];
					double[] B = distGlobalOptData[caseNum2];

					// MannWhitneyUTest (Apache Commons)
					MannWhitneyUTest mw1 = new MannWhitneyUTest();
					double p1 = mw1.mannWhitneyUTest(A, B);
					double u1 = mw1.mannWhitneyU(A, B);	

					// MannWhitneyTest (jsc)
					MannWhitneyTest mw2 = new MannWhitneyTest(A, B);
					double p2a = mw2.getSP();
					//					double p2b = mw2.approxSP();
					//					double u2a = mw2.getStatistic();
					double u2b = mw2.getTestStatistic();
					double z2 = mw2.getZ();

					//					int commaIndex1 = caseInfo[caseNum1][0].indexOf(',');
					//					int commaIndex2 = caseInfo[caseNum2][0].indexOf(',');
					mwOutputFile.println("DISTANCE FROM GLOBAL OPTIMUM FOR " + TestFunctions.getFunctionName(functionNum));
					mwOutputFile.println("------------------------------------------------------------------");
					mwOutputFile.println("  Case 1: " + caseInfo[caseNum1][0]);
					if (caseInfo[caseNum1][0].contains("STANDARD")) {
						mwOutputFile.println("             " + caseInfo[caseNum1][3] + " -- " + caseInfo[caseNum1][5].substring(2) + " -- " + caseInfo[caseNum1][6].substring(2));
					}					
					if (caseInfo[caseNum1][0].contains("FLOCK")) {
						mwOutputFile.println("             " + caseInfo[caseNum1][9] + " -- " + caseInfo[caseNum1][11].substring(2) + " -- " + caseInfo[caseNum1][12].substring(2));
						mwOutputFile.println("             " + caseInfo[caseNum1][1]);
						mwOutputFile.println("             " + caseInfo[caseNum1][2]);
						mwOutputFile.println("             " + caseInfo[caseNum1][3]);
						mwOutputFile.println("             " + caseInfo[caseNum1][4]);
						mwOutputFile.println("             " + caseInfo[caseNum1][5]);
						mwOutputFile.println("             " + caseInfo[caseNum1][6]);
					}

					mwOutputFile.println("  Case 2: " + caseInfo[caseNum2][0]);
					if (caseInfo[caseNum2][0].contains("STANDARD")) {
						mwOutputFile.println("             " + caseInfo[caseNum2][3] + " -- " + caseInfo[caseNum2][5].substring(2) + " -- " + caseInfo[caseNum2][6].substring(2));
					}if (caseInfo[caseNum2][0].contains("FLOCK")) {
						mwOutputFile.println("             " + caseInfo[caseNum2][9] + " -- " + caseInfo[caseNum2][11].substring(2) + " -- " + caseInfo[caseNum2][12].substring(2));
						mwOutputFile.println("             " + caseInfo[caseNum2][1]);
						mwOutputFile.println("             " + caseInfo[caseNum2][2]);
						mwOutputFile.println("             " + caseInfo[caseNum2][3]);
						mwOutputFile.println("             " + caseInfo[caseNum2][4]);
						mwOutputFile.println("             " + caseInfo[caseNum2][5]);
						mwOutputFile.println("             " + caseInfo[caseNum2][6]);
					}


					//					mwOutputFile.println("FUNCTION VALUE  (First One: " + caseInfo[caseNum1][0].substring(2, commaIndex1) + 
					//							" , Second One: " + caseInfo[caseNum2][0].substring(2, commaIndex2) + ")");
					mwOutputFile.println();					
					//					mwOutputFile.println(caseInfo[caseNum1][1]);
					mwOutputFile.println("Case 1:");
					mwOutputFile.println("     mean rank (jsc) = " + mw2.getRankSumA()/numRuns);
					//					mwOutputFile.println(caseInfo[caseNum2][1]);
					mwOutputFile.println("Case 2:");
					mwOutputFile.println("     mean rank (jsc) = " + mw2.getRankSumB()/numRuns);
					mwOutputFile.println();


					//					int commaIndex1 = caseInfo[caseNum1][0].indexOf(',');
					//					int commaIndex2 = caseInfo[caseNum2][0].indexOf(',');
					//					mwOutputFile.println("DISTANCE FROM GLOBAL OPTIMUM  (First One: " + caseInfo[caseNum1][0].substring(2, commaIndex1) + 
					//							" , Second One: " + caseInfo[caseNum2][0].substring(2, commaIndex2) + ")");
					//					mwOutputFile.println();						
					//					mwOutputFile.println(caseInfo[caseNum1][1]);
					//					mwOutputFile.println("     mean rank (jsc) = " + mw2.getRankSumA()/numRuns);
					//					mwOutputFile.println(caseInfo[caseNum2][1]);
					//					mwOutputFile.println("     mean rank (jsc) = " + mw2.getRankSumB()/numRuns);
					//					mwOutputFile.println();


					// MannWhitneyUTest
					mwOutputFile.println("p (apache) = " + p1);
					mwOutputFile.println("u (apache) = " + u1);
					mwOutputFile.println();

					// MannWhitneyTest
					mwOutputFile.println("p (jsc) = " + p2a);
					//					allFinalFuncValsDataFile.println("p2b = " + p2b);
					//					allFinalFuncValsDataFile.println("u2a = " + u2a);
					mwOutputFile.println("u (jsc) = " + u2b);
					mwOutputFile.println("z (jsc) = " + z2);

					mwOutputFile.println();
					mwOutputFile.println();
					mwOutputFile.println();



				}
			}



			mwOutputFile.println();
			mwOutputFile.println();
			mwOutputFile.println("-----------------------------------------------------");
			mwOutputFile.println("CASE INFORMATION");
			mwOutputFile.println("-----------------------------------------------------");
			mwOutputFile.println();
			mwOutputFile.println();
			for (int cNum = 0; cNum < numCases; ++cNum) {
				printCaseInfo(caseInfo, cNum, mwOutputFile);
				mwOutputFile.println();
				mwOutputFile.println();
			}

			mwOutputFile.close();

		}  // try

		catch (Exception e)
		{
			e.printStackTrace();
		}

		return returnPValue;

	}





	public static void printCaseInfo(String[][] caseInfo, int caseNum, PrintWriter mwOutputFile) {

		for (int infoLine = 0; infoLine < caseInfo[caseNum].length; ++infoLine) {
			if (caseInfo[caseNum][infoLine] != null) {
				if (caseInfo[caseNum][infoLine].contains("-----"))
					mwOutputFile.println("-------------------------------------------------------------");
				else
					mwOutputFile.println(caseInfo[caseNum][infoLine]);
			}
		}
	}




}

