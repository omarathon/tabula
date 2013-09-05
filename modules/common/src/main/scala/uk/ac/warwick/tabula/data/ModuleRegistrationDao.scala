package uk.ac.warwick.tabula.data

import org.hibernate.annotations.AccessType
import org.springframework.stereotype.Repository
import javax.persistence.Entity
import uk.ac.warwick.tabula.data.model.ModuleRegistration
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.AcademicYear

trait ModuleRegistrationDao {
	def saveOrUpdate(moduleRegistration: ModuleRegistration)
	def getByNotionalKey(studentCourseDetails: StudentCourseDetails, moduleCode: String, cats: Double, academicYear: AcademicYear): Option[ModuleRegistration]
}

@Repository
class ModuleRegistrationDaoImpl extends ModuleRegistrationDao with Daoisms {

	def saveOrUpdate(moduleRegistration: ModuleRegistration) = session.saveOrUpdate(moduleRegistration)

	def getByNotionalKey(studentCourseDetails: StudentCourseDetails, moduleCode: String, cats: Double, academicYear: AcademicYear) = {
		session.newCriteria[ModuleRegistration]
			.add(is("studentCourseDetails", studentCourseDetails))
			.add(is("moduleCode", moduleCode))
			.add(is("academicYear", academicYear))
			.add(is("cats", cats))
			.uniqueResult
	}

	def getByUsercodeAndYear(usercode: String, academicYear: AcademicYear) : Seq[ModuleRegistration] = {
		session.newQuery[ModuleRegistration]("""
				select distinct mr
					from ModuleRegistration mr
					where studentCourseDetails.student.userId = :usercode
					and academicYear = :academicYear
				""")
					.setString("usercode", usercode)
					.setString("academicYear", academicYear.getStoreValue.toString)
					.seq
	}
}
