package LCE;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Vector;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.logging.log4j.*;
import org.apache.logging.log4j.core.config.Configurator;
import java.util.Collections;

public class Extractor {
    LCS lcs;
    private String gumtree_vector_path;
    private String target_vector_path;
    private String commit_file_path;

    private List<String> source_files;
    private int[] max_N_index_list;
    private List<List<String>> storedCommitList = new ArrayList<>();
    private List<List<String>> cleaned_meta_pool_list = new ArrayList<>();
    private int nummax;
    private int threshold;

    static Logger extractionLogger = LogManager.getLogger(Extractor.class.getName());

    /**
     * Initalize using lce.properties file
     * @param argv 
     */
    public Extractor(Properties argv) {
        super();
        Configurator.setLevel(Extractor.class, Level.TRACE);
        lcs = new LCS();
        gumtree_vector_path = argv.getProperty("pool_file.dir"); // gumtree vector path
        commit_file_path = argv.getProperty("meta_pool_file.dir"); // commit file path
        target_vector_path = argv.getProperty("target_vector.dir"); // target dir
        nummax = argv.getProperty("candidate_number").equals("") ? 10
                : Integer.parseInt(argv.getProperty("candidate_number")); // nummax
        threshold = argv.getProperty("threshold").equals("") ? 1000
                : Integer.parseInt(argv.getProperty("threshold")); // threshold
    }
    
    public void run() {

        Vector<Integer> indexListToRemove = new Vector<Integer>();
        
        try {
            // Read gumtree_vector.csv file and convert to int[][]
            int[][] storedPoolArray = convert2DStringArrayListTo2DIntArray(ReadCSVto2DStringArrayList(gumtree_vector_path));
            
            findEmptyLines(storedPoolArray, indexListToRemove);
            findExtraLongLines(storedPoolArray, indexListToRemove, threshold);

            int[][] regressed_pool_array = syncPoolWithIndexListToRemove(storedPoolArray, indexListToRemove);

            storedCommitList = ReadCSVto2DStringArrayList(commit_file_path);
            cleaned_meta_pool_list = syncPoolWithIndexListToRemove(storedCommitList, indexListToRemove);

            
            extractionLogger.trace(App.ANSI_BLUE + "[status] original pool array size = " + storedPoolArray.length + App.ANSI_RESET);
            extractionLogger.trace(App.ANSI_BLUE + "[status] meta pool list size = " + storedCommitList.size() + App.ANSI_RESET);
            extractionLogger.trace(App.ANSI_BLUE + "[status] cleaned pool array size = " + regressed_pool_array.length + App.ANSI_RESET);
            extractionLogger.trace(App.ANSI_BLUE + "[status] index_list_to_remove size : " + indexListToRemove.size() + App.ANSI_RESET);
            extractionLogger.trace(App.ANSI_BLUE + "[status] cleaned meta pool list size = " + cleaned_meta_pool_list.size() + App.ANSI_RESET);

            int[][] vector_array = convert2DStringArrayListTo2DIntArray(ReadCSVto2DStringArrayList(target_vector_path));
            
            float[] sim_score_array = new float[regressed_pool_array.length];

            // score similarity on each line of pool and vector
            for (int i = 0; i < regressed_pool_array.length; i++) {
                sim_score_array[i] = lcs.ScoreSimilarity(regressed_pool_array[i], vector_array[0]);
            }
            HashMap<Float, Integer> sim_score_map = count_number_of_sim_scores(sim_score_array); // new
            // max_N_index_list = indexesOfTopElements(sim_score_array, nummax);
            max_N_index_list = index_of_candidate_patches(regressed_pool_array, sim_score_array, sim_score_map, nummax);
            //[jh] version
            //max_N_index_list = index_of_candidate_patches(sim_score_array, sim_score_map, threshold);
            extractionLogger.trace(
                    App.ANSI_BLUE + "[status] max_N_index_list size = " + max_N_index_list.length + App.ANSI_RESET);
        } catch (FileNotFoundException e) {
            extractionLogger.error(App.ANSI_RED + "[error] file not found exception" + App.ANSI_RESET); // ERROR
            e.printStackTrace();
        } catch (Exception e) {
            extractionLogger.error(App.ANSI_RED + "[error] exception" + App.ANSI_RESET); // ERROR
            e.printStackTrace();
        }
    }


    /**
     * 
     * @return List<String> source_files
     */
    public List<String> extract() {

        source_files = new ArrayList<>();

        for (int i = 0; i < max_N_index_list.length; i++) {
            source_files.add(combineStringListToCommaSeperatedLine(cleaned_meta_pool_list.get(max_N_index_list[i])));
            // print the result
            /* example: [result] 113471d6457b4afa2523afc74b40be09935292d0,1925a50d860b7b8f8422f1c2f251d0ea11def736,
                runners/spark/src/main/java/org/apache/beam/runners/spark/translation/streaming/StreamingTransformTranslator.java,
                runners/spark/src/main/java/org/apache/beam/runners/spark/translation/streaming/StreamingTransformTranslator.java,
                https://github.com/apache/beam.git,BEAM
            */
            extractionLogger.trace(App.ANSI_BLUE + "[result] " + source_files.get(i) + App.ANSI_RESET);
        }

        return source_files;
    }

