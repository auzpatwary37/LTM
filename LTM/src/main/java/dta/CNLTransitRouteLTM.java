package dta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import dynamicTransitRouter.fareCalculators.FareCalculator;
import transitFareAndHandler.FareLink;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.TransitDirectLink;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.TransitLink;
import ust.hk.praisehk.metamodelcalibration.analyticalModelImpl.CNLSUEModel;
import ust.hk.praisehk.metamodelcalibration.analyticalModelImpl.CNLTransitRoute;
import utils.LTMUtils;


public class CNLTransitRouteLTM extends CNLTransitRoute{

	public final String routeTransitTravelAndWaitTimeKey = "trTravelAndWaitTime";
	
	
	
	public CNLTransitRouteLTM(ArrayList<Leg> ptlegList, ArrayList<Activity> ptactivityList, TransitSchedule ts,
			Scenario scenario) {
		super(ptlegList, ptactivityList, ts, scenario);
	}

	
	
	
	
	public Tuple<Double,double[]> calcRouteUtility(LinkedHashMap<String, Double> params,LinkedHashMap<String, Double> anaParams,Network network,Map<Id<TransitLink>,TransitLink>transitLinks,Map<String,FareCalculator>farecalc,Map<String,Object> additionalDataContainer,
			Tuple<Double,Double> timeBean, String timeBeanId) {
		
		double MUTravelTime=params.get(CNLSUEModel.MarginalUtilityofTravelptName)/3600.0-params.get(CNLSUEModel.MarginalUtilityofPerformName)/3600.0;
		double MUDistance=params.get(CNLSUEModel.MarginalUtilityOfDistancePtName);
		double MUWalkTime=params.get(CNLSUEModel.MarginalUtilityOfWalkingName)/3600.0-params.get(CNLSUEModel.MarginalUtilityofPerformName)/3600.0;
		double MUWaitingTime=params.get(CNLSUEModel.MarginalUtilityofWaitingName)/3600-params.get(CNLSUEModel.MarginalUtilityofPerformName)/3600.0;
		double ModeConstant=params.get(CNLSUEModel.ModeConstantPtname);
		double MUMoney=params.get(CNLSUEModel.MarginalUtilityofMoneyName);
		double DistanceBasedMoneyCostWalk=params.get(CNLSUEModel.DistanceBasedMoneyCostWalkName);
		double fare=-1*this.getFare(this.transitSchedule, farecalc, additionalDataContainer);
		//Tuple<Tuple<Double,RealVector>,Tuple<Double,RealVector>> trTravelAndWaitTime;
		double travelTime = 0;
		double waitingTime = 0;
		double[] ttGrad = null;
		double[] twGrad = null;
		Map<NetworkRoute, Map<String, Map<String, Tuple<Tuple<Double, double[]>,Tuple<Double, double[]>>>>> trTravelAndWaitTime = (Map<NetworkRoute, Map<String, Map<String, Tuple<Tuple<Double, double[]>, Tuple<Double, double[]>>>>>) additionalDataContainer.get("transit");
		if(trTravelAndWaitTime==null) {
			travelTime = this.getFreeFlowTravelTime(network);
		}else {
			for(TransitDirectLink tdl:this.getTransitDirectLinks()) {
				NetworkRoute r = tdl.getTs().getTransitLines().get(Id.create(tdl.getLineId(), TransitLine.class)).getRoutes().get(Id.create(tdl.getRouteId(), TransitRoute.class)).getRoute();
				Tuple<Tuple<Double, double[]>, Tuple<Double, double[]>> tw = trTravelAndWaitTime.get(r).get(timeBeanId).get(tdl.getStartingLinkId().toString()+"___"+tdl.getEndingLinkId().toString());
				travelTime+=tw.getFirst().getFirst();
				waitingTime+=tw.getSecond().getFirst();
				if(ttGrad == null)ttGrad = tw.getFirst().getSecond();
				else ttGrad = LTMUtils.sum(ttGrad, tw.getFirst().getSecond());
				
				if(twGrad==null)twGrad = tw.getSecond().getSecond();
				else twGrad = LTMUtils.sum(twGrad, tw.getSecond().getSecond());
			}
		}
		
		double walkTime=this.getRouteWalkingDistance()/1.4;
		double walkDist=this.getRouteWalkingDistance();
		
		double distance=this.calcRouteDistance(network);
		double utility=0;
		double MUTransfer=params.get(CNLSUEModel.UtilityOfLineSwitchName);
		double[] utilityGrad = new double[ttGrad.length];
		
		
		
		utility=ModeConstant+
				travelTime*MUTravelTime+
				MUMoney*fare+
				MUWalkTime*walkTime+
				MUMoney*DistanceBasedMoneyCostWalk*walkDist+
				MUWaitingTime*waitingTime
				+MUTransfer*(this.getTransitTransferLinks().size()-1)
				+MUDistance*distance*MUMoney;
		
		
		for(int i =0; i<ttGrad.length;i++) {
			utilityGrad[i] = (MUTravelTime*ttGrad[i] + MUWaitingTime*twGrad[i])*anaParams.get(CNLSUEModel.LinkMiuName);
		}
		
		
		if(utility==0 ||Double.isNaN(utility)) {
			logger.warn("Stop!!! route utility is zero or infinity.");
		}
		return new Tuple<>(utility*anaParams.get(CNLSUEModel.LinkMiuName),utilityGrad);
	}
	
