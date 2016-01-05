package uk.ac.warwick.tabula.data

import org.hibernate.FetchMode
import org.hibernate.criterion.Projections._
import org.hibernate.criterion._
import org.hibernate.transform.Transformers
import org.joda.time.{LocalDate, DateTime}
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model._

import scala.collection.mutable

trait StudentCourseYearDetailsDao {
	def saveOrUpdate(studentCourseYearDetails: StudentCourseYearDetails)
	def delete(studentCourseYearDetails: StudentCourseYearDetails)
	def getStudentCourseYearDetails(id: String): Option[StudentCourseYearDetails]
	def getBySceKey(studentCourseDetails: StudentCourseDetails, seq: Integer): Option[StudentCourseYearDetails]
	def getBySceKeyStaleOrFresh(studentCourseDetails: StudentCourseDetails, seq: Integer): Option[StudentCourseYearDetails]
	def getFreshIds: Seq[String]
	def getFreshKeys: Seq[StudentCourseYearKey]
	def getIdFromKey(key: StudentCourseYearKey): Option[String]
	def convertKeysToIds(keys: mutable.HashSet[StudentCourseYearKey]): mutable.HashSet[String]
	def stampMissingFromImport(newStaleScydIds: Seq[String], importStart: DateTime)

	def findByCourseRouteYear(academicYear: AcademicYear, course: Course, route: Route, yearOfStudy: Int, eagerLoad: Boolean = false): Seq[StudentCourseYearDetails]
}

@Repository
class StudentCourseYearDetailsDaoImpl extends StudentCourseYearDetailsDao with Daoisms {

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

	def getBySceKeyStaleOrFresh(studentCourseDetails: StudentCourseDetails, seq: Integer): Option[StudentCourseYearDetails] = {
		sessionWithoutFreshFilters.newCriteria[StudentCourseYearDetails]
			.add(is("studentCourseDetails", studentCourseDetails))
			.add(is("sceSequenceNumber", seq))
			.uniqueResult
	}

	def getFreshKeys: Seq[StudentCourseYearKey] = {
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

	def getFreshIds = {
		session.newCriteria[StudentCourseYearDetails]
			.add(isNull("missingFromImportSince"))
			.project[String](Projections.property("id"))
		.seq
	}

	// TODO - put these two methods in a service
	def convertKeysToIds(keys: mutable.HashSet[StudentCourseYearKey]): mutable.HashSet[String] =
			keys.flatMap {
				key => getIdFromKey(key)
			}

	def getIdFromKey(key: StudentCourseYearKey) = {
		session.newCriteria[StudentCourseYearDetails]
		.add(is("studentCourseDetails.scjCode", key.scjCode))
		.add(is("sceSequenceNumber", key.sceSequenceNumber))
		.project[String](Projections.property("id"))
		.uniqueResult
	}

	def stampMissingFromImport(newStaleScydIds: Seq[String], importStart: DateTime) = {
		newStaleScydIds.grouped(Daoisms.MaxInClauseCount).foreach { newStaleIds =>
			val sqlString = """
				update
					StudentCourseYearDetails
				set
					missingFromImportSince = :importStart
				where
					id in (:newStaleScydIds)
				"""

				session.newQuery(sqlString)
					.setParameter("importStart", importStart)
					.setParameterList("newStaleScydIds", newStaleIds)
					.executeUpdate()
		}
	}

	def findByCourseRouteYear(academicYear: AcademicYear, course: Course, route: Route, yearOfStudy: Int, eagerLoad: Boolean = false): Seq[StudentCourseYearDetails] = {
		val c = session.newCriteria[StudentCourseYearDetails]
			.createAlias("studentCourseDetails", "scd")
			.add(is("academicYear", academicYear))
			.add(is("yearOfStudy", yearOfStudy))
			.add(is("scd.course", course))
			.add(is("scd.route", route))
			.add(Restrictions.or(
				Restrictions.isNull("scd.endDate"),
				Restrictions.ge("scd.endDate", LocalDate.now)
			))

		if (eagerLoad) {
			c.setFetchMode("studentCourseDetails", FetchMode.JOIN)
				.setFetchMode("studentCourseDetails.studentMember", FetchMode.JOIN)
		}

		c.seq
	}
}

trait StudentCourseYearDetailsDaoComponent {
	def studentCourseYearDetailsDao: StudentCourseYearDetailsDao
}

trait AutowiringStudentCourseYearDetailsDaoComponent extends StudentCourseYearDetailsDaoComponent{
	var studentCourseYearDetailsDao = Wire[StudentCourseYearDetailsDao]
}
