package uk.ac.warwick.tabula.data

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable.HashSet

import org.hibernate.criterion._
import org.hibernate.criterion.Projections._
import org.hibernate.transform.Transformers
import org.joda.time.DateTime
import org.springframework.stereotype.Repository

import uk.ac.warwick.tabula.JavaImports.JList
import uk.ac.warwick.tabula.data.model._

trait StudentCourseYearDetailsDao {
	def saveOrUpdate(studentCourseYearDetails: StudentCourseYearDetails)
	def delete(studentCourseYearDetails: StudentCourseYearDetails)
	def getStudentCourseYearDetails(id: String): Option[StudentCourseYearDetails]
	def getBySceKey(studentCourseDetails: StudentCourseDetails, seq: Integer): Option[StudentCourseYearDetails]
	def getBySceKeyStaleOrFresh(studentCourseDetails: StudentCourseDetails, seq: Integer): Option[StudentCourseYearDetails]
	def getFreshKeys(): Seq[StudentCourseYearKey]
	def getIdFromKey(key: StudentCourseYearKey): Option[String]
	def stampMissingFromImport(seenIds: HashSet[String], importStart: DateTime)

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

	def getFreshKeys(): Seq[StudentCourseYearKey] = {
		session.newCriteria[StudentCourseYearDetails]
			.add(is("missingFromImportSince", null))
				.project[Array[Any]](
					projectionList()
						.add(property("studentCourseDetails.scjCode"), "scjCode")
						.add(property("sceSequenceNumber"), "sceSequenceNumber")
				)
		.setResultTransformer(Transformers.aliasToBean(classOf[StudentCourseYearKey]))
		.seq
	}

	def stampMissingFromImport(seenIds: HashSet[String], importStart: DateTime) =
		session.createQuery("""
				update
					StudentCourseDetails
				set
					missingFromImportSince = :importStart
				where
					id not in (:seenIds)
				""")
			.setParameter("importStart", importStart)
			.setParameterList("seenIds", seenIds)
			.executeUpdate

	def getIdFromKey(key: StudentCourseYearKey) = {
		session.newCriteria[StudentCourseYearDetails]
		.add(is("studentCourseDetails.scjCode", key.scjCode))
		.add(is("sceSequenceNumber", key.sceSequenceNumber))
		.project[String](Projections.property("scjCode"))
		.uniqueResult

	}
}
