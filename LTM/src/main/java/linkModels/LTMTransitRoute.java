package linkModels;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModelNetwork;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.TransitDirectLink;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.TransitLink;
import ust.hk.praisehk.metamodelcalibration.analyticalModelImpl.CNLTransitDirectLink;
import ust.hk.praisehk.metamodelcalibration.analyticalModelImpl.CNLTransitRoute;
import ust.hk.praisehk.metamodelcalibration.analyticalModelImpl.CNLTransitTransferLink;



public class LTMTransitRoute extends CNLTransitRoute{

	public LTMTransitRoute(ArrayList<Leg> ptlegList, ArrayList<Activity> ptactivityList, TransitSchedule ts,
			Scenario scenario) {
		super(ptlegList, ptactivityList, ts, scenario);
		
	}
	
	public LTMTransitRoute(CNLTransitRoute tr, Scenario scenario, TransitSchedule ts) {
		super((List<CNLTransitTransferLink>)(List<?>)tr.getTransitTransferLinks(), (List<CNLTransitDirectLink>)(List<?>)tr.getTransitDirectLinks(), 
				scenario , ts, tr.getRouteWalkingDistance(), tr.getTrRouteId().toString());
	}
	
	@Override
	public double calcRouteTravelTime(AnalyticalModelNetwork network,Map<Id<TransitLink>,TransitLink>transitLinks, Tuple<Double,Double>timeBean,LinkedHashMap<String,Double>params,LinkedHashMap<String,Double>anaParams) {
		double routeTravelTime=0;
		for(TransitDirectLink dlink:this.getTransitDirectLinks()) {
			routeTravelTime+=dlink.getLinkTravelTime(network,timeBean,params,anaParams);
		}
		return routeTravelTime;
	}

}
