package fitnesse.wikitext.widgets;

import fitnesse.wiki.PageData;
import hudson.maven.MavenRequest;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import static fitnesse.html.HtmlUtil.BRtag;
import static fitnesse.html.HtmlUtil.metaText;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class MavenClasspathWidgetTest extends WidgetTestCase {
    private static final String TEST_PROJECT_ROOT = new File(
            "src/test/resources/MavenClasspathWidget").getAbsolutePath();
    private final static String TEST_POM_FILE = "src/test/resources/MavenClasspathWidget/pom.xml";
    private File mavenLocalRepo = new File(System.getProperty("java.io.tmpdir"), "MavenClasspathWidgetTest/m2/repo");
    private String path = mavenLocalRepo.getAbsolutePath();

    private MavenClasspathWidget widget;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        widget = new MavenClasspathWidget(new MockWidgetRoot(), "!pomFile " + TEST_POM_FILE) {
            @Override
            protected String getLocalRepository(String localRepository) {
                return mavenLocalRepo.getAbsolutePath();
            }

        };
        mavenLocalRepo.mkdirs();
        FileUtils.copyDirectoryStructure(new File(TEST_PROJECT_ROOT, "repository"),
                mavenLocalRepo);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        FileUtils.deleteDirectory(mavenLocalRepo);
    }

    @Override
    protected String getRegexp() {
        return MavenClasspathWidget.REGEXP;
    }

    public void testThatMavenClasspathWidgetIsAddedToTheClasspathWidgetBuilder()
            throws Exception {
        assertEquals(MavenClasspathWidget.class, PageData.classpathWidgetBuilder
                .findWidgetClassMatching("!pomFile pom.xml"));
    }

    public void testRegexp() throws Exception {
        assertMatchEquals("!pomFile pom.xml", "!pomFile pom.xml");
    }

    public void testAsWikiText() throws Exception {
        assertEquals("!pomFile " + TEST_POM_FILE, widget.asWikiText());
    }

    public void testRender() throws Exception {
        String actual = widget.render();
        String expected = metaText("classpath: " + TEST_PROJECT_ROOT + "/target/test-classes")
                + BRtag
                + metaText("classpath: " + TEST_PROJECT_ROOT + "/target/classes")
                + BRtag
                + metaText(classpathElementForRender("/fitnesse/fitnesse-dep/1.0/fitnesse-dep-1.0.jar"))
                + BRtag
                + metaText(classpathElementForRender("/fitnesse/fitnesse-subdep/1.0/fitnesse-subdep-1.0.jar"))
                + BRtag;
        assertEquals(expected, actual);
    }

    public void testGetText() throws Exception {
        String actual = widget.getText();
        String expected = TEST_PROJECT_ROOT + "/target/test-classes"
                + ":"
                + TEST_PROJECT_ROOT + "/target/classes"
                + classpathElementForText("/fitnesse/fitnesse-dep/1.0/fitnesse-dep-1.0.jar")
                + classpathElementForText("/fitnesse/fitnesse-subdep/1.0/fitnesse-subdep-1.0.jar");
        assertEquals(expected, actual);
    }

    public void testFailFastWhenPomFileDoesNotExist() throws Exception {
        try {
            new MavenClasspathWidget(new MockWidgetRoot(),
                    "!pomFile /non/existing/pom.xml");
            fail("should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testFailFastWhenPomFileIsNotSpecified() throws Exception {
        try {
            new MavenClasspathWidget(new MockWidgetRoot(), "!pomFile");
            fail("should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testShouldReplaceVariablesInPath() throws Exception {
        System.setProperty("MY_PATH", "src/test/resources");
        widget = new MavenClasspathWidget(new MockWidgetRoot(),
                "!pomFile ${MY_PATH}/MavenClasspathWidget/pom.xml") {
            @Override
            protected String getLocalRepository(String localRepository) {
                return mavenLocalRepo.getAbsolutePath();
            }
        };
        String html = widget.childHtml();
        assertEquals(TEST_POM_FILE, html);
    }

    public void testShouldSupportLocalMavenRepositoryFromUserSettings() throws Exception {
        File userSettingsFile = getSettingsFile("/user-settings.xml");
        widget = new MavenClasspathWidget(new MockWidgetRoot(), "!pomFile " + TEST_POM_FILE);
        widget.setMavenUserSettingsFile(userSettingsFile);
        widget.setMavenGlobalSettingsFile(null);
        assertThat(widget.getLocalRepository(widget.mavenConfiguration().getLocalRepositoryPath()), is("/tmp/fitnesse-maven-classpath/user-repository"));
    }

    public void testShouldSupportLocalMavenRepositoryFromGlobalSettings() throws Exception {
        File globalSettingsFile = getSettingsFile("/global-settings.xml");
        widget = new MavenClasspathWidget(new MockWidgetRoot(), "!pomFile " + TEST_POM_FILE);
        widget.setMavenUserSettingsFile(null);
        widget.setMavenGlobalSettingsFile(globalSettingsFile);
        assertThat(widget.getLocalRepository(widget.mavenConfiguration().getLocalRepositoryPath()), is("/tmp/fitnesse-maven-classpath/global-repository"));
    }

    public void testShouldSupportPreferLocalRepoFromUserSettingsAboveGlobalSettings() throws Exception {
        File globalSettingsFile = getSettingsFile("/global-settings.xml");
        File userSettingsFile = getSettingsFile("/user-settings.xml");
        widget = new MavenClasspathWidget(new MockWidgetRoot(), "!pomFile " + TEST_POM_FILE);
        widget.setMavenUserSettingsFile(userSettingsFile);
        widget.setMavenGlobalSettingsFile(globalSettingsFile);
        assertThat(widget.getLocalRepository(widget.mavenConfiguration().getLocalRepositoryPath()), is("/tmp/fitnesse-maven-classpath/user-repository"));
    }

    public void testShouldFallBackToDefaultRepositoryLocationWhenNotConfigured() throws Exception {
        URI uri = this.getClass().getResource("/settings-without-local-repo.xml").toURI();
        File settingsFile = new File(uri);
        widget = new MavenClasspathWidget(new MockWidgetRoot(), "!pomFile " + TEST_POM_FILE);
        widget.setMavenUserSettingsFile(settingsFile);
        assertThat(widget.getLocalRepository(DependencyResolvingMavenEmbedder.DEFAULT_LOCAL_REPO_ID), is(DependencyResolvingMavenEmbedder.DEFAULT_LOCAL_REPO_ID));
    }

    public void testShouldFallBackToDefaultRepositoryLocationWhenBothSettingsFilesDoNotExist() throws Exception {
        File globalSettingsFile = new File("/non-existing-global-settings.xml");
        File userSettingsFile = new File("/non-existing-user-settings.xml");
        widget = new MavenClasspathWidget(new MockWidgetRoot(), "!pomFile " + TEST_POM_FILE);
        widget.setMavenUserSettingsFile(userSettingsFile);
        widget.setMavenGlobalSettingsFile(globalSettingsFile);

        assertThat(widget.getLocalRepository(DependencyResolvingMavenEmbedder.DEFAULT_LOCAL_REPO_ID), is(DependencyResolvingMavenEmbedder.DEFAULT_LOCAL_REPO_ID));
    }

    public void testShouldIgnoreNonExistingSettingsFiles() throws Exception {
        File globalSettingsFile = new File("/non-existing-global-settings.xml");
        File userSettingsFile = new File("/non-existing-user-settings.xml");
        widget = new MavenClasspathWidget(new MockWidgetRoot(), "!pomFile " + TEST_POM_FILE);
        widget.setMavenUserSettingsFile(userSettingsFile);
        widget.setMavenGlobalSettingsFile(globalSettingsFile);

        MavenRequest mavenConfiguration = widget.mavenConfiguration();
        assertThat(mavenConfiguration.getUserSettingsFile(), is(nullValue()));
        assertThat(mavenConfiguration.getGlobalSettingsFile(), is(nullValue()));
    }

    private File getSettingsFile(String path) throws URISyntaxException {
        URI settingsFileUri = this.getClass().getResource(path).toURI();
        return new File(settingsFileUri);
    }

    private String classpathElementForRender(String file) {
        return "classpath: " + path + file;
    }

    private String classpathElementForText(String file) {
        return File.pathSeparator + path + file;
    }
}
