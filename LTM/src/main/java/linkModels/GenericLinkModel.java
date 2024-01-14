package linkModels;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math.linear.MatrixUtils;
import org.jboss.logging.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;

import nodeModels.OriginNodeModel;
import utils.LTMUtils;
import utils.MapToArray;
import utils.TuplesOfThree;
import utils.VariableDetails;

/**
 * 
 * @author ashrafzaman
 *
 */



public class GenericLinkModel implements LinkModel{
	
	public double Nx0Sum = 0;
	public static int globalTimeStep = 0;
	private Logger logger = Logger.getLogger(GenericLinkModel.class);
	private final Link link;
	private double[] timePoints;// time slots
	private double[][] Nrx0;// flow at x0 for route r//note: this will be set from the node model directly. 
	// For source nodes, this term will be instatiated from the route demand. Summing up all the Nrx0 at a certain time step 
	// should provide for the Nx0. The rest of the time, this term will come from the node model G_ij assuming FIFO behavior
	private double[][] Nrx0dt;//timegradient of Nrx0
	private double[][][]dNrx0;// gradient of flow at x0 for route r
	private double[][] Nrxl;//flow at xl for route r
	private double[][]Nrxldt;// timeGradient of Nrxl
	private double[][][]dNrxl;//gradient of flow at xl for route r
	private double[] Nx0;//flow at x0
	private double[] Nx0dt;//time gradient of Nx0
	private double[][]dNx0;//gradient of flow at x0
	private double[] Nxl;//flow at xl
	private double[] Nxldt;//time gradient for Nxl
	private double[][] dNxl;//gradient for flow at xl
//	private double[] S;//sending flow
//	private double[] R;//receiving flow
//	private double[][] dS;//gradient of sending flow
//	private double[][] dR;//gradient of receiving flow
//	private double[] Sdt;//time gradient of S
//	private double[] Rdt;//time gradient of R
	private double[]k;// number of vehicles on the road 
	private MapToArray<Id<NetworkRoute>> routesId;// map to array converter for the routes on this link
	private Map<Id<NetworkRoute>,NetworkRoute> routes;
	private int T;//The number of time slots
	private FD fd;// Triangular fundamental diagram with vf, vw, qm, kj
	private double delT;//length of each time slots
	private MapToArray<VariableDetails> variables;//Map to array converter for variables to calculate gradient with respect to, this can be shared all over
	
	
//	private double[][][] dNrx0;
//	private double[][][] dNrxl;
//	private double[][] dNx0;
//	private double[][] dNxl;
//	private double[][] dk;
	
	
	public GenericLinkModel (Link l) {
		this.link = l;
		this.fd = new FD(l);
	}
	
	public void setOptimizationVariables(MapToArray<VariableDetails> variables) {
		this.variables = variables;
		this.dNx0 = new double[this.T][this.variables.getKeySet().size()];
		this.dNxl = new double[this.T][this.variables.getKeySet().size()];
		this.dNrx0 = new double[routesId.getKeySet().size()][T][this.variables.getKeySet().size()];
		this.dNrxl = new double[routesId.getKeySet().size()][T][this.variables.getKeySet().size()];
//		this.dS = new double[T][this.variables.getKeySet().size()];
//		this.dR = new double[T][this.variables.getKeySet().size()];
	}
	
	@Override
	public void setLTMTimeBeanAndRouteSet(double[]timePoints, Map<Id<NetworkRoute>,NetworkRoute> routes) {
		this.timePoints = timePoints;
		this.T = timePoints.length;
		this.delT = timePoints[1]-timePoints[0];
 		this.routes = routes;
 		this.routesId = new MapToArray<Id<NetworkRoute>>("routes",routes.keySet());
		Nrx0 = new double[routes.size()][T];
		Nrx0dt = new double[routes.size()][T];
		Nrxl = new double[routes.size()][T];
		Nrxldt = new double[routes.size()][T];
		Nx0 = new double[T];
		Nx0dt = new double[T];
		Nxl = new double[T];
		Nxldt = new double[T];
		k = new double[T];
//		S = new double[T];
//		Sdt = new double[T];
//		R = new double[T];
//		Rdt = new double[T];
	}

	@Override
	public int getTimeIndex(double t) {

		for(int i = timePoints.length-1; i>=0;i--) {
			if(t>=timePoints[i])return i;
		}
		return 0;
	}


