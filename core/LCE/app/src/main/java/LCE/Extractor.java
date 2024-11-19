package LCE;

import java.util.Comparator;
import java.util.TreeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Vector;
import java.util.HashMap;
import org.apache.logging.log4j.*;
import org.apache.logging.log4j.core.config.Configurator;
import java.util.Collections;

/**
 * The Extractor class is responsible for extracting candidate source codes based on
 * similarity scores and other specified criteria.
 * It reads input files, processes the data, and provides an interface to extract
 * relevant information.
 */
public class Extractor {
    LCS lcs;
    private String gumtreeVectorPath;
    private String targetVectorPath;
    private String commitFilePath;

    private List<String> sourceFiles;
    private int[] maxNumberIndexList;
    private List<List<String>> storedCommitList = new ArrayList<>();
    private List<List<String>> cleanedCommitList = new ArrayList<>();
    private int nummax;
    private int threshold;
    private boolean textSimOrNot;

    static Logger extractionLogger = LogManager.getLogger(Extractor.class.getName());
    public Extractor() {}

    /**
     * Constructor for the Extractor class, initialized using the provided properties.
     *
     * @param argv Properties containing configuration details for the Extractor.
     */
    public Extractor(Properties argv) {
        super();
        Configurator.setLevel(Extractor.class, Level.TRACE);
        lcs = new LCS();
        gumtreeVectorPath = argv.getProperty("pool_file.dir"); // gumtree vector path
        commitFilePath = argv.getProperty("meta_pool_file.dir"); // commit file path
        targetVectorPath = argv.getProperty("target_vector.dir"); // target dir
        nummax = argv.getProperty("candidate_number").equals("") ? 10
                : Integer.parseInt(argv.getProperty("candidate_number")); // nummax
        threshold = argv.getProperty("threshold").equals("") ? 1000
                : Integer.parseInt(argv.getProperty("threshold")); // threshold
        textSimOrNot = argv.getProperty("text_sim").equals("true") ? true : false ;
    }
    

    /**
     * Runs the extraction process, including reading files, processing data, and calculating
     * similarity scores to identify candidate patches.
     */
    public void run() {

        Vector<Integer> indexListToRemove = new Vector<Integer>();
        
        try {
            // Read gumtree_vector.csv file and convert to int[][]
            int[][] storedPoolArray = convert2DStringArrayListTo2DIntArray(ReadCSVto2DStringArrayList(gumtreeVectorPath));
            
            findEmptyLines(storedPoolArray, indexListToRemove);
            findExtraLongLines(storedPoolArray, indexListToRemove, threshold);

            int[][] cleanedGumTreeArray = syncPoolWithIndexListToRemove(storedPoolArray, indexListToRemove);

            storedCommitList = ReadCSVto2DStringArrayList(commitFilePath);
            cleanedCommitList = syncPoolWithIndexListToRemove(storedCommitList, indexListToRemove);

            
            extractionLogger.trace(App.ANSI_BLUE + "[status] original pool array size = " + storedPoolArray.length + App.ANSI_RESET);
            extractionLogger.trace(App.ANSI_BLUE + "[status] meta pool list size = " + storedCommitList.size() + App.ANSI_RESET);
            extractionLogger.trace(App.ANSI_BLUE + "[status] cleaned pool array size = " + cleanedGumTreeArray.length + App.ANSI_RESET);
            extractionLogger.trace(App.ANSI_BLUE + "[status] index_list_to_remove size : " + indexListToRemove.size() + App.ANSI_RESET);
            extractionLogger.trace(App.ANSI_BLUE + "[status] cleaned meta pool list size = " + cleanedCommitList.size() + App.ANSI_RESET);

            int[][] vectorArray = convert2DStringArrayListTo2DIntArray(ReadCSVto2DStringArrayList(targetVectorPath));
            float[] simScoreArray = new float[cleanedGumTreeArray.length];

            // score similarity on each line of pool and vector
            for (int i = 0; i < cleanedGumTreeArray.length; i++) {
                simScoreArray[i] = lcs.scoreSimilarity(cleanedGumTreeArray[i], vectorArray[0]);
            }

            HashMap<Float, ArrayList<Integer> > simScoreMap = makeMapScoreToIndex(simScoreArray);

            if (textSimOrNot) {
                maxNumberIndexList = indexOfCandidatePatches(simScoreMap, nummax*3 , cleanedGumTreeArray); //TODO:magic number, need to fix
            } else {
                maxNumberIndexList = indexOfCandidatePatches(simScoreMap, nummax , cleanedGumTreeArray); //TODO:magic number, need to fix
            }
            
            extractionLogger.trace(
                    App.ANSI_BLUE + "[status] max_N_index_list size = " + maxNumberIndexList.length + App.ANSI_RESET);
        } catch (FileNotFoundException e) {
            extractionLogger.error(App.ANSI_RED + "[error] file not found exception" + App.ANSI_RESET); // ERROR
            e.printStackTrace();
        } catch (Exception e) {
            extractionLogger.error(App.ANSI_RED + "[error] exception" + App.ANSI_RESET); // ERROR
            e.printStackTrace();
        }
    }


