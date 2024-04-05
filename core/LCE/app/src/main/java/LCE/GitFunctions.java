package LCE;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;

public class GitFunctions {

    private String resultDir;
    private String gitDir;
    private int count = 0;


    public GitFunctions(String gitDir, String resultDir) {

        this.gitDir = gitDir;
        this.resultDir = resultDir;

        boolean success = Util.createDirectory(resultDir);

        if (success) {
            System.out.println("Directory created successfully.");
        } else {
            System.out.println("Failed to create directory.");
        }
    }

    public List<String> getTopCandidatesUsingTextSimimilarity(List<String> LCEcandidate){

        String BBIC_dest;
        String BIC_dest;
        String BFC_dest;

        String BBIC_BIC_diff;
        String BIC_BFC_diff;

        for (String line : LCEcandidate) {

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

            String gitPath = gitDir + Util.getFileNameFromPath(csvValues[4]);

            String diff[] = extract_diff(gitPath, csvValues[2], csvValues[0], Integer.parseInt(csvValues[7]), Integer.parseInt(csvValues[6])); // OldPath, BISha1, LineNumInPreFix, LineNumInBI
            

            String[] commitCollection = new String[6];

            if (diff == null) continue;

            for (int i = 0 ; i < 4 ; i++) {
                commitCollection[i] = diff[i];
            }
            commitCollection[4] = csvValues[3];
            commitCollection[5] = csvValues[2];

            /* extract the BBIC commit id and file
            e.g)
            BBIC: 5b6ed0544a4b81a6f28c6b33384c45f357fdc909
            BBICFile: sdk/src/main/java/com/google/cloud/dataflow/sdk/util/ReduceFnRunner.java
            BIC: f111e6762a3404c3241564808460789cffcff41c
            BICFile: sdk/src/main/java/com/google/cloud/dataflow/sdk/util/ReduceFnRunner.java
            BFC: 91999bdc8e51a51bdd271862e23c6dc33f777bc1
            BFCFile: sdk/src/main/java/com/google/cloud/dataflow/sdk/util/ReduceFnRunner.java
            */

            BBIC_dest += "result/BBIC_"+ Integer.toString(count) + ".java";
            BIC_dest += "result/BIC_"+ Integer.toString(count) + ".java";
            BFC_dest += "result/BFC_"+ Integer.toString(count) + ".java";
            
            openLocalRepository(gitPath, commitCollection[0], commitCollection[1], BBIC_dest);
            openLocalRepository(gitPath, commitCollection[2], commitCollection[3], BIC_dest);
            openLocalRepository(gitPath, commitCollection[4], commitCollection[5], BFC_dest);

            BBIC_BIC_diff += "result/BBIC-BIC" + Integer.toString(count) + ".txt";
            BIC_BFC_diff += "result/BIC-BFC" + Integer.toString(count) + ".txt";

            diffTwoFiles(BBIC_dest, BIC_dest, BBIC_BIC_diff);
            diffTwoFiles(BIC_dest, BFC_dest,  BIC_BFC_diff);

            count++;
        }

        return null;
    }


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

    // get commit id of head commit
    // @param path : path of git repository
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

    // get list of commit hashes from a git repository which certain file has been
    // changed
    // @param path : path of git repository
    // @param file : file to check
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

    // execute git blame on the file and line within project directory and collect
    // old, new cid
    // @param project_dir: the directory of the Defects4J bug
    // @param file : the file to be blamed
    // @param line : the line to be blamed
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

    // extract source code differences between a commit id and before of a certain
    // source file
    // @param repo_git : path of git repository
    // @param file_name : file name to check
    // @param new_cid : Bug Inducing Commit ID
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

    // extract source code differences between two commit ids of a certain source
    // @param repo_git : path of git repository
    // @param file_name : file name to check
    // @param new_cid : Fix Inducing Commit ID
    // @param old_cid : Commit ID before Fix Inducing Commit ID
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

    // check if given commit id is the initial commit or not
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
