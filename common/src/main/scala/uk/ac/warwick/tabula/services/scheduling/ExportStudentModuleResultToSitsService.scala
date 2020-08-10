package uk.ac.warwick.tabula.services.scheduling

import java.sql.{ResultSet, Types}

import javax.sql.DataSource
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.springframework.context.annotation.Profile
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.`object`.SqlUpdate
import org.springframework.jdbc.core.SqlParameter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.ModuleResult._
import uk.ac.warwick.tabula.data.model.{GradeBoundary, GradeBoundaryProcess, MarkState, ModuleRegistration, RecordedModuleRegistration}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent}
import uk.ac.warwick.tabula.services.scheduling.ExportFeedbackToSitsService.CountQuery
import uk.ac.warwick.tabula.services.scheduling.ExportStudentModuleResultToSitsService.{ExportStudentModuleResultToSitsUpdateQuery, SmoCountQuery, SmrQuery, SmrSubset}

trait ExportStudentModuleResultToSitsServiceComponent {
  def exportStudentModuleResultToSitsService: ExportStudentModuleResultToSitsService
}

trait AutowiringExportStudentModuleResultToSitsServiceComponent extends ExportStudentModuleResultToSitsServiceComponent {
  var exportStudentModuleResultToSitsService: ExportStudentModuleResultToSitsService = Wire[ExportStudentModuleResultToSitsService]
}

trait ExportStudentModuleResultToSitsService {
  def smoRecordExists(recordedModuleRegistration: RecordedModuleRegistration): Boolean
  def smrRecordSubdata(recordedModuleRegistration: RecordedModuleRegistration): Option[SmrSubset]
  def exportModuleMarksToSits(recordedModuleRegistration: RecordedModuleRegistration, finalAssessmentAttended: Boolean): Int
}

class AbstractExportStudentModuleResultToSitsService extends ExportStudentModuleResultToSitsService with Logging {
  self: SitsDataSourceComponent with AssessmentMembershipServiceComponent =>

  private def extractSmrSubsetData(recordedModuleRegistration: RecordedModuleRegistration): SmrSubset = {
    val latestResult = recordedModuleRegistration.latestResult
    val latestGrade = recordedModuleRegistration.latestGrade
    val isAgreedMark = recordedModuleRegistration.latestState.contains(MarkState.Agreed)

    val mr: ModuleRegistration = recordedModuleRegistration.moduleRegistration.getOrElse(throw new IllegalStateException(s"Expected ModuleRegistration record but 0 found for module mark $recordedModuleRegistration"))

    lazy val gradeBoundary = assessmentMembershipService.gradesForMark(mr, recordedModuleRegistration.latestMark).find(gb => latestGrade.contains(gb.grade))
    val requiresResit = gradeBoundary.exists(_.generatesResit)
    val incrementAttempt = gradeBoundary.exists(_.incrementsAttempt)

    val smrCredits: Option[BigDecimal] = latestResult match {
      case Some(Pass) => mr.safeCats
      case None => None // null this field if we don't have a result yet
      case _ => Some(BigDecimal(0))
    }

    val smrProcess: Option[String] = {
      if (isAgreedMark) {
        latestResult match {
          case Some(Pass) => Some("COM")
          case Some(Deferred) if latestGrade.contains(GradeBoundary.ForceMajeureMissingComponentGrade) => Some("COM")
          case Some(Fail) | Some(Deferred) => if(requiresResit) Some("RAS") else Some("COM")
          case _ => Some("SAS")
        }
      } else {
        if (mr.currentResitAttempt.nonEmpty) Some("RAS") else Some("SAS")
      }
    }

    val currentAttempt: Int = mr.currentResitAttempt.getOrElse(1)

    val smrCurrentAttempt: Int = {
      if (isAgreedMark && incrementAttempt) currentAttempt + 1
      else currentAttempt
    }

    val smrCompletedAttempt: Int =
      if (isAgreedMark) currentAttempt
      else currentAttempt - 1

    val smrSasStatus: Option[String] =
      if (mr.currentResitAttempt.nonEmpty || (isAgreedMark && requiresResit)) Some("R")
      else if (isAgreedMark) Some("A")
      else None

    val smrProcStatus: Option[String] = {
      if (isAgreedMark) {
        latestResult match {
          case Some(Pass) => Some("A")
          case _ => None
        }
      } else {
        latestResult match {
          case r if r.nonEmpty => Some("C")
          case _ => None
        }
      }
    }

    // Use the latest non-agreed mark (which may be the current) as the actual mark/grade
    // If we can't find a non-agreed mark, just use the agreed mark
    val actualRecordedMark =
      recordedModuleRegistration.marks.find(_.markState != MarkState.Agreed)
        .getOrElse(recordedModuleRegistration.marks.head)

    val smrActualMark: Option[Int] = actualRecordedMark.mark
    val smrAgreedMark: Option[Int] = recordedModuleRegistration.latestMark.filter(_ => isAgreedMark)

    val smrActualGrade: Option[String] = actualRecordedMark.grade
    val smrAgreedGrade: Option[String] = latestGrade.filter(_ => isAgreedMark)

    SmrSubset(
      sasStatus = smrSasStatus,
      processStatus = smrProcStatus,
      process = smrProcess,
      credits = smrCredits.map(_.underlying),
      currentAttempt = Option(smrCurrentAttempt),
      completedAttempt = Option(smrCompletedAttempt),
      actualMark = smrActualMark,
      actualGrade = smrActualGrade,
      agreedMark = smrAgreedMark,
      agreedGrade = smrAgreedGrade,
      result = recordedModuleRegistration.latestResult.map(_.dbValue)
    )
  }

