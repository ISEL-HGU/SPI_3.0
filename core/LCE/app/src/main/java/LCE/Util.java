package LCE;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.io.File;

public class Util {

    public static boolean createDirectory(String directoryPath) {
        File directory = new File(directoryPath);

        if (!directory.exists()) {
            return directory.mkdir();
        } else {
            System.out.println("Directory already exists.");
            return false;
        }
    }

    /**
     * Extracts the file name from a given file path.
     *
     * @param filePath The file path.
     * @return The file name extracted from the path.
     */
    public static String getFileNameFromPath(String pathPath) {
        String[] splittedPatchPath = pathPath.split("/");
        return splittedPatchPath[splittedPatchPath.length - 1];
    }

    public static double getCosineSimilarity(String str1, String str2) {
        Map<String, Integer> vector1 = buildVector(str1);
        Map<String, Integer> vector2 = buildVector(str2);

        double dotProduct = dotProduct(vector1, vector2);
        double magnitude1 = calculateMagnitude(vector1);
        double magnitude2 = calculateMagnitude(vector2);

        return dotProduct / (magnitude1 * magnitude2);
    }

    public static String readFile(String filePath) {
        StringBuilder fileContents = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                fileContents.append(line);
                fileContents.append(System.lineSeparator());
            }
        } catch (IOException e) {
            return null;
        }
        return fileContents.toString();
    }

    private static Map<String, Integer> buildVector(String str) {
        Map<String, Integer> vector = new HashMap<>();
        String[] tokens = str.toLowerCase().split("\\s+");

        for (String token : tokens) {
            vector.put(token, vector.getOrDefault(token, 0) + 1);
        }

        return vector;
    }

    private static double dotProduct(Map<String, Integer> vector1, Map<String, Integer> vector2) {
        double dotProduct = 0;

        for (Map.Entry<String, Integer> entry : vector1.entrySet()) {
            String key = entry.getKey();
            int value1 = entry.getValue();
            int value2 = vector2.getOrDefault(key, 0);

            dotProduct += value1 * value2;
        }

        return dotProduct;
    }

    private static double calculateMagnitude(Map<String, Integer> vector) {
        double magnitudeSquared = 0;

        for (int value : vector.values()) {
            magnitudeSquared += Math.pow(value, 2);
        }

        return Math.sqrt(magnitudeSquared);
    }
    
}
