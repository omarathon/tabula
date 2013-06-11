package uk.ac.warwick.tabula.data

import scala.collection.JavaConversions._
import org.hibernate.annotations.AccessType
import org.hibernate.annotations.FilterDefs
import org.hibernate.annotations.Filters
import org.hibernate.criterion._
import org.joda.time.DateTime
import org.springframework.stereotype.Repository
import javax.persistence.Entity
import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.data.model._

trait StudentCourseDetailsDao {
	def saveOrUpdate(studentCourseDetails: StudentCourseDetails)
	def delete(studentCourseDetails: StudentCourseDetails)
	def getByScjCode(scjCode: String): Option[StudentCourseDetails]
}

@Repository
class StudentCourseDetailsDaoImpl extends Daoisms {
	import Restrictions._
	import Order._

	def saveOrUpdate(studentCourseDetails: StudentCourseDetails) = {
		session.saveOrUpdate(studentCourseDetails)
	}

	def delete(studentCourseDetails: StudentCourseDetails) =  {
		session.delete(studentCourseDetails)
		session.flush()
	}

	def getByScjCode(scjCode: String) =
		session.newCriteria[StudentCourseDetails]
				.add(is("scjCode", scjCode.trim))
				.uniqueResult
}
