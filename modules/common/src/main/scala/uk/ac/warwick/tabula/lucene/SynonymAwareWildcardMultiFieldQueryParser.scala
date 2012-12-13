package uk.ac.warwick.tabula.lucene

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.search.BooleanClause.Occur
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.PhraseQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery
import org.apache.lucene.search.WildcardQuery
import org.apache.lucene.util.Version
import org.apache.lucene.queryparser.classic.QueryParser.Operator

class SynonymAwareWildcardMultiFieldQueryParser(fields: Traversable[String], analyzer: Analyzer) extends MultiFieldQueryParser(Version.LUCENE_40, fields.toArray[String], analyzer) {
	
	setDefaultOperator(Operator.AND)
	
	override def getFieldQuery(field: String, queryText: String, slop: Int): Query = handleQuery(super.getFieldQuery(field, queryText, slop))
	override def getFieldQuery(field: String, queryText: String, quoted: Boolean): Query = handleQuery(super.getFieldQuery(field, queryText, quoted))
	
	def handleQuery(query: Query) =
		query match {
			case null => null
			case q: BooleanQuery => {
				val bq = new BooleanQuery
				for (clause <- q.getClauses())
					clause.getQuery() match {
						case query: TermQuery =>
							// TODO synonyms
							
							bq.add(new WildcardQuery(new Term(query.getTerm.field, query.getTerm.text + "*")), clause.getOccur)
						
						case query => 
							bq.add(clause)
					}
				
				bq
			}
			case q: PhraseQuery => {
				val bq = new BooleanQuery
				
				val terms = q.getTerms
				for (term <- terms.slice(0, terms.length - 1))
					bq.add(new TermQuery(term), Occur.MUST)
					
				val last = terms.last
				bq.add(new WildcardQuery(new Term(last.field, last.text + "*")), Occur.MUST)
					
				bq
			}
			case q => q
		}

}