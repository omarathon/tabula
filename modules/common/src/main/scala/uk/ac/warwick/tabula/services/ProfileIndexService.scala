package uk.ac.warwick.tabula.services

import scala.collection.JavaConverters._
import org.springframework.stereotype.Component
import uk.ac.warwick.spring.Wire
import org.springframework.beans.factory.annotation.Value
import java.io.File
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.Member
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper
import org.apache.lucene.analysis.core.KeywordAnalyzer
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
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.lucene.DelimitByCharacterFilter
import uk.ac.warwick.tabula.lucene.SurnamePunctuationFilter
import uk.ac.warwick.tabula.lucene.SynonymAwareWildcardMultiFieldQueryParser
import uk.ac.warwick.tabula.data.MemberDao
import org.apache.lucene.queryparser.classic.ParseException
import org.apache.lucene.analysis.core.WhitespaceAnalyzer
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.MemberUserType
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.BooleanClause.Occur
import org.apache.lucene.search.TermQuery
import org.apache.lucene.index.Term
import uk.ac.warwick.tabula.helpers.Logging
import org.apache.lucene.search.WildcardQuery
import uk.ac.warwick.tabula.data.model.StudentMember

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
	// Overrides AbstractIndexService when used in ProfileIndexService
	override def parser = new SynonymAwareWildcardMultiFieldQueryParser(IndexService.ProfileIndexLuceneVersion, nameFields, analyzer)

	def findWithQuery(
		query: String,
		departments: Seq[Department],
		includeTouched: Boolean,
		userTypes: Set[MemberUserType],
		searchAcrossAllDepartments: Boolean
	): Seq[Member] =
		if (departments.isEmpty && !searchAcrossAllDepartments) Seq()
		else try {
			val bq = new BooleanQuery

			if (query.hasText) {
				val q = parser.parse(sanitise(query))
				bq.add(q, Occur.MUST)
			}

			if (!searchAcrossAllDepartments) {
				val deptQuery = new BooleanQuery
				for (dept <- departments) {
					deptQuery.add(new TermQuery(new Term("department", dept.code)), Occur.SHOULD)
					if (includeTouched) deptQuery.add(new TermQuery(new Term("touchedDepartments", dept.code)), Occur.SHOULD)
				}

				bq.add(deptQuery, Occur.MUST)
			}

			if (!userTypes.isEmpty) {
				// Restrict user type
				val typeQuery = new BooleanQuery
				for (userType <- userTypes)
					typeQuery.add(new TermQuery(new Term("userType", userType.dbValue)), Occur.SHOULD)

				bq.add(typeQuery, Occur.MUST)
			}

			// Active only
			val inUseQuery = new BooleanQuery
			inUseQuery.add(new TermQuery(new Term("inUseFlag", "Active")), Occur.SHOULD)
			inUseQuery.add(new WildcardQuery(new Term("inUseFlag", "Inactive - Starts *")), Occur.SHOULD)
			bq.add(inUseQuery, Occur.MUST)

			search(bq) transformAll { toItems }
		} catch {
			case e: ParseException => Seq() // Invalid query string
		}

	def find(query: String, departments: Seq[Department], userTypes: Set[MemberUserType], isGod: Boolean): Seq[Member] = {
		if (!query.hasText) Seq()
		else findWithQuery(query, departments, includeTouched = true, userTypes = userTypes, searchAcrossAllDepartments = isGod)
	}

	def find(ownDepartment: Department, includeTouched: Boolean, userTypes: Set[MemberUserType]): Seq[Member] =
		findWithQuery("", Seq(ownDepartment), includeTouched, userTypes, searchAcrossAllDepartments = false)

	def stripTitles(query: String) =
		FullStops.replaceAllIn(
			Title.replaceAllIn(query, ""),
		". $1")

	def sanitise(query: String) = {
		val deslashed = query.replace("/", "\\/") // TAB-1331
		stripTitles(deslashed)
	}
}

@Component
class ProfileIndexService extends AbstractIndexService[Member] with ProfileQueryMethods with Logging {

	// largest batch of items we'll load in at once.
	final override val MaxBatchSize = 100000

	// largest batch of items we'll load in at once during scheduled incremental index.
	final override val IncrementalBatchSize = 1500

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

