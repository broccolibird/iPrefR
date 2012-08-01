package verify;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import model.OutcomeSequence;
import model.PerformanceRecord;
import model.PreferenceMetaData;
import model.WorkingPreferenceModel;

import util.Constants;

/**
 * A parser for traces generated by NuSMV model checker. 
 * @author gsanthan
 *
 */
public class NuSMVTraceFormatter implements TraceFormatter {

	/*
	 * (non-Javadoc)
	 * @see verify.TraceFormatter#parseCycleFromTrace(model.PreferenceMetaData)
	 */
	public OutcomeSequence parseCycleFromTrace(PreferenceMetaData pmd) throws FileNotFoundException, IOException {
		BufferedReader reader = new BufferedReader(new FileReader(pmd.getCounterExampleFile()));
		OutcomeSequence c = new OutcomeSequence();
		String nextLine = reader.readLine();
		Set<String> outcome = new HashSet<String>();
		Set<String> previousOutcome = null;
		try{
			boolean loopStarted = false;
			do {
				if(nextLine.indexOf("Loop starts here") != -1) {
					loopStarted = true;
				}
				if (nextLine.indexOf("State: ") != -1) {
					outcome = parseOutcome(previousOutcome, nextLine, reader);
					if(nextLine != null && nextLine.indexOf("Loop starts here") != -1) {
						loopStarted = true;
					}
					if(loopStarted && previousOutcome == null) {
						Set<String> tempOutcome = new HashSet<String>(outcome);
						c.addOutcome(tempOutcome);
					} else if(loopStarted && !(previousOutcome.containsAll(outcome) && outcome.containsAll(previousOutcome))) {
						Set<String> tempOutcome = new HashSet<String>(outcome);
						c.addOutcome(tempOutcome);
					}
					previousOutcome = new HashSet<String>(outcome);
				}
				
				nextLine = reader.readLine();
				
			} while (nextLine != null);
		}finally{reader.close();}
		return c;
	}
	
	/*
	 * (non-Javadoc)
	 * @see verify.TraceFormatter#parsePathFromTrace(model.PreferenceMetaData)
	 */
	public OutcomeSequence parsePathFromTrace(PreferenceMetaData pmd) throws FileNotFoundException, IOException {
		BufferedReader reader = new BufferedReader(new FileReader(pmd.getCounterExampleFile()));
		OutcomeSequence c = new OutcomeSequence();
		String nextLine = reader.readLine();
		Set<String> outcome = new HashSet<String>();
		Set<String> previousOutcome = null;
		try{
			do {
				if (nextLine.indexOf("State: ") != -1) {
					outcome = parseOutcome(previousOutcome, nextLine, reader);
					if(previousOutcome == null) {
						Set<String> tempOutcome = new HashSet<String>(outcome);
						c.addOutcome(tempOutcome);
					} else if(!(previousOutcome.containsAll(outcome) && outcome.containsAll(previousOutcome))) {
						Set<String> tempOutcome = new HashSet<String>(outcome);
						c.addOutcome(tempOutcome);
					}
					previousOutcome = new HashSet<String>(outcome);
				}
				nextLine = reader.readLine();
			} while (nextLine != null);
		}finally{reader.close();}
		return c;
	}
	
