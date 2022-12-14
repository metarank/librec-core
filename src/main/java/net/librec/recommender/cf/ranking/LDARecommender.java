/**
 * Copyright (C) 2016 LibRec
 * <p>
 * This file is part of LibRec.
 * LibRec is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * LibRec is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
 */
package net.librec.recommender.cf.ranking;

import net.librec.annotation.ModelData;
import net.librec.common.LibrecException;
import net.librec.math.algorithm.Gamma;
import net.librec.math.algorithm.Randoms;
import net.librec.math.structure.DenseMatrix;
import net.librec.math.structure.MatrixEntry;
import net.librec.math.structure.VectorBasedDenseVector;
import net.librec.recommender.MatrixProbabilisticGraphicalRecommender;


/**
 * Latent Dirichlet Allocation for implicit feedback: Tom Griffiths, <strong>Gibbs sampling in the generative model of
 * Latent Dirichlet Allocation</strong>, 2002. <br>
 * <p>
 * <strong>Remarks:</strong> This implementation of LDA is for implicit feedback, where users are regarded as documents
 * and items as words. To directly apply LDA to explicit ratings, Ian Porteous et al. (AAAI 2008, Section Bi-LDA)
 * mentioned that, one way is to treat items as documents and ratings as words. We did not provide such an LDA
 * implementation for explicit ratings. Instead, we provide recommender {@code URP} as an alternative LDA model for
 * explicit ratings.
 *
 * @author guoguibing and Keqiang Wang
 */
@ModelData({"isRanking", "lda", "userTopicProbs", "topicItemProbs", "trainMatrix"})
public class LDARecommender extends MatrixProbabilisticGraphicalRecommender {

    /**
     * Dirichlet hyper-parameters of user-topic distribution: typical value is 50/K
     */
    protected float initAlpha;

    /**
     * Dirichlet hyper-parameters of topic-item distribution, typical value is 0.01
     */
    protected float initBeta;
    /**
     * entry[k, i]: number of tokens assigned to topic k, given item i.
     */
    protected DenseMatrix topicItemNumbers;

    /**
     * entry[u, k]: number of tokens assigned to topic k, given user u.
     */
    protected DenseMatrix userTopicNumbers;

    /**
     * topic assignment as list from the iterator of trainMatrix
     */
    protected int[] topicAssignments;

    /**
     * entry[u]: number of tokens rated by user u.
     */
    protected VectorBasedDenseVector userTokenNumbers;

    /**
     * entry[k]: number of tokens assigned to topic t.
     */
    protected VectorBasedDenseVector topicTokenNumbers;

    /**
     * number of topics
     */
    protected int numTopics;

    /**
     * vector of hyperparameters for alpha and beta
     */
    protected VectorBasedDenseVector alpha, beta;

    /**
     * cumulative statistics of theta, phi
     */
    protected DenseMatrix userTopicProbsSum, topicItemProbsSum;

    /**
     * posterior probabilities of parameters
     */
    protected DenseMatrix userTopicProbs, topicItemProbs;

    /**
     * size of statistics
     */
    protected int numStats = 0;

    /**
     * setup
     * init member method
     *
     * @throws LibrecException if error occurs
     */
    protected void setup() throws LibrecException {
        super.setup();
        numTopics = conf.getInt("rec.topic.number", 10);

        userTopicProbsSum = new DenseMatrix(numUsers, numTopics);
        topicItemProbsSum = new DenseMatrix(numTopics, numItems);

        // initialize count variables.
        userTopicNumbers = new DenseMatrix(numUsers, numTopics);
        userTokenNumbers = new VectorBasedDenseVector(numUsers);

        topicItemNumbers = new DenseMatrix(numTopics, numItems);
        topicTokenNumbers = new VectorBasedDenseVector(numTopics);

        // default value:
        // homas L Griffiths and Mark Steyvers. Finding scientific topics.
        // Proceedings of the National Academy of Sciences, 101(suppl 1):5228???5235, 2004.
        initAlpha = conf.getFloat("rec.user.dirichlet.prior", 50.0f / numTopics);
        initBeta = conf.getFloat("rec.topic.dirichlet.prior", 0.01f);

        alpha = new VectorBasedDenseVector(numTopics);
        alpha.assign((index, value) -> initAlpha);

        beta = new VectorBasedDenseVector(numItems);
        beta.assign((index, value) -> initBeta);

        // The z_u,i are initialized to values in [0, K-1] to determine the initial state of the Markov chain.
        topicAssignments = new int[trainMatrix.size()];
        int topicAssignmentsIndex = 0;
        for (MatrixEntry matrixEntry : trainMatrix) {
            int userIdx = matrixEntry.row();
            int itemIdx = matrixEntry.column();
            int num = (int) (matrixEntry.get());
            for (int numIdx = 0; numIdx < num; numIdx++) {
                int topicIdx = Randoms.uniform(numTopics); // 0 ~ k-1

                // assign a topic t to pair (u, i)
                topicAssignments[topicAssignmentsIndex++] = topicIdx;

                // number of items of user u assigned to topic t.
                userTopicNumbers.plus(userIdx, topicIdx, 1);
                // total number of items of user u
                userTokenNumbers.plus(userIdx, 1);
                // number of instances of item i assigned to topic t
                topicItemNumbers.plus(topicIdx, itemIdx, 1);
                // total number of words assigned to topic t.
                topicTokenNumbers.plus(topicIdx, 1);
            }
        }
    }

