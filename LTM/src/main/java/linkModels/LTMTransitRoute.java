package linkModels;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import dynamicTransitRouter.fareCalculators.FareCalculator;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModelNetwork;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.TransitLink;
import ust.hk.praisehk.metamodelcalibration.analyticalModelImpl.CNLSUEModel;
import ust.hk.praisehk.metamodelcalibration.analyticalModelImpl.CNLTransitDirectLink;
import ust.hk.praisehk.metamodelcalibration.analyticalModelImpl.CNLTransitRoute;
import ust.hk.praisehk.metamodelcalibration.analyticalModelImpl.routeInfoOut;


public class LTMTransitRoute extends CNLTransitRoute{

	public LTMTransitRoute(ArrayList<Leg> ptlegList, ArrayList<Activity> ptactivityList, TransitSchedule ts,
			Scenario scenario) {
		super(ptlegList, ptactivityList, ts, scenario);
		
	}
	
	@Override
	public double calcRouteTravelTime(AnalyticalModelNetwork network,Map<Id<TransitLink>,TransitLink>transitLinks, Tuple<Double,Double>timeBean,LinkedHashMap<String,Double>params,LinkedHashMap<String,Double>anaParams) {
		double routeTravelTime=0;
		for(CNLTransitDirectLink dlink:this.directLinks) {
			routeTravelTime+=dlink.getLinkTravelTime(network,timeBean,params,anaParams);
		}
		return routeTravelTime;
	}

}
