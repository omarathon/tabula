package uk.ac.warwick.tabula.data

import org.hibernate.FetchMode
import org.hibernate.criterion.Order._
import org.hibernate.criterion.Projections
import org.springframework.stereotype.Repository
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports.{JBigDecimal, JList}
import uk.ac.warwick.tabula.data.model._
import scala.collection.JavaConverters._

trait ModuleRegistrationDaoComponent {
	val moduleRegistrationDao: ModuleRegistrationDao
}

trait AutowiringModuleRegistrationDaoComponent extends ModuleRegistrationDaoComponent {
	val moduleRegistrationDao: ModuleRegistrationDao = Wire[ModuleRegistrationDao]
}

trait ModuleRegistrationDao {
	def saveOrUpdate(moduleRegistration: ModuleRegistration): Unit
	def saveOrUpdate(coreRequiredModule: CoreRequiredModule): Unit
	def delete(coreRequiredModule: CoreRequiredModule): Unit
	def deleteByIds(ids: Seq[String]): Unit // delete by query, no hibernate cascade magic
	def getOrphaned: Seq[String] // modReg whose scj code cannot be found from scd
	def getByNotionalKey(
		studentCourseDetails: StudentCourseDetails,
		module: Module,
		cats: JBigDecimal,
		academicYear: AcademicYear,
		occurrence: String
	): Option[ModuleRegistration]
	def getByUsercodesAndYear(usercodes: Seq[String], academicYear: AcademicYear) : Seq[ModuleRegistration]
	def getByModuleAndYear(module: Module, academicYear: AcademicYear): Seq[ModuleRegistration]

	def findCoreRequiredModules(route: Route, academicYear: AcademicYear, yearOfStudy: Int): Seq[CoreRequiredModule]
	def findRegisteredUsercodes(module: Module, academicYear: AcademicYear): Seq[String]
}

@Repository
class ModuleRegistrationDaoImpl extends ModuleRegistrationDao with Daoisms {

	def saveOrUpdate(moduleRegistration: ModuleRegistration): Unit = session.saveOrUpdate(moduleRegistration)

	def saveOrUpdate(coreRequiredModule: CoreRequiredModule): Unit = session.saveOrUpdate(coreRequiredModule)

	def delete(coreRequiredModule: CoreRequiredModule): Unit = session.delete(coreRequiredModule)

	def deleteByIds(ids: Seq[String]): Unit = {
		val query = session.createQuery("""delete ModuleRegistration where id in (:ids)""")
		query.setParameterList("ids", ids.asJava)
		query.executeUpdate
		session.flush()
	}

	def getOrphaned: Seq[String] = {
		session.createSQLQuery(
			"""
				select MODULEREGISTRATION.ID
				from
				  MODULEREGISTRATION
				  left join
				  STUDENTCOURSEDETAILS
				    on
				      STUDENTCOURSEDETAILS.SCJCODE = MODULEREGISTRATION.SCJCODE
				where STUDENTCOURSEDETAILS.SCJCODE is null;
			""")
			.list
			.asInstanceOf[JList[String]]
			.asScala
	}

	def getByNotionalKey(
		studentCourseDetails: StudentCourseDetails,
		module: Module,
		cats: JBigDecimal,
		academicYear: AcademicYear,
		occurrence: String
	): Option[ModuleRegistration] =
		session.newCriteria[ModuleRegistration]
				.add(is("studentCourseDetails", studentCourseDetails))
				.add(is("module", module))
				.add(is("academicYear", academicYear))
				.add(is("cats", cats))
				.add(is("occurrence", occurrence))
				.uniqueResult

	def getByUsercodesAndYear(userCodes: Seq[String], academicYear: AcademicYear) : Seq[ModuleRegistration] = {
		val query = session.newQuery[ModuleRegistration]("""
				select distinct mr
					from ModuleRegistration mr
					where academicYear = :academicYear
					and studentCourseDetails.missingFromImportSince is null
					and studentCourseDetails.student.userId in :usercodes
				""")
					.setString("academicYear", academicYear.getStoreValue.toString)

		query.setParameterList("usercodes", userCodes)
					.seq
	}

	def getByModuleAndYear(module: Module, academicYear: AcademicYear): Seq[ModuleRegistration] = {
		session.newCriteria[ModuleRegistration]
			.createAlias("studentCourseDetails", "studentCourseDetails")
			.add(is("module", module))
			.add(is("academicYear", academicYear))
			.setFetchMode("studentCourseDetails", FetchMode.JOIN)
			.addOrder(asc("studentCourseDetails.scjCode"))
			.seq
	}

	def findCoreRequiredModules(route: Route, academicYear: AcademicYear, yearOfStudy: Int): Seq[CoreRequiredModule] = {
		session.newCriteria[CoreRequiredModule]
		  .add(is("route", route))
			.add(is("academicYear", academicYear))
			.add(is("yearOfStudy", yearOfStudy))
		  .seq
	}

	def findRegisteredUsercodes(module: Module, academicYear: AcademicYear): Seq[String] = {
		session.newCriteria[ModuleRegistration]
		  .createAlias("studentCourseDetails", "studentCourseDetails")
		 	.createAlias("studentCourseDetails.student", "student")
			.add(is("module", module))
	  	.add(is("academicYear", academicYear))
		  .project[String](Projections.property("student.userId"))
		  .seq
	}
}
