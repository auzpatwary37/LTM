package nodeModels;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Set;

import org.apache.commons.math.linear.MatrixUtils;
import org.apache.commons.math.linear.RealVector;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;

import linkModels.GenericLinkModel;
import linkModels.LinkModel;
import utils.LTMUtils;
import utils.MapToArray;
import utils.TuplesOfThree;
import utils.VariableDetails;

public class GenericNodeModel implements NodeModel{
	
	private final Node node;
	
	private int T;//The number of time slots
	private Map<Id<Link>,LinkModel> inLinkModels = new HashMap<>();// Link models for all the in links
	private Map<Id<Link>,LinkModel> outLinkModels = new HashMap<>();// Link models for all the out links 
	private MapToArray<NetworkRoute> routes;// Map to array for all the routes going through this node 
	private MapToArray<VariableDetails> variables;// Map to array for all the variables that the gradients are supposed to be calculated for; this can be shared over all over the system
	private Map<Id<Link>,Map<Id<Link>,double[]>> turnRatio = new HashMap<>();// the turn ratio, this can be static or dynamic 
	private Map<Id<Link>,Map<Id<Link>,double[]>> turnRatiodt = new HashMap<>();// the turn ratio gradient with respect to time, this can be static or dynamic 
	private Map<Id<Link>,Map<Id<Link>,double[][]>> dturnRatio = new HashMap<>();// the turn ratio gradient with respect to the variables, this can be static or dynamic 
	
