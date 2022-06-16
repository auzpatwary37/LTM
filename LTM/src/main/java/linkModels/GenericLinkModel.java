package linkModels;

import org.apache.commons.math.linear.MatrixUtils;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.Tuple;

import ust.hk.praisehk.metamodelcalibration.Utils.MapToArray;
import utils.LTMUtils;


public class GenericLinkModel implements LinkModel{
	private final Link link;
	private double[] timePoints;// time slots
	private double[][] Nrx0;// flow at x0 for route r
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
	private double[] S;//sending flow
	private double[] R;//receiving flow
	private double[][] dS;//gradient of sending flow
	private double[][] dR;//gradient of receiving flow
	private double[] Sdt;//time gradient of S
	private double[] Rdt;//time gradient of R
	private double[]k;// number of vehicles on the road 
	private MapToArray routes;// map to array converter for the routes on this link
	private int T;//The number of time slots
	private FD fd;// Triangular fundamental diagram with vf, vw, qm, kj
	private double delT;//length of each time slots
	private MapToArray variables;//Map to array converter for variables to calculate gradient with respect to
	
//	private double[][][] dNrx0;
//	private double[][][] dNrxl;
//	private double[][] dNx0;
//	private double[][] dNxl;
//	private double[][] dk;
	
	
	
	public GenericLinkModel (Link l) {
		this.link = l;
		this.fd = new FD(l);
	}
	
	public void setOptimizationVariables(MapToArray variables) {
		this.variables = variables;
		this.dNx0 = new double[this.T][this.variables.getKeySet().size()];
		this.dNxl = new double[this.T][this.variables.getKeySet().size()];
		this.dNrx0 = new double[routes.getKeySet().size()][T][this.variables.getKeySet().size()];
		this.dNrxl = new double[routes.getKeySet().size()][T][this.variables.getKeySet().size()];
		this.dS = new double[T][this.variables.getKeySet().size()];
		this.dR = new double[T][this.variables.getKeySet().size()];
	}
	
	@Override
	public void setLTMTimeBeanAndRouteSet(double[]timePoints, MapToArray routes) {
		this.timePoints = timePoints;
		this.T = timePoints.length;
		this.delT = timePoints[1]-timePoints[0];
		this.routes = routes;
		Nrx0 = new double[routes.getKeySet().size()][T];
		Nrx0dt = new double[routes.getKeySet().size()][T];
		Nrxl = new double[routes.getKeySet().size()][T];
		Nrxldt = new double[routes.getKeySet().size()][T];
		Nx0 = new double[T];
		Nx0dt = new double[T];
		Nxl = new double[T];
		Nxldt = new double[T];
		k = new double[T];
		S = new double[T];
		Sdt = new double[T];
		R = new double[T];
		Rdt = new double[T];
	}

	@Override
	public int getTimeIndex(double t) {
		for(int i = 0; i<timePoints.length;i++) {
			
			if(t>=timePoints[i])return i;
		}
		return 0;
	}


	@Override
	public Tuple<double[],double[][]> getSendingFlow(int timeIdx) {
		
			double t = timePoints[timeIdx]+delT-fd.L/fd.vf;// get the time index which flow is relevant
			int tl = this.getTimeIndex(t);// get the time point which is just lower that t
			double N = 0;
			double s = 0;
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
				double[] ds = new double[this.variables.getKeySet().size()];
				double sdt = 0;
				if(s!=fd.qm*delT) {
					ds = MatrixUtils.createRealVector(dN).subtract(MatrixUtils.createRealVector(this.dNxl[timeIdx])).getData();
					sdt = Ndt-Nxldt[timeIdx];
				}else {
					sdt = fd.qm;
				}
				this.dS[timeIdx] = ds;
				this.Sdt[timeIdx] = sdt;
			}else {
				N = this.Nx0[tl]+(this.Nx0[tl]-this.Nx0[tl+1])*(t-timePoints[tl])/delT;//interpolate between the cumulative flow of tl and tu = tl+1 to gert the cumulative flow at t. 
				s = Math.min(N-this.Nxl[timeIdx], fd.qm*delT);
			}
			this.S[timeIdx] = s;
		
		return new Tuple<>(S,dS);// the sending flow is the min between this two term 
	}



	@Override
	public Tuple<double[],double[][]> getRecivingFlow(int timeIdx) {
		//for(int timeIdx = 0; timeIdx<this.T;timeIdx++) {
		double t = timePoints[timeIdx]+delT-fd.L/fd.wf;// get the time index which flow is relevant
		if(t>0) {
			int tl = this.getTimeIndex(t);// get the time point which is just lower that t
			double N = 0;
			double r = 0;
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
				double[] dr = new double[this.variables.getKeySet().size()];
				double rdt = 0;
				if(r != fd.qm*delT) {
					dr = MatrixUtils.createRealVector(dN).subtract(MatrixUtils.createRealVector(this.dNx0[timeIdx])).getData();
					rdt = Ndt-Nx0dt[timeIdx];
				}else {
					rdt = fd.qm;
				}
				this.dR[timeIdx] = dr;
				this.Rdt[timeIdx] = rdt;
			}else {
				N = this.Nxl[tl]+(this.Nxl[tl]-this.Nxl[tl+1])*(t-timePoints[tl])/delT;//interpolate between the cumulative flow of tl and tu = tl+1 to gert the cumulative flow at t. 
				r = Math.min(N+fd.kj*fd.L-this.Nx0[timeIdx], fd.qm*delT);
			}
			this.R[timeIdx] = r;
		}
		//}
		return new Tuple<>(R,dR);// the sending flow is the min between this two term 
	}



	@Override
	public void updateNx0(double flow, int timeIndx) {
		this.Nx0[timeIndx] = flow;
	}



	@Override
	public void updateNxl(double flow, int timeInd) {
		this.Nxl[timeInd] = flow;
		
	}



	@Override
	public void updateNrx0(double flow, int routeIndx, int timeIndx) {
		this.Nrx0[routeIndx][timeIndx] = flow;
	}



	@Override
	public void updateNrxl(double flow, int routeIndx, int timeInd) {
		this.Nrxl[routeIndx][timeInd] = flow;
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
	public MapToArray getRoutes() {
		return routes;
	}



	@Override
	public int getTimeindexNo() {
		return T;
	}


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
		qm = 1800;
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
		this(link.getLength(),link.getFreespeed(),link.getFlowCapacityPerSec(),0.15);
	}
	
}
