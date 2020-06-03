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
import uk.ac.warwick.tabula.JavaImports.{JBigDecimal, _}
import uk.ac.warwick.tabula.ScjCode
import uk.ac.warwick.tabula.data.model.ModuleResult.{Deferred, Fail, Pass}
import uk.ac.warwick.tabula.data.model.{ModuleResult, RecordedModuleMark, RecordedModuleRegistration}
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
  def exportToSits(recordedModuleRegistrationMark: RecordedModuleMark): Int

  def SmoRecordExists(recordedModuleRegistrationMark: RecordedModuleMark): Boolean

  def SmrProcessCompleted(recordedModuleRegistrationMark: RecordedModuleMark): Boolean

}

class AbstractExportStudentModuleResultToSitsService extends ExportStudentModuleResultToSitsService with Logging {
  self: SitsDataSourceComponent =>

  /**
   * Student result record  can be amended as long as  -
   * SMR record exists in SITS. Exams Office may not yet have run SAS process if no SMR record found.
   * SMR_PROC value should not be COM - completed - (the Exams Office needs to do any further changes if COM status)
   * SMO record has to exist.
   *
   * moduleResult -if (grade.matches("[P13ABC]")  || grade.equals("CP") // compensated pass
   * || grade.equals("21") // 2:1
   * || grade.equals("22") // 2:2
   * || grade.equals("A+")) {
   * // moduleResult - it's a pass  smr.setRslt("P")/smr.setSass("A") / smr.setPrcs("A");
   * if (agreed )    smr.setProc("COM")
   *
   * if (grade.matches("[DEFNWX]")  || grade.equals("QF"))  // qualified fail
   * // moduleResult - it's a fail  smr.setRslt("F")/smr.setSass("A") / smr.setPrcs("A"), 0 credits
   * if (agreed )    smr.setProc("COM")
   *
   * if (grade.matches("[SR]")) { // resit (permitted or forced)
   * // moduleResult - smr.setRslt("D")/smr.setSass("R") / smr.setPrcs(null), 0 credits
   * if (agreed )    smr.setProc("RAS")
   *
   * else if (grade.matches("[LM]") // late submission or mitigating circumstance
   * || grade.equals("PL") // plagiarism
   * || grade.equals("AM") // academic misconduct
   * || grade.equals("AB")) { // absent
   * // moduleResult - smr.setRslt("D")/smr.setSass("H") / smr.setPrcs("H"), 0 credits
   * if (agreed)  	smr.setProc("SAS");
   */
  case class SmrSubset(sasStatus: Option[String], processStatus: Option[String], process: Option[String], credits: JBigDecimal)


  private def extractInitialSASStatus(recordedModuleMark: RecordedModuleMark, actualMarks: Boolean = true): SmrSubset = {

    def smrCredits(result: Option[ModuleResult]): JBigDecimal = result match {
      case Some(Pass) => recordedModuleMark.recordedModuleRegistration.cats
      case _ => new JBigDecimal(0)
    }

    def smrProcess(result: Option[ModuleResult]): Option[String] = {
      if (actualMarks) {
        None
      } else {
        result match {
          case Some(Pass) | Some(Fail) => Some("COM")
          case Some(Deferred) => if (recordedModuleMark.grade.exists(_.matches(("[SR]")))) Some("RAS") else Some("SAS")
          case _ => None
        }
      }
    }

    recordedModuleMark.result match {
      case Some(Pass) | Some(Fail) => {
        SmrSubset(Some("A"), Some("A"), smrProcess(recordedModuleMark.result), smrCredits(recordedModuleMark.result))
      }
      case Some(Deferred) => if (recordedModuleMark.grade.exists(_.matches(("[SR]")))) { //resit (permitted or forced)

        SmrSubset(Some("R"), None, smrProcess(recordedModuleMark.result), new JBigDecimal(0))
      } else {
        SmrSubset(Some("H"), Some("H"), smrProcess(recordedModuleMark.result), new JBigDecimal(0))
      }
      case _ => SmrSubset(None, None, None, new JBigDecimal(0))
    }

  }

  def keysParamaterMap(recordedModuleRegistration: RecordedModuleRegistration): JMap[String, Any] = {
    JHashMap(
      "studentId" -> ScjCode.getUniversityId(recordedModuleRegistration.scjCode),
      "moduleCodeMatcher" -> (recordedModuleRegistration.module.code.toUpperCase + "%"),
      "occurrence" -> recordedModuleRegistration.occurrence,
      "academicYear" -> recordedModuleRegistration.academicYear.toString,
    )
  }

  def SmoRecordExists(recordedModuleMark: RecordedModuleMark): Boolean = {
    val countQuery = new SmoCountQuery(sitsDataSource)
    val parameterMap: JMap[String, Any] = keysParamaterMap(recordedModuleMark.recordedModuleRegistration)
    countQuery.getCount(parameterMap) > 0

  }

  def SmrProcessCompleted(recordedModuleMark: RecordedModuleMark): Boolean = {
    val countQuery = new SmrProcessCompletedCountQuery(sitsDataSource)
    val recordedModuleRegistration = recordedModuleMark.recordedModuleRegistration
    val parameterMap: JMap[String, Any] = keysParamaterMap(recordedModuleMark.recordedModuleRegistration)
    countQuery.getCount(parameterMap) > 0

  }

  def exportToSits(recordedModuleMark: RecordedModuleMark): Int = {
    val recordedModuleRegistration = recordedModuleMark.recordedModuleRegistration
    //If required we can throw an exception in case want to report dedicated errors to  user via UI
    if (!SmoRecordExists(recordedModuleMark)) {
      logger.warn(s"SMO doesn't exists. Unable to update module mark record for ${recordedModuleRegistration.scjCode}, ${recordedModuleRegistration.module}, ${recordedModuleRegistration.academicYear.toString}")
      0 //can throw an exception in case we want to report this to  user via UI
    } else if (SmrProcessCompleted(recordedModuleMark)) {
      logger.warn(s"The SMR (mark) record is already set to completed (COM). Exam office can do further changes. Unable to update module mark record for ${recordedModuleRegistration.scjCode}, ${recordedModuleRegistration.module}, ${recordedModuleRegistration.academicYear.toString}")
      0
    } else {
      val updateQuery = new ExportStudentModuleResultToSitsUpdateQuery(sitsDataSource)
      val recordedModuleRegistration = recordedModuleMark.recordedModuleRegistration
      //TODO currently dealing with actual marks
      val subsetData = extractInitialSASStatus(recordedModuleMark)

      val parameterMap: JMap[String, Any] = keysParamaterMap(recordedModuleMark.recordedModuleRegistration)
      parameterMap.putAll(JHashMap(
        "currentAttemptNumber" -> 1, //TODO set 1 currently (mandatory as per MRM  dept xml and set same value for current/completed attempt number fields)
        "completedAttemptNumber" -> 1,
        "moduleMarks" -> JInteger(recordedModuleMark.mark),
        "moduleGrade" -> recordedModuleMark.grade.orNull,
        "credits" -> subsetData.credits,
        "currentDateTime" -> DateTimeFormat.forPattern("dd/MM/yy:HHmm").print(DateTime.now),
        "finalAssesmentsAttended" -> "Y", //TODO  Required for `HEFCE` return by ARO. MRM gathers it via xml. Value should be “Y” if the studentattended the chronologically last assessment for the module, and “N” otherwise
        "dateTimeMarksUploaded" -> DateTime.now.toDate,
        "moduleResult" -> recordedModuleMark.result.map(_.dbValue).orNull,
        "initialSASStatus" -> subsetData.sasStatus.orNull,
        "processStatus" -> subsetData.processStatus.orNull,
        "process" -> subsetData.process.orNull,
        "dateTimeMarksUploaded" -> DateTime.now.toDate
      ))
      val rowUpdated = updateQuery.updateByNamedParam(parameterMap)
      if (rowUpdated == 0) {
        logger.warn(s"No SMR record found to update. Possible SAS hasn't generated initial SMR for ${recordedModuleRegistration.scjCode}, ${recordedModuleRegistration.module}, ${recordedModuleRegistration.academicYear.toString}, ${recordedModuleRegistration.occurrence}")
      }
      rowUpdated
    }

  }

}

object ExportStudentModuleResultToSitsService {
  val sitsSchema: String = Wire.property("${schema.sits}")

  final def rootWhereClause =
    f"""
       |where spr_code in (select spr_code from $sitsSchema.ins_spr where spr_stuc = :studentId)
       |    and mod_code like :moduleCodeMatcher
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
       |      SMR_COMA = :completedAttemptNumber, -- MRM currently sets both current and completed attempt as same  for SMR but this may not be correct?
       |      SMR_ACTM = :moduleMarks,
       |      SMR_ACTG = :moduleGrade,
       |      SMR_CRED = :credits,
       |      SMR_UDF2 = :currentDateTime, -- dd/MM/yy:HHmm format used by MRM. We store the same date time in fasd. May be we don't need this?
       |      SMR_UDF3 = :finalAssesmentsAttended,
       |      SMR_UDF5 = 'SRAs by dept', -- MRM rule: If true  via xml value as "SRAs by dept" else "SRAs by EO". This will always be by dept now. May be we can leave the field?
       |      SMR_FASD = :dateTimeMarksUploaded,
       |      SMR_RSLT = :moduleResult, -- P/F/D/null values
       |      SMR_SASS = :initialSASStatus,
       |      SMR_PRCS = :processStatus,
       |      SMR_PROC = :process
       |  $rootWhereClause
       |""".stripMargin

  class ExportStudentModuleResultToSitsUpdateQuery(ds: DataSource) extends SqlUpdate(ds, UpdateModuleResultSql) {

    declareParameter(new SqlParameter("studentId", Types.VARCHAR))
    declareParameter(new SqlParameter("moduleCodeMatcher", Types.VARCHAR))
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
  def exportToSits(recordedModuleRegistrationMark: RecordedModuleMark): Int = 0

  def SmoRecordExists(recordedModuleRegistrationMark: RecordedModuleMark): Boolean = true

  def SmrProcessCompleted(recordedModuleRegistrationMark: RecordedModuleMark): Boolean = true

}


