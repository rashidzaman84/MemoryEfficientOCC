package org.processmining.memoryawareocc.plugins;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.deckfour.xes.model.XLog;
import org.javatuples.Triplet;
import org.processmining.memoryawareocc.algorithms.impl.MeanMedianMode;
import org.processmining.memoryawareocc.algorithms.impl.NullConfiguration;
import org.processmining.memoryawareocc.algorithms.impl.StatesCalculator;
import org.processmining.memoryawareocc.algorithms.impl.TimeStampsBasedLogToStreamConverter;
//import org.processmining.memoryawareocc.algorithms.IncrementalReplayer;
//import org.processmining.memoryawareocc.parameters.IncrementalReplayerParametersImpl;
//import org.processmining.memoryawareocc.parameters.IncrementalRevBasedReplayerParametersImpl;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.PetrinetGraph;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.onlineconformance.algorithms.IncrementalReplayer;
import org.processmining.onlineconformance.models.IncrementalReplayResult;
import org.processmining.onlineconformance.models.ModelSemanticsPetrinet;
import org.processmining.onlineconformance.models.Move;
import org.processmining.onlineconformance.models.PartialAlignment;
import org.processmining.onlineconformance.models.PartialAlignment.State;
import org.processmining.onlineconformance.parameters.IncrementalReplayerParametersImpl;
import org.processmining.onlineconformance.parameters.IncrementalRevBasedReplayerParametersImpl;

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;


//@Plugin(name = "01 Compute Prefix Alignments - With Bounded States", parameterLabels = {"Model", "Event Data" }, 
//returnLabels = { "Replay Result" }, returnTypes = { NullConfiguration.class },
//help = "")

public class IncrementalPrefixAlignmentComputation {
//	@UITopiaVariant(author = "R. Zaman", email = "r.zaman@tue.nl", affiliation = "Eindhoven University of Technology")
//	@PluginVariant(variantLabel = "01 Compute Prefix Alignments - With Bounded States", requiredParameterLabels = { 0, 1})

