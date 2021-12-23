package org.processmining.memoryawareocc.algorithms.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.javatuples.Triplet;

public class TimeStampsBasedLogToStreamConverter {
	
	public static ArrayList<Triplet<String,String,Date>> sortEventLogByDate(XLog log){
		
		List<Triplet<String,String,Date>> eventsStream = new ArrayList<Triplet<String,String,Date>>();
		
		for (XTrace t : log) {
			for(XEvent e: t) {
				String caseId = XConceptExtension.instance().extractName(t);
				String newEventName = XConceptExtension.instance().extractName(e);
				//Pair<String,String> eventPacket = new Pair<String, String>(caseId, newEventName);
				Date date = XTimeExtension.instance().extractTimestamp(e);
				Triplet<String,String,Date> eventPacket = new Triplet<String,String,Date>(caseId, newEventName, date);				
				eventsStream.add(eventPacket);
			}
		}
		//need to sort the hashmap on date
		Comparator<Triplet<String,String,Date>> valueComparator = new Comparator<Triplet<String,String,Date>>() { 
			@Override public int compare(Triplet<String,String,Date> e1, Triplet<String,String,Date> e2) { 
				Date v1 = e1.getValue2(); 
				Date v2 = e2.getValue2(); 
				return v1.compareTo(v2);
				}
			};
		ArrayList<Triplet<String,String,Date>> entries = new ArrayList<Triplet<String,String,Date>>(eventsStream);
		//entries.addAll(eventsStream.);
		Collections.copy(entries,eventsStream);
		List<Triplet<String,String,Date>> listOfEntries = new ArrayList<Triplet<String,String,Date>>(entries);
		Collections.sort(listOfEntries, valueComparator);
	    ArrayList<Triplet<String,String,Date>> sortedByValue = new ArrayList<Triplet<String,String,Date>>(listOfEntries.size());
	    //System.out.println(sortedByValue.size());
	    for(Triplet<String,String,Date> entry : listOfEntries){
	    	sortedByValue.add(entry);
	    	}
	    //printTripletList(sortedByValue);
		return sortedByValue;
	}
}
