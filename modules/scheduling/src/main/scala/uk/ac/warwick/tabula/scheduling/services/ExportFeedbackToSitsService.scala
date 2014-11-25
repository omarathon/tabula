package uk.ac.warwick.tabula.scheduling.services

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.FeedbackForSits
import javax.sql.DataSource
import org.springframework.jdbc.`object`.SqlUpdate
import java.sql.Types
import org.springframework.jdbc.core.SqlParameter
import uk.ac.warwick.tabula.JavaImports.JHashMap
import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import uk.ac.warwick.tabula.scheduling.services.ExportFeedbackToSitsService.{CountQuery, ExportFeedbackToSitsQuery}
import collection.JavaConverters._
import java.util
import uk.ac.warwick.tabula.JavaImports._

trait ExportFeedbackToSitsServiceComponent {
	def exportFeedbackToSitsService: ExportFeedbackToSitsService
}

trait AutowiringExportFeedbackToSitsServiceComponent extends ExportFeedbackToSitsServiceComponent {
	var exportFeedbackToSitsService = Wire[ExportFeedbackToSitsService]
}

trait ExportFeedbackToSitsService {
	def countMatchingBlankSasRecords(feedbackForSits: FeedbackForSits): Integer
	def exportToSits(feedbackToLoad: FeedbackForSits): Integer
}

class ParameterGetter(feedbackForSits: FeedbackForSits) {
	val assessGroups = feedbackForSits.feedback.assignment.assessmentGroups.asScala.toSeq
	val possibleOccurrenceSequencePairs = assessGroups.map(assessGroup => (assessGroup.occurrence, assessGroup.assessmentComponent.sequence))
	val occurrences = possibleOccurrenceSequencePairs.map(_._1).mkString(",")
	val sequences = possibleOccurrenceSequencePairs.map(_._2).mkString(",")

	def getQueryParams: util.HashMap[String, Object] = JHashMap(
		// for the where clause
		("studentId", feedbackForSits.feedback.universityId),
		("academicYear", feedbackForSits.feedback.assignment.academicYear.toString),
		("moduleCode", feedbackForSits.feedback.assignment.module.code.toUpperCase),
		("now", DateTime.now.toDate),

		// in theory we should look for a record with occurrence and sequence from the same pair,
		// but in practice there won't be any ambiguity since the record is already determined
		// by student, module code and year
		("occurrences", occurrences),
		("sequences", sequences)
	)

	def getUpdateParams = JHashMap(
		// for the where clause
		("studentId", feedbackForSits.feedback.universityId),
		("academicYear", feedbackForSits.feedback.assignment.academicYear.toString),
		("moduleCode", feedbackForSits.feedback.assignment.module.code.toUpperCase),
		("now", DateTime.now.toDate),

		// in theory we should look for a record with occurrence and sequence from the same pair,
		// but in practice there won't be any ambiguity since the record is already determined
		// by student, module code and year
		("occurrences", occurrences),
		("sequences", sequences),

		// data to insert
		("actualMark", feedbackForSits.feedback.actualMark),
		("actualGrade", feedbackForSits.feedback.actualGrade)
	)

}


class AbstractExportFeedbackToSitsService extends ExportFeedbackToSitsService {

	self: SitsDataSourceComponent =>
	val countQuery = new CountQuery(sitsDataSource)

	def countMatchingBlankSasRecords(feedbackForSits: FeedbackForSits): Integer = {
		val countQuery = new CountQuery(sitsDataSource)
		val parameterGetter: ParameterGetter = new ParameterGetter(feedbackForSits)
		countQuery.getCount(parameterGetter.getQueryParams)
	}

	def exportToSits(feedbackForSits: FeedbackForSits): Integer = {
		val parameterGetter: ParameterGetter = new ParameterGetter(feedbackForSits)
		val updateQuery = new ExportFeedbackToSitsQuery(sitsDataSource)


		// execute the query.  Spring's SqlUpdate.updateByNamedParam returns the number of rows affected by the update (should be 0 or 1).
		updateQuery.updateByNamedParam(parameterGetter.getUpdateParams)
	}
}

object ExportFeedbackToSitsService {
	val sitsSchema: String = Wire.property("${schema.sits}")

	val whereClause = """
			|		where spr_code in (select spr_code from intuit.ins_spr where spr_stuc = :studentId)
			|		and mod_code like ':moduleCode + %'
			|		and mav_occur in :occurrences
			|		and ayr = :academicYear
			|		and psl = 'Y'
			|		and mab_seq in :sequences
		"""

	final val CountMatchingBlankSasRecordsSql = f"""
		select count(*) from $sitsSchema.cam_sas $whereClause
	"""

	class CountQuery(ds: DataSource) extends NamedParameterJdbcTemplate(ds) {

		def getCount(params: util.HashMap[String, Object]): Int = {
			this.queryForObject(CountMatchingBlankSasRecordsSql, params, classOf[JInteger]).asInstanceOf[Int]
		}
	}

	final val UpdateSITSFeedbackSql = f"""
		update $sitsSchema.CAM_SAS
		(SAS_ACTM, SAS_ACTG, SAS_PRCS, SAS_PROC, SAS_UDF1, SAS_UDF2)
		values (:actualMark, :actualGrade, 'I', 'SAS', 'Tabula', :now)
		$whereClause
	"""

	class ExportFeedbackToSitsQuery(ds: DataSource) extends SqlUpdate(ds, UpdateSITSFeedbackSql) {
		declareParameter(new SqlParameter("actualMark", Types.INTEGER))
		declareParameter(new SqlParameter("actualGrade", Types.VARCHAR))
		declareParameter(new SqlParameter("studentId", Types.VARCHAR))
		declareParameter(new SqlParameter("academicYear", Types.VARCHAR))
		declareParameter(new SqlParameter("moduleCode", Types.VARCHAR))
		declareParameter(new SqlParameter("now", Types.DATE))
		declareParameter(new SqlParameter("occurrences", Types.VARCHAR))
		declareParameter(new SqlParameter("sequences", Types.VARCHAR))

		compile()

	}
}

@Profile(Array("dev", "test", "production"))
@Service
class ExportFeedbackToSitsServiceImpl
	extends AbstractExportFeedbackToSitsService with AutowiringSitsDataSourceComponent

@Profile(Array("sandbox"))
@Service
class ExportFeedbackToSitsSandboxService extends ExportFeedbackToSitsService {
	def countMatchingBlankSasRecords(feedbackForSits: FeedbackForSits) = 0
	def exportToSits(feedbackForSits: FeedbackForSits) = 0
}

trait SitsDataSourceComponent {
	def sitsDataSource: DataSource
}

trait AutowiringSitsDataSourceComponent extends SitsDataSourceComponent {
	var sitsDataSource = Wire[DataSource]("sitsDataSource")
}

