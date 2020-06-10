package uk.ac.warwick.tabula.services.scheduling

import java.sql.{ResultSet, Types}

import javax.sql.DataSource
import org.apache.commons.lang3.builder.{EqualsBuilder, HashCodeBuilder, ToStringBuilder, ToStringStyle}
import org.joda.time.DateTime
import org.springframework.beans.{BeanWrapper, PropertyAccessorFactory}
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.jdbc.core.SqlParameter
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports.{JBigDecimal, JList, JMap}
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.commands.scheduling.imports.{ImportMemberHelpers, ImportModuleRegistrationsCommand}
import uk.ac.warwick.tabula.data.model.CourseType.PGT
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{Daoisms, StudentCourseDetailsDao}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.scheduling.PropertyCopying
import uk.ac.warwick.tabula.sandbox.SandboxData
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.services.marks.AutowiringModuleRegistrationMarksServiceComponent
import uk.ac.warwick.tabula.services.scheduling.ModuleRegistrationImporter.{ModuleRegistrationsByAcademicYearsQuery, ModuleRegistrationsByUniversityIdsQuery}
import uk.ac.warwick.tabula.{AcademicYear, AutowiringFeaturesComponent, Features}

import scala.jdk.CollectionConverters._
import scala.math.BigDecimal.RoundingMode

/**
 * Import module registration data from SITS.
 *
 */
trait ModuleRegistrationImporter {
  def getModuleRegistrationRowsForAcademicYears(academicYears: Seq[AcademicYear]): Seq[ModuleRegistrationRow]
  def getModuleRegistrationRowsForUniversityIds(universityIds: Seq[String]): Seq[ModuleRegistrationRow]
}

trait AbstractModuleRegistrationImporter extends ModuleRegistrationImporter with Logging {

  var studentCourseDetailsDao: StudentCourseDetailsDao = Wire[StudentCourseDetailsDao]
  var moduleAndDepartmentService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]

  protected def applyForRows(rows: Seq[ModuleRegistrationRow]): Seq[ImportModuleRegistrationsCommand] = {
    val tabulaModules: Set[Module] = rows.groupBy(_.sitsModuleCode).flatMap { case (sitsModuleCode, moduleRows) =>
      moduleAndDepartmentService.getModuleBySitsCode(sitsModuleCode) match {
        case None =>
          logger.warn(s"No stem module for $sitsModuleCode found in Tabula for SPR: ${moduleRows.map(_.sprCode).distinct.mkString(", ")}")
          None
        case Some(module) => Some(module)
      }
    }.toSet
    val tabulaModuleCodes = tabulaModules.map(_.code)
    val rowsBySCD: Map[StudentCourseDetails, Seq[ModuleRegistrationRow]] = rows.groupBy(_.sprCode).map { case (sprCode, sprRows) =>
      val allBySpr = studentCourseDetailsDao.getBySprCode(sprCode)

      allBySpr.find(_.mostSignificant).orElse(allBySpr.headOption).getOrElse {
        logger.error("Can't record module registration - could not find a StudentCourseDetails for SPR " + sprCode)
        null
      } -> sprRows.filter(row => {
        val moduleCode = Module.stripCats(row.sitsModuleCode)
        moduleCode.isDefined && tabulaModuleCodes.contains(moduleCode.get.toLowerCase)
      })
    }
    rowsBySCD.view.filterKeys(_ != null).map { case (scd, scdRows) => new ImportModuleRegistrationsCommand(scd, scdRows, tabulaModules) }.toSeq
  }
}

@Profile(Array("dev", "test", "production"))
@Service
class ModuleRegistrationImporterImpl extends AbstractModuleRegistrationImporter with AutowiringSitsDataSourceComponent with TaskBenchmarking with AutowiringFeaturesComponent {
  lazy val moduleRegistrationsByAcademicYearsQuery: ModuleRegistrationsByAcademicYearsQuery = new ModuleRegistrationsByAcademicYearsQuery(sitsDataSource)
  lazy val moduleRegistrationsByUniversityIdsQuery: ModuleRegistrationsByUniversityIdsQuery = new ModuleRegistrationsByUniversityIdsQuery(sitsDataSource)

