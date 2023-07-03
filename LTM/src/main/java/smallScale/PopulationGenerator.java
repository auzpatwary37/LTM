package smallScale;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

public class PopulationGenerator {
	public static void addPlanParameter(PlanCalcScoreConfigGroup config, String name, int typicalDuration){
		ActivityParams act = new ActivityParams(name);
		act.setTypicalDuration(typicalDuration);
		config.addActivityParams(act);
	}
	
	public static void LoadPopulation(Scenario scenario, int popNo,Network network) {

		//creating people going from 1-4
		Vehicles v=scenario.getVehicles();
		VehiclesFactory vf=scenario.getVehicles().getFactory();
		VehicleType vt1=vf.createVehicleType(Id.create("car", VehicleType.class));
		v.addVehicleType(vt1);
		Double rand=null;
		
		int j=0;
		int popPerOD=popNo/4;
		for(int i=0;i<popPerOD;i++) {
			rand=1800*Math.random()-1800*Math.random();
			createOnePerson(scenario,scenario.getPopulation(),vt1,i,new Coord(0,20000),
					new Coord(30010,20000),rand.intValue());
			j++;
		}
		for(int i=0;i<popPerOD;i++) {
			rand=1800*Math.random()-1800*Math.random();
			createOnePerson(scenario,scenario.getPopulation(),vt1,j,new Coord(0,20000),
					new Coord(15000,0),rand.intValue());
			j++;
		}
		for(int i=0;i<popPerOD;i++) {
			rand=1800*Math.random()-1800*Math.random();
			createOnePerson(scenario,scenario.getPopulation(),vt1,j,new Coord(10000,20000),
					new Coord(15000,0),rand.intValue());
			j++;
		}
		for(int i=0;i<popPerOD;i++) {
			rand=1800*Math.random()-1800*Math.random();
			createOnePerson(scenario,scenario.getPopulation(),vt1,j,new Coord(10000,20000),
					new Coord(30010,20000),rand.intValue());
			j++;
		}
	}
	private static void createOnePerson(Scenario scenario,
			Population population, VehicleType vt, int i, Coord coord, Coord coordWork, int time_offset) {
		String personId = "p_"+i+"_"+time_offset;
		Person person = population.getFactory().createPerson(Id.createPersonId(personId)); //Create person

		//Create and add vehicle for this person
		Vehicles vehicles = scenario.getVehicles();
		VehiclesFactory vf = vehicles.getFactory();
		Vehicle v = vf.createVehicle(Id.createVehicleId(personId), vt);
		vehicles.addVehicle(v);
		

		Plan plan = population.getFactory().createPlan();

		Activity home = population.getFactory().createActivityFromCoord("home", coord);
		home.setEndTime(9*60*60+time_offset);
		plan.addActivity(home);

		Leg hinweg;
		if(i%2==0){
			hinweg = population.getFactory().createLeg("car");
		}
		else{
			hinweg = population.getFactory().createLeg("pt");
		}
		plan.addLeg(hinweg);

		Activity work = population.getFactory().createActivityFromCoord("work", coordWork);
		work.setEndTime(17*60*60+time_offset);
		plan.addActivity(work);

		Leg rueckweg;
		if(i%2==0){
			rueckweg = population.getFactory().createLeg("car");
		}
		else{
			rueckweg = population.getFactory().createLeg("pt");
		}
		plan.addLeg(rueckweg);

		Activity home2 = population.getFactory().createActivityFromCoord("home", coord);
		plan.addActivity(home2);

		person.addPlan(plan);
		Map<String,Id<Vehicle>> vSet = new HashMap<>();
		vSet.put("car", v.getId());
		VehicleUtils.insertVehicleIdsIntoAttributes(person, vSet);
		population.addPerson(person);
	}


private static void createOnePerson(Scenario scenario,
		Population population, VehicleType vt, int i, Link homeLink, Link workLink, int time_offset) {
	String personId = "p_"+i+"_"+time_offset;
	Person person = population.getFactory().createPerson(Id.createPersonId(personId)); //Create person

	//Create and add vehicle for this person
	Vehicles vehicles = scenario.getVehicles();
	VehiclesFactory vf = vehicles.getFactory();
	Vehicle v = vf.createVehicle(Id.createVehicleId(personId), vt);
	vehicles.addVehicle(v);

	Plan plan = population.getFactory().createPlan();

	Activity home = population.getFactory().createActivityFromLinkId("home", homeLink.getId());
	home.setCoord(homeLink.getCoord());
	home.setEndTime(9*60*60+time_offset);
	plan.addActivity(home);

	Leg hinweg;
	if(i%2==0){
		hinweg = population.getFactory().createLeg("car");
	}
	else{
		hinweg = population.getFactory().createLeg("pt");
	}
	plan.addLeg(hinweg);

	Activity work = population.getFactory().createActivityFromLinkId("work", workLink.getId());
	work.setCoord(workLink.getCoord());
	work.setEndTime(17*60*60+time_offset);
	plan.addActivity(work);

	Leg rueckweg;
	if(i%2==0){
		rueckweg = population.getFactory().createLeg("car");
	}
	else{
		rueckweg = population.getFactory().createLeg("pt");
	}
	plan.addLeg(rueckweg);

	Activity home2 = population.getFactory().createActivityFromLinkId("home", homeLink.getId());
	home2.setCoord(homeLink.getCoord());
	plan.addActivity(home2);

	person.addPlan(plan);
	population.addPerson(person);
}
}
