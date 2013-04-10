package uk.ac.warwick.tabula.lucene

import uk.ac.warwick.tabula.TestBase
import org.apache.lucene.analysis.standard.StandardTokenizer
import org.apache.lucene.util.Version
import java.io.Reader
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.TokenStream
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents
import java.io.StringReader
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute

class DelimitByCharacterFilterTest extends TestBase {
	
	val analyzer = new DelimitByCharacterFilterAnalyzer
	
	@Test def itWorks() {
		val tokenStream = analyzer.tokenStream("name", new StringReader("sarah o'toole"))
		val attribute = tokenStream.addAttribute(classOf[CharTermAttribute])
		
		tokenStream.incrementToken() should be (true)
		term(attribute) should be ("sarah")
		
		tokenStream.incrementToken() should be (true)
		term(attribute) should be ("o")
		
		tokenStream.incrementToken() should be (true)
		term(attribute) should be ("toole")
		
		tokenStream.incrementToken() should be (false)
	}
	
	private def term(term: CharTermAttribute) = new String(term.buffer, 0, term.length)

}

class DelimitByCharacterFilterAnalyzer extends Analyzer {
	
	final val LuceneVersion = Version.LUCENE_40
	
	override def createComponents(fieldName: String, reader: Reader) = {
		val source = new StandardTokenizer(LuceneVersion, reader)
		val result: TokenStream = new DelimitByCharacterFilter(source, '\'')
		
		new TokenStreamComponents(source, result)
	}
	
}