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

/**
 * Methods for querying stuff out of the index. Separated out from
 * the main index service into this trait so they're easier to find.
 * Possibly the indexer and the index querier should be separate classes
 * altogether.
 */
trait ProfileQueryMethods { self: ProfileIndexService =>
	
	private val Title = """^(?:Mr|Ms|Mrs|Miss|Dr|Sir|Doctor|Prof(?:essor)?)\b""".r
	private val FullStops = """\.(\S)""".r
	
	override lazy val parser = new SynonymAwareWildcardMultiFieldQueryParser(nameFields, analyzer)
	
	def find(query: String) =
		if (!StringUtils.hasText(query)) Seq()
		else search(parse(query)) flatMap { toItem(_) }
	
	private def parse(rawQuery: String) = {
		var query = Title.replaceAllIn(rawQuery, "")
		query = FullStops.replaceAllIn(query, ". $1")
		
		parser.parse(query)
	}
	
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
	
	override val analyzer = {
		val defaultAnalyzer = new KeywordAnalyzer()
		
		val nameAnalyzer = new ProfileAnalyzer(false)
		val nameMappings = nameFields.map(field => (field -> nameAnalyzer))
		
		val mappings = (nameMappings).toMap[String, Analyzer].asJava
		
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
		
		if (item.firstName != null) doc add tokenisedStringField("firstName", item.firstName)
		if (item.lastName != null) doc add tokenisedStringField("lastName", item.lastName)
		if (item.fullFirstName != null) doc add tokenisedStringField("fullFirstName", item.fullFirstName)
		if (item.firstName != null && item.lastName != null) doc add tokenisedStringField("fullName", item.fullName)
		
		doc add dateField(UpdatedDateField, item.lastUpdatedDate)
		doc
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
				
		result = new StandardFilter(LuceneVersion, result)
		result = new LowerCaseFilter(LuceneVersion, result)
		result = new StopFilter(LuceneVersion, result, StopWords)
		
		new TokenStreamComponents(source, result)
	}
	
}