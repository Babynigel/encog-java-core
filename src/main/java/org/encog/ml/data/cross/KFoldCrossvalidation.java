package org.encog.ml.data.cross;

import java.util.ArrayList;
import java.util.List;

import org.encog.mathutil.randomize.generate.GenerateRandom;
import org.encog.mathutil.randomize.generate.MersenneTwisterGenerateRandom;
import org.encog.ml.data.versatile.MatrixMLDataSet;
import org.encog.util.EngineArray;

public class KFoldCrossvalidation {

	private final MatrixMLDataSet baseDataset;
	private final int k;
	private final List<DataFold> folds = new ArrayList<DataFold>();
	private GenerateRandom rnd = new MersenneTwisterGenerateRandom();

	public KFoldCrossvalidation(MatrixMLDataSet theBaseDataset, int theK) {
		this.baseDataset = theBaseDataset;
		this.k = theK;
	}

	/**
	 * @return the rnd
	 */
	public GenerateRandom getRnd() {
		return rnd;
	}

	/**
	 * @param rnd
	 *            the rnd to set
	 */
	public void setRnd(GenerateRandom rnd) {
		this.rnd = rnd;
	}

	/**
	 * @return the baseDataset
	 */
	public MatrixMLDataSet getBaseDataset() {
		return baseDataset;
	}

	/**
	 * @return the k
	 */
	public int getK() {
		return k;
	}

	/**
	 * @return the folds
	 */
	public List<DataFold> getFolds() {
		return folds;
	}

	private int[] buildFirstList(int length) {
		int[] result = new int[length];

		if( this.baseDataset==null) {
			for (int i = 0; i < length; i++) {
				result[i] = i;
			}
		} else {
			for (int i = 0; i < length; i++) {
				result[i] = this.baseDataset.getMask()[i];
			}
		}

		return result;
	}

	private void shuffleList(int[] list) {
		for (int i = list.length - 1; i > 0; i--) {
			int n = this.rnd.nextInt(i + 1);
			int t = list[i];
			list[i] = list[n];
			list[n] = t;
		}
	}
	
	private List<int[]> allocateFolds() {
		List<int[]> folds = new ArrayList<int[]>();
		int countPer = this.baseDataset.size() / this.k;
		int countFirst = this.baseDataset.size() - (countPer * (k - 1));
		
		folds.add(new int[countFirst]);
		for (int i = 1; i < this.k; i++) {
			folds.add(new int[countPer]);
		}
		
		return folds;
	}
	
	private void populateFolds(List<int[]> folds, int[] firstList) {
		int idx = 0;
		for( int[] fold: folds) {
			for(int i=0;i<fold.length;i++) {
				fold[i] = firstList[idx++];
			}
		}	
	}
	
	private void buildSets(List<int[]> foldContents) {
		this.folds.clear();
		
		for(int i=0;i<this.k;i++) {
			// first calculate the size
			int trainingSize = 0;
			int validationSize = 0;
			for(int j=0;j<foldContents.size();j++) {
				int foldSize = foldContents.get(i).length;
				if( j==i ) {
					validationSize+=foldSize;
				} else {
					trainingSize+=foldSize;
				}
			}
			// create the masks
			int[] trainingMask = new int[trainingSize];
			int[] validationMask = new int[validationSize];
			int trainingIndex = 0;
			for(int j=0;j<foldContents.size();j++) {
				int[] source = foldContents.get(j);
				if(j==i) {
					EngineArray.arrayCopy(source, 0, validationMask, 0, source.length);
				} else {
					EngineArray.arrayCopy(source, 0, trainingMask, trainingIndex, source.length);
					trainingIndex+=source.length;
				}
			}
			// Build the set
			MatrixMLDataSet training = new MatrixMLDataSet(this.baseDataset,trainingMask);
			MatrixMLDataSet validation = new MatrixMLDataSet(this.baseDataset,validationMask);
			this.folds.add(new DataFold(training,validation));
		}
	}

	public void process(boolean shuffle) {
		int[] firstList = buildFirstList(this.baseDataset.size());
		
		if( shuffle ) {
			shuffleList(firstList);
		}
		
		List<int[]> foldContents = allocateFolds();
		populateFolds(foldContents,firstList);
		buildSets(foldContents);
	}

}
