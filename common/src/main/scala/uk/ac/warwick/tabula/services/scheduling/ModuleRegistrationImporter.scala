package uk.ac.warwick.tabula.services.scheduling

import java.sql.{ResultSet, Types}

import javax.sql.DataSource
import org.apache.commons.lang3.builder.{EqualsBuilder, HashCodeBuilder, ToStringBuilder, ToStringStyle}
import org.springframework.beans.{BeanWrapper, PropertyAccessorFactory}
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.jdbc.core.SqlParameter
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports.JBigDecimal
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.commands.scheduling.imports.ImportModuleRegistrationsCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{Daoisms, StudentCourseDetailsDao}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.scheduling.PropertyCopying
import uk.ac.warwick.tabula.sandbox.SandboxData
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.services.scheduling.ModuleRegistrationImporter.{ModuleRegistrationsByAcademicYearQuery, ModuleRegistrationsByUniversityIdsQuery}

import scala.jdk.CollectionConverters._
import scala.math.BigDecimal.RoundingMode
import scala.util.Try

/**
  * Import module registration data from SITS.
  *
  */
trait ModuleRegistrationImporter {
  def getModuleRegistrationRowsForAcademicYear(academicYear: AcademicYear): Seq[ModuleRegistrationRow]
  def getModuleRegistrationRowsForUniversityIds(universityIds: Seq[String]): Seq[ModuleRegistrationRow]
}

trait AbstractModuleRegistrationImporter extends ModuleRegistrationImporter with Logging {

  var studentCourseDetailsDao: StudentCourseDetailsDao = Wire[StudentCourseDetailsDao]
  var moduleAndDepartmentService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]

  protected def applyForRows(rows: Seq[ModuleRegistrationRow]): Seq[ImportModuleRegistrationsCommand] = {
    val tabulaModules: Set[Module] = rows.groupBy(_.sitsModuleCode).flatMap { case (sitsModuleCode, moduleRows) =>
      moduleAndDepartmentService.getModuleBySitsCode(sitsModuleCode) match {
        case None =>
          logger.warn(s"No stem module for $sitsModuleCode found in Tabula for SCJ: ${moduleRows.map(_.scjCode).distinct.mkString(", ")}")
          None
        case Some(module) => Some(module)
      }
    }.toSet
    val tabulaModuleCodes = tabulaModules.map(_.code)
    val rowsBySCD: Map[StudentCourseDetails, Seq[ModuleRegistrationRow]] = rows.groupBy(_.scjCode).map { case (scjCode, scjRows) =>
      studentCourseDetailsDao.getByScjCode(scjCode).getOrElse {
        logger.error("Can't record module registration - could not find a StudentCourseDetails for " + scjCode)
        null
      } -> scjRows.filter(row => {
        val moduleCode = Module.stripCats(row.sitsModuleCode)
        moduleCode.isDefined && tabulaModuleCodes.contains(moduleCode.get.toLowerCase)
      })
    }
    rowsBySCD.view.filterKeys(_ != null).map { case (scd, scdRows) => new ImportModuleRegistrationsCommand(scd, scdRows, tabulaModules) }.toSeq
  }
}

@Profile(Array("dev", "test", "production"))
@Service
class ModuleRegistrationImporterImpl extends AbstractModuleRegistrationImporter with AutowiringSitsDataSourceComponent with TaskBenchmarking {
  lazy val moduleRegistrationsByAcademicYearQuery: ModuleRegistrationsByAcademicYearQuery = new ModuleRegistrationsByAcademicYearQuery(sitsDataSource)
  lazy val moduleRegistrationsByUniversityIdsQuery: ModuleRegistrationsByUniversityIdsQuery = new ModuleRegistrationsByUniversityIdsQuery(sitsDataSource)

  def getModuleRegistrationRowsForAcademicYear(academicYear: AcademicYear): Seq[ModuleRegistrationRow] =
    benchmarkTask("Fetch module registrations") {
      moduleRegistrationsByAcademicYearQuery.executeByNamedParam(Map("academicYear" -> academicYear).asJava).asScala.distinct.toSeq
    }

