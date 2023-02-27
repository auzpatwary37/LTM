package siouxFalls;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class SiouxFallsOD {
public static void main(String[] args) throws IOException {
	Config config = ConfigUtils.createConfig();
	config.plans().setInputFile("outputSiouxFalls/output_plans.xml.gz");
	config.transit().setTransitScheduleFile("siouxfalls-2014/Siouxfalls_transitSchedule.xml");
	Scenario scenario = ScenarioUtils.loadScenario(config);
	Population population = scenario.getPopulation();
	FileWriter fw = new FileWriter(new File("outputSiouxFalls/actLocs.csv"));
	fw.append("x,y\n");
	fw.flush();
	for(Person p:population.getPersons().values()) {
		p.getSelectedPlan().getPlanElements().forEach(pl->{
			if(pl instanceof Activity) {
				Activity a = (Activity)pl;
				try {
					fw.append(a.getCoord().getX()+","+a.getCoord().getY()+"\n");
					fw.flush();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}
	fw.close();
	Set<String> trModes = new HashSet<>();
	scenario.getTransitSchedule().getTransitLines().values().forEach(tl->{
		tl.getRoutes().values().forEach(tr->{
			trModes.add(tr.getTransportMode());
			
		});
	});
	
	trModes.forEach(m->System.out.println(m));
	//"bus" is the only transit mode.
}
}
