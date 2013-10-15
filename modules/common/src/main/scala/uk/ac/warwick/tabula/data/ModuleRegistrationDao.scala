package uk.ac.warwick.tabula.data

import org.hibernate.annotations.AccessType
import org.springframework.stereotype.Repository
import javax.persistence.Entity
import uk.ac.warwick.tabula.data.model.ModuleRegistration
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Module
import org.hibernate.criterion.Restrictions

trait ModuleRegistrationDao {
	def saveOrUpdate(moduleRegistration: ModuleRegistration)
	def getByNotionalKey(studentCourseDetails: StudentCourseDetails, module: Module, cats: java.math.BigDecimal, academicYear: AcademicYear, occurrence: String): Option[ModuleRegistration]
	def getByUsercodesAndYear(usercodes: Seq[String], academicYear: AcademicYear) : Seq[ModuleRegistration]
}

@Repository
class ModuleRegistrationDaoImpl extends ModuleRegistrationDao with Daoisms {

	def saveOrUpdate(moduleRegistration: ModuleRegistration) = session.saveOrUpdate(moduleRegistration)

	def getByNotionalKey(studentCourseDetails: StudentCourseDetails, module: Module, cats: java.math.BigDecimal, academicYear: AcademicYear, occurrence: String) =
		session.newCriteria[ModuleRegistration]
				.add(is("studentCourseDetails", studentCourseDetails))
				.add(is("module", module))
				.add(is("academicYear", academicYear))
				.add(is("cats", cats))
				.add(is("occurrence", occurrence))
				.uniqueResult

	def getByUsercodesAndYear(userCodes: Seq[String], academicYear: AcademicYear) : Seq[ModuleRegistration] = {
		//val usercodesInQuotes = usercodes.map { code => "'" + code + "'"}
		//val usercodeString: String = usercodesInQuotes.mkString(", ")

		session.newQuery[ModuleRegistration]("""
				select distinct mr
					from ModuleRegistration mr
					where academicYear = :academicYear
					and studentCourseDetails.student.userId in :usercodes
				""")
					.setParameterList("usercodes", userCodes)
					.setString("academicYear", academicYear.getStoreValue.toString)
					.seq
	}
}