	@Override
	public TuplesOfThree<Double,Double,double[]> getSendingFlow(int timeIdx) {
//			if(fd.L/fd.vf<delT) {
//				throw new IllegalArgumentException("Reduce time step size!!! L/vf is greater than time step size!!!");
//			}
			double t = timePoints[timeIdx]+delT-fd.L/fd.vf;// get the time index which flow is relevant
			int tl = this.getTimeIndex(t);// get the time point which is just lower than or equal to t
			double N = 0;
			double s = 0;
			double sdt = 0;
			double[] ds = null;
			if(this.variables!=null) {
				Tuple<Double, double[]> Ne = LTMUtils.calcLinearInterpolationAndGradient(new Tuple<Double,Double>(this.timePoints[tl],this.timePoints[tl+1]), new Tuple<Double,Double>(this.Nx0[tl],this.Nx0[tl+1]),
						new Tuple<double[],double[]>(new double[this.variables.getKeySet().size()],new double[this.variables.getKeySet().size()]), 
						new Tuple<double[],double[]>(this.dNx0[tl],this.dNx0[tl+1]), t, new double[this.variables.getKeySet().size()]); 
				Tuple<Double, double[]> Nedt = LTMUtils.calcLinearInterpolationAndGradient(new Tuple<Double,Double>(this.timePoints[tl],this.timePoints[tl+1]), new Tuple<Double,Double>(this.Nx0[tl],this.Nx0[tl+1]),
						new Tuple<double[],double[]>(new double[1],new double[1]), 
						new Tuple<double[],double[]>(new double[] {this.Nx0dt[tl]},new double[] {this.Nx0dt[tl+1]}), t, new double[1]); 
				N = Ne.getFirst();
				double[] dN = Ne.getSecond();
				double Ndt = Nedt.getSecond()[0];
				s = Math.min(N-this.Nxl[timeIdx], fd.qm*delT);
				ds = new double[this.variables.getKeySet().size()];
				
				if(s!=fd.qm*delT) {
					ds = MatrixUtils.createRealVector(dN).subtract(MatrixUtils.createRealVector(this.dNxl[timeIdx])).getData();
					sdt = Ndt-Nxldt[timeIdx];
				}else {
					sdt = fd.qm;
				}
//				this.dS[timeIdx] = ds;
//				this.Sdt[timeIdx] = sdt;
				
				if(Double.isNaN(s)||Double.isInfinite(s)||s<0||s>this.Nx0[timeIdx]) {
					logger.debug("sending flow is nan or infinite!");
				}
				if(Double.isNaN(sdt)||Double.isInfinite(sdt)) {
					logger.debug("sending flow time gradient is nan or infinite!");
				}
				
				if(MatrixUtils.createRealVector(ds).isNaN()||MatrixUtils.createRealVector(ds).isInfinite()) {
					logger.debug("sending flow gradient is nan or infinite!");
				}
				
			}else {
				N = this.Nx0[tl]+(this.Nx0[tl]-this.Nx0[tl+1])*(t-timePoints[tl])/delT;//interpolate between the cumulative flow of tl and tu = tl+1 to gert the cumulative flow at t. 
				s = Math.min(N-this.Nxl[timeIdx], fd.qm*delT);
				
				if(Double.isNaN(s)||Double.isInfinite(s)||s<0) {
					logger.debug("sending flow is nan or infinite or negative!");
				}
			}
//			this.S[timeIdx] = s;
//			if(s>0 && OriginNodeModel.arraySum(ds)==0) {
//				logger.debug("ds is zero even though s is non zero.");
//			}
		
		return new TuplesOfThree<>(s,sdt,ds);// the sending flow is the min between this two term 
	}



