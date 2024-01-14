package smallScale;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class NetworkGenerator {
	
	public static Network generateNetwork(Network network) {

		NetworkFactory netfac=network.getFactory();

		HashMap<String,Node> NodeList=new HashMap<>();
		HashMap<String,Link> LinkList=new HashMap<>();

		NodeList.put("1",netfac.createNode(Id.createNodeId("1"), new Coord(0,20000)));
		NodeList.put("1r",netfac.createNode(Id.createNodeId("1r"), new Coord(100,20000)));
		NodeList.put("2",netfac.createNode(Id.createNodeId("2"), new Coord(10000,20000)));
		NodeList.put("2r",netfac.createNode(Id.createNodeId("2r"), new Coord(10100,20000)));
		NodeList.put("3",netfac.createNode(Id.createNodeId("3"), new Coord(20000,20000)));
		NodeList.put("3r",netfac.createNode(Id.createNodeId("3r"), new Coord(20100,20000)));
		NodeList.put("4",netfac.createNode(Id.createNodeId("4"), new Coord(30000,20000)));
		NodeList.put("4r",netfac.createNode(Id.createNodeId("4r"), new Coord(30100,20000)));
		NodeList.put("5",netfac.createNode(Id.createNodeId("5"), new Coord(10000,10000)));
		NodeList.put("5r",netfac.createNode(Id.createNodeId("5r"), new Coord(10100,10000)));
		NodeList.put("6",netfac.createNode(Id.createNodeId("6"), new Coord(20000,10000)));
		NodeList.put("6r",netfac.createNode(Id.createNodeId("6r"), new Coord(20100,10000)));
		NodeList.put("7",netfac.createNode(Id.createNodeId("7"), new Coord(15000,0)));
		NodeList.put("7r",netfac.createNode(Id.createNodeId("7r"), new Coord(15100,0)));


		//create car Links

		Link l=netfac.createLink(Id.createLinkId("1r_2"), NodeList.get("1r"), NodeList.get("2"));
		LinkList.put(l.getId().toString(),setLinkAttributeCar(l));
		Link l1=createoppositeLink(l, netfac);
		LinkList.put(l1.getId().toString(),l1);
		
		l=netfac.createLink(Id.createLinkId("2r_3"), NodeList.get("2r"), NodeList.get("3"));
		LinkList.put(l.getId().toString(),setLinkAttributeCar(l));
		l1=createoppositeLink(l, netfac);
		LinkList.put(l1.getId().toString(),l1);
		
		l=netfac.createLink(Id.createLinkId("3r_4"), NodeList.get("3r"), NodeList.get("4"));
		LinkList.put(l.getId().toString(),setLinkAttributeCar(l));
		l1=createoppositeLink(l, netfac);
		LinkList.put(l1.getId().toString(),l1);
		
		l=netfac.createLink(Id.createLinkId("1r_5"), NodeList.get("1r"), NodeList.get("5"));
		LinkList.put(l.getId().toString(),setLinkAttributeCar(l));
		l1=createoppositeLink(l, netfac);
		LinkList.put(l1.getId().toString(),l1);
		
		l=netfac.createLink(Id.createLinkId("2r_5"), NodeList.get("2r"), NodeList.get("5"));
		LinkList.put(l.getId().toString(),setLinkAttributeCar(l));
		l1=createoppositeLink(l, netfac);
		LinkList.put(l1.getId().toString(),l1);
		
		l=netfac.createLink(Id.createLinkId("5r_6"), NodeList.get("5r"), NodeList.get("6"));
		LinkList.put(l.getId().toString(),setLinkAttributeCar(l));
		l1=createoppositeLink(l, netfac);
		LinkList.put(l1.getId().toString(),l1);
		
		l=netfac.createLink(Id.createLinkId("6r_3"), NodeList.get("6r"), NodeList.get("3"));
		LinkList.put(l.getId().toString(),setLinkAttributeCar(l));
		l1=createoppositeLink(l, netfac);
		LinkList.put(l1.getId().toString(),l1);
		
		l=netfac.createLink(Id.createLinkId("6r_4"), NodeList.get("6r"), NodeList.get("4"));
		LinkList.put(l.getId().toString(),setLinkAttributeCar(l));
		l1=createoppositeLink(l, netfac);
		LinkList.put(l1.getId().toString(),l1);
		
		l=netfac.createLink(Id.createLinkId("5r_7"), NodeList.get("5r"), NodeList.get("7"));
		LinkList.put(l.getId().toString(),setLinkAttributeCar(l));
		l1=createoppositeLink(l, netfac);
		LinkList.put(l1.getId().toString(),l1);
		
		l=netfac.createLink(Id.createLinkId("7r_6"), NodeList.get("7r"), NodeList.get("6"));
		LinkList.put(l.getId().toString(),setLinkAttributeCar(l));
		l1=createoppositeLink(l, netfac);
		LinkList.put(l1.getId().toString(),l1);
		
		
		
		
		
		
		
		//createMtRlinks
		l=netfac.createLink(Id.createLinkId("1r_5_MTR"), NodeList.get("1r"), NodeList.get("5"));
		LinkList.put(l.getId().toString(),setLinkAttributeMTR(l));
		l1=createoppositeMTRLink(l, netfac);
		LinkList.put(l1.getId().toString(),l1);
		
		l=netfac.createLink(Id.createLinkId("5r_6_MTR"), NodeList.get("5r"), NodeList.get("6"));
		LinkList.put(l.getId().toString(),setLinkAttributeMTR(l));
		l1=createoppositeMTRLink(l, netfac);
		LinkList.put(l1.getId().toString(),l1);
		
		l=netfac.createLink(Id.createLinkId("6r_4_MTR"), NodeList.get("6r"), NodeList.get("4"));
		LinkList.put(l.getId().toString(),setLinkAttributeMTR(l));
		l1=createoppositeMTRLink(l, netfac);
		LinkList.put(l1.getId().toString(),l1);
		
		l=netfac.createLink(Id.createLinkId("2r_5_MTR"), NodeList.get("2r"), NodeList.get("5"));
		LinkList.put(l.getId().toString(),setLinkAttributeMTR(l));
		l1=createoppositeMTRLink(l, netfac);
		LinkList.put(l1.getId().toString(),l1);
		
		l=netfac.createLink(Id.createLinkId("5r_7_MTR"), NodeList.get("5r"), NodeList.get("7"));
		LinkList.put(l.getId().toString(),setLinkAttributeMTR(l));
		l1=createoppositeMTRLink(l, netfac);
		LinkList.put(l1.getId().toString(),l1);
		
		l=netfac.createLink(Id.createLinkId("7r_6_MTR"), NodeList.get("7r"), NodeList.get("6"));
		LinkList.put(l.getId().toString(),setLinkAttributeMTR(l));
		l1=createoppositeMTRLink(l, netfac);
		LinkList.put(l1.getId().toString(),l1);
		
		l=netfac.createLink(Id.createLinkId("6r_3_MTR"), NodeList.get("6r"), NodeList.get("3"));
		LinkList.put(l.getId().toString(),setLinkAttributeMTR(l));
		l1=createoppositeMTRLink(l, netfac);
		LinkList.put(l1.getId().toString(),l1);
		
		//Create Stop Links
		
		for(int i=1;i<=7;i++) {
			l=netfac.createLink(Id.createLinkId(i+"_"+i+"r_stop_train"), NodeList.get(Integer.toString(i)), NodeList.get(i+"r"));
			LinkList.put(l.getId().toString(),setLinkAttributeStop(l,"train"));
			l1=createoppositeLinkStopMTR(l, netfac);
			LinkList.put(l1.getId().toString(),l1);
		}
		
		for(int i=1;i<=7;i++) {
			l=netfac.createLink(Id.createLinkId(i+"_"+i+"r_stop"), NodeList.get(Integer.toString(i)), NodeList.get(i+"r"));
			LinkList.put(l.getId().toString(),setLinkAttributeStop(l));
			l1=createoppositeLinkStop(l, netfac);
			LinkList.put(l1.getId().toString(),l1);
		}
		
		

		for(Node n:NodeList.values()) {
			network.addNode(n);
		}
		for(Link ll:LinkList.values()) {
			network.addLink(ll);
		}



		return network;
	}
	private static Link setLinkAttributeCar(Link l) {
		Set<String> modes=new HashSet<>();
		modes.add("car");
		modes.add("bus");
		l.setAllowedModes(modes);
		l.setNumberOfLanes(1);
		l.setCapacity(1800*l.getNumberOfLanes());
		l.setFreespeed(13.89);
		return l;
	}
	private static Link setLinkAttributeMTR(Link l) {
		Set<String> modes=new HashSet<>();
		modes.add("train");
		l.setAllowedModes(modes);
		l.setNumberOfLanes(2);
		l.setCapacity(1800*l.getNumberOfLanes());
		l.setFreespeed(18.22);
		return l;
	}
	
	
	
	private static Link setLinkAttributeStop(Link l) {
		Set<String> modes=new HashSet<>();
		modes.add("car");
		modes.add("bus");
		l.setAllowedModes(modes);
		l.setNumberOfLanes(2);
		l.setCapacity(3600*l.getNumberOfLanes());
		l.setFreespeed(16.67);
		return l;
	}
	
	private static Link setLinkAttributeStop(Link l,String mode) {
		Set<String> modes=new HashSet<>();
		modes.add(mode);
		l.setAllowedModes(modes);
		l.setNumberOfLanes(2);
		l.setCapacity(3600*l.getNumberOfLanes());
		l.setFreespeed(16.67);
		return l;
	}
	
	private static Link createoppositeLink(Link l,NetworkFactory netFac) {
		Link loposite= netFac.createLink(Id.createLinkId(l.getToNode().getId().toString()+"_"+l.getFromNode().getId().toString()), l.getToNode(), l.getFromNode());
		loposite.setAllowedModes(l.getAllowedModes());
		loposite.setCapacity(l.getCapacity());
		loposite.setFreespeed(l.getFreespeed());
		loposite.setLength(l.getLength());
		loposite.setNumberOfLanes(l.getNumberOfLanes());
		return loposite;
	}
	private static Link createoppositeMTRLink(Link l,NetworkFactory netFac) {
		Link loposite= netFac.createLink(Id.createLinkId(l.getToNode().getId().toString()+"_"+l.getFromNode().getId().toString()+"_MTR"), l.getToNode(), l.getFromNode());
		loposite.setAllowedModes(l.getAllowedModes());
		loposite.setCapacity(l.getCapacity());
		loposite.setFreespeed(l.getFreespeed());
		loposite.setLength(l.getLength());
		loposite.setNumberOfLanes(l.getNumberOfLanes());
		return loposite;
	}
	private static Link createoppositeLinkStop(Link l,NetworkFactory netFac) {
		Link loposite= netFac.createLink(Id.createLinkId(l.getToNode().getId().toString()+"_"+l.getFromNode().getId().toString()+"_stop"), l.getToNode(), l.getFromNode());
		loposite.setAllowedModes(l.getAllowedModes());
		loposite.setCapacity(l.getCapacity());
		loposite.setFreespeed(l.getFreespeed());
		loposite.setLength(l.getLength());
		loposite.setNumberOfLanes(l.getNumberOfLanes());
		return loposite;
	}
	private static Link createoppositeLinkStopMTR(Link l,NetworkFactory netFac) {
		Link loposite= netFac.createLink(Id.createLinkId(l.getToNode().getId().toString()+"_"+l.getFromNode().getId().toString()+"_stop_train"), l.getToNode(), l.getFromNode());
		loposite.setAllowedModes(l.getAllowedModes());
		loposite.setCapacity(l.getCapacity());
		loposite.setFreespeed(l.getFreespeed());
		loposite.setLength(l.getLength());
		loposite.setNumberOfLanes(l.getNumberOfLanes());
		return loposite;
	}
	
	public static void main(String[] args) {
		Network network=generateNetwork(ScenarioUtils.createScenario(ConfigUtils.createConfig()).getNetwork());
		new NetworkWriter(network).write("toyScenario/testNetwork.xml"); 
	}
	
}
