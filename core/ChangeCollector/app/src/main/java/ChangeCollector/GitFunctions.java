package ChangeCollector;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;


/**
 * Utility class for Git-related functions.
 */
public class GitFunctions {
    public String name;
    public String project;
    public String projectDirectory;
    public String identifier;

    /**
     * Retrieves the commit ID of the head commit in the specified Git repository.
     *
     * @param path Path of the Git repository.
     * @return The commit ID of the head commit.
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
     * Clones a Git repository from a given URL to a specified path.
     *
     * @param url  Git repository URL.
     * @param path Path to clone the repository.
     * @return True if cloning is successful, false otherwise.
     */
    public boolean clone(String url, String path) {
        String repo_name = get_repo_name_from_url(url);
        App.logger.info(App.ANSI_BLUE + "[status] > cloning " + App.ANSI_YELLOW + url + App.ANSI_RESET + " to "
                + App.ANSI_YELLOW + path
                + App.ANSI_RESET + " as " + App.ANSI_YELLOW + repo_name + App.ANSI_RESET);
        try {
            if (new File(path + "/" + repo_name).exists()) {
                App.logger.info(App.ANSI_YELLOW + "[status] > " + repo_name + App.ANSI_RESET + " already exists");
                return true;
            }
            if (!new File(path).exists()) {
                new File(path).mkdir();
            }
            Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(new java.io.File(path + "/" + repo_name))
                    .setProgressMonitor(new TextProgressMonitor())
                    .call();
            return true;
        } catch (Exception e) {
            App.logger.error(App.ANSI_RED + "[error] > Exception : " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves a list of all commit IDs in the Git repository.
     *
     * @param repo_path Path of the Git repository.
     * @return List of commit IDs.
     */
    private ArrayList<String> log(String repo_path) {
        ArrayList<String> hashes = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(new File(repo_path));
            pb.command("git", "log", "origin", "--pretty=format:%H");
            Process p = pb.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                hashes.add(line);
            }
            p.waitFor();
            p.destroy();
        } catch (Exception e) {
            App.logger.error(App.ANSI_RED + "[error] > Exception : " + e.getMessage() + App.ANSI_RESET);
            return null;
        }
        return hashes;
    }

    /**
     * Retrieves a list of commit IDs where a specific file has been changed.
     *
     * @param repo_path  Path of the Git repository.
     * @param file_name  File to check.
     * @return List of commit IDs.
     */
    public ArrayList<String> log(String repo_path, String file_name) {
        App.logger.trace(App.ANSI_BLUE + "[status] > getting log of " + App.ANSI_YELLOW + repo_path + App.ANSI_RESET
                + " with " + App.ANSI_YELLOW + file_name + App.ANSI_RESET);
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
            App.logger.error(App.ANSI_RED + "[error] > Exception : " + e.getMessage() + App.ANSI_RESET);
            return null;
        }
        return hashes;
    }

