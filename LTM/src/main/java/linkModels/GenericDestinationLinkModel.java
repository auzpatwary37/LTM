package linkModels;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.Tuple;

import ust.hk.praisehk.metamodelcalibration.Utils.MapToArray;
@Deprecated
public class GenericDestinationLinkModel implements LinkModel{

	
	private double[] timePoints;
	private double[][] Nrx0;
	private double[] Nx0;
	private MapToArray routes;
	private int T;
	private double delT;
	

//	private double[][][] dNrx0;
//	private double[][][] dNrxl;
//	private double[][] dNx0;
//	private double[][] dNxl;
//	private double[][] dk;
	
	
	
	@Override
	public void setLTMTimeBeanAndRouteSet(double[]timePoints, MapToArray routes) {
		this.timePoints = timePoints;
		this.T = timePoints.length;
		this.delT = timePoints[1]-timePoints[0];
		this.routes = routes;
		Nrx0 = new double[routes.getKeySet().size()][T];
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
	public MapToArray getRoutes() {
		return routes;
	}



	@Override
	public int getTimeindexNo() {
		return T;
	}

	


	@Override
	public void setOptimizationVariables(MapToArray variables) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Tuple<double[], double[][]> getSendingFlow(int timeIndx) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Tuple<double[], double[][]> getRecivingFlow(int timeIdx) {
		// TODO Auto-generated method stub
		return null;
	}

}