    /**
     * Extracts information from the processed data and returns a list of source files
     *
     * @return List<String> source_files List of source files meeting the extraction criteria.
     */
    public List<String> extract() {

        sourceFiles = new ArrayList<>();

        for (int i = 0; i < maxNumberIndexList.length; i++) {
            sourceFiles.add(combineStringListToCommaSeperatedLine(cleanedCommitList.get(maxNumberIndexList[i])));
            // print the result
            /* example: [result] 113471d6457b4afa2523afc74b40be09935292d0,1925a50d860b7b8f8422f1c2f251d0ea11def736,
                runners/spark/src/main/java/org/apache/beam/runners/spark/translation/streaming/StreamingTransformTranslator.java,
                runners/spark/src/main/java/org/apache/beam/runners/spark/translation/streaming/StreamingTransformTranslator.java,
                https://github.com/apache/beam.git,BEAM,(BlameLineNumber),(FixLineNumber)
            */
            extractionLogger.trace(App.ANSI_BLUE + "[result] " + sourceFiles.get(i) + App.ANSI_RESET);
        }

        return sourceFiles;
    }

    /**
     * Reads the contents of a CSV file and converts them into a List of Lists of Strings.
     *
     * @param filename The name of the CSV file to be read.
     * @return List<List<String>> A list containing records, where each record is represented as a list of strings.
     * @throws FileNotFoundException If the specified file is not found.
     */
    private List<List<String>> ReadCSVto2DStringArrayList(String filename) throws FileNotFoundException {

        List<List<String>> records = new ArrayList<>();
        try (Scanner scanner = new Scanner(new File(filename));) {
            while (scanner.hasNextLine()) {
                records.add(splitLine(scanner.nextLine()));
            }
        } catch (FileNotFoundException e) {
            extractionLogger
                    .error(App.ANSI_RED + "[status] file not found : " + App.ANSI_YELLOW + filename + App.ANSI_RESET); // DEBUG
            e.printStackTrace();
        }
        return records;
    }

    /**
     * Extracts individual values from a line by splitting it using a comma (",") delimiter.
     *
     * @param line The input line to be split.
     * @return List<String> A list containing individual values extracted from the input line.
     */
    private List<String> splitLine(String line) {
        List<String> values = new ArrayList<String>();
        try (Scanner rowScanner = new Scanner(line)) {
            rowScanner.useDelimiter(",");
            while (rowScanner.hasNext()) {
                values.add(rowScanner.next());
            }
        }
        return values;
    }