    // get list of all file names which ends with .java within a git repository at
    // certain commit hash id
    // @param repo_git : path of git repository
    // @param hash : commit id
    private ArrayList<String> list_tree(String repo_git, String hash) {
        ArrayList<String> files = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.directory(new File(repo_git));
            pb.command("git", "ls-tree", "-r", "--name-only", hash);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = "";
            while ((line = reader.readLine()) != null) {
                if (line.contains(".java")) {
                    files.add(line);
                }
            }
            process.waitFor();
        } catch (Exception e) {
            App.logger.error(App.ANSI_RED + "[error] > Exception : " + e.getMessage() + App.ANSI_RESET);
            return null;
        }
        return files;
    }

    /**
     * Checks out a specific commit and performs blame on a file and line within the project directory.
     *
     * @param project_dir Project directory.
     * @param file        File to be blamed.
     * @param lineBlame   Line number to be blamed.
     * @param lineFix     Line number in the fixed version.
     * @return Array containing old and new commit IDs.
     */
    public String[] blame(String project_dir, String file, int lineBlame, int lineFix) {
        String[] bic_bbic_cid_set = new String[2]; // [0] old cid, [1] new cid
        String bic_cid; // new
        String bbic_cid; // old
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
            bic_cid = str_builder.toString().split(" ")[0].strip(); // new cid
            exit_code = p.waitFor();
            if (exit_code != 0) {
                App.logger.error(
                        App.ANSI_RED + "[ERROR] > Failed to get the commit id of the line " + lineBlame + " in file "
                                + file + App.ANSI_RESET);
                return null;
            }
            ProcessBuilder parse_builder = new ProcessBuilder("git", "-C", project_dir, "rev-parse",
                    String.format("%s~1", bic_cid));
            parse_builder.directory(new File(project_dir));
            p = parse_builder.start();
            reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            str_builder = new StringBuilder();
            for (String l = reader.readLine(); l != null; l = reader.readLine()) {
                str_builder.append(l);
                str_builder.append(System.lineSeparator());
            }
            bbic_cid = str_builder.toString().split(" ")[0].strip(); // old cid
            exit_code = p.waitFor();
            if (exit_code != 0) {
                App.logger.error(
                        App.ANSI_RED + "[ERROR] > Failed to get the commit id of the line " + lineBlame + " in file "
                                + file + App.ANSI_RESET);
                return null;
            }
        } catch (Exception e) {
            App.logger.error(App.ANSI_RED + "[ERROR] > Exception : " + e.getMessage() + App.ANSI_RESET);
            return null;
        }
        bic_bbic_cid_set[0] = bbic_cid;
        bic_bbic_cid_set[1] = bic_cid;
        return bic_bbic_cid_set;
    }

   /**
     * Blames a file and line at a specific commit ID and retrieves the old commit ID.
     *
     * @param project_dir The directory of the project.
     * @param file        The file to be blamed.
     * @param lineBlame   The line to be blamed.
     * @param lineFix     The line after fixing.
     * @param bic         The Fix Inducing Commit ID.
     * @return The old commit ID.
     */
    public String blame(String project_dir, String file, int lineBlame, int lineFix, String bic) {
        String bbic = ""; // old
        int exit_code = -1;
        try {
            App.logger.info(App.ANSI_BLUE + "[status] > checking out " + App.ANSI_YELLOW + project_dir + App.ANSI_BLUE
                    + " to " + App.ANSI_YELLOW + bic + App.ANSI_RESET);
            ProcessBuilder checkout_builder = new ProcessBuilder("git", "checkout", "-f", bic);
            checkout_builder.directory(new File(project_dir));
            Process checkout = checkout_builder.start();
            exit_code = checkout.waitFor();
            if (exit_code != 0) {
                App.logger.error(App.ANSI_RED + "[ERROR] > Failed to checkout " + bic + "with exit code : "
                        + App.ANSI_YELLOW + exit_code + App.ANSI_RESET);
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
                App.logger.error(App.ANSI_RED + "[ERROR] > process exit code : " + exit_code + App.ANSI_RESET);
                App.logger.error(
                        App.ANSI_RED + "[ERROR] > Failed to get the commit id of the line " + lineBlame + " in file "
                                + file + App.ANSI_RESET);
                return null;
            }
        } catch (Exception e) {
            App.logger.error(App.ANSI_RED + "[ERROR] > Exception : " + e.getMessage() + App.ANSI_RESET);
            return null;
        }
        return bbic;
    }

    /**
     * Extracts source code differences between a commit ID and the version before in a specific source file.
     *
     * @param repo_git   The path of the Git repository.
     * @param file_name  The file name to check.
     * @param new_cid    The Fix Inducing Commit ID.
     * @param lineFix    The line after fixing.
     * @param lineBlame  The line to be blamed.
     * @return An array containing the old and new commit IDs, and the file names.
     */
    public String[] extract_diff(String repo_git, String file_name, String new_cid, int lineFix, int lineBlame) {
        String repo_name = get_repo_name_from_url(repo_git);
        String old_cid = null;
        App.logger.trace(App.ANSI_BLUE + "[status] > extracting diff from " + repo_name + App.ANSI_RESET + " to "
                + App.ANSI_BLUE + file_name + App.ANSI_RESET + " with " + App.ANSI_BLUE + new_cid + App.ANSI_RESET);
        try {
            if (isInit(repo_git, new_cid)) {
                App.logger
                        .debug(App.ANSI_PURPLE + "[debug] > bic is initial commit of the repository" + App.ANSI_RESET);
                return null;
            }
            old_cid = blame(repo_git, file_name, lineBlame, lineFix, new_cid);
            if (old_cid == null || old_cid.isEmpty()) {
                App.logger.error(App.ANSI_RED + "[ERROR] > Failed to get old commit id" + App.ANSI_RESET);
                return null;
            } else if (old_cid.equals(new_cid)) {
                ArrayList<String> hashes = log(repo_git, file_name);
                int index = hashes.indexOf(new_cid);
                if (index == -1) {
                    App.logger.error(App.ANSI_RED + "[ERROR] > Failed to find new_cid's index" + App.ANSI_RESET);
                    return null;
                }
                // find old_cid's index
                int old_index = index + 1;
                if (old_index >= hashes.size()) {
                    App.logger.error(App.ANSI_RED + "[ERROR] > Failed to find old_cid's index" + App.ANSI_RESET);
                    return null;
                }
                old_cid = hashes.get(old_index);
            }
        } catch (Exception e) {
            App.logger.error(App.ANSI_RED + e.getMessage() + App.ANSI_RESET);
            return null;
        }
        // if old_cid contains "^" as first character, remove it
        if (old_cid.charAt(0) == '^') {
            old_cid = old_cid.substring(1);
        }
        return extract_diff(repo_git, file_name, new_cid, old_cid);
    }

    /**
     * Extracts source code differences between two commit IDs of a specified source file.
     *
     * @param repo_git   Path of the Git repository.
     * @param file_name  File name to check.
     * @param new_cid    Fix Inducing Commit ID.
     * @param old_cid    Commit ID before Fix Inducing Commit ID.
     * @return Array containing commit IDs and file paths.
     */
    public String[] extract_diff(String repo_git, String file_name, String new_cid, String old_cid) {
        String repo_name = get_repo_name_from_url(repo_git);
        App.logger.trace(App.ANSI_BLUE + "[status] > extracting diff from " + App.ANSI_YELLOW + repo_name
                + App.ANSI_BLUE
                + " between "
                + App.ANSI_YELLOW + old_cid + App.ANSI_BLUE + " and " + App.ANSI_YELLOW + new_cid + App.ANSI_RESET);
        App.logger.trace(
                App.ANSI_BLUE + "[status] > extracting file name : " + App.ANSI_YELLOW + file_name + App.ANSI_RESET);
        String[] result = new String[4];
        try {
            Git git = Git.open(new File(repo_git));
            Repository repository = git.getRepository();
            App.logger.trace(App.ANSI_BLUE + "[status] > repo " + App.ANSI_YELLOW + repo_name + App.ANSI_RESET + " is "
                    + App.ANSI_YELLOW + repository.getDirectory().getAbsolutePath() + App.ANSI_RESET);
            App.logger.trace(App.ANSI_BLUE + "[status] > old cid : " + App.ANSI_YELLOW + old_cid + App.ANSI_RESET);
            App.logger.trace(App.ANSI_BLUE + "[status] > new cid : " + App.ANSI_YELLOW + new_cid + App.ANSI_RESET);

            ObjectReader reader = repository.newObjectReader();

            ObjectId oldHead = repository.resolve(old_cid + "^{tree}");
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            oldTreeIter.reset(reader, oldHead);
            if (oldHead == null) {
                App.logger.error(App.ANSI_RED + "[error] > oldHead is null" + App.ANSI_RESET);
                return null;
            }

            ObjectId newHead = repository.resolve(new_cid + "^{tree}");
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            newTreeIter.reset(reader, newHead);
            if (newHead == null) {
                App.logger.error(App.ANSI_RED + "[error] > newHead is null" + App.ANSI_RESET);
                return null;
            }

            DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
            diffFormatter.setRepository(repository);
            List<DiffEntry> entries = diffFormatter.scan(oldTreeIter, newTreeIter);
            if (entries.size() == 0 || entries.equals(null)) {
                App.logger.error(App.ANSI_RED + "[error] > no diff found" + App.ANSI_RESET);
                return null;
            }
            
            for (DiffEntry entry : entries) {
                String str_new = entry.getNewPath();
                String str_old = entry.getOldPath();
                if (str_new.endsWith(".java") && str_old.endsWith(".java")) {
                    if (file_name.equals("")) {
                        result[0] = new_cid;
                        result[1] = old_cid;
                        result[2] = str_new;
                        result[3] = str_old;
                    } else {
                        if (str_new.contains(file_name)) {
                            result[0] = new_cid;
                            result[1] = old_cid;
                            result[2] = str_new;
                            result[3] = str_old;
                        }
                    }
                }
            }
            for (String entry : result) {
                if (entry == null || entry.equals("")) {
                    App.logger.error(App.ANSI_RED + "[error] > no file found within given commits!" + App.ANSI_RESET);
                    return null;
                }
            }
            diffFormatter.close();
            repository.close();
            git.close();
        } catch (Exception e) {
            App.logger.error(App.ANSI_RED + "[error] > Exception : " + e.getMessage() + App.ANSI_RESET);
            return null;
        }
        return result;
    }

    /**
     * Retrieves the repository name from a given Git repository URL.
     *
     * @param url Git repository URL.
     * @return Repository name.
     */
    public static String get_repo_name_from_url(String url) {
        String[] url_split = url.split("/");
        for (String split : url_split) {
            if (split.contains(".git")) {
                return split.replace(".git", "");
            }
        }
        return url_split[url_split.length - 1];
    }

    /**
     * Checks if a given commit ID is the initial commit of a Git repository.
     *
     * @param repo_path Path of the Git repository.
     * @param cid       Commit ID to check.
     * @return True if the commit is the initial commit, false otherwise.
     */
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
            App.logger.error(App.ANSI_RED + "[error] > Exception : " + e.getMessage() + App.ANSI_RESET);
            return false;
        }
        return false;
    }
}
