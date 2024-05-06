package LCE;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UtilTest {


    @Test
    public void testGetCosineSimilarity() {

    // Mocked values for test purposes
        String str1 = "This is a sample string for testing";
        String str2 = "This is a sample string for testing";
        // Prepare some dummy data
        double expectedCosineSimilarity = 1.0; // Expected similarity value based on the provided strings

        // Invoke the method under test
        double actualCosineSimilarity = Util.getCosineSimilarity(str1, str2);

        // Assert the result
        assertEquals(expectedCosineSimilarity, actualCosineSimilarity, 0.0001);

        str2 = "This is a sample string for testing haha";

        expectedCosineSimilarity = 0.94;

        actualCosineSimilarity = Util.getCosineSimilarity(str1, str2);

        assertEquals(expectedCosineSimilarity, actualCosineSimilarity, 0.1);

    }
    
}
