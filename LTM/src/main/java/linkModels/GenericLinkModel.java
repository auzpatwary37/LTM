package linkModels;

import org.matsim.api.core.v01.network.Link;


public class GenericLinkModel implements LinkModel{
	private final Link link;
	private double[] timePoints;
	private double[][] Nrx0;
	private double[][] Nrxl;
	private double[] Nx0;
	private double[] Nxl;
	private double[]k;
	private int[]rIndex;
	private int T;
	private FD fd;
	private double delT;
	
//	private double[][][] dNrx0;
//	private double[][][] dNrxl;
//	private double[][] dNx0;
//	private double[][] dNxl;
//	private double[][] dk;
	
	
	
	public GenericLinkModel (Link l) {
		this.link = l;
		this.fd = new FD(l);
	}
	
	
	
	@Override
	public void setLTMTimeBeanAndRouteSet(double[]timePoints, int[]routeIndex) {
		this.timePoints = timePoints;
		this.T = timePoints.length;
		this.delT = timePoints[1]-timePoints[0];
		this.rIndex = routeIndex;
		Nrx0 = new double[rIndex.length][T];
		Nrxl = new double[rIndex.length][T];
		Nx0 = new double[T];
		Nxl = new double[T];
		k = new double[T];
	}

	@Override
	public int getTimeIndex(double t) {
		for(int i = 0; i<timePoints.length;i++) {
			
			if(t>=timePoints[i])return i;
		}
		return 0;
	}


	@Override
	public double getSendingFlow(int timeIdx) {
		double t = timePoints[timeIdx]+delT-fd.L/fd.vf;// get the time index which flow is relevant
		int tl = this.getTimeIndex(t);// get the time point which is just lower that t
		double N = this.Nx0[tl]+(this.Nx0[tl]-this.Nx0[tl+1])*(t-timePoints[tl])/delT;//interpolate between the cumulative flow of tl and tu = tl+1 to gert the cumulative flow at t. 
		return Math.min(N-this.Nxl[timeIdx], fd.qm*delT);// the sending flow is the min between this two term 
	}



	@Override
	public double getRecivingFlow(int timeIdx) {
		double t = timePoints[timeIdx]+delT-fd.L/fd.wf;// get the time index which flow is relevant
		int tl = this.getTimeIndex(t);// get the time point which is just lower that t
		double N = this.Nxl[tl]+(this.Nxl[tl]-this.Nxl[tl+1])*(t-timePoints[tl])/delT;//interpolate between the cumulative flow of tl and tu = tl+1 to gert the cumulative flow at t. 
		return Math.min(N+fd.kj*fd.L-this.Nx0[timeIdx], fd.qm*delT);// the sending flow is the min between this two term 
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
	public int[] getrIndex() {
		return rIndex;
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
