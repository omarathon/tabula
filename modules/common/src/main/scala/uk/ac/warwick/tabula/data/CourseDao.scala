package uk.ac.warwick.tabula.data
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model.{Department, Course}
import uk.ac.warwick.spring.Wire

trait CourseDaoComponent {
	val courseDao: CourseDao
}

trait AutowiringCourseDaoComponent extends CourseDaoComponent {
	val courseDao = Wire[CourseDao]
}

trait CourseDao {
	def saveOrUpdate(course: Course)
	def getByCode(code: String): Option[Course]
	def getAllCourseCodes: Seq[String]
	def findByDepartment(department: Department): Seq[Course]
}

@Repository
class CourseDaoImpl extends CourseDao with Daoisms {

	def saveOrUpdate(course: Course) = session.saveOrUpdate(course)

	def getByCode(code: String) =
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

}
