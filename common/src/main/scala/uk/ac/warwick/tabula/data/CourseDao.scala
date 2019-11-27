package uk.ac.warwick.tabula.data

import org.hibernate.criterion.Projections.{distinct, projectionList, property}
import org.hibernate.criterion.Restrictions.disjunction
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.data.model.{Course, CourseYearWeighting, Department, StudentCourseYearDetails}

trait CourseDaoComponent {
  val courseDao: CourseDao
}

trait AutowiringCourseDaoComponent extends CourseDaoComponent {
  val courseDao: CourseDao = Wire[CourseDao]
}

trait CourseDao {
  def saveOrUpdate(course: Course): Unit

  def saveOrUpdate(courseYearWeighting: CourseYearWeighting): Unit

  def delete(courseYearWeighting: CourseYearWeighting): Unit

  def getByCode(code: String): Option[Course]

  def getAllCourseCodes: Seq[String]

  def getAll: Seq[Course]

  def findByDepartment(department: Department): Seq[Course]

  def findCoursesNamedLike(query: String): Seq[Course]

  def getCourseYearWeighting(courseCode: String, academicYear: AcademicYear, yearOfStudy: YearOfStudy): Option[CourseYearWeighting]

  def findAllCourseYearWeightings(courses: Seq[Course], academicYear: AcademicYear): Seq[CourseYearWeighting]

  def getOccurrencesForCourses(courses: Seq[Course]): Seq[String]
}

@Repository
class CourseDaoImpl extends CourseDao with Daoisms {

  def saveOrUpdate(course: Course): Unit = session.saveOrUpdate(course)

  def saveOrUpdate(courseYearWeighting: CourseYearWeighting): Unit = session.saveOrUpdate(courseYearWeighting)

  def delete(courseYearWeighting: CourseYearWeighting): Unit = session.delete(courseYearWeighting)

  def getByCode(code: String): Option[Course] =
    session.newQuery[Course]("from Course course where code = :code").setString("code", code).uniqueResult

  def getAllCourseCodes: Seq[String] =
    session.newQuery[String]("select distinct code from Course").seq

  def getAll: Seq[Course] = session.newCriteria[Course].seq

  def findByDepartment(department: Department): Seq[Course] =
    session.newCriteria[Course]
      .add(is("_department", department))
      .seq

  def findCoursesNamedLike(query: String): Seq[Course] = {
    session.newCriteria[Course]
      .add(disjunction()
        .add(likeIgnoreCase("code", s"%${query.toLowerCase}%"))
        .add(likeIgnoreCase("name", s"%${query.toLowerCase}%"))
      )
      .setMaxResults(20).seq
  }

  def getCourseYearWeighting(courseCode: String, academicYear: AcademicYear, yearOfStudy: Int): Option[CourseYearWeighting] =
    session.newCriteria[CourseYearWeighting]
      .createAlias("course", "course")
      .add(is("course.code", courseCode))
      .add(is("sprStartAcademicYear", academicYear))
      .add(is("yearOfStudy", yearOfStudy))
      .uniqueResult

  def findAllCourseYearWeightings(courses: Seq[Course], academicYear: AcademicYear): Seq[CourseYearWeighting] =
    safeInSeq[CourseYearWeighting](
      () => session.newCriteria[CourseYearWeighting]
        .add(is("sprStartAcademicYear", academicYear)),
      "course",
      courses
    )

  def getOccurrencesForCourses(courses: Seq[Course]): Seq[String] = {
    safeInSeqWithProjection[StudentCourseYearDetails, String](
      () => {
        session.newCriteria[StudentCourseYearDetails]
          .createAlias("studentCourseDetails", "scd")
      },
      projectionList()
        .add(distinct(property("blockOccurrence"))),
      "scd.course",
      courses
    ).filterNot(_ == null)
  }

}
