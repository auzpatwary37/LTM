package nodeModels;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import linkModels.LinkModel;

public class GenericNodeModel implements NodeModel{
	
	private final Node node;
	private Map<Id<Link>,LinkModel> inLinkModels = new HashMap<>();
	private Map<Id<Link>,LinkModel> outLinkModels = new HashMap<>();
	private static final String sourceLinkId = "source";
	private static final String sinkLinkId = "sink";
	
	
	
	public GenericNodeModel(Node node, Map<Id<Link>,LinkModel>linkModels) {
		this.node = node;
		node.getInLinks().entrySet().forEach(le->{
			inLinkModels.put(le.getKey(),linkModels.get(le.getKey()));
		});
		
		node.getOutLinks().entrySet().forEach(le->{
			outLinkModels.put(le.getKey(),linkModels.get(le.getKey()));
		});
		
		
	}
	
}