  def getModuleRegistrationRowsForUniversityIds(universityIds: Seq[String]): Seq[ModuleRegistrationRow] =
    benchmarkTask("Fetch module registrations") {
      universityIds.grouped(Daoisms.MaxInClauseCountOracle).flatMap { ids =>
        moduleRegistrationsByUniversityIdsQuery.executeByNamedParam(Map("universityIds" -> ids.asJava).asJava).asScala.distinct.toSeq
      }.toSeq
    }
}

@Profile(Array("sandbox"))
@Service
class SandboxModuleRegistrationImporter extends AbstractModuleRegistrationImporter {
  override def getModuleRegistrationRowsForAcademicYear(academicYear: AcademicYear): Seq[ModuleRegistrationRow] =
    SandboxData.Departments.values
      .flatMap(_.routes.values)
      .flatMap(route => route.studentsStartId to route.studentsEndId)
      .flatMap(universityId => studentModuleRegistrationRows(universityId.toString))
      .toSeq

  override def getModuleRegistrationRowsForUniversityIds(universityIds: Seq[String]): Seq[ModuleRegistrationRow] =
    universityIds.flatMap(studentModuleRegistrationRows)

  def studentModuleRegistrationRows(universityId: String): Seq[ModuleRegistrationRow] = {
    val yearOfStudy = ((universityId.toLong % 3) + 1).toInt

    (for {
      (_, d) <- SandboxData.Departments
      route <- d.routes.values.toSeq
      if (route.studentsStartId to route.studentsEndId).contains(universityId.toInt)
      moduleCode <- route.moduleCodes if moduleCode.substring(3, 4).toInt <= yearOfStudy
    } yield {
      val isPassFail = moduleCode.takeRight(1) == "9" // modules with a code ending in 9 are pass/fails
      val markScheme = if (isPassFail) "PF" else "WAR"

      val level = moduleCode.substring(3, 4).toInt
      val academicYear = AcademicYear.now - (yearOfStudy - level)

      val (mark, grade, result) =
        if (academicYear < AcademicYear.now()) {
          val m =
            if (isPassFail) {
              if (math.random < 0.25) 0 else 100
            } else {
              (universityId ++ universityId ++ moduleCode.substring(3)).toCharArray.map(char =>
                Try(char.toString.toInt).toOption.getOrElse(0) * universityId.toCharArray.apply(0).toString.toInt
              ).sum % 100
            }

          val g =
            if (isPassFail) if (m == 100) "P" else "F"
            else SandboxData.GradeBoundaries.find(gb => gb.marksCode == "TABULA-UG" && gb.minimumMark <= m && gb.maximumMark >= m).map(_.grade).getOrElse("F")

          (Some(new JBigDecimal(m)), g, if (m < 40) "F" else "P")
        } else (None: Option[JBigDecimal], null: String, null: String)

      new ModuleRegistrationRow(
        scjCode = "%s/1".format(universityId),
        sitsModuleCode = "%s-15".format(moduleCode.toUpperCase),
        cats = new JBigDecimal(15),
        assessmentGroup = "A",
        selectionStatusCode = (universityId.toInt + Try(moduleCode.substring(3).toInt).getOrElse(0)) % 2 match {
          case 0 => "C"
          case _ => "O"
        },
        occurrence = "A",
        academicYear = academicYear.toString,
        actualMark = mark,
        actualGrade = grade,
        agreedMark = mark,
        agreedGrade = grade,
        markScheme = markScheme,
        moduleResult = result
      )
    }).toSeq
  }
}

object ModuleRegistrationImporter {
  val sitsSchema: String = Wire.property("${schema.sits}")

  // a list of all the markscheme codes that we consider to be pass/fail modules
  final val PassFailMarkSchemeCodes = Seq("PF")

  // union 2 things -
  // 1. unconfirmed module registrations from the SMS table
  // 2. confirmed module registrations from the SMO table

  def UnconfirmedModuleRegistrationsForAcademicYear =
    s"""
      select
      scj_code,
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
      smr_rslt  -- result of module

      from $sitsSchema.cam_sms sms

      join $sitsSchema.ins_spr spr
        on spr.spr_code = sms.spr_code

      join $sitsSchema.srs_scj scj
        on scj.scj_sprc = spr.spr_code

      join ins_mod mod on mod.mod_code = sms.mod_code

      left join $sitsSchema.ins_smr smr -- Student Module Result
        on sms.spr_code = smr.spr_code
        and sms.ayr_code = smr.ayr_code
        and sms.mod_code = smr.mod_code
        and sms.sms_occl = smr.mav_occur

       where sms.ayr_code = :academicYear"""

