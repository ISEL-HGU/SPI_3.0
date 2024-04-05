package LCE;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class LCSTest {

    @Test
    void testLongestCommonSubsequenceofIntegerArray() {

        LCS lcs = new LCS();

        // Test case 1: Arrays with common subsequence
        int[] array1 = {1, 2, 3, 4, 5};
        int[] array2 = {2, 4, 5, 6};
        int[] expectedLCS = {5, 4, 2};
        assertArrayEquals(expectedLCS, lcs.longestCommonSubsequenceOfIntegerArray(array1, array2));

        // Test case 2: Arrays with no common subsequence
        int[] array3 = {1, 2, 3};
        int[] array4 = {4, 5, 6};
        int[] expectedEmptyLCS = {};
        assertArrayEquals(expectedEmptyLCS, lcs.longestCommonSubsequenceOfIntegerArray(array3, array4));
    }

    @Test
    void testBacktrack() {
        LCS lcs = new LCS();

        // Test case 1: Reverse array
        int[] dpTable = {1, 2, 3, 4};
        int[] expectedReversed = {4, 3, 2, 1};
        assertArrayEquals(expectedReversed, lcs.backtrack(dpTable));

        // Test case 2: Empty array
        int[] emptyArray = {};
        int[] expectedEmptyArray = {};
        assertArrayEquals(expectedEmptyArray, lcs.backtrack(emptyArray));
    }

    @Test
    void testScoreSimilarity() {
        LCS lcs = new LCS();

        // Test case 1: Arrays with common subsequence
        int[] array1 = {243,316,243,316,338,316,272,316,442,73,73,168,102};
        int[] array2 = {243,316,243,316,338,316,272,316,442,73,73,168,102};
        float expectedScore1 = 1;
        assertEquals(expectedScore1, lcs.scoreSimilarity(array1, array2));

        // Test case 2: Arrays with no common subsequence
        int[] array3 = {1, 2, 3};
        int[] array4 = {4, 5, 6};
        float expectedScore2 = 0.0f;
        assertEquals(expectedScore2, lcs.scoreSimilarity(array3, array4));

        
        int[] array5 = {243,316,243,316,338,316,272,316,442,73,73,168,102,597,613,612,522,602,542,613,612,670,612,535,597,613,612,522,602,542,613,612,560,564,607,675,591,526,552,525,612,612,526,546,615,613,607,612,607,526,613,612,670,612,535,613,612,607,607,607,607,552,580,676,612,612,677,607,612};
        int[] array6 = {243,316,243,316,338,316,272,316,442,73,73,168,102};
        float expectedScore3 = 1;
        assertEquals(expectedScore3, lcs.scoreSimilarity(array5, array6));

    }
}