  def keysParamaterMap(recordedModuleRegistration: RecordedModuleRegistration): JMap[String, Any] = {
    JHashMap(
      "sprCode" -> recordedModuleRegistration.sprCode,
      "moduleCode" -> recordedModuleRegistration.sitsModuleCode,
      "occurrence" -> recordedModuleRegistration.occurrence,
      "academicYear" -> recordedModuleRegistration.academicYear.toString,
    )
  }

  override def smoRecordExists(recordedModuleRegistration: RecordedModuleRegistration): Boolean = {
    val countQuery = new SmoCountQuery(sitsDataSource)
    val parameterMap: JMap[String, Any] = keysParamaterMap(recordedModuleRegistration)
    countQuery.getCount(parameterMap) > 0
  }

  override def smrRecordSubdata(recordedModuleRegistration: RecordedModuleRegistration): Option[SmrSubset] = {
    val smrExistingRowQuery = new SmrQuery(sitsDataSource)
    val parameterMap: JMap[String, Any] = keysParamaterMap(recordedModuleRegistration)
    try {
      val existingRow = smrExistingRowQuery.getExistingSmrRow(parameterMap)
      Some(existingRow)
    } catch {
      case _: EmptyResultDataAccessException => None
    }
  }

  override def exportModuleMarksToSits(recordedModuleRegistration: RecordedModuleRegistration, finalAssessmentAttended: Boolean): Int = {
    val updateQuery = new ExportStudentModuleResultToSitsUpdateQuery(sitsDataSource)

    val subsetData = extractSmrSubsetData(recordedModuleRegistration)
    val parameterMap: JMap[String, Any] = keysParamaterMap(recordedModuleRegistration)
    parameterMap.putAll(JHashMap(
      "currentAttemptNumber" -> subsetData.currentAttempt.orNull,
      "completedAttemptNumber" -> subsetData.completedAttempt.orNull,
      "moduleMarks" -> subsetData.actualMark.orNull,
      "moduleGrade" -> subsetData.actualGrade.orNull,
      "agreedModuleMarks" -> subsetData.agreedMark.orNull,
      "agreedModuleGrade" -> subsetData.agreedGrade.orNull,
      "credits" -> subsetData.credits.orNull,
      "currentDateTime" -> DateTimeFormat.forPattern("dd/MM/yy:HHmm").print(DateTime.now),
      "finalAssesmentsAttended" -> recordedModuleRegistration.latestMark.map(_ => if (finalAssessmentAttended) "Y" else "N").orNull,
      "dateTimeMarksUploaded" -> DateTime.now.toDate,
      "sraByDept" -> recordedModuleRegistration.latestMark.map(_ => "SRAs by dept").orNull,
      "moduleResult" -> subsetData.result.orNull,
      "initialSASStatus" -> subsetData.sasStatus.orNull,
      "processStatus" -> subsetData.processStatus.orNull,
      "process" -> subsetData.process.orNull,
      "dateTimeMarksUploaded" -> DateTime.now.toDate
    ))
    val rowUpdated = updateQuery.updateByNamedParam(parameterMap)
    if (rowUpdated == 0) {
      logger.warn(s"No SMR record found to update. Possible SAS hasn't yet generated initial SMR for $recordedModuleRegistration")
    }
    rowUpdated
  }

}

