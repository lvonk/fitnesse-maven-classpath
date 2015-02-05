package fitnesse.wikitext.widgets;

import fitnesse.wiki.PageData;
import fitnesse.wiki.WikiPage;
import fitnesse.wiki.fs.InMemoryPage;
import fitnesse.wikitext.parser.*;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MavenClasspathSymbolTypeTest {

    private MavenClasspathSymbolType mavenClasspathSymbolType;
    private MavenClasspathExtractor mavenClasspathExtractor;
    private Symbol symbol;
    private WikiPage wikiPage;

    @Before
    public void setUp() throws Exception {
        System.clearProperty(MavenClasspathSymbolType.DISABLE_KEY);
        symbol = mock(Symbol.class);
        wikiPage = InMemoryPage.makeRoot("RooT");
        mavenClasspathExtractor = mock(MavenClasspathExtractor.class);

        mavenClasspathSymbolType = new MavenClasspathSymbolType();
        mavenClasspathSymbolType.setMavenClasspathExtractor(mavenClasspathExtractor);
    }

    @Test
    public void translatesToClasspathEntries() throws MavenClasspathExtractionException {
        Symbol child = mock(Symbol.class);
      Translator translator = mock(Translator.class);

        when(symbol.childAt(0)).thenReturn(child);
        when(translator.translate(child)).thenReturn("thePomFile");

        when(mavenClasspathExtractor.extractClasspathEntries(any(File.class), isA(String.class)))
                .thenReturn(Arrays.asList("test1", "test2"));

        assertEquals("<p class='meta'>Maven classpath [file: thePomFile, scope: test]:</p><ul class='meta'><li>test1</li><li>test2</li></ul>"
                , mavenClasspathSymbolType.toTarget(translator, symbol));
    }

    @Test
    public void translatesToJavaClasspath() throws MavenClasspathExtractionException {
        Symbol child = mock(Symbol.class);
        Translator translator = mock(Translator.class);

        when(symbol.childAt(0)).thenReturn(child);
        when(translator.translate(child)).thenReturn("thePomFile");

        when(mavenClasspathExtractor.extractClasspathEntries(any(File.class), isA(String.class)))
                .thenReturn(Arrays.asList("test1", "test2"));

        assertArrayEquals(new Object[]{"test1", "test2"}, mavenClasspathSymbolType.providePaths(translator, symbol).toArray());
    }

    @Test
    public void loadPomXml() throws Exception {
        configureMavenClasspathSymbolType();
        PageData pageData = wikiPage.getData();
        pageData.setContent("!pomFile pom.xml\n");
        wikiPage.commit(pageData);
        String html = wikiPage.getHtml();
        assertTrue(html, html.startsWith("<p class='meta'>Maven classpath [file: pom.xml, scope: test]:</p><ul class='meta'><li>"));
    }

    @Test
    public void loadPomXmlFromVariable() throws Exception {
        configureMavenClasspathSymbolType();
        PageData pageData = wikiPage.getData();
        pageData.setContent("!define POM_XML {pom.xml}\n" +
                "!pomFile ${POM_XML}\n");
        wikiPage.commit(pageData);
        String html = wikiPage.getHtml();
        assertTrue(html, html.contains("<p class='meta'>Maven classpath [file: pom.xml, scope: test]:</p><ul class='meta'><li>"));
    }

    @Test
    public void canBeDisabled() throws Exception {
        System.setProperty(MavenClasspathSymbolType.DISABLE_KEY, "TRUE");
        mavenClasspathSymbolType = new MavenClasspathSymbolType();

        Symbol child = mock(Symbol.class);
        Translator translator = mock(Translator.class);

        when(symbol.childAt(0)).thenReturn(child);
        when(translator.translate(child)).thenReturn("thePomFile");

        assertEquals("<p class='meta'>Maven classpath [file: thePomFile, scope: test]:</p><ul class='meta'></ul>"
                , mavenClasspathSymbolType.toTarget(translator, symbol));
    }

    private void configureMavenClasspathSymbolType() throws Exception {
        SymbolProvider.wikiParsingProvider.add(new MavenClasspathSymbolType());
    }
}
