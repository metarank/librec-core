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
package net.librec.eval.fairness;

import com.google.common.collect.BiMap;
import net.librec.eval.AbstractRecommenderEvaluator;
import net.librec.math.algorithm.Maths;
import net.librec.math.structure.SequentialAccessSparseMatrix;
import net.librec.recommender.item.KeyValue;
import net.librec.recommender.item.RecommendedList;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Discounted Proportional Fairness based on NormalizedDCGEvaluator @topN
 *
 * Kelly, F. P., Maulloo, A. K., & Tan, D. K. (1998).
 * Rate control for communication networks: shadow prices, proportional fairness and stability.
 * Journal of the Operational Research society, 49(3), 237-252.
 *
 * @author Nasim Sonboli extending the the implementation of WangYuFeng on NDCG
 *
 * consumer-side fairness
 */


public class DiscountedProportionalCFairnessEvaluator extends AbstractRecommenderEvaluator {

    /**
     * item feature matrix - indicating an item is associated to certain feature or not
     */
    protected SequentialAccessSparseMatrix userFeatureMatrix;

    /**
     * Evaluate on the test set with the the list of recommended items.
     *
     * @param groundTruthList
     *            the given test set
     * @param recommendedList
     *            the list of recommended items
     * @return evaluate result
     */

    public double evaluate(RecommendedList groundTruthList, RecommendedList recommendedList) {
        userFeatureMatrix = getDataModel().getFeatureAppender().getUserFeatures();
        BiMap<String, Integer> featureIdMapping = getDataModel().getFeatureAppender().getUserFeatureMap();
        double minUtility = 1 / Maths.log(this.topN + 1, 2);


        int numUsers = groundTruthList.size();
        int numFeatures = userFeatureMatrix.columnSize();
        int protectedId = 0;


        //protected users
        String protectedAttribute = "";
        if (conf != null && StringUtils.isNotBlank(conf.get("data.protected.feature"))) {
            protectedAttribute = conf.get("data.protected.feature");
            protectedId = featureIdMapping.get(protectedAttribute);
        } else {
            return 0;
        }

        double proDCG = 0.0;
        double unproDCG = 0.0;
        for (int userID = 0; userID < numUsers; userID++) {
            Set<Integer> testSetByUser = groundTruthList.getKeySetByContext(userID);

            if (testSetByUser.size() > 0) {

                double dcg = 0.0;
                List<KeyValue<Integer, Double>> recommendListByUser = recommendedList.getKeyValueListByContext(userID);

                // calculate DCG
                int topK = this.topN <= recommendListByUser.size() ? this.topN : recommendListByUser.size();
                for (int indexOfItem = 0; indexOfItem < topK; indexOfItem++) {
                    int itemID = recommendListByUser.get(indexOfItem).getKey();
                    if (!testSetByUser.contains(itemID)) {
                        continue;
                    }
                    int rank = indexOfItem + 1;
                    dcg += 1 / Maths.log(rank + 1, 2);
                }

                if (userFeatureMatrix.get(userID, protectedId) == 1) {
                    proDCG += dcg;
                } else {
                    unproDCG += dcg;
                }
            }
        }

        double sumDCG = 0.0;
        if (proDCG == 0.0) {
            proDCG = minUtility;
        }
        if (unproDCG == 0) {
            unproDCG = minUtility;
        }
        sumDCG = proDCG + unproDCG;


        double dpf = 0.0;
        dpf = Maths.log((proDCG/sumDCG),2) + Maths.log((unproDCG/sumDCG),2);
        return dpf;
    }
}

// log(1/2) + log(1/2) = -1 + -1 = -2
// log(1/4) + log(3/4) = -2 + log(3/4) = -2 + (negative)

// log(0) + log(1) = -Inf + 0 =

// log(1/k / SUM) + log((sum - 1/k)/sum) = worst possible non-infinite result

// if utility = 0, then let utility for 0 group = 1/(k+1).
