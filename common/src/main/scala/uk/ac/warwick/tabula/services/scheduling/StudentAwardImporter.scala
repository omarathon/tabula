package uk.ac.warwick.tabula.services.scheduling

import java.sql.{ResultSet, Types}

import javax.sql.DataSource
import org.joda.time.LocalDate
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.MappingSqlQuery
import org.springframework.jdbc.core.SqlParameter
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports.{JList, JMap}
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.model.DegreeType
import uk.ac.warwick.tabula.helpers.JodaConverters._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.sandbox.SandboxData
import uk.ac.warwick.tabula.services.AutowiringAwardServiceComponent
import uk.ac.warwick.tabula.services.scheduling.StudentAwardImporter.{StudentAwardsByAcademicYearsQuery, StudentAwardsByUniversityIdsQuery}
import uk.ac.warwick.tabula.{AcademicYear, AutowiringFeaturesComponent, Features}

import scala.jdk.CollectionConverters._

/**
 * Import student awards from SITS.
 *
 */
trait StudentAwardImporter {
  def getStudentAwardRowsForAcademicYears(academicYears: Seq[AcademicYear]): Seq[StudentAwardRow]

  def getStudentAwardRowsForUniversityIds(universityIds: Seq[String]): Seq[StudentAwardRow]
}

@Profile(Array("dev", "test", "production"))
@Service
class StudentAwardImporterImpl extends StudentAwardImporter with AutowiringSitsDataSourceComponent with TaskBenchmarking
  with AutowiringFeaturesComponent with AutowiringAwardServiceComponent {
  lazy val studentAwardsByAcademicYearsQuery: StudentAwardsByAcademicYearsQuery = new StudentAwardsByAcademicYearsQuery(sitsDataSource)
  lazy val studentAwardsByUniversityIdsQuery: StudentAwardsByUniversityIdsQuery = new StudentAwardsByUniversityIdsQuery(sitsDataSource)

  private def yearsToImportArray(yearsToImport: Seq[AcademicYear]): JList[String] = yearsToImport.map(_.toString).asJava: JList[String]

  def getStudentAwardRowsForAcademicYears(academicYears: Seq[AcademicYear]): Seq[StudentAwardRow] =
    benchmarkTask(s"Fetch student awards for $academicYears") {
      studentAwardsByAcademicYearsQuery.executeByNamedParam(JMap("academicYears" -> yearsToImportArray(academicYears))).asScala.distinct.toSeq
    }

  def getStudentAwardRowsForUniversityIds(universityIds: Seq[String]): Seq[StudentAwardRow] =
    benchmarkTask(s"Fetch student awards for $universityIds") {
      universityIds.grouped(Daoisms.MaxInClauseCountOracle).flatMap { ids =>
        studentAwardsByUniversityIdsQuery.executeByNamedParam(JMap("universityIds" -> ids.asJava)).asScala.distinct.toSeq
      }.toSeq
    }
}

@Profile(Array("sandbox"))
@Service
class SandboxStudentAwardImporter extends StudentAwardImporter {

  override def getStudentAwardRowsForAcademicYears(academicYears: Seq[AcademicYear]): Seq[StudentAwardRow] =
    SandboxData.Departments.values
      .flatMap(_.routes.values)
      .flatMap(route => route.studentsStartId to route.studentsEndId)
      .flatMap(universityId => studentStudentAwardRows(universityId.toString))
      .filter(sa => academicYears.contains(sa.academicYear))
      .toSeq

  override def getStudentAwardRowsForUniversityIds(universityIds: Seq[String]): Seq[StudentAwardRow] =
    universityIds.flatMap(studentStudentAwardRows)

  def studentStudentAwardRows(universityId: String): Seq[StudentAwardRow] = {
    SandboxData.Departments.flatMap(_._2.routes.values)
      .find(route => route.degreeType == DegreeType.Undergraduate && (route.studentsStartId to route.studentsEndId).contains(universityId.toInt))
      .map { route =>
        val yearOfStudy = ((universityId.toLong % 3) + 1).toInt
        (1 until yearOfStudy).reverse.map(AcademicYear.now() - _).zipWithIndex.map { case (academicYear, index) =>
          StudentAwardRow(
            sprCode = "%s/1".format(universityId),
            academicYear = academicYear,
            awardCode = route.awardCode,
            awardDate = None,
            classificationCode = None
          )
        }
      }
      .getOrElse(Nil)
  }
}

object StudentAwardImporter {
  val sitsSchema: String = Wire.property("${schema.sits}")

  var features: Features = Wire[Features]

  final def BaseSql: String =
    s"""
       |select
       |   saw.spr_code as spr_code,
       |   saw.ayr_code as academic_year,
       |   saw.awd_code as award_code,
       |   saw.saw_date as award_date,
       |   saw.cla2_code as classification_code
       |from $sitsSchema.cam_saw saw
       |""".stripMargin

  final def StudentAwardsForAcademicYearSql =
    s"$BaseSql where saw.ayr_code in (:academicYears)"

  final def StudentAwardsForUniversityIdsSql =
    s"$BaseSql where SUBSTR(saw.spr_code, 0, 7) in (:universityIds)"

  def mapResultSet(rs: ResultSet): StudentAwardRow = {
    StudentAwardRow(
      sprCode = rs.getString("spr_code"),
      academicYear = AcademicYear.parse(rs.getString("academic_year")),
      awardCode = rs.getString("award_code"),
      awardDate = Option(rs.getDate("award_date")).map(_.toLocalDate.asJoda),
      classificationCode = rs.getString("classification_code").maybeText
    )
  }

  class StudentAwardsByAcademicYearsQuery(ds: DataSource)
    extends MappingSqlQuery[StudentAwardRow](ds, StudentAwardsForAcademicYearSql) {
    declareParameter(new SqlParameter("academicYears", Types.VARCHAR))
    compile()

    override def mapRow(resultSet: ResultSet, rowNumber: Int): StudentAwardRow = mapResultSet(resultSet)
  }

  class StudentAwardsByUniversityIdsQuery(ds: DataSource)
    extends MappingSqlQuery[StudentAwardRow](ds, StudentAwardsForUniversityIdsSql) {
    declareParameter(new SqlParameter("universityIds", Types.VARCHAR))
    compile()

    override def mapRow(resultSet: ResultSet, rowNumber: Int): StudentAwardRow = mapResultSet(resultSet)
  }

}

trait StudentAwardImporterComponent {
  def studentAwardImporter: StudentAwardImporter
}

trait AutowiringStudentAwardImporterComponent extends StudentAwardImporterComponent {
  var studentAwardImporter: StudentAwardImporter = Wire[StudentAwardImporter]
}

case class StudentAwardRow(
  sprCode: String,
  academicYear: AcademicYear,
  awardCode: String,
  awardDate: Option[LocalDate],
  classificationCode: Option[String]
)
