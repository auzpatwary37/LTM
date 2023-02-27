package siouxFalls;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;

import ust.hk.praisehk.metamodelcalibration.matsimIntegration.LinkCountEventHandler;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurement;
import ust.hk.praisehk.metamodelcalibration.measurements.MeasurementType;
import ust.hk.praisehk.metamodelcalibration.measurements.Measurements;
import ust.hk.praisehk.metamodelcalibration.measurements.MeasurementsWriter;

public class RunScenario {
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, "siouxfalls-2014/config_default.xml");
		config.plans().setInputFile("siouxfalls-2014/Siouxfalls_population.xml.gz");
		config.facilities().setInputFile("siouxfalls-2014/Siouxfalls_facilities.xml.gz");
		config.network().setInputFile("siouxfalls-2014/Siouxfalls_network_PT.xml");
		config.transit().setTransitScheduleFile("siouxfalls-2014/Siouxfalls_transitSchedule.xml");
		config.transit().setVehiclesFile("siouxfalls-2014/Siouxfalls_vehicles.xml");
	
		config.controler().setOutputDirectory("outputSiouxFalls/");
		config.controler().setOverwriteFileSetting(
				OverwriteFileSetting.deleteDirectoryIfExists);
		
		config.qsim().setStartTime(0);
		config.qsim().setEndTime(27*3600);
		
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(250);
		config.strategy().setFractionOfIterationsToDisableInnovation(.8);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Map<String,Tuple<Double,Double>> timeBean = new HashMap<>();
		for(int i = 1;i<=24;i++) {
			timeBean.put(Integer.toString(i), new Tuple<>((i-1)*3600.,i*3600.));
		}
		
		Measurements mm = Measurements.createMeasurements(timeBean);
		scenario.getNetwork().getLinks().entrySet().forEach(l->{
			Measurement m = mm.createAnadAddMeasurement(l.getKey().toString(), MeasurementType.linkVolume);
			ArrayList<Id<Link>> links = new ArrayList<>();
			links.add(l.getKey());
			m.setAttribute(Measurement.linkListAttributeName,links);
			for(String s:timeBean.keySet()) {
				m.putVolume(s, 0);
			}
		});
		new MeasurementsWriter(mm).write("siouxFallsEmptyLinkVolumes.xml");
		
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				LinkCountEventHandler lc=new LinkCountEventHandler(mm);
				this.addEventHandlerBinding().toInstance(lc);
				bind(LinkCountEventHandler.class).toInstance(lc);
			}
			
		});
		controler.run();
		
		new MeasurementsWriter(mm).write("siouxFallsSimulatedLinkVolumes.xml");
	}
}
