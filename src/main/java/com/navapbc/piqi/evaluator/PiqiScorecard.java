package com.navapbc.piqi.evaluator;

import lombok.Data;
import org.piqialliace.model.DataClassScoreResult;

import java.util.HashMap;
import java.util.Map;

@Data
public class PiqiScorecard {

    private Long numberOfFilesProcessed = 0L;
    private Long numberOfFilesSuccess = 0L;
    private Long numberOfFilesFailed = 0L;
    private Long accumulatedPiqiScore = 0L;
    private Long overallPiqiScore = 0L;
    private Map<String, Long> categoryScores = new HashMap<>();

    public PiqiScorecard() {}

    public void addToAccumulatedScore(Long overallScore) {
        this.accumulatedPiqiScore = this.accumulatedPiqiScore + overallScore;
        if (numberOfFilesSuccess > 0) {
            this.overallPiqiScore = this.accumulatedPiqiScore / numberOfFilesSuccess;
        }
    }

    public void addToNumberOfFilesProcessed(Long numberOfFilesProcessed) {
        this.numberOfFilesProcessed = this.numberOfFilesProcessed + numberOfFilesProcessed;
    }

    public void addToSuccess(Long numberOfFilesSuccess) {
        this.numberOfFilesSuccess = this.numberOfFilesSuccess + numberOfFilesSuccess;
    }

    public void addToFailed(Long numberOfFilesFailed) {
        this.numberOfFilesFailed = this.numberOfFilesFailed + numberOfFilesFailed;
    }

    public void addToCategoryScore(DataClassScoreResult  dataClassScoreResult) {
        if (dataClassScoreResult != null && dataClassScoreResult.getDataClassName() != null) {
            categoryScores.putIfAbsent(dataClassScoreResult.getDataClassName(), 0L);
            if (dataClassScoreResult.getPiqiScore() != null) {
                Long currentScore = categoryScores.get(dataClassScoreResult.getDataClassName());
                currentScore = currentScore + dataClassScoreResult.getPiqiScore();
                categoryScores.put(dataClassScoreResult.getDataClassName(), currentScore);
            }
        }
    }



}
