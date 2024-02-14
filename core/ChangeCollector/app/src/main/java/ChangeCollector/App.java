/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package ChangeCollector;

import java.io.*;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;

public class App {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    public static Logger logger = LogManager.getLogger(App.class.getName());

    private GitFunctions gitFunctions;
    private Extractor extractor;
    private Implemental implemental;
    private String project_root;
    private String file_name;
    private String commit_id;
    private String git_name;
    private String git_url;
    private String output_dir;
    private String workspace_dir;
    private boolean doClean;
    // clean output directory or not
    private String mode;
    private String java_home_8;
    // Defects4J
    private String defects4j_name;
    private String defects4j_id;
    private String hash_id;

    public static void main(String[] args) {
        App app = new App();
        Properties properties = args.length > 0 ? app.loadProperties(args[0]) : app.loadProperties();
        app.run(properties);
    }

    private void initProperties(Properties properties) {
        project_root = properties.getProperty("project_root"); // the root directory of the project
        file_name = properties.getProperty("file_name"); // file name to extract change vector from
        commit_id = properties.getProperty("commit_id"); // commit id to extract change vector from
        git_name = properties.getProperty("git_name"); // repository name : unnecessary if url is given
        git_url = properties.getProperty("git_url"); // repository url
        output_dir = properties.getProperty("output_dir"); // output directory
        doClean = properties.getProperty("doClean").equals("true"); // a boolean trigger to determine whether to
                                                                            // clean output directory or not
        mode = properties.getProperty("mode"); // mode : "repository" or "file" or "defects4j"
        java_home_8 = properties.getProperty("JAVA_HOME.8"); // directory where jdk 8 is installed
        // Defects4J
        defects4j_name = properties.getProperty("defects4j_name"); // defects4j bug name
        defects4j_id = properties.getProperty("defects4j_id"); // defects4j bug id
        hash_id = properties.getProperty("hash_id"); // hash id of the current execution

        if (hash_id == null || hash_id.equals("")) {
	    hash_id = String.valueOf(System.currentTimeMillis());
	}

        // in case of hash id usage
        output_dir = String.format("%s/%s/%s", output_dir, hash_id, "outputs/ChangeCollector");
        workspace_dir = String.format("%s/%s", output_dir, hash_id);
    }

    private void cleanOutputDir() {
        logger.debug(ANSI_PURPLE + "[debug] > Cleaning output directory" + ANSI_RESET);
        try {
            FileUtils.deleteDirectory(new File(workspace_dir));
            if (!new File(output_dir).exists()) {
                new File(output_dir).mkdirs();
            }
            // remove commit_file.csv, gumtree_vector.csv
            if (new File(String.format("%s/commit_file.csv", output_dir)).exists()) {
                new File(String.format("%s/commit_file.csv", output_dir)).delete();
            }
            if (new File(String.format("%s/gumtree_vector.csv", output_dir)).exists()) {
                new File(String.format("%s/gumtree_vector.csv", output_dir)).delete();
            }
        } catch (Exception e) {
            logger.error(ANSI_RED + "[error] > Exception : " + e.getMessage() + ANSI_RESET);
        }
    }

