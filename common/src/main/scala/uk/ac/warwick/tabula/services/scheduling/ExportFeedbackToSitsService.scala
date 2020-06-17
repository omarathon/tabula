package uk.ac.warwick.tabula.services.scheduling

import java.sql.{ResultSet, Types}

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
import uk.ac.warwick.tabula.data.model.{AssessmentGroup, Feedback, RecordedAssessmentComponentStudent}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.scheduling.ExportFeedbackToSitsService._

import scala.jdk.CollectionConverters._

trait ExportFeedbackToSitsServiceComponent {
  def exportFeedbackToSitsService: ExportFeedbackToSitsService
}

trait AutowiringExportFeedbackToSitsServiceComponent extends ExportFeedbackToSitsServiceComponent {
  var exportFeedbackToSitsService: ExportFeedbackToSitsService = Wire[ExportFeedbackToSitsService]
}

trait ExportFeedbackToSitsService {
  def countMatchingSitsRecords(feedback: Feedback): Int
  def countMatchingSitsRecords(student: RecordedAssessmentComponentStudent, resit: Boolean): Int
  def exportToSits(feedback: Feedback): Int
  def exportToSits(student: RecordedAssessmentComponentStudent, resit: Boolean): Int
  def getPartialMatchingSITSRecords(feedback: Feedback): Seq[ExportFeedbackToSitsService.SITSMarkRow]
}

class FeedbackParameterGetter(feedback: Feedback) {
  val assessGroups: Seq[AssessmentGroup] = feedback.assessmentGroups
  val possibleOccurrenceSequencePairs: Seq[(String, String)] = assessGroups.map(assessGroup => (assessGroup.occurrence, assessGroup.assessmentComponent.sequence))

  def getQueryParams: Option[JMap[String, Any]] = possibleOccurrenceSequencePairs match {
    case pairs if pairs.isEmpty => None
    case _ => Option(JHashMap(
      // for the where clause
      "studentId" -> feedback.studentIdentifier,
      "academicYear" -> feedback.academicYear.toString,
      "moduleCodeMatcher" -> (feedback.module.code.toUpperCase + "%"),

      // in theory we should look for a record with occurrence and sequence from the same pair,
      // but in practice there won't be any ambiguity since the record is already determined
      // by student, module code and year
      "occurrences" -> possibleOccurrenceSequencePairs.map(_._1).asJava,
      "sequences" -> possibleOccurrenceSequencePairs.map(_._2).asJava
    ))
  }

  def getUpdateParams(mark: Int, grade: String): Option[JMap[String, Any]] = possibleOccurrenceSequencePairs match {
    case pairs if pairs.isEmpty => None
    case _ => Option(JHashMap(
      // for the where clause
      "studentId" -> feedback.studentIdentifier,
      "academicYear" -> feedback.academicYear.toString,
      "moduleCodeMatcher" -> (feedback.module.code.toUpperCase + "%"),
      "now" -> DateTime.now.toDate,

      // in theory we should look for a record with occurrence and sequence from the same pair,
      // but in practice there won't be any ambiguity since the record is already determined
      // by student, module code and year
      "occurrences" -> possibleOccurrenceSequencePairs.map(_._1).asJava,
      "sequences" -> possibleOccurrenceSequencePairs.map(_._2).asJava,

      // data to insert
      "actualMark" -> mark,
      "actualGrade" -> grade
    ))
  }
}

class RecordedAssessmentComponentStudentParameterGetter(student: RecordedAssessmentComponentStudent) {
  def getQueryParams: JMap[String, Any] = JHashMap(
    // for the where clause
    "studentId" -> student.universityId,
    "academicYear" -> student.academicYear.toString,
    "moduleCodeMatcher" -> student.moduleCode,
    "occurrences" -> Seq(student.occurrence).asJava,
    "sequences" -> Seq(student.sequence).asJava
  )

  def getUpdateParams: JMap[String, Any] = JHashMap(
    // for the where clause
    "studentId" -> student.universityId,
    "academicYear" -> student.academicYear.toString,
    "moduleCodeMatcher" -> student.moduleCode,
    "occurrences" -> Seq(student.occurrence).asJava,
    "sequences" -> Seq(student.sequence).asJava,

    // data to insert
    "now" -> DateTime.now.toDate,
    "actualMark" -> JInteger(student.latestMark),
    "actualGrade" -> student.latestGrade.orNull
  )

  def getResetModuleResultParams(process: String): JMap[String, Any] = JHashMap(
    "studentId" -> student.universityId,
    "academicYear" -> student.academicYear.toString,
    "moduleCodeMatcher" -> student.moduleCode,
    "occurrences" -> Seq(student.occurrence).asJava,
    "process" -> process,
  )
}


class AbstractExportFeedbackToSitsService extends ExportFeedbackToSitsService with Logging {
  self: SitsDataSourceComponent =>

