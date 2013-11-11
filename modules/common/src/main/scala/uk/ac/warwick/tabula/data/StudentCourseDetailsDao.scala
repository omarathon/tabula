package uk.ac.warwick.tabula.data

import scala.collection.JavaConversions._
import org.springframework.stereotype.Repository
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.spring.Wire
import org.hibernate.criterion.Restrictions

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
	def getAllFreshInSits: Seq[StudentCourseDetails]
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
				.add(is("missingFromImportSince", null))
				.uniqueResult

	def getByScjCodeStaleOrFresh(scjCode: String) =
		session.newCriteria[StudentCourseDetails]
				.add(is("scjCode", scjCode.trim))
				.uniqueResult

	def getBySprCode(sprCode: String) =
		session.newCriteria[StudentCourseDetails]
				.add(is("sprCode", sprCode.trim))
				.add(is("missingFromImportSince", null))
				.seq

	def findByDepartment(department:Department):Seq[StudentCourseDetails] = {
		session.newCriteria[StudentCourseDetails]
			.add(is("department", department))
			.add(is("missingFromImportSince", null))
			.list
	}

	def getStudentBySprCode(sprCode: String) = {
		val scdList: Seq[StudentCourseDetails] = session.newCriteria[StudentCourseDetails]
				.add(is("sprCode", sprCode.trim))
				.add(is("missingFromImportSince", null))
				.list

		if (scdList.size > 0) Some(scdList.head.student)
		else None
	}

	def getByRoute(route: Route) = {
		session.newCriteria[StudentCourseDetails]
			.add(is("route.code", route.code))
			.add(is("missingFromImportSince", null))
			.seq
	}

	def getAllFreshInSits() =
		session.newCriteria[StudentCourseDetails]
			.add(is("missingFromImportSince", null))
			.seq
}
