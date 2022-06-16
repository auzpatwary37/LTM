package linkModels;

import org.apache.commons.math.linear.MatrixUtils;
import org.apache.commons.math.linear.RealVector;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.collections.Tuple;

import ust.hk.praisehk.metamodelcalibration.Utils.MapToArray;
@Deprecated
public class GenericOriginLinkModel implements LinkModel{

		private double[] timePoints;
		private double[][] Nrxl;
		private double[] Nxl;
		private double[][]dNxl;
		private double[][][] dNrxl;
		private MapToArray routes;
		private MapToArray variables;
		private double[][] dS;
		private int T;
		private double delT;
		private double[] S;
		
		private double[][] routeDemand;
		private double[][][] routeDemandGrad;
		private double[] routeDemandTimePoints;//assuming route departure rate time ticks are separate from ltm time ticks

		@Override
		public void setOptimizationVariables(MapToArray variables) {
			this.variables = variables;
		
			this.dNxl = new double[this.T][this.variables.getKeySet().size()];
			
			this.dNrxl = new double[routes.getKeySet().size()][T][this.variables.getKeySet().size()];
			this.dS = new double[T][this.variables.getKeySet().size()];
			
		}
		
		@Override
		public void setLTMTimeBeanAndRouteSet(double[]timePoints, MapToArray routes) {
			this.timePoints = timePoints;
			this.T = timePoints.length;
			this.delT = timePoints[1]-timePoints[0];
			this.routes = routes;
			Nrxl = new double[routes.getKeySet().size()][T];
			Nxl = new double[T];
			S = new double[T];
		}

		@Override
		public int getTimeIndex(double t) {
			for(int i = 0; i<timePoints.length;i++) {
				
				if(t>=timePoints[i])return i;
			}
			return 0;
		}

		public void setRouteDemand(double[][] routeDemand, double[]routeTimePoints, double[][][] routeDemandGrad) {
			this.routeDemand = routeDemand;
			this.routeDemandTimePoints = routeTimePoints;  
			this.routeDemandGrad = routeDemandGrad;
		}

	
		
		@Override
		public Tuple<double[],double[][]> getSendingFlow() {
			for(int timeIdx = 0; timeIdx<T;timeIdx++) {
				double t = timePoints[timeIdx]+delT;// get the time index which flow is relevant
				double N = 0;
				RealVector dN = null;
				if(this.variables!=null)dN = MatrixUtils.createRealVector(new double[this.variables.getKeySet().size()]);
				for(int r = 0; r<routeDemand.length;r++) {
					for(int i = 0;i<this.routeDemandTimePoints.length;i++) {
						if(this.routeDemandTimePoints[i]<t) {
							N+=this.routeDemand[r][i];
							if(variables!=null) {
								dN = dN.add(this.routeDemandGrad[r][i]);
							}
						}
					}
				}
				double s = N-this.Nxl[timeIdx];
				S[timeIdx] = s;
				if(variables!=null) {
					double[]ds = dN.subtract(this.dNxl[timeIdx]).getData();
					this.dS[timeIdx] = ds;
				}
			}
			return null;// the sending flow is the cumulative route demand minus the cumulative outflow up to timeIndx 
		}
		
		public  GenericOriginLinkModel(double[][] routeDemand, double[]routeTimePoints, double[][][] routeDemandGradient) {
			this.setRouteDemand(routeDemand, routeTimePoints,routeDemandGradient);
		}


		@Override
		public Tuple<double[],double[][]> getRecivingFlow() {//this function should never be called for an origin link
			//throw new IllegalArgumentException("This function should never have been called for an origin or source link. Please Debug!!!");
			return null;
		}



		@Override
		public void updateNx0(double flow, int timeIndx) {
			//this.Nx0[timeIndx] = flow;
		}



		@Override
		public void updateNxl(double flow, int timeInd) {
			this.Nxl[timeInd] = flow;
			
		}



		@Override
		public void updateNrx0(double flow, int routeIndx, int timeIndx) {
			//this.Nrx0[routeIndx][timeIndx] = flow;
		}



		@Override
		public void updateNrxl(double flow, int routeIndx, int timeInd) {
			this.Nrxl[routeIndx][timeInd] = flow;
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
			return null;
		}

		
		@Override
		public double[][] getNrxl() {
			return Nrxl;
		}


		@Override
		public double[] getNx0() {
			return null;
		}



		@Override
		public double[] getNxl() {
			return Nxl;
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

