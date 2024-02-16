package LCE;


public class LCS {

    public int[] LongestCommonSubsequenceofIntegerArray(int[] poolVector, int[] givenBugVector) {
        // 1. get length of the subsequence with general bottom-up LCS algorithm
        int i = 0;
        int[][] dpTable = new int[poolVector.length + 1][givenBugVector.length + 1];
        for (i = 1; i <= poolVector.length; i++) {
            for (int j = 1; j <= givenBugVector.length; j++) {
                if (poolVector[i - 1] == givenBugVector[j - 1]) {
                    dpTable[i][j] = dpTable[i - 1][j - 1] + 1;
                } else {
                    dpTable[i][j] = Math.max(dpTable[i - 1][j], dpTable[i][j - 1]);
                }
            }
        }
        // 2. get subseqeunce of two integer array
        int x = poolVector.length;
        int y = givenBugVector.length;
        int[] subSequence = new int[dpTable[poolVector.length][givenBugVector.length]]; 
        int index = 0;
        while (x != 0 && y != 0) {
            if (poolVector[x - 1] == givenBugVector[y - 1]) {
                subSequence[index] = poolVector[x - 1];
                x--;
                y--;
                index++;
            } else if (dpTable[x][y] == dpTable[x - 1][y]) {
                x--;
            } else {
                y--;
            }
        }
        return subSequence;
    }


    public int[] Backtrack(int[] dpTable) {
        // reverse the dp array
        int[] subSequence = new int[dpTable.length];
        int index = 0;
        for (int i = dpTable.length - 1; i >= 0; i--) {
            subSequence[index] = dpTable[i];
            index++;
        }
        return subSequence;
    }

    public float ScoreSimilarity(int[] poolVector, int[] givenBugVector) {

        float score = 1; 
        int sum = 0;

        int[] dpTable = Backtrack(LongestCommonSubsequenceofIntegerArray(poolVector, givenBugVector));

        int[] sigma = new int[ dpTable.length + 1];
        
        int trigger = 0;

        for (int i = 0; i < dpTable.length; i++) {
            sigma[i] = 0;
            for (int j = trigger; j < poolVector.length; j++) {
                if (poolVector[j] != dpTable[i]) {
                    sigma[i]++;
                } else {
                    trigger = j + 1;
                    break;
                }
            }
            if (i + 1 == dpTable.length) {
                sigma[i + 1] = poolVector.length - trigger;
            }
        }
        for (int i = 1; i < sigma.length - 1; i++) {
            sum += sigma[i];
        }

        if (poolVector.length !=  dpTable.length)
            score = 1 - (float) sum / (poolVector.length -  dpTable.length);

        score = score * ((float)  dpTable.length / givenBugVector.length);

        return score;
    }
}