  def ConfirmedModuleRegistrationsForAcademicYear =
    s"""
      select
      scj_code,
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
      smr_rslt  -- result of module

      from $sitsSchema.cam_smo smo

      join $sitsSchema.ins_spr spr
        on spr.spr_code = smo.spr_code

      join $sitsSchema.srs_scj scj
        on scj.scj_sprc = spr.spr_code

      join ins_mod mod on mod.mod_code = smo.mod_code

      left join $sitsSchema.ins_smr smr -- Student Module Result
        on smo.spr_code = smr.spr_code
        and smo.ayr_code = smr.ayr_code
        and smo.mod_code = smr.mod_code
        and smo.mav_occur = smr.mav_occur

       where
       (smo.smo_rtsc is null or (smo.smo_rtsc not like 'X%' and smo.smo_rtsc != 'Z')) -- exclude WMG cancelled registrations
       and smo.ayr_code = :academicYear"""

  def UnconfirmedModuleRegistrationsForUniversityIds =
    s"""
      select
      scj_code,
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
      smr_rslt  -- result of module

      from $sitsSchema.cam_sms sms

      join $sitsSchema.ins_spr spr
        on spr.spr_code = sms.spr_code

      join $sitsSchema.srs_scj scj
        on scj.scj_sprc = spr.spr_code

      join ins_mod mod on mod.mod_code = sms.mod_code

      left join $sitsSchema.ins_smr smr -- Student Module Result
        on sms.spr_code = smr.spr_code
        and sms.ayr_code = smr.ayr_code
        and sms.mod_code = smr.mod_code
        and sms.sms_occl = smr.mav_occur

       where spr.spr_stuc in (:universityIds)"""

  def ConfirmedModuleRegistrationsForUniversityIds =
    s"""
      select
      scj_code,
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
      smr_rslt  -- result of module

      from $sitsSchema.cam_smo smo

      join $sitsSchema.ins_spr spr
        on spr.spr_code = smo.spr_code

      join $sitsSchema.srs_scj scj
        on scj.scj_sprc = spr.spr_code

      join ins_mod mod on mod.mod_code = smo.mod_code

      left join $sitsSchema.ins_smr smr -- Student Module Result
        on smo.spr_code = smr.spr_code
        and smo.ayr_code = smr.ayr_code
        and smo.mod_code = smr.mod_code
        and smo.mav_occur = smr.mav_occur

       where
       (smo.smo_rtsc is null or (smo.smo_rtsc not like 'X%' and smo.smo_rtsc != 'Z')) -- exclude WMG cancelled registrations
       and spr.spr_stuc in (:universityIds)"""

  def mapResultSet(resultSet: ResultSet): ModuleRegistrationRow = {
    new ModuleRegistrationRow(
      resultSet.getString("scj_code"),
      resultSet.getString("mod_code"),
      resultSet.getBigDecimal("credit"),
      resultSet.getString("assess_group"),
      resultSet.getString("ses_code"),
      resultSet.getString("occurrence"),
      resultSet.getString("ayr_code"),
      Option(resultSet.getBigDecimal("smr_actm")),
      resultSet.getString("smr_actg"),
      Option(resultSet.getBigDecimal("smr_agrm")),
      resultSet.getString("smr_agrg"),
      resultSet.getString("smr_mksc"),
      resultSet.getString("smr_rslt")
    )
  }

  class ModuleRegistrationsByAcademicYearQuery(ds: DataSource)
    extends MappingSqlQuery[ModuleRegistrationRow](ds, s"$UnconfirmedModuleRegistrationsForAcademicYear union $ConfirmedModuleRegistrationsForAcademicYear") {
    declareParameter(new SqlParameter("academicYear", Types.VARCHAR))
    compile()

    override def mapRow(resultSet: ResultSet, rowNumber: Int): ModuleRegistrationRow = mapResultSet(resultSet)
  }

