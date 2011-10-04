package fitnesse.wikitext.widgets;

import fitnesse.wikitext.parser.Parser;
import fitnesse.wikitext.parser.Symbol;
import fitnesse.wikitext.parser.SymbolType;

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
    public void canParseAProperDirective() {
        when(parser.moveNext(1))
                .thenReturn(new Symbol(SymbolType.Whitespace))
                .thenReturn(new Symbol(SymbolType.Text, "thePomFile"));

        Maybe<Symbol> result = mavenClasspathSymbolType.parse(symbol, parser);
        assertNotNull(result);
        assertNotSame(Symbol.nothing, result);

        verify(symbol).add("thePomFile");
    }

    @Test
    public void translatesToClasspathEntries() {
        Symbol child = mock(Symbol.class);

        when(symbol.childAt(0)).thenReturn(child);
        when(child.getContent()).thenReturn("thePomFile");

        when(mavenClasspathExtractor.extractClasspathEntries(any(File.class), isA(String.class)))
                .thenReturn(Arrays.asList("test1", "test2"));

        assertEquals("<span class=\"meta\">classpath: test1</span><br/><span class=\"meta\">classpath: test2</span><br/>"
                , mavenClasspathSymbolType.toTarget(null, symbol));
    }

    @Test
    public void translatesToJavaClasspath() {
        Symbol child = mock(Symbol.class);

        when(symbol.childAt(0)).thenReturn(child);
        when(child.getContent()).thenReturn("thePomFile");

        when(mavenClasspathExtractor.extractClasspathEntries(any(File.class), isA(String.class)))
                .thenReturn(Arrays.asList("test1", "test2"));

        assertArrayEquals(new Object[] { "test1", "test2" }, mavenClasspathSymbolType.providePaths(null, symbol).toArray());
    }


}