    @Override
    protected void eStep() {
        double sumAlpha = alpha.sum();
        double sumBeta = beta.sum();

        // Gibbs sampling from full conditional distribution
        int topicAssignmentsIdx = 0;
        for (MatrixEntry matrixEntry : trainMatrix) {
            int userIdx = matrixEntry.row();
            int itemIdx = matrixEntry.column();

            int num = (int) (matrixEntry.get());
            for (int numIdx = 0; numIdx < num; numIdx++) {
                int topicIdx = topicAssignments[topicAssignmentsIdx]; // topic

                userTopicNumbers.plus(userIdx, topicIdx, -1);
                userTokenNumbers.plus(userIdx, -1);
                topicItemNumbers.plus(topicIdx, itemIdx, -1);
                topicTokenNumbers.plus(topicIdx, -1);

                // do multinomial sampling via cumulative method:
                double[] p = new double[numTopics];
                for (topicIdx = 0; topicIdx < numTopics; topicIdx++) {
                    p[topicIdx] = (userTopicNumbers.get(userIdx, topicIdx) + alpha.get(topicIdx)) / (userTokenNumbers.get(userIdx)
                            + sumAlpha) * (topicItemNumbers.get(topicIdx, itemIdx) + beta.get(itemIdx))
                            / (topicTokenNumbers.get(topicIdx) + sumBeta);
                }
                // cumulating multinomial parameters
                for (topicIdx = 1; topicIdx < p.length; topicIdx++) {
                    p[topicIdx] += p[topicIdx - 1];
                }
                // scaled sample because of unnormalized p[], randomly sampled a new topic t
                double rand = Randoms.uniform() * p[numTopics - 1];
                for (topicIdx = 0; topicIdx < p.length; topicIdx++) {
                    if (rand < p[topicIdx])
                        break;
                }

                // plus newly estimated z_i to count variables
                userTopicNumbers.plus(userIdx, topicIdx, 1);
                userTokenNumbers.plus(userIdx, 1);
                topicItemNumbers.plus(topicIdx, itemIdx, 1);
                topicTokenNumbers.plus(topicIdx, 1);

                topicAssignments[topicAssignmentsIdx] = topicIdx;
                topicAssignmentsIdx++;
            }
        }
    }

    @Override
    protected void mStep() {
        double sumAlpha = alpha.sum();
        double sumBeta = beta.sum();
        double digammaTopicAlpha, digammaItemBeta, topicAlpha, itemBeta;
        double digammaAlphaSum = Gamma.digamma(sumAlpha);

        double denominator = 0.0d;
        for (int userIdx = 0; userIdx < numUsers; userIdx++) {
            denominator += Gamma.digamma(userTokenNumbers.get(userIdx) + sumAlpha) - digammaAlphaSum;
        }

        // update alpha vector
        for (int topicIdx = 0; topicIdx < numTopics; topicIdx++) {
            topicAlpha = alpha.get(topicIdx);
            digammaTopicAlpha = Gamma.digamma(topicAlpha);
            double numerator = 0.0;
            for (int userIdx = 0; userIdx < numUsers; userIdx++) {
                numerator += Gamma.digamma(userTopicNumbers.get(userIdx, topicIdx) + topicAlpha) - digammaTopicAlpha;
            }
            if (numerator != 0)
                alpha.set(topicIdx, topicAlpha * (numerator / denominator));
        }

        // update beta_k
        denominator = 0.0d;
        double digammaBetaSum = Gamma.digamma(sumBeta);
        for (int topicIdx = 0; topicIdx < numTopics; topicIdx++) {
            denominator += Gamma.digamma(topicTokenNumbers.get(topicIdx) + sumBeta) - digammaBetaSum;
        }

        for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
            itemBeta = beta.get(itemIdx);
            digammaItemBeta = Gamma.digamma(itemBeta);
            double numerator = 0;
            for (int topicIdx = 0; topicIdx < numTopics; topicIdx++) {
                numerator += Gamma.digamma(topicItemNumbers.get(topicIdx, itemIdx) + itemBeta) - digammaItemBeta;
            }
            if (numerator != 0)
                beta.set(itemIdx, itemBeta * (numerator / denominator));
        }
    }

    /**
     * Add to the statistics the values of theta and phi for the current state.
     */
    protected void readoutParams() {
        double sumAlpha = alpha.sum();
        double sumBeta = beta.sum();

        double val;
        for (int userIdx = 0; userIdx < numUsers; userIdx++) {
            for (int factorIdx = 0; factorIdx < numTopics; factorIdx++) {
                val = (userTopicNumbers.get(userIdx, factorIdx) + alpha.get(factorIdx)) / (userTokenNumbers.get(userIdx) + sumAlpha);
                userTopicProbsSum.plus(userIdx, factorIdx, val);
            }
        }

        for (int factorIdx = 0; factorIdx < numTopics; factorIdx++) {
            for (int itemIdx = 0; itemIdx < numItems; itemIdx++) {
                val = (topicItemNumbers.get(factorIdx, itemIdx) + beta.get(itemIdx)) / (topicTokenNumbers.get(factorIdx) + sumBeta);
                topicItemProbsSum.plus(factorIdx, itemIdx, val);
            }
        }
        numStats++;
    }

    @Override
    protected void estimateParams() {
        userTopicProbs = userTopicProbsSum.times(1.0 / numStats);
        topicItemProbs = topicItemProbsSum.times(1.0 / numStats);
    }


    @Override
    protected double predict(int userIdx, int itemIdx) throws LibrecException {
        return userTopicProbs.row(userIdx).dot(topicItemProbs.column(itemIdx));
    }
}
