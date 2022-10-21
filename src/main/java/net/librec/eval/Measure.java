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
package net.librec.eval;

import net.librec.eval.fairness.*;
import net.librec.eval.ranking.*;
import net.librec.eval.rating.MAEEvaluator;
import net.librec.eval.rating.MPEEvaluator;
import net.librec.eval.rating.MSEEvaluator;
import net.librec.eval.rating.RMSEEvaluator;

import java.util.ArrayList;
import java.util.List;

/**
 * Measure
 *
 * @author WangYuFeng
 */
public enum Measure {

    AUC(AUCEvaluator.class),
    AP(AveragePrecisionEvaluator.class),
    IDCG(IdealDCGEvaluator.class),
    NDCG(NormalizedDCGEvaluator.class),
    PRECISION(PrecisionEvaluator.class),
    RECALL(RecallEvaluator.class),
    RR(ReciprocalRankEvaluator.class),
    Novelty(NoveltyEvaluator.class),
    Entropy(EntropyEvaluator.class),
    GiniIndex(GiniIndexEvaluator.class),
    Diversity(DiversityEvaluator.class),
    FeatureDiversity(DiversityByFeaturesEvaluator.class),
    ICOV(ItemCoverageEvaluator.class),
    RMSE(RMSEEvaluator.class),
    MSE(MSEEvaluator.class),
    MAE(MAEEvaluator.class),
    MPE(MPEEvaluator.class),
    PSP(PStatisticalParityEvaluator.class),
    CSP(CStatisticalParityEvaluator.class),
    PPR(PPercentRuleEvaluator.class),
    MISCALIB(MiscalibrationEvaluator.class),
    DPCF(DiscountedProportionalCFairnessEvaluator.class),
    DPPF(DiscountedProportionalPFairnessEvaluator.class),
    NONPAR(NonParityUnfairnessEvaluator.class),
    VALUNFAIRNESS(ValueUnfairnessEvaluator.class),
    ABSUNFAIRNESS(ValueUnfairnessEvaluator.class),
    OVERESTIMATE(OverestimationUnfairnessEvaluator.class),
    UNDERESTIMATE(UnderestimationUnfairnessEvaluator.class)
    ;

    private Class<? extends RecommenderEvaluator> evaluatorClass;

    Measure(Class<? extends RecommenderEvaluator> evaluatorClass) {
        this.evaluatorClass = evaluatorClass;
    }

    public static List<MeasureValue> getMeasureEnumList(boolean isRanking, int topN) {
        if (isRanking) {
            return getRankingEnumList(topN);
        } else {
            return getRatingEnumList();
        }
    }

    /**
     * Get ranking default enum list.
     *
     * @return ranking default enum list
     */
    private static List<MeasureValue> getRankingEnumList(int topN) {
        List<MeasureValue> rankingEnumList = new ArrayList<>(10);

        if (topN <= 0) {
            rankingEnumList.add(new MeasureValue(PRECISION, 5));
            rankingEnumList.add(new MeasureValue(PRECISION, 10));
            rankingEnumList.add(new MeasureValue(RECALL, 5));
            rankingEnumList.add(new MeasureValue(RECALL, 10));
            rankingEnumList.add(new MeasureValue(AUC, 10));
            rankingEnumList.add(new MeasureValue(AP, 10));
            rankingEnumList.add(new MeasureValue(NDCG, 10));
            rankingEnumList.add(new MeasureValue(RR, 10));
            rankingEnumList.add(new MeasureValue(Diversity, 10));
            rankingEnumList.add(new MeasureValue(FeatureDiversity, 10));
            rankingEnumList.add(new MeasureValue(RR, 10));
            rankingEnumList.add(new MeasureValue(Novelty, 10));
            rankingEnumList.add(new MeasureValue(Entropy, 10));
            rankingEnumList.add(new MeasureValue(GiniIndex, 10));
            rankingEnumList.add(new MeasureValue(ICOV, 10));
            rankingEnumList.add(new MeasureValue(PSP, 10));
            rankingEnumList.add(new MeasureValue(CSP, 10));
            rankingEnumList.add(new MeasureValue(PPR, 10));
            rankingEnumList.add(new MeasureValue(MISCALIB, 10));
            rankingEnumList.add(new MeasureValue(DPCF, 10));
            rankingEnumList.add(new MeasureValue(DPPF, 10));
            rankingEnumList.add(new MeasureValue(NONPAR, 10));
        } else {
            rankingEnumList.add(new MeasureValue(PRECISION, topN));
            rankingEnumList.add(new MeasureValue(RECALL, topN));
            rankingEnumList.add(new MeasureValue(AUC, topN));
            rankingEnumList.add(new MeasureValue(AP, topN));
            rankingEnumList.add(new MeasureValue(NDCG, topN));
            rankingEnumList.add(new MeasureValue(RR, topN));
            rankingEnumList.add(new MeasureValue(Diversity, topN));
            rankingEnumList.add(new MeasureValue(FeatureDiversity, topN));
            rankingEnumList.add(new MeasureValue(Novelty, topN));
            rankingEnumList.add(new MeasureValue(Entropy, topN));
            rankingEnumList.add(new MeasureValue(GiniIndex, topN));
            rankingEnumList.add(new MeasureValue(ICOV, topN));
            rankingEnumList.add(new MeasureValue(PSP, topN));
            rankingEnumList.add(new MeasureValue(CSP, topN));
            rankingEnumList.add(new MeasureValue(PPR, topN));
            rankingEnumList.add(new MeasureValue(MISCALIB, topN));
            rankingEnumList.add(new MeasureValue(DPCF, topN));
            rankingEnumList.add(new MeasureValue(DPPF, topN));
            rankingEnumList.add(new MeasureValue(NONPAR, topN));
        }
        return rankingEnumList;
    }

