package uk.ac.warwick.tabula.services.scheduling

import java.sql.Types

import javax.sql.DataSource
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.SqlUpdate
import org.springframework.jdbc.core.SqlParameter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports.{JInteger, JMap}
import uk.ac.warwick.tabula.data.model.RecordedResit
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.scheduling.ExportResitsToSitsService._
import uk.ac.warwick.tabula.JavaImports._

trait ExportResitsToSitsServiceComponent {
  def exportResitsToSitsService: ExportResitsToSitsService
}

trait AutowiringExportResitsToSitsServiceComponent extends ExportResitsToSitsServiceComponent {
  var exportResitsToSitsService: ExportResitsToSitsService = Wire[ExportResitsToSitsService]
}

trait ExportResitsToSitsService {
  def exportToSits(resit: RecordedResit): Int
  def countMatchingSitsRecords(resit: RecordedResit): Int
}

object ExportResitsToSitsService {
  val sitsSchema: String = Wire.property("${schema.sits}")

  final def CreateResitRecordSql: String =
    s"""
       |insert into $sitsSchema.cam_sra
       |(
       |  spr_code,
       |  ayr_code,
       |  psl_code,
       |  mod_code,
       |  mav_occur,
       |  sra_seq,
       |  sra_rseq,
       |  mks_code,
       |  ast_code,
       |  sra_cura,
       |  sra_perc,
       |  sra_proc
       |) values (
       |  :sprCode,
       |  :academicYear,
       |  'Y',
       |  :moduleCode,
       |  :occurrence,
       |  :sequence,
       |  :resitSequence,
       |  :marksCode,
       |  :assessmentType,
       |  :attempt,
       |  :weighting,
       |  'RAS'
       |)
       |""".stripMargin

  class ResetModuleResultQuery(ds: DataSource) extends SqlUpdate(ds, CreateResitRecordSql) {
    declareParameter(new SqlParameter("sprCode", Types.VARCHAR))
    declareParameter(new SqlParameter("academicYear", Types.VARCHAR))
    declareParameter(new SqlParameter("moduleCode", Types.VARCHAR))
    declareParameter(new SqlParameter("occurrence", Types.VARCHAR))
    declareParameter(new SqlParameter("sequence", Types.VARCHAR))
    declareParameter(new SqlParameter("resitSequence", Types.VARCHAR))
    declareParameter(new SqlParameter("marksCode", Types.VARCHAR))
    declareParameter(new SqlParameter("assessmentType", Types.VARCHAR))
    declareParameter(new SqlParameter("attempt", Types.VARCHAR))
    declareParameter(new SqlParameter("weighting", Types.VARCHAR))
    compile()
  }

  final def CountMatchingBlankSraRecordsSql =
    f"""
    select count(*) from $sitsSchema.cam_sra
      where spr_code = :sprCode
      and mod_code like :moduleCode
      and mav_occur = :occurrence
      and ayr_code = :academicYear
      and psl_code = 'Y'
      and sra_seq = :sequence
      and sra_rseq = :resitSequence
    """

  class CountQuery(ds: DataSource) extends NamedParameterJdbcTemplate(ds) {
    def getCount(params: JMap[String, Any]): Int =
      this.queryForObject(CountMatchingBlankSraRecordsSql, params, classOf[JInteger]).asInstanceOf[Int]
  }


}

class RecordedResitsParameterGetter(resit: RecordedResit) {
  def getQueryParams: JMap[String, Any] = JHashMap(
    "sprCode" -> resit.sprCode,
    "academicYear" -> resit.academicYear.toString,
    "moduleCode" -> resit.moduleCode,
    "occurrence" -> resit.occurrence,
    "sequence" -> resit.sequence,
    "resitSequence" -> resit.resitSequence,
  )

  def getCreateParams: JMap[String, Any] = JHashMap(
    "sprCode" -> resit.sprCode,
    "academicYear" -> resit.academicYear.toString,
    "moduleCode" -> resit.moduleCode,
    "occurrence" -> resit.occurrence,
    "sequence" -> resit.sequence,
    "resitSequence" -> resit.resitSequence,
    "marksCode" -> resit.marksCode,
    "assessmentType" -> resit.assessmentType.astCode,
    "attempt" -> resit.currentResitAttempt.toString,
    "weighting" -> resit.weighting.toString,
  )
}

class AbstractExportResitsToSitsService extends ExportResitsToSitsService with Logging {
  self: SitsDataSourceComponent =>

  def exportToSits(resit: RecordedResit): Int = {
    val parameterGetter = new RecordedResitsParameterGetter(resit)
    val updateQuery = new ResetModuleResultQuery(sitsDataSource)
    val rowUpdated = updateQuery.updateByNamedParam(parameterGetter.getCreateParams)
    rowUpdated
  }

  override def countMatchingSitsRecords(resit: RecordedResit): Int = {
    val parameterGetter = new RecordedResitsParameterGetter(resit)
    val countQuery = new CountQuery(sitsDataSource)
    countQuery.getCount(parameterGetter.getQueryParams)
  }
}

@Profile(Array("dev", "test", "production"))
@Service
class ExportResitsToSitsServiceImpl
  extends AbstractExportResitsToSitsService with AutowiringSitsDataSourceComponent

@Profile(Array("sandbox"))
@Service
class ExportResitsToSitsSandboxService extends ExportResitsToSitsService {
  def exportToSits(resit: RecordedResit): Int = 0
  def countMatchingSitsRecords(resit: RecordedResit): Int = 0
}
