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

trait StudentCourseYearDetailsDao {
	def saveOrUpdate(studentCourseYearDetails: StudentCourseYearDetails)
	def delete(studentCourseYearDetails: StudentCourseYearDetails)
	def getStudentCourseYearDetails(id: String): Option[StudentCourseYearDetails]
	def getByScjCodeAndSequence(scjCode: String, seq: Integer): Option[StudentCourseYearDetails]
}

@Repository
class StudentCourseYearDetailsDaoImpl extends Daoisms {
	import Restrictions._
	import Order._

	def saveOrUpdate(studentCourseYearDetails: StudentCourseYearDetails) = {
		session.saveOrUpdate(studentCourseYearDetails)
	}

	def delete(studentCourseYearDetails: StudentCourseYearDetails) =  {
		session.delete(studentCourseYearDetails)
		session.flush()
	}

	def getStudentCourseYearDetails(id: String) = getById[StudentCourseYearDetails](id)

	def getByScjCodeAndSequenceNumber(scjCode: String, seq: Integer): Option[StudentCourseYearDetails] =
		session.newCriteria[StudentCourseYearDetails]
			.add(is("scjCode", scjCode))
			.add(is("sceSequenceNumber", seq))
			.uniqueResult
}
