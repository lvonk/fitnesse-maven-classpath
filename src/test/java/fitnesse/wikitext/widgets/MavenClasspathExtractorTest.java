package fitnesse.wikitext.widgets;

import org.codehaus.plexus.PlexusContainerException;
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
    public void setUp() throws PlexusContainerException {
        pomFile = new File(MavenClasspathExtractor.class
                .getClassLoader().getResource("MavenClasspathWidget/pom.xml").getFile());

        mavenClasspathExtractor = new MavenClasspathExtractor();
    }

    @Test
    public void extractedClasspathIncludesTestScopeDependencies() throws MavenClasspathExtractionException {
        List<String> classpathEntries = mavenClasspathExtractor.extractClasspathEntries(pomFile);
        StringBuffer sb = new StringBuffer();
        for (String cpEntry : classpathEntries) {
            sb.append(cpEntry);
        }

        String path = sb.toString();

        assertEquals(3, classpathEntries.size());
        assertTrue(path.contains("commons-lang"));
    }

    @Test(expected = MavenClasspathExtractionException.class)
    public void failsOnNonExistingPom() throws MavenClasspathExtractionException {
        mavenClasspathExtractor.extractClasspathEntries(new File("test-pom.xml"));
    }

    
}
