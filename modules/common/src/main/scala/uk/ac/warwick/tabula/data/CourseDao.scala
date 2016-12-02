package uk.ac.warwick.tabula.data
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.{CourseYearWeighting, Department, Course}
import uk.ac.warwick.spring.Wire

trait CourseDaoComponent {
	val courseDao: CourseDao
}

trait AutowiringCourseDaoComponent extends CourseDaoComponent {
	val courseDao: CourseDao = Wire[CourseDao]
}

trait CourseDao {
	def saveOrUpdate(course: Course)
	def saveOrUpdate(courseYearWeighting: CourseYearWeighting)

	def getByCode(code: String): Option[Course]
	def getAllCourseCodes: Seq[String]
	def findByDepartment(department: Department): Seq[Course]

	def getCourseYearWeighting(courseCode: String, academicYear: AcademicYear, yearOfStudy: Int): Option[CourseYearWeighting]
}

@Repository
class CourseDaoImpl extends CourseDao with Daoisms {

	def saveOrUpdate(course: Course): Unit = session.saveOrUpdate(course)

	def saveOrUpdate(courseYearWeighting: CourseYearWeighting): Unit = session.saveOrUpdate(courseYearWeighting)

	def getByCode(code: String): Option[Course] =
		session.newQuery[Course]("from Course course where code = :code").setString("code", code).uniqueResult

	def getAllCourseCodes: Seq[String] =
		session.newQuery[String]("select distinct code from Course").seq

	def findByDepartment(department: Department): Seq[Course] =
		session.newQuery[Course](
		"""
			select distinct course from Course course, StudentCourseDetails scd
	  	where scd.course = course and scd.department = :department
		"""
		).setParameter("department", department).seq

	def getCourseYearWeighting(courseCode: String, academicYear: AcademicYear, yearOfStudy: Int): Option[CourseYearWeighting] =
		session.newCriteria[CourseYearWeighting]
			.createAlias("course", "course")
			.add(is("course.code", courseCode))
			.add(is("academicYear", academicYear))
			.add(is("yearOfStudy", yearOfStudy))
			.uniqueResult

}
