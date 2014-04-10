package uk.ac.warwick.tabula.lucene

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.services.IndexService

class SynonymAwareWildcardMultiFieldQueryParserTest extends TestBase {
	
	val analyzer = new SurnamePunctuationFilterAnalyzer
	
	@Test def itWorks() {
		val parser = new SynonymAwareWildcardMultiFieldQueryParser(IndexService.TabulaLuceneVersion, List("forenames", "surname"), analyzer)
		
		parser.parse("mathew").toString should be ("(forenames:mathew* forenames:matthew) (surname:mathew* surname:matthew)")
	}

}