    /**
     * Converts a String array to an int array.
     *
     * @param array The String array to be converted.
     * @return int[] An array of integers converted from the input String array.
     */
    private int[] Array2Int(String[] array) {
        int[] converted = new int[array.length];
        // convert array to int array
        for (int i = 0; i < array.length; i++) {
            converted[i] = Integer.parseInt(array[i]);
        }
        // return int array
        return converted;
    }

    /**
     * Converts a List of Lists of Strings to a 2D int array.
     *
     * @param list The input list to be converted.
     * @return int[][] A 2D array of integers converted from the input list.
     */
    private int[][] convert2DStringArrayListTo2DIntArray(List<List<String>> list) {
        int[][] array = new int[list.size()][];
        for (int i = 0; i < list.size(); i++) {
            array[i] = Array2Int(list.get(i).toArray(new String[list.get(i).size()]));
        }
        return array;
    }

    /**
     * Combines a list of strings into a comma-separated line.
     *
     * @param list The list of strings to be combined.
     * @return String A comma-separated line containing the combined strings.
     */
    private String combineStringListToCommaSeperatedLine(List<String> list) {
        String combinedLine = "";
        for (int i = 0; i < list.size(); i++) {
            combinedLine += list.get(i) + ",";
        }
        // remove last comma of combined
        combinedLine = combinedLine.substring(0, combinedLine.length() - 1);
        return combinedLine;
    }

    /**
     * Locates and removes lines in the pool array with a vector size over the given threshold.
     *
     * @param poolArray       The pool array containing vectors.
     * @param indexList       The list to store indices of lines to be removed.
     * @param threshold       The threshold for vector size beyond which lines will be removed.
     */
    private void findExtraLongLines(int[][] poolArray, Vector<Integer> indexList, int threshold) {

        for (int i = 0; i < poolArray.length; i++) {
            if (poolArray[i].length > threshold) {
                indexList.add(i);
            }
        }

    }

   /**
     * Locates and removes empty lines in the pool array.
     *
     * @param poolArray           The pool array containing vectors.
     * @param indexListToRemove   The list to store indices of empty lines to be removed.
     */
    private void findEmptyLines(int[][] poolArray, Vector<Integer> indexListToRemove) {
        
        for (int i = 0; i < poolArray.length; i++) {
            if (poolArray[i].length == 0) {
                indexListToRemove.add(i);
            }
        }

    }

    /**
     * Synchronizes the cleaned commit list with the index list of lines to be removed.
     *
     * @param cleanedCommitList   The cleaned commit list to be synchronized.
     * @param indexListToRemove   The list containing indices of lines to be removed.
     * @return List<List<String>> The synchronized meta pool after removing specified lines.
     */
    private List<List<String>> syncPoolWithIndexListToRemove(List<List<String>> cleanedCommitList, Vector<Integer> indexListToRemove) {
        
        List<List<String>> synced_meta_pool = new ArrayList<>();
        for (int i = 0; i < cleanedCommitList.size(); i++) {
            if (!indexListToRemove.contains(i)) {
                synced_meta_pool.add(cleanedCommitList.get(i));
            } else {
                extractionLogger.trace("[debug] index " + i + " is removed"); // TODO:Need to Remove
            }
        }
        return synced_meta_pool;
    }
    
    /**
     * Synchronizes the pool array with the index list of lines to be removed.
     *
     * @param cleanedGumTreeArray The cleaned pool array to be synchronized.
     * @param indexListToRemove   The list containing indices of lines to be removed.
     * @return int[][] The synchronized pool array after removing specified lines.
     */
    private int[][] syncPoolWithIndexListToRemove(int[][] cleanedGumTreeArray, Vector<Integer> indexListToRemove) {

        int[][] new_pool_array = new int[cleanedGumTreeArray.length - indexListToRemove.size()][];
        int j = 0;
        for (int i = 0; i < cleanedGumTreeArray.length; i++) {
            if (!indexListToRemove.contains(i)) {
                new_pool_array[j] = cleanedGumTreeArray[i];
                j++;
            }
        }
        
        return new_pool_array;
    }
    
