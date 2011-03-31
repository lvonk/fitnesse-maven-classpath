package fitnesse.wikitext.widgets;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class MavenClasspathExtractorTest {

    private MavenClasspathExtractor mavenClasspathExtractor;
    private File pomFile;

    @Before
    public void setUp() {
        pomFile = new File(MavenClasspathExtractor.class
                .getClassLoader().getResource("MavenClasspathWidget/pom.xml").getFile());

        mavenClasspathExtractor = new MavenClasspathExtractor();
    }

    @Test
    public void extractedClasspathIncludesTestScopeDependencies() {
        List<String> classpathEntries = mavenClasspathExtractor.extractClasspathEntries(pomFile);
        StringBuffer sb = new StringBuffer();
        for (String cpEntry : classpathEntries) {
            sb.append(cpEntry);
        }

        String path = sb.toString();

        assertEquals(4, classpathEntries.size());
        assertTrue(path.contains("commons-lang"));
    }

    @Test(expected = MavenClasspathExtractionException.class)
    public void failsOnNonExistingPom() {
        mavenClasspathExtractor.extractClasspathEntries(new File("test-pom.xml"));
    }

}
