package uk.ac.warwick.tabula.services

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import org.springframework.stereotype.Component
import org.apache.lucene.util.Version
import uk.ac.warwick.spring.Wire
import org.springframework.beans.factory.annotation.Value
import java.io.File
import java.util.concurrent.ScheduledExecutorService
import org.joda.time.Duration
import org.joda.time.DateTime
import java.util.concurrent.Executors
import uk.ac.warwick.tabula.data.model.Member
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper
import org.apache.lucene.analysis.core.KeywordAnalyzer
import uk.ac.warwick.tabula.data.model.AuditEvent
import org.apache.lucene.document.Document
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardTokenizer
import java.io.Reader
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents
import org.apache.lucene.analysis.standard.StandardFilter
import org.apache.lucene.analysis.core.StopFilter
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter
import org.apache.lucene.analysis.core.LowerCaseFilter
import org.apache.lucene.analysis.TokenFilter
import org.apache.lucene.analysis.TokenStream
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import uk.ac.warwick.util.core.StringUtils
import uk.ac.warwick.tabula.lucene.DelimitByCharacterFilter
import uk.ac.warwick.tabula.lucene.SurnamePunctuationFilter
import uk.ac.warwick.tabula.lucene.SynonymAwareWildcardMultiFieldQueryParser
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.MemberDao
import org.apache.lucene.queryparser.classic.ParseException
import org.apache.lucene.analysis.core.WhitespaceAnalyzer
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.MemberUserType
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.BooleanClause.Occur
import org.apache.lucene.search.TermQuery
import org.apache.lucene.index.Term

/**
 * Methods for querying stuff out of the index. Separated out from
 * the main index service into this trait so they're easier to find.
 * Possibly the indexer and the index querier should be separate classes
 * altogether.
 */
trait ProfileQueryMethods { self: ProfileIndexService =>
	
	private val Title = """^(?:Mr|Ms|Mrs|Miss|Dr|Sir|Doctor|Prof(?:essor)?)(\.?|\b)\s*""".r
	private val FullStops = """\.(\S)""".r
	
	// QueryParser isn't thread safe, hence why this is a def
	override def parser = new SynonymAwareWildcardMultiFieldQueryParser(nameFields, analyzer)

	private def findWithQuery(query: String, departments: Seq[Department], userTypes: Set[MemberUserType], isGod: Boolean): Seq[Member] = {
		if (departments.isEmpty && !isGod) Seq()
		else try {
			val bq = new BooleanQuery

			if (StringUtils.hasText(query)) {
				val q = parser.parse(stripTitles(query))
				bq.add(q, Occur.MUST)
			}

			if (!isGod) {
				val deptQuery = new BooleanQuery
				for (dept <- departments)
					deptQuery.add(new TermQuery(new Term("department", dept.code)), Occur.SHOULD)

				bq.add(deptQuery, Occur.MUST)
			}
			
			if (!userTypes.isEmpty) {
				// Restrict user type
				val typeQuery = new BooleanQuery
				for (userType <- userTypes)
					typeQuery.add(new TermQuery(new Term("userType", userType.dbValue)), Occur.SHOULD)
					
				bq.add(typeQuery, Occur.MUST)
			}
			
			search(bq) flatMap { toItem(_) }
		} catch {
			case e: ParseException => Seq() // Invalid query string
		}
	}
	
	def find(query: String, departments: Seq[Department], userTypes: Set[MemberUserType], isGod: Boolean): Seq[Member] = {
		if (!StringUtils.hasText(query)) Seq()
		else findWithQuery(query, departments, userTypes, isGod)
	}
	
	def find(ownDepartment: Department, userTypes: Set[MemberUserType]): Seq[Member] =
		findWithQuery("", Seq(ownDepartment), userTypes, false)
	
	def stripTitles(query: String) = 
		FullStops.replaceAllIn(
			Title.replaceAllIn(query, ""), 
		". $1")
	
}

@Component
class ProfileIndexService extends AbstractIndexService[Member] with ProfileQueryMethods {	
	
	// largest batch of items we'll load in at once.
	final override val MaxBatchSize = 100000

	// largest batch of items we'll load in at once during scheduled incremental index.
	final override val IncrementalBatchSize = 1000
	
	var dao = Wire.auto[MemberDao]
	
	@Value("${filesystem.index.profiles.dir}") override var indexPath: File = _
	
