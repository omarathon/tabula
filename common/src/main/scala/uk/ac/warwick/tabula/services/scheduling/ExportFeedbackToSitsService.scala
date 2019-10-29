package uk.ac.warwick.tabula.services.scheduling

import java.sql.{ResultSet, Types}
import java.util

import javax.sql.DataSource
import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.{MappingSqlQueryWithParameters, SqlUpdate}
import org.springframework.jdbc.core.SqlParameter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.scheduling.imports.ImportMemberHelpers
import uk.ac.warwick.tabula.data.model.{AssessmentGroup, AssignmentFeedback, Feedback}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.scheduling.ExportFeedbackToSitsService.{CountQuery, ExportFeedbackToSitsQuery, ExportResitFeedbackToSitsQuery, SASPartialMatchQuery, SITSMarkRow, SRAPartialMatchQuery, SasCountQuery, SraCountQuery}

import scala.collection.JavaConverters._

trait ExportFeedbackToSitsServiceComponent {
  def exportFeedbackToSitsService: ExportFeedbackToSitsService
}

trait AutowiringExportFeedbackToSitsServiceComponent extends ExportFeedbackToSitsServiceComponent {
  var exportFeedbackToSitsService: ExportFeedbackToSitsService = Wire[ExportFeedbackToSitsService]
}

trait ExportFeedbackToSitsService {
  def countMatchingSitsRecords(feedback: Feedback): Integer

  def exportToSits(feedback: Feedback): Integer

  def getPartialMatchingSITSRecords(feedback: Feedback): Seq[ExportFeedbackToSitsService.SITSMarkRow]
}

class ParameterGetter(feedback: Feedback) {
  val assessGroups: Seq[AssessmentGroup] = feedback.assessmentGroups
  val possibleOccurrenceSequencePairs: Seq[(String, String)] = assessGroups.map(assessGroup => (assessGroup.occurrence, assessGroup.assessmentComponent.sequence))

  def getQueryParams: Option[util.HashMap[String, Object]] = possibleOccurrenceSequencePairs match {
    case pairs if pairs.isEmpty => None
    case _ => Option(JHashMap(
      // for the where clause
      ("studentId", feedback.studentIdentifier),
      ("academicYear", feedback.academicYear.toString),
      ("moduleCodeMatcher", feedback.module.code.toUpperCase + "%"),
      ("now", DateTime.now.toDate),

      // in theory we should look for a record with occurrence and sequence from the same pair,
      // but in practice there won't be any ambiguity since the record is already determined
      // by student, module code and year
      ("occurrences", possibleOccurrenceSequencePairs.map(_._1).asJava),
      ("sequences", possibleOccurrenceSequencePairs.map(_._2).asJava)
    ))
  }

  def getUpdateParams(mark: Integer, grade: String): Option[util.HashMap[String, Object]] = possibleOccurrenceSequencePairs match {
    case pairs if pairs.isEmpty => None
    case _ => Option(JHashMap(
      // for the where clause
      ("studentId", feedback.studentIdentifier),
      ("academicYear", feedback.academicYear.toString),
      ("moduleCodeMatcher", feedback.module.code.toUpperCase + "%"),
      ("now", DateTime.now.toDate),

      // in theory we should look for a record with occurrence and sequence from the same pair,
      // but in practice there won't be any ambiguity since the record is already determined
      // by student, module code and year
      ("occurrences", possibleOccurrenceSequencePairs.map(_._1).asJava),
      ("sequences", possibleOccurrenceSequencePairs.map(_._2).asJava),

      // data to insert
      ("actualMark", mark),
      ("actualGrade", grade)
    ))
  }

}


class AbstractExportFeedbackToSitsService extends ExportFeedbackToSitsService with Logging {
  self: SitsDataSourceComponent =>

  private def countInternal(feedback: Feedback, tableName: String, query: CountQuery) = {
    val parameterGetter: ParameterGetter = new ParameterGetter(feedback)
    parameterGetter.getQueryParams match {
      case Some(params) =>
        query.getCount(params)
      case None =>
        logger.warn(s"Cannot upload feedback ${feedback.id} for SITS as no $tableName records found")
        0
    }
  }

  private def countMatchingSasRecords(feedback: Feedback): Integer = {
    val countQuery = new SasCountQuery(sitsDataSource)
    countInternal(feedback, "SAS", countQuery)

  }

  private def countMatchingSraRecords(feedback: Feedback): Integer = {
    val countQuery = new SraCountQuery(sitsDataSource)
    countInternal(feedback, "SRA", countQuery)
  }

