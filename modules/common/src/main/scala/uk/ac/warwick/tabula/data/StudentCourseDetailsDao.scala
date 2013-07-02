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
	def getStudentBySprCode(sprCode: String): Option[StudentMember]
	def getScjCodeBySprCode(sprCode: String): Option[String]
}

@Repository
class StudentCourseDetailsDaoImpl extends StudentCourseDetailsDao with Daoisms {
	import Restrictions._
	import Order._

	def saveOrUpdate(studentCourseDetails: StudentCourseDetails) = {
		session.saveOrUpdate(studentCourseDetails)
		session.flush()
	}

	def delete(studentCourseDetails: StudentCourseDetails) =  {
		session.delete(studentCourseDetails)
		session.flush()
	}

	def getByScjCode(scjCode: String) =
		session.newCriteria[StudentCourseDetails]
				.add(is("scjCode", scjCode.trim))
				.uniqueResult

	def getStudentBySprCode(sprCode: String) = {
		val scdList: Seq[StudentCourseDetails] = session.newCriteria[StudentCourseDetails]
				.add(is("sprCode", sprCode.trim)).list

		if (scdList.size > 0) Some(scdList.head.student)
		else None
	}

	def getScjCodeBySprCode(sprCode: String) = {
		val stuCourseDetails = session.newCriteria[StudentCourseDetails]
				.add(is("sprCode", sprCode.trim))
				.uniqueResult
		stuCourseDetails.map(_.scjCode)
	}
}
