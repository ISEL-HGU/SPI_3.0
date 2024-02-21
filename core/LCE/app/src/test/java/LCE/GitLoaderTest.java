package LCE;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

public class GitLoaderTest {


    @Test
    public void testLoad() {
        GitLoader gitLoader = new GitLoader();
        // Assuming valid configuration and set values
        gitLoader.config("https://github.com/apache/beam.git", "f111e6762a3404c3241564808460789cffcff41c", "91999bdc8e51a51bdd271862e23c6dc33f777bc1", 
                "sdk/src/main/java/com/google/cloud/dataflow/sdk/util/ReduceFnRunner.java", "sdk/src/main/java/com/google/cloud/dataflow/sdk/util/ReduceFnRunner.java",
                "Closure", 14);

        gitLoader.set("/home/newwin0189/SPI_3.0/core/LCE/result", "/home/newwin0189/SPI_3.0/core/LCE/candidates");
        boolean result = gitLoader.load();
        assertTrue(result);
    }

    @Test
    public void testClone() {
        GitLoader gitLoader = new GitLoader();

        // Assuming valid configuration and set values
        gitLoader.config("https://github.com/apache/beam.git", "f111e6762a3404c3241564808460789cffcff41c", "91999bdc8e51a51bdd271862e23c6dc33f777bc1", 
                "sdk/src/main/java/com/google/cloud/dataflow/sdk/util/ReduceFnRunner.java", "sdk/src/main/java/com/google/cloud/dataflow/sdk/util/ReduceFnRunner.java",
                "Closure", 14);

        gitLoader.set("/home/newwin0189/SPI_3.0/core/LCE/result", "/home/newwin0189/SPI_3.0/core/LCE/candidates");
        boolean result = gitLoader.clone("/home/newwin0189/SPI_3.0/core/LCE/result/beam");
        assertTrue(result);
    }

    @Test
    public void testCheckOut() {
        GitLoader gitLoader = new GitLoader();

        // Assuming valid configuration and set values
        gitLoader.config("https://github.com/apache/beam.git", "f111e6762a3404c3241564808460789cffcff41c", "91999bdc8e51a51bdd271862e23c6dc33f777bc1", 
                "sdk/src/main/java/com/google/cloud/dataflow/sdk/util/ReduceFnRunner.java", "sdk/src/main/java/com/google/cloud/dataflow/sdk/util/ReduceFnRunner.java",
                "Closure", 14);

        gitLoader.set("/home/newwin0189/SPI_3.0/core/LCE/result", "/home/newwin0189/SPI_3.0/core/LCE/candidates");
        boolean result = gitLoader.checkout("/home/newwin0189/SPI_3.0/core/LCE/result/beam");
        assertTrue(result);
    }

    @Test
    public void testCopy() {
        
        GitLoader gitLoader = new GitLoader();
        String sourcePath = "/home/newwin0189/SPI_3.0/core/LCE/result/beam/sdk/src/main/java/com/google/cloud/dataflow/sdk/util/ReduceFnRunner.java";
        String destinationPath = "/home/newwin0189/SPI_3.0/core/LCE/candidates/hello.java";
        boolean result = gitLoader.copy(sourcePath, destinationPath);
        assertTrue(result);
    }

    @Test
    public void testCleanCandidateAndResultDir() {
        GitLoader gitLoader = new GitLoader();
        // Assuming valid result and candidate directories
        gitLoader.set("result", "candidates");
        gitLoader.cleanCandidateAndResultDir();
        // Check if the directories are empty
        // (You may need to add more assertions based on your specific directory structure)
        assertTrue(new File("result").list().length == 0);
        assertTrue(new File("candidates").list().length == 0);
    }
    
}
