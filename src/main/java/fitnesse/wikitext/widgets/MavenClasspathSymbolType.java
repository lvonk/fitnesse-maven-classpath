package fitnesse.wikitext.widgets;

import fitnesse.wikitext.parser.Matcher;
import fitnesse.wikitext.parser.Maybe;
import fitnesse.wikitext.parser.Parser;
import fitnesse.wikitext.parser.Path;
import fitnesse.wikitext.parser.PathsProvider;
import fitnesse.wikitext.parser.Rule;
import fitnesse.wikitext.parser.Symbol;
import fitnesse.wikitext.parser.SymbolProvider;
import fitnesse.wikitext.parser.SymbolType;
import fitnesse.wikitext.parser.Translation;
import fitnesse.wikitext.parser.Translator;
import org.codehaus.plexus.PlexusContainerException;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FitNesse SymbolType implementation. Enables Maven classpath integration for FitNesse.
 */
public class MavenClasspathSymbolType extends SymbolType implements Rule, Translation, PathsProvider {
    /** System property to disable this Symbol (if given value true). */
    public static final String DISABLE_KEY = "fitnesse.wikitext.widgets.MavenClasspathSymbolType.Disable";

    private MavenClasspathExtractor mavenClasspathExtractor;

    private final Map<String, List<String>> classpathCache = new HashMap<String, List<String>>();

    public MavenClasspathSymbolType() throws PlexusContainerException {
        super("MavenClasspathSymbolType");

        String disablePropertyValue = System.getProperty(DISABLE_KEY);
        if (!"true".equalsIgnoreCase(disablePropertyValue)) {
            this.mavenClasspathExtractor = new MavenClasspathExtractor();
        }

        wikiMatcher(new Matcher().startLineOrCell().string("!pomFile"));

        wikiRule(this);
        htmlTranslation(this);
    }

    @Override
    public String toTarget(Translator translator, Symbol symbol) {
        List<String> classpathElements = null;
        ParsedSymbol parsedSymbol = getParsedSymbol(translator, symbol);
        StringBuilder classpathForRender = new StringBuilder("<p class='meta'>Maven classpath [file: ")
                .append(parsedSymbol.getPomFile())
                .append(", scope: ")
                .append(parsedSymbol.getScope())
                .append("]:</p>")
                .append("<ul class='meta'>");
        try {
            classpathElements = getClasspathElements(parsedSymbol);
            for (String element : classpathElements) {
                classpathForRender.append("<li>").append(element).append("</li>");
            }
        } catch (MavenClasspathExtractionException e) {
            classpathForRender.append("<li class='error'>Unable to parse POM file: ")
                            .append(e.getMessage()).append("</li>");
        }

        classpathForRender.append("</ul>");
        return classpathForRender.toString();

    }

    @SuppressWarnings("unchecked")
	private List<String> getClasspathElements(final ParsedSymbol parsedSymbol) throws MavenClasspathExtractionException {
        String symbol = parsedSymbol.symbol;
        if(classpathCache.containsKey(symbol)) {
    		return classpathCache.get(symbol);
    	} else {
    		List<String> classpath = Collections.emptyList();
            if (mavenClasspathExtractor != null) {
                classpath = mavenClasspathExtractor.extractClasspathEntries(parsedSymbol.getPomFile(), parsedSymbol.getScope());
            }
            classpathCache.put(symbol, classpath);
            return classpath;
    	}
    }

    private ParsedSymbol getParsedSymbol(Translator translator, Symbol symbol) {
        return new ParsedSymbol(translator.translate(symbol.childAt(0)));
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
            return getClasspathElements(getParsedSymbol(translator, symbol));
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