  class ModuleRegistrationsByUniversityIdsQuery(ds: DataSource)
    extends MappingSqlQuery[ModuleRegistrationRow](ds, s"$UnconfirmedModuleRegistrationsForUniversityIds union $ConfirmedModuleRegistrationsForUniversityIds") {
    declareParameter(new SqlParameter("universityIds", Types.VARCHAR))
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
      copyBigDecimal(moduleRegistrationBean, "actualMark", modRegRow.actualMark) |
      copyBigDecimal(moduleRegistrationBean, "agreedMark", modRegRow.agreedMark)
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

  private val properties = Set(
    "assessmentGroup", "occurrence", "actualGrade", "agreedGrade", "passFail"
  )
}

// Full class rather than case class so it can be BeanWrapped (these need to be vars)
class ModuleRegistrationRow(
  var scjCode: String,
  var sitsModuleCode: String,
  var cats: JBigDecimal,
  var assessmentGroup: String,
  var selectionStatusCode: String,
  var occurrence: String,
  var academicYear: String,
  var actualMark: Option[JBigDecimal],
  var actualGrade: String,
  var agreedMark: Option[JBigDecimal],
  var agreedGrade: String,
  var passFail: Boolean,
  var moduleResult: String
) {

  def this(
    scjCode: String,
    sitsModuleCode: String,
    cats: JBigDecimal,
    assessmentGroup: String,
    selectionStatusCode: String,
    occurrence: String,
    academicYear: String,
    actualMark: Option[JBigDecimal],
    actualGrade: String,
    agreedMark: Option[JBigDecimal],
    agreedGrade: String,
    markScheme: String,
    moduleResult: String
  ) {
    this(scjCode, sitsModuleCode, cats, assessmentGroup, selectionStatusCode, occurrence, academicYear, actualMark, actualGrade, agreedMark, agreedGrade, ModuleRegistrationImporter.PassFailMarkSchemeCodes.contains(markScheme), moduleResult)
  }

  def moduleCode: Option[String] = Module.stripCats(sitsModuleCode).map(_.toLowerCase)

  def notionalKey: String = Seq(scjCode, sitsModuleCode, AcademicYear.parse(academicYear), scaled(cats), occurrence).mkString("-")

  override def toString: String =
    new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
      .append(scjCode)
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
      .append(passFail)
      .build()

  override def hashCode(): Int =
    new HashCodeBuilder()
      .append(scjCode)
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
      .append(passFail)
      .build()

  private def scaled(bg: JBigDecimal): JBigDecimal =
    JBigDecimal(Option(bg).map(_.setScale(2, RoundingMode.HALF_UP)))

  def matches(that: ModuleRegistration) : Boolean = {
    scjCode == that._scjCode &&
    Module.stripCats(sitsModuleCode).get.toLowerCase == that.module.code &&
    AcademicYear.parse(academicYear) == that.academicYear &&
    scaled(cats) == scaled(that.cats) &&
    occurrence == that.occurrence
  }

  def toModuleRegistration(module: Module): ModuleRegistration = new ModuleRegistration(
    scjCode,
    module,
    cats,
    AcademicYear.parse(academicYear),
    occurrence,
    passFail
  )

  override def equals(other: Any): Boolean = other match {
    case that: ModuleRegistrationRow =>
      new EqualsBuilder()
        .append(scjCode, that.scjCode)
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
        .append(passFail, that.passFail)
        .build()
    case _ => false
  }
}

object ModuleRegistrationRow {
  def combine(rows: Seq[ModuleRegistrationRow]): ModuleRegistrationRow = {
    require(rows.size > 1, "Can't combine fewer than 2 rows")
    require(rows.forall(_.notionalKey == rows.head.notionalKey), "Rows must all have the same notional key")

    def coalesce[A >: Null](values: Seq[A]): A = values.filterNot(_ == null).headOption.orNull

    // scjCode, sitsModuleCode, AcademicYear.parse(academicYear), scaled(cats), occurrence
    new ModuleRegistrationRow(
      scjCode = rows.head.scjCode, // Part of notional key
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
      passFail = rows.exists(_.passFail),
      moduleResult = coalesce(rows.map(_.moduleResult))
    )
  }
}
