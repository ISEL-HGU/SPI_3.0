package LCE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import org.apache.logging.log4j.*;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.commons.io.FileUtils;
import java.nio.file.Files;

public class GitLoader {
    private int maxCandidateNum = -1;
    private int counter = -1;
    private String url; // git url
    private String name; // git repo name
    private String BIC; // bug inducing commit
    private String BFC; // bug fixing commit
    private String filepathBefore; // file path before
    private String filepathAfter; // file path (after)
    private String filename; // file name
    private String resultDir; // result dir
    private String candidateDir; // candidate dir
    private boolean set; // if result dir exist

    static Logger gitLogger = LogManager.getLogger(GitLoader.class.getName());
    // d4j
    private String d4jProjectName; // d4j project name
    private int d4jProjectNum; // d4j project num

    /**
     * The GitLoader class facilitates the loading and manipulation of Git repositories
     * to extract Bug Inducing File and Fix Inducing File
     */
    public GitLoader() {
        Configurator.setLevel(GitLoader.class, Level.TRACE);
        this.url = "";
        this.name = "";
        this.BIC = "";
        this.BFC = "";
        this.filepathBefore = "";
        this.filepathAfter = "";
        this.filename = "";
        this.resultDir = "";
        this.candidateDir = "";
        this.set = false;
        this.d4jProjectName = "";
        this.d4jProjectNum = -1;
    }

    /**
     * Configures the GitLoader with necessary information
     *
     * @param url             The URL of the Git repository.
     * @param BIC             The commit ID associated with a bug-inducing commit.
     * @param BFC             The commit ID associated with a fix-inducing commit.
     * @param filepathBefore  The file path before the changes.
     * @param filepathAfter   The file path after the changes.
     * @param d4jName         The name of the Defects4J project.
     * @param d4jNum          The number of the Defects4J project.
     */
    public void config(String url, String BIC, String BFC, String filepathBefore, String filepathAfter,
            String d4jName, int d4jNum) {
        this.url = url;
        this.name = getRepoNameFromUrl(url);
        this.BIC = BIC;
        this.BFC = BFC;
        this.filepathBefore = filepathBefore;
        this.filepathAfter = filepathAfter;
        this.filename = Util.getFileNameFromPath(filepathBefore);
        this.d4jProjectName = d4jName;
        this.d4jProjectNum = d4jNum;
    }

    /**
     * Sets the result and candidate directories for storing project results and
     * candidate files respectively.
     *
     * @param resultDir     The path to the result directory.
     * @param candidateDir  The path to the candidate directory.
     */
    public void set(String resultDir, String candidateDir) {
        this.resultDir = resultDir;
        this.candidateDir = candidateDir;
        if (resultDir != null && candidateDir != null) {
            set = true;
        }
    }

    /**
     * Logs the status of the Git clone operation, including the repository URL.
     */
    public void logGitCloneStatus() { 
        gitLogger
                .trace(App.ANSI_YELLOW + "==========================================================" + App.ANSI_RESET);
        gitLogger
                .trace(App.ANSI_BLUE + "[info #" + counter + "] > git clone " + App.ANSI_YELLOW + url + App.ANSI_RESET);
        printDebugInfo();
    }

    /**
     * Sets the internal counter value for tracking candidate numbers
     *
     * @param counter The counter value to set.
     */
    public void setCounter(int counter) {
        this.counter = counter;
    }

    /**
     * Sets the maximum number of candidates to track
     *
     * @param maxCandidateNum The maxCandidateNum value to set.
     */
    public void setmaxCandidateNum(int maxCandidateNum) {
        this.maxCandidateNum = maxCandidateNum;
    }

    /**
     * Extracts the repository name from a Git repository URL.
     *
     * @param url The Git repository URL.
     * @return The repository name extracted from the URL.
     */
    private String getRepoNameFromUrl(String url) {
        String[] url_split = url.split("/");
        for (String split : url_split) {
            if (split.contains(".git")) {
                return split.replace(".git", "");
            }
        }
        return url_split[url_split.length - 1];
    }


    /**
     * Prints debug information about the GitLoader configuration to the logging system.
     */
    private void printDebugInfo() {
        gitLogger.trace(App.ANSI_BLUE + "[info] > url : " + App.ANSI_YELLOW + url + App.ANSI_RESET);
        gitLogger.trace(App.ANSI_BLUE + "[info] > repo_name : " + App.ANSI_YELLOW + name + App.ANSI_RESET);
        gitLogger.trace(App.ANSI_BLUE + "[info] > cid_before : " + App.ANSI_YELLOW + BIC + App.ANSI_RESET);
        gitLogger.trace(App.ANSI_BLUE + "[info] > cid_after : " + App.ANSI_YELLOW + BFC + App.ANSI_RESET);
        gitLogger.trace(App.ANSI_BLUE + "[info] > file_name : " + App.ANSI_YELLOW + filename + App.ANSI_RESET);
        gitLogger.trace(App.ANSI_BLUE + "[info] > result_dir : " + App.ANSI_YELLOW + resultDir + App.ANSI_RESET);
        gitLogger.trace(App.ANSI_BLUE + "[info] > candidate_dir : " + App.ANSI_YELLOW + candidateDir + App.ANSI_RESET);
    }

    
    /**
     * Clones the Git repository into the specified directory.
     *
     * @param directory The directory where the repository will be cloned.
     * @return True if cloning is successful, false otherwise.
     */
    @Deprecated
    public boolean clone(String directory) {
        try {
            if ( checkDirExists(directory) ) { //already exists
                return true;
            }
            gitLogger.trace(App.ANSI_BLUE + "[status] > cloning start" + App.ANSI_RESET);
            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(new File(resultDir));
            pb.command("git", "clone", url, directory);
            Process p = pb.start();
            p.waitFor();
            gitLogger.trace(App.ANSI_GREEN + "[status] > cloning done" + App.ANSI_RESET);
            return true;
        } catch (Exception e) {
            gitLogger.error(App.ANSI_RED + "[error] > " + e.getMessage() + App.ANSI_RESET);
            return false;
        }
    }

