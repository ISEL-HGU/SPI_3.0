package LCE;

/**
 * The LCS class provides methods for calculating the Longest Common Subsequence (LCS)
 * and scoring similarity between two integer arrays.
 * 
 * https://www.geeksforgeeks.org/longest-common-subsequence-dp-4/
 */
public class LCS {

    /**
     * Calculates the Longest Common Subsequence (LCS) of two integer arrays.
     * 
     * @param poolVector vector from the given pool
     * @param givenBugVector vector from Change Collector
     * @return An array representing the Longest Common Subsequence
     */
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

    /**
     * Reverses the given dynamic programming (DP) table to obtain a subsequence.
     *
     * The method takes a DP table as input and reverses it to construct a subsequence.
     * It returns the reversed subsequence as a new array.
     *
     * @param dpTable The dynamic programming table obtained from a Longest Common Subsequence (LCS) calculation.
     * @return A subsequence obtained by reversing the input DP table.
     */
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

    /**
     * Calculates the similarity score between two integer arrays using a heuristic approach.
     *
     * The method calculates the similarity score by considering the Longest Common Subsequence (LCS)
     * between the given 'poolVector' and 'givenBugVector'. It uses a heuristic approach to give weight
     * according to the incorrect vectors in the 'poolVector'.
     *
     * @param poolVector The vector representing a pool of items.
     * @param givenBugVector The vector representing a specific set of items (e.g., a bug vector).
     * @return A similarity score between 0 and 1, where 1 indicates high similarity.
     */
    public float ScoreSimilarity(int[] poolVector, int[] givenBugVector) {

        float score = 1; 
        int sum = 0;

        // Obtain the DP table by finding the Longest Common Subsequence
        int[] dpTable = Backtrack(LongestCommonSubsequenceofIntegerArray(poolVector, givenBugVector));

        // Array to store the count of incorrect vectors in the pool
        int[] sigma = new int[ dpTable.length + 1];
        
        // Variable to keep track of the current position in the pool vector
        int trigger = 0;

         // Calculate the count of incorrect vectors in the pool for each element in the DP table
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

         // Calculate the total count of incorrect vectors in the pool
        for (int i = 1; i < sigma.length - 1; i++) {
            sum += sigma[i];
        }

         //heuristic to give weight according to the incorrect vectors
        if (poolVector.length !=  dpTable.length)
            score = 1 - (float) sum / (poolVector.length -  dpTable.length);

        // Adjust the score based on the length of the DP table
        score = score * ((float)  dpTable.length / givenBugVector.length);

        return score;
    }
}
