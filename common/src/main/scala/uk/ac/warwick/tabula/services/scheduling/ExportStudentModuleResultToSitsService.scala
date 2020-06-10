package uk.ac.warwick.tabula.services.scheduling

import java.sql.Types

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
import uk.ac.warwick.tabula.data.model.ModuleResult.{Deferred, Fail, Pass}
import uk.ac.warwick.tabula.data.model.{GradeBoundary, ModuleRegistration, RecordedModuleRegistration}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.scheduling.ExportFeedbackToSitsService.CountQuery
import uk.ac.warwick.tabula.services.scheduling.ExportStudentModuleResultToSitsService.{ExportStudentModuleResultToSitsUpdateQuery, SmoCountQuery, SmrQuery}

trait ExportStudentModuleResultToSitsServiceComponent {
  def exportStudentModuleResultToSitsService: ExportStudentModuleResultToSitsService
}

trait AutowiringExportStudentModuleResultToSitsServiceComponent extends ExportStudentModuleResultToSitsServiceComponent {
  var exportStudentModuleResultToSitsService: ExportStudentModuleResultToSitsService = Wire[ExportStudentModuleResultToSitsService]
}

trait ExportStudentModuleResultToSitsService {

  def exportModuleMarksToSits(recordedModuleRegistration: RecordedModuleRegistration): Int

}

class AbstractExportStudentModuleResultToSitsService extends ExportStudentModuleResultToSitsService with Logging {
  self: SitsDataSourceComponent =>

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
    currentAttempt: Option[JInteger],
    completedAttempt: Option[JInteger])


  private def extractSmrSubsetData(existingSmr: SmrSubset, recordedModuleRegistration: RecordedModuleRegistration, actualMarks: Boolean = true): SmrSubset = {

    val latestResult = recordedModuleRegistration.latestResult
    val latestGrade = recordedModuleRegistration.latestGrade
    val mr: ModuleRegistration = recordedModuleRegistration.moduleRegistration.getOrElse(throw new IllegalStateException(s"Expected ModuleRegistration record but 0 found for module mark $recordedModuleRegistration"))
    //if there is any resit row
    val currentResitAttempt = mr.currentResitAttempt
    val noResits = currentResitAttempt.isEmpty

    def smrCredits: Option[JBigDecimal] = latestResult match {
      //GradeBoundary.ForceMajeureMissingComponentGrade must have a result of Pass but grant zero credits.
      case Some(Pass) if !latestGrade.contains(GradeBoundary.ForceMajeureMissingComponentGrade) => Some(mr.cats)
      case _ => Some(new JBigDecimal(0))
    }

    def smrProcess: Option[String] = {
      if (actualMarks) {
        existingSmr.process
      } else {
        latestResult match {
          case Some(Pass) => Some("COM")
          case Some(Fail) => Some("RAS")
          case Some(Deferred) => if (latestGrade.exists(_.matches(("[S]")))) Some("RAS") else Some("SAS")
          case _ => None
        }
      }
    }

    def smrCurrentAttempt: Option[JInteger] = {
      if (actualMarks) {
        existingSmr.currentAttempt
      } else {
        val attempt = latestResult match {
          //Possibility of 1 or 2 currentResitAttempt value depending on Ist attempt or Resit  SRA record
          case Some(Pass) => if (noResits) Some(1) else mr.currentResitAttempt
          case Some(Fail) => mr.currentResitAttempt
          case Some(Deferred) => if (latestGrade.exists(_.matches(("[S]")))) {
            mr.currentResitAttempt // Ideally 1 value. The grade of S for further first attempt creates an SRA with an current attempt of 1.  SRA record generation is pending -TODO
          } else {
            existingSmr.currentAttempt.map(_.intValue) // Possible something on hold so we can leave it as it is
          }
          case _ => None
        }
        Some(JInteger(attempt))
      }
    }


    def smrCompletedAttempt: Option[JInteger] = {
      if (actualMarks) {
        existingSmr.completedAttempt
      } else {

        val attempt = latestResult match {
          case Some(Pass) => if (noResits) Some(1) else mr.currentResitAttempt
          case Some(Fail) => mr.currentResitAttempt.map(a => a - 1) //1 less than current attempt. CurrentResitAttempt(Ideally 2)  at this stage by SRA generation
          case Some(Deferred) => if (latestGrade.exists(_.matches(("[S]")))) {
            mr.currentResitAttempt // Ideally 1 value and both current and completed same
          } else {
            existingSmr.completedAttempt.map(_.intValue)
          }
          case _ => None
        }
        Some(JInteger(attempt))
      }
    }

    def smrSasStatus: Option[String] = latestResult match {
      case Some(Pass) | Some(Fail) => if (noResits) Some("A") else Some("R")
      case Some(Deferred) => if (latestGrade.exists(_.matches(("[S]")))) Some("R") else Some("H")
      case _ => None
    }

    def smrProcStatus: Option[String] = latestResult match {
      case Some(Pass) => Some("A")
      case Some(Fail) => None
      case Some(Deferred) => if (latestGrade.exists(_.matches(("[S]")))) None else Some("H")
      case _ => None
    }

    SmrSubset(
      sasStatus = smrSasStatus,
      processStatus = smrProcStatus,
      process = smrProcess,
      credits = smrCredits,
      currentAttempt = smrCurrentAttempt,
      completedAttempt = smrCompletedAttempt
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

  def SmoRecordExists(recordedModuleRegistration: RecordedModuleRegistration): Boolean = {
    val countQuery = new SmoCountQuery(sitsDataSource)
    val parameterMap: JMap[String, Any] = keysParamaterMap(recordedModuleRegistration)
    countQuery.getCount(parameterMap) > 0

  }


  def SmrRecordSubdata(recordedModuleRegistration: RecordedModuleRegistration): Option[SmrSubset] = {
    val smrExistingRowQuery = new SmrQuery(sitsDataSource)
    val parameterMap: JMap[String, Any] = keysParamaterMap(recordedModuleRegistration)
    try {
      val existingRow = smrExistingRowQuery.getExsitingSmrRow(parameterMap)
      Some(SmrSubset(Option(existingRow.get("SMR_SASS").asInstanceOf[String]), Option(existingRow.get("SMR_PRCS").asInstanceOf[String]),
        Option(existingRow.get("SMR_PROC").asInstanceOf[String]), Option(existingRow.get("SMR_CRED").asInstanceOf[JBigDecimal]),
        Option(existingRow.get("SMR_CURA").asInstanceOf[JInteger]), Option(existingRow.get("SMR_COMA").asInstanceOf[JInteger])))
    } catch {
      case e: EmptyResultDataAccessException => None
    }
  }


  def exportModuleMarksToSits(recordedModuleRegistration: RecordedModuleRegistration): Int = {
    val existingSmr = SmrRecordSubdata(recordedModuleRegistration)

    if (!SmoRecordExists(recordedModuleRegistration)) {
      logger.warn(s"SMO doesn't exists. Unable to update module mark record for $recordedModuleRegistration")
      0 //can throw an exception in case we want to report this to  user via UI
    } else if (existingSmr.isEmpty) {
      logger.warn(s"SMR entry not found. SAS process may not have been run. Unable to update module mark record for $recordedModuleRegistration")
      0
    } else {
      val updateQuery = new ExportStudentModuleResultToSitsUpdateQuery(sitsDataSource)

      //TODO currently dealing with actual marks
      val subsetData = extractSmrSubsetData(existingSmr.get, recordedModuleRegistration)

      val parameterMap: JMap[String, Any] = keysParamaterMap(recordedModuleRegistration)
      parameterMap.putAll(JHashMap(
        "currentAttemptNumber" -> subsetData.currentAttempt.orNull,
        "completedAttemptNumber" -> subsetData.completedAttempt.orNull,
        "moduleMarks" -> JInteger(recordedModuleRegistration.latestMark),
        "moduleGrade" -> recordedModuleRegistration.latestGrade.orNull,
        "agreedModuleMarks" -> null,
        "agreedModuleGrade" -> null,
        "credits" -> subsetData.credits.orNull,
        "currentDateTime" -> DateTimeFormat.forPattern("dd/MM/yy:HHmm").print(DateTime.now),
        "finalAssesmentsAttended" -> "Y", //TAB-8438
        "dateTimeMarksUploaded" -> DateTime.now.toDate,
        "moduleResult" -> recordedModuleRegistration.latestResult.map(_.dbValue).orNull,
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

}

object ExportStudentModuleResultToSitsService {
  val sitsSchema: String = Wire.property("${schema.sits}")

  final def rootWhereClause =
    f"""
       |where spr_code = :sprCode
       |    and mod_code like :moduleCode
       |    and mav_occur = :occurrence
       |    and ayr_code = :academicYear
       |    and psl_code = 'Y'
       |""".stripMargin

  final def CountSmoRecordsSql =
    f"""
    select count(*) from $sitsSchema.cam_smo $rootWhereClause
    """


  final def SmrRecordSql =
    f"""
    select SMR_PROC, SMR_PRCS, SMR_SASS, SMR_CURA, SMR_COMA, SMR_CRED from $sitsSchema.ins_smr $rootWhereClause
    """

  //TODO -Currently dealing with actual grade/marks only.
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
       |      SMR_UDF4 = 'TABULA',    --TODO- waiting confirmation from Roger but  doesn't look like anyone using it.
       |      SMR_UDF5 = 'SRAs by dept',
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

  class SmrQuery(ds: DataSource) extends NamedParameterJdbcTemplate(ds) {
    def getExsitingSmrRow(params: JMap[String, Any]): JMap[String, AnyRef] = {
      this.queryForMap(SmrRecordSql, params)
    }
  }

}

@Profile(Array("dev", "test", "production"))
@Service
class ExportStudentModuleResultToSitsServiceImpl
  extends AbstractExportStudentModuleResultToSitsService with AutowiringSitsDataSourceComponent

@Profile(Array("sandbox"))
@Service
class ExportStudentModuleResultToSitsSandboxService extends ExportStudentModuleResultToSitsService {

  def exportModuleMarksToSits(recordedModuleRegistration: RecordedModuleRegistration): Int = 0

}