object ExportStudentModuleResultToSitsService {
  var sitsSchema: String = Wire.property("${schema.sits}")

  final def rootWhereClause: String =
    f"""
       |where spr_code = :sprCode
       |    and mod_code = :moduleCode
       |    and mav_occur = :occurrence
       |    and ayr_code = :academicYear
       |    and psl_code = 'Y'
       |""".stripMargin

  final def CountSmoRecordsSql: String =
    f"""
    select count(*) from $sitsSchema.cam_smo $rootWhereClause
    """


  final def SmrRecordSql: String =
    f"""
    select SMR_SASS, SMR_PRCS, SMR_PROC, SMR_CRED, SMR_CURA, SMR_COMA, SMR_ACTM, SMR_ACTG, SMR_AGRM, SMR_AGRG, SMR_RSLT from $sitsSchema.ins_smr $rootWhereClause
    """

  final def UpdateModuleResultSql: String =
    s"""
       |update $sitsSchema.ins_smr
       |  set SMR_CURA = :currentAttemptNumber,
       |      SMR_COMA = :completedAttemptNumber,
       |      SMR_ACTM = :moduleMarks,
       |      SMR_ACTG = :moduleGrade,
       |      SMR_AGRM = :agreedModuleMarks,
       |      SMR_AGRG = :agreedModuleGrade,
       |      SMR_CRED = :credits,
       |      SMR_UDF2 = :currentDateTime, -- dd/MM/yy:HHmm format used by MRM. We store the same date time in fasd. May be we don't need this?
       |      SMR_UDF3 = :finalAssesmentsAttended,
       |      SMR_UDFA = 'TABULA',
       |      SMR_UDF5 = :sraByDept,
       |      SMR_FASD = :dateTimeMarksUploaded,
       |      SMR_RSLT = :moduleResult, -- P/F/D/null values
       |      SMR_SASS = :initialSASStatus,
       |      SMR_PRCS = :processStatus,
       |      SMR_PROC = :process -- Updated for agreed marks also(mrm doesn't touch this field)
       |  $rootWhereClause
       |""".stripMargin

  class ExportStudentModuleResultToSitsUpdateQuery(ds: DataSource) extends SqlUpdate(ds, UpdateModuleResultSql) {