  private def yearsToImportArray(yearsToImport: Seq[AcademicYear]): JList[String] = yearsToImport.map(_.toString).asJava: JList[String]
  def getModuleRegistrationRowsForAcademicYears(academicYears: Seq[AcademicYear]): Seq[ModuleRegistrationRow] =
    benchmarkTask("Fetch module registrations") {
      val currentAcademicYears = if (features.includeSMSForCurrentYear && academicYears.intersect(AcademicYear.allCurrent()).nonEmpty) {
        yearsToImportArray(academicYears.intersect(AcademicYear.allCurrent()))
      } else Seq("").asJava //set blank for SMS table to be ignored in the actual SQL
      val paraMap = JMap(
        "academicYears" -> yearsToImportArray(academicYears),
        "currentAcademicYears" -> currentAcademicYears
      )
      moduleRegistrationsByAcademicYearsQuery.executeByNamedParam(paraMap).asScala.distinct.toSeq
    }

  def getModuleRegistrationRowsForUniversityIds(universityIds: Seq[String]): Seq[ModuleRegistrationRow] =
    benchmarkTask("Fetch module registrations") {
      val currentAcademicYears = if (features.includeSMSForCurrentYear) {
        yearsToImportArray(AcademicYear.allCurrent())
      } else Seq("").asJava

      universityIds.grouped(Daoisms.MaxInClauseCountOracle).flatMap { ids =>
        val paraMap = JMap(
          "universityIds" -> ids.asJava,
          "currentAcademicYears" -> currentAcademicYears
        )
        moduleRegistrationsByUniversityIdsQuery.executeByNamedParam(paraMap).asScala.distinct.toSeq
      }.toSeq
    }
}

@Profile(Array("sandbox"))
@Service
class SandboxModuleRegistrationImporter extends AbstractModuleRegistrationImporter
  with AutowiringModuleRegistrationMarksServiceComponent {

  override def getModuleRegistrationRowsForAcademicYears(academicYears: Seq[AcademicYear]): Seq[ModuleRegistrationRow] =
    SandboxData.Departments.values
      .flatMap(_.routes.values)
      .flatMap(route => route.studentsStartId to route.studentsEndId)
      .flatMap(universityId => studentModuleRegistrationRows(universityId.toString))
      .filter(mr => academicYears.map(_.toString).contains(mr.academicYear))
      .toSeq

  override def getModuleRegistrationRowsForUniversityIds(universityIds: Seq[String]): Seq[ModuleRegistrationRow] =
    universityIds.flatMap(studentModuleRegistrationRows)

  def studentModuleRegistrationRows(universityId: String): Seq[ModuleRegistrationRow] = uk.ac.warwick.tabula.data.Transactions.transactional() {
    val yearOfStudy = ((universityId.toLong % 3) + 1).toInt

    (for {
      (_, d) <- SandboxData.Departments
      route <- d.routes.values.toSeq
      if (route.studentsStartId to route.studentsEndId).contains(universityId.toInt)
      moduleCode <- route.moduleCodes if route.courseType == PGT || moduleCode.substring(3, 4).toInt <= yearOfStudy
    } yield {
      val module = SandboxData.module(moduleCode)
      val isPassFail = moduleCode.takeRight(1) == "9" // modules with a code ending in 9 are pass/fails
      val marksCode =
        if (isPassFail) "TABULA-PF"
        else route.degreeType match {
          case DegreeType.Postgraduate => "TABULA-PG"
          case _ => "TABULA-UG"
        }

      val academicYear = if (route.courseType == PGT) AcademicYear.now() else AcademicYear.now() - (yearOfStudy - moduleCode.substring(3, 4).toInt)

      val recordedModuleRegistration: Option[RecordedModuleRegistration] =
        moduleRegistrationMarksService.getAllRecordedModuleRegistrations(module.fullModuleCode,  academicYear, "A")
          .find(_.sprCode == "%s/1".format(universityId))

      val (mark, grade, result) =
        // generate marks for all years for HOM students
        if (route.code.substring(0, 2) == "hm" || academicYear < AcademicYear.now()) {
          val randomMark = SandboxData.randomMarkSeed(universityId, moduleCode) % 100

          val m =
            if (isPassFail) {
              if (randomMark < 40) 0 else 100
            } else randomMark

          val g =
            if (isPassFail) if (m == 100) "P" else "F"
            else SandboxData.GradeBoundaries.find(gb => gb.marksCode == marksCode && gb.isValidForMark(Some(m))).map(_.grade).getOrElse("F")

          (Some(m), g, if (m < 40) "F" else "P")
        } else (None: Option[Int], null: String, null: String)

      recordedModuleRegistration.filter(_.needsWritingToSits).foreach { r =>
        r.needsWritingToSits = false
        r.lastWrittenToSits = Some(DateTime.now)
        moduleRegistrationMarksService.saveOrUpdate(r)
      }

      new ModuleRegistrationRow(
        sprCode = "%s/1".format(universityId),
        sitsModuleCode = module.fullModuleCode,
        cats = module.cats.underlying(),
        assessmentGroup = "A",
        selectionStatusCode = if (route.coreModules.contains(moduleCode)) "C" else "O",
        occurrence = "A",
        academicYear = academicYear.toString,
        actualMark = recordedModuleRegistration.flatMap(_.latestMark).orElse(mark),
        actualGrade = recordedModuleRegistration.flatMap(_.latestGrade).getOrElse(grade),
        agreedMark = if (academicYear < AcademicYear.now()) mark else None,
        agreedGrade = if (academicYear < AcademicYear.now()) grade else null,
        marksCode = marksCode,
        moduleResult = result,
        endWeek = None
      )
    }).toSeq
  }
}

