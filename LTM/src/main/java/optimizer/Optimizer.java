package optimizer;

import java.util.Map;

import utils.VariableDetails;


public interface Optimizer {
	
	public Map<String,VariableDetails> takeStep(Map<String,Double> gradient);
	public String getId();
	public Map<String,VariableDetails> getVarables();
}
