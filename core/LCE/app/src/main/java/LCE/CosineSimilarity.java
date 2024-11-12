/**
 * This class provides functions to interact with a Git repository,
 * including extracting source code differences between commits,
 * retrieving commit hashes, performing blame operations, and more.
 */
package LCE;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.client.Run;
import com.github.gumtreediff.gen.TreeGenerators;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.Tree;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Comparator;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import org.apache.commons.text.*;
import org.apache.commons.text.similarity.CosineDistance;

/**
 * This class provides functions to interact with a Git repository,
 * including extracting source code differences between commits,
 * retrieving commit hashes, performing blame operations, and more.
 */

import org.apache.logging.log4j.*;

public class CosineSimilarity {

    private String resultDir;
    private String gitDir;
    private String targetDiffDir;
    private String textSimTarget;
    private boolean poolHasCommitId;
    private int count = 0;
    private int nummax = 0;

    static Logger cosineSimLog = LogManager.getLogger(CosineSimilarity.class.getName());

    /**
     * Constructor for GitFunctions class.
     * @param gitDir Path to the Git repository directory.
     * @param resultDir Path to the directory where results will be stored.
     * @param nummax Maximum number of top candidates to retrieve.
     */

    public CosineSimilarity(String gitDir, String resultDir, String targetDiffDir, String textSimTarget, boolean poolHasCommitId, int nummax) {

        this.gitDir = gitDir;
        this.resultDir = resultDir;
        this.targetDiffDir = targetDiffDir;
        this.textSimTarget = textSimTarget;
        this.poolHasCommitId = poolHasCommitId;
        this.nummax = nummax;

        boolean success = Util.createDirectory(resultDir);

        if (!success) {
            cosineSimLog.trace(App.ANSI_RED + "[status] > copying BBIC, BIC, BFC" + App.ANSI_RESET);
        }

    }


    /**
     * Runs the GitFunctions to process a list of LCE candidates.
     * @param LCEcandidate List of LCE candidates.
     * @return List of top candidates based on text similarity.
     */
    public List<String> run(List<String> LCEcandidate) {
        if (poolHasCommitId) {
            if (getDiffCandidates(LCEcandidate)) {
                cosineSimLog.trace(App.ANSI_GREEN + "[status] > copying BBIC, BIC, BFC" + App.ANSI_RESET);
            } else {
                cosineSimLog.error(App.ANSI_RED + "[status] > failed to copy BBIC, BIC, BFC" + App.ANSI_RESET);
            }
        } else if (textSimTarget.equals("tree")) {
            if (getDiffCandidatesNoCommitId(LCEcandidate)) {
                cosineSimLog.trace(App.ANSI_GREEN + "[status] > copying BBIC, BIC, BFC" + App.ANSI_RESET);
            } else {
                cosineSimLog.error(App.ANSI_RED + "[status] > failed to copy BBIC, BIC, BFC" + App.ANSI_RESET);
            }
        }

        int[] topIndex = getTopCandidatesUsingTextSim();
        List<String> result = new ArrayList<String>();

        for (int index : topIndex){
            result.add(LCEcandidate.get(index));
        }

        return result;
    }

