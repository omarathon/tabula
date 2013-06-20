package uk.ac.warwick.tabula.data
import org.springframework.stereotype.Repository
import org.hibernate.SessionFactory
import model.Module
import org.hibernate.`type`._
import org.springframework.beans.factory.annotation.Autowired
import collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import model.Department
import uk.ac.warwick.tabula.data.model.Route
import uk.ac.warwick.tabula.data.model.SitsStatus
import uk.ac.warwick.tabula.data.model.Course

trait CourseDao {
	def saveOrUpdate(course: Course)
	def getByCode(code: String): Option[Course]
	def getAllCourseCodes: Seq[String]

	def getName(code: String): Option[String]
}

@Repository
class CourseDaoImpl extends CourseDao with Daoisms {

	def saveOrUpdate(course: Course) = session.saveOrUpdate(course)

	def getByCode(code: String) =
		session.newQuery[Course]("from Course course where code = :code").setString("code", code).uniqueResult

	def getAllCourseCodes: Seq[String] =
		session.newQuery[String]("select distinct code from Course").seq


	def getName(code: String): Option[String] =
		session.newQuery[String]("select name from Course where code = :code").setString("code", code).uniqueResult
}
