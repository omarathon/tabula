package uk.ac.warwick.tabula.services.scheduling

import java.sql.Types
import java.util
import javax.sql.DataSource

import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.SqlUpdate
import org.springframework.jdbc.core.SqlParameter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.FeedbackForSits
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.scheduling.ExportFeedbackToSitsService.{ExportFeedbackToSitsQuery, CountQuery}

import scala.collection.JavaConverters._

trait ExportFeedbackToSitsServiceComponent {
	def exportFeedbackToSitsService: ExportFeedbackToSitsService
}

trait AutowiringExportFeedbackToSitsServiceComponent extends ExportFeedbackToSitsServiceComponent {
	var exportFeedbackToSitsService = Wire[ExportFeedbackToSitsService]
}

trait ExportFeedbackToSitsService {
	def countMatchingSasRecords(feedbackForSits: FeedbackForSits): Integer
	def exportToSits(feedbackToLoad: FeedbackForSits): Integer
}

class ParameterGetter(feedbackForSits: FeedbackForSits) {
	val assessGroups = feedbackForSits.feedback.assessmentGroups.asScala
	val possibleOccurrenceSequencePairs = assessGroups.map(assessGroup => (assessGroup.occurrence, assessGroup.assessmentComponent.sequence))

	def getQueryParams: util.HashMap[String, Object] = JHashMap(
		// for the where clause
		("studentId", feedbackForSits.feedback.universityId),
		("academicYear", feedbackForSits.feedback.academicYear.toString),
		("moduleCodeMatcher", feedbackForSits.feedback.module.code.toUpperCase + "%"),
		("now", DateTime.now.toDate),

		// in theory we should look for a record with occurrence and sequence from the same pair,
		// but in practice there won't be any ambiguity since the record is already determined
		// by student, module code and year
		("occurrences", possibleOccurrenceSequencePairs.map(_._1).asJava),
		("sequences", possibleOccurrenceSequencePairs.map(_._2).asJava)
	)

	def getUpdateParams(mark: Integer, grade: String) = JHashMap(
		// for the where clause
		("studentId", feedbackForSits.feedback.universityId),
		("academicYear", feedbackForSits.feedback.academicYear.toString),
		("moduleCodeMatcher", feedbackForSits.feedback.module.code.toUpperCase + "%"),
		("now", DateTime.now.toDate),

		// in theory we should look for a record with occurrence and sequence from the same pair,
		// but in practice there won't be any ambiguity since the record is already determined
		// by student, module code and year
		("occurrences", possibleOccurrenceSequencePairs.map(_._1).asJava),
		("sequences", possibleOccurrenceSequencePairs.map(_._2).asJava),

		// data to insert
		("actualMark", mark),
		("actualGrade", grade)
	)

}


class AbstractExportFeedbackToSitsService extends ExportFeedbackToSitsService with Logging {

	self: SitsDataSourceComponent =>

	def countMatchingSasRecords(feedbackForSits: FeedbackForSits): Integer = {
		val countQuery = new CountQuery(sitsDataSource)
		val parameterGetter: ParameterGetter = new ParameterGetter(feedbackForSits)
		countQuery.getCount(parameterGetter.getQueryParams)
	}

	def exportToSits(feedbackForSits: FeedbackForSits): Integer = {
		val parameterGetter: ParameterGetter = new ParameterGetter(feedbackForSits)
		val updateQuery = new ExportFeedbackToSitsQuery(sitsDataSource)

		val grade = feedbackForSits.feedback.latestGrade
		val mark = feedbackForSits.feedback.latestMark
		val numRowsChanged =
			if (grade.isDefined && mark.isDefined)
				updateQuery.updateByNamedParam(parameterGetter.getUpdateParams(mark.get, grade.get))
		else {
				0 // issue a warning when the FeedbackForSits record is created, not here
			}
		numRowsChanged
	}
}

object ExportFeedbackToSitsService {
	val sitsSchema: String = Wire.property("${schema.sits}")

	// match on the Student Programme Route (SPR) code for the student
	// mav_occur = module occurrence code
	// psl_code = "Period Slot"
	// mab_seq = sequence code determining an assessment component
	// Only upload when the mark/grade is empty or was previously uploaded by Tabula
	def whereClause = f"""where spr_code in (select spr_code from $sitsSchema.ins_spr where spr_stuc = :studentId)
		and mod_code like :moduleCodeMatcher
		and mav_occur in (:occurrences)
		and ayr_code = :academicYear
		and psl_code = 'Y'
		and mab_seq in (:sequences)
		and (
			sas_actm is null and sas_actg is null
			or sas_udf1 = 'Tabula'
		)
	"""

	final def CountMatchingBlankSasRecordsSql = f"""
		select count(*) from $sitsSchema.cam_sas $whereClause
	"""

	class CountQuery(ds: DataSource) extends NamedParameterJdbcTemplate(ds) {

		def getCount(params: util.HashMap[String, Object]): Int = {
			this.queryForObject(CountMatchingBlankSasRecordsSql, params, classOf[JInteger]).asInstanceOf[Int]
		}
	}

	// update Student Assessment table (CAM_SAS) which holds module component marks
	// SAS_PRCS = Process Status - Value of I enables overall marks to be calculated in SITS
	// SAS_PROC = Current Process - Value of SAS enabled overall marks to be calculated in SITS
	// SAS_UDF1, SAS_UDF2 - user defined fields used for audit
	final def UpdateSITSFeedbackSql = f"""
		update $sitsSchema.cam_sas
		set sas_actm = :actualMark,
			sas_actg = :actualGrade,
			sas_prcs = 'I',
			sas_proc = 'SAS',
			sas_udf1 = 'Tabula',
			sas_udf2 = :now
		$whereClause
	"""

	class ExportFeedbackToSitsQuery(ds: DataSource) extends SqlUpdate(ds, UpdateSITSFeedbackSql) {
		declareParameter(new SqlParameter("actualMark", Types.INTEGER))
		declareParameter(new SqlParameter("actualGrade", Types.VARCHAR))
		declareParameter(new SqlParameter("studentId", Types.VARCHAR))
		declareParameter(new SqlParameter("academicYear", Types.VARCHAR))
		declareParameter(new SqlParameter("moduleCodeMatcher", Types.VARCHAR))
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
	def countMatchingSasRecords(feedbackForSits: FeedbackForSits) = 0
	def exportToSits(feedbackForSits: FeedbackForSits) = 0
}
