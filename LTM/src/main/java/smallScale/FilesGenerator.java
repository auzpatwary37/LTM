package smallScale;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.Vehicles;

import ust.hk.praisehk.metamodelcalibration.measurements.Measurement;
import ust.hk.praisehk.metamodelcalibration.measurements.MeasurementType;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurements;
import ust.hk.praisehk.metamodelcalibration.measurements.MeasurementsWriter;

public class FilesGenerator {
public static void main(String[] args) {
	int popNo = 10000;
	String dataFolder = "toyScenarioData";
	Network network=NetworkGenerator.generateNetwork(ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork());
	new NetworkWriter(network).write(dataFolder+"/network.xml"); 
	Scenario scn = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	TransitSchedule ts = transitGenerator.createTransit(scn, network);
	Vehicles transitVehicles = scn.getTransitVehicles();
	new TransitScheduleWriter(ts).writeFile(dataFolder+"/transitSchedule.xml");
	new MatsimVehicleWriter(transitVehicles).writeFile(dataFolder+"/transitVehicle.xml");	
	PopulationGenerator.LoadPopulation(scn, popNo, network);
	new PopulationWriter(scn.getPopulation()).write(dataFolder+"/population"+popNo+".xml");
	new MatsimVehicleWriter(scn.getVehicles()).writeFile(dataFolder+"/vehicles"+popNo+".xml");
	Map<String,Tuple<Double,Double>> timeBean = new HashMap<>();
	for(int i = 8;i<15;i++) {
		timeBean.put(Integer.toString(i), new Tuple<>(i*3600.,(i+1)*3600.));
	}
	Measurements m = Measurements.createMeasurements(timeBean);
	network.getLinks().values().forEach(l->{
		if(l.getAllowedModes().contains("car")) {
			Measurement mm = m.createAnadAddMeasurement(l.getId().toString(), MeasurementType.linkVolume);
			ArrayList<Id<Link>> lList = new ArrayList<>();
			lList.add(l.getId());
			mm.setAttribute(Measurement.linkListAttributeName,lList);
			timeBean.keySet().forEach(t->mm.putVolume(t, 0));
		}
		});
	new MeasurementsWriter(m).write(dataFolder+"/zeroMeasurements.xml");
}
}