    /**
     * Counts the number of occurrences of each similarity score and maps them to the corresponding indices.
     *
     * @param orig Array of similarity scores.
     * @return HashMap<Float, ArrayList<Integer>> Resulting map of similarity scores to indices.
     */
    public HashMap<Float, ArrayList<Integer> > makeMapScoreToIndex(float[] orig) {

        HashMap<Float, ArrayList<Integer> > result = new HashMap<>();

        for (int i = 0; i < orig.length; i++) {

            float onTrial = orig[i];
            
            if (result.containsKey(onTrial)) {

                result.get(onTrial).add(i);

            } else {
                
                ArrayList<Integer> scoreIndex = new ArrayList<>();
                
                scoreIndex.add(i);
                result.put(onTrial, scoreIndex);

            }
        }

        return result;
    }

    /**
     * Identifies the indices of candidate patches based on similarity scores.
     *
     * If the number of left candidates is limited, 
     * The algorithm considers the vector length first and goes with the index 
     * 
     * @param simScoreMap Map of similarity scores to corresponding indices.
     * @param nummax      Maximum number of candidate patches to identify.
     * @param cleanedGumTreeArray Array containing the original pool of source code.
     * @return int[] Array of indices representing the identified candidate patches.
     */
    public int[] indexOfCandidatePatches(HashMap<Float, ArrayList<Integer> > simScoreMap, int nummax, int[][] cleanedGumTreeArray) {
        
        List<Float> scores = new ArrayList<>(simScoreMap.keySet());
        // sort from highest to lowsest
       Collections.sort(scores, Collections.reverseOrder());

       int resultPos = 0;
       int scorePos = 0;
       float targetScore = 0;
       int[] result = new int[nummax];
       int tieBreakingScore = 0;

        while (resultPos < nummax && !simScoreMap.isEmpty()) {

            int leftCandNum = nummax - resultPos;
            targetScore = scores.get(scorePos);
            
            extractionLogger.info(App.ANSI_BLUE + "[status] Candidate score = " + String.valueOf(targetScore) + " Size: " + simScoreMap.get(targetScore).size() + App.ANSI_RESET);
            
            if (leftCandNum >= simScoreMap.get(targetScore).size() ) {
                
                for (int index : simScoreMap.get(targetScore)) {
                    extractionLogger.info(App.ANSI_BLUE + "[status] LCS = " + Arrays.toString(cleanedGumTreeArray[index]) + " index: " + Integer.toString(index) + App.ANSI_RESET);
                    result[resultPos++] = index;
                }
                scorePos++;

            } else {

                
                TreeMap<Integer, ArrayList<Integer>> vectorLengthToIndex = new TreeMap<>(Comparator.naturalOrder());



                for (int index : simScoreMap.get(targetScore)) {

                    tieBreakingScore = cleanedGumTreeArray[index].length;
                    
                    if (vectorLengthToIndex.containsKey(tieBreakingScore)) {

                        vectorLengthToIndex.get(tieBreakingScore).add(index);

                    } else {
                        vectorLengthToIndex.put(tieBreakingScore, new ArrayList<>());
                        vectorLengthToIndex.get(tieBreakingScore).add(index);
                    }


                }

                int breaker = leftCandNum;

                extractionLogger.info("Vector size" + vectorLengthToIndex.size()); 


                for (int key : vectorLengthToIndex.keySet()) {

                    for (int index : vectorLengthToIndex.get(key)) {
                        
                        result[resultPos++] = index;
                        extractionLogger.info(App.ANSI_BLUE + "[status] LCS = " + Arrays.toString(cleanedGumTreeArray[index]) + " index: " + Integer.toString(index) + App.ANSI_RESET);
                        if (--breaker == 0) return result;

                    }

                }
            }
        }
        
        return result;

    }
}