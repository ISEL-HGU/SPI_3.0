package LCE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import org.apache.logging.log4j.*;
import org.apache.logging.log4j.core.config.Configurator;

import org.apache.commons.io.FileUtils;
import java.util.ArrayList;

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

    public void set(String path, String candidateDir) {
        this.resultDir = path;
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
    private boolean clone(String directory) {
        try {
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
     * Using git checkout -f, go to BIC and BFC
     * And start with ProcessBuilder
     * @param directory
     * @return 
     */
    private boolean checkout(String directory) {
        String cid1 = BIC; // possible bug inducing commit
        String cid2 = FIC; // possible fix inducing commit
        try {
            String project = d4jProjectName + "-" + d4jProjectNum;
            gitLogger.trace(App.ANSI_BLUE + "[status] > git checkout cid before : " + App.ANSI_YELLOW + cid1
                    + App.ANSI_RESET);
            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(new File(directory));
            pb.command("git", "checkout", "-f", cid1);
            Process p = pb.start();
            p.waitFor();
            gitLogger.trace(App.ANSI_GREEN + "[status] > git checkout success" + App.ANSI_RESET);
            if (!!copy(resultDir + "/" + name + "_" + counter + "/" + filepathBefore,
                    candidateDir + "/" + project + "_rank-" + counter + "_old.java"))
                gitLogger.trace(App.ANSI_GREEN + "[status] > copy success" + App.ANSI_RESET);
            else {
                gitLogger.error(App.ANSI_RED + "[error] > copy failed" + App.ANSI_RESET);
                return false;
            }

            gitLogger.trace(App.ANSI_BLUE + "[status] > git checkout cid after : " + App.ANSI_YELLOW + cid2
                    + App.ANSI_RESET);
            pb = new ProcessBuilder();
            pb.directory(new File(directory));
            pb.command("git", "checkout", "-f", cid2);
            p = pb.start();
            p.waitFor();
            gitLogger.trace(App.ANSI_GREEN + "[status] > git checkout success" + App.ANSI_RESET);
            if (copy(resultDir + "/" + name + "_" + counter + "/" + filepathAfter,
                    candidateDir + "/" + project + "_rank-" + counter + "_new.java"))
                gitLogger.trace(App.ANSI_GREEN + "[status] > copy success" + App.ANSI_RESET);
            else {
                gitLogger.error(App.ANSI_RED + "[error] > copy failed" + App.ANSI_RESET);
                return false;
            }
            return true;
        } catch (Exception e) {
            gitLogger.error(App.ANSI_RED + "[error] > " + e.getMessage() + App.ANSI_RESET);
            return false;
        }
    }

    public boolean load() {
        String path = resultDir + "/" + name + "_" + counter;
        try {
            if (set) {
                gitLogger.trace(App.ANSI_BLUE + "[status] > cloning to " + App.ANSI_YELLOW + path + App.ANSI_RESET);
                if (!clone(path))
                    return false;
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

    // get list of commit hashes from a git repository which certain file has been
    // changed
    // @param path : path of git repository
    // @param file : file to check
    private ArrayList<String> extractCid() {
        String repoPath = resultDir + name + "_" + counter;
        gitLogger.trace(App.ANSI_BLUE + "[status] > getting log of " + App.ANSI_YELLOW + repoPath + App.ANSI_RESET
                + " with " + App.ANSI_YELLOW + filename + App.ANSI_RESET);
        ArrayList<String> hashes = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(new File(repoPath));
            pb.command("git", "log", "--pretty=format:%H", filename);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = "";
            while ((line = reader.readLine()) != null) {
                hashes.add(line);
            }
            process.waitFor();
            process.destroy();
        } catch (Exception e) {
            gitLogger.error(App.ANSI_RED + "[error] > Exception : " + e.getMessage() + App.ANSI_RESET);
            return null;
        }
        return hashes;
    }

    // find the commit hash just before given cid
    // cid_before is the commit hash just before bug was induced
    // cid_after is the commit hash just after bug was induced
    // cid_fixed should be the commit hash which fixed the bug
    public String traverse(String BIC) {
        String FIC = "";
        try {
            ArrayList<String> commitHashList = extractCid();
            if (commitHashList == null) {
                gitLogger.error(App.ANSI_RED + "[error] > git log failed" + App.ANSI_RESET);
            }
            int index = commitHashList.indexOf(BIC);
            if (index == -1) {
                gitLogger.error(App.ANSI_RED + "[error] > given hash commit id not found" + App.ANSI_RESET);
            } else if (index == 0) {
                gitLogger.error(App.ANSI_RED + "[error] > buggy commit hash id is the latest" + App.ANSI_RESET);
            } else
                FIC = commitHashList.get(index - 1);
        } catch (Exception e) {
            gitLogger.error(App.ANSI_RED + "[error] > Exception : " + e.getMessage() + App.ANSI_RESET);
        }
        return FIC;
    }

    public String revTraverse(String FIC) {
        String BIC = "";
        try {
            ArrayList<String> commitHashList = extractCid();
            if (commitHashList == null) {
                gitLogger.error(App.ANSI_RED + "[error] > git log failed" + App.ANSI_RESET);
            }
            int index = commitHashList.indexOf(FIC);
            if (index == -1) {
                gitLogger.error(App.ANSI_RED + "[error] > given hash commit id not found" + App.ANSI_RESET);
            } else if (index == commitHashList.size() - 1) {
                gitLogger.error(App.ANSI_RED + "[error] > fixed commit hash id is the oldest" + App.ANSI_RESET);
            } else
                BIC = commitHashList.get(index + 1);
        } catch (Exception e) {
            gitLogger.error(App.ANSI_RED + "[error] > Exception : " + e.getMessage() + App.ANSI_RESET);
        }
        return BIC;
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

    public void clean2Directories() {
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
}