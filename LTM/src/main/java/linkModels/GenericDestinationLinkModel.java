package linkModels;

import org.matsim.api.core.v01.network.Link;

public class GenericDestinationLinkModel implements LinkModel{

	
	private double[] timePoints;
	private double[][] Nrx0;
	private double[] Nx0;
	private int[]rIndex;
	private int T;
	private double delT;
	

//	private double[][][] dNrx0;
//	private double[][][] dNrxl;
//	private double[][] dNx0;
//	private double[][] dNxl;
//	private double[][] dk;
	
	
	
	@Override
	public void setLTMTimeBeanAndRouteSet(double[]timePoints, int[]routeIndex) {
		this.timePoints = timePoints;
		this.T = timePoints.length;
		this.delT = timePoints[1]-timePoints[0];
		this.rIndex = routeIndex;
		Nrx0 = new double[rIndex.length][T];
		Nx0 = new double[T];
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
		return 0;
	}
	


	@Override
	public double getRecivingFlow(int timeIdx) {
		return Double.POSITIVE_INFINITY;
	}



	@Override
	public void updateNx0(double flow, int timeIndx) {
		//this.Nx0[timeIndx] = flow;
	}



	@Override
	public void updateNxl(double flow, int timeInd) {
		//this.Nxl[timeInd] = flow;
		
	}



	@Override
	public void updateNrx0(double flow, int routeIndx, int timeIndx) {
		this.Nrx0[routeIndx][timeIndx] = flow;
	}



	@Override
	public void updateNrxl(double flow, int routeIndx, int timeInd) {
//		this.Nrxl[routeIndx][timeInd] = flow;
	}
	
	//_____________________________Getter Setter_________________________________________________
	
	@Override
	public Link getLink() {
		return null;
	}




	@Override
	public double[] getTimePoints() {
		return timePoints;
	}



	@Override
	public double[][] getNrx0() {
		return this.Nrx0;
	}

	
	@Override
	public double[][] getNrxl() {
		return null;
	}


	@Override
	public double[] getNx0() {
		return Nx0;
	}



	@Override
	public double[] getNxl() {
		return null;
	}


	@Override
	public double[] getK() {
		return null;
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
