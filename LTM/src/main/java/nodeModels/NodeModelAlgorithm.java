package nodeModels;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import linkModels.LinkModel;
import utils.TuplesOfThree;

public interface NodeModelAlgorithm {

	public TuplesOfThree<Map<Id<Link>,Map<Id<Link>,double[]>>,
	Map<Id<Link>,Map<Id<Link>,double[]>>,Map<Id<Link>,
	Map<Id<Link>,double[][]>>> applyNodeModel(int timeStep,
			TuplesOfThree<Map<Id<Link>,Map<Id<Link>,double[]>>,
			Map<Id<Link>,Map<Id<Link>,double[]>>,Map<Id<Link>,
			Map<Id<Link>,double[][]>>> S_ij,
			TuplesOfThree<Map<Id<Link>,double[]>,
			Map<Id<Link>,double[]>,Map<Id<Link>,double[][]>> S_i,
			TuplesOfThree<Map<Id<Link>,double[][]>,
			Map<Id<Link>,double[][]>,Map<Id<Link>,double[][][]>> S_ir,
			TuplesOfThree<Map<Id<Link>,double[]>,
			Map<Id<Link>,double[]>,Map<Id<Link>,double[][]>> R_j,
			Node node, Map<Id<Link>,LinkModel> inLinkModels, 
			Map<Id<Link>,LinkModel> outLinkModel
			);
	
}
