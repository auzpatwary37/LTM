package smallScale;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.Vehicles;

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
	
}
}