object ModuleRegistrationImporter {
  val sitsSchema: String = Wire.property("${schema.sits}")

  var features: Features = Wire[Features]

  // union 2 things -
  // 1. unconfirmed module registrations from the SMS table
  // 2. confirmed module registrations from the SMO table

  def UnconfirmedModuleRegistrationsForAcademicYear =
    s"""
      select
      spr.spr_code,
      sms.mod_code,
      sms.sms_mcrd as credit,
      sms.sms_agrp as assess_group,
      sms.ses_code,
      sms.ayr_code,
      sms.sms_occl as occurrence,
      smr_actm, -- actual overall module mark
      smr_actg, -- actual overall module grade
      smr_agrm, -- agreed overall module mark
      smr_agrg, -- agreed overall module grade
      smr_mksc, -- mark scheme - used to work out if this is a pass/fail module
      smr_rslt, -- result of module
      mav_endw  -- end week of occurrence

      from $sitsSchema.cam_sms sms

      join $sitsSchema.ins_spr spr
        on spr.spr_code = sms.spr_code

      join ins_mod mod on mod.mod_code = sms.mod_code

      left join $sitsSchema.ins_smr smr -- Student Module Result
        on sms.spr_code = smr.spr_code
        and sms.ayr_code = smr.ayr_code
        and sms.mod_code = smr.mod_code
        and sms.sms_occl = smr.mav_occur

      left join $sitsSchema.cam_mav mav
        on mav.ayr_code = sms.ayr_code
        and mav.mod_code = sms.mod_code
        and mav.mav_occur = sms.sms_occl

       where sms.ayr_code in (:currentAcademicYears)"""

  def ConfirmedModuleRegistrationsForAcademicYear =
    s"""
      select
      spr.spr_code,
      smo.mod_code,
      smo.smo_mcrd as credit,
      smo.smo_agrp as assess_group,
      smo.ses_code,
      smo.ayr_code,
      smo.mav_occur as occurrence,
      smr_actm, -- actual overall module mark
      smr_actg, -- actual overall module grade
      smr_agrm, -- agreed overall module mark
      smr_agrg, -- agreed overall module grade
      smr_mksc, -- mark scheme - used to work out if this is a pass/fail module
      smr_rslt, -- result of module
      mav_endw  -- end week of occurrence


      from $sitsSchema.cam_smo smo

      join $sitsSchema.ins_spr spr
        on spr.spr_code = smo.spr_code

      join ins_mod mod on mod.mod_code = smo.mod_code

      left join $sitsSchema.ins_smr smr -- Student Module Result
        on smo.spr_code = smr.spr_code
        and smo.ayr_code = smr.ayr_code
        and smo.mod_code = smr.mod_code
        and smo.mav_occur = smr.mav_occur

      left join $sitsSchema.cam_mav mav
        on mav.ayr_code = smo.ayr_code
        and mav.mod_code = smo.mod_code
        and mav.mav_occur = smo.mav_occur

       where
       (smo.smo_rtsc is null or (smo.smo_rtsc not like 'X%' and smo.smo_rtsc != 'Z')) -- exclude WMG cancelled registrations
       and smo.ayr_code in (:academicYears)"""

