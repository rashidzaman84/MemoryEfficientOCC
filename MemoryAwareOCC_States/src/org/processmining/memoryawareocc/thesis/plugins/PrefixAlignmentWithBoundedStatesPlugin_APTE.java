package org.processmining.memoryawareocc.thesis.plugins;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;

import org.apache.commons.io.FilenameUtils;
import org.deckfour.xes.in.XUniversalParser;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.memoryawareocc.algorithms.impl.NullConfiguration;
//import org.processmining.memoryawareocc.algorithms.IncrementalReplayer;
//import org.processmining.memoryawareocc.parameters.IncrementalReplayerParametersImpl;
//import org.processmining.memoryawareocc.parameters.IncrementalRevBasedReplayerParametersImpl;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;


@Plugin(name = "01 Compute Prefix Alignments - With Bounded States THESIS (ATPE)", parameterLabels = {"Model", "Event Data" }, 
returnLabels = { "Replay Result" }, returnTypes = { NullConfiguration.class },
help = "")

public class PrefixAlignmentWithBoundedStatesPlugin_APTE {
	@UITopiaVariant(author = "R. Zaman", email = "r.zaman@tue.nl", affiliation = "Eindhoven University of Technology")
	@PluginVariant(variantLabel = "01 Compute Prefix Alignments - With Bounded States THESIS (ATPE)", requiredParameterLabels = { 0, 1})

	public NullConfiguration apply(
			final UIPluginContext context, final Petrinet net, XLog log) throws IOException {

		//-------parameter to set
		int[] stateLimits = {2,3,4,5,10};
		String outputFolderPath = "D:/Research Work/latest/Streams/Rashid Prefix Alignment/Information Systems/Results/W/With End marker/ATPE/";
		
		LinkedHashMap<String, ResultsCollection2> globalResults = new LinkedHashMap<>(); //contains the results for all n values
		
		for(int j=0; j<stateLimits.length; j++) {
			int stateLimit = stateLimits[j];
			System.out.println("APTE" + stateLimit);
			ResultsCollection2 resultsCollection2 = IncrementalPrefixAlignmentComputation_APTE.apply(net, log, stateLimit);
			globalResults.put("(" + Integer.MAX_VALUE + "/" + stateLimit + ")", resultsCollection2);
		}
		
	

		//PublishResults.writeToFilesCC(globalResults, "BPIC12", "", outputFolderPath );	
		PublishResults.writeToFilesATPE(globalResults, "BPIC12", "", outputFolderPath);	

		return null;		
	}
	
	@UITopiaVariant(author = "R. Zaman", email = "r.zaman@tue.nl", affiliation = "Eindhoven University of Technology")
	@PluginVariant(variantLabel = "01 Compute Prefix Alignments - With Bounded States", requiredParameterLabels = { 0})

	public NullConfiguration apply(
			final UIPluginContext context, final Petrinet net) throws IOException {

		//-------parameter to set
		int[] stateLimits = {2,3,4,5,10};
		
		String[] logTypes = {"a12", "a22", "a32"};
		String logType = logTypes[0];
		
		String eventLogInputFolderPath = "D:/Research Work/latest/Streams/Rashid Prefix Alignment/Process Models from Eric/Event Logs Repository/" + logType + "/timed logs/";
		String outputFolderPath = "D:/Research Work/latest/Streams/Rashid Prefix Alignment/Thesis/Bounded States/Results/W/With End marker/";
		
		
		File inputFolder = new File(eventLogInputFolderPath);
		
		for (File file : inputFolder.listFiles()) { 
			System.out.println(file.getName());

			String fileName = FilenameUtils.getBaseName(file.getName());
			String fileExtension = FilenameUtils.getExtension(file.getName());
			
			XLog log = null; 

			if(!fileExtension.equals("xes") || fileName.endsWith("25_50") || fileName.endsWith("50_2") || fileName.endsWith("50_5")) {
				System.out.println("error!! not an xes file or a wanted file");
				continue;
			}

			try {
				log = new XUniversalParser().parse(file).iterator().next();
			} catch (Exception e) {
				e.printStackTrace();
			}				
			
			LinkedHashMap<String, ResultsCollection2> globalResults = new LinkedHashMap<>(); //contains the results for all n values
			
			for(int j=0; j<stateLimits.length; j++) {
				int stateLimit = stateLimits[j];
				ResultsCollection2 resultsCollection2 = IncrementalPrefixAlignmentComputation.apply(net, log, stateLimit);
				globalResults.put("(" + Integer.MAX_VALUE + "/" + stateLimit + ")", resultsCollection2);
			}		

			PublishResults.writeToFilesCC(globalResults, fileName, "", outputFolderPath );			
		}
		
			

		return null;		
	}

}
