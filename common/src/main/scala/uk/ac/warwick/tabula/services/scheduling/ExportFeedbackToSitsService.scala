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
import uk.ac.warwick.tabula.data.model.{AssessmentGroup, Feedback, MarkState, RecordedAssessmentComponentStudent, RecordedAssessmentComponentStudentMark, UpstreamAssessmentGroupMember, UpstreamAssessmentGroupMemberAssessmentType}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent}
import uk.ac.warwick.tabula.services.scheduling.ExportFeedbackToSitsService._

import scala.jdk.CollectionConverters._

trait ExportFeedbackToSitsServiceComponent {
  def exportFeedbackToSitsService: ExportFeedbackToSitsService
}

trait AutowiringExportFeedbackToSitsServiceComponent extends ExportFeedbackToSitsServiceComponent {
  var exportFeedbackToSitsService: ExportFeedbackToSitsService = Wire[ExportFeedbackToSitsService]
}

trait ExportFeedbackToSitsService {
  def countMatchingSitsRecords(student: RecordedAssessmentComponentStudent): Int
  def exportToSits(student: RecordedAssessmentComponentStudent, member: Option[UpstreamAssessmentGroupMember]): Int
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
      "resitSequenceMatcher" -> "%", // We don't hold this for Feedback

      // in theory we should look for a record with occurrence and sequence from the same pair,
      // but in practice there won't be any ambiguity since the record is already determined
      // by student, module code and year
      "occurrences" -> possibleOccurrenceSequencePairs.map(_._1).asJava,
      "sequences" -> possibleOccurrenceSequencePairs.map(_._2).asJava
    ))
  }
}

class RecordedAssessmentComponentStudentParameterGetter(student: RecordedAssessmentComponentStudent) {
  def getQueryParams: JMap[String, Any] = JHashMap(
    // for the where clause
    "studentId" -> student.universityId,
    "academicYear" -> student.academicYear.toString,
    "moduleCodeMatcher" -> student.moduleCode,
    "resitSequenceMatcher" -> student.resitSequence.getOrElse("%"),
    "occurrences" -> Seq(student.occurrence).asJava,
    "sequences" -> Seq(student.sequence).asJava
  )

  // If we've got an agreed mark/grade, store the last recorded mark/grade before it was agreed as the actual mark/grade
  lazy val latestRecordedActualMark: Option[RecordedAssessmentComponentStudentMark] = student.marks.find(_.markState != MarkState.Agreed)
  lazy val actualMark: JInteger = JInteger(latestRecordedActualMark match {
    case Some(m) => m.mark
    case _ => student.latestMark
  })
  lazy val actualGrade: String = (latestRecordedActualMark match {
    case Some(m) => m.grade
    case _ => student.latestGrade
  }).orNull

  def getUpdateParams: JMap[String, Any] = JHashMap(
    // for the where clause
    "studentId" -> student.universityId,
    "academicYear" -> student.academicYear.toString,
    "moduleCodeMatcher" -> student.moduleCode,
    "resitSequenceMatcher" -> student.resitSequence.getOrElse("%"),
    "occurrences" -> Seq(student.occurrence).asJava,
    "sequences" -> Seq(student.sequence).asJava,

    // data to insert
    "now" -> DateTime.now.toDate,
    "actualMark" -> actualMark,
    "actualGrade" -> actualGrade,
    "agreedMark" -> JInteger(student.latestMark.filter(_ => student.latestState.contains(MarkState.Agreed))),
    "agreedGrade" -> student.latestGrade.filter(_ => student.latestState.contains(MarkState.Agreed)).orNull
  )
}


class AbstractExportFeedbackToSitsService extends ExportFeedbackToSitsService with Logging {
  self: SitsDataSourceComponent with AssessmentMembershipServiceComponent =>

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

  override def countMatchingSitsRecords(student: RecordedAssessmentComponentStudent): Int = student.assessmentType match {
    case UpstreamAssessmentGroupMemberAssessmentType.OriginalAssessment =>
      countMatchingSasRecords(student)

    case UpstreamAssessmentGroupMemberAssessmentType.Reassessment =>
      countMatchingSraRecords(student)
  }