    /**
     * Checks out a specific commit (indicated by the commit ID) in the Git repository
     * located in the given directory. Optionally, copies either the file before or after
     * the changes based on the 'oldFile' parameter.
     *
     * @param directory The directory containing the Git repository.
     * @param cid       The commit ID to check out.
     * @param oldFile   Indicates whether to copy the file before changes (true) or after changes (false).
     * @return True if the checkout and copy operations are successful, false otherwise.
     */
    private boolean checkoutAndCopy(String directory, String cid, boolean oldFile) {

        try {
            String project = d4jProjectName + "-" + d4jProjectNum;
            gitLogger.trace(App.ANSI_BLUE + "[status] > git checkout cid before : " + App.ANSI_YELLOW + cid
                    + App.ANSI_RESET);
            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(new File(directory));
            pb.command("git", "checkout", "-f", cid);
            Process p = pb.start();
            p.waitFor();
            gitLogger.trace(App.ANSI_GREEN + "[status] > git checkout success" + App.ANSI_RESET);

            int paddingLength = String.valueOf(maxCandidateNum).length();
            String paddedCounter = String.format("%0" + paddingLength + "d", counter);
            
            if (oldFile) {
                if (copy(directory + "/" + filepathBefore,
                        candidateDir + "/" + project + "_rank-" + paddedCounter + "_old.java"))
                    return true;
                else 
                    return false;
            } else {
                if (copy(directory + "/" + filepathAfter,
                        candidateDir + "/" + project + "_rank-" + paddedCounter + "_new.java"))
                    return true;
                else
                    return false;
            }


        } catch (Exception e) {
            gitLogger.error(App.ANSI_RED + "[error] > " + e.getMessage() + App.ANSI_RESET);
            return false;
        }
    }

    /**
     * Checks out specific commits (BIC and BFC) in the Git repository located in the given directory.
     * Copies both the file before and after changes to candidate directories.
     *
     * @param directory The directory containing the Git repository.
     * @return True if both checkout and copy operations are successful, false otherwise.
     */
    public boolean checkout(String directory) {

        if (checkoutAndCopy(directory, BIC, true)) {
            gitLogger.trace(App.ANSI_GREEN + "[status] > copy success" + App.ANSI_RESET);
        } else {
            gitLogger.error(App.ANSI_RED + "[error] > copy failed" + App.ANSI_RESET);
            return false;
        }

        if (checkoutAndCopy(directory, BFC, false)){
            gitLogger.trace(App.ANSI_GREEN + "[status] > copy success" + App.ANSI_RESET);
        } else {
            gitLogger.error(App.ANSI_RED + "[error] > copy failed" + App.ANSI_RESET);
            return false;
        }

        return true;
    }

    /**
     * Loads the Git repository by cloning it to a specified directory, checking out specific commits,
     * and copying relevant files to candidate directories.
     *
     * @return True if the loading process is successful, false otherwise.
     */
    public boolean load() {
        String path = resultDir + "/" + name; 
        try {
            if (set) {
                gitLogger.trace(App.ANSI_BLUE + "[status] > checkout to " + App.ANSI_YELLOW + path + App.ANSI_RESET);
                if (!checkout(path))
                    return false;
                gitLogger.trace(App.ANSI_GREEN + "[status] > loading done" + App.ANSI_RESET);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            gitLogger.error(App.ANSI_RED + "[error] > " + e.getMessage() + App.ANSI_RESET);
            return false;
        }
    }

    /**
     * Copies a file from one path to another.
     *
     * @param path1 The source file path.
     * @param path2 The destination file path.
     * @return True if the copy is successful, false otherwise.
     */
    public boolean copy(String path1, String path2) {
        File file = new File(path1);
        File file2 = new File(path2);
        try {
            FileInputStream input = new FileInputStream(file);
            FileOutputStream output = new FileOutputStream(file2);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
            input.close();
            output.close();
            return true;
        } catch (IOException e) {
            gitLogger.error(App.ANSI_RED + "[error] > IOException : " + e.getMessage() + App.ANSI_RESET);
            return false;
        }
    }

     /**
     * Cleans the candidate and result directories by removing all contents.
     */
    public void cleanCandidateAndResultDir() {
        try {
            File dir = new File(resultDir);
            File dir2 = new File(candidateDir);
            if (!dir.exists())
                dir.mkdir();
            if (!dir2.exists())
                dir2.mkdir();
            FileUtils.cleanDirectory(dir);
            FileUtils.cleanDirectory(dir2);
        } catch (IOException e) {
            gitLogger.error(App.ANSI_RED + "[error] > IOException : " + e.getMessage() + App.ANSI_RESET);
        }
    }

    /**
     * Checks if a directory exists at the specified path.
     *
     * @param directoryPath The path of the directory to check.
     * @return True if the directory exists, false otherwise.
     */
    private boolean checkDirExists(String directoryPath) {

        Path path = Paths.get(directoryPath);

        if (Files.exists(path) && Files.isDirectory(path)) {
            return true;
        } else {
            return false;
        }
    }

}

