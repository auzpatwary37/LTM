package dta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math.linear.MatrixUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;

import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModelNetwork;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModelRoute;
import ust.hk.praisehk.metamodelcalibration.analyticalModelImpl.CNLRoute;
import ust.hk.praisehk.metamodelcalibration.analyticalModelImpl.CNLSUEModel;

public class CNLLTMRoute extends CNLRoute{
	
	public final String routeTravelTimeKey = "routeTravelTime";

	public CNLLTMRoute(Id<AnalyticalModelRoute> routeId, ArrayList<Id<Link>> linkList, double dist,
			List<PlanElement> pe) {
		super(routeId, linkList, dist, pe);
		
	}
	public CNLLTMRoute(Route r) {
		super(r);
	}
	
	@Override
	public double getTravelTime(AnalyticalModelNetwork network, Tuple<Double, Double> timeBean,
			LinkedHashMap<String, Double> params, LinkedHashMap<String, Double> anaParams,
			Map<String, Object> additionalDataContainer) {
		
		Tuple<Double,double[]> travelTime = (Tuple<Double,double[]>)additionalDataContainer.get(routeTravelTimeKey);
		
		return travelTime.getFirst();
	}
	
	public Tuple<Double,double[]> calcRouteUtility(LinkedHashMap<String, Double> parmas,LinkedHashMap<String, Double> anaParmas,Network network,Tuple<Double,Double>timeBean,String timeBeanId,Map<String, ? extends Object> additionalDataContainer) {
		Map<NetworkRoute, Map<String, Tuple<Double, double[]>>> tt = (Map<NetworkRoute, Map<String, Tuple<Double, double[]>>>)additionalDataContainer.get("car");
		
		
		double MUTravelTime=parmas.get(CNLSUEModel.MarginalUtilityofTravelCarName)/3600.0-parmas.get(CNLSUEModel.MarginalUtilityofPerformName)/3600.0;
		double ModeConstant;
		if(parmas.get(CNLSUEModel.ModeConstantCarName)==null) {
			ModeConstant=0;
		}else {
			ModeConstant=parmas.get(CNLSUEModel.ModeConstantCarName);
		}
		Double MUMoney=parmas.get(CNLSUEModel.MarginalUtilityofMoneyName);
		if(MUMoney==null) {
			MUMoney=1.;
		}
		Double DistanceBasedMoneyCostCar=parmas.get(CNLSUEModel.DistanceBasedMoneyCostCarName);
		if(DistanceBasedMoneyCostCar==null) {
			DistanceBasedMoneyCostCar=0.;
		}
		double MUDistanceCar=parmas.get(CNLSUEModel.MarginalUtilityofDistanceCarName);
		double travelTime = 0;
		double[] travelTimeGrad = null;
		if(tt==null)travelTime = this.getFreeFlowTravelTime(network);
		else {
			travelTime = tt.get((NetworkRoute)this.getRoute()).get(timeBeanId).getFirst();
			travelTimeGrad = tt.get((NetworkRoute)this.getRoute()).get(timeBeanId).getSecond();
		}
		double utility=ModeConstant+
				travelTime*MUTravelTime+
				(MUDistanceCar+MUMoney*DistanceBasedMoneyCostCar)*this.getRouteDistance();
		double[] utilityGrad = MatrixUtils.createRealVector(travelTimeGrad).mapMultiply(MUTravelTime*anaParmas.get(CNLSUEModel.LinkMiuName)).toArray();
		this.RouteUtility = utility;
 		return new Tuple<>(this.RouteUtility*anaParmas.get(CNLSUEModel.LinkMiuName),utilityGrad);
	}
	
	public double getFreeFlowTravelTime(Network network) {
		double tt = 0;
		for(Id<Link>l:this.getLinkIds()) {
			tt+=network.getLinks().get(l).getLength()/network.getLinks().get(l).getFreespeed();
		}
		return tt;
	}

}