  def UnconfirmedModuleRegistrationsForUniversityIds =
    s"""
      select
      spr.spr_code,
      sms.mod_code,
      sms.sms_mcrd as credit,
      sms.sms_agrp as assess_group,
      sms.ses_code,
      sms.ayr_code,
      sms.sms_occl as occurrence,
      smr_actm, -- actual overall module mark
      smr_actg, -- actual overall module grade
      smr_agrm, -- agreed overall module mark
      smr_agrg, -- agreed overall module grade
      smr_mksc, -- mark scheme - used to work out if this is a pass/fail module
      smr_rslt, -- result of module
      mav_endw  -- end week of occurrence

      from $sitsSchema.cam_sms sms

      join $sitsSchema.ins_spr spr
        on spr.spr_code = sms.spr_code

      join ins_mod mod on mod.mod_code = sms.mod_code

      left join $sitsSchema.ins_smr smr -- Student Module Result
        on sms.spr_code = smr.spr_code
        and sms.ayr_code = smr.ayr_code
        and sms.mod_code = smr.mod_code
        and sms.sms_occl = smr.mav_occur

      left join $sitsSchema.cam_mav mav
        on mav.ayr_code = sms.ayr_code
        and mav.mod_code = sms.mod_code
        and mav.mav_occur = sms.sms_occl

       where spr.spr_stuc in (:universityIds) and sms.ayr_code in (:currentAcademicYears)"""

  def ConfirmedModuleRegistrationsForUniversityIds =
    s"""
      select
      spr.spr_code,
      smo.mod_code,
      smo.smo_mcrd as credit,
      smo.smo_agrp as assess_group,
      smo.ses_code,
      smo.ayr_code,
      smo.mav_occur as occurrence,
      smr_actm, -- actual overall module mark
      smr_actg, -- actual overall module grade
      smr_agrm, -- agreed overall module mark
      smr_agrg, -- agreed overall module grade
      smr_mksc, -- mark scheme - used to work out if this is a pass/fail module
      smr_rslt, -- result of module
      mav_endw  -- end week of occurrence


      from $sitsSchema.cam_smo smo

      join $sitsSchema.ins_spr spr
        on spr.spr_code = smo.spr_code

      join ins_mod mod on mod.mod_code = smo.mod_code

      left join $sitsSchema.ins_smr smr -- Student Module Result
        on smo.spr_code = smr.spr_code
        and smo.ayr_code = smr.ayr_code
        and smo.mod_code = smr.mod_code
        and smo.mav_occur = smr.mav_occur

      left join $sitsSchema.cam_mav mav
        on mav.ayr_code = smo.ayr_code
        and mav.mod_code = smo.mod_code
        and mav.mav_occur = smo.mav_occur

       where
       (smo.smo_rtsc is null or (smo.smo_rtsc not like 'X%' and smo.smo_rtsc != 'Z')) -- exclude WMG cancelled registrations
       and spr.spr_stuc in (:universityIds)"""


  def mapResultSet(resultSet: ResultSet): ModuleRegistrationRow = {
    def getNullableInt(column: String): Option[Int] = {
      val intValue = resultSet.getInt(column)
      if (resultSet.wasNull()) None else Some(intValue)
    }

    new ModuleRegistrationRow(
      resultSet.getString("spr_code"),
      resultSet.getString("mod_code"),
      resultSet.getBigDecimal("credit"),
      resultSet.getString("assess_group"),
      resultSet.getString("ses_code"),
      resultSet.getString("occurrence"),
      resultSet.getString("ayr_code"),
      getNullableInt("smr_actm"),
      resultSet.getString("smr_actg"),
      getNullableInt("smr_agrm"),
      resultSet.getString("smr_agrg"),
      resultSet.getString("smr_mksc"),
      resultSet.getString("smr_rslt"),
      ImportMemberHelpers.getInteger(resultSet, "mav_endw")
    )
  }

  class ModuleRegistrationsByAcademicYearsQuery(ds: DataSource)
    extends MappingSqlQuery[ModuleRegistrationRow](ds, s"$UnconfirmedModuleRegistrationsForAcademicYear union $ConfirmedModuleRegistrationsForAcademicYear") {
    declareParameter(new SqlParameter("academicYears", Types.VARCHAR))
    declareParameter(new SqlParameter("currentAcademicYears", Types.VARCHAR))
    compile()

    override def mapRow(resultSet: ResultSet, rowNumber: Int): ModuleRegistrationRow = mapResultSet(resultSet)
  }

