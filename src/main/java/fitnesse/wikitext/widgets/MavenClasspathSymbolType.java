package fitnesse.wikitext.widgets;

import fitnesse.html.HtmlUtil;
import fitnesse.wikitext.parser.*;
import util.Maybe;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.codehaus.plexus.PlexusContainerException;

/**
 * FitNesse SymbolType implementation which enables Maven classpath integration for FitNesse.
 */
public class MavenClasspathSymbolType extends SymbolType implements Rule, Translation, PathsProvider {

    private MavenClasspathExtractor mavenClasspathExtractor;

    public MavenClasspathSymbolType() throws PlexusContainerException {
        super("MavenClasspathSymbolType");
        this.mavenClasspathExtractor = new MavenClasspathExtractor();

        wikiMatcher(new Matcher().startLineOrCell().string("!pomFile"));

        wikiRule(this);
        htmlTranslation(this);
    }

    @Override
    public String toTarget(Translator translator, Symbol symbol) {
        List<String> classpathElements = getClasspathElements(symbol);

        String classpathForRender = "";
        for (String element : classpathElements) {
            classpathForRender += HtmlUtil.metaText("classpath: " + element) + HtmlUtil.BRtag;

        }
        return classpathForRender;

    }

	private List<String> getClasspathElements(Symbol symbol) {
        String pomFile = symbol.childAt(0).getContent();
		String scope = MavenClasspathExtractor.DEFAULT_SCOPE;

		if (pomFile.contains("@")) {
        	String[] s = pomFile.split("@");
        	pomFile = s[0];
        	scope = s[1];
        }

		return mavenClasspathExtractor.extractClasspathEntries(new File(pomFile), scope);
	}

    @Override
    public Maybe<Symbol> parse(Symbol symbol, Parser parser) {
        Symbol next = parser.moveNext(1);

        if (!next.isType(SymbolType.Whitespace)) return Symbol.nothing;

        symbol.add(parser.moveNext(1).getContent());


        return new Maybe<Symbol>(symbol);
    }

    @Override
    public boolean matchesFor(SymbolType symbolType) {
        return symbolType instanceof Path || super.matchesFor(symbolType);
    }
    

    /**
     * Exposed for testing
     */
    protected void setMavenClasspathExtractor(MavenClasspathExtractor mavenClasspathExtractor) {
        this.mavenClasspathExtractor = mavenClasspathExtractor;
    }

	@Override
	public Collection<String> providePaths(Translator translator, Symbol symbol) {
		return getClasspathElements(symbol);
	}
}