	private Map<Id<Link>,Map<Id<Link>,double[]>> turn = new HashMap<>();// the turn ratio, this can be static or dynamic 
	private Map<Id<Link>,Map<Id<Link>,double[]>> turndt = new HashMap<>();// the turn ratio gradient with respect to time, this can be static or dynamic 
	private Map<Id<Link>,Map<Id<Link>,double[][]>> dturn = new HashMap<>();// the turn ratio gradient with respect to the variables, this can be static or dynamic 
	private Map<Id<Link>,Map<Id<Link>,Set<NetworkRoute>>> linkToLinkRouteIncidence = new HashMap<>();
	
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
	
	
	
	
	public GenericNodeModel(Node node, Map<Id<Link>,LinkModel>linkModels) {
		
	
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

	public void setTimeStepAndRoutes() {
		
	}



	@Override
	public void generateTurnRatio(int timeStep) {//This is what actually happened, i.e., from calculated Nr, this method can be invoked to calculate the turns after the Nr has been calculated from g_ij.
		Map<Id<Link>,Map<Id<Link>,Double>> turns = new HashMap<>();
		Map<Id<Link>,Map<Id<Link>,RealVector>> turnsGrad = new HashMap<>();
		Map<Id<Link>,Map<Id<Link>,Double>> turnsdt = new HashMap<>();

		for(Entry<Id<Link>, LinkModel> inLink:this.inLinkModels.entrySet()) {
			turns.put(inLink.getKey(), new HashMap<>());
			int i = 0;
			for(NetworkRoute r:inLink.getValue().getRoutes().getKeySet()) {
				Id<Link>toLink = LTMUtils.findNextLink(r, inLink.getKey());

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
				turnsGrad.get(inLink.getKey()).compute(toLink, (k,v)->v==null?MatrixUtils.createRealVector(inLink.getValue().getdNrxl()[rInd][timeStep]):v.add(inLink.getValue().getdNrxl()[rInd][timeStep]));
				turnsdt.get(inLink.getKey()).compute(toLink, (k,v)->v==null?inLink.getValue().getNrxldt()[rInd][timeStep]:v+inLink.getValue().getNrxldt()[rInd][timeStep]);
				i++;
			}
			
			Map<Id<Link>,double[]> o = turnsGrad.get(inLink.getKey()).entrySet().stream().collect(Collectors.toMap(k->k.getKey(), k->k.getValue().getData()));
			Map<Id<Link>,double[]> ot = turnsdt.get(inLink.getKey()).entrySet().stream().collect(Collectors.toMap(k->k.getKey(), k->new double[] {k.getValue()}));

			Map<Id<Link>, Tuple<Double, double[]>> outd = LTMUtils.calculateRatioAndGradient(turns.get(inLink.getKey()), o);
			Map<Id<Link>, Tuple<Double, double[]>> outdt = LTMUtils.calculateRatioAndGradient(turns.get(inLink.getKey()), ot);
			//this.turnRatio.put(inLink.getKey(), ot)
			outd.entrySet().forEach(e->{
				this.turnRatio.get(inLink.getKey()).get(e.getKey())[timeStep] = outd.get(e.getKey()).getFirst();
				this.turn.get(inLink.getKey()).get(e.getKey())[timeStep] = turns.get(inLink.getKey()).get(e.getKey());
				this.dturnRatio.get(inLink.getKey()).get(e.getKey())[timeStep] = outd.get(e.getKey()).getSecond();
				this.dturn.get(inLink.getKey()).get(e.getKey())[timeStep] = turnsGrad.get(inLink.getKey()).get(e.getKey()).getData();
				this.turnRatiodt.get(inLink.getKey()).get(e.getKey())[timeStep] = outdt.get(e.getKey()).getSecond()[0];
				this.turndt.get(inLink.getKey()).get(e.getKey())[timeStep] = turnsdt.get(inLink.getKey()).get(e.getKey());
			});

		}
	}
		
	@Override
	public void applyNodeModel(int timeStep) {
		
		
	}


	@Override
	public void updateFlow(int timeStep) {
		
		Map<Id<Link>,Double> gi = new HashMap<>();
		Map<Id<Link>,Double> gidt = new HashMap<>();
		Map<Id<Link>,double[]> dgi = new HashMap<>();
		
		Map<Id<Link>,Double> gj = new HashMap<>();
		Map<Id<Link>,Double> gjdt = new HashMap<>();
		Map<Id<Link>,double[]> dgj = new HashMap<>();
		
		
		this.inLinkModels.entrySet().forEach(inLink->{
			
			for(Entry<Id<Link>, double[]> d:this.G_ij.get(inLink.getKey()).entrySet()){
				//update gi
				gi.compute(inLink.getKey(), (k,v)->v==null?d.getValue()[timeStep]:v+d.getValue()[timeStep]);
				gidt.compute(inLink.getKey(), (k,v)->v==null?this.G_ijdt.get(inLink.getKey()).get(d.getKey())[timeStep]:v+this.G_ijdt.get(inLink.getKey()).get(d.getKey())[timeStep]);
				dgi.compute(inLink.getKey(), (k,v)->v==null?this.dG_ij.get(inLink.getKey()).get(d.getKey())[timeStep]:
					MatrixUtils.createRealVector(this.dG_ij.get(inLink.getKey()).get(d.getKey())[timeStep]).add(v).getData());
				
				//update gj
				gj.compute(d.getKey(), (k,v)->v==null?d.getValue()[timeStep]:v+d.getValue()[timeStep]);
				gjdt.compute(d.getKey(), (k,v)->v==null?this.G_ijdt.get(inLink.getKey()).get(d.getKey())[timeStep]:v+this.G_ijdt.get(inLink.getKey()).get(d.getKey())[timeStep]);
				dgj.compute(d.getKey(), (k,v)->v==null?this.dG_ij.get(inLink.getKey()).get(d.getKey())[timeStep]:
					MatrixUtils.createRealVector(this.dG_ij.get(inLink.getKey()).get(d.getKey())[timeStep]).add(v).getData());
			}
			
		});
		
		//update of Nxl and Nx0 at time step timeStep+1
		for(Entry<Id<Link>, LinkModel> inLink:this.inLinkModels.entrySet()) {
			inLink.getValue().getNxl()[timeStep+1] = inLink.getValue().getNxl()[timeStep]+gi.get(inLink.getKey());
			inLink.getValue().getdNxl()[timeStep+1] = MatrixUtils.createRealVector(inLink.getValue().getdNxl()[timeStep]).add(dgi.get(inLink.getKey())).getData();
			inLink.getValue().getNxldt()[timeStep+1] = inLink.getValue().getNxldt()[timeStep]+gidt.get(inLink.getKey());
		}
		
		for(Entry<Id<Link>, LinkModel> outLink:this.outLinkModels.entrySet()) {
			outLink.getValue().getNx0()[timeStep+1] = outLink.getValue().getNx0()[timeStep]+gj.get(outLink.getKey());
			outLink.getValue().getdNx0()[timeStep+1] = MatrixUtils.createRealVector(outLink.getValue().getdNx0()[timeStep]).add(dgj.get(outLink.getKey())).getData();
			outLink.getValue().getNx0dt()[timeStep+1] = outLink.getValue().getNx0dt()[timeStep]+gjdt.get(outLink.getKey());
		}
		
		//Update the route specific flow first at the end of the inLinks
		
		for(Entry<Id<Link>, LinkModel> inLink:this.inLinkModels.entrySet()) {
			double flow = inLink.getValue().getNxl()[timeStep+1];
			int tBefore = timeStep;
			int tAfter = timeStep;
			for(int j = timeStep-1;j>0;j--) {
				if(flow<=inLink.getValue().getNx0()[j])tAfter = j;
				if(flow>=inLink.getValue().getNx0()[j]) {
					tBefore = j;
					break;
				}
			}
			
			RealVector tBeforeGrad = MatrixUtils.createRealVector(inLink.getValue().getdNxl()[timeStep+1]).mapMultiply(1/inLink.getValue().getNx0dt()[tBefore]);
			double tBeforedt = inLink.getValue().getNxldt()[timeStep+1]*1/inLink.getValue().getNx0dt()[tBefore];
			RealVector tAfterGrad = MatrixUtils.createRealVector(inLink.getValue().getdNxl()[timeStep+1]).mapMultiply(1/inLink.getValue().getNx0dt()[tAfter]);
			double tAfterdt = inLink.getValue().getNxldt()[timeStep+1]*1/inLink.getValue().getNx0dt()[tAfter];
			int i = 0;
			for(NetworkRoute r:inLink.getValue().getRoutes().getKeySet()) {
				int rInd = i;
				Id<Link>toLink = LTMUtils.findNextLink(r, inLink.getKey());
				
				double NxrlBefore = inLink.getValue().getNrx0()[rInd][tBefore];
				RealVector dNxrlBefore = tBeforeGrad.ebeMultiply(inLink.getValue().getdNrx0()[rInd][tBefore]);
				double NxrlBeforedt = inLink.getValue().getNrx0dt()[rInd][tBefore]*tBeforedt;
				
				
				double NxrlAfter = inLink.getValue().getNrx0()[rInd][tAfter];
				RealVector dNxrlAfter = tAfterGrad.ebeMultiply(inLink.getValue().getdNrx0()[rInd][tAfter]);
				double NxrlAfterdt = inLink.getValue().getNrx0dt()[rInd][tAfter]*tAfterdt;
				//interpolation
				double Nrxl;
				double[] dNrxl;
				double Nrxldt;
				if(tBefore==tAfter) {
					Nrxl = NxrlBefore;
					dNrxl = dNxrlBefore.getData();
					Nrxldt = NxrlBeforedt;
				}else {//interpolate
					Tuple<Double,double[]> NxrlTuple = LTMUtils.calcLinearInterpolationAndGradient(new Tuple<>(inLink.getValue().getNx0()[tBefore],inLink.getValue().getNx0()[tAfter]),
							new Tuple<>(NxrlBefore,NxrlAfter),
							new Tuple<>(inLink.getValue().getdNx0()[tBefore],inLink.getValue().getdNx0()[tAfter]),
							new Tuple<>(dNxrlBefore.getData(),dNxrlAfter.getData()),
							flow,
							inLink.getValue().getdNxl()[timeStep+1]);
					Tuple<Double,double[]> NxrldtTuple = LTMUtils.calcLinearInterpolationAndGradient(new Tuple<>(inLink.getValue().getNx0()[tBefore],inLink.getValue().getNx0()[tAfter]),
							new Tuple<>(NxrlBefore,NxrlAfter),
							new Tuple<>(new double[] {inLink.getValue().getNx0dt()[tBefore]},new double[] {inLink.getValue().getNx0dt()[tAfter]}),
							new Tuple<>(new double[] {NxrlBeforedt},new double[] {NxrlAfterdt}),
							flow,
							inLink.getValue().getdNxl()[timeStep+1]);
					
					Nrxl = NxrlTuple.getFirst()-inLink.getValue().getNrxl()[rInd][timeStep];
					dNrxl = NxrlTuple.getSecond();
					Nrxldt = NxrldtTuple.getSecond()[0];
				}
				inLink.getValue().getNrxl()[rInd][timeStep+1] = Nrxl;
				inLink.getValue().getdNrxl()[rInd][timeStep+1] = dNrxl;
				inLink.getValue().getNrxldt()[rInd][timeStep+1] = Nrxldt;
				
				//update the toLink's Nrx0
				
				int outRouteInd = this.outLinkModels.get(toLink).getRoutes().getIndex(r);
				this.outLinkModels.get(toLink).getNrx0()[outRouteInd][timeStep+1] = Nrxl;
				this.outLinkModels.get(toLink).getdNrx0()[outRouteInd][timeStep+1] = dNrxl;
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
			turns.put(inLink.getKey(), new HashMap<>());
			turnsGrad.put(inLink.getKey(), new HashMap<>());
			turnsdt.put(inLink.getKey(), new HashMap<>());
			
			if(!this.S_i.containsKey(inLink.getKey()))this.S_i.put(inLink.getKey(), new double[T]);
			if(!this.S_idt.containsKey(inLink.getKey()))this.S_idt.put(inLink.getKey(), new double[T]);
			if(!this.dS_i.containsKey(inLink.getKey()))this.dS_i.put(inLink.getKey(), new double[T][this.variables.getKeySet().size()]);
			
			
			if(!this.S_ir.containsKey(inLink.getKey()))this.S_ir.put(inLink.getKey(), new double[inLink.getValue().getRoutes().getKeySet().size()][T]);
			if(!this.S_irdt.containsKey(inLink.getKey()))this.S_irdt.put(inLink.getKey(), new double[inLink.getValue().getRoutes().getKeySet().size()][T]);
			if(!this.dS_ir.containsKey(inLink.getKey()))this.dS_ir.put(inLink.getKey(), new double[inLink.getValue().getRoutes().getKeySet().size()][T][this.variables.getKeySet().size()]);
			
			if(!this.S_ij.containsKey(inLink.getKey()))this.S_ij.put(inLink.getKey(), new HashMap<>());
			if(!this.S_ijdt.containsKey(inLink.getKey()))this.S_ijdt.put(inLink.getKey(), new HashMap<>());
			if(!this.dS_ij.containsKey(inLink.getKey()))this.dS_ij.put(inLink.getKey(), new HashMap<>());
			
			
			
			TuplesOfThree<double[], double[], double[][]> S_ij = inLink.getValue().getSendingFlow(timeStep);
			this.S_i.get(inLink.getKey())[timeStep] = S_ij.getFirst()[timeStep];
			this.S_idt.get(inLink.getKey())[timeStep] = S_ij.getSecond()[timeStep];
			this.dS_i.get(inLink.getKey())[timeStep] = S_ij.getThird()[timeStep];
			
			double nlPlusSi = inLink.getValue().getNxl()[timeStep]+S_ij.getFirst()[timeStep];
			RealVector dnlPlusSi = MatrixUtils.createRealVector(inLink.getValue().getdNxl()[timeStep]).add(S_ij.getThird()[timeStep]);
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
			
			RealVector tBeforeGrad = dnlPlusSi.mapMultiply(1/inLink.getValue().getNx0dt()[tBefore]);
			double tBeforedt = nlPlusSidt*1/inLink.getValue().getNx0dt()[tBefore];
			RealVector tAfterGrad = dnlPlusSi.mapMultiply(1/inLink.getValue().getNx0dt()[tAfter]);
			double tAfterdt = nlPlusSidt*1/inLink.getValue().getNx0dt()[tAfter];
		
			
			int i = 0;
			for(NetworkRoute r:inLink.getValue().getRoutes().getKeySet()) {
				int rInd = i;
				Id<Link>toLink = LTMUtils.findNextLink(r, inLink.getKey());
				
				
				if(!this.S_ij.get(inLink.getKey()).containsKey(toLink))this.S_ij.get(inLink.getKey()).put(toLink, new double[T]);
				if(!this.S_ijdt.get(inLink.getKey()).containsKey(toLink))this.S_ijdt.get(inLink.getKey()).put(toLink, new double[T]);
				if(!this.dS_ij.get(inLink.getKey()).containsKey(toLink))this.dS_ij.get(inLink.getKey()).put(toLink, new double[T][this.variables.getKeySet().size()]);
				
				if(!this.R_j.containsKey(toLink))this.R_j.put(toLink, new double[T]);
				if(!this.R_jdt.containsKey(toLink))this.R_jdt.put(toLink, new double[T]);
				if(!this.dR_j.containsKey(toLink))this.dR_j.put(toLink, new double[T][this.variables.getKeySet().size()]);
				
				
				double S_irBefore = inLink.getValue().getNrx0()[rInd][tBefore];
				RealVector dS_irBefore = tBeforeGrad.ebeMultiply(inLink.getValue().getdNrx0()[rInd][tBefore]);
				double S_irBeforedt = inLink.getValue().getNrx0dt()[rInd][tBefore]*tBeforedt;
				
				
				double S_irAfter = inLink.getValue().getNrx0()[rInd][tAfter];
				RealVector dS_irAfter = tAfterGrad.ebeMultiply(inLink.getValue().getdNrx0()[rInd][tAfter]);
				double S_irAfterdt = inLink.getValue().getNrx0dt()[rInd][tAfter]*tAfterdt;
				
				//interpolation
				double S_ir;
				double[] dS_ir;
				double S_irdt;
				if(tBefore==tAfter) {
					S_ir = S_irBefore-inLink.getValue().getNrxl()[rInd][timeStep];
					dS_ir = dS_irBefore.subtract(inLink.getValue().getdNrxl()[rInd][timeStep]).getData();
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
				dS_ir = MatrixUtils.createRealVector(S_irTuple.getSecond()).subtract(inLink.getValue().getdNrxl()[rInd][timeStep]).getData();
				S_irdt = S_irdtTuple.getSecond()[0]-inLink.getValue().getNrxldt()[rInd][timeStep];
				}
				
				this.S_ir.get(inLink.getKey())[rInd][timeStep] = S_ir;
				this.S_irdt.get(inLink.getKey())[rInd][timeStep] = S_irdt;
				this.dS_ir.get(inLink.getKey())[rInd][timeStep] = dS_ir;
				
				
				turns.get(inLink.getKey()).compute(toLink, (k,v)->v==null?S_ir:v+S_ir);
				turnsGrad.get(inLink.getKey()).compute(toLink, (k,v)->v==null?MatrixUtils.createRealVector(dS_ir):v.add(dS_ir));
				turnsdt.get(inLink.getKey()).compute(toLink, (k,v)->v==null?S_irdt:v+S_irdt);
				i++;
			}
			turns.get(inLink.getKey()).entrySet().forEach(e->{
				if(!this.S_ij.get(inLink.getKey()).containsKey(e.getKey()))this.S_ij.get(inLink.getKey()).put(e.getKey(), new double[T]);
				if(!this.S_ijdt.get(inLink.getKey()).containsKey(e.getKey()))this.S_ijdt.get(inLink.getKey()).put(e.getKey(), new double[T]);
				if(!this.dS_ij.get(inLink.getKey()).containsKey(e.getKey()))this.dS_ij.get(inLink.getKey()).put(e.getKey(), new double[T][this.variables.getKeySet().size()]);
				this.S_ij.get(inLink.getKey()).get(e.getKey())[timeStep] = e.getValue();
				this.S_ijdt.get(inLink.getKey()).get(e.getKey())[timeStep] = turnsdt.get(inLink.getKey()).get(e.getKey());
				this.dS_ij.get(inLink.getKey()).get(e.getKey())[timeStep] = turnsGrad.get(inLink.getKey()).get(e.getKey()).getData();
			});
		}
		
		for(Entry<Id<Link>, LinkModel> outLink: this.outLinkModels.entrySet()) {
			TuplesOfThree<double[], double[], double[][]> R = outLink.getValue().getRecivingFlow(timeStep);
			this.R_j.get(outLink.getKey())[timeStep] = R.getFirst()[timeStep];
			this.R_jdt.get(outLink.getKey())[timeStep] = R.getSecond()[timeStep];
			this.dR_j.get(outLink.getKey())[timeStep] = R.getThird()[timeStep];
		}
	}
	
	
	
}
