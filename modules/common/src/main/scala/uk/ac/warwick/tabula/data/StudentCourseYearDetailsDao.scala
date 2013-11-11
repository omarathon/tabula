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
	def getBySceKey(studentCourseDetails: StudentCourseDetails, seq: Integer): Option[StudentCourseYearDetails]
	def getBySceKeyStaleOrFresh(studentCourseDetails: StudentCourseDetails, seq: Integer): Option[StudentCourseYearDetails]
	def getAllFreshInSits(): Seq[StudentCourseYearDetails]
}

@Repository
class StudentCourseYearDetailsDaoImpl extends StudentCourseYearDetailsDao with Daoisms {
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

	def getBySceKey(studentCourseDetails: StudentCourseDetails, seq: Integer): Option[StudentCourseYearDetails] =
		session.newCriteria[StudentCourseYearDetails]
			.add(is("studentCourseDetails", studentCourseDetails))
			.add(is("missingFromImportSince", null))
			.add(is("sceSequenceNumber", seq))
			.uniqueResult

	def getBySceKeyStaleOrFresh(studentCourseDetails: StudentCourseDetails, seq: Integer): Option[StudentCourseYearDetails] =
		session.newCriteria[StudentCourseYearDetails]
			.add(is("studentCourseDetails", studentCourseDetails))
			.add(is("sceSequenceNumber", seq))
			.uniqueResult

	def getAllFreshInSits() =
		session.newCriteria[StudentCourseYearDetails]
			.add(is("missingFromImportSince", null))
			.seq

}
