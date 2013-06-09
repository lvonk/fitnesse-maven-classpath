package fitnesse.wikitext.widgets;

import fitnesse.html.HtmlElement;
import fitnesse.wiki.WikiPage;
import fitnesse.wikitext.parser.*;

import fitnesse.wikitext.test.ParserTestHelper;
import fitnesse.wikitext.test.TestRoot;
import org.codehaus.plexus.PlexusContainerException;
import org.junit.Before;
import org.junit.Test;
import util.Maybe;

import java.io.File;
import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MavenClasspathSymbolTypeTest {

    private MavenClasspathSymbolType mavenClasspathSymbolType;
    private MavenClasspathExtractor mavenClasspathExtractor;
    private Symbol symbol;
    private Parser parser;

    @Before
    public void setUp() throws Exception {
        symbol = mock(Symbol.class);
        parser = mock(Parser.class);
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

        assertEquals("<span class=\"meta\">classpath: test1</span><br/><span class=\"meta\">classpath: test2</span><br/>"
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
        String pageContents = "!pomFile pom.xml\n";
        VariableSource variableSource = mock(VariableSource.class);
        String html = ParserTestHelper.translateToHtml(null, pageContents, variableSource);
        assertTrue(html, html.startsWith("<span class=\"meta\">classpath: "));
    }

    @Test
    public void loadPomXmlFromVariable() throws Exception {
        configureMavenClasspathSymbolType();
        String pageContents = "!pomFile ${POM_XML}\n";
        VariableSource variableSource = mock(VariableSource.class);
        when(variableSource.findVariable("POM_XML")).thenReturn(new Maybe<String>("pom.xml"));
        String html = ParserTestHelper.translateToHtml(null, pageContents, variableSource);
        assertTrue(html, html.startsWith("<span class=\"meta\">classpath: "));
    }

    private void configureMavenClasspathSymbolType() throws Exception {
        SymbolProvider.wikiParsingProvider.add(new MavenClasspathSymbolType());
    }
}
