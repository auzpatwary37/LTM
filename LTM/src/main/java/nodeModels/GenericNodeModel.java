package nodeModels;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math.linear.MatrixUtils;
import org.apache.commons.math.linear.RealVector;
import org.jboss.logging.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;

import linkModels.GenericLinkModel;
import linkModels.LinkModel;
import ust.hk.praisehk.metamodelcalibration.matsimIntegration.SignalFlowReductionGenerator;
import utils.LTMUtils;
import utils.MapToArray;
import utils.TuplesOfThree;
import utils.VariableDetails;

public class GenericNodeModel implements NodeModel{
	private static final Logger logger = Logger.getLogger(GenericNodeModel.class);
	private final Node node;
	private NodeModelAlgorithm algo;//maybe later can be implemented to change the node model algo
	private int T;//The number of time slots
	private double[] timePoints;
	private Map<Id<Link>,LinkModel> inLinkModels = new HashMap<>();// Link models for all the in links
	private Map<Id<Link>,LinkModel> outLinkModels = new HashMap<>();// Link models for all the out links 
	private MapToArray<VariableDetails> variables;// Map to array for all the variables that the gradients are supposed to be calculated for; this can be shared over all over the system
	private Map<Id<Link>,Map<Id<Link>,double[]>> turnRatio = new HashMap<>();// the turn ratio, this can be static or dynamic 
	private Map<Id<Link>,Map<Id<Link>,double[]>> turnRatiodt = new HashMap<>();// the turn ratio gradient with respect to time, this can be static or dynamic 
	private Map<Id<Link>,Map<Id<Link>,double[][]>> dturnRatio = new HashMap<>();// the turn ratio gradient with respect to the variables, this can be static or dynamic 
	
	private Map<Id<Link>,Map<Id<Link>,double[]>> turn = new HashMap<>();// the turn ratio, this can be static or dynamic 
	private Map<Id<Link>,Map<Id<Link>,double[]>> turndt = new HashMap<>();// the turn ratio gradient with respect to time, this can be static or dynamic 
	private Map<Id<Link>,Map<Id<Link>,double[][]>> dturn = new HashMap<>();// the turn ratio gradient with respect to the variables, this can be static or dynamic 
	private Map<Id<Link>,Set<NetworkRoute>> linkToRouteIncidence = new HashMap<>();
	
	private Map<Id<Link>,double[]> S_i = new HashMap<>();
	private Map<Id<Link>,double[]> S_idt = new HashMap<>();
	private Map<Id<Link>,double[][]> dS_i = new HashMap<>();
	
	private Map<Id<Link>,double[][]> S_ir = new HashMap<>();
	private Map<Id<Link>,double[][]> S_irdt = new HashMap<>();
	private Map<Id<Link>,double[][][]> dS_ir = new HashMap<>();
	
	private Map<Id<Link>,Map<Id<Link>,double[]>>S_ij = new HashMap<>();
	private Map<Id<Link>,Map<Id<Link>,double[]>>S_ijdt = new HashMap<>();
	private Map<Id<Link>,Map<Id<Link>,double[][]>>dS_ij = new HashMap<>();	
	
	private Map<Id<Link>,double[]> R_j = new HashMap<>();
	private Map<Id<Link>,double[]> R_jdt = new HashMap<>();
	private Map<Id<Link>,double[][]> dR_j = new HashMap<>();
	
	
	private Map<Id<Link>,Map<Id<Link>,double[]>> G_ij = new HashMap<>();
	private Map<Id<Link>,Map<Id<Link>,double[]>> G_ijdt = new HashMap<>();
	private Map<Id<Link>,Map<Id<Link>,double[][]>>dG_ij = new HashMap<>();
	
	SignalFlowReductionGenerator sg = null;
	
	
	private Map<Id<NetworkRoute>,OriginNodeModel> originNodeModels = new HashMap<>();
	
	private Map<Id<NetworkRoute>,DestinationNodeModel> destinationNodeModels = new HashMap<>();
	
	public GenericNodeModel(Node node, Map<Id<Link>,LinkModel>linkModels, SignalFlowReductionGenerator  sg) {
		
		this.sg = sg;
		this.node = node;
		node.getInLinks().entrySet().forEach(le->{
			if(!linkModels.containsKey(le.getKey())) linkModels.put(le.getKey(), new GenericLinkModel(le.getValue()));
			inLinkModels.put(le.getKey(),linkModels.get(le.getKey()));
		});
		
		node.getOutLinks().entrySet().forEach(le->{
			if(!linkModels.containsKey(le.getKey())) linkModels.put(le.getKey(), new GenericLinkModel(le.getValue()));
			outLinkModels.put(le.getKey(),linkModels.get(le.getKey()));
		});
		
		
	}
	@Override
	public void setTimeStepAndRoutes(double[] timePoints, MapToArray<NetworkRoute> routes) {
		this.timePoints = timePoints;
		this.T = timePoints.length;
	}
	@Override
	public void setOptimizationVariables(MapToArray<VariableDetails> variables) {
		this.variables = variables;
	}


