package uk.ac.warwick.tabula.data

import org.hibernate.criterion.Order._
import org.joda.time.LocalDate
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
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
    sitsModuleCode: String,
    academicYear: AcademicYear,
    occurrence: String
  ): Option[ModuleRegistration]

  def getByUsercodesAndYear(usercodes: Seq[String], academicYear: AcademicYear): Seq[ModuleRegistration]

  def getByModuleAndYear(module: Module, academicYear: AcademicYear): Seq[ModuleRegistration]

  def getByDepartmentAndYear(department: Department, academicYear: AcademicYear): Seq[ModuleRegistration]

  def getByModuleOccurrence(sitsModuleCode: String, academicYear: AcademicYear, occurrence: String): Seq[ModuleRegistration]

  def getByYears(academicYears: Seq[AcademicYear], includeDeleted: Boolean): Seq[ModuleRegistration]

  def getByUniversityIds(universityIds: Seq[String], includeDeleted: Boolean): Seq[ModuleRegistration]

  def findCoreRequiredModules(route: Route, academicYear: AcademicYear, yearOfStudy: Int): Seq[CoreRequiredModule]

  def findRegisteredUsercodes(module: Module, academicYear: AcademicYear, endDate: Option[LocalDate], occurrence: Option[String]): Seq[String]

  def getByRecordedAssessmentComponentStudentsNeedsWritingToSits: Seq[ModuleRegistration]
}

@Repository
class ModuleRegistrationDaoImpl extends ModuleRegistrationDao with Daoisms {

  def saveOrUpdate(moduleRegistration: ModuleRegistration): Unit = session.saveOrUpdate(moduleRegistration)

  def saveOrUpdate(coreRequiredModule: CoreRequiredModule): Unit = session.saveOrUpdate(coreRequiredModule)

  def delete(coreRequiredModule: CoreRequiredModule): Unit = session.delete(coreRequiredModule)

  def getByNotionalKey(
    studentCourseDetails: StudentCourseDetails,
    sitsModuleCode: String,
    academicYear: AcademicYear,
    occurrence: String
  ): Option[ModuleRegistration] =
    session.newCriteria[ModuleRegistration]
      .add(is("sprCode", studentCourseDetails.sprCode))
      .add(is("sitsModuleCode", sitsModuleCode))
      .add(is("academicYear", academicYear))
      .add(is("occurrence", occurrence))
      .uniqueResult

  def getByUsercodesAndYear(userCodes: Seq[String], academicYear: AcademicYear): Seq[ModuleRegistration] =
    session.newQuery[ModuleRegistration](
      """
        select distinct mr
          from ModuleRegistration mr
          join StudentCourseDetails studentCourseDetails
            on studentCourseDetails.sprCode = mr.sprCode
          where mr.academicYear = :academicYear
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
      .addOrder(asc("sprCode"))
      .seq

  override def getByDepartmentAndYear(department: Department, academicYear: AcademicYear): Seq[ModuleRegistration] =
    session.newCriteria[ModuleRegistration]
      .createAlias("module", "module")
      .add(is("module.adminDepartment", department))
      .add(is("academicYear", academicYear))
      .add(is("deleted", false))
      .seq

  override def getByModuleOccurrence(sitsModuleCode: String, academicYear: AcademicYear, occurrence: String): Seq[ModuleRegistration] =
    session.newCriteria[ModuleRegistration]
      .add(is("sitsModuleCode", sitsModuleCode))
      .add(is("academicYear", academicYear))
      .add(is("occurrence", occurrence))
      .add(is("deleted", false))
      .addOrder(asc("sprCode"))
      .seq

  def getByYears(academicYears: Seq[AcademicYear], includeDeleted: Boolean): Seq[ModuleRegistration] = {
    safeInSeq(() => {
      val criteria = session.newCriteria[ModuleRegistration]
        .addOrder(asc("sprCode"))
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
        join StudentCourseDetails studentCourseDetails
          on studentCourseDetails.sprCode = mr.sprCode
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
    val query = session.newQuery[String](
      s"""
        select distinct studentCourseDetails.student.userId
        from ModuleRegistration mr
        join StudentCourseDetails studentCourseDetails
          on studentCourseDetails.sprCode = mr.sprCode
        where
          mr.module = :module and
          mr.deleted is :deleted and
          ${if (occurrence.nonEmpty) "mr.occurrence = :occurrence and" else ""}
          ${
            if (endDate.nonEmpty) "((mr.academicYear = :academicYear and mr.endDate = null) or mr.endDate >= :endDate)"
            else "mr.academicYear = :academicYear"
          }
       """
    )

    query
      .setParameter("module", module)
      .setParameter("deleted", false)
      .setParameter("academicYear", academicYear)

    occurrence.foreach(query.setParameter("occurrence", _))
    endDate.foreach(query.setParameter("endDate", _))

    query.seq
  }

  override def getByRecordedAssessmentComponentStudentsNeedsWritingToSits: Seq[ModuleRegistration] =
    session.newQuery[ModuleRegistration](
      """select distinct mr
        |from ModuleRegistration mr
        |join fetch mr._allStudentCourseDetails studentCourseDetails
        |join RecordedAssessmentComponentStudent racs
        |  on racs.moduleCode = mr.sitsModuleCode and
        |     racs.occurrence = mr.occurrence and
        |     racs.academicYear = mr.academicYear and
        |     racs.universityId = studentCourseDetails.student.universityId
        |where mr.deleted is false and racs.needsWritingToSits is true""".stripMargin
    ).seq
}
