package linkModels;

import org.matsim.api.core.v01.network.Link;

public class GenericOriginLinkModel implements LinkModel{

		private double[] timePoints;
		private double[][] Nrxl;
		private double[] Nxl;
		private int[]rIndex;
		private int T;
		private double delT;
		
		private double[][] routeDemand;
		private double[] routeDemandTimePoints;
//		private double[][][] dNrx0;
//		private double[][][] dNrxl;
//		private double[][] dNx0;
//		private double[][] dNxl;
//		private double[][] dk;
		
		
		
		@Override
		public void setLTMTimeBeanAndRouteSet(double[]timePoints, int[]routeIndex) {
			this.timePoints = timePoints;
			this.T = timePoints.length;
			this.delT = timePoints[1]-timePoints[0];
			this.rIndex = routeIndex;
			Nrxl = new double[rIndex.length][T];
			Nxl = new double[T];
		}

		@Override
		public int getTimeIndex(double t) {
			for(int i = 0; i<timePoints.length;i++) {
				
				if(t>=timePoints[i])return i;
			}
			return 0;
		}

		public void setRouteDemand(double[][] routeDemand, double[]routeTimePoints) {
			this.routeDemand = routeDemand;
			this.routeDemandTimePoints = routeTimePoints;  
		}

		@Override
		public double getSendingFlow(int timeIdx) {
			double t = timePoints[timeIdx]+delT;// get the time index which flow is relevant
			double N = 0;
			for(int r = 0; r<routeDemand.length;r++) {
				for(int i = 0;i<this.routeDemandTimePoints.length;i++) {
					if(this.routeDemandTimePoints[i]<t)N+=this.routeDemand[r][i];
				}
			}
			return N-this.Nxl[timeIdx];// the sending flow is the cumulative route demand minus the cumulative outflow up to timeIndx 
		}
		
		public  GenericOriginLinkModel() {
			
		}


		@Override
		public double getRecivingFlow(int timeIdx) {
			return 0;
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
		public int[] getrIndex() {
			return rIndex;
		}



		@Override
		public int getTimeindexNo() {
			return T;
		}


	}

