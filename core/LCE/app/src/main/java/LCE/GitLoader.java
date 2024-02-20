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
    private int counter = -1;
    private String url; // git url
    private String name; // git repo name
    private String BIC; // bug inducing commit
    private String FIC; // fix inducing commit
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

    public GitLoader() {
        Configurator.setLevel(GitLoader.class, Level.TRACE);
        this.url = "";
        this.name = "";
        this.BIC = "";
        this.FIC = "";
        this.filepathBefore = "";
        this.filepathAfter = "";
        this.filename = "";
        this.resultDir = "";
        this.candidateDir = "";
        this.set = false;
        this.d4jProjectName = "";
        this.d4jProjectNum = -1;
    }


    public void config(String url, String BIC, String FIC, String filepathBefore, String filepathAfter,
            String d4jName, int d4jNum) {
        this.url = url;
        this.name = getRepoNameFromUrl(url);
        this.BIC = BIC;
        this.FIC = FIC;
        this.filepathBefore = filepathBefore;
        this.filepathAfter = filepathAfter;
        this.filename = getFileNameFromPath(filepathBefore);
        this.d4jProjectName = d4jName;
        this.d4jProjectNum = d4jNum;
    }

    public void set(String resultDir, String candidateDir) {
        this.resultDir = resultDir;
        this.candidateDir = candidateDir;
        if (resultDir != null && candidateDir != null) {
            set = true;
        }
    }

    /**
     * print log
     */
    public void logGitCloneStatus() { 
        gitLogger
                .trace(App.ANSI_YELLOW + "==========================================================" + App.ANSI_RESET);
        gitLogger
                .trace(App.ANSI_BLUE + "[info #" + counter + "] > git clone " + App.ANSI_YELLOW + url + App.ANSI_RESET);
        printDebugInfo();
    }

    public void getCounter(int counter) {
        this.counter = counter;
    }

    private String getRepoNameFromUrl(String url) {
        String[] url_split = url.split("/");
        for (String split : url_split) {
            if (split.contains(".git")) {
                return split.replace(".git", "");
            }
        }
        return url_split[url_split.length - 1];
    }

    private String getFileNameFromPath(String pathPath) {
        String[] splittedPatchPath = pathPath.split("/");
        return splittedPatchPath[splittedPatchPath.length - 1];
    }

    private void printDebugInfo() {
        gitLogger.trace(App.ANSI_BLUE + "[info] > url : " + App.ANSI_YELLOW + url + App.ANSI_RESET);
        gitLogger.trace(App.ANSI_BLUE + "[info] > repo_name : " + App.ANSI_YELLOW + name + App.ANSI_RESET);
        gitLogger.trace(App.ANSI_BLUE + "[info] > cid_before : " + App.ANSI_YELLOW + BIC + App.ANSI_RESET);
        gitLogger.trace(App.ANSI_BLUE + "[info] > cid_after : " + App.ANSI_YELLOW + FIC + App.ANSI_RESET);
        gitLogger.trace(App.ANSI_BLUE + "[info] > file_name : " + App.ANSI_YELLOW + filename + App.ANSI_RESET);
        gitLogger.trace(App.ANSI_BLUE + "[info] > result_dir : " + App.ANSI_YELLOW + resultDir + App.ANSI_RESET);
        gitLogger.trace(App.ANSI_BLUE + "[info] > candidate_dir : " + App.ANSI_YELLOW + candidateDir + App.ANSI_RESET);
    }

    /**
     * For git clone
     * @param directory
     * @return boolean
     */
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

    private boolean checkOutAndCopy(String directory, String cid, boolean oldFile) {

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
            
            if (oldFile) {
                if (copy(directory + "/" + filepathBefore,
                        candidateDir + "/" + project + "_rank-" + counter + "_old.java"))
                    return true;
                else 
                    return false;
            } else {
                if (copy(directory + "/" + filepathAfter,
                        candidateDir + "/" + project + "_rank-" + counter + "_new.java"))
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
     * Using git checkout -f, go to BIC and BFC
     * And start with ProcessBuilder
     * @param directory
     * @return 
     */
    public boolean checkOut(String directory) {

        if (checkOutAndCopy(directory, BIC, true)) {
            gitLogger.trace(App.ANSI_GREEN + "[status] > copy success" + App.ANSI_RESET);
        } else {
            gitLogger.error(App.ANSI_RED + "[error] > copy failed" + App.ANSI_RESET);
            return false;
        }

        if (checkOutAndCopy(directory, FIC, false)){
            gitLogger.trace(App.ANSI_GREEN + "[status] > copy success" + App.ANSI_RESET);
        } else {
            gitLogger.error(App.ANSI_RED + "[error] > copy failed" + App.ANSI_RESET);
            return false;
        }

        return true;
    }

    public boolean load() {
        String path = resultDir + "/" + name; 
        try {
            if (set) {
                gitLogger.trace(App.ANSI_BLUE + "[status] > cloning to " + App.ANSI_YELLOW + path + App.ANSI_RESET);
                if (!clone(path))
                    return false;
                gitLogger.trace(App.ANSI_BLUE + "[status] > checkout to " + App.ANSI_YELLOW + path + App.ANSI_RESET);
                if (!checkOut(path))
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

    private boolean checkDirExists(String directoryPath) {

        Path path = Paths.get(directoryPath);

        if (Files.exists(path) && Files.isDirectory(path)) {
            return true;
        } else {
            return false;
        }
    }

}

