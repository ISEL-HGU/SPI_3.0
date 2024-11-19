package LCE;

/**
 * The LCS class provides methods for calculating the Longest Common Subsequence (LCS)
 * and scoring similarity between two integer arrays.
 * 
 * https://www.geeksforgeeks.org/longest-common-subsequence-dp-4/
 */
public class LCS {

    /**
     * Calculates the length of the Longest Common Subsequence (LCS) between two integer arrays.
     *
     * This method uses dynamic programming to build a table that represents the length
     * of the LCS at each position in the arrays.
     * 
     * Calcuate LCS score using givenBugVector to poolVector
     *
     * @param poolVector The first integer array.
     * @param givenBugVector The second integer array.
     * @return A 2D array representing the LCS length table.
     */
    private int[][] calculateLCSLength(int[] poolVector, int[] givenBugVector) {

        int[][] dpTable = new int[poolVector.length + 1][givenBugVector.length + 1];

        for (int i = 1; i <= poolVector.length; i++) {
            for (int j = 1; j <= givenBugVector.length; j++) {
                if (poolVector[i - 1] == givenBugVector[j - 1]) {
                    dpTable[i][j] = dpTable[i - 1][j - 1] + 1;
                } else {
                    dpTable[i][j] = Math.max(dpTable[i - 1][j], dpTable[i][j - 1]);
                }
            }
        }

        return dpTable;
    }

    /**
     * Retrieves the Longest Common Subsequence (LCS) between two integer arrays.
     *
     * This method uses the LCS length table obtained from the dynamic programming
     * approach to reconstruct the LCS as an array of integers.
     *
     * @param poolVector The first integer array.
     * @param givenBugVector The second integer array.
     * @param dpTable The LCS length table obtained from calculateLCSLength method.
     * @return The LCS as an array.
     */
    private int[] retrieveLCS(int[] poolVector, int[] givenBugVector, int[][] dpTable) {
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
     * Calculates the Longest Common Subsequence (LCS) between two integer arrays.
     *
     * @param poolVector The first integer array.
     * @param givenBugVector The second integer array.
     * @return The LCS as an array.
     */
    public int[] longestCommonSubsequenceOfIntegerArray(int[] poolVector, int[] givenBugVector) {
        int[][] dpTable = calculateLCSLength(poolVector, givenBugVector);
        return retrieveLCS(poolVector, givenBugVector, dpTable);
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
    public int[] backtrack(int[] dpTable) {
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
     * Calculates the count of incorrect vectors in the pool for each element in the DP table.
     *
     * @param poolVector The pool vector.
     * @param subSequence The dynamic programming table obtained from LCS.
     * @return An array representing the count of incorrect vectors for each element in the DP table.
     */
    private int[] calculateSigma(int[] poolVector, int[] subSequence) {
        
        // Array to store the count of incorrect vectors in the pool
        int[] sigma = new int[subSequence.length + 1];

        // Variable to keep track of the current position in the pool vector
        int trigger = 0;

        // Calculate the count of incorrect vectors in the pool for each element in the DP table
        for (int i = 0; i < subSequence.length; i++) {
            sigma[i] = 0;
            for (int j = trigger; j < poolVector.length; j++) {
                if (poolVector[j] != subSequence[i]) {
                    sigma[i]++;
                } else {
                    trigger = j + 1;
                    break;
                }
            }
            if (i + 1 == subSequence.length) {
                sigma[i + 1] = poolVector.length - trigger;
            }
        }

        return sigma;
    }

    /**
     * Calculates the total count of incorrect vectors in the pool.
     *
     * @param sigma An array representing the count of incorrect vectors for each element in the DP table.
     * @return The total count of incorrect vectors in the pool.
     */
    private int calculateTotalIncorrectVectors(int[] sigma) {
        int incorrectVectorCount = 0;

        for (int i = 0; i < sigma.length; i++) {
            incorrectVectorCount += sigma[i];
        }

        return incorrectVectorCount;
    }

    /**
     * Calculates the similarity score between two integer arrays. 
     * 
     * The score is then adjusted based on the heuristic to give weight according to the count of incorrect vectors. 
     * If the lengths of the pool vector and the subsequence are not equal, the score is adjusted to reflect the ratio of incorrect vectors to the difference in lengths.
     * The score is further adjusted based on the ratio of the length of the subsequence to the length of the givenBugVector.
     * 
     * @param poolVector The pool vector.
     * @param givenBugVector The given bug vector.
     * @return A similarity score between 0 and 1, where approaching 1 indicates higher similarity.
     */
    public float scoreSimilarity(int[] poolVector, int[] givenBugVector) {

        float score = 1;
        float weight = 1;
        int[] subSequence = backtrack(longestCommonSubsequenceOfIntegerArray(poolVector, givenBugVector));

        // Calculate the count of incorrect vectors in the pool for each element in the DP table
        int[] sigma = calculateSigma(poolVector, subSequence);

        // Calculate the total count of incorrect vectors in the pool
        int incorrectVectorCount = calculateTotalIncorrectVectors(sigma);

        // Heuristic to give weight according to the incorrect vectors
        weight = 1 - ((float) incorrectVectorCount / poolVector.length);

        // Adjust the score based on the length of the DP table
        score = weight * ((float) subSequence.length / givenBugVector.length);

        return score;
    }

}
