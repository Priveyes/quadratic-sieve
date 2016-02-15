package com.gazman.factor;

import com.gazman.factor.matrix.BitMatrix;
import com.gazman.factor.wheels.Wheel;
import com.gazman.math.MathUtils;
import com.gazman.math.SqrRoot;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedList;

/**
 * Created by Ilya Gazman on 1/27/2016.
 */
public class QuadraticThieve extends Logger {
    private static final int B_SMOOTH = 10000;
    private static final double MINIMUM_LOG = 0.0000001;
    private double minimumBigPrimeLog;
    private int sieveVectorBound;
    private BigInteger primeBase[];
    private ArrayList<VectorData> bSmoothVectors = new ArrayList<>();
    private BigInteger N;
    private Wheel wheels[] = new Wheel[B_SMOOTH];
    private BigInteger root;
    private double baseLog;
    private int bSmoothFound;
    private BigPrimesList bigPrimesList = new BigPrimesList();
    private int bigPrimesIndex;

    public void factor(BigInteger input) {
        log("Factoring started");
        N = input;
        root = SqrRoot.bigIntSqRootCeil(input);

        log("Building Prime Base");
        buildPrimeBase();
        bigPrimesIndex = primeBase.length;
        BigInteger highestPrime = primeBase[primeBase.length - 1];
        sieveVectorBound = highestPrime.intValue();
        minimumBigPrimeLog = Math.log(highestPrime.pow(2).doubleValue());
        log("Biggest prime is", highestPrime);
        log();

        log("Building wheels");
        initSieveWheels();

        log("Start searching");
        long position = 0;
        long step = sieveVectorBound;


//        minimumBigPrimeLog = MINIMUM_LOG;

        while (true) {
            baseLog = calculateBaseLog(position);
            position += step;
            boolean sieve = sieve(position);
            log("Processed", position, "B-Smooth found", bSmoothFound, "Big primes found", bigPrimesList.getPrimesFound());
            if (sieve && tryToSolve()) {
                break;
            }
        }
    }

    private double calculateBaseLog(long position) {
        double target = root.add(BigInteger.valueOf(position)).pow(2).subtract(N).doubleValue();
        return Math.log(target);
    }

    private void initSieveWheels() {
        for (int i = 0; i < wheels.length; i++) {
            wheels[i] = new Wheel();
            wheels[i].init(primeBase[i], N, root);
        }
    }

    private boolean sieve(long destination) {
        boolean vectorsFound = false;
        Double[] logs = new Double[sieveVectorBound];
        Double[] trueLogs = new Double[sieveVectorBound];
        VectorData[] vectors = new VectorData[sieveVectorBound];

        for (int i = 0; i < primeBase.length; i++) {
            Wheel wheel = wheels[i];
            wheel.savePosition();
            while (wheel.testMove(destination)) {
                double log = wheel.nextLog();
                long position = wheel.move();
                int index = (int) (position % sieveVectorBound);
                if (logs[index] == null) {
                    logs[index] = 0d;
                }
                logs[index] += log;
            }
            wheel.restorePosition();
        }

        for (int i = primeBase.length - 1; i >= 0; i--) {
            Wheel wheel = wheels[i];
            while (wheel.testMove(destination)) {
                wheel.nextLog();
                long position = wheel.move();
                int index = (int) (position % sieveVectorBound);
                if (logs[index] == null) {
                    continue;
                }
                if (trueLogs[index] == null) {
                    if (baseLog - logs[index] > minimumBigPrimeLog) {
                        continue;
                    }
                    double trueLog = calculateBaseLog(position);
                    trueLogs[index] = trueLog;
                }

                double reminderLog = trueLogs[index] - logs[index];
                if (reminderLog > minimumBigPrimeLog) {
                    continue;
                }

                boolean bigPrime = reminderLog > MINIMUM_LOG;

                if (vectors[index] == null) {
                    vectors[index] = new VectorData(new BitSet(i), index + destination - sieveVectorBound);
                    if (bigPrime) {
                        VectorData vector = vectors[index];
                        bigPrimesList.add(reminderLog, vector);
                    } else {
                        bSmoothVectors.add(vectors[index]);
                        bSmoothFound++;
                    }
                    vectorsFound = true;
                }
                if (wheel.getPowers() % 2 != 0) {
                    vectors[index].vector.set(i);
                }
            }
        }

        return vectorsFound;
    }

    private boolean tryToSolve() {
        if (bSmoothVectors.size() + bigPrimesList.getPrimesFound() < B_SMOOTH) {
            return false;
        }

        log("Building matrix");

        @SuppressWarnings("unchecked")
        ArrayList<VectorData> bSmoothVectors = (ArrayList<VectorData>) this.bSmoothVectors.clone();
        LinkedList<LinkedList<VectorData>> bigPrimes = bigPrimesList.getBigPrimes();
        for (LinkedList<VectorData> bigPrimeList : bigPrimes) {
            boolean updateIndex = false;
            boolean firstVector = true;
            int bigPrimeIndex = -1;
            for (VectorData vectorData : bigPrimeList) {
                if(firstVector && vectorData.bigPrimeIndex == -1){
                    firstVector = false;
                    updateIndex = true;
                }
                if(vectorData.bigPrimeIndex == -1){
                    if(bigPrimeIndex == -1) {
                        bigPrimeIndex = this.bigPrimesIndex;
                    }
                    vectorData.bigPrimeIndex = bigPrimeIndex;
                    vectorData.vector.set(bigPrimeIndex);
                }
                else{
                    bigPrimeIndex = vectorData.bigPrimeIndex;
                }
                bSmoothVectors.add(vectorData);
            }
            if(updateIndex){
                this.bigPrimesIndex++;
            }
        }

        BitMatrix bitMatrix = new BitMatrix();
        ArrayList<ArrayList<VectorData>> solutions = bitMatrix.solve(bSmoothVectors);

        for (int i = 0; i < solutions.size(); i++) {
            ArrayList<VectorData> solution = solutions.get(i);
            log("Testing solution", (i + 1) + "/" + solutions.size());
            if (testSolution(solution)) {
                return true;
            }
        }
        log("no luck");

        return true;
    }

    private boolean testSolution(ArrayList<VectorData> solutionVector) {
        BigInteger y = one;
        BigInteger x = one;

        for (VectorData vectorData : solutionVector) {
            BigInteger savedX = root.add(BigInteger.valueOf(vectorData.position));
            BigInteger savedY = savedX.pow(2).subtract(N);
            x = x.multiply(savedX);
            y = y.multiply(savedY);
        }

        y = SqrRoot.bigIntSqRootFloor(y);
        BigInteger gcd = N.gcd(x.add(y));
        if (!gcd.equals(one) && !gcd.equals(N)) {
            log("Solved");
            log(gcd);

            return true;
        }

        return false;
    }

    private void buildPrimeBase() {
        primeBase = new BigInteger[B_SMOOTH];
        BigInteger prime = BigInteger.ONE;

        for (int i = 0; i < B_SMOOTH; ) {
            prime = prime.nextProbablePrime();
            if (MathUtils.isRootInQuadraticResidues(N, prime)) {
                primeBase[i] = prime;
                i++;
            }
        }
    }
}
