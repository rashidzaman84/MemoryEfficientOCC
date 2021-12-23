package org.processmining.memoryawareocc.plugins;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JPanel;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.memoryawareocc.algorithms.impl.ConfigurationDialog;
import org.processmining.memoryawareocc.algorithms.impl.NullConfiguration;
import org.processmining.memoryawareocc.algorithms.impl.Pair;
//import org.processmining.memoryawareocc.algorithms.IncrementalReplayer;
//import org.processmining.memoryawareocc.parameters.IncrementalReplayerParametersImpl;
//import org.processmining.memoryawareocc.parameters.IncrementalRevBasedReplayerParametersImpl;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;


@Plugin(name = "01 Compute Prefix Alignments - With Bounded States", parameterLabels = {"Model", "Event Data" }, 
returnLabels = { "Replay Result" }, returnTypes = { NullConfiguration.class },
help = "")

public class PrefixAlignmentWithBoundedStatesPlugin {
	@UITopiaVariant(author = "R. Zaman", email = "r.zaman@tue.nl", affiliation = "Eindhoven University of Technology")
	@PluginVariant(variantLabel = "01 Compute Prefix Alignments - With Bounded States", requiredParameterLabels = { 0, 1})

	public NullConfiguration apply(
			final UIPluginContext context, final Petrinet net, XLog log) throws IOException {

		//-------parameter to set
		int stateLimit;
		ConfigurationDialog configurationDialog = new ConfigurationDialog();

		List<Pair<String, JPanel>> configurations = new LinkedList<Pair<String, JPanel>>();
		configurations.add(new Pair<String, JPanel>("State Limit Setup", configurationDialog));

		// ask the user for the configuration parameters
		InteractionResult result = InteractionResult.NEXT;
		int currentStep = 0;
		int nofSteps = configurations.size();
		boolean configurationOngoing = true;
		while (configurationOngoing && currentStep < nofSteps) {
			Pair<String, JPanel> config = configurations.get(currentStep);
			result = context.showWizard(
					config.getFirst(),
					currentStep == 0,
					currentStep == nofSteps - 1,
					config.getSecond());

			switch (result) {
				case NEXT:
					currentStep++;
					break;
				case PREV:
					currentStep--;
					break;
				case FINISHED:
					configurationOngoing = false;
					break;
				case CANCEL:
					return null;
				default:
					configurationOngoing = false;
					break;
			}
		}
		if (result != InteractionResult.FINISHED) {
			return null;
		}

		stateLimit = configurationDialog.getStateLimit();

		IncrementalPrefixAlignmentComputation.apply(net, log, stateLimit);

		return null;		
	}

}
