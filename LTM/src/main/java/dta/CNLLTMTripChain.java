package dta;

import java.util.ArrayList;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModelRoute;
import ust.hk.praisehk.metamodelcalibration.analyticalModel.AnalyticalModelTransitRoute;
import ust.hk.praisehk.metamodelcalibration.analyticalModelImpl.CNLTripChain;

public class CNLLTMTripChain extends CNLTripChain{

	public CNLLTMTripChain(Plan plan, TransitSchedule ts, Scenario scenario, Network network) {
		super(plan, ts, scenario, network);
	}
	
	@Override
	protected AnalyticalModelTransitRoute getTransitRoute(ArrayList<Leg> ptlegList, ArrayList<Activity> ptactivityList, TransitSchedule ts,Scenario scenario) {
		return new CNLTransitRouteLTM(ptlegList,ptactivityList,ts,scenario);
	}
	
	@Override
	protected AnalyticalModelRoute createRoute(Route r) {
		
		return new CNLLTMRoute(r);
	}


}