	@Override
	public void generateTurnRatio(int timeStep) {//This is what actually happened, i.e., from calculated Nr, this method can be invoked to calculate the turns after the Nr has been calculated from g_ij.
		Map<Id<Link>,Map<Id<Link>,Double>> turns = new HashMap<>();
		Map<Id<Link>,Map<Id<Link>,RealVector>> turnsGrad = new HashMap<>();
		Map<Id<Link>,Map<Id<Link>,Double>> turnsdt = new HashMap<>();

		for(Entry<Id<Link>, LinkModel> inLink:this.inLinkModels.entrySet()) {
			turns.put(inLink.getKey(), new HashMap<>());
			int i = 0;
			for(Entry<Id<NetworkRoute>, NetworkRoute> rEntry:inLink.getValue().getRoutes().entrySet()) {
				Id<NetworkRoute> rId = rEntry.getKey();
				NetworkRoute r = rEntry.getValue();
				Id<Link>toLink = LTMUtils.findNextLink(new Tuple<>(rId,r), inLink.getKey(),this.destinationNodeModels);

				if(!this.turnRatio.containsKey(inLink.getKey())) {
					this.turnRatio.put(inLink.getKey(), new HashMap<>());
					this.turnRatiodt.put(inLink.getKey(), new HashMap<>());
					this.dturnRatio.put(inLink.getKey(), new HashMap<>());
					
					this.turn.put(inLink.getKey(), new HashMap<>());
					this.turndt.put(inLink.getKey(), new HashMap<>());
					this.dturn.put(inLink.getKey(), new HashMap<>());
				}
				if(!this.turnRatio.get(inLink.getKey()).containsKey(toLink)) {
					this.turnRatio.get(inLink.getKey()).put(toLink, new double[T]);
					this.turnRatiodt.get(inLink.getKey()).put(toLink, new double[T]);
					this.dturnRatio.get(inLink.getKey()).put(toLink, new double[T][this.variables.getKeySet().size()]);
					
					this.turn.get(inLink.getKey()).put(toLink, new double[T]);
					this.turndt.get(inLink.getKey()).put(toLink, new double[T]);
					this.dturn.get(inLink.getKey()).put(toLink, new double[T][this.variables.getKeySet().size()]);
				}
				int rInd = i;
				turns.get(inLink.getKey()).compute(toLink, (k,v)->v==null?inLink.getValue().getNrxl()[rInd][timeStep]:v+inLink.getValue().getNrxl()[rInd][timeStep]);
				if(this.variables!=null)turnsGrad.get(inLink.getKey()).compute(toLink, (k,v)->v==null?MatrixUtils.createRealVector(inLink.getValue().getdNrxl()[rInd][timeStep]):v.add(inLink.getValue().getdNrxl()[rInd][timeStep]));
				turnsdt.get(inLink.getKey()).compute(toLink, (k,v)->v==null?inLink.getValue().getNrxldt()[rInd][timeStep]:v+inLink.getValue().getNrxldt()[rInd][timeStep]);
				i++;
			}
			
			Map<Id<Link>,double[]> o = null;
			if(this.variables!=null)o = turnsGrad.get(inLink.getKey()).entrySet().stream().collect(Collectors.toMap(k->k.getKey(), k->k.getValue().getData()));
			Map<Id<Link>,double[]> ot = turnsdt.get(inLink.getKey()).entrySet().stream().collect(Collectors.toMap(k->k.getKey(), k->new double[] {k.getValue()}));

			Map<Id<Link>, Tuple<Double, double[]>> outd = LTMUtils.calculateRatioAndGradient(turns.get(inLink.getKey()), o);
			Map<Id<Link>, Tuple<Double, double[]>> outdt = LTMUtils.calculateRatioAndGradient(turns.get(inLink.getKey()), ot);
			//this.turnRatio.put(inLink.getKey(), ot)
			outd.entrySet().forEach(e->{
				this.turnRatio.get(inLink.getKey()).get(e.getKey())[timeStep] = outd.get(e.getKey()).getFirst();
				this.turn.get(inLink.getKey()).get(e.getKey())[timeStep] = turns.get(inLink.getKey()).get(e.getKey());
				if(this.variables!=null)this.dturnRatio.get(inLink.getKey()).get(e.getKey())[timeStep] = outd.get(e.getKey()).getSecond();
				if(this.variables!=null)this.dturn.get(inLink.getKey()).get(e.getKey())[timeStep] = turnsGrad.get(inLink.getKey()).get(e.getKey()).getData();
				this.turnRatiodt.get(inLink.getKey()).get(e.getKey())[timeStep] = outdt.get(e.getKey()).getSecond()[0];
				this.turndt.get(inLink.getKey()).get(e.getKey())[timeStep] = turnsdt.get(inLink.getKey()).get(e.getKey());
			});

		}
	}
		
