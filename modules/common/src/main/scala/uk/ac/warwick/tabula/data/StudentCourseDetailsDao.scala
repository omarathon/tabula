package uk.ac.warwick.tabula.data

import scala.collection.mutable.HashSet
import org.hibernate.annotations.AccessType
import org.joda.time.DateTime
import org.springframework.stereotype.Repository
import javax.persistence.{DiscriminatorValue, Entity, NamedQueries}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{Department, Route, StudentCourseDetails, StudentMember}
import org.hibernate.criterion.Projections

trait StudentCourseDetailsDaoComponent {
	val studentCourseDetailsDao: StudentCourseDetailsDao
}

trait AutowiringStudentCourseDetailsDaoComponent extends StudentCourseDetailsDaoComponent {
	val studentCourseDetailsDao = Wire[StudentCourseDetailsDao]
}

trait StudentCourseDetailsDao {
	def saveOrUpdate(studentCourseDetails: StudentCourseDetails)
	def delete(studentCourseDetails: StudentCourseDetails)
	def getByScjCode(scjCode: String): Option[StudentCourseDetails]
	def getByScjCodeStaleOrFresh(scjCode: String): Option[StudentCourseDetails]
	def getBySprCode(sprCode: String): Seq[StudentCourseDetails]
	def getStudentBySprCode(sprCode: String): Option[StudentMember]
	def getByRoute(route: Route) : Seq[StudentCourseDetails]
	def findByDepartment(department:Department):Seq[StudentCourseDetails]
	def getFreshScjCodes: Seq[String]
	def stampMissingFromImport(newStaleScjCodes: Seq[String], importStart: DateTime)
}

@Repository
class StudentCourseDetailsDaoImpl extends StudentCourseDetailsDao with Daoisms {

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
				.add(isNull("missingFromImportSince"))
				.uniqueResult

	def getByScjCodeStaleOrFresh(scjCode: String) =
		session.newCriteria[StudentCourseDetails]
				.add(is("scjCode", scjCode.trim))
				.uniqueResult

	def getBySprCode(sprCode: String) =
		session.newCriteria[StudentCourseDetails]
				.add(is("sprCode", sprCode.trim))
				.add(isNull("missingFromImportSince"))
				.seq

	def findByDepartment(department:Department):Seq[StudentCourseDetails] = {
		session.newCriteria[StudentCourseDetails]
			.add(is("department", department))
			.add(isNull("missingFromImportSince"))
			.seq
	}

	def getStudentBySprCode(sprCode: String) = {
		val scdList: Seq[StudentCourseDetails] = session.newCriteria[StudentCourseDetails]
				.add(is("sprCode", sprCode.trim))
				.add(isNull("missingFromImportSince"))
				.seq

		if (scdList.size > 0) Some(scdList.head.student)
		else None
	}

	def getByRoute(route: Route) = {
		session.newCriteria[StudentCourseDetails]
			.add(is("route.code", route.code))
			.add(isNull("missingFromImportSince"))
			.seq
	}

	def getFreshScjCodes() =
		session.newCriteria[StudentCourseDetails]
			.add(isNull("missingFromImportSince"))
			.project[String](Projections.property("scjCode"))
			.seq

	def stampMissingFromImport(newStaleScjCodes: Seq[String], importStart: DateTime) = {
		newStaleScjCodes.grouped(Daoisms.MaxInClauseCount).foreach { newStaleCodes =>
			var sqlString = """
				update
					StudentCourseDetails
				set
					missingFromImportSince = :importStart
				where
					scjCode in (:newStaleScjCodes)
				"""

				session.newQuery(sqlString)
					.setParameter("importStart", importStart)
					.setParameterList("newStaleScjCodes", newStaleCodes)
					.executeUpdate
			}
	}
}