    /**
     * Get rating default enum list.
     *
     * @return rating default enum list
     */
    private static List<MeasureValue> getRatingEnumList() {
        List<MeasureValue> ratingEnumList = new ArrayList<>(4);
        ratingEnumList.add(new MeasureValue(RMSE));
        ratingEnumList.add(new MeasureValue(MSE));
        ratingEnumList.add(new MeasureValue(MAE));
        ratingEnumList.add(new MeasureValue(MPE));
        // fairness metrics
        ratingEnumList.add(new MeasureValue(VALUNFAIRNESS));
        ratingEnumList.add(new MeasureValue(ABSUNFAIRNESS));
        ratingEnumList.add(new MeasureValue(OVERESTIMATE));
        ratingEnumList.add(new MeasureValue(UNDERESTIMATE));

        return ratingEnumList;
    }

    /**
     * Return the Class object of the evaluator.
     *
     * @return the Class object of the evaluator
     */
    public Class<? extends RecommenderEvaluator> getEvaluatorClass() {
        return evaluatorClass;
    }

    public static class MeasureValue {

        /** measure type of the value */
        private Measure measure;

        /** number of items in the recommended list */
        private Integer topN;

        /**
         * Construct with the measure type of the value.
         *
         * @param measure the measure type of the value
         */
        public MeasureValue(Measure measure) {
            this.measure = measure;
        }

        /**
         * Construct with the measure type of the value and
         * the number of items in the recommended list.
         *
         * @param measure the measure type of the value
         * @param topN    number of items in the recommended list
         */
        public MeasureValue(Measure measure, Integer topN) {
            this.measure = measure;
            this.topN = topN;
        }

        /**
         * Return the {@code Measure} object of the {@code MeasureValue} object
         *
         * @return the {@code Measure} object of the {@code MeasureValue} object
         */
        public Measure getMeasure() {
            return measure;
        }

        /**
         * Set the {@code Measure} object of the {@code MeasureValue} object
         *
         * @param measure the measure to set
         */
        public void setMeasure(Measure measure) {
            this.measure = measure;
        }

        /**
         * Return the number of items in the recommended list.
         *
         * @return the number of items in the recommended list
         */
        public Integer getTopN() {
            return topN;
        }

        /**
         * Set the number of items in the recommended list
         *
         * @param topN the number of items in the recommended list to set
         */
        public void setTopN(Integer topN) {
            this.topN = topN;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((measure == null) ? 0 : measure.hashCode());
            result = prime * result + ((topN == null) ? 0 : topN.hashCode());
            return result;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            MeasureValue other = (MeasureValue) obj;
            if (measure != other.measure)
                return false;
            if (topN == null) {
                if (other.topN != null)
                    return false;
            } else if (!topN.equals(other.topN))
                return false;
            return true;
        }

    }
}
