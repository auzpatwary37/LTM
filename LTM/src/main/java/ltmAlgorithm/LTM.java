package ltmAlgorithm;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import linkModels.GenericLinkModel;
import linkModels.LinkModel;
import nodeModels.NodeModel;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModelRoute;
import utils.LTMLoadableDemand;

public class LTM implements DNL{
	
	private final Network network;
	private Map<Id<Link>,LinkModel> linkModels = new HashMap<>();
	private Map<Id<Node>,NodeModel> nodeModels = new HashMap<>();
	private final int timeStepSize;
	
	public LTM(Network network, double minTime, double maxTime) {
		this.network = network;
		double minFFTravelTime = Double.POSITIVE_INFINITY;
		for(Link link:this.network.getLinks().values()) {
			double tt = link.getLength()/(double)link.getFreespeed();
			if(tt!=0 && tt<minFFTravelTime)minFFTravelTime = tt;
			this.linkModels.put(link.getId(), new GenericLinkModel(link));
		}
		for(Link link:this.network.getLinks().values()) {
			double tt = link.getLength()/(double)link.getFreespeed();
			if(tt!=0 && tt<minFFTravelTime)minFFTravelTime = tt;
			this.linkModels.put(link.getId(), new GenericLinkModel(link));
		}
		
		this.timeStepSize = (int)minFFTravelTime;
	}
	
	
	
	@Override
	public void performLTM(LTMLoadableDemand demand) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<String, Map<Id<AnalyticalModelRoute>, Double>> getRouteTravelTime() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Map<Id<AnalyticalModelRoute>, Double>> getTrvRouteTravelTime() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Map<Id<Link>, Double>> getLTMLinkFlow() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Map<Id<Link>, Double>> getLTMTrvLinkFlow() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Map<Id<Link>, Double>> getLTMCombinedLinkFlow() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Map<Id<Link>, Double>> getLTMLinkTravelTime() {
		// TODO Auto-generated method stub
		return null;
	}

}