	@Override
	public TuplesOfThree<Double,Double,double[]> getRecivingFlow(int timeIdx) {
		//for(int timeIdx = 0; timeIdx<this.T;timeIdx++) {
		double t = timePoints[timeIdx]+delT+fd.L/fd.wf;// get the time index which flow is relevant
		double r = 0;
		double[] dr = null;
		double rdt = 0;
		if(t>0) {
			int tl = this.getTimeIndex(t);// get the time point which is just lower that t
			double N = 0;
			
			if(this.variables!=null) {
				Tuple<Double, double[]> Ne = LTMUtils.calcLinearInterpolationAndGradient(new Tuple<Double,Double>(this.timePoints[tl],this.timePoints[tl+1]), new Tuple<Double,Double>(this.Nxl[tl],this.Nxl[tl+1]), 
						new Tuple<double[],double[]>(new double[this.variables.getKeySet().size()],new double[this.variables.getKeySet().size()]), 
						new Tuple<double[],double[]>(this.dNxl[tl],this.dNxl[tl+1]),t, new double[this.variables.getKeySet().size()]); 
				Tuple<Double, double[]> Nedt = LTMUtils.calcLinearInterpolationAndGradient(new Tuple<Double,Double>(this.timePoints[tl],this.timePoints[tl+1]), new Tuple<Double,Double>(this.Nxl[tl],this.Nxl[tl+1]),
						new Tuple<double[],double[]>(new double[1],new double[1]), 
						new Tuple<double[],double[]>(new double[] {this.Nxldt[tl]},new double[] {this.Nxldt[tl+1]}), t, new double[1]); 
				N = Ne.getFirst();
				N = Ne.getFirst();
				r = Math.min(N+fd.kj*fd.L-this.Nx0[timeIdx], fd.qm*delT);
				double[] dN = Ne.getSecond();
				double Ndt = Nedt.getSecond()[0];
				dr = new double[this.variables.getKeySet().size()];
				// Here we assume that d(fd.Kj*fd.L) is zero. 
				if(r != fd.qm*delT) {
					dr = MatrixUtils.createRealVector(dN).subtract(MatrixUtils.createRealVector(this.dNx0[timeIdx])).getData();
					rdt = Ndt-Nx0dt[timeIdx];
				}else {
					rdt = fd.qm;
				}
//				this.dR[timeIdx] = dr;
//				this.Rdt[timeIdx] = rdt;
				if(Double.isNaN(r)||Double.isInfinite(r)||r<0) {
					logger.debug("Reciving flow is nan or infinite!");
				}
				if(Double.isNaN(rdt)||Double.isInfinite(rdt)) {
					logger.debug("Reciving flow time gradient is nan or infinite!");
				}
				
				if(MatrixUtils.createRealVector(dr).isNaN()||MatrixUtils.createRealVector(dr).isInfinite()) {
					logger.debug("Reciving flow gradient is nan or infinite!");
				}
			}else {
				N = this.Nxl[tl]+(this.Nxl[tl]-this.Nxl[tl+1])*(t-timePoints[tl])/delT;//interpolate between the cumulative flow of tl and tu = tl+1 to gert the cumulative flow at t. 
				r = Math.min(N+fd.kj*fd.L-this.Nx0[timeIdx], fd.qm*delT);
				if(Double.isNaN(r)||Double.isInfinite(r)||r<0) {
					logger.debug("Reciving flow is nan or infinite!");
				}
			}
			//this.R[timeIdx] = r;
		}
		//}
		return new TuplesOfThree<>(r,rdt,dr);// the sending flow is the min between this two term 
	}



	@Override
	public void updateNx0(double flow, int timeIndx,double[] dNx0) {
		this.Nx0[timeIndx] = flow;
		if(dNx0!=null)this.dNx0[timeIndx]=dNx0;
	}



	@Override
	public void updateNxl(double flow, int timeInd,double[] dNxl) {
		this.Nxl[timeInd] = flow;
		if(dNxl!=null)this.dNxl[timeInd]=dNxl;
	}



	@Override
	public void updateNrx0(double flow, Id<NetworkRoute> routeId, int timeIndx,double[] dNrx0) {
		int ind = this.routesId.getIndex(routeId);
		if(ind ==-1)throw new IllegalArgumentException("Route not present in the route list!!! Debug!!!");
		this.Nrx0[ind][timeIndx] = flow;
		if(dNrx0!=null)this.dNrx0[ind][timeIndx]=dNrx0;
	}



	@Override
	public void updateNrxl(double flow, Id<NetworkRoute> route, int timeInd,double[] dNrxl) {
		int ind = this.routesId.getIndex(route);
		if(ind ==-1)throw new IllegalArgumentException("Route not present in the route list!!! Debug!!!");
		this.Nrxl[ind][timeInd] = flow;
		if(dNrxl!=null)this.dNrxl[ind][timeInd]=dNrxl;
	}
	
