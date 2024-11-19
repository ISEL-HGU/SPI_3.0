package LCE;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ExtractorTest {

    @Test
    void testMakeMapScoreToIndexTest() {
        // Test Case
        float[] originalArray = {2.5f, 3.0f, 2.5f, 4.0f, 3.0f, 4.0f, 2.5f};

        // Expected Result
        HashMap<Float, ArrayList<Integer>> expectedResult = new HashMap<>();
        expectedResult.put(2.5f, new ArrayList<>(List.of(0, 2, 6)));
        expectedResult.put(3.0f, new ArrayList<>(List.of(1, 4)));
        expectedResult.put(4.0f, new ArrayList<>(List.of(3, 5)));

        Extractor extractor = new Extractor();

        HashMap<Float, ArrayList<Integer>> result = extractor.makeMapScoreToIndex(originalArray);

        // Assertion
        assertEquals(expectedResult, result, "Test Case Failed!");
    }

    @Test
    void testIndexOfCandidatePatchesTest1() {
        // Test Case
        HashMap<Float, ArrayList<Integer>> simScoreMap = new HashMap<>();
        simScoreMap.put(0.9f, new ArrayList<>(List.of(7, 8, 9)));
        simScoreMap.put(0.5f, new ArrayList<>(List.of(0, 10)));
        simScoreMap.put(0.8f, new ArrayList<>(List.of(2, 4, 6)));
        simScoreMap.put(0.7f, new ArrayList<>(List.of(1, 3, 5)));

        int nummax = 10;
        int[][] storedPoolArray = {
            {1, 2, 3},
            {4, 5, 6, 7},
            {8, 9, 10, 11, 12},
            {8, 9, 10, 11, 12},
            {8, 9, 10, 11, 12},
            {8, 9, 10},
            {8, 9, 10, 11, 12}, 
            {1},
            {1, 2},
            {1, 2, 3}, 
            {2, 3}
        };

        // Expected Result
        int[] expectedResult = {7, 8, 9, 2, 4, 6, 1, 3, 5, 10};

        Extractor extractor = new Extractor();

        // Running the Test;
        int[] result = extractor.indexOfCandidatePatches(simScoreMap, nummax, storedPoolArray);

        // Assertion
        assertArrayEquals(expectedResult, result, "Test Case Failed!");;
    }

    @Test
    void testIndexOfCandidatePatches2() {
        // Assuming you have sample input data for testing
        HashMap<Float, ArrayList<Integer>> testScoreMap = new HashMap<>();
        testScoreMap.put(0.8f, new ArrayList<>(List.of(0, 2)));
        testScoreMap.put(0.5f, new ArrayList<>(List.of(1, 4)));
        testScoreMap.put(0.7f, new ArrayList<>(List.of(3)));

        int nummax = 3;
        int[][] testStoredPoolArray = {
                {1, 2, 3},
                {4, 5, 6, 7},
                {8, 9, 10, 11, 12},
                {13, 14, 15},
                {16, 17, 18, 19, 20}
        };

        int[] expectedIndexes = {0, 2, 3};

        // Create the Extractor instance and call the method
        Extractor extractor = new Extractor();
        int[] actualIndexes = extractor.indexOfCandidatePatches(testScoreMap, nummax, testStoredPoolArray);

        // Validate the results
        assertArrayEquals(expectedIndexes, actualIndexes, "Indexes of Candidate Patches should match");
    }

    @Test
    void testIndexOfCandidatePatches3() {
        // Assuming you have sample input data for testing
        HashMap<Float, ArrayList<Integer>> testScoreMap = new HashMap<>();
        testScoreMap.put(0.8f, new ArrayList<>(List.of(0, 2, 5, 6)));
        testScoreMap.put(0.5f, new ArrayList<>(List.of(1, 4, 11, 12, 13)));
        testScoreMap.put(0.7f, new ArrayList<>(List.of(3, 7, 8, 9, 10)));

        int nummax = 3;
        int[][] testStoredPoolArray = {
                {1, 2, 3},
                {4, 5, 6, 7},
                {8, 9, 10, 11, 12},
                {13, 14, 15},
                {16, 17, 18, 19, 20},
                {1, 2, 3},
                {1, 2, 3}, 
                {1, 2, 3},
                {4, 5, 6, 7},
                {8, 9, 10, 11, 12},
                {13, 14, 15},
                {16, 17, 18, 19, 20},
                {1, 2, 3},
                {1, 2, 3}
        };

        int[] expectedIndexes = {0, 5, 6};

        // Create the Extractor instance and call the method
        Extractor extractor = new Extractor();
        int[] actualIndexes = extractor.indexOfCandidatePatches(testScoreMap, nummax, testStoredPoolArray);

        // Validate the results
        assertArrayEquals(expectedIndexes, actualIndexes, "Indexes of Candidate Patches should match");

        nummax = 5;
        int[] expectedIndexes2 = {0, 2, 5, 6, 3};

        actualIndexes = extractor.indexOfCandidatePatches(testScoreMap, nummax, testStoredPoolArray);

        // Validate the results
        assertArrayEquals(expectedIndexes2, actualIndexes, "Indexes of Candidate Patches should match");

    }
    
}
