package uk.ac.warwick.tabula.services.scheduling

import java.sql.Types

import javax.sql.DataSource
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.SqlUpdate
import org.springframework.jdbc.core.SqlParameter
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.ModuleResult.{Deferred, Fail, Pass}
import uk.ac.warwick.tabula.data.model.{GradeBoundary, RecordedModuleRegistration}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.scheduling.ExportFeedbackToSitsService.CountQuery
import uk.ac.warwick.tabula.services.scheduling.ExportStudentModuleResultToSitsService.{ExportStudentModuleResultToSitsUpdateQuery, SmoCountQuery, SmrProcessCompletedCountQuery}

trait ExportStudentModuleResultToSitsServiceComponent {
  def exportStudentModuleResultToSitsService: ExportStudentModuleResultToSitsService
}

trait AutowiringExportStudentModuleResultToSitsServiceComponent extends ExportStudentModuleResultToSitsServiceComponent {
  var exportStudentModuleResultToSitsService: ExportStudentModuleResultToSitsService = Wire[ExportStudentModuleResultToSitsService]
}

trait ExportStudentModuleResultToSitsService {

  def exportModuleMarksToSits(recordedModuleRegistration: RecordedModuleRegistration): Int

  def SmoRecordExists(recordedModuleRegistration: RecordedModuleRegistration): Boolean


}

class AbstractExportStudentModuleResultToSitsService extends ExportStudentModuleResultToSitsService with Logging {
  self: SitsDataSourceComponent =>

  /**
   * Student result record  can be amended as long as  -
   * SMR record exists in SITS. Exams Office may not yet have run SAS process if no SMR record found.
   * SMO record has to exist.

   * Pass moduleResult -  smr.setSass("A") / smr.setPrcs("A"). O credits for ForceMajeureMissingComponentGrade. For agreed, smr.setProc("COM") ;
   * Fail moduleResult -  smr.setSass("A") / smr.setPrcs("A"), 0 credits. For agreed, smr.setProc("COM")
   * Deferred  moduleResult with  grade.matches("[SR]" -  smr.setSass("R") / smr.setPrcs(null), 0 credits. For agreed, smr.setProc("RAS")
   * Deferred  moduleResult with  grade other than above -  smr.setSass("H") / smr.setPrcs("H"), 0 credits. For agreed, smr.setProc("SAS")
   */
  case class SmrSubset(sasStatus: Option[String], processStatus: Option[String], process: Option[String], credits: JBigDecimal)


  private def extractInitialSASStatus(recordedModuleRegistration: RecordedModuleRegistration, actualMarks: Boolean = true): SmrSubset = {

    val latestResult = recordedModuleRegistration.latestResult
    val latestGrade = recordedModuleRegistration.latestGrade

    def smrCredits: JBigDecimal = latestResult match {

      //GradeBoundary.ForceMajeureMissingComponentGrade must have a result of Pass but grant zero credits.
      case Some(Pass) if !latestGrade.contains(GradeBoundary.ForceMajeureMissingComponentGrade) =>  recordedModuleRegistration.moduleRegistration.map(_.cats).getOrElse(new JBigDecimal(0))
      case _ => new JBigDecimal(0)
    }

    def smrProcess: Option[String] = {
        latestResult match {
          case Some(Pass) | Some(Fail) => Some("COM")
          case Some(Deferred) => if (latestGrade.exists(_.matches(("[SR]")))) Some("RAS") else Some("SAS")
          case _ => None
        }
    }

    latestResult match {
      case Some(Pass) | Some(Fail) => {
        SmrSubset(Some("A"), Some("A"), smrProcess, smrCredits)
      }
      case Some(Deferred) => if (latestGrade.exists(_.matches(("[SR]")))) { //resit (permitted or forced)
        SmrSubset(Some("R"), None, smrProcess, new JBigDecimal(0))
      } else {
        SmrSubset(Some("H"), Some("H"), smrProcess, new JBigDecimal(0))
      }
      case _ => SmrSubset(None, None, None, new JBigDecimal(0))
    }

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


  def exportModuleMarksToSits(recordedModuleRegistration: RecordedModuleRegistration): Int = {
    if (!SmoRecordExists(recordedModuleRegistration)) {
      logger.warn(s"SMO doesn't exists. Unable to update module mark record for ${recordedModuleRegistration.sprCode}, ${recordedModuleRegistration.sitsModuleCode}, ${recordedModuleRegistration.academicYear.toString}")
      0 //can throw an exception in case we want to report this to  user via UI
    } else {
      val updateQuery = new ExportStudentModuleResultToSitsUpdateQuery(sitsDataSource)

      //TODO currently dealing with actual marks
      val subsetData = extractInitialSASStatus(recordedModuleRegistration)

      val parameterMap: JMap[String, Any] = keysParamaterMap(recordedModuleRegistration)
      parameterMap.putAll(JHashMap(
        "currentAttemptNumber" -> 1, //TODO set 1 currently (mandatory as per MRM  dept xml and set same value for current/completed attempt number fields).Waiting for confirmation from exams
        "completedAttemptNumber" -> 1,
        "moduleMarks" -> JInteger(recordedModuleRegistration.latestMark),
        "moduleGrade" -> recordedModuleRegistration.latestGrade.orNull,
        "credits" -> subsetData.credits,
        "currentDateTime" -> DateTimeFormat.forPattern("dd/MM/yy:HHmm").print(DateTime.now),
        "finalAssesmentsAttended" -> "Y", //TODO  Required for `HEFCE` return by ARO. MRM gathers it via xml. Value should be “Y” if the studentattended the chronologically last assessment for the module, and “N” otherwise
        "dateTimeMarksUploaded" -> DateTime.now.toDate,
        "moduleResult" -> recordedModuleRegistration.latestResult.map(_.dbValue).orNull,
        "initialSASStatus" -> subsetData.sasStatus.orNull,
        "processStatus" -> subsetData.processStatus.orNull,
        "process" -> subsetData.process.orNull,
        "dateTimeMarksUploaded" -> DateTime.now.toDate
      ))
      val rowUpdated = updateQuery.updateByNamedParam(parameterMap)
      if (rowUpdated == 0) {
        logger.warn(s"No SMR record found to update. Possible SAS hasn't generated initial SMR for ${recordedModuleRegistration.sprCode}, ${recordedModuleRegistration.sitsModuleCode}, ${recordedModuleRegistration.academicYear.toString}, ${recordedModuleRegistration.occurrence}")
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


  final def CountSmrProcessCompletedSql =
    f"""
    select count(*) from $sitsSchema.ins_smr $rootWhereClause  and smr_proc = 'COM'
    """

  //TODO -Currently dealing with actual grade/marks only.
  final def UpdateModuleResultSql: String =
    s"""
       |update $sitsSchema.ins_smr
       |  set SMR_CURA = :currentAttemptNumber,
       |      SMR_COMA = :completedAttemptNumber,
       |      SMR_ACTM = :moduleMarks,
       |      SMR_ACTG = :moduleGrade,
       |      SMR_CRED = :credits,
       |      SMR_UDF2 = :currentDateTime, -- dd/MM/yy:HHmm format used by MRM. We store the same date time in fasd. May be we don't need this?
       |      SMR_UDF3 = :finalAssesmentsAttended,
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

  class SmrProcessCompletedCountQuery(ds: DataSource) extends CountQuery(ds) {
    def getCount(params: JMap[String, Any]): Int = {
      this.queryForObject(CountSmrProcessCompletedSql, params, classOf[JInteger]).asInstanceOf[Int]
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

  def SmoRecordExists(recordedModuleRegistration: RecordedModuleRegistration): Boolean = true

}