  override def exportToSits(student: RecordedAssessmentComponentStudent, member: Option[UpstreamAssessmentGroupMember]): Int = {
    val parameterGetter = new RecordedAssessmentComponentStudentParameterGetter(student)
    val agreed = student.latestState.contains(MarkState.Agreed)
    val reassessmentExpected = agreed && {
      val assessmentComponent = member.flatMap(_.upstreamAssessmentGroup.assessmentComponent)
      val currentResitAttempt = member.flatMap(_.currentResitAttempt)
      val gradeBoundaries = assessmentComponent.map(ac => assessmentMembershipService.gradesForMark(ac, student.latestMark, currentResitAttempt)).getOrElse(Nil)
      gradeBoundaries.find(gb => student.latestGrade.contains(gb.grade)).exists(_.generatesResit)
    }

    val updateQuery = student.assessmentType match {
      case UpstreamAssessmentGroupMemberAssessmentType.OriginalAssessment =>
        new ExportFeedbackToSitsQuery(sitsDataSource, agreed, reassessmentExpected)

      case UpstreamAssessmentGroupMemberAssessmentType.Reassessment =>
        new ExportResitFeedbackToSitsQuery(sitsDataSource, agreed, reassessmentExpected)
    }

    updateQuery.updateByNamedParam(parameterGetter.getUpdateParams)
  }

