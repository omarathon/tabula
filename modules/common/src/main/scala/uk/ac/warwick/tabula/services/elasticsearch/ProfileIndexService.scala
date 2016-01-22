package uk.ac.warwick.tabula.services.elasticsearch

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.analyzers._
import com.sksamuel.elastic4s.mappings.TypedFieldDefinition
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.data.AutowiringMemberDaoComponent
import uk.ac.warwick.tabula.data.model.{Department, Member, StudentMember}
import uk.ac.warwick.tabula.lucene.Synonyms

import scala.collection.mutable

object ProfileIndexService {
	implicit object MemberIndexable extends ElasticsearchIndexable[Member] {

		private val FarAwayDateTime = DateTime.now.plusYears(100)

		override def fields(item: Member): Map[String, Any] = {
			var fields = mutable.Map[String, Any]()

			fields += (
				"userId" -> item.userId,
				"department" -> item.affiliatedDepartments.map { _.code },
				"touchedDepartments" -> item.touchedDepartments.map { _.code },
				"lastUpdatedDate" -> DateFormats.IsoDateTime.print(item.lastUpdatedDate)
				)

			Option(item.firstName).foreach { fields += "firstName" -> _ }
			Option(item.lastName).foreach { fields += "lastName" -> _ }
			Option(item.fullFirstName).foreach { fields += "fullFirstName" -> _ }
			item.fullName.foreach { fields += "fullName" -> _ }

			Option(item.userType).foreach { fields += "userType" -> _.dbValue }

			// Treat permanently withdrawn students as inactive
			item match {
				case student: StudentMember
					if Option(student.freshStudentCourseDetails).nonEmpty && student.mostSignificantCourseDetails.nonEmpty =>

					fields += "inUseFlag" -> "Active"  //  students should always be active; use the course date for filtering

					if (student.mostSignificantCourseDetails.get.isEnded) {
						fields += "courseEndDate" -> DateFormats.IsoDate.print(student.mostSignificantCourseDetails.get.endDate)
					} else {
						// Index a date in the future so range queries work
						fields += "courseEndDate" -> DateFormats.IsoDate.print(FarAwayDateTime)
					}
				case _ =>
					Option(item.inUseFlag).foreach { fields += "inUseFlag" -> _ }

					// Index a date in the future so range queries work
					fields += "courseEndDate" -> DateFormats.IsoDate.print(FarAwayDateTime)
			}

			fields.toMap
		}

		override def lastUpdatedDate(item: Member): DateTime = item.lastUpdatedDate

	}
}

@Service
class ProfileIndexService
	extends AbstractIndexService[Member]
		with AutowiringMemberDaoComponent
		with ProfileElasticsearchConfig {

	override implicit val indexable = ProfileIndexService.MemberIndexable

	/**
		* The name of the index that this service writes to
		*/
	@Value("${elasticsearch.index.profiles.name}") var indexName: String = _

	// largest batch of items we'll load in at once during scheduled incremental index.
	final override val IncrementalBatchSize = 1500

	override val UpdatedDateField: String = "lastUpdatedDate"

	def indexByDateAndDepartment(startDate: DateTime, dept: Department) = {
		val deptMembers = memberDao.listUpdatedSince(startDate, dept).all
		if (debugEnabled) logger.debug("Indexing " + deptMembers.size + " members with home department " + dept.code)
		indexItems(deptMembers)
	}

	// Note batch size is ignored - we use a Scrollable and it will go through all newer items
	override protected def listNewerThan(startDate: DateTime, batchSize: Int) =
		memberDao.listUpdatedSince(startDate).all
}

trait ProfileElasticsearchConfig extends ElasticsearchConfig {
	override def fields: Seq[TypedFieldDefinition] = Seq(
		// id field stores the universityId
		stringField("firstName") analyzer "name",
		stringField("lastName") analyzer "name",
		stringField("fullFirstName") analyzer "name",
		stringField("fullName") analyzer "name",

		stringField("inUseFlag") analyzer KeywordAnalyzer,
		stringField("userType") analyzer KeywordAnalyzer,

		stringField("department") analyzer WhitespaceAnalyzer,
		stringField("touchedDepartments") analyzer WhitespaceAnalyzer,

		dateField("courseEndDate") format "strict_date",
		dateField("lastUpdatedDate") format "strict_date_time_no_millis"
	)

	override def analysers: Seq[AnalyzerDefinition] = Seq(
		// A custom analyzer for names
		CustomAnalyzerDefinition("name",
			// Delimit on whitespace (preserves punctuation for the word delimiter filter)
			WhitespaceTokenizer,
			LowercaseTokenFilter,
			// Delimit words, preserving original, i.e. o'toole -> o toole o'toole
			WordDelimiterTokenFilter("name_word_delimiter")
				.preserveOriginal(true)
				.stemEnglishPossesive(false)
				.catenateAll(true),
			// Fold non-ASCII characters (e.g. accented characters) into their ASCII equivalent
			AsciiFoldingTokenFilter,
			SynonymTokenFilter(
				name = "name_synonyms",
				synonyms = Synonyms.names.toSeq.map { case (key, values) => s"$key => ${values.mkString(",")}" }.toSet,
				ignoreCase = Some(true)
			)
		)
	)
}