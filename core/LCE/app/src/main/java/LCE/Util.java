package LCE;

public class Util {

        /**
     * Extracts the repository name from a Git repository URL.
     *
     * @param url The Git repository URL.
     * @return The repository name extracted from the URL.
     */
    public static String getRepoNameFromUrl(String url) {
        String[] url_split = url.split("/");
        for (String split : url_split) {
            if (split.contains(".git")) {
                return split.replace(".git", "");
            }
        }
        return url_split[url_split.length - 1];
    }
    
}