  def countMatchingSitsRecords(feedback: Feedback): Integer = feedback match {
    case f: AssignmentFeedback if f.assignment.resitAssessment => countMatchingSraRecords(feedback: Feedback)
    case _ => countMatchingSasRecords(feedback: Feedback)
  }

  def exportToSits(feedback: Feedback): Integer = {
    val parameterGetter: ParameterGetter = new ParameterGetter(feedback)
    val (updateQuery, tableName) = feedback match {
      case f: AssignmentFeedback if f.assignment.resitAssessment => (new ExportResitFeedbackToSitsQuery(sitsDataSource), "CAM_SRA")
      case _ => (new ExportFeedbackToSitsQuery(sitsDataSource), "CAM_SAS")
    }

    val grade = feedback.latestGrade
    val mark = feedback.latestMark
    val numRowsChanged = {
      if (grade.isDefined && mark.isDefined) {
        parameterGetter.getUpdateParams(mark.get, grade.get) match {
          case Some(params) =>
            updateQuery.updateByNamedParam(params)
          case None =>
            logger.warn(s"Cannot upload feedback ${feedback.id} for SITS $tableName as no assessment groups found")
            0
        }
      } else {
        logger.warn(f"Not updating SITS $tableName for feedback ${feedback.id} - no latest mark or grade found")
        0 // issue a warning when the FeedbackForSits record is created, not here
      }
    }
    numRowsChanged
  }

  def getPartialMatchingSITSRecords(feedback: Feedback): Seq[ExportFeedbackToSitsService.SITSMarkRow] = {

    val (matchQuery, tableName) = feedback match {
      case f: AssignmentFeedback if f.assignment.resitAssessment => (new SRAPartialMatchQuery(sitsDataSource), "CAM_SRA")
      case _ => (new SASPartialMatchQuery(sitsDataSource), "CAM_SAS")
    }

    val parameterGetter: ParameterGetter = new ParameterGetter(feedback)
    parameterGetter.getQueryParams match {
      case Some(params) =>
        matchQuery.executeByNamedParam(params.asScala.view.filterKeys(_ != "now").toMap.asJava).asScala.toSeq
      case None =>
        logger.warn(s"Cannot get partial matching $tableName records for feedback ${feedback.id} as no assessment groups found")
        Nil
    }
  }
}

object ExportFeedbackToSitsService {
  val sitsSchema: String = Wire.property("${schema.sits}")
  val tabulaIdentifier = "Tabula"

  // match on the Student Programme Route (SPR) code for the student
  // mav_occur = module occurrence code
  // psl_code = "Period Slot"
  def rootWhereClause =
    f"""where spr_code in (select spr_code from $sitsSchema.ins_spr where spr_stuc = :studentId)
    and mod_code like :moduleCodeMatcher
    and mav_occur in (:occurrences)
    and ayr_code = :academicYear
    and psl_code = 'Y'
    """

  def whereClause = s"$rootWhereClause and mab_seq in (:sequences)" // mab_seq = sequence code determining an assessment component
  def resitWhereClause = s"$rootWhereClause and sra_seq in (:sequences)" // sra_seq = sequence code determining an assessment component

  // Only upload when the mark/grade is empty or was previously uploaded by Tabula
  def writeableWhereClause =
    f"""$whereClause
    and (
      sas_actm is null and sas_actg is null
      or sas_udf1 = '$tabulaIdentifier'
    )
    """

  def resitWriteableWhereClause =
    f"""$resitWhereClause
    and (
      sra_actm is null and sra_actg is null
      or sra_udf2 = '$tabulaIdentifier'
    )
    """

  final def CountMatchingBlankSasRecordsSql =
    f"""
    select count(*) from $sitsSchema.cam_sas $writeableWhereClause
    """

  final def CountMatchingBlankSraRecordsSql =
    f"""
    select count(*) from $sitsSchema.cam_sra $resitWriteableWhereClause
    """

  abstract class CountQuery(ds: DataSource) extends NamedParameterJdbcTemplate(ds) {
    def getCount(params: util.HashMap[String, Object]): Int
  }

  class SasCountQuery(ds: DataSource) extends CountQuery(ds) {

    def getCount(params: util.HashMap[String, Object]): Int = {
      this.queryForObject(CountMatchingBlankSasRecordsSql, params, classOf[JInteger]).asInstanceOf[Int]
    }
  }

  class SraCountQuery(ds: DataSource) extends CountQuery(ds) {