    /**
     * convert csv file contects to arraylist
     * @param filename
     * @return List<List<String>> records
     * @throws FileNotFoundException
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
     * Get a record from a line
     * split using "," 
     * @param line
     * @return List<String>
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
     * convert String array to int array
     * @param array
     * @return intArray
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
     * convert List<List<String>> list to int[][] array
     * @param list
     * @return int[][] array
     */
    private int[][] convert2DStringArrayListTo2DIntArray(List<List<String>> list) {
        int[][] array = new int[list.size()][];
        for (int i = 0; i < list.size(); i++) {
            array[i] = Array2Int(list.get(i).toArray(new String[list.get(i).size()]));
        }
        return array;
    }

    private String combineStringListToCommaSeperatedLine(List<String> list) {
        String combinedLine = "";
        for (int i = 0; i < list.size(); i++) {
            combinedLine += list.get(i) + ",";
        }
        // remove last comma of combined
        combinedLine = combinedLine.substring(0, combinedLine.length() - 1);
        return combinedLine;
    }

    // locate and remove lines with vector size over given threshold
    private void findExtraLongLines(int[][] poolArray, Vector<Integer> index_list, int threshold) {

        for (int i = 0; i < poolArray.length; i++) {
            if (poolArray[i].length > threshold) {
                index_list.add(i);
            }
        }

    }

    // locate and remove empty lines in pool
    private void findEmptyLines(int[][] poolArray, Vector<Integer> indexListToRemove) {
        
        for (int i = 0; i < poolArray.length; i++) {
            if (poolArray[i].length == 0) {
                indexListToRemove.add(i);
            }
        }

    }

    /**
     * 
     * @param meta_pool
     * @param index_list
     * @return synced_meta_pool
     */
    private List<List<String>> syncPoolWithIndexListToRemove(List<List<String>> meta_pool, Vector<Integer> index_list) {
        
        List<List<String>> synced_meta_pool = new ArrayList<>();
        for (int i = 0; i < meta_pool.size(); i++) {
            if (!index_list.contains(i)) {
                synced_meta_pool.add(meta_pool.get(i));
            } else {
                extractionLogger.trace("[debug] index " + i + " is removed"); // TODO:Need to Remove
            }
        }
        return synced_meta_pool;
    }
    
    private int[][] syncPoolWithIndexListToRemove(int[][] pool_array, Vector<Integer> index_list) {

        int[][] new_pool_array = new int[pool_array.length - index_list.size()][];
        int j = 0;
        for (int i = 0; i < pool_array.length; i++) {
            if (!index_list.contains(i)) {
                new_pool_array[j] = pool_array[i];
                j++;
            }
        }
        
        return new_pool_array;
    }
    
    /**
     * count number of each similarity score
     * @param orig
     * @return HashMap<Float, Integer> result
     */
    private HashMap<Float, Integer> count_number_of_sim_scores(float[] orig) {
        HashMap<Float, Integer> result = new HashMap<>();
        for (int i = 0; i < orig.length; i++) {
            float onTrial = orig[i];
            if (result.containsKey(onTrial)) {
                result.put(onTrial, result.get(onTrial) + 1);
            } else {
                result.put(onTrial, 1);
            }
        }
        return result;
    }

    /**
     * return index of specific similarity score
     */
    private int[] indexes_of_specific_sim_score(float[] orig, HashMap<Float, Integer> map, float targetScore) {
        int[] result = new int[map.get(targetScore)];
        int resultPos = 0;
        for (int i = 0; i < orig.length; i++) {
            float onTrial = orig[i];
            if (onTrial == targetScore) {
                result[resultPos++] = i;
            }
        }
        return result;
    }
    
    /**
     * 
     */
    private int[] index_of_candidate_patches(int[][] regressed_pool_array, float[] orig, HashMap<Float, Integer> map, int nummax) {
        // if nummax = 10, then return index of top 10 max similarity scores
        int[] result = new int[nummax];
        int resultPos = 0;
        float targetScore = 0;
        Iterator<Float> keyIterator = map.keySet().iterator();
        Float[] scores = new Float[map.keySet().size()];
        int scorePos = 0;

        while(keyIterator.hasNext()){
            scores[scorePos] = (float) keyIterator.next();
            scorePos++;
        }
        scorePos = 0;
        // sort from highest to lowsest
        Arrays.sort(scores, Collections.reverseOrder());

        while (resultPos < nummax) {
            int leftCandNum = nummax - resultPos;
            // if there is no more similarity score, then break
            if (map.isEmpty()) {
                break;
            }
            // if a similarity score is highest, and has more occurence than nummax, return
            // randomly picked indexes of that similarity score

            targetScore = scores[scorePos];
            
            // if left candidate number is higher than number of occurence of target score,
            // then return all indexes of target score
            if (leftCandNum >= map.get(targetScore)) {
                int[] indexes = indexes_of_specific_sim_score(orig, map, targetScore);
                for (int i = 0; i < indexes.length; i++) {
                    result[resultPos++] = indexes[i];
                }
                map.remove(targetScore);
                scorePos++;
            } else {
                // if left candidate number is lower than number of occurence of target score,
                // then return randomly picked indexes of target score
                int[] indexes = indexes_of_specific_sim_score(orig, map, targetScore);
                extractionLogger.trace("In candidate patch, indexes length = " + indexes.length + App.ANSI_RESET);
                
                //[JH] version2 no_random
                
                //Seperate index by project

                String temp_project_name = new String();
                List<List<String>> seperatedIndex = new ArrayList<>();
                List<String> targetList;
                for(int i = 0; i < indexes.length; i++) {
                    targetList = cleaned_meta_pool_list.get(indexes[i]);
                    temp_project_name = targetList.get(5);
                    List<String> seperatedList;
                    String projectName;
                    int checkNewProject = 1;
                    for(int j = 0; j < seperatedIndex.size(); j++) {
                        extractionLogger.trace(App.ANSI_BLUE + "[status] i, j = " + i + " " + j + App.ANSI_RESET);
                        seperatedList = seperatedIndex.get(j);
                        projectName = seperatedList.get(0);
                        if(projectName.equals(temp_project_name)) {
                            // insertion sort by vector length version
                            // int shorterCount = 1; // start = 1, 0th element = project name
                            // int idx = indexes[i];
                            // for(int k = 1; k < seperatedList.size(); k++) {
                            //     int kthIndex = Integer.parseInt(seperatedList.get(k));
                            //     if(regressed_pool_array[idx].length < regressed_pool_array[kthIndex].length) break;
                            //     shorterCount++;
                            // }                            
                            //seperatedList.add(shorterCount, Integer.toString(idx));
                            
                            //

                            // 크기 상관없이 뒤로 들어가는 버전
                            seperatedList.add(Integer.toString(indexes[i]));
                            //
                            
                            checkNewProject = 0;    
                            extractionLogger.trace(App.ANSI_BLUE + "[status] seperatedList add = " + seperatedList.toString() + App.ANSI_RESET);
                            break;
                        }
                    }
                    if(checkNewProject == 1) {
                        List<String> newProjectList = new ArrayList<>();
                        newProjectList.add(temp_project_name);
                        seperatedIndex.add(newProjectList);
                        extractionLogger.trace(
                        App.ANSI_BLUE + "[status] newProject add = " + newProjectList.toString()
                            + App.ANSI_RESET);
                    }
                }
                
                

                //Extract index form projects one by one
                int count = 0;
                for(int i =0; count < leftCandNum ; i++) {
                    int order = i % seperatedIndex.size();
                    targetList = seperatedIndex.get(order);
                    int projectCount = targetList.size() - 1;
                    extractionLogger.trace(
                        App.ANSI_BLUE + "[status] targetList = " + targetList.toString()
                            + App.ANSI_RESET);      
                    if(projectCount >= 1) {
                        // int randomIndex = (int) (Math.random() * projectCount + 1);
                        // result[resultPos++] = Integer.parseInt(targetList.get(randomIndex));
                        result[resultPos++] = Integer.parseInt(targetList.get(1));
                        targetList.remove(1);
                        count++;
                    }
                }
                for(int z =0 ; z< result.length;z++) {
            extractionLogger.trace(
                    App.ANSI_BLUE + "[status] candidate result [" + z+  "] =  " + result[z]
                            + App.ANSI_RESET);
        }
                map.remove(targetScore);
                scorePos++;

                //capstone version

                // int[] randomIndexes = new int[leftCandNum];
                // for (int i = 0; i < leftCandNum; i++) {
                //     int randomIndex = (int) (Math.random() * indexes.length);
                //     randomIndexes[i] = indexes[randomIndex];
                // }
                // for (int i = 0; i < randomIndexes.length; i++) {
                //     result[resultPos++] = randomIndexes[i];
                // }
                // map.remove(targetScore);
                // scorePos++;
                
                //[JH] version1 random_no_duplicate
                // int[] randomIndexes = new int[leftCandNum];
                // for (int i = 0; i < leftCandNum; i++) {
                //     int randomIndex = (int) (Math.random() * indexes.length);
                //     int checkDuplicates = 0;
                //     for(int j = 0; j < randomindexes.length; j++) {
                //         if(randomIndexes[j] == indexes[randomIndex]) {
                //             checkDuplicates = 1;
                //             i--;
                //             break;
                //         }
                //     }
                //     if(checkDuplicates == 0){
                //         randomIndexes[i] = indexes[randomIndex];
                //     }
                // }
                // for (int i = 0; i < randomIndexes.length; i++) {
                //     result[resultPos++] = randomIndexes[i];
                // }
                // map.remove(targetScore);
                // scorePos++;
            }
        }
        return result;
    }
}