  override def getPartialMatchingSITSRecords(feedback: Feedback): Seq[ExportFeedbackToSitsService.SITSMarkRow] = {
    val (matchQuery, tableName) = feedback match {
      case f: Feedback if f.assignment.resitAssessment => (new SRAPartialMatchQuery(sitsDataSource), "CAM_SRA")
      case _ => (new SASPartialMatchQuery(sitsDataSource), "CAM_SAS")
    }

    val parameterGetter: FeedbackParameterGetter = new FeedbackParameterGetter(feedback)
    parameterGetter.getQueryParams match {
      case Some(params) =>
        matchQuery.executeByNamedParam(params).asScala.toSeq
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

  // :resitSequenceMatcher = '%' is stupid but necessary as the number of bind params needs to match
  def whereClause = s"$rootWhereClause and mab_seq in (:sequences) and '%' = :resitSequenceMatcher" // mab_seq = sequence code determining an assessment component
  def resitWhereClause = s"$rootWhereClause and sra_seq in (:sequences) and sra_rseq like :resitSequenceMatcher" // sra_seq = sequence code determining an assessment component

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
  final def UpdateSITSFeedbackSql(agreed: Boolean, reassessment: Boolean): String = {

    val (processStatus, currentProcess) = if (agreed) {
      if(reassessment) ("R", "RAS") else ("A", "COM")
    } else {
      ("I", "SAS")
    }

    f"""
    update $sitsSchema.cam_sas
    set sas_actm = :actualMark,
      sas_actg = :actualGrade,
      sas_agrm = :agreedMark,
      sas_agrg = :agreedGrade,
      sas_prcs = '$processStatus',
      sas_proc = '$currentProcess',
      ${if (agreed) "sas_coma = sas_cura," else ""}
      sas_udf1 = '$tabulaIdentifier',
      sas_udf2 = :now
    $whereClause
    """
  }

  // update Student Assessment table (CAM_SRA) which holds module component resit marks
  // SRA_PRCS = Process Status - Value of I enables overall marks to be calculated in SITS
  // SRA_PROC = Current Process - Value of RAS enabled overall marks to be calculated in SITS
  // SRA_UDF2, SRA_UDF3 - user defined fields used for audit
  final def UpdateSITSResitFeedbackSql(agreed: Boolean, reassessment: Boolean): String = {

    val (processStatus, currentProcess) = if (agreed) {
      if(reassessment) ("R", "RAS") else ("A", "COM")
    } else {
      ("I", "RAS")
    }

    f"""
    update $sitsSchema.cam_sra
    set sra_actm = :actualMark,
      sra_actg = :actualGrade,
      sra_agrm = :agreedMark,
      sra_agrg = :agreedGrade,
      sra_prcs = '$processStatus',
      sra_proc = '$currentProcess',
      ${if (agreed) "sra_coma = sra_cura," else ""}
      sra_udf2 = '$tabulaIdentifier',
      sra_udf3 = :now
    $resitWhereClause
    """
  }

  abstract class ExportQuery(ds: DataSource, val query: String) extends SqlUpdate(ds, query) {
    declareParameter(new SqlParameter("actualMark", Types.INTEGER))
    declareParameter(new SqlParameter("actualGrade", Types.VARCHAR))
    declareParameter(new SqlParameter("agreedMark", Types.INTEGER))
    declareParameter(new SqlParameter("agreedGrade", Types.VARCHAR))
    declareParameter(new SqlParameter("studentId", Types.VARCHAR))
    declareParameter(new SqlParameter("academicYear", Types.VARCHAR))
    declareParameter(new SqlParameter("moduleCodeMatcher", Types.VARCHAR))
    declareParameter(new SqlParameter("resitSequenceMatcher", Types.VARCHAR))
    declareParameter(new SqlParameter("now", Types.DATE))
    declareParameter(new SqlParameter("occurrences", Types.VARCHAR))
    declareParameter(new SqlParameter("sequences", Types.VARCHAR))

    compile()
  }

  class ExportFeedbackToSitsQuery(ds: DataSource, agreed: Boolean, reassessment: Boolean) extends ExportQuery(ds, UpdateSITSFeedbackSql(agreed, reassessment))
  class ExportResitFeedbackToSitsQuery(ds: DataSource, agreed: Boolean, reassessment: Boolean) extends ExportQuery(ds, UpdateSITSResitFeedbackSql(agreed, reassessment))

  final def PartialMatchingSasRecordsSql =
    f"""
      select sas_actm, sas_actg, sas_agrm, sas_agrg, sas_udf1 from $sitsSchema.cam_sas $whereClause
    """

  final def PartialMatchingSraRecordsSql =
    f"""
      select sra_actm, sra_actg, sra_agrm, sra_agrg, sra_udf2 from $sitsSchema.cam_sra $resitWhereClause
    """

  abstract class PartialMatchQuery(ds: DataSource, sql: String) extends MappingSqlQueryWithParameters[SITSMarkRow](ds, sql) {
    declareParameter(new SqlParameter("studentId", Types.VARCHAR))
    declareParameter(new SqlParameter("academicYear", Types.VARCHAR))
    declareParameter(new SqlParameter("moduleCodeMatcher", Types.VARCHAR))
    declareParameter(new SqlParameter("resitSequenceMatcher", Types.VARCHAR))
    declareParameter(new SqlParameter("occurrences", Types.VARCHAR))
    declareParameter(new SqlParameter("sequences", Types.VARCHAR))
  }

  class SASPartialMatchQuery(ds: DataSource) extends PartialMatchQuery(ds, PartialMatchingSasRecordsSql) {
    override def mapRow(rs: ResultSet, rowNum: Int, parameters: Array[AnyRef], context: JMap[_, _]): SITSMarkRow = {
      SITSMarkRow(
        ImportMemberHelpers.getInteger(rs, "sas_actm"),
        rs.getString("sas_actg"),
        ImportMemberHelpers.getInteger(rs, "sas_agrm"),
        rs.getString("sas_agrg"),
        rs.getString("sas_udf1")
      )
    }
  }

  class SRAPartialMatchQuery(ds: DataSource) extends PartialMatchQuery(ds, PartialMatchingSraRecordsSql) {
    override def mapRow(rs: ResultSet, rowNum: Int, parameters: Array[AnyRef], context: JMap[_, _]): SITSMarkRow = {
      SITSMarkRow(
        ImportMemberHelpers.getInteger(rs, "sra_actm"),
        rs.getString("sra_actg"),
        ImportMemberHelpers.getInteger(rs, "sra_agrm"),
        rs.getString("sra_agrg"),
        rs.getString("sra_udf2")
      )
    }
  }

  case class SITSMarkRow(
    actualMark: Option[Int],
    actualGrade: String,
    agreedMark: Option[Int],
    agreedGrade: String,
    uploader: String
  )

}

@Profile(Array("dev", "test", "production"))
@Service
class ExportFeedbackToSitsServiceImpl
  extends AbstractExportFeedbackToSitsService with AutowiringSitsDataSourceComponent with AutowiringAssessmentMembershipServiceComponent

@Profile(Array("sandbox"))
@Service
class ExportFeedbackToSitsSandboxService extends ExportFeedbackToSitsService {
  override def countMatchingSitsRecords(student: RecordedAssessmentComponentStudent): Int = 0
  override def exportToSits(student: RecordedAssessmentComponentStudent, member: Option[UpstreamAssessmentGroupMember]): Int = 0
  override def getPartialMatchingSITSRecords(feedback: Feedback): Seq[SITSMarkRow] = Nil
}