	private def makeAnalyzer(forIndexing: Boolean) = {
		val defaultAnalyzer = new KeywordAnalyzer()

		val nameAnalyzer = new ProfileAnalyzer(forIndexing)
		val nameMappings = nameFields.map(field => field -> nameAnalyzer)

		val whitespaceAnalyzer = new WhitespaceAnalyzer(IndexService.ProfileIndexLuceneVersion)
		val whitespaceMappings = whitespaceDelimitedFields.map(field => field -> whitespaceAnalyzer)

		val mappings = (nameMappings ++ whitespaceMappings).toMap[String, Analyzer].asJava

		new PerFieldAnalyzerWrapper(defaultAnalyzer, mappings)
	}

	override val analyzer = makeAnalyzer(forIndexing = false)
	override lazy val indexAnalyzer = makeAnalyzer(forIndexing = true)

	override val IdField = "universityId"
	override def getId(item: Member) = item.universityId

	override val UpdatedDateField = "lastUpdatedDate"
	override def getUpdatedDate(item: Member) = item.lastUpdatedDate

	override def listNewerThan(startDate: DateTime, batchSize: Int) = {
		val toUpdate = dao.listUpdatedSince(startDate, batchSize)
		logger.info(s"${toUpdate.size} profiles will be indexed from $startDate, ${dao.countUpdatedSince(startDate)} in total")
		toUpdate
	}

	protected def toItems(docs: Seq[Document]) =
		docs.flatMap { doc => documentValue(doc, IdField).flatMap { id => dao.getByUniversityId(id) } }

	/**
	 * TODO reuse one Document and set of Fields for all items
	 */
	protected def toDocuments(item: Member): Seq[Document] = {
		val doc = new Document

		doc add plainStringField(IdField, item.universityId)
		doc add plainStringField("userId", item.userId)

		indexTokenised(doc, "firstName", Option(item.firstName))
		indexTokenised(doc, "lastName", Option(item.lastName))
		indexTokenised(doc, "fullFirstName", Option(item.fullFirstName))
		indexTokenised(doc, "fullName", item.fullName)

		indexSeq(doc, "department", item.affiliatedDepartments map { _.code })
		indexSeq(doc, "touchedDepartments", item.touchedDepartments map { _.code })

		indexPlain(doc, "userType", Option(item.userType) map {_.dbValue})

		// Treat permanently withdrawn students as inactive
		item match {
			case student: StudentMember if student.freshStudentCourseDetails != null && student.mostSignificantCourseDetails.isDefined =>
				val status = student.mostSignificantCourseDetails.map { _.statusOnRoute }.orNull
				if (status != null && status.code == "P") indexPlain(doc, "inUseFlag", Some("Inactive"))
				else indexPlain(doc, "inUseFlag", Option(item.inUseFlag))
			case _ => indexPlain(doc, "inUseFlag", Option(item.inUseFlag))
		}

		doc add dateField(UpdatedDateField, item.lastUpdatedDate)
		Seq(doc)
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

	def indexByDateAndDepartment(startDate: DateTime, dept: Department) = {
		val deptMembers = dao.listUpdatedSince(startDate, dept, MaxBatchSize)
		logger.debug("Indexing " + deptMembers.size + " members with home department " + dept.code)
		indexItems(deptMembers)
	}
}

class ProfileAnalyzer(val indexing: Boolean) extends Analyzer {

	val StopWords = StopFilter.makeStopSet(IndexService.ProfileIndexLuceneVersion,
			"whois", "who", "email", "address", "room", "e-mail",
			"mail", "phone", "extension", "ext", "homepage", "tel",
			"mobile", "mob")

	override def createComponents(fieldName: String, reader: Reader) = {
		val source = new StandardTokenizer(IndexService.ProfileIndexLuceneVersion, reader)

		// Filter stack
		var result: TokenStream = new DelimitByCharacterFilter(source, '&')
		result =
			if (indexing) new SurnamePunctuationFilter(result)
			else new DelimitByCharacterFilter(result, '\'')

		def standard(delegate: TokenFilter) = new StandardFilter(IndexService.ProfileIndexLuceneVersion, delegate)

		result = new StandardFilter(IndexService.ProfileIndexLuceneVersion, result)
		result = new LowerCaseFilter(IndexService.ProfileIndexLuceneVersion, result)
		result = new ASCIIFoldingFilter(result)
		result = new StopFilter(IndexService.ProfileIndexLuceneVersion, result, StopWords)

		new TokenStreamComponents(source, result)
	}

}
