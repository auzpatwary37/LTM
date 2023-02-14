package dta;

import java.util.ArrayList;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import ust.hk.praisehk.metamodelcalibration.analyticalModelImpl.CNLTransitRoute;

public class CNLTransitRouteLTM extends CNLTransitRoute{

	public CNLTransitRouteLTM(ArrayList<Leg> ptlegList, ArrayList<Activity> ptactivityList, TransitSchedule ts,
			Scenario scenario) {
		super(ptlegList, ptactivityList, ts, scenario);
	}

	
}