	public static NullConfiguration apply(
			final Petrinet net, XLog log, int stateLimit) throws IOException {
		
		//-------parameter to set
		//int stateLimit = 5;

		Map<Transition, String> modelElementsToLabelMap = new HashMap<>();
		Map<String, Collection<Transition>> labelsToModelElementsMap = new HashMap<>();
		TObjectDoubleMap<Transition> modelMoveCosts = new TObjectDoubleHashMap<>();
		TObjectDoubleMap<String> labelMoveCosts = new TObjectDoubleHashMap<>();

		Marking initialMarking = getInitialMarking(net);
		Marking finalMarking = getFinalMarking(net);

		setupLabelMap(net, modelElementsToLabelMap, labelsToModelElementsMap);
		setupModelMoveCosts(net, modelMoveCosts, labelMoveCosts, labelsToModelElementsMap);
		IncrementalRevBasedReplayerParametersImpl<Petrinet, String, Transition> parameters = new IncrementalRevBasedReplayerParametersImpl<>();
		parameters.setUseMultiThreading(false);
		parameters.setLabelMoveCosts(labelMoveCosts);
		parameters.setLabelToModelElementsMap(labelsToModelElementsMap);
		parameters.setModelMoveCosts(modelMoveCosts);
		parameters.setModelElementsToLabelMap(modelElementsToLabelMap);
		parameters.setSearchAlgorithm(IncrementalReplayer.SearchAlgorithm.A_STAR);
		parameters.setUseSolutionUpperBound(false);
		//parameters.setMaxCasesToStore(Integer.MAX_VALUE); // Integer.MAX_VALUE; specifies the maximum number of cases to be stored in memory
		//parameters.setLookBackWindow(Integer.MAX_VALUE);
		parameters.setExperiment(false);		
		parameters.setLookBackWindow(stateLimit);

		applyGeneric(net, initialMarking, finalMarking, log, parameters, stateLimit);
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <A extends PartialAlignment<String, Transition, Marking>> void applyGeneric(
			final Petrinet net, final Marking initialMarking, final Marking finalMarking,
			/*final*/ XLog log, final IncrementalRevBasedReplayerParametersImpl<Petrinet, String, Transition> parameters, int stateLimit) throws IOException {
		ModelSemanticsPetrinet<Marking> modelSemantics = ModelSemanticsPetrinet.Factory.construct(net);
		Map<Transition, String> labelsInPN = new HashMap<Transition, String>();
		for (Transition t : net.getTransitions()) {
			if (!t.isInvisible()) {
				labelsInPN.put(t, t.getLabel());
			}
		}

		Map<String, PartialAlignment<String, Transition, Marking>> store = new HashMap<>();
		IncrementalReplayer<Petrinet, String, Marking, Transition, String, PartialAlignment<String, Transition, Marking>, IncrementalRevBasedReplayerParametersImpl<Petrinet, String, Transition>> replayer = IncrementalReplayer.Factory
				.construct(initialMarking, finalMarking, store, modelSemantics, parameters, labelsInPN,
						IncrementalReplayer.Strategy.REVERT_BASED);
		processXLog(log, net, initialMarking, replayer, stateLimit);
	}


	@SuppressWarnings("unchecked")
	private static <A extends PartialAlignment<String, Transition, Marking>> IncrementalReplayResult<String, String, Transition, Marking, A> processXLog(
			XLog log, Petrinet net, Marking iMarking,
			IncrementalReplayer<Petrinet, String, Marking, Transition, String, A, ? extends IncrementalReplayerParametersImpl<Petrinet, String, Transition>> replayer,int stateLimit){

		ArrayList<Triplet<String,String,Date>>	eventLogSortedByDate = new ArrayList<>();		
		HashMap<String, List<Double>> compoundCost = new HashMap<>();
		ArrayList<Triplet<Integer, Integer, Double>> CostRecords = new ArrayList<>();
		ArrayList<Triplet<Integer, String, Double>> universalCostRecords = new ArrayList<>();
		HashMap<Integer, ArrayList<Integer>> universalStateRecords = new HashMap<>();
		
		String caseId;
		String event;
		int observedEvents = 0;
		final double noOfWindows = 10d;
		eventLogSortedByDate = TimeStampsBasedLogToStreamConverter.sortEventLogByDate(log);
		int eventsWindowSize = (int) Math.ceil(eventLogSortedByDate.size()/noOfWindows);
		int remainder = eventLogSortedByDate.size()%eventsWindowSize;
		int window = 0;

		for (Triplet<String,String, Date> entry : eventLogSortedByDate) {

			caseId = entry.getValue0();
			event = entry.getValue1();

			PartialAlignment<String, Transition, Marking> partialAlignment = replayer.processEvent(caseId, event);  //Prefix Alignment of the current observed event

			
			trackCosts(partialAlignment, caseId, compoundCost);
			if (!((partialAlignment.size()  < stateLimit) || (StatesCalculator.getNumberOfStates(partialAlignment) <= stateLimit))) {
				partialAlignment = trimAlignment(partialAlignment, stateLimit);
				replayer.getDataStore().put(caseId, (A)partialAlignment);
			}

			if(StatesCalculator.getNumberOfStates(partialAlignment) > stateLimit) { //to check the correctness of trimming
				System.out.println("I have more states than allowed...");
			}

			java.util.Iterator<Triplet<Integer, String, Double>> iterator = universalCostRecords.iterator();			//recording fitness costs
			while(iterator.hasNext()) {
				Triplet<Integer, String, Double> temp = iterator.next();
				if(temp.getValue0()==(window+1) && temp.getValue1().equals(caseId)) {
					iterator.remove();
					break;
				}
			}			
			universalCostRecords.add(new Triplet<Integer, String, Double>(window+1, caseId, partialAlignment.getCost()));

			//recording states			
			if(universalStateRecords.containsKey(window+1)) {
				universalStateRecords.get(window+1).add(StatesCalculator.getNumberOfStatesInMemory(replayer.getDataStore()));
			}else {
				ArrayList<Integer> tempStates = new ArrayList<>();
				tempStates.add(StatesCalculator.getNumberOfStatesInMemory(replayer.getDataStore()));
				universalStateRecords.put(window+1, tempStates);
			}
			//System.out.println(StatesCalculator.getNumberOfStatesInMemory(replayer.getDataStore()));

			observedEvents++;

			if(observedEvents==eventsWindowSize || (window+1 == noOfWindows && observedEvents == remainder)){ 
				
				observedEvents = 0;	

				int noOfObservedCases=0;
				int noOfNonConformantCases=0;
				double nonConformanceCosts=0.0;

				for(Entry<String, List<Double>> record: compoundCost.entrySet()){
					boolean nonConformant=false;
					noOfObservedCases++;
					for(Double cost : record.getValue()) {
						nonConformanceCosts += cost;
						if(cost>0.0) {
							nonConformant = true;
						}
					}
					if(nonConformant) {
						noOfNonConformantCases++;
					}
				}
				CostRecords.add(new Triplet<Integer, Integer, Double>(noOfObservedCases, noOfNonConformantCases, nonConformanceCosts));
				compoundCost.clear();
				window++; 


			}
		}


		//writeRecordsToFile(universalCostRecords, wLimit, nLimit);

		System.out.println("\n ,Non-conformant \n Window,Cases,Costs");
		int index = 1;
		for(Triplet<Integer, Integer, Double> entry : CostRecords) {
			System.out.println(index + "," + entry.getValue1() + "," + entry.getValue2());
			index++;
		}

		calculateStatesInWindows(universalStateRecords, "Max");

		//System.out.println("\n");

		//System.out.println("Window,Type1,Type2,Distinct Cases,Fresh Cases,Type1 Events,Type2 Events ");

		return null;
	}

	private static void setupLabelMap(final Petrinet net, Map<Transition, String> modelElementsToLabelMap, Map<String, Collection<Transition>> labelsToModelElementsMap) {
		for (org.processmining.models.graphbased.directed.petrinet.elements.Transition t : net.getTransitions()) {
			if (!t.isInvisible()) {
				String label = t.getLabel();
				modelElementsToLabelMap.put(t, label);
				if (!labelsToModelElementsMap.containsKey(label)) {
					Collection collection = new ArrayList<org.processmining.models.graphbased.directed.petrinet.elements.Transition>();
					collection.add(t);
					labelsToModelElementsMap.put(label, collection);
					//labelsToModelElementsMap.put(label, Collections.singleton(t));
				} else {
					labelsToModelElementsMap.get(label).add(t);
				}
			}
		}				
	}	

	//TODO: needs a parameter object
	private static void setupModelMoveCosts(final Petrinet net, TObjectDoubleMap<Transition> modelMoveCosts, TObjectDoubleMap<String> labelMoveCosts,
			Map<String, Collection<Transition>> labelsToModelElementsMap) {
		for (org.processmining.models.graphbased.directed.petrinet.elements.Transition t : net.getTransitions()) {
			if (t.isInvisible() /*|| (t.getLabel().equals("A_FINALIZED"))*/) {
				modelMoveCosts.put(t, (short) 0);
			} else {
				modelMoveCosts.put(t, (short) 1);
			}
		}

		for(String label : labelsToModelElementsMap.keySet()) {
			labelMoveCosts.put(label, (short) 1);
		}
	}


	public static Marking getFinalMarking(PetrinetGraph net) {
		Marking finalMarking = new Marking();

		for (Place p : net.getPlaces()) {
			if (net.getOutEdges(p).isEmpty())
				finalMarking.add(p);
		}

		return finalMarking;
	}

	public static Marking getInitialMarking(PetrinetGraph net) {
		Marking initMarking = new Marking();

		for (Place p : net.getPlaces()) {
			if (net.getInEdges(p).isEmpty())
				initMarking.add(p);
		}

		return initMarking;
	}

	public static Integer countFinalStates(Map<String, ArrayList<Integer>> alignmentsWindowsStates) {
		Integer sum = 0;
		for(Entry<String, ArrayList<Integer>> entry: alignmentsWindowsStates.entrySet()) {
			sum += entry.getValue().get(entry.getValue().size()-1);
		}
		return sum;
	}

	public static Integer countAllStates(Map<String, ArrayList<Integer>> alignmentsWindowsStates) {
		Integer sum = 0;
		for(Entry<String, ArrayList<Integer>> entry: alignmentsWindowsStates.entrySet()) {
			sum += getSum(entry.getValue());
		}
		return sum;
	}

	public static int getSum(ArrayList<Integer> list) {
		int sum = 0;
		for (int i: list) {
			sum += i;
		}
		return sum;
	}


	public static double sumArrayList(ArrayList<Double> arrayList) {
		Double sum = 0.0;
		for(int i = 0; i < arrayList.size(); i++)
		{
			sum += arrayList.get(i);
		}
		return sum;		
	}


	public static <A extends PartialAlignment<String, Transition, Marking>> A processEventUsingReplayer(String caseId,
			String event,
			IncrementalReplayer<Petrinet, String, Marking, Transition, String, A, ? extends IncrementalReplayerParametersImpl<Petrinet, String, Transition>> replayer) {
		return replayer.processEvent(caseId, event);
	}

	private static void writeRecordsToFile(ArrayList<Triplet<Integer, String, Double>> universalRecords, int w, int n) {

		String outputFilePath = "D:/Prefix Alignment/w="+  w + ",n=" + n + ".csv";
		File file = new File(outputFilePath);	
		BufferedWriter bf = null;
		boolean first = true;
		try {
			bf = new BufferedWriter(new FileWriter(file));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		try {
			bf.write("window,case,cost");
			bf.newLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(Triplet<Integer, String, Double> rec:universalRecords) {
			//System.out.println(rec);
			try {
				if(first) {
					first = false;
				}else {
					bf.newLine();
				}
				bf.write(rec.getValue0() + "," + rec.getValue1() + "," + rec.getValue2());

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			bf.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			bf.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void calculateStatesInWindows(HashMap<Integer, ArrayList<Integer>> universalStateRecords, String option) {
		System.out.println("\nWindow," + option + " states stored");
		//if(option.equals("Max")) {
		for(Entry<Integer, ArrayList<Integer>> rec : universalStateRecords.entrySet()) {
			System.out.println(rec.getKey() + "," + Collections.max(rec.getValue()));

		}
		//}else if (option.equals("Mean")) {
		System.out.println("\nWindow, Mean states stored");
		for(Entry<Integer, ArrayList<Integer>> rec2 : universalStateRecords.entrySet()) {
			System.out.println(rec2.getKey() + "," + MeanMedianMode.findMean(rec2.getValue()));
		}
	}	

	private static void trackCosts(PartialAlignment alignment, String caseId, HashMap<String, List<Double>> compoundCost) {
		
		if(compoundCost.containsKey(caseId)) {
			compoundCost.get(caseId).add(alignment.getCost());
		}else {
			List<Double> temp = new ArrayList<>();
			temp.add(alignment.getCost());
			compoundCost.put(caseId, temp);
		}		
	}

	public static PartialAlignment resetAlignment(PartialAlignment alignment) {
		State state = alignment.getState();		
		return PartialAlignment.Factory.construct(state);
	}

	private static PartialAlignment trimAlignment(PartialAlignment alignment, int stateLimit) {
		
		ArrayList<State> statesArray = new ArrayList<>();
		State state = alignment.getState();
		statesArray.add(state);
		int i = 1;
		while (state.getParentState() != null && i < (stateLimit-1)) { //i < getParameters().getLookBackWindow() because we are adding ne dummy state
			state = state.getParentState();
			statesArray.add(state);
			i++;
		}

		double residualCosts =  state.getParentState() == null? 0 : calculateResidualCosts(state.getParentState());	
		state = PartialAlignment.State.Factory.construct(state.getParentState().getStateInModel(), 0, null, Move.Factory.construct(null,null , residualCosts));
		statesArray.add(state);

		for(int index=statesArray.size()-2; index>=0;index--) {
			statesArray.set(index, PartialAlignment.State.Factory.construct(statesArray.get(index).getStateInModel(), statesArray.get(index).getNumLabelsExplained() , statesArray.get(index+1), statesArray.get(index).getParentMove()));
		}

		alignment = PartialAlignment.Factory.construct(statesArray.get(0));

		return alignment;
	}

	public static double calculateResidualCosts(State statesToBeForgotten) {
		double costs = 0d;
		costs += statesToBeForgotten.getParentMove().getCost();
		while (statesToBeForgotten.getParentState() != null && statesToBeForgotten.getParentState().getParentMove() != null) {
			statesToBeForgotten = statesToBeForgotten.getParentState();
			costs += statesToBeForgotten.getParentMove().getCost();
		}
		return costs;
	}

}