	// Fields that will be tokenised as names
	val nameFields = Set(
		"firstName",
		"lastName",
		"fullFirstName",
		"fullName"
	)
	
	// Fields that will be split on whitespace
	val whitespaceDelimitedFields = Set(
		"department",
		"touchedDepartments"
	)
	
	override val analyzer = {
		val defaultAnalyzer = new KeywordAnalyzer()
		
		val nameAnalyzer = new ProfileAnalyzer(false)
		val nameMappings = nameFields.map(field => (field -> nameAnalyzer))
		
		val whitespaceAnalyzer = new WhitespaceAnalyzer(LuceneVersion)
		val whitespaceMappings = whitespaceDelimitedFields.map(field => (field -> whitespaceAnalyzer))
		
		val mappings = (nameMappings ++ whitespaceMappings).toMap[String, Analyzer].asJava
		
		new PerFieldAnalyzerWrapper(defaultAnalyzer, mappings)
	}
	
	override lazy val indexAnalyzer = {
		val defaultAnalyzer = new KeywordAnalyzer()
		
		val nameAnalyzer = new ProfileAnalyzer(true)
		val nameMappings = nameFields.map(field => (field -> nameAnalyzer))
		
		val mappings = (nameMappings).toMap[String, Analyzer].asJava
		
		new PerFieldAnalyzerWrapper(defaultAnalyzer, mappings)
	}
	
	override val IdField = "universityId"
	override def getId(item: Member) = item.universityId
	
	override val UpdatedDateField = "lastUpdatedDate"
	override def getUpdatedDate(item: Member) = item.lastUpdatedDate
	
	override def listNewerThan(startDate: DateTime, batchSize: Int) = dao.listUpdatedSince(startDate, batchSize)
	
	protected def toItem(id: String) = dao.getByUniversityId(id)
	
	/**
	 * TODO reuse one Document and set of Fields for all items
	 */
	protected def toDocument(item: Member): Document = {
		val doc = new Document

		doc add plainStringField(IdField, item.universityId)
		doc add plainStringField("userId", item.userId)
		
		indexTokenised(doc, "firstName", Option(item.firstName))
		indexTokenised(doc, "lastName", Option(item.lastName))
		indexTokenised(doc, "fullFirstName", Option(item.fullFirstName))
		indexTokenised(doc, "fullName", Option(item.fullName))
		
		indexSeq(doc, "department", item.affiliatedDepartments map { _.code })
		indexSeq(doc, "touchedDepartments", item.touchedDepartments map { _.code })
		
		indexPlain(doc, "userType", Option(item.userType) map {_.dbValue})
		
		doc add dateField(UpdatedDateField, item.lastUpdatedDate)
		doc
	}
	
	private def indexTokenised(doc: Document, fieldName: String, value: Option[String]) = {
		if (value.isDefined)
			doc add tokenisedStringField(fieldName, value.get)
	}
	
	private def indexPlain(doc: Document, fieldName: String, value: Option[String]) = {
		if (value.isDefined)
			doc add plainStringField(fieldName, value.get)
	}
	
	private def indexSeq(doc: Document, fieldName: String, values: Seq[_]) = {
		if (!values.isEmpty)
			doc add seqField(fieldName, values)
	}
	
}

class ProfileAnalyzer(val indexing: Boolean) extends Analyzer {
	
	final val LuceneVersion = Version.LUCENE_40
	
	val StopWords = StopFilter.makeStopSet(LuceneVersion, 
			"whois", "who", "email", "address", "room", "e-mail",
			"mail", "phone", "extension", "ext", "homepage", "tel",
			"mobile", "mob")
				
	override def createComponents(fieldName: String, reader: Reader) = {
		val source = new StandardTokenizer(LuceneVersion, reader)
		
		// Filter stack
		var result: TokenStream = new DelimitByCharacterFilter(source, '&')
		result = 
			if (indexing) new SurnamePunctuationFilter(result)
			else new DelimitByCharacterFilter(result, '\'')
		
		def standard(delegate: TokenFilter) = new StandardFilter(LuceneVersion, delegate)
		
		result = new StandardFilter(LuceneVersion, result)
		result = new LowerCaseFilter(LuceneVersion, result)
		result = new StopFilter(LuceneVersion, result, StopWords)
		
		new TokenStreamComponents(source, result)
	}
	
}