	@Override
	public boolean checkNx0SumChange() {
		double sum = 0;
		for(double d:this.Nx0) {
			sum+=d;
		}
		if(this.Nx0Sum-sum!=0) {
			this.Nx0Sum = sum;
			return true;
		}
		return false;
		
	}
	
	
	//_____________________________Getter Setter_________________________________________________
	
	@Override
	public Link getLink() {
		return link;
	}




	@Override
	public double[] getTimePoints() {
		return timePoints;
	}



	@Override
	public double[][] getNrx0() {
		return Nrx0;
	}

	
	@Override
	public double[][] getNrxl() {
		return Nrxl;
	}


	@Override
	public double[] getNx0() {
		return Nx0;
	}



	@Override
	public double[] getNxl() {
		return Nxl;
	}


	@Override
	public double[] getK() {
		return k;
	}


	@Override
	public MapToArray<Id<NetworkRoute>> getRouteIds() {
		return routesId;
	}
	
	
	@Override
	public Map<Id<NetworkRoute>, NetworkRoute> getRoutes() {
		return routes;
	}

	@Override
	public double[][][] getdNrx0() {
		return dNrx0;
	}
	@Override
	public double[][] getNrxldt() {
		return Nrxldt;
	}
	@Override
	public double[][][] getdNrxl() {
		return dNrxl;
	}
	@Override
	public double[] getNx0dt() {
		return Nx0dt;
	}
	@Override
	public double[][] getNrx0dt() {
		return Nrx0dt;
	}

	public double[][] getdNx0() {
		return dNx0;
	}

	public double[] getNxldt() {
		return Nxldt;
	}

	public double[][] getdNxl() {
		return dNxl;
	}

	@Override
	public int getTimeindexNo() {
		return T;
	}
	@Override
	public MapToArray<VariableDetails> getVariables() {
		return variables;
	}

	@Override
	public void reset() {
		Nrx0 = new double[routes.size()][T];
		Nrx0dt = new double[routes.size()][T];
		Nrxl = new double[routes.size()][T];
		Nrxldt = new double[routes.size()][T];
		Nx0 = new double[T];
		Nx0dt = new double[T];
		Nxl = new double[T];
		Nxldt = new double[T];
		k = new double[T];
		this.dNx0 = new double[this.T][this.variables.getKeySet().size()];
		this.dNxl = new double[this.T][this.variables.getKeySet().size()];
		this.dNrx0 = new double[routes.size()][T][this.variables.getKeySet().size()];
		this.dNrxl = new double[routes.size()][T][this.variables.getKeySet().size()];

	}

//	@Override
//	public Tuple<Map<NetworkRoute, Double>,Map<NetworkRoute,double[]>> getNrxl(int timeIndex) {
//		Map<NetworkRoute,Double> flow = new HashMap<>();
//		Map<NetworkRoute,double[]> flowGrad = new HashMap<>();
//		
//		int k = 0;
//		for(NetworkRoute r:this.routes.getKeySet()) {
//			flow.put(r, this.Nrxl[k][timeIndex]);
//			flowGrad.put(r, this.dNrxl[k][timeIndex]);
//			k++;
//		}
//		return new Tuple<>(flow,flowGrad);
//	}
//
//	@Override
//	public Tuple<Map<NetworkRoute, Double>,Map<NetworkRoute,double[]>> getNrx0(int timeIndex) {
//		Map<NetworkRoute,Double> flow = new HashMap<>();
//		Map<NetworkRoute,double[]> flowGrad = new HashMap<>();
//		
//		int k = 0;
//		for(NetworkRoute r:this.routes.getKeySet()) {
//			flow.put(r, this.Nrx0[k][timeIndex]);
//			flowGrad.put(r, this.dNrx0[k][timeIndex]);
//			k++;
//		}
//		return new Tuple<>(flow,flowGrad);
//	}


}

class FD{
	public final double vf;
	public final double qm;
	public final double kj;
	public final double wf;
	public final double L;
	
	public FD(double L) {
		this.L = L;
		vf = 16.67;
		qm = .5;
		kj = 0.15;
		wf = -1*qm/kj;
	}
	
	public FD(double L, double vf, double qm, double kj) {
		this.vf = vf;
		this.qm = qm;
		this.kj = kj;
		wf = -1*qm/kj;
		this.L = L;
	}
	
	public FD(Link link) {
		this(link.getLength(),link.getFreespeed(),link.getCapacity()/3600,0.15);// K_j = 0.15 is assuming a jam density of 250 veh/mile
	}
	
}