    declareParameter(new SqlParameter("sprCode", Types.VARCHAR))
    declareParameter(new SqlParameter("moduleCode", Types.VARCHAR))
    declareParameter(new SqlParameter("occurrence", Types.VARCHAR))
    declareParameter(new SqlParameter("academicYear", Types.VARCHAR))
    declareParameter(new SqlParameter("currentAttemptNumber", Types.INTEGER))
    declareParameter(new SqlParameter("completedAttemptNumber", Types.INTEGER))
    declareParameter(new SqlParameter("moduleMarks", Types.INTEGER))
    declareParameter(new SqlParameter("moduleGrade", Types.VARCHAR))
    declareParameter(new SqlParameter("agreedModuleMarks", Types.INTEGER))
    declareParameter(new SqlParameter("agreedModuleGrade", Types.VARCHAR))
    declareParameter(new SqlParameter("credits", Types.DECIMAL))
    declareParameter(new SqlParameter("currentDateTime", Types.VARCHAR))
    declareParameter(new SqlParameter("finalAssesmentsAttended", Types.VARCHAR))
    declareParameter(new SqlParameter("sraByDept", Types.VARCHAR))
    declareParameter(new SqlParameter("dateTimeMarksUploaded", Types.DATE))
    declareParameter(new SqlParameter("moduleResult", Types.VARCHAR))
    declareParameter(new SqlParameter("initialSASStatus", Types.VARCHAR))
    declareParameter(new SqlParameter("processStatus", Types.VARCHAR))
    declareParameter(new SqlParameter("process", Types.VARCHAR))

    compile()

  }

  class SmoCountQuery(ds: DataSource) extends CountQuery(ds) {
    def getCount(params: JMap[String, Any]): Int = {
      this.queryForObject(CountSmoRecordsSql, params, classOf[JInteger]).asInstanceOf[Int]
    }
  }

  /**
   * Student result record  can be amended as long as  -
   * SMR record exists in SITS. Exams Office may not yet have run SAS process if no SMR record found.
   * SMO record has to exist.
   */
  case class SmrSubset(
    sasStatus: Option[String],
    processStatus: Option[String],
    process: Option[String],
    credits: Option[JBigDecimal],
    currentAttempt: Option[Int],
    completedAttempt: Option[Int],
    agreedMark: Option[Int],
    agreedGrade: Option[String],
    actualMark: Option[Int],
    actualGrade: Option[String],
    result: Option[String],
  )

  class SmrQuery(ds: DataSource) extends NamedParameterJdbcTemplate(ds) {
    def getExistingSmrRow(params: JMap[String, Any]): SmrSubset = {
      this.queryForObject(SmrRecordSql, params, (rs: ResultSet, _: Int) => {
        def getNullableInt(column: String): Option[Int] = {
          val intValue = rs.getInt(column)
          if (rs.wasNull()) None else Some(intValue)
        }

        SmrSubset(
          sasStatus = rs.getString("SMR_SASS").maybeText,
          processStatus = rs.getString("SMR_PRCS").maybeText,
          process = rs.getString("SMR_PROC").maybeText,
          credits = Option(rs.getBigDecimal("SMR_CRED")),
          currentAttempt = getNullableInt("SMR_CURA"),
          completedAttempt = getNullableInt("SMR_COMA"),
          agreedMark = getNullableInt("SMR_AGRM"),
          agreedGrade = rs.getString("SMR_AGRG").maybeText,
          actualMark = getNullableInt("SMR_ACTM"),
          actualGrade = rs.getString("SMR_ACTG").maybeText,
          result = rs.getString("SMR_RSLT").maybeText
        )
      })
    }
  }

}

@Profile(Array("dev", "test", "production"))
@Service
class ExportStudentModuleResultToSitsServiceImpl
  extends AbstractExportStudentModuleResultToSitsService with AutowiringSitsDataSourceComponent with AutowiringAssessmentMembershipServiceComponent

@Profile(Array("sandbox"))
@Service
class ExportStudentModuleResultToSitsSandboxService extends ExportStudentModuleResultToSitsService {
  override def smoRecordExists(recordedModuleRegistration: RecordedModuleRegistration): Boolean = false
  override def smrRecordSubdata(recordedModuleRegistration: RecordedModuleRegistration): Option[SmrSubset] = None
  override def exportModuleMarksToSits(recordedModuleRegistration: RecordedModuleRegistration, finalAssessmentAttended: Boolean): Int = 0
}