    def getCount(params: util.HashMap[String, Object]): Int = {
      this.queryForObject(CountMatchingBlankSraRecordsSql, params, classOf[JInteger]).asInstanceOf[Int]
    }
  }

  // update Student Assessment table (CAM_SAS) which holds module component marks
  // SAS_PRCS = Process Status - Value of I enables overall marks to be calculated in SITS
  // SAS_PROC = Current Process - Value of SAS enabled overall marks to be calculated in SITS
  // SAS_UDF1, SAS_UDF2 - user defined fields used for audit
  final def UpdateSITSFeedbackSql =
    f"""
		update $sitsSchema.cam_sas
		set sas_actm = :actualMark,
			sas_actg = :actualGrade,
			sas_prcs = 'I',
			sas_proc = 'SAS',
			sas_udf1 = '$tabulaIdentifier',
			sas_udf2 = :now
		$writeableWhereClause
	"""

  // update Student Assessment table (CAM_SRA) which holds module component resit marks
  // SRA_PRCS = Process Status - Value of I enables overall marks to be calculated in SITS
  // SRA_PROC = Current Process - Value of RAS enabled overall marks to be calculated in SITS
  // SRA_UDF2, SRA_UDF3 - user defined fields used for audit
  final def UpdateSITSResitFeedbackSql =
    f"""
		update $sitsSchema.cam_sra
		set sra_actm = :actualMark,
			sra_actg = :actualGrade,
			sra_prcs = 'I',
			sra_proc = 'RAS',
			sra_udf2 = '$tabulaIdentifier',
			sra_udf3 = :now
		$resitWriteableWhereClause
	"""

  abstract class ExportQuery(ds: DataSource, val query: String) extends SqlUpdate(ds, query) {
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

  class ExportFeedbackToSitsQuery(ds: DataSource) extends ExportQuery(ds, UpdateSITSFeedbackSql)
  class ExportResitFeedbackToSitsQuery(ds: DataSource) extends ExportQuery(ds, UpdateSITSResitFeedbackSql)

  final def PartialMatchingSasRecordsSql =
    f"""
      select sas_actm, sas_actg, sas_udf1 from $sitsSchema.cam_sas $whereClause
    """

  final def PartialMatchingSraRecordsSql =
    f"""
      select sra_actm, sra_actg, sra_udf2 from $sitsSchema.cam_sra $resitWhereClause
    """

  abstract class PartialMatchQuery(ds: DataSource, sql: String) extends MappingSqlQueryWithParameters[SITSMarkRow](ds, sql) {
    declareParameter(new SqlParameter("studentId", Types.VARCHAR))
    declareParameter(new SqlParameter("academicYear", Types.VARCHAR))
    declareParameter(new SqlParameter("moduleCodeMatcher", Types.VARCHAR))
    declareParameter(new SqlParameter("occurrences", Types.VARCHAR))
    declareParameter(new SqlParameter("sequences", Types.VARCHAR))
  }

  class SASPartialMatchQuery(ds: DataSource) extends PartialMatchQuery(ds, PartialMatchingSasRecordsSql) {
    override def mapRow(rs: ResultSet, rowNum: Int, parameters: Array[AnyRef], context: JMap[_, _]): SITSMarkRow = {
      SITSMarkRow(
        ImportMemberHelpers.getInteger(rs, "sas_actm"),
        rs.getString("sas_actg"),
        rs.getString("sas_udf1")
      )
    }
  }

  class SRAPartialMatchQuery(ds: DataSource) extends PartialMatchQuery(ds, PartialMatchingSraRecordsSql) {
    override def mapRow(rs: ResultSet, rowNum: Int, parameters: Array[AnyRef], context: JMap[_, _]): SITSMarkRow = {
      SITSMarkRow(
        ImportMemberHelpers.getInteger(rs, "sra_actm"),
        rs.getString("sra_actg"),
        rs.getString("sra_udf2")
      )
    }
  }

  case class SITSMarkRow(
    actualMark: Option[Int],
    actualGrade: String,
    uploader: String
  )

}

@Profile(Array("dev", "test", "production"))
@Service
class ExportFeedbackToSitsServiceImpl
  extends AbstractExportFeedbackToSitsService with AutowiringSitsDataSourceComponent

@Profile(Array("sandbox"))
@Service
class ExportFeedbackToSitsSandboxService extends ExportFeedbackToSitsService {
  def countMatchingSitsRecords(feedback: Feedback) = 0

  def exportToSits(feedback: Feedback) = 0

  def getPartialMatchingSITSRecords(feedback: Feedback): Seq[SITSMarkRow] = Nil
}
