package uk.ac.warwick.tabula.lucene

import uk.ac.warwick.tabula.TestBase

class SynonymAwareWildcardMultiFieldQueryParserTest extends TestBase {
	
	val analyzer = new SurnamePunctuationFilterAnalyzer
	
	@Test def itWorks() {
		val parser = new SynonymAwareWildcardMultiFieldQueryParser(List("forenames", "surname"), analyzer)
		
		parser.parse("mathew").toString should be ("(forenames:mathew* forenames:matthew) (surname:mathew* surname:matthew)")
	}

}