    /**
     * This method retrieves the top candidates using text similarity.
    *
    * @return An array of integers representing the indices of the top candidates.
    */ 
     public int[] getTopCandidatesUsingTextSim() {
        
        CosineDistance cosineDistance = new CosineDistance();
        String BBIC_BIC_diff;
        String BIC_BFC_diff;

        HashMap<Integer, Double> scoreCandidateMap = new HashMap<>();

        for (int i = 0 ; i < count ; i++) {

            BBIC_BIC_diff = resultDir;
            BIC_BFC_diff = resultDir;
                    
            BBIC_BIC_diff += "/BBIC-BIC" + Integer.toString(i) + ".txt";
            BIC_BFC_diff += "/BIC-BFC" + Integer.toString(i) + ".txt";

            double simScore = 0;

            if (textSimTarget.equals("tree")) {
                String targetDiff = targetDiffDir;
                
                // tokenize: candidateDiff & targetDiff
                ArrayList<String> candidateDiffFile = gumtreeDiffTokenizer(BBIC_BIC_diff);
                ArrayList<String> targetDiffFile = gumtreeDiffTokenizer(targetDiff);

                double cosineSimScore;

                if (candidateDiffFile == null) {
                    simScore = 0;
                } else {
                    for (String targetChange : targetDiffFile) {
                        for (String candidateChage : candidateDiffFile) {
                            cosineSimScore = Util.getCosineSimilarity(candidateChage, targetChange);
                            
                            if (cosineSimScore > simScore) {
                                simScore = cosineSimScore;
                            }
                        }
                    }
                }
            } else {
                String BBIC_BIC_diff_File = Util.readFile(BBIC_BIC_diff);
                String BIC_BFC_diff_File = Util.readFile(BIC_BFC_diff);  

                if (BBIC_BIC_diff_File == null || BIC_BFC_diff_File == null) {
                    simScore = 0;     
                } else {
                    simScore = Util.getCosineSimilarity(BBIC_BIC_diff_File, BIC_BFC_diff_File);
                    // simScore = cosineDistance.apply(BBIC_BIC_diff_File, BIC_BFC_diff_File);
                }
            }

            scoreCandidateMap.put(i, simScore);

            cosineSimLog.trace(App.ANSI_GREEN + "  cosine score: " + i + " " + simScore + App.ANSI_RESET);

        }

        // Create a list from the entries of the HashMap
        List<Map.Entry<Integer, Double>> list = new LinkedList<>(scoreCandidateMap.entrySet());

        // Sort the list based on values in descending order
        Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() {
            @Override
            public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
                return Double.compare(o2.getValue(), o1.getValue()); // Descending order
            }
        });

        // Create a LinkedHashMap to store the sorted entries
        LinkedHashMap<Integer, Double> sortedHashMap = new LinkedHashMap<>();
        for (Map.Entry<Integer, Double> entry : list) {
            sortedHashMap.put(entry.getKey(), entry.getValue());
        }

        int[] topTextSimCandidates = new int[nummax];
        int i = 0;

        for (Integer index : sortedHashMap.keySet()) {

            topTextSimCandidates[i++] = index;

            cosineSimLog.trace(App.ANSI_GREEN + "[status] > selected index" + index + App.ANSI_RESET);

            if (nummax == i+1) {
                break;
            }

        }

        return topTextSimCandidates;

   }

    public boolean getDiffCandidatesNoCommitId(List<String> LCEcandidate) {
        String diffOutputDir;

        for (String line : LCEcandidate) {

            System.err.println(line);

            /* example: [result] poolDir + Closure_67_b/src/com/google/javascript/jscomp/AnalyzePrototypeProperties.java
            ,poolDir + Closure_67_f/src/com/google/javascript/jscomp/AnalyzePrototypeProperties.java
            ,Closure
            */
            String[] csvValues = line.split(",");
            String BBIC_dest = "";
            String BIC_dest = "";
            BBIC_dest += csvValues[1];
            BIC_dest += csvValues[0];

            diffOutputDir = resultDir;
            diffOutputDir += "/BBIC-BIC" + Integer.toString(count) + ".txt";

            try {
                File diffOutputFile = new File(diffOutputDir);
                BufferedWriter writer = new BufferedWriter(new FileWriter(diffOutputFile, false));

                writer.write(BBIC_dest+"\n");
                writer.write(BIC_dest+"\n");

                Run.initGenerators();
                // create bic and bbic java files

                Tree src = TreeGenerators.getInstance().getTree(BBIC_dest).getRoot();
                Tree dst = TreeGenerators.getInstance().getTree(BIC_dest).getRoot();

                Matcher defaultMatcher = Matchers.getInstance().getMatcher();
                MappingStore mappings = defaultMatcher.match(src, dst);
                EditScriptGenerator editScriptGenerator = new SimplifiedChawatheScriptGenerator();
                EditScript actions = editScriptGenerator.computeActions(mappings);

                String line_log = actions.asList().toString();
                writer.write(line_log + "\n");

                writer.close();
            } catch (Exception e) {
                // catch exception
            }

            count += 1;
        }

        return true;
    }
     
    /**
     * This method retrieves differences for a list of candidate files.
     *
     * @param LCEcandidate A list of strings representing candidate file information.
     * @return True if differences are successfully obtained, otherwise false.
     */
    public boolean getDiffCandidates(List<String> LCEcandidate){

        String BBIC_dest;
        String BIC_dest;
        String BFC_dest;

        String BBIC_BIC_diff;
        String BIC_BFC_diff;

        for (String line : LCEcandidate) {

            System.err.println(line);

            BBIC_dest = resultDir;
            BIC_dest = resultDir;
            BFC_dest = resultDir;
    
            BBIC_BIC_diff = resultDir;
            BIC_BFC_diff = resultDir;

            String[] csvValues = line.split(","); 
             /* example: [result] 113471d6457b4afa2523afc74b40be09935292d0,
                1925a50d860b7b8f8422f1c2f251d0ea11def736,
                runners/spark/src/main/java/org/apache/beam/runners/spark/translation/streaming/StreamingTransformTranslator.java,
                runners/spark/src/main/java/org/apache/beam/runners/spark/translation/streaming/StreamingTransformTranslator.java,
                https://github.com/apache/beam.git,
                BEAM,
                (BlameLineNumber),(FixLineNumber)
            */

            String gitPath = gitDir + "/" + Util.getFileNameFromPath(csvValues[4]);

            String diff[] = extract_diff(gitPath, csvValues[2], csvValues[0], Integer.parseInt(csvValues[7]), Integer.parseInt(csvValues[6])); // OldPath, BISha1, LineNumInPreFix, LineNumInBI
            

            String[] commitCollection = new String[6];

            if (diff == null) {
                count += 1;
                continue;
            }

            for (int i = 0 ; i < 4 ; i++) {
                commitCollection[i] = diff[i];
            }
            commitCollection[4] = csvValues[1];
            commitCollection[5] = csvValues[3];

            /* extract the BBIC commit id and file
            e.g)
            BBIC: 5b6ed0544a4b81a6f28c6b33384c45f357fdc909
            BBICFile: sdk/src/main/java/com/google/cloud/dataflow/sdk/util/ReduceFnRunner.java
            BIC: f111e6762a3404c3241564808460789cffcff41c
            BICFile: sdk/src/main/java/com/google/cloud/dataflow/sdk/util/ReduceFnRunner.java
            BFC: 91999bdc8e51a51bdd271862e23c6dc33f777bc1
            BFCFile: sdk/src/main/java/com/google/cloud/dataflow/sdk/util/ReduceFnRunner.java
            */
        
            BBIC_dest += "/BBIC_"+ Integer.toString(count) + ".java";
            BIC_dest += "/BIC_"+ Integer.toString(count) + ".java";
            BFC_dest += "/BFC_"+ Integer.toString(count) + ".java";
            
            openLocalRepository(gitPath, commitCollection[0], commitCollection[1], BBIC_dest);
            openLocalRepository(gitPath, commitCollection[2], commitCollection[3], BIC_dest);
            openLocalRepository(gitPath, commitCollection[4], commitCollection[5], BFC_dest);

            BBIC_BIC_diff += "/BBIC-BIC" + Integer.toString(count) + ".txt";
            BIC_BFC_diff += "/BIC-BFC" + Integer.toString(count) + ".txt";

            diffTwoFiles(BBIC_dest, BIC_dest, BBIC_BIC_diff);
            diffTwoFiles(BIC_dest, BFC_dest, BIC_BFC_diff);

            count += 1;
        }

        return true;
    }

    public ArrayList<String> gumtreeDiffTokenizer(String gumtree) {
        ArrayList<String> gumtreeTokens = new ArrayList<>();
        StringBuilder tokens = new StringBuilder();
        boolean startTokenizing = false;

        try {
            BufferedReader logReader = new BufferedReader(new FileReader(gumtree));
            String line;

            while ((line = logReader.readLine()) != null) {
                // Skip lines until the first "===" is found
                if (!startTokenizing) {
                    if (line.contains("===")) {
                        startTokenizing = true;
                    }
                    continue;
                }

                // Tokenize after the first "===" is found
                StringTokenizer stringTokenizer = new StringTokenizer(line);
                while (stringTokenizer.hasNext()) {
                    String token = stringTokenizer.nextToken();
                    
                    // If "===" is encountered, save current tokens and reset for next set
                    if (token.equals("===")) {
                        if (tokens.length() > 0) {
                            gumtreeTokens.add(tokens.toString().trim());
                            tokens.setLength(0);  // Reset the tokens string builder
                        }
                        continue;
                    }

                    // Skip tokens matching the pattern "[<number>,<number>]"
                    if (token.matches("\\[\\d+,\\d+\\]")) {
                        continue;
                    }

                    tokens.append(token).append(" ");
                }
            }

            // Add remaining tokens if any
            if (tokens.length() > 0) {
                gumtreeTokens.add(tokens.toString().trim());
            }

            logReader.close();
        } catch (Exception e) {
            return null;
        }

        return gumtreeTokens;
    }

    /**
     * Opens a local repository, checks out a specific commit, and copies a file from it.
     *
     * @param gitRepo          Path to the Git repository.
     * @param commitId         Commit ID to checkout.
     * @param fileName         Name of the file to copy.
     * @param destinationPath  Path where the copied file will be saved.
     * @return True if the operation is successful, otherwise false.
     */
    private static boolean openLocalRepository(String gitRepo, String commitId, String fileName, String destinationPath) {

        try {
           ProcessBuilder pb = new ProcessBuilder("git", "checkout", commitId);
           pb.directory(new File(gitRepo));
           Process p = pb.start();
           p.waitFor();
           copyFile(gitRepo, fileName, destinationPath);
       } catch (IOException | InterruptedException e) {
           e.printStackTrace();
           return false;
       }

       return true;
   }

    /**
     * Copies a file from a source location to a destination location.
     *
     * @param repositoryPath  Path to the repository containing the file.
     * @param filePath        Path to the file to be copied.
     * @param destinationPath Destination path for the copied file.
     */ 
   public static void copyFile(String repositoryPath, String filePath, String destinationPath) {
       try {
           // Locate the file in the repository
           Path sourceFilePath = Paths.get(repositoryPath, filePath);

           // Read the contents of the file
           byte[] fileContent = Files.readAllBytes(sourceFilePath);

           // Write the contents to a new file
           Path destinationFilePath = Paths.get(destinationPath);
           Files.write(destinationFilePath, fileContent);

       } catch (IOException e) {
           e.printStackTrace();
       }
   }


    /**
     * Computes the difference between two files and writes the output to a third file.
     *
     * @param file1Path Path to the first file.
     * @param file2Path Path to the second file.
     * @param outputPath Path to write the difference output.
     * @return True if the difference computation is successful, otherwise false.
     */
   public static boolean diffTwoFiles(String file1Path, String file2Path, String outputPath) {

         try {
           List<String> command = new ArrayList<>();
           command.add("diff");
           command.add("-u"); // Unified diff format
           command.add(file1Path);
           command.add(file2Path);

           ProcessBuilder processBuilder = new ProcessBuilder(command);
           // processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT); // Redirect output to stdio

           File outputFile = new File(outputPath);
           processBuilder.redirectOutput(outputFile);

           Process p = processBuilder.start();

           p.waitFor();

       } catch (IOException | InterruptedException e) {
           e.printStackTrace();
           return false;
       }

       return true;
   }


  /**
     * Retrieves the commit ID of the HEAD commit in the specified Git repository.
     *
     * @param path Path to the Git repository.
     * @return The commit ID of the HEAD commit, or null if an error occurs.
     */ 
    public String extract_head_commit_id(String path) {
        String commit_id = null;
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "HEAD");
            pb.directory(new File(path));
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            commit_id = reader.readLine();
            p.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return commit_id;
    }

    /**
     * Retrieves a list of commit hashes from a Git repository for a file that has been changed.
     *
     * @param repo_path   Path to the Git repository.
     * @param file_name   Name of the file to check.
     * @return A list of commit hashes, or null if an error occurs.
     */
    public ArrayList<String> log(String repo_path, String file_name) {

        ArrayList<String> hashes = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(new File(repo_path));
            pb.command("git", "log", "--pretty=format:%H", file_name);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = "";
            while ((line = reader.readLine()) != null) {
                hashes.add(line);
            }
            process.waitFor();
            process.destroy();
        } catch (Exception e) {

            return null;
        }
        return hashes;
    }

    /**
     * Executes 'git blame' on a file and line within a project directory and collects old and new commit IDs.
     *
     * @param project_dir Directory of the Defects4J bug.
     * @param file        The file to be blamed.
     * @param lineBlame   The line to be blamed.
     * @param lineFix     The line number to be fixed.
     * @return An array containing the old and new commit IDs, or null if an error occurs.
     */  
    public String[] blame(String project_dir, String file, int lineBlame, int lineFix) {
        String[] cid_set = new String[2]; // [0] old cid, [1] new cid
        String cid1; // new
        String cid2; // old
        int exit_code = -1;
        try {
            ProcessBuilder blame_builder = new ProcessBuilder("git", "-C", project_dir, "blame", "-C", "-C", "-f", "-l",
                    "-L",
                    String.format("%s,%s", lineBlame, lineFix), file);
            blame_builder.directory(new File(project_dir));
            Process p = blame_builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder str_builder = new StringBuilder();
            for (String l = reader.readLine(); l != null; l = reader.readLine()) {
                str_builder.append(l);
                str_builder.append(System.lineSeparator());
            }
            cid1 = str_builder.toString().split(" ")[0].strip(); // new cid
            exit_code = p.waitFor();
            if (exit_code != 0) {


                return null;
            }
            ProcessBuilder parse_builder = new ProcessBuilder("git", "-C", project_dir, "rev-parse",
                    String.format("%s~1", cid1));
            parse_builder.directory(new File(project_dir));
            p = parse_builder.start();
            reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            str_builder = new StringBuilder();
            for (String l = reader.readLine(); l != null; l = reader.readLine()) {
                str_builder.append(l);
                str_builder.append(System.lineSeparator());
            }
            cid2 = str_builder.toString().split(" ")[0].strip(); // old cid
            exit_code = p.waitFor();
            if (exit_code != 0) {

                return null;
            }
        } catch (Exception e) {

            return null;
        }
        cid_set[0] = cid2;
        cid_set[1] = cid1;
        return cid_set;
    }

    /**
     * Retrieves the old commit ID (BBIC) for a given file, line, and new commit ID (BIC).
     *
     * @param project_dir Directory of the project.
     * @param file        The file to be blamed.
     * @param lineBlame   The line to be blamed.
     * @param lineFix     The line number to be fixed.
     * @param bic         The new commit ID.
     * @return The old commit ID (BBIC), or null if an error occurs.
     */
    public String blame(String project_dir, String file, int lineBlame, int lineFix, String bic) {
        String bbic = ""; // old
        int exit_code = -1;
        try {

            ProcessBuilder checkout_builder = new ProcessBuilder("git", "checkout", "-f", bic);
            checkout_builder.directory(new File(project_dir));
            Process checkout = checkout_builder.start();
            exit_code = checkout.waitFor();
            if (exit_code != 0) {

                System.err.println("git checkout failed");
                return null;
            }
            ProcessBuilder parse_builder = new ProcessBuilder("git", "-C", project_dir, "blame", "-C", "-C", "-f", "-l",
                    "-L",
                    String.format("%s,%s", lineBlame, lineFix), file);
            parse_builder.directory(new File(project_dir));
            Process p = parse_builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder str_builder = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            str_builder = new StringBuilder();
            for (String l = reader.readLine(); l != null; l = reader.readLine()) {

                str_builder.append(l);
                str_builder.append(System.lineSeparator());
            }
            bbic = str_builder.toString().split(" ")[0].strip(); // old cid
            exit_code = p.waitFor();
            if (exit_code != 0) {

                return null;
            }
        } catch (Exception e) {

            System.err.println("exception");
            return null;
        }
        return bbic;
    }

    /**
     * Extracts source code differences between a commit ID and the version of a certain source file.
     *
     * @param repo_git  Path of the Git repository.
     * @param file_name File name to check.
     * @param new_cid   Bug Inducing Commit ID.
     * @param lineFix   Line number to be fixed.
     * @param lineBlame Line to be blamed.
     * @return An array containing old and new commit IDs, or null if an error occurs.
     */
    public String[] extract_diff(String repo_git, String file_name, String new_cid, int lineFix, int lineBlame) {
        
        String old_cid = null;
       
        try {
            if (isInit(repo_git, new_cid)) {
              
                return null;
            }
            old_cid = blame(repo_git, file_name, lineBlame, lineFix, new_cid);
            if (old_cid == null || old_cid.isEmpty()) {

                return null;
            } else if (old_cid.equals(new_cid)) {
                ArrayList<String> hashes = log(repo_git, file_name);
               
                int index = hashes.indexOf(new_cid);
                if (index == -1) {
                    
                    return null;
                }
                // find old_cid's index
                int old_index = index + 1;
                if (old_index >= hashes.size()) {

                    return null;
                }
                old_cid = hashes.get(old_index);
            }
        } catch (Exception e) {

            return null;
        }
        // if old_cid contains "^" as first character, remove it
        if (old_cid.charAt(0) == '^') {
            old_cid = old_cid.substring(1);
        }
        return extract_diff(repo_git, file_name, new_cid, old_cid);
    }

    /**
     * Extracts source code differences between two commit IDs of a certain source file.
     *
     * @param repo_git Path of the Git repository.
     * @param file_name File name to check.
     * @param new_cid Fix Inducing Commit ID.
     * @param old_cid Commit ID before Fix Inducing Commit ID.
     * @return An array containing old and new commit IDs, or null if an error occurs.
     */
    public String[] extract_diff(String repo_git, String file_name, String new_cid, String old_cid) {

        String[] result = new String[4];
        try {
            Git git = Git.open(new File(repo_git));
            Repository repository = git.getRepository();

            ObjectReader reader = repository.newObjectReader();

            ObjectId oldHead = repository.resolve(old_cid + "^{tree}");
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            oldTreeIter.reset(reader, oldHead);
            if (oldHead == null) {

                return null;
            }

            ObjectId newHead = repository.resolve(new_cid + "^{tree}");
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            newTreeIter.reset(reader, newHead);
            if (newHead == null) {

                return null;
            }
            DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
            diffFormatter.setRepository(repository);
            List<DiffEntry> entries = diffFormatter.scan(oldTreeIter, newTreeIter);
            if (entries.size() == 0 || entries.equals(null)) {

                return null;
            }
            for (DiffEntry entry : entries) {
                String str_new = entry.getNewPath();
                String str_old = entry.getOldPath();
                if (str_new.endsWith(".java") && str_old.endsWith(".java")) {
                    if (file_name.equals("")) {
                        result[0] = old_cid;
                        result[1] = str_old;
                        result[2] = new_cid;
                        result[3] = str_new;
                    } else {
                        if (str_new.contains(file_name)) {
                            result[0] = old_cid;
                            result[1] = str_old;
                            result[2] = new_cid;
                            result[3] = str_new;
                        }
                    }
                }
            }
            for (String entry : result) {
                if (entry == null || entry.equals("")) {

                    return null;
                }
            }
            diffFormatter.close();
            repository.close();
            git.close();
        } catch (Exception e) {

            return null;
        }
        return result;
    }


    /**
     * Checks if a given commit ID is the initial commit in the repository.
     *
     * @param repo_path Path of the Git repository.
     * @param cid       The commit ID to check.
     * @return True if the commit ID is the initial commit, otherwise false.
     */    // check if given commit id is the initial commit or not
    public static boolean isInit(String repo_path, String cid) {
        try {
            Git git = Git.open(new File(repo_path));
            Repository repository = git.getRepository();
            ObjectId head = repository.resolve(cid + "^{tree}");
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            oldTreeIter.reset(repository.newObjectReader(), head);
            if (oldTreeIter == null) {
                return true;
            }
            git.close();
            repository.close();
        } catch (Exception e) {

            return false;
        }
        return false;
    }
}
