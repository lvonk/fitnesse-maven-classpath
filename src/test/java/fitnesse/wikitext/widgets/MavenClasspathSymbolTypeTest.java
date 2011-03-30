package fitnesse.wikitext.widgets;

import org.codehaus.plexus.util.FileUtils;
import org.junit.After;
import org.junit.Before;

import java.io.File;

public class MavenClasspathSymbolTypeTest {
    private static final String TEST_PROJECT_ROOT = new File(
            "src/test/resources/MavenClasspathWidget").getAbsolutePath();
    private final static String TEST_POM_FILE = "src/test/resources/MavenClasspathWidget/pom.xml";
    private File mavenLocalRepo = new File(System.getProperty("java.io.tmpdir"), "MavenClasspathWidgetTest/m2/repo");
    private String path = mavenLocalRepo.getAbsolutePath();


    @Before
    public void setUp() throws Exception {
        mavenLocalRepo.mkdirs();
        FileUtils.copyDirectoryStructure(new File(TEST_PROJECT_ROOT, "repository"), mavenLocalRepo);
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(mavenLocalRepo);
    }


}