    private void githubChangeCollect(Properties properties, String repo_git) {
        // STEP 1 : extract source code differences between current commit and before
        int lineFix = Integer.parseInt(properties.getProperty("lineFix"));
        int lineBlame = Integer.parseInt(properties.getProperty("lineBlame"));
        String[] diff = gitFunctions.extract_diff(repo_git, file_name, commit_id, lineFix, lineBlame);
        if (diff == null) {
            logger.fatal(ANSI_RED + "[fatal] > Failed to extract diff" + ANSI_RESET);
            System.exit(1);
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(output_dir, "diff.txt")));
            for (String line : diff) {
                writer.write(line);
                writer.write(" ");
            }
            writer.newLine();
            writer.close();
        } catch (Exception e) {
            logger.fatal(ANSI_RED + "[fatal] > Exception : " + e.getMessage() + ANSI_RESET);
            System.exit(1);
        }
        logger.info(ANSI_GREEN + "[info] > Extracted diff successfully" + ANSI_RESET);

        // STEP 2 : extract commit ids and file names and write them to a file
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(
                    new File(output_dir, GitFunctions.get_repo_name_from_url(git_url) + "_commit_file.csv")));
            for (String line : diff) {
                writer.write(line + ",");
            }
            writer.write(git_url);
            writer.newLine();
            writer.close();
        } catch (Exception e) {
            logger.fatal(ANSI_RED + "[fatal] > Exception : " + e.getMessage() + ANSI_RESET);
            System.exit(1);
        }
        logger.info(ANSI_GREEN + "[info] > Successfully created " + GitFunctions.get_repo_name_from_url(git_url)
                + "_commit_file.csv for single file " + file_name + ANSI_RESET);

        // STEP 3 : extract change vector from diff and write it to a file

        String diff_path = output_dir + "/diff.txt";
        if (!extractor.extract_gumtree_log(repo_git, diff_path, output_dir)) {
            logger.fatal(ANSI_RED + "[fatal] > Failed to extract gumtree log" + ANSI_RESET);
            System.exit(1);
        }
        logger.info(ANSI_GREEN + "[info] > Successfully extracted gumtree log" + ANSI_RESET);

        String gumtree_log = output_dir + "/gumtree_log.txt";
        int cv_extraction_result = extractor.extract_vector(git_name, gumtree_log, output_dir);
        if (cv_extraction_result == -1) {
            logger.fatal(ANSI_RED + "[fatal] > Failed to extract change vector due to exception" + ANSI_RESET);
            System.exit(1);
        } else if (cv_extraction_result == 1) {
            logger.fatal(ANSI_RED + "[fatal] > Failed to extract change vector due to no change" + ANSI_RESET);
            System.exit(1);
        }
        logger.info(ANSI_GREEN + "[info] > Successfully extracted change vector" + ANSI_RESET);
    }

    private void defects4jChangeCollect() {
        String[] cid_set = new String[2]; // [0] old cid [1] new cid

        // STEP 1 : extract Defects4J commit ids and file names from given Defects4J
        // name and identifier

        if (!implemental.config(project_root, defects4j_name, Integer.parseInt(defects4j_id), output_dir,
                java_home_8, hash_id)) {
            logger.fatal(ANSI_RED + "[fatal] > Failed to configure defects4j" + ANSI_RESET);
            System.exit(1);
        }
        if (!implemental.preprocess()) {
            logger.fatal(ANSI_RED + "[fatal] > Failed to preprocess defects4j" + ANSI_RESET);
            System.exit(1);
        }
        if (!implemental.fetch()) {
            logger.fatal(ANSI_RED + "[fatal] > Failed to fetch defects4j" + ANSI_RESET);
            logger.fatal(ANSI_RED + "==============================================" + ANSI_RESET);
            logger.fatal(
                    ANSI_RED + "[fatal] > Defects4J - last exit code : " + implemental.last_exit_code + ANSI_RESET);
            System.exit(1);
        }
        if (!implemental.parse()) {
            logger.fatal(ANSI_RED + "[fatal] > Failed to parse defects4j" + ANSI_RESET);
            System.exit(1);
        }
        if (implemental.d4j_ready) {
            cid_set = gitFunctions.blame(implemental.faultyProject, implemental.faultyPath,
                    implemental.faultyLineBlame,
                    implemental.faultyLineFix);

        }
        if (!implemental.cid_config(cid_set[0], cid_set[1])) {
            logger.fatal(ANSI_RED + "[fatal] > Failed to configure commit ids" + ANSI_RESET);
            System.exit(1);
        }

        // STEP 2 : extract git diff from given Defects4J bug informations
        String[] diff = gitFunctions.extract_diff(implemental.faultyProject, implemental.faultyPath,
                implemental.cid_fixed,
                implemental.cid_buggy);
        if (diff == null) {
            logger.fatal(ANSI_RED + "[fatal] > Failed to extract diff" + ANSI_RESET);
            System.exit(1);
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(output_dir, "diff.txt")));
            for (String line : diff) {
                writer.write(line);
                writer.write(" ");
            }
            writer.newLine();
            writer.close();
        } catch (Exception e) {
            logger.fatal(ANSI_RED + "[fatal] > Exception : " + e.getMessage() + ANSI_RESET);
            System.exit(1);
        }
        logger.info(ANSI_GREEN + "[info] > Extracted diff successfully" + ANSI_RESET);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(
                    new File(output_dir, defects4j_name + "_commit_file.csv")));
            for (String line : diff) {
                writer.write(line + ",");
            }
            writer.write(implemental.faultyProject);
            writer.newLine();
            writer.close();
        } catch (Exception e) {
            logger.fatal(ANSI_RED + "[fatal] > Exception : " + e.getMessage() + ANSI_RESET);
            System.exit(1);
        }
        logger.info(ANSI_GREEN + "[info] > Successfully created " + defects4j_name
                + "_commit_file.csv for single file " + file_name + ANSI_RESET);

        // STEP 3 : extract change vector from diff and write it to a file
        String diff_path = output_dir + "/diff.txt";
        if (!extractor.extract_gumtree_log(implemental.faultyProject, diff_path, output_dir)) {
            logger.fatal(ANSI_RED + "[fatal] > Failed to extract gumtree log" + ANSI_RESET);
            System.exit(1);
        }
        logger.info(ANSI_GREEN + "[info] > Successfully extracted gumtree log" + ANSI_RESET);

        String gumtree_log = output_dir + "/gumtree_log.txt";
        int cv_extraction_result = extractor.extract_vector(defects4j_name, gumtree_log, output_dir);
        if (cv_extraction_result == -1) {
            logger.fatal(ANSI_RED + "[fatal] > Failed to extract change vector due to exception" + ANSI_RESET);
            System.exit(1);
        } else if (cv_extraction_result == 1) {
            logger.fatal(ANSI_RED + "[fatal] > Failed to extract change vector due to no change" + ANSI_RESET);
            System.exit(1);
        }
        logger.info(ANSI_GREEN + "[info] > Successfully extracted change vector" + ANSI_RESET);
    }

    public void run(Properties properties) {
	gitFunctions = new GitFunctions();
	extractor = new Extractor();
	implemental = new Implemental();
	    
        initProperties(properties);

        // clean output directory
        if (doClean) {
       	    cleanOutputDir();
        }

        // clone repository
        if (!mode.equals("defects4j") && !gitFunctions.clone(git_url, workspace_dir)) {
            logger.fatal(ANSI_RED + "[fatal] > Failed to clone " + git_url + ANSI_RESET);
            System.exit(1);
        }
        // logger.info(ANSI_GREEN + "[info] > Successfully cloned " + git_url +
        // ANSI_RESET);
        if (git_name == null || git_name.equals("")) {
            git_name = GitFunctions.get_repo_name_from_url(git_url);
        }
        String repo_git = workspace_dir + "/" + git_name;

        logger.trace(ANSI_YELLOW + "[info] > executing ChangeCollector for mode : " + mode + ANSI_RESET);
        // MODE 1 : collect change vector between current commit and before of a single
        // source code file
        if (mode.equals("github")) {
            githubChangeCollect(properties, repo_git);
        }
        // MODE 2 : collect change vector between current commit and before from a
        // Defects4J bug
        else if (mode.equals("defects4j")) {
            defects4jChangeCollect();
        }
        else {
            logger.fatal(ANSI_RED + "[fatal] > Invalid mode" + ANSI_RESET);
            System.exit(1);
        }

        System.exit(0);
    }

    public Properties loadProperties() {
        return loadProperties("../cc.properties");
    }

    public Properties loadProperties(String path) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(path));
        } catch (Exception e) {
            logger.fatal(
                    ANSI_RED + "[fatal] > failed to load properties for launching ChangeCollector due to Exception : "
                            + e.getMessage());
            return null;
        }
        return properties;
    }
}