	@Override
	public void applyNodeModel(int timeStep) {
		int k = 0;
		int maxK = 10;
		Map<Id<Link>,Map<Id<Link>,Double>>R_ij = new HashMap<>();
		Map<Id<Link>,Map<Id<Link>,Double>>R_ijdt = new HashMap<>();
		Map<Id<Link>,Map<Id<Link>,double[]>>dR_ij = new HashMap<>();

		Map<Id<Link>,Map<Id<Link>,Double>>g_ij = new HashMap<>();
		Map<Id<Link>,Map<Id<Link>,Double>>g_ijdt = new HashMap<>();
		Map<Id<Link>,Map<Id<Link>,double[]>>dg_ij = new HashMap<>();

		Set<Id<Link>> Iprime = new HashSet<>();
		Set<Id<Link>> I = new HashSet<>();
		Map<Id<Link>,Double> sumgbyctimesqmij = new HashMap<>();
		//Initialization
		for(LinkModel i:this.inLinkModels.values()) {
			for(LinkModel j:this.outLinkModels.values()) {
				if(!R_ij.containsKey(i.getLink().getId())) {
					R_ij.put(i.getLink().getId(), new HashMap<>());
					dR_ij.put(i.getLink().getId(), new HashMap<>());
					R_ijdt.put(i.getLink().getId(), new HashMap<>());

				}
				//For now lets assume the turning capacity is 1800, i.e, the same
				double cap = 1800;//this can be inserted somehow, but from MATSim alone, I do not think so. 
				double gbyc = 1;//this can be extracted from matsim HK.
				
				if(sg!=null)gbyc = sg.getGCratio(i.getLink(), j.getLink().getId())[0];
				double gc = gbyc;
				sumgbyctimesqmij.compute(j.getLink().getId(), (kk,v)->v==null?gc*cap:v+gc*cap);

			}
		}
		for(LinkModel i:this.inLinkModels.values()) {
			if(i.getRoutes()!=null) {
			for(LinkModel j:this.outLinkModels.values()) {//calculate Rij_0
				if(j.getRoutes()!=null) {
				//if(!R_ij.containsKey(i.getLink().getId()))R_ij.put(i.getLink().getId(), new HashMap<>());
				//For now lets assume the turning capacity is 1800, i.e, the same
				double cap = 1800;//this can be inserted somehow, but from MATSim alone, I do not think so. 
				double gbyc = 1;//this can be extracted from matsim HK.
				R_ij.get(i.getLink().getId()).put(j.getLink().getId(),cap*gbyc/sumgbyctimesqmij.get(j.getLink().getId())*this.R_j.get(j.getLink().getId())[timeStep]);
				if(this.variables!=null)dR_ij.get(i.getLink().getId()).put(j.getLink().getId(), 
						MatrixUtils.createRealVector(this.dR_j.get(j.getLink().getId())[timeStep]).mapMultiply(cap*gbyc/sumgbyctimesqmij.get(j.getLink().getId())).getData());
				R_ijdt.get(i.getLink().getId()).put(j.getLink().getId(), cap*gbyc/sumgbyctimesqmij.get(j.getLink().getId())*this.R_jdt.get(j.getLink().getId())[timeStep]);
			
				}
			}
			}
		}
		//Find out the most limiting constriant and its gradient
		for(Id<Link> iId:this.S_ij.keySet()) {
			LinkModel i = this.inLinkModels.get(iId);
			if(i.getRoutes()!=null) {
			double constraint = 1;
			double[] dConstraint =  null;
			if(this.variables!=null)dConstraint = new double[this.variables.getKeySet().size()];
			double constraintdt = 1;

			for(Id<Link> jId:this.S_ij.get(iId).keySet()) {//find out the most active constraint
				LinkModel j = this.outLinkModels.get(jId);
				double cap = 1800;//this can be inserted somehow, but from MATSim alone, I do not think so. 
				double gbyc = 1;
				double qnij = cap*gbyc;
				double sij = this.S_ij.get(i.getLink().getId()).get(j.getLink().getId())[timeStep];

				double qbys = qnij/sij;

				if(qbys<constraint) {
					constraint = qbys;
					if(this.variables!=null)dConstraint = MatrixUtils.createRealVector(this.dS_ij.get(i.getLink().getId()).get(j.getLink().getId())[timeStep]).mapMultiply(-1*qnij/Math.pow(sij,2)).getData();
					constraintdt = -1*qnij/Math.pow(sij,2)*this.S_ijdt.get(i.getLink().getId()).get(j.getLink().getId())[timeStep];
				}

				double RijbySij = R_ij.get(i.getLink().getId()).get(j.getLink().getId())/sij;

				if(RijbySij<constraint) {// We assume the already selected one will be kept selected
					constraint = RijbySij;
					TuplesOfThree<Double, Double, double[]> a = LTMUtils.calcAbyBGrad(new TuplesOfThree<>(R_ij.get(i.getLink().getId()).get(j.getLink().getId()),
							R_ijdt.get(i.getLink().getId()).get(j.getLink().getId()),
							dR_ij.get(i.getLink().getId()).get(j.getLink().getId())), 
							new TuplesOfThree<>(sij,this.S_ijdt.get(i.getLink().getId()).get(j.getLink().getId())[timeStep],this.dS_ij.get(i.getLink().getId()).get(j.getLink().getId())[timeStep]));
					if(this.variables!=null)dConstraint = a.getThird();
					constraintdt = a.getSecond();
				}


			}
			for(Id<Link> jId:this.S_ij.get(iId).keySet()) {//calculate Gij using the most limiting constraint 
				LinkModel j = this.outLinkModels.get(jId);
				if(!g_ij.containsKey(i.getLink().getId())) {
					g_ij.put(i.getLink().getId(), new HashMap<>());
					dg_ij.put(i.getLink().getId(), new HashMap<>());
					g_ijdt.put(i.getLink().getId(), new HashMap<>());

				}
				TuplesOfThree<Double,Double,double[]> gij = LTMUtils.calcAtimesBGrad(new TuplesOfThree<>(constraint,constraintdt,dConstraint), 
						new TuplesOfThree<>(this.S_ij.get(i.getLink().getId()).get(j.getLink().getId())[timeStep],
								this.S_ijdt.get(i.getLink().getId()).get(j.getLink().getId())[timeStep],this.dS_ij.get(i.getLink().getId()).get(j.getLink().getId())[timeStep]));
				g_ij.get(i.getLink().getId()).put(j.getLink().getId(), gij.getFirst());
				if(this.variables!=null)dg_ij.get(i.getLink().getId()).put(j.getLink().getId(), gij.getThird());
				g_ijdt.get(i.getLink().getId()).put(j.getLink().getId(), gij.getSecond());

			}
			}

		}
		//Calculate sets I and I'
		for(Id<Link> iId:this.S_ij.keySet()) {
			LinkModel i = this.inLinkModels.get(iId);
			boolean isIprime = true;
			for(Id<Link> jId:this.S_ij.get(iId).keySet()) {
				LinkModel j = this.outLinkModels.get(jId);
				double cap = 1800;//this can be inserted somehow, but from MATSim alone, I do not think so. 
				double gbyc = 1;
				double qnij = cap*gbyc;
				if(g_ij.get(i.getLink().getId()).get(j.getLink().getId())-R_ij.get(i.getLink().getId()).get(j.getLink().getId())<0.001
						&& R_ij.get(i.getLink().getId()).get(j.getLink().getId())<this.S_ij.get(i.getLink().getId()).get(j.getLink().getId())[timeStep]
								&& R_ij.get(i.getLink().getId()).get(j.getLink().getId())<qnij) {

				}else {
					isIprime = false;
					break;
				}
			}
			if(isIprime)Iprime.add(i.getLink().getId());
			else I.add(i.getLink().getId());
		}

		if(I.size()==0 || Iprime.size()==0) {
			for(Id<Link> iId:this.S_ij.keySet()) {
				LinkModel i = this.inLinkModels.get(iId);
				for(Id<Link> jId:this.S_ij.get(iId).keySet()) {
					LinkModel j = this.outLinkModels.get(jId);
					if(!this.G_ij.containsKey(i.getLink().getId())) {
						this.G_ij.put(i.getLink().getId(), new HashMap<>());
						this.G_ijdt.put(i.getLink().getId(),new HashMap<>());
						this.dG_ij.put(i.getLink().getId(), new HashMap<>());
					}
					if(!this.G_ij.get(i.getLink().getId()).containsKey(j.getLink().getId())) {
						this.G_ij.get(i.getLink().getId()).put(j.getLink().getId(), new double[T]);
						this.G_ijdt.get(i.getLink().getId()).put(j.getLink().getId(), new double[T]);
						if(this.variables!=null)this.dG_ij.get(i.getLink().getId()).put(j.getLink().getId(), new double[T][this.variables.getKeySet().size()]);
					}
					this.G_ij.get(i.getLink().getId()).get(j.getLink().getId())[timeStep] = g_ij.get(i.getLink().getId()).get(j.getLink().getId());
					this.G_ijdt.get(i.getLink().getId()).get(j.getLink().getId())[timeStep] = g_ijdt.get(i.getLink().getId()).get(j.getLink().getId());
					this.dG_ij.get(i.getLink().getId()).get(j.getLink().getId())[timeStep] = dg_ij.get(i.getLink().getId()).get(j.getLink().getId());
				}

			}
			return;
		}
		//According to the thesis initialization finished. 

		for(k = 0;k<maxK;k++) {
			
			Map<Id<Link>,Double> remainingRj = new HashMap<>();
			Map<Id<Link>,Double> remainingRjdt = new HashMap<>();
			Map<Id<Link>,RealVector> dremainingRj = new HashMap<>();

			for(Id<Link> iId:this.S_ij.keySet()) {//Calculate Rij
				LinkModel i = this.inLinkModels.get(iId);
				if(I.contains(i.getLink().getId())) {
					for(Id<Link> jId:this.S_ij.get(iId).keySet()) {
						LinkModel j = this.outLinkModels.get(jId);
						double remainingCap = R_ij.get(i.getLink().getId()).get(j.getLink().getId())-g_ij.get(i.getLink().getId()).get(j.getLink().getId());
						double remainingCapdt = R_ijdt.get(i.getLink().getId()).get(j.getLink().getId())-g_ijdt.get(i.getLink().getId()).get(j.getLink().getId());
						RealVector dremainingCap = MatrixUtils.createRealVector(dR_ij.get(i.getLink().getId()).get(j.getLink().getId())).subtract(dg_ij.get(i.getLink().getId()).get(j.getLink().getId()));
						remainingRj.compute(j.getLink().getId(), (kk,v)->v==null?remainingCap:v+remainingCap);
						remainingRjdt.compute(j.getLink().getId(), (kk,v)->v==null?remainingCapdt:v+remainingCapdt);
						if(this.variables!=null)dremainingRj.compute(j.getLink().getId(), (kk,v)->v==null?dremainingCap:v.add(dremainingCap));
					}
				}	
			}
			
			for(Id<Link> iId:this.S_ij.keySet()) {
				LinkModel i = this.inLinkModels.get(iId);
				if(Iprime.contains(i.getLink().getId())) {
					for(Id<Link> jId:this.S_ij.get(iId).keySet()) {
						LinkModel j = this.outLinkModels.get(jId);
						R_ij.get(i.getLink().getId()).put(j.getLink().getId(),
								R_ij.get(i.getLink().getId()).get(j.getLink().getId())+1/Iprime.size()*remainingRj.get(j.getLink().getId()));
						R_ijdt.get(i.getLink().getId()).put(j.getLink().getId(),
								R_ijdt.get(i.getLink().getId()).get(j.getLink().getId())+1/Iprime.size()*remainingRjdt.get(j.getLink().getId()));
						if(this.variables!=null)dR_ij.get(i.getLink().getId()).put(j.getLink().getId(),
								MatrixUtils.createRealVector(dR_ij.get(i.getLink().getId()).get(j.getLink().getId())).add(dremainingRj.get(j.getLink().getId()).mapMultiply(1/Iprime.size())).getData());
					}
				}	
			}
			//Find out the most limiting constriant and its gradient
			for(Id<Link> iId:this.S_ij.keySet()) {
				LinkModel i = this.inLinkModels.get(iId);
				if(Iprime.contains(i.getLink().getId())) {
					double constraint = 1;
					double[] dConstraint = new double[this.variables.getKeySet().size()];
					double constraintdt = 1;

					for(Id<Link> jId:this.S_ij.get(iId).keySet()) {//find out the most active constraint
						LinkModel j = this.outLinkModels.get(jId);
						double cap = 1800;//this can be inserted somehow, but from MATSim alone, I do not think so. 
						double gbyc = 1;
						double qnij = cap*gbyc;
						double sij = this.S_ij.get(i.getLink().getId()).get(j.getLink().getId())[timeStep];

						double qbys = qnij/sij;

						if(qbys<constraint) {
							constraint = qbys;
							if(this.variables!=null)dConstraint = MatrixUtils.createRealVector(this.dS_ij.get(i.getLink().getId()).get(j.getLink().getId())[timeStep]).mapMultiply(-1*qnij/Math.pow(sij,2)).getData();
							constraintdt = -1*qnij/Math.pow(sij,2)*this.S_ijdt.get(i.getLink().getId()).get(j.getLink().getId())[timeStep];
						}

						double RijbySij = R_ij.get(i.getLink().getId()).get(j.getLink().getId())/sij;

						if(RijbySij<constraint) {
							constraint = RijbySij;
							TuplesOfThree<Double, Double, double[]> a = LTMUtils.calcAbyBGrad(new TuplesOfThree<>(R_ij.get(i.getLink().getId()).get(j.getLink().getId()),
									R_ijdt.get(i.getLink().getId()).get(j.getLink().getId()),
									dR_ij.get(i.getLink().getId()).get(j.getLink().getId())), 
									new TuplesOfThree<>(sij,this.S_ijdt.get(i.getLink().getId()).get(j.getLink().getId())[timeStep],this.dS_ij.get(i.getLink().getId()).get(j.getLink().getId())[timeStep]));
							if(this.variables!=null)dConstraint = a.getThird();
							constraintdt = a.getSecond();
						}


					}
					for(Id<Link> jId:this.S_ij.get(iId).keySet()) {//calculate Gij using the most limiting constraint 
						LinkModel j = this.outLinkModels.get(jId);
						if(!g_ij.containsKey(i.getLink().getId())) {
							g_ij.put(i.getLink().getId(), new HashMap<>());
							if(this.variables!=null)dg_ij.put(i.getLink().getId(), new HashMap<>());
							g_ijdt.put(i.getLink().getId(), new HashMap<>());

						}
						TuplesOfThree<Double,Double,double[]> gij = LTMUtils.calcAtimesBGrad(new TuplesOfThree<>(constraint,constraintdt,dConstraint), 
								new TuplesOfThree<>(this.S_ij.get(i.getLink().getId()).get(j.getLink().getId())[timeStep],
										this.S_ijdt.get(i.getLink().getId()).get(j.getLink().getId())[timeStep],this.dS_ij.get(i.getLink().getId()).get(j.getLink().getId())[timeStep]));
						g_ij.get(i.getLink().getId()).put(j.getLink().getId(), gij.getFirst());
						if(this.variables!=null)dg_ij.get(i.getLink().getId()).put(j.getLink().getId(), gij.getThird());
						g_ijdt.get(i.getLink().getId()).put(j.getLink().getId(), gij.getSecond());

					}
				}

			}
			//Update the sets I and Iprime
			Set<Id<Link>> newI = new HashSet<>();
			Set<Id<Link>> newIprime = new HashSet<>();
			
			Iprime.forEach(i->{
				boolean isIprime = true;
				for(LinkModel j:this.outLinkModels.values()) {
					double cap = 1800;//this can be inserted somehow, but from MATSim alone, I do not think so. 
					double gbyc = 1;
					double qnij = cap*gbyc;
					if(g_ij.get(i).get(j.getLink().getId())-R_ij.get(i).get(j.getLink().getId())<0.001
							&& R_ij.get(i).get(j.getLink().getId())<this.S_ij.get(i).get(j.getLink().getId())[timeStep]
									&& R_ij.get(i).get(j.getLink().getId())<qnij) {

					}else {
						isIprime = false;
						break;
					}
				}
				if(isIprime)newIprime.add(i);
				else newI.add(i);
			});
			I = newI;
			Iprime = newIprime;
			if(I.size()==0 || Iprime.size()==0) {
				break;
			}	
		}
		for(Id<Link> iId:this.S_ij.keySet()) {
			LinkModel i = this.inLinkModels.get(iId);
			for(Id<Link> jId:this.S_ij.get(iId).keySet()) {
				LinkModel j = this.outLinkModels.get(jId);
				if(!this.G_ij.containsKey(i.getLink().getId())) {
					this.G_ij.put(i.getLink().getId(), new HashMap<>());
					this.G_ijdt.put(i.getLink().getId(),new HashMap<>());
					this.dG_ij.put(i.getLink().getId(), new HashMap<>());
				}
				if(!this.G_ij.get(i.getLink().getId()).containsKey(j.getLink().getId())) {
					this.G_ij.get(i.getLink().getId()).put(j.getLink().getId(), new double[T]);
					this.G_ijdt.get(i.getLink().getId()).put(j.getLink().getId(), new double[T]);
					if(this.variables!=null)this.dG_ij.get(i.getLink().getId()).put(j.getLink().getId(), new double[T][this.variables.getKeySet().size()]);
				}
				this.G_ij.get(i.getLink().getId()).get(j.getLink().getId())[timeStep] = g_ij.get(i.getLink().getId()).get(j.getLink().getId());
				this.G_ijdt.get(i.getLink().getId()).get(j.getLink().getId())[timeStep] = g_ijdt.get(i.getLink().getId()).get(j.getLink().getId());
				if(this.variables!=null)this.dG_ij.get(i.getLink().getId()).get(j.getLink().getId())[timeStep] = dg_ij.get(i.getLink().getId()).get(j.getLink().getId());
			}

		}
		return;

	}


	@Override
	public void updateFlow(int timeStep) {

		Map<Id<Link>,Double> gi = new HashMap<>();
		Map<Id<Link>,Double> gidt = new HashMap<>();
		Map<Id<Link>,double[]> dgi = new HashMap<>();

		Map<Id<Link>,Double> gj = new HashMap<>();
		Map<Id<Link>,Double> gjdt = new HashMap<>();
		Map<Id<Link>,double[]> dgj = new HashMap<>();
		
		Map<Id<Link>,LinkModel> activeOutLinkModels = new HashMap<>();

		this.G_ij.entrySet().forEach(inLink->{
			
			for(Entry<Id<Link>, double[]> d:this.G_ij.get(inLink.getKey()).entrySet()){
				//update gi
				activeOutLinkModels.put(d.getKey(), this.outLinkModels.get(d.getKey()));
				gi.compute(inLink.getKey(), (k,v)->v==null?d.getValue()[timeStep]:v+d.getValue()[timeStep]);
				gidt.compute(inLink.getKey(), (k,v)->v==null?this.G_ijdt.get(inLink.getKey()).get(d.getKey())[timeStep]:v+this.G_ijdt.get(inLink.getKey()).get(d.getKey())[timeStep]);
				if(this.variables!=null)dgi.compute(inLink.getKey(), (k,v)->v==null?this.dG_ij.get(inLink.getKey()).get(d.getKey())[timeStep]:
					MatrixUtils.createRealVector(this.dG_ij.get(inLink.getKey()).get(d.getKey())[timeStep]).add(v).getData());

				//update gj
				gj.compute(d.getKey(), (k,v)->v==null?d.getValue()[timeStep]:v+d.getValue()[timeStep]);
				gjdt.compute(d.getKey(), (k,v)->v==null?this.G_ijdt.get(inLink.getKey()).get(d.getKey())[timeStep]:v+this.G_ijdt.get(inLink.getKey()).get(d.getKey())[timeStep]);
				if(this.variables!=null)dgj.compute(d.getKey(), (k,v)->v==null?this.dG_ij.get(inLink.getKey()).get(d.getKey())[timeStep]:
					MatrixUtils.createRealVector(this.dG_ij.get(inLink.getKey()).get(d.getKey())[timeStep]).add(v).getData());
			
			}
		});

		//update of Nxl and Nx0 at time step timeStep+1
		for(Id<Link> inLinkId:this.G_ij.keySet()) {
			LinkModel inLink = this.inLinkModels.get(inLinkId);
			inLink.getNxl()[timeStep+1] = inLink.getNxl()[timeStep]+gi.get(inLinkId);
			if(this.variables!=null)inLink.getdNxl()[timeStep+1] = MatrixUtils.createRealVector(inLink.getdNxl()[timeStep]).add(dgi.get(inLinkId)).getData();
			inLink.getNxldt()[timeStep+1] = inLink.getNxldt()[timeStep]+gidt.get(inLinkId);
		}

		for(Entry<Id<Link>, LinkModel> outLink:activeOutLinkModels.entrySet()) {
			outLink.getValue().getNx0()[timeStep+1] = outLink.getValue().getNx0()[timeStep]+gj.get(outLink.getKey());
			if(this.variables!=null)outLink.getValue().getdNx0()[timeStep+1] = MatrixUtils.createRealVector(outLink.getValue().getdNx0()[timeStep]).add(dgj.get(outLink.getKey())).getData();
			outLink.getValue().getNx0dt()[timeStep+1] = outLink.getValue().getNx0dt()[timeStep]+gjdt.get(outLink.getKey());
		}

		//Update the route specific flow first at the end of the inLinks

		for(Id<Link> inLinkId:this.G_ij.keySet()) {
			LinkModel inLink = this.inLinkModels.get(inLinkId);
			double flow = inLink.getNxl()[timeStep+1];
			int tBefore = timeStep;
			int tAfter = timeStep;
			for(int j = timeStep;j>=0;j--) {
				if(flow<=inLink.getNx0()[j])tAfter = j;
				if(flow>inLink.getNx0()[j]) {
					tBefore = j;
					break;
				}
			}
			if(tBefore>tAfter)tBefore=tAfter;
			// What happens if time is zero?
			RealVector tBeforeGrad = null;
					
			if(this.variables!=null)tBeforeGrad = MatrixUtils.createRealVector(inLink.getdNxl()[timeStep+1]).mapMultiply(1/inLink.getNx0dt()[tBefore]);
			double tBeforedt = 0;
			if(inLink.getNx0dt()[tBefore] == 0) {
				logger.debug("Flow rate is zero!!!");
			}else {
				tBeforedt = inLink.getNxldt()[timeStep+1]*1/inLink.getNx0dt()[tBefore];
			}
			RealVector tAfterGrad = null;
			if(this.variables!=null)tAfterGrad = MatrixUtils.createRealVector(inLink.getdNxl()[timeStep+1]).mapMultiply(1/inLink.getNx0dt()[tAfter]);
			double tAfterdt= 0;
			if(inLink.getNx0dt()[tBefore] == 0) {
				logger.debug("Flow rate is zero!!!");
			}else {
			 tAfterdt = inLink.getNxldt()[timeStep+1]*1/inLink.getNx0dt()[tAfter];
			}
			
			for(Entry<Id<NetworkRoute>, NetworkRoute> rEntry:inLink.getRoutes().entrySet()) {
				Id<NetworkRoute> rId = rEntry.getKey();
				NetworkRoute r = rEntry.getValue();
				int rInd = inLink.getRouteIds().getIndex(rId);
				Id<Link>toLink = LTMUtils.findNextLink(new Tuple<>(rId,r), inLinkId,this.destinationNodeModels);

				double NxrlBefore = inLink.getNrx0()[rInd][tBefore];
				RealVector dNxrlBefore = null;
				if(this.variables!=null)dNxrlBefore = tBeforeGrad.ebeMultiply(inLink.getdNrx0()[rInd][tBefore]);
				double NxrlBeforedt = inLink.getNrx0dt()[rInd][tBefore]*tBeforedt;


				double NxrlAfter = inLink.getNrx0()[rInd][tAfter];
				RealVector dNxrlAfter = null;
				if(this.variables!=null)dNxrlAfter = tAfterGrad.ebeMultiply(inLink.getdNrx0()[rInd][tAfter]);
				double NxrlAfterdt = inLink.getNrx0dt()[rInd][tAfter]*tAfterdt;
				//interpolation
				double Nrxl;
				double[] dNrxl = null;
				double Nrxldt;
				if(tBefore==tAfter) {
					Nrxl = NxrlBefore;
					if(this.variables!=null)dNrxl = dNxrlBefore.getData();
					Nrxldt = NxrlBeforedt;
				}else {//interpolate
					Tuple<Double,double[]> NxrlTuple = null;
					if(this.variables!=null) NxrlTuple = LTMUtils.calcLinearInterpolationAndGradient(new Tuple<>(inLink.getNx0()[tBefore],inLink.getNx0()[tAfter]),
							new Tuple<>(NxrlBefore,NxrlAfter),
							new Tuple<>(inLink.getdNx0()[tBefore],inLink.getdNx0()[tAfter]),
							new Tuple<>(dNxrlBefore.getData(),dNxrlAfter.getData()),
							flow,
							inLink.getdNxl()[timeStep+1]);
					Tuple<Double,double[]> NxrldtTuple = LTMUtils.calcLinearInterpolationAndGradient(new Tuple<>(inLink.getNx0()[tBefore],inLink.getNx0()[tAfter]),
							new Tuple<>(NxrlBefore,NxrlAfter),
							new Tuple<>(new double[] {inLink.getNx0dt()[tBefore]},new double[] {inLink.getNx0dt()[tAfter]}),
							new Tuple<>(new double[] {NxrlBeforedt},new double[] {NxrlAfterdt}),
							flow,
							inLink.getdNxl()[timeStep+1]);

					Nrxl = NxrldtTuple.getFirst()-inLink.getNrxl()[rInd][timeStep];// maybe the second term is redundent Ashraf Feb23
					if(this.variables!=null)dNrxl = NxrlTuple.getSecond();
					Nrxldt = NxrldtTuple.getSecond()[0];
				}
				inLink.getNrxl()[rInd][timeStep+1] = Nrxl;
				if(this.variables!=null)inLink.getdNrxl()[rInd][timeStep+1] = dNrxl;
				inLink.getNrxldt()[rInd][timeStep+1] = Nrxldt;

				//update the toLink's Nrx0

				int outRouteInd = this.outLinkModels.get(toLink).getRouteIds().getIndex(rId);
				this.outLinkModels.get(toLink).getNrx0()[outRouteInd][timeStep+1] = Nrxl;
				if(this.variables!=null)this.outLinkModels.get(toLink).getdNrx0()[outRouteInd][timeStep+1] = dNrxl;
				this.outLinkModels.get(toLink).getNrx0dt()[outRouteInd][timeStep+1] = Nrxldt;


			}
		}



	}