  private def countInternal(feedback: Feedback, tableName: String, query: CountQuery): Int = {
    val parameterGetter: FeedbackParameterGetter = new FeedbackParameterGetter(feedback)
    parameterGetter.getQueryParams match {
      case Some(params) =>
        query.getCount(params)
      case None =>
        logger.warn(s"Cannot upload feedback ${feedback.id} for SITS as no $tableName records found")
        0
    }
  }

  private def countInternal(student: RecordedAssessmentComponentStudent, query: CountQuery): Int = {
    val parameterGetter = new RecordedAssessmentComponentStudentParameterGetter(student)
    query.getCount(parameterGetter.getQueryParams)
  }

  private def countMatchingSasRecords(feedback: Feedback): Int = {
    val countQuery = new SasCountQuery(sitsDataSource)
    countInternal(feedback, "SAS", countQuery)
  }

  private def countMatchingSasRecords(student: RecordedAssessmentComponentStudent): Int = {
    val countQuery = new SasCountQuery(sitsDataSource)
    countInternal(student, countQuery)
  }

  private def countMatchingSraRecords(feedback: Feedback): Int = {
    val countQuery = new SraCountQuery(sitsDataSource)
    countInternal(feedback, "SRA", countQuery)
  }

  private def countMatchingSraRecords(student: RecordedAssessmentComponentStudent): Int = {
    val countQuery = new SraCountQuery(sitsDataSource)
    countInternal(student, countQuery)
  }

  def countMatchingSitsRecords(feedback: Feedback): Int = feedback match {
    case f: Feedback if f.assignment.resitAssessment => countMatchingSraRecords(feedback: Feedback)
    case _ => countMatchingSasRecords(feedback: Feedback)
  }

  override def countMatchingSitsRecords(student: RecordedAssessmentComponentStudent, resit: Boolean): Int =
    if (resit) countMatchingSraRecords(student)
    else countMatchingSasRecords(student)

  def exportToSits(feedback: Feedback): Int = {
    val parameterGetter: FeedbackParameterGetter = new FeedbackParameterGetter(feedback)
    val (updateQuery, tableName) = feedback match {
      case f: Feedback if f.assignment.resitAssessment => (new ExportResitFeedbackToSitsQuery(sitsDataSource), "CAM_SRA")
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

  override def exportToSits(student: RecordedAssessmentComponentStudent, resit: Boolean): Int = {
    val parameterGetter = new RecordedAssessmentComponentStudentParameterGetter(student)
    val updateQuery =
      if (resit) new ExportResitFeedbackToSitsQuery(sitsDataSource)
      else new ExportFeedbackToSitsQuery(sitsDataSource)

    updateQuery.updateByNamedParam(parameterGetter.getUpdateParams)
  }

  def getPartialMatchingSITSRecords(feedback: Feedback): Seq[ExportFeedbackToSitsService.SITSMarkRow] = {

    val (matchQuery, tableName) = feedback match {
      case f: Feedback if f.assignment.resitAssessment => (new SRAPartialMatchQuery(sitsDataSource), "CAM_SRA")
      case _ => (new SASPartialMatchQuery(sitsDataSource), "CAM_SAS")
    }

    val parameterGetter: FeedbackParameterGetter = new FeedbackParameterGetter(feedback)
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

  final def CountMatchingBlankSasRecordsSql =
    f"""
    select count(*) from $sitsSchema.cam_sas $whereClause
    """

  final def CountMatchingBlankSraRecordsSql =
    f"""
    select count(*) from $sitsSchema.cam_sra $resitWhereClause
    """

  abstract class CountQuery(ds: DataSource) extends NamedParameterJdbcTemplate(ds) {
    def getCount(params: JMap[String, Any]): Int
  }

  class SasCountQuery(ds: DataSource) extends CountQuery(ds) {
    def getCount(params: JMap[String, Any]): Int = {
      this.queryForObject(CountMatchingBlankSasRecordsSql, params, classOf[JInteger]).asInstanceOf[Int]
    }
  }

  class SraCountQuery(ds: DataSource) extends CountQuery(ds) {
    def getCount(params: JMap[String, Any]): Int = {
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
      sas_agrm = null,
      sas_agrg = null,
      sas_prcs = 'I',
      sas_proc = 'SAS',
      sas_udf1 = '$tabulaIdentifier',
      sas_udf2 = :now
    $whereClause
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
      sra_agrm = null,
      sra_agrg = null,
      sra_prcs = 'I',
      sra_proc = 'RAS',
      sra_udf2 = '$tabulaIdentifier',
      sra_udf3 = :now
    $resitWhereClause
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
  def countMatchingSitsRecords(student: RecordedAssessmentComponentStudent, resit: Boolean): Int = 0
  def exportToSits(feedback: Feedback) = 0
  def exportToSits(student: RecordedAssessmentComponentStudent, resit: Boolean): Int = 0
  def getPartialMatchingSITSRecords(feedback: Feedback): Seq[SITSMarkRow] = Nil
}
