package aktie.index;

import java.io.IOException;
import java.io.Reader;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.util.Version;

/**
    Note this code was lifted from Apache StandardAnalyzer so that
    WordDelimiterFilter could be added.
*/
public class GenenskapAnalyzer extends StopwordAnalyzerBase
{
    /** Default maximum allowed token length */

    public static final int DEFAULT_MAX_TOKEN_LENGTH = 255;
    private int maxTokenLength = DEFAULT_MAX_TOKEN_LENGTH;

    /** An unmodifiable set containing some common English words that are usually not
        useful for searching. */
    public static final CharArraySet STOP_WORDS_SET = StopAnalyzer.ENGLISH_STOP_WORDS_SET;

    /** Builds an analyzer with the given stop words.
        @param matchVersion Lucene version to match See {@link
        <a href="#version">above</a>}

        @param stopWords stop words */
    public GenenskapAnalyzer ( CharArraySet stopWords )
    {
        super ( stopWords );
    }

    /** Builds an analyzer with the default stop words ({@link
        #STOP_WORDS_SET}).

        @param matchVersion Lucene version to match See {@link
        <a href="#version">above</a>}

    */
    public GenenskapAnalyzer()
    {
        this ( STOP_WORDS_SET );
    }

    /** Builds an analyzer with the stop words from the given reader.
        @see WordlistLoader#getWordSet(Reader, Version)
        @param matchVersion Lucene version to match See {@link
        <a href="#version">above</a>}

        @param stopwords Reader to read stop words from */
    public GenenskapAnalyzer ( Reader stopwords ) throws IOException
    {
        this ( loadStopwordSet ( stopwords ) );
    }

    /**
        Set maximum allowed token length. If a token is seen
        that exceeds this length then it is discarded. This
        setting only takes effect the next time tokenStream or
        tokenStream is called.
    */
    public void setMaxTokenLength ( int length )
    {
        maxTokenLength = length;
    }

    /**
        @see #setMaxTokenLength
    */
    public int getMaxTokenLength()
    {
        return maxTokenLength;
    }

    @Override
    protected TokenStreamComponents createComponents ( final String fieldName )
    {
        //final StandardTokenizer src = new StandardTokenizer(matchVersion, reader);
        //src.setMaxTokenLength(maxTokenLength);

        Tokenizer src = new WhitespaceTokenizer ( );
        TokenStream wtok = new WordDelimiterFilter ( src,
                WordDelimiterFilter.GENERATE_WORD_PARTS |
                WordDelimiterFilter.GENERATE_NUMBER_PARTS |
                WordDelimiterFilter.SPLIT_ON_CASE_CHANGE |
                WordDelimiterFilter.SPLIT_ON_NUMERICS |
                WordDelimiterFilter.PRESERVE_ORIGINAL |
                WordDelimiterFilter.CATENATE_ALL |
                WordDelimiterFilter.CATENATE_NUMBERS |
                WordDelimiterFilter.CATENATE_WORDS,
                null );
        TokenStream tok = new StandardFilter ( wtok );
        tok = new LowerCaseFilter ( tok );
        tok = new StopFilter ( tok, stopwords );
        return new TokenStreamComponents ( src, tok );

        /*
            return new TokenStreamComponents(src, tok) {
            @Override
            protected void setReader(final Reader reader) throws IOException {
                src.setMaxTokenLength(OFSAnalyzer.this.maxTokenLength);
                super.setReader(reader);
            }

            };

        */
    }


}