	/**
	 * Parses and returns an outcome by making delta changes to the previously parsed outcome from the current position of the BufferedReader
	 * @param previousOutcome
	 * @param nextLine
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	private Set<String> parseOutcome(Set<String> previousOutcome, String nextLine, BufferedReader reader) throws IOException {
		Set<String> outcome = new HashSet<String>();// First state/outcome in the trace
		if(previousOutcome != null) {
			outcome = new HashSet<String>(previousOutcome); // Some intermediate state/outcome in the trace
		}
		
		do {
			for (String var : WorkingPreferenceModel.getPrefMetaData().getVariables()) {
				if (nextLine.indexOf(" " + var + " = ") != -1) {
					nextLine = nextLine.trim();
					String varValuation = nextLine.substring(nextLine.indexOf("=") + 2);
					if (varValuation.equalsIgnoreCase("1") || varValuation.equalsIgnoreCase("TRUE")) {
						outcome.add(var);
						// variableAssigned = true;//This means there is a new outcome to be added
					} else if (varValuation.equalsIgnoreCase("0") || varValuation.equalsIgnoreCase("FALSE")) {
						outcome.remove(var);
						// variableAssigned = true;//This means there is a new outcome to be added
					}
				}
			}
			nextLine = reader.readLine();
		} while (nextLine != null && !nextLine.startsWith("->") && !nextLine.startsWith("--"));
		return outcome;
	}
	
	/*
	 * (non-Javadoc)
	 * @see verify.TraceFormatter#parseCounterExampleFromTrace(model.PreferenceMetaData)
	 */
	public String[] parseCounterExampleFromTrace(PreferenceMetaData pmd) throws FileNotFoundException, IOException {
		
		Set<String> variablesTrueInOutcome = new HashSet<String>();
		if(Constants.CURRENT_MODEL_CHECKER==Constants.MODEL_CHECKER.NuSMV) {
			BufferedReader reader = new BufferedReader (new FileReader(pmd.getCounterExampleFile()));
			try{
			String nextLine;
			while((nextLine = reader.readLine()) != null) {
				for (String var : pmd.getVariables()) {
					if(nextLine.indexOf(" " + var + " = ") != -1) {
						nextLine = nextLine.trim();
						String varValuation = nextLine.substring(nextLine.length()-1);
						if(varValuation.equalsIgnoreCase("1")) {
							variablesTrueInOutcome.add(var);
						} else if(varValuation.equalsIgnoreCase("0")){
							variablesTrueInOutcome.remove(var);
						}
					}					
				}
			}
			}finally{reader.close();}
		} else if (Constants.CURRENT_MODEL_CHECKER==Constants.MODEL_CHECKER.CadenceSMV) {
			BufferedReader reader = new BufferedReader (new FileReader(pmd.getCounterExampleFile()));
			String nextLine;
			try{
			while((nextLine = reader.readLine()) != null) {
				for (String var : pmd.getVariables()) {
					if(nextLine.indexOf("\\" + var + "  = ") != -1) {
						nextLine = nextLine.trim();
						String varValuation = "";
						if(!nextLine.endsWith(",")) {
							varValuation = nextLine.substring(nextLine.length()-1);
						} else {
							varValuation = nextLine.substring(nextLine.length()-2,nextLine.length()-1);
						}
						if(varValuation.equalsIgnoreCase("1")) {
							variablesTrueInOutcome.add(var);
						} else if(varValuation.equalsIgnoreCase("0")){
							variablesTrueInOutcome.remove(var);
						}
					}					
				}
			}
			}finally{reader.close();}
		}
		return variablesTrueInOutcome.toArray(new String[]{});
	}

	/*
	 * (non-Javadoc)
	 * @see verify.TraceFormatter#parseCounterExampleWithChangeVariablesFromTrace(model.PreferenceMetaData)
	 */
	public String[] parseCounterExampleWithChangeVariablesFromTrace(PreferenceMetaData pmd) throws FileNotFoundException, IOException {
		
		Set<String> variablesTrueInOutcome = new HashSet<String>();
			BufferedReader reader = new BufferedReader (new FileReader(pmd.getCounterExampleFile()));
			String nextLine;
			try{
			while((nextLine = reader.readLine()) != null) {
				for (String var : pmd.getVariables()) {
					if(nextLine.indexOf(" " + var + " = ") != -1) {
						nextLine = nextLine.trim();
						String varValuation = nextLine.substring(nextLine.length()-1);
						if(varValuation.equalsIgnoreCase("1")) {
							variablesTrueInOutcome.add(var);
						} else if(varValuation.equalsIgnoreCase("0")){
							variablesTrueInOutcome.remove(var);
						}
					}
					
					if(nextLine.indexOf(" ch" + var + " = ") != -1) {
						nextLine = nextLine.trim();
						String varValuation = nextLine.substring(nextLine.length()-1);
						if(varValuation.equalsIgnoreCase("1")) {
							variablesTrueInOutcome.add("ch"+var);
						} else if(varValuation.equalsIgnoreCase("0")){
							variablesTrueInOutcome.remove("ch"+var);
						}
					}			
				}
			}
			}finally{reader.close();}
		return variablesTrueInOutcome.toArray(new String[]{});
	}

	/*
	 * (non-Javadoc)
	 * @see verify.TraceFormatter#formatResults(java.lang.String)
	 */
	public String formatResults(String fileName) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see verify.TraceFormatter#getPerformanceRecord(java.lang.String)
	 */
	public PerformanceRecord getPerformanceRecord(String fileName)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
}	