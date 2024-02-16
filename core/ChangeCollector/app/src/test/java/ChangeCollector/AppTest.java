package ChangeCollector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

class AppTest {

    private App app;
    private String testOutputDir = "test_output";
    private String testWorkspaceDir = "test_workspace";

    @BeforeEach
    void setUp() {
        app = new App();
        createTestOutputDir();
    }

    @Test
    void testInitProperties() {
        Properties properties = new Properties();
        properties.setProperty("project_root", "/path/to/project");
        properties.setProperty("file_name", "test.java");
        properties.setProperty("commit_id", "abc123");
        properties.setProperty("git_name", "my_repo");
        properties.setProperty("git_url", "https://github.com/myusername/my_repo.git");
        properties.setProperty("output_dir", "/path/to/output");
        properties.setProperty("doClean", "true");
        properties.setProperty("mode", "repository");
        properties.setProperty("JAVA_HOME.8", "/path/to/jdk8");
        properties.setProperty("defects4j_name", "my_defects4j_bug");
        properties.setProperty("defects4j_id", "123");
        properties.setProperty("hash_id", "456");

        app.initProperties(properties);

        assertEquals("/path/to/project", app.getProjectRoot());
        assertEquals("test.java", app.getFileName());
        assertEquals("abc123", app.getCommitId());
        assertEquals("my_repo", app.getGitName());
        assertEquals("https://github.com/myusername/my_repo.git", app.getGitUrl());
        assertTrue(app.isDoClean());
        assertEquals("repository", app.getMode());
        assertEquals("/path/to/jdk8", app.getJavaHome8());
        assertEquals("my_defects4j_bug", app.getDefects4jName());
        assertEquals("123", app.getDefects4jId());
        assertEquals("456", app.getHashId());
        assertEquals(String.format("/path/to/output/%s/%s", app.getHashId(), "outputs/ChangeCollector"), app.getOutputDir());
        assertEquals(String.format("%s/%s", app.getOutputDir(), app.getHashId()), app.getWorkspaceDir());
    }

    @Test
    void testCleanOutputDir() {
        app.setWorkspaceDir(testWorkspaceDir);
        app.setOutputDir(testOutputDir);

        File commitFile = new File(testOutputDir, "commit_file.csv");
        File gumtreeVector = new File(testOutputDir, "gumtree_vector.csv");

        app.cleanOutputDir();

        assertFalse(new File(testWorkspaceDir).exists());
        assertFalse(new File("commit_file.csv").exists());
        assertFalse(new File("gumtree_vector.csv").exists());
    }

    @Test
    void testLoadPropertiesFromFile() {
        Properties properties = app.loadProperties("test_properties/test_cc.properties");

        assertNotNull(properties);
        assertEquals("/test/project/root", properties.getProperty("project_root"));
        assertEquals("defects4j", properties.getProperty("mode"));
        assertEquals("src/main/java/edu/handong/csee/isel/Custom.java", properties.getProperty("file_name"));
        assertEquals("8c6ec00e753c9090fb2baa4fa59f76bb05d033ae", properties.getProperty("commit_id"));
        assertEquals("https://github.com/ISEL-HGU/SPI_3.0", properties.getProperty("git_url"));
        assertEquals("23", properties.getProperty("lineFix"));
        assertEquals("23", properties.getProperty("lineBlame"));
        assertEquals("SPI_3.0", properties.getProperty("git_name"));
        assertEquals("/test/project/output_dir", properties.getProperty("output_dir"));
        assertTrue(Boolean.parseBoolean(properties.getProperty("doClean")));
        assertEquals("/usr/lib/jvm/java-1.8.0-openjdk-amd64", properties.getProperty("JAVA_HOME.8"));
        assertEquals("Closure", properties.getProperty("defects4j_name"));
        assertEquals("14", properties.getProperty("defects4j_id"));
        assertEquals("abc123", properties.getProperty("hash_id"));
        assertEquals("/test/project/BBIC_BIC.csv", properties.getProperty("set_file_path"));
    }

    // Add more tests for other methods as needed

    private void createTestOutputDir() {
        File outputDir = new File(testOutputDir);
        if (!outputDir.exists()) {
            assertTrue(outputDir.mkdirs());
        }
    }
}