	@Override
	public String generatelinkToLinkKey(Id<Link> fromLink, Id<Link> toLink) {
		
		return fromLink.toString()+"___"+toLink.toString();
	}

	@Override
	public void generateIntendedTurnRatio(int timeStep) {//link specific sending flow ratio s_ij calculation (Intended) and at the R_j calculation
		//Steps 
		//1. calculate S_i(t) and its gradient from the link model.
		//2. Calculate Nx_L(t) + S_i(t) and its gradient. Here Nx_L(t) is already updated from previous iteration. 
		//3. calculate t(Nx_L(t) + S_i(t)). This is done basically first finding the corresponding before and after time step t- and t+ before and after flow Nx_L(t)+S_i(t) 
		//occurred in Nx_0 and then interpolating the two based on t- + {Nx_0(t+)-Nx_0(t-)}/{t+ - t-}*(Nx_L(t)+S_i(t)-Nx_0(t-))
		//
		//calculate Npx_0(t(Nx_L(t)+S_i(t))) which is again the interpolation of Npx_0(t-) and before and after
		
		Map<Id<Link>,Map<Id<Link>,Double>> turns = new HashMap<>();
		Map<Id<Link>,Map<Id<Link>,RealVector>> turnsGrad = new HashMap<>();
		Map<Id<Link>,Map<Id<Link>,Double>> turnsdt = new HashMap<>();
		
		
		
		for(Entry<Id<Link>, LinkModel> inLink:this.inLinkModels.entrySet()) {
			if(inLink.getValue().getRoutes()!=null) {
				turns.put(inLink.getKey(), new HashMap<>());
				turnsGrad.put(inLink.getKey(), new HashMap<>());
				turnsdt.put(inLink.getKey(), new HashMap<>());

				if(!this.S_i.containsKey(inLink.getKey()))this.S_i.put(inLink.getKey(), new double[T]);
				if(!this.S_idt.containsKey(inLink.getKey()))this.S_idt.put(inLink.getKey(), new double[T]);
				if(this.variables!=null && !this.dS_i.containsKey(inLink.getKey()))this.dS_i.put(inLink.getKey(), new double[T][this.variables.getKeySet().size()]);


				if(!this.S_ir.containsKey(inLink.getKey()))this.S_ir.put(inLink.getKey(), new double[inLink.getValue().getRoutes().size()][T]);
				if(!this.S_irdt.containsKey(inLink.getKey()))this.S_irdt.put(inLink.getKey(), new double[inLink.getValue().getRoutes().size()][T]);
				if(this.variables!=null &&!this.dS_ir.containsKey(inLink.getKey()))this.dS_ir.put(inLink.getKey(), new double[inLink.getValue().getRoutes().size()][T][this.variables.getKeySet().size()]);

				if(!this.S_ij.containsKey(inLink.getKey()))this.S_ij.put(inLink.getKey(), new HashMap<>());
				if(!this.S_ijdt.containsKey(inLink.getKey()))this.S_ijdt.put(inLink.getKey(), new HashMap<>());
				if(this.variables!=null &&!this.dS_ij.containsKey(inLink.getKey()))this.dS_ij.put(inLink.getKey(), new HashMap<>());



				TuplesOfThree<double[], double[], double[][]> S_ij = inLink.getValue().getSendingFlow(timeStep);
				this.S_i.get(inLink.getKey())[timeStep] = S_ij.getFirst()[timeStep];
				this.S_idt.get(inLink.getKey())[timeStep] = S_ij.getSecond()[timeStep];
				if(this.variables!=null)this.dS_i.get(inLink.getKey())[timeStep] = S_ij.getThird()[timeStep];

				double nlPlusSi = inLink.getValue().getNxl()[timeStep]+S_ij.getFirst()[timeStep];
				RealVector dnlPlusSi = null;
				if(this.variables!=null )dnlPlusSi =MatrixUtils.createRealVector(inLink.getValue().getdNxl()[timeStep]).add(S_ij.getThird()[timeStep]);
				double nlPlusSidt = inLink.getValue().getNxldt()[timeStep]+S_ij.getSecond()[timeStep];
				int tBefore = timeStep;
				int tAfter = timeStep;

				for(int j = timeStep-1;j>0;j--) {
					if(nlPlusSi<=inLink.getValue().getNx0()[j])tAfter = j;
					if(nlPlusSi>=inLink.getValue().getNx0()[j]) {
						tBefore = j;
						break;
					}
				}
				RealVector tBeforeGrad = null;
				double tBeforedt = 0;
				RealVector tAfterGrad = null;
				double tAfterdt = 0;
				if(inLink.getValue().getNx0dt()[tBefore]==0) {
					//this should only happen when the flow itself is zero, meaning nlplussi is zero. 
					//Or the time step going backward do not generate any flow
					if(inLink.getValue().getNx0()[tBefore]!=0) {
						logger.debug("careful. the flow is non zero but dt is zero. This would mean it is a jammed condition, where vehicles are stuck");
					}
					tBeforedt = 0;
					tBeforeGrad = MatrixUtils.createRealVector(new double[this.variables.getKeySet().size()]);
				}else {
					if(this.variables!=null )tBeforeGrad = dnlPlusSi.mapMultiply(1/inLink.getValue().getNx0dt()[tBefore]);
					tBeforedt = nlPlusSidt*1/inLink.getValue().getNx0dt()[tBefore];

				}
				if(inLink.getValue().getNx0dt()[tAfter]==0) {
					//this should only happen when the flow itself is zero, meaning nlplussi is zero. 
					if(inLink.getValue().getNx0()[tAfter]!=0) {
						logger.debug("careful. the flow is non zero but dt is zero. This would mean it is a jammed condition, where vehicles are stuck");
					}
					
					tAfterdt = 0;
					tAfterGrad = MatrixUtils.createRealVector(new double[this.variables.getKeySet().size()]);

				}else {

					if(this.variables!=null )tAfterGrad = dnlPlusSi.mapMultiply(1/inLink.getValue().getNx0dt()[tAfter]);
					tAfterdt = nlPlusSidt*1/inLink.getValue().getNx0dt()[tAfter];

				}


				int i = 0;
				for(Entry<Id<NetworkRoute>, NetworkRoute> rEntry:inLink.getValue().getRoutes().entrySet()) {
					int rInd = i;
					Id<NetworkRoute> rId = rEntry.getKey();
					NetworkRoute r = rEntry.getValue();
					Id<Link>toLink = LTMUtils.findNextLink(new Tuple<>(rId,r), inLink.getKey(),this.destinationNodeModels);

					if(toLink!=null) {
						if(!this.S_ij.get(inLink.getKey()).containsKey(toLink))this.S_ij.get(inLink.getKey()).put(toLink, new double[T]);
						if(!this.S_ijdt.get(inLink.getKey()).containsKey(toLink))this.S_ijdt.get(inLink.getKey()).put(toLink, new double[T]);
						if(this.variables!=null && !this.dS_ij.get(inLink.getKey()).containsKey(toLink))this.dS_ij.get(inLink.getKey()).put(toLink, new double[T][this.variables.getKeySet().size()]);
	
						if(!this.R_j.containsKey(toLink))this.R_j.put(toLink, new double[T]);
						if(!this.R_jdt.containsKey(toLink))this.R_jdt.put(toLink, new double[T]);
						if(this.variables!=null && !this.dR_j.containsKey(toLink))this.dR_j.put(toLink, new double[T][this.variables.getKeySet().size()]);
						}

					double S_irBefore = inLink.getValue().getNrx0()[rInd][tBefore];
					RealVector dS_irBefore = null;
					if(this.variables!=null)dS_irBefore = tBeforeGrad.ebeMultiply(inLink.getValue().getdNrx0()[rInd][tBefore]);
					double S_irBeforedt = inLink.getValue().getNrx0dt()[rInd][tBefore]*tBeforedt;


					double S_irAfter = inLink.getValue().getNrx0()[rInd][tAfter];
					RealVector dS_irAfter = null;
					if(this.variables!=null)dS_irAfter = tAfterGrad.ebeMultiply(inLink.getValue().getdNrx0()[rInd][tAfter]);
					double S_irAfterdt = inLink.getValue().getNrx0dt()[rInd][tAfter]*tAfterdt;

					//interpolation
					double S_ir;
					double[] dS_ir = null;
					double S_irdt;
					if(tBefore==tAfter) {
						S_ir = S_irBefore-inLink.getValue().getNrxl()[rInd][timeStep];
						if(this.variables!=null)dS_ir = dS_irBefore.subtract(inLink.getValue().getdNrxl()[rInd][timeStep]).getData();
						S_irdt = S_irBeforedt-inLink.getValue().getNrxldt()[rInd][timeStep];
					}else {

						Tuple<Double,double[]> S_irTuple = LTMUtils.calcLinearInterpolationAndGradient(new Tuple<>(inLink.getValue().getNx0()[tBefore],inLink.getValue().getNx0()[tAfter]),
								new Tuple<>(S_irBefore,S_irAfter),
								new Tuple<>(inLink.getValue().getdNx0()[tBefore],inLink.getValue().getdNx0()[tAfter]),
								new Tuple<>(dS_irBefore.getData(),dS_irAfter.getData()),
								nlPlusSi,
								dnlPlusSi.getData());
						Tuple<Double,double[]> S_irdtTuple = LTMUtils.calcLinearInterpolationAndGradient(new Tuple<>(inLink.getValue().getNx0()[tBefore],inLink.getValue().getNx0()[tAfter]),
								new Tuple<>(S_irBefore,S_irAfter),
								new Tuple<>(new double[] {inLink.getValue().getNx0dt()[tBefore]},new double[] {inLink.getValue().getNx0dt()[tAfter]}),
								new Tuple<>(new double[] {S_irBeforedt},new double[] {S_irAfterdt}),
								nlPlusSi,
								dnlPlusSi.getData());

						S_ir = S_irTuple.getFirst()-inLink.getValue().getNrxl()[rInd][timeStep];
						if(this.variables!=null)dS_ir = MatrixUtils.createRealVector(S_irTuple.getSecond()).subtract(inLink.getValue().getdNrxl()[rInd][timeStep]).getData();
						S_irdt = S_irdtTuple.getSecond()[0]-inLink.getValue().getNrxldt()[rInd][timeStep];
					}

					this.S_ir.get(inLink.getKey())[rInd][timeStep] = S_ir;
					this.S_irdt.get(inLink.getKey())[rInd][timeStep] = S_irdt;
					if(this.variables!=null)this.dS_ir.get(inLink.getKey())[rInd][timeStep] = dS_ir;


					turns.get(inLink.getKey()).compute(toLink, (k,v)->v==null?S_ir:v+S_ir);
					if(this.variables!=null) {
						double[] a = dS_ir;
						turnsGrad.get(inLink.getKey()).compute(toLink, (k,v)->v==null?MatrixUtils.createRealVector(a):v.add(a));
					}
					turnsdt.get(inLink.getKey()).compute(toLink, (k,v)->v==null?S_irdt:v+S_irdt);
					i++;
				}
				turns.get(inLink.getKey()).entrySet().forEach(e->{
					if(!this.S_ij.get(inLink.getKey()).containsKey(e.getKey()))this.S_ij.get(inLink.getKey()).put(e.getKey(), new double[T]);
					if(!this.S_ijdt.get(inLink.getKey()).containsKey(e.getKey()))this.S_ijdt.get(inLink.getKey()).put(e.getKey(), new double[T]);
					if(this.variables!=null && !this.dS_ij.get(inLink.getKey()).containsKey(e.getKey()))this.dS_ij.get(inLink.getKey()).put(e.getKey(), new double[T][this.variables.getKeySet().size()]);
					this.S_ij.get(inLink.getKey()).get(e.getKey())[timeStep] = e.getValue();
					this.S_ijdt.get(inLink.getKey()).get(e.getKey())[timeStep] = turnsdt.get(inLink.getKey()).get(e.getKey());
					if(this.variables!=null)this.dS_ij.get(inLink.getKey()).get(e.getKey())[timeStep] = turnsGrad.get(inLink.getKey()).get(e.getKey()).getData();
				});
			}
		}

		for(Entry<Id<Link>, LinkModel> outLink: this.outLinkModels.entrySet()) {
			if(outLink.getValue().getRoutes()!=null) {
				TuplesOfThree<double[], double[], double[][]> R = outLink.getValue().getRecivingFlow(timeStep);
				this.R_j.get(outLink.getKey())[timeStep] = R.getFirst()[timeStep];
				this.R_jdt.get(outLink.getKey())[timeStep] = R.getSecond()[timeStep];
				if(this.variables!=null)this.dR_j.get(outLink.getKey())[timeStep] = R.getThird()[timeStep];
			}
		}
	}
	@Override
	public void performLTMStep(int timeStep) {
		this.generateIntendedTurnRatio(timeStep);
		this.applyNodeModel(timeStep);
		this.updateFlow(timeStep);
	}
	
	@Override
	public Node getNode() {
		return this.node;
	}
	@Override
	public void addOriginNode(OriginNodeModel node) {
		this.originNodeModels.put(node.getRoute().getFirst(), node);
		this.inLinkModels.put(node.getOutLinkModel().getLink().getId(), node.getOutLinkModel());
	}
	@Override
	public void addDestinationNode(DestinationNodeModel node) {
		this.destinationNodeModels.put(node.getRoute().getFirst(), node);
		this.outLinkModels.put(node.getInLinkModel().getLink().getId(), node.getInLinkModel());
	}
	@Override
	public void reset() {
		this.dturn.clear();
		this.turn.clear();
		this.turndt.clear();
		
		this.turnRatio.clear();
		this.dturnRatio.clear();
		this.turnRatiodt.clear();
		
		this.S_i.clear();
		this.S_idt.clear();
		this.dS_i.clear();
		
		this.S_ij.clear();
		this.dS_ij.clear();
		this.S_ijdt.clear();
		
		this.R_j.clear();
		this.R_jdt.clear();
		this.dR_j.clear();
		
		this.G_ij.clear();
		this.dG_ij.clear();
		this.G_ijdt.clear();
	}
	
}