	public double getFreeFlowTravelTime(Network network) {
		double time = 0;
		for(TransitDirectLink tdl:this.getTransitDirectLinks()) {
			for(Id<Link>l:tdl.getLinkList()) {
				time+=network.getLinks().get(l).getLength()/network.getLinks().get(l).getFreespeed();
			}
		}
		return time;
	}
	public double getFreeFlowTravelTime(TransitDirectLink tdl, Network network) {
		double time = 0;
		
		for(Id<Link>l:tdl.getLinkList()) {
			time+=network.getLinks().get(l).getLength()/network.getLinks().get(l).getFreespeed();
		}
		
		return time;
	}
	
	public Map<String,Set<TransitDirectLink>> getDirectLinkUsage(Network network, Map<String,Tuple<Double,Double>>timeBean, String departingTimeId,Map<String, Object> additionalDataContainer){
		Map<String,Set<TransitDirectLink>> links = new HashMap<>();
		double time = timeBean.get(departingTimeId).getFirst()+Math.random()*(timeBean.get(departingTimeId).getSecond()-timeBean.get(departingTimeId).getFirst());
		Map<NetworkRoute, Map<String, Map<String, Tuple<Tuple<Double, double[]>,Tuple<Double, double[]>>>>> trTravelAndWaitTime = (Map<NetworkRoute, Map<String, Map<String, Tuple<Tuple<Double, double[]>, Tuple<Double, double[]>>>>>) additionalDataContainer.get("transit");
		for(TransitDirectLink tdl:this.getTransitDirectLinks()) {
			String timeBeanId = this.getTimeId(time, timeBean);
			if(!links.containsKey(timeBeanId))links.put(timeBeanId, new HashSet<>());
			links.get(timeBeanId).add(tdl);
			NetworkRoute r = tdl.getTs().getTransitLines().get(Id.create(tdl.getLineId(), TransitLine.class)).getRoutes().get(Id.create(tdl.getRouteId(), TransitRoute.class)).getRoute();
			if(trTravelAndWaitTime!=null) {
				Tuple<Tuple<Double, double[]>, Tuple<Double, double[]>> tw = trTravelAndWaitTime.get(r).get(timeBeanId).get(tdl.getStartingLinkId().toString()+"___"+tdl.getEndingLinkId().toString());
				time+=tw.getFirst().getFirst();
				time+=tw.getSecond().getFirst();
			}else {
				time+=this.getFreeFlowTravelTime(tdl, network);
			}
			
		}
		
		return links;
	}
	
	public Map<String,Set<FareLink>> getFareLinkUsage(Network network, Map<String,Tuple<Double,Double>>timeBean, String departingTimeId,Map<String, Object> additionalDataContainer){
		Map<String,Set<FareLink>> links = new HashMap<>();
		double time = timeBean.get(departingTimeId).getFirst()+Math.random()*(timeBean.get(departingTimeId).getSecond()-timeBean.get(departingTimeId).getFirst());
		Map<NetworkRoute, Map<String, Map<String, Tuple<Tuple<Double, double[]>,Tuple<Double, double[]>>>>> trTravelAndWaitTime = (Map<NetworkRoute, Map<String, Map<String, Tuple<Tuple<Double, double[]>, Tuple<Double, double[]>>>>>) additionalDataContainer.get("transit");
		Map<Id<TransitLink>,FareLink> startingLinkMap = new HashMap<>();
		
		for(FareLink l:this.getFareLinks()) {
			for(TransitDirectLink tdl:this.getTransitDirectLinks()) {
				if(tdl.getStartStopId().equals(l.getBoardingStopFacility().toString())) {
					startingLinkMap.put(tdl.getTrLinkId(),l);
					break;
				}
			}
		}
		for(TransitDirectLink tdl:this.getTransitDirectLinks()) {
			String timeBeanId = this.getTimeId(time, timeBean);
			
			if(!links.containsKey(timeBeanId) && startingLinkMap.containsKey(tdl.getTrLinkId()))links.put(timeBeanId, new HashSet<>());
			if(startingLinkMap.containsKey(tdl.getTrLinkId()))links.get(timeBeanId).add(startingLinkMap.get(tdl.getTrLinkId()));
			NetworkRoute r = tdl.getTs().getTransitLines().get(Id.create(tdl.getLineId(), TransitLine.class)).getRoutes().get(Id.create(tdl.getRouteId(), TransitRoute.class)).getRoute();
			if(trTravelAndWaitTime!=null) {
				Tuple<Tuple<Double, double[]>, Tuple<Double, double[]>> tw = trTravelAndWaitTime.get(r).get(timeBeanId).get(tdl.getStartingLinkId().toString()+"___"+tdl.getEndingLinkId().toString());
				time+=tw.getFirst().getFirst();
				time+=tw.getSecond().getFirst();
			}else {
				time+=this.getFreeFlowTravelTime(tdl, network);
			}
			
		}
		
		return links;
	}
	
	public double calcRouteDistance(Network net) {
		double d = 0;
		for(TransitDirectLink dl:this.getTransitDirectLinks()){
			for(Id<Link> l:dl.getLinkList()) {
				d+=net.getLinks().get(l).getLength();
			}
		}
		return d;
	}
}
