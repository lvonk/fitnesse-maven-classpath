package fitnesse.wikitext.widgets;

import fitnesse.html.HtmlUtil;
import fitnesse.wikitext.parser.*;
import org.codehaus.plexus.PlexusContainerException;
import util.Maybe;

import java.io.File;
import java.util.Collection;
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
        List<String> classpathElements = getClasspathElements(symbol);

        String classpathForRender = "";
        for (String element : classpathElements) {
            classpathForRender += HtmlUtil.metaText("classpath: " + element) + HtmlUtil.BRtag;

        }
        return classpathForRender;

    }

	private List<String> getClasspathElements(Symbol symbol) {
 		
		ParsedSymbol parsedSymbol = new ParsedSymbol(symbol.childAt(0).getContent());

		return mavenClasspathExtractor.extractClasspathEntries(parsedSymbol.getPomFile(), parsedSymbol.getScope());
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


