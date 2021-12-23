package org.processmining.memoryawareocc.algorithms.impl;

import java.util.ArrayList;

public class MeanMedianMode {
	public static Integer findMean(ArrayList<Integer> inputArray){
		ArrayList<Integer> temp = inputArray;
		int sum = 0;
		int average = 0;
		for(int x = 0; x < temp.size() - 1; x++)
			sum += temp.get(x);		
		average = sum / temp.size();
		return average;
	}
}
