package uk.ac.warwick.tabula.lucene

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.search.BooleanClause.Occur
import org.apache.lucene.search.{BooleanClause, BooleanQuery, PhraseQuery, Query, TermQuery, WildcardQuery}
import org.apache.lucene.util.Version
import org.apache.lucene.queryparser.classic.QueryParser.Operator

class SynonymAwareWildcardMultiFieldQueryParser(
		fields: Traversable[String],
		analyzer: Analyzer)
		extends MultiFieldQueryParser(Version.LUCENE_40, fields.toArray[String], analyzer) {

	setDefaultOperator(Operator.AND)

	override def getFieldQuery(field: String, queryText: String, slop: Int): Query = handleQuery(super.getFieldQuery(field, queryText, slop))
	override def getFieldQuery(field: String, queryText: String, quoted: Boolean): Query = handleQuery(super.getFieldQuery(field, queryText, quoted))

	def handleQuery(query: Query) =
		query match {
			case null => null
			case q: BooleanQuery => {
				val bq = new BooleanQuery(false)
				for (clause <- q.getClauses())
					bq.add(clause.getQuery() match {
						case query: TermQuery =>
							// Synonyms
							val term = query.getTerm
							val text = term.text

							Synonyms.names.get(text.toLowerCase) match {
								case Some(synonyms) => {
									val synonymBq = new BooleanQuery

									// all of these SHOULD occur, not MUST

									// firstly, add the main term as a wildcard
									synonymBq.add(new WildcardQuery(new Term(term.field, text + "*")), Occur.SHOULD)

									// add all synonyms as an exact termquery
									for (synonym <- synonyms)
										synonymBq.add(new TermQuery(new Term(term.field, synonym)), Occur.SHOULD)

									synonymBq
								}

								case None => new WildcardQuery(new Term(term.field, text + "*"))
							}
						case query: BooleanQuery =>
							// This happens with DelimitByCharacterFilter for names with apostrophes
							// If it starts to happen anywhere else, we may need to review whether this is still the right place to wildcard
							val clauses = query.getClauses.toSeq
							if (!clauses.isEmpty) {
								// mutate the last term
								val lastClause = clauses.last
								val lastQuery = lastClause.getQuery.asInstanceOf[TermQuery]
								val term = lastQuery.getTerm
								lastClause.setQuery(new WildcardQuery(new Term(term.field, term.text + "*")))
							}
							query
						case query: Query => query
					}, clause.getOccur)

				bq
			}
			case q: PhraseQuery => {
				val bq = new BooleanQuery(false)

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