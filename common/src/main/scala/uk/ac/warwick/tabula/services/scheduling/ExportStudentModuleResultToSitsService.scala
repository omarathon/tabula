package uk.ac.warwick.tabula.services.scheduling

import java.sql.Types

import javax.sql.DataSource
import org.joda.time.DateTime
import org.springframework.jdbc.`object`.SqlUpdate
import org.springframework.jdbc.core.SqlParameter
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports.JHashMap
import uk.ac.warwick.tabula.data.model.RecordedModuleMark
import uk.ac.warwick.tabula.services.scheduling.ExportStudentModuleResultToSitsService.ExportStudentModuleResultToSitsInsertQuery

trait ExportStudentModuleResultToSitsServiceComponent {
  def exportStudentModuleResultToSitsService: ExportStudentModuleResultToSitsService
}

trait AutowiringExportStudentModuleResultToSitsServiceComponent extends ExportStudentModuleResultToSitsServiceComponent {
  var exportStudentModuleResultToSitsService: ExportStudentModuleResultToSitsService = Wire[ExportStudentModuleResultToSitsService]
}

trait ExportStudentModuleResultToSitsService {
  //TODO based on RecordedModuleMarke for the student
  def exportToSits(recordedModuleRegistrationMark: RecordedModuleMark): Int

}

class AbstractExportStudentModuleResultToSitsService extends ExportStudentModuleResultToSitsService {
  self: SitsDataSourceComponent =>

  /**
   * SMR_UDF2 - date when marks updated
   * SMR_UDF3/Final Assess Attended - This flag has been included in the overall module mark upload to provide the necessary information for our statutory
   * return to the Higher Education Funding Council of England (HEFCE), submitted on the University’s behalf by the Academic Registrar’s Office.
   * The value should be “Y” if the studentattended the chronologically last assessment for the module, and “N” otherwise.
   * SMR_UDF5 - if (true set by dept) value as smr.setUdf5("SRAs by dept");
   * else smr.setUdf5("SRAs by EO");
   *
   * Overallmarks cane be amended as long as  -
   *  SMR record exists in SITS, if not we can't update. Exams Office may not yet have run SAS process.
   *  SMR_PROC value should not be null and COM - completed - (the Exams Office needs to any further changes if COM)
   *  SMO record has to exist.
   *
   *
   * moduleResult -if (grade.matches("[P13ABC]")  || grade.equals("CP") // compensated pass
   * || grade.equals("21") // 2:1
   * || grade.equals("22") // 2:2
   * || grade.equals("A+")) {
   * // moduleResult - it's a pass  smr.setRslt("P")/smr.setSass("A") / smr.setPrcs("A");
   * if (agreed )    smr.setProc("COM")
   *
   *
   * if (grade.matches("[DEFNWX]")  || grade.equals("QF"))  // qualified fail
   * // fail
   * // moduleResult - it's a fail  smr.setRslt("F")/smr.setSass("A") / smr.setPrcs("A"), 0 credits
   * if (agreed )    smr.setProc("COM")
   *
   *
   *  if (grade.matches("[SR]")) { // resit (permitted or forced)
   *  // moduleResult - smr.setRslt("D")/smr.setSass("R") / smr.setPrcs(null), 0 credits
   * if (agreed )    smr.setProc("RAS")
   *
   *
   * else if (grade.matches("[LM]") // late submission or mitigating circumstance
   * || grade.equals("PL") // plagiarism
   * || grade.equals("AM") // academic misconduct
   * || grade.equals("AB")) { // absent
   * // moduleResult - smr.setRslt("D")/smr.setSass("H") / smr.setPrcs("H"), 0 credits
   * if (agreed)  	smr.setProc("SAS");
   */


  def exportToSits(recordedModuleMark: RecordedModuleMark): Int = {

    val insertQuery = new ExportStudentModuleResultToSitsInsertQuery(sitsDataSource)
    val recordedModuleRegistration = recordedModuleMark.recordedModuleRegistration
    //select spr_code from $sitsSchema.ins_spr where spr_stuc = :studentId
    val parameterMap = JHashMap(
      ("sprCode", recordedModuleRegistration.scjCode),  //TODO they may vary for course transfer,  extract spr -Need injecting ProfileService  on RecordedModuleMark and then extract -getStudentCourseDetailsByScjCode on
      ("modCode", recordedModuleRegistration.module.code),
      ("mavOccur", recordedModuleRegistration.occurrence),
      ("academicYear", recordedModuleRegistration.academicYear.toString),
      ("attemptNumber", 1), //TODO set 1 currently, 2 and onwards means resits (mandatory by dept xml)
      ("overallModuleMarks", recordedModuleMark.mark), //TODO set as actual currently
        ("overallActualMarks", recordedModuleMark.mark),
        ("moduleResult", "P"), //TODO based on grade
        ("initialSASStatus", "A"),//TODO based on grade as 'P' set 'A'
        ("processStatus", "A"),//TODO based on grade as 'P' set 'A'
        ("process", "CON"),//TODO only when agreed marks/grades filled, for grade `SR` it is `RAS`
        ("credits", recordedModuleRegistration.cats),//TODO - for pass result set to credits otherwise 0 credits
        ("agreedGrade", recordedModuleMark.grade), //TODO does this need setting?
        ("actualGrade", recordedModuleMark.grade),
        ("dateTime", DateTime.now.toDate),
        ("finalAssesmentsAttended", "Y"), //TODO valid values  Y/N ( mandatory and provided  by dept xml)
        ("sraInfo", DateTime.now.toDate),
        ("dateTimeMarksUploaded",  DateTime.now.toDate)
    )
    //TODO
   1
  }
}

object ExportStudentModuleResultToSitsService {
  val sitsSchema: String = Wire.property("${schema.sits}")

  // insert into Student Absence (SAB) table
  final def InsertToSITSSql =
    f"""
		insert into $sitsSchema.ins_smr
		(SPR_CODE,MOD_CODE,MAV_OCCUR,AYR_CODE,PSL_CODE,SMR_CURA,SMR_COMA,SMR_AGRM,SMR_ACTM,SMR_RSLT,SMR_SASS, SMR_PRCS, SMR_PROC, SMR_CRED, SMR_AGRG, SMR_ACTG, SMR_UDF2, SMR_UDF3, SMR_UDF5, SMR_FASD)
		values (:sprCode, :modCode, :mavOccur, :academicYear, :pslCode, :attemptNumber, :attemptNumber, :overallModuleMarks,
		:overallActualMarks, :moduleResult, :initialSASStatus, :processStatus, :process, :credits, :agreedGrade, :actualGrade, :dateTime, :finalAssesmentsAttended, :sraInfo, :dateTimeMarksUploaded)
	"""

  class ExportStudentModuleResultToSitsInsertQuery(ds: DataSource) extends SqlUpdate(ds, InsertToSITSSql) {

    declareParameter(new SqlParameter("sprCode", Types.VARCHAR))
    declareParameter(new SqlParameter("modCode", Types.VARCHAR))
    declareParameter(new SqlParameter("macOccur", Types.DATE))
    declareParameter(new SqlParameter("academicYear", Types.VARCHAR))
    declareParameter(new SqlParameter("pslCode", Types.VARCHAR))
    declareParameter(new SqlParameter("attemptNumber", Types.VARCHAR))
    declareParameter(new SqlParameter("overallModuleMarks", Types.VARCHAR))
    declareParameter(new SqlParameter("overallActualMarks", Types.VARCHAR))
    declareParameter(new SqlParameter("moduleResult", Types.VARCHAR))
    declareParameter(new SqlParameter("initialSASStatus", Types.VARCHAR))
    declareParameter(new SqlParameter("processStatus", Types.VARCHAR))
    declareParameter(new SqlParameter("process", Types.VARCHAR))
    declareParameter(new SqlParameter("credits", Types.VARCHAR))
    declareParameter(new SqlParameter("agreedGrade", Types.VARCHAR))
    declareParameter(new SqlParameter("actualGrade", Types.VARCHAR))
    declareParameter(new SqlParameter("dateTime", Types.VARCHAR))
    declareParameter(new SqlParameter("finalAssesmentsAttended", Types.VARCHAR))
    declareParameter(new SqlParameter("sraInfo", Types.VARCHAR))
    declareParameter(new SqlParameter("dateTimeMarksUploaded", Types.DATE))

    compile()

  }


  class ExportExistingStudentModuleResultToSitsService(ds: DataSource) extends SqlUpdate(ds, InsertToSITSSql) {

    declareParameter(new SqlParameter("sprCode", Types.VARCHAR))
    declareParameter(new SqlParameter("modCode", Types.VARCHAR))
    declareParameter(new SqlParameter("macOccur", Types.DATE))
    declareParameter(new SqlParameter("academicYear", Types.VARCHAR))
    declareParameter(new SqlParameter("pslCode", Types.VARCHAR))
    declareParameter(new SqlParameter("attemptNumber", Types.VARCHAR))
    declareParameter(new SqlParameter("overallModuleMarks", Types.VARCHAR))
    declareParameter(new SqlParameter("overallActualMarks", Types.VARCHAR))
    declareParameter(new SqlParameter("moduleResult", Types.VARCHAR))
    declareParameter(new SqlParameter("initialSASStatus", Types.VARCHAR))
    declareParameter(new SqlParameter("processStatus", Types.VARCHAR))
    declareParameter(new SqlParameter("credits", Types.VARCHAR))
    declareParameter(new SqlParameter("agreedGrade", Types.VARCHAR))
    declareParameter(new SqlParameter("actualGrade", Types.VARCHAR))
    declareParameter(new SqlParameter("dateTime", Types.VARCHAR))
    declareParameter(new SqlParameter("finalAssesmentsAttended", Types.VARCHAR))
    declareParameter(new SqlParameter("sraInfo", Types.VARCHAR))
    declareParameter(new SqlParameter("dateTimeMarksUploaded", Types.DATE))

    compile()

  }

}


