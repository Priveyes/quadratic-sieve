package com.gazman.factor.matrix;

import com.gazman.factor.BigPrimesList;
import com.gazman.factor.VectorData;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by Ilya Gazman on 2/15/2016.
 */
public class VectorsShrinker {


    private BigInteger N;
    private BigInteger root;
    public int bigPrimesIndex;

    public void init(BigInteger root, int biggestPrimeIndex, BigInteger N) {
        this.root = root;
        this.bigPrimesIndex = biggestPrimeIndex;
        this.N = N;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<VectorData> shrink(ArrayList<VectorData> bSmoothVectors, BigPrimesList bigPrimesList) {
        bSmoothVectors = (ArrayList<VectorData>) bSmoothVectors.clone();
        ArrayList<LinkedList<VectorData>> bigPrimes = bigPrimesList.getBigPrimes();


        for (int i = bigPrimes.size() - 1; i >= 0; i--) {
            LinkedList<VectorData> vectorDatas = bigPrimes.get(i);
            if (vectorDatas.size() == 2) {
                bigPrimes.remove(i);
                VectorData vectorData = vectorDatas.get(0);
                merge(vectorData, vectorDatas.get(1));
                bSmoothVectors.add(vectorData);
            }
        }


        for (LinkedList<VectorData> bigPrimeList : bigPrimes) {
            boolean updateIndex = false;
            boolean firstVector = true;
            int bigPrimeIndex = -1;

            for (VectorData vectorData : bigPrimeList) {
                if (firstVector && vectorData.bigPrimeIndex == -1) {
                    firstVector = false;
                    updateIndex = true;
                }
                if (vectorData.bigPrimeIndex == -1) {
                    if (bigPrimeIndex == -1) {
                        bigPrimeIndex = this.bigPrimesIndex;
                    }
                    vectorData.bigPrimeIndex = bigPrimeIndex;
                    vectorData.vector.set(bigPrimeIndex);
                } else {
                    bigPrimeIndex = vectorData.bigPrimeIndex;
                }
                bSmoothVectors.add(vectorData);
            }
            if (updateIndex) {
                this.bigPrimesIndex++;
            }
        }

//        Collections.sort(bSmoothVectors, new Comparator<VectorData>() {
//            @Override
//            public int compare(VectorData vectorData1, VectorData vectorData2) {
//                return Long.compare(vectorData1.biggestPrime, vectorData2.biggestPrime);
//            }
//        });
//
//        int linkSize = 0;
//        int linkId = -1;
//
//        for (int i = bSmoothVectors.size() - 2; i > 0; i--) {
//            VectorData vectorDataPrevious = bSmoothVectors.get(i + 1);
//            VectorData vectorDataCurrent = bSmoothVectors.get(i);
//            VectorData vectorDataNext = bSmoothVectors.get(i - 1);
//
//            if(linkId == vectorDataCurrent.bigPrimeIndex){
//                linkSize++;
//            }
//            else{
//                linkSize = 0;
//            }
//            linkId = vectorDataCurrent.bigPrimeIndex;
//            if(vectorDataCurrent.bigPrimeIndex != vectorDataNext.bigPrimeIndex){
//                switch (linkSize){
//                    case 1:
//                        bSmoothVectors.remove(i);
//                        merge(vectorDataPrevious, vectorDataCurrent);
//                        vectorDataPrevious = null;
//                        break;
//                }
//            }
//        }

        return bSmoothVectors;
    }

    private void merge(VectorData result, VectorData vectorToMerge) {
        result.vector.xor(vectorToMerge.vector);
        BigInteger x1 = calculateX(result.position);
        BigInteger x2 = calculateX(vectorToMerge.position);
        result.x = x1.multiply(x2);
        result.y = calculateY(x1).multiply(calculateY(x2));
    }

    private BigInteger calculateY(BigInteger x) {
        return x.pow(2).subtract(N);
    }

    private BigInteger calculateX(long position) {
        return root.add(BigInteger.valueOf(position));
    }


}
