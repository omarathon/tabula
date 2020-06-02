package uk.ac.warwick.tabula.data

import org.hibernate.criterion.Order._
import org.hibernate.criterion.{Projections, Restrictions}
import org.joda.time.LocalDate
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports.JBigDecimal
import uk.ac.warwick.tabula.data.model._

trait ModuleRegistrationDaoComponent {
  val moduleRegistrationDao: ModuleRegistrationDao
}

trait AutowiringModuleRegistrationDaoComponent extends ModuleRegistrationDaoComponent {
  val moduleRegistrationDao: ModuleRegistrationDao = Wire[ModuleRegistrationDao]
}

trait ModuleRegistrationDao {
  def saveOrUpdate(moduleRegistration: ModuleRegistration): Unit

  def saveOrUpdate(coreRequiredModule: CoreRequiredModule): Unit

  def delete(coreRequiredModule: CoreRequiredModule): Unit

  def getByNotionalKey(
    studentCourseDetails: StudentCourseDetails,
    module: Module,
    cats: JBigDecimal,
    academicYear: AcademicYear,
    occurrence: String
  ): Option[ModuleRegistration]

  def getByUsercodesAndYear(usercodes: Seq[String], academicYear: AcademicYear): Seq[ModuleRegistration]

  def getByModuleAndYear(module: Module, academicYear: AcademicYear): Seq[ModuleRegistration]

  def getByModuleOccurrence(module: Module, cats: JBigDecimal, academicYear: AcademicYear, occurrence: String): Seq[ModuleRegistration]

  def getByYears(academicYears: Seq[AcademicYear], includeDeleted: Boolean): Seq[ModuleRegistration]

  def getByUniversityIds(universityIds: Seq[String], includeDeleted: Boolean): Seq[ModuleRegistration]

  def findCoreRequiredModules(route: Route, academicYear: AcademicYear, yearOfStudy: Int): Seq[CoreRequiredModule]

  def findRegisteredUsercodes(module: Module, academicYear: AcademicYear, endDate: Option[LocalDate], occurrence: Option[String]): Seq[String]
}

@Repository
class ModuleRegistrationDaoImpl extends ModuleRegistrationDao with Daoisms {

  def saveOrUpdate(moduleRegistration: ModuleRegistration): Unit = session.saveOrUpdate(moduleRegistration)

  def saveOrUpdate(coreRequiredModule: CoreRequiredModule): Unit = session.saveOrUpdate(coreRequiredModule)

  def delete(coreRequiredModule: CoreRequiredModule): Unit = session.delete(coreRequiredModule)

  def getByNotionalKey(
    studentCourseDetails: StudentCourseDetails,
    module: Module,
    cats: JBigDecimal,
    academicYear: AcademicYear,
    occurrence: String
  ): Option[ModuleRegistration] =
    session.newCriteria[ModuleRegistration]
      .add(is("studentCourseDetails", studentCourseDetails))
      .add(is("module", module))
      .add(is("academicYear", academicYear))
      .add(is("cats", cats))
      .add(is("occurrence", occurrence))
      .uniqueResult

  def getByUsercodesAndYear(userCodes: Seq[String], academicYear: AcademicYear): Seq[ModuleRegistration] =
    session.newQuery[ModuleRegistration](
      """
        select distinct mr
          from ModuleRegistration mr
          where academicYear = :academicYear
          and studentCourseDetails.missingFromImportSince is null
          and studentCourseDetails.student.userId in :usercodes
          and mr.deleted is false
        """)
      .setParameter("academicYear", academicYear)
      .setParameterList("usercodes", userCodes)
      .seq

  def getByModuleAndYear(module: Module, academicYear: AcademicYear): Seq[ModuleRegistration] =
    session.newCriteria[ModuleRegistration]
      .add(is("module", module))
      .add(is("academicYear", academicYear))
      .add(is("deleted", false))
      .addOrder(asc("_scjCode"))
      .seq

  override def getByModuleOccurrence(module: Module, cats: JBigDecimal, academicYear: AcademicYear, occurrence: String): Seq[ModuleRegistration] =
    session.newCriteria[ModuleRegistration]
      .add(is("module", module))
      .add(is("cats", cats))
      .add(is("academicYear", academicYear))
      .add(is("occurrence", occurrence))
      .add(is("deleted", false))
      .addOrder(asc("_scjCode"))
      .seq

  def getByYears(academicYears: Seq[AcademicYear], includeDeleted: Boolean): Seq[ModuleRegistration] = {
    safeInSeq(() => {
      val criteria = session.newCriteria[ModuleRegistration]
        .addOrder(asc("_scjCode"))
      if (!includeDeleted) {
        criteria.add(is("deleted", false))
      }
      criteria
    }, "academicYear", academicYears)
  }

  def getByUniversityIds(universityIds: Seq[String], includeDeleted: Boolean): Seq[ModuleRegistration] =
    session.newQuery[ModuleRegistration](
      """
         select distinct mr
         from ModuleRegistration mr
         where studentCourseDetails.missingFromImportSince is null
          and studentCourseDetails.student.universityId in :universityIds
          and (mr.deleted is false or :includeDeleted is true)
      """)
      .setParameterList("universityIds", universityIds)
      .setBoolean("includeDeleted", includeDeleted)
      .seq

  def findCoreRequiredModules(route: Route, academicYear: AcademicYear, yearOfStudy: Int): Seq[CoreRequiredModule] = {
    session.newCriteria[CoreRequiredModule]
      .add(is("route", route))
      .add(is("academicYear", academicYear))
      .add(is("yearOfStudy", yearOfStudy))
      .seq
  }

  def findRegisteredUsercodes(module: Module, academicYear: AcademicYear, endDate: Option[LocalDate], occurrence: Option[String]): Seq[String] = {
    def applyAndSeq(extraCriteria: ScalaCriteria[ModuleRegistration] => Unit): Seq[String] = {
      val c = session.newCriteria[ModuleRegistration]
        .createAlias("studentCourseDetails", "studentCourseDetails")
        .createAlias("studentCourseDetails.student", "student")
        .add(is("module", module))
        .add(is("deleted", false))
      occurrence.map(o =>
        c.add(is("occurrence", o))
      )
      extraCriteria(c)
      c.project[String](Projections.property("student.userId")).seq
    }

    if (endDate.isEmpty) {
      applyAndSeq { c =>
        c.add(is("academicYear", academicYear))
      }
    } else {
      val mrWithNoEndDate = applyAndSeq { c =>
        c.add(is("academicYear", academicYear))
          .add(is("endDate", null))
      }

      val mrWithEndDate = applyAndSeq { c =>
        c.add(Restrictions.ge("endDate", endDate.get))
      }

      (mrWithNoEndDate ++ mrWithEndDate).distinct
    }
  }
}