  class ModuleRegistrationsByUniversityIdsQuery(ds: DataSource)
    extends MappingSqlQuery[ModuleRegistrationRow](ds, s"$UnconfirmedModuleRegistrationsForUniversityIds union $ConfirmedModuleRegistrationsForUniversityIds") {
    declareParameter(new SqlParameter("universityIds", Types.VARCHAR))
    declareParameter(new SqlParameter("currentAcademicYears", Types.VARCHAR))
    compile()

    override def mapRow(resultSet: ResultSet, rowNumber: Int): ModuleRegistrationRow = mapResultSet(resultSet)
  }

}

trait ModuleRegistrationImporterComponent {
  def moduleRegistrationImporter: ModuleRegistrationImporter
}

trait AutowiringModuleRegistrationImporterComponent extends ModuleRegistrationImporterComponent {
  var moduleRegistrationImporter: ModuleRegistrationImporter = Wire[ModuleRegistrationImporter]
}

trait CopyModuleRegistrationProperties {
  self: PropertyCopying with Logging =>

  def copyProperties(modRegRow: ModuleRegistrationRow, moduleRegistration: ModuleRegistration): Boolean = {
    val rowBean = PropertyAccessorFactory.forBeanPropertyAccess(modRegRow)
    val moduleRegistrationBean = PropertyAccessorFactory.forBeanPropertyAccess(moduleRegistration)

    copyBasicProperties(properties, rowBean, moduleRegistrationBean) |
    copySelectionStatus(moduleRegistrationBean, modRegRow.selectionStatusCode) |
    copyModuleResult(moduleRegistrationBean, modRegRow.moduleResult) |
    copyOptionProperty(moduleRegistrationBean, "actualMark", modRegRow.actualMark) |
    copyOptionProperty(moduleRegistrationBean, "actualGrade", modRegRow.actualGrade.maybeText) |
    copyOptionProperty(moduleRegistrationBean, "agreedMark", modRegRow.agreedMark) |
    copyOptionProperty(moduleRegistrationBean, "agreedGrade", modRegRow.agreedGrade.maybeText) |
    copyEndDate(moduleRegistrationBean, modRegRow.endWeek, moduleRegistration.academicYear)
  }

  private def copyCustomProperty[A](property: String, destinationBean: BeanWrapper, code: String, fn: String => A): Boolean = {
    val oldValue = destinationBean.getPropertyValue(property)
    val newValue = fn(code)

    // null == null in Scala so this is safe for unset values
    if (oldValue != newValue) {
      logger.debug("Detected property change for " + property + " (" + oldValue + " -> " + newValue + "); setting value")

      destinationBean.setPropertyValue(property, newValue)
      true
    }
    else false
  }

  private def copySelectionStatus(destinationBean: BeanWrapper, selectionStatusCode: String): Boolean =
    copyCustomProperty("selectionStatus", destinationBean, selectionStatusCode, ModuleSelectionStatus.fromCode)

  private def copyModuleResult(destinationBean: BeanWrapper, moduleResultCode: String): Boolean =
    copyCustomProperty("moduleResult", destinationBean, moduleResultCode, ModuleResult.fromCode)

  private def copyEndDate(destinationBean: BeanWrapper, endWeek: Option[Int], academicYear: AcademicYear): Boolean = {
    val oldValue = destinationBean.getPropertyValue("endDate")
    val newValue = endWeek.map(academicYear.dateFromSITSWeek).orNull

    if (oldValue != newValue) {
      destinationBean.setPropertyValue("endDate", newValue)
      true
    }
    else false
  }

  private val properties = Set(
    "assessmentGroup", "occurrence", "marksCode", "sitsModuleCode"
  )
}

