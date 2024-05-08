package ChangeCollector;

import org.apache.commons.cli.*;
import java.io.*;
import java.util.*;
import org.apache.logging.log4j.Logger;
import com.opencsv.*;
import java.nio.file.FileSystems;

/**
 * The Implemental class represents the implementation of a Defects4J bug. It provides
 * functionality to configure, preprocess, fetch source code, and parse information for a
 * given Defects4J bug.
 */
public class Implemental {
    // log4j2 logger
    public Logger logger = App.logger;
    // Defects4J bug
    public String name = null; // Defects4J Bug Name
    public int identifier = -1; // Defects4J Bug Identifier
    // directory paths
    public String project_root;
    public String target;
    public String result_dir;
    public String jdk8_dir;
    public String workspace_dir;
    // hash id of the current execution
    public String hash_id;
    // if configured or not
    public boolean config_ready = false;
    // Defects4J bug information
    public boolean bench_ready = false;
    public String faultyProject;
    public String faultyPath;
    public Integer faultyLineBlame;
    public Integer faultyLineFix;
    // Defects4J bug commit ids
    public String cid_fixed;
    public String cid_buggy;
    // last exit code of the executed command
    public int last_exit_code = -1;

    // constructor
    public Implemental() {
        super();
    }

    /**
     * Configures the variables to collect the change vector of a Defects4J bug.
     *
     * @param project_root The root directory of the project.
     * @param name The Defects4J bug name.
     * @param identifier The Defects4J bug identifier.
     * @param result_dir The result directory.
     * @param jdk8_dir The JDK8 directory.
     * @param hash_id The hash ID of the current execution.
     * @return True if configuration is successful, false otherwise.
     */
    public boolean config(String project_root, String name, int identifier, String result_dir, String jdk8_dir,
            String hash_id) {
        this.project_root = project_root;
        this.name = name;
        this.identifier = identifier;
        this.target = String.format("%s/%s", this.project_root, "target");
        this.result_dir = result_dir;
        this.jdk8_dir = jdk8_dir;
        this.hash_id = hash_id;
        config_ready = true;
        return config_ready;
    }

    /**
     * Sets the commit IDs for the Defects4J bug.
     *
     * @param cid_fixed The commit ID of the fixed version.
     * @param cid_buggy The commit ID of the buggy version.
     * @return True if commit IDs are set successfully, false otherwise.
     */
    public boolean cid_config(String cid_fixed, String cid_buggy) {
        this.cid_fixed = cid_fixed;
        this.cid_buggy = cid_buggy;
        return true;
    }

    /**
     * Preprocesses the configuration by creating necessary directories.
     *
     * @return True if preprocessing is successful, false otherwise.
     */
    public boolean preprocess() {
        if (config_ready) {
            try {
                workspace_dir = String.format("%s/%s", result_dir, hash_id);
                File workspace = new File(workspace_dir);
                if (!workspace.exists()) {
                    if (!workspace.mkdirs()) {
                        // failed to create workspace directory
                        logger.error(App.ANSI_RED + "[error] > Failed to create workspace directory " + App.ANSI_RESET);
                        return false;
                    }
                }
                String output_dir = String.format("%s/outputs/ChangeCollector", workspace_dir);
                File output_path = new File(output_dir);
                if (!output_path.exists()) {
                    if (!output_path.mkdirs()) {
                        // failed to create output directory
                        logger.error(App.ANSI_RED + "[error] > Failed to create output directory " + App.ANSI_RESET);
                        return false;
                    }
                }
            } catch (Exception e) {
                logger.error(App.ANSI_RED + "[error] > Exception : " + e.getMessage() + App.ANSI_RESET);
                return false;
            }
            return true;
        }
        logger.error(App.ANSI_RED + "[error] > Not configured yet" + App.ANSI_RESET);
        return false;
    }

    /**
     * Fetches the current source code of the Defects4J bug in a buggy version.
     *
     * @return True if fetching is successful, false otherwise.
     */
    public boolean fetch() {
        String project_dir = String.format("%s/%s", workspace_dir, name);
        if (config_ready) {
            try {
                ProcessBuilder pb = new ProcessBuilder("defects4j", "checkout", "-p", name, "-v",
                        String.format("%db", identifier),
                        "-w", project_dir);
                Process p = pb.start();
                last_exit_code = p.waitFor();
            } catch (Exception e) {
                logger.error(App.ANSI_RED + "[error] > Exception : " + e.getMessage() + App.ANSI_RESET);
                return false;
            }
        }
        return last_exit_code == 0;
    }

    public boolean parseBugsInfo(String repository_path) {
        String project_dir = String.format("%s/%s", workspace_dir, name);
        String info = String.format("%s/components/commit_collector/VJBench_bugs_info/%s.csv", project_root,
                name);
        try {
            CSVReader reader = new CSVReader(new FileReader(info));
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
		        logger.info(nextLine[0]);
                if (nextLine[0].startsWith("VJBench"))
                    continue;
                if (Integer.parseInt(nextLine[0]) == identifier) {
                    faultyProject = String.format("%s/%s", repository_path, name);
                    faultyPath = nextLine[1];
                    faultyLineBlame = Integer.parseInt(nextLine[2]);
                    faultyLineFix = Integer.parseInt(nextLine[3]);
                    bench_ready = true;
                    break;
                }
            }
        } catch (Exception e) {
            logger.error(App.ANSI_RED + "[error] > Exception : " + e.getMessage() + App.ANSI_RESET);
            return false;
        }

	    return bench_ready;
    }

    /**
     * Parses the information of the given Defects4J bug with the provided name and identifier.
     *
     * @return True if parsing is successful, false otherwise.
     */
    public boolean parse() {
        String project_dir = String.format("%s/%s", workspace_dir, name);
        String info = String.format("%s/components/commit_collector/Defects4J_bugs_info/%s.csv", project_root,
                name);
        try {
            CSVReader reader = new CSVReader(new FileReader(info));
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                if (nextLine[0].startsWith("Defects4J"))
                    continue;
                if (Integer.parseInt(nextLine[0]) == identifier) {
                    faultyProject = project_dir;
                    faultyPath = nextLine[1];
                    faultyLineBlame = Integer.parseInt(nextLine[2]);
                    faultyLineFix = Integer.parseInt(nextLine[3]);
                    bench_ready = true;
                    break;
                }
            }
        } catch (Exception e) {
            logger.error(App.ANSI_RED + "[error] > Exception : " + e.getMessage() + App.ANSI_RESET);
            return false;
        }
        return bench_ready;
    }
}
