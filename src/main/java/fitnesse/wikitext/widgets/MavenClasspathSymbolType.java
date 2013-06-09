package fitnesse.wikitext.widgets;

import fitnesse.html.HtmlUtil;
import fitnesse.wikitext.parser.*;
import org.codehaus.plexus.PlexusContainerException;
import util.Maybe;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * FitNesse SymbolType implementation. Enables Maven classpath integration for FitNesse.
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
        List<String> classpathElements = null;
        try {
            classpathElements = getClasspathElements(translator, symbol);
        } catch (MavenClasspathExtractionException e) {
            return HtmlUtil.metaText("Unable to parse POM file: " + e.getMessage()) + HtmlUtil.BRtag;
        }

        String classpathForRender = "";
        for (String element : classpathElements) {
            classpathForRender += HtmlUtil.metaText("classpath: " + element) + HtmlUtil.BRtag;

        }
        return classpathForRender;

    }

	private List<String> getClasspathElements(Translator translator, Symbol symbol) throws MavenClasspathExtractionException {
		ParsedSymbol parsedSymbol = new ParsedSymbol(translator.translate(symbol.childAt(0)));

		return mavenClasspathExtractor.extractClasspathEntries(parsedSymbol.getPomFile(), parsedSymbol.getScope());
	}


    @Override
    public Maybe<Symbol> parse(Symbol current, Parser parser) {
        if (!parser.isMoveNext(SymbolType.Whitespace)) return Symbol.nothing;

        return new Maybe<Symbol>(current.add(parser.parseToEnds(0, SymbolProvider.pathRuleProvider, new SymbolType[] {SymbolType.Newline})));
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
        try {
            return getClasspathElements(translator, symbol);
        } catch (MavenClasspathExtractionException e) {
            return Collections.EMPTY_LIST;
        }
    }
	
	/**
	 * Turn the pom+scope key into a comparable object, using the pom's last modified timestamp as
	 * cache key.
	 */
	static class ParsedSymbol {
		private String symbol;
		private File pomFile;
		private String scope;
		private long lastModified;
		
		public ParsedSymbol(String symbol) {
			super();
			this.symbol = symbol;
			parseSymbol();
		}
		
		private void parseSymbol() {
			if (symbol.contains("@")) {
	        	String[] s = symbol.split("@");
	        	pomFile = new File(s[0]);
	        	scope = s[1];
	        } else {
	        	pomFile = new File(symbol);
	        	scope = MavenClasspathExtractor.DEFAULT_SCOPE;
	        }

			lastModified = pomFile.lastModified();
		}
		
		public File getPomFile() {
			return pomFile;
		}
		
		public String getScope() {
			return scope;
		}

		/* hashCode() and equals() are optimized for used in the cache */
		
		@Override
		public int hashCode() {
			return symbol.hashCode() + (int) lastModified;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ParsedSymbol) {
				ParsedSymbol ps = (ParsedSymbol) obj;
				return symbol.equals(ps.symbol) && lastModified == ps.lastModified;
			}
			return false;
		}
	}
}