// Full class rather than case class so it can be BeanWrapped (these need to be vars)
class ModuleRegistrationRow(
  var sprCode: String,
  var sitsModuleCode: String,
  var cats: JBigDecimal,
  var assessmentGroup: String,
  var selectionStatusCode: String,
  var occurrence: String,
  var academicYear: String,
  var actualMark: Option[Int],
  var actualGrade: String,
  var agreedMark: Option[Int],
  var agreedGrade: String,
  var marksCode: String,
  var moduleResult: String,
  var endWeek: Option[Int]
) {
  def moduleCode: Option[String] = Module.stripCats(sitsModuleCode).map(_.toLowerCase)

  def notionalKey: String = Seq(sprCode, sitsModuleCode, AcademicYear.parse(academicYear), scaled(cats), occurrence).mkString("-")

  override def toString: String =
    new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
      .append(sprCode)
      .append(sitsModuleCode)
      .append(cats)
      .append(assessmentGroup)
      .append(selectionStatusCode)
      .append(occurrence)
      .append(academicYear)
      .append(actualMark)
      .append(actualGrade)
      .append(agreedMark)
      .append(agreedGrade)
      .append(marksCode)
      .build()

  override def hashCode(): Int =
    new HashCodeBuilder()
      .append(sprCode)
      .append(sitsModuleCode)
      .append(cats)
      .append(assessmentGroup)
      .append(selectionStatusCode)
      .append(occurrence)
      .append(academicYear)
      .append(actualMark)
      .append(actualGrade)
      .append(agreedMark)
      .append(agreedGrade)
      .append(marksCode)
      .build()

  private def scaled(bg: JBigDecimal): JBigDecimal =
    JBigDecimal(Option(bg).map(_.setScale(2, RoundingMode.HALF_UP)))

  def matches(that: ModuleRegistration) : Boolean = {
    sprCode == that.sprCode &&
    Module.stripCats(sitsModuleCode).get.toLowerCase == that.module.code &&
    AcademicYear.parse(academicYear) == that.academicYear &&
    scaled(cats) == scaled(that.cats) &&
    occurrence == that.occurrence
  }

  def toModuleRegistration(module: Module): ModuleRegistration = new ModuleRegistration(
    sprCode,
    module,
    cats,
    sitsModuleCode,
    AcademicYear.parse(academicYear),
    occurrence,
    marksCode
  )

  override def equals(other: Any): Boolean = other match {
    case that: ModuleRegistrationRow =>
      new EqualsBuilder()
        .append(sprCode, that.sprCode)
        .append(sitsModuleCode, that.sitsModuleCode)
        .append(cats, that.cats)
        .append(assessmentGroup, that.assessmentGroup)
        .append(selectionStatusCode, that.selectionStatusCode)
        .append(occurrence, that.occurrence)
        .append(academicYear, that.academicYear)
        .append(actualMark, that.actualMark)
        .append(actualGrade, that.actualGrade)
        .append(agreedMark, that.agreedMark)
        .append(agreedGrade, that.agreedGrade)
        .append(marksCode, that.marksCode)
        .append(endWeek, that.endWeek)
        .build()
    case _ => false
  }
}

object ModuleRegistrationRow {
  def combine(rows: Seq[ModuleRegistrationRow]): ModuleRegistrationRow = {
    require(rows.size > 1, "Can't combine fewer than 2 rows")
    require(rows.forall(_.notionalKey == rows.head.notionalKey), "Rows must all have the same notional key")

    def coalesce[A >: Null](values: Seq[A]): A = values.filterNot(_ == null).headOption.orNull

    // sprCode, sitsModuleCode, AcademicYear.parse(academicYear), scaled(cats), occurrence
    new ModuleRegistrationRow(
      sprCode = rows.head.sprCode, // Part of notional key
      sitsModuleCode = rows.head.sitsModuleCode, // Part of notional key
      cats = rows.head.cats, // Part of notional key
      assessmentGroup = coalesce(rows.map(_.assessmentGroup)),
      selectionStatusCode = coalesce(rows.map(_.selectionStatusCode)),
      occurrence = rows.head.occurrence, // Part of notional key
      academicYear = rows.head.academicYear, // Part of notional key
      actualMark = rows.flatMap(_.actualMark).headOption,
      actualGrade = coalesce(rows.map(_.actualGrade)),
      agreedMark = rows.flatMap(_.agreedMark).headOption,
      agreedGrade = coalesce(rows.map(_.agreedGrade)),
      marksCode = coalesce(rows.map(_.marksCode)),
      moduleResult = coalesce(rows.map(_.moduleResult)),
      endWeek = coalesce(rows.map(_.endWeek))
    )
  }
}
