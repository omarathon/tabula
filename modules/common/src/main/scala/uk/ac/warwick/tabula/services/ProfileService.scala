package uk.ac.warwick.tabula.services

import org.joda.time.DateTime
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.PrsCode
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.Logging
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.commands.FiltersStudents

/**
 * Service providing access to members and profiles.
 */
trait ProfileService {
	def save(member: Member)
	def getMemberByUniversityId(universityId: String): Option[Member]
	def getAllMembersWithUniversityIds(universityIds: Seq[String]): Seq[Member]
	def getMemberByPrsCode(prsCode: String): Option[Member]
	def getAllMembersWithUserId(userId: String, disableFilter: Boolean = false): Seq[Member]
	def getMemberByUserId(userId: String, disableFilter: Boolean = false): Option[Member]
	def getStudentBySprCode(sprCode: String): Option[StudentMember]
	def findMembersByQuery(query: String, departments: Seq[Department], userTypes: Set[MemberUserType], isGod: Boolean): Seq[Member]
	def findMembersByDepartment(department: Department, includeTouched: Boolean, userTypes: Set[MemberUserType]): Seq[Member]
	def listMembersUpdatedSince(startDate: DateTime, max: Int): Seq[Member]
	def countStudentsByDepartment(department: Department): Int
	def getStudentsByRoute(route: Route): Seq[StudentMember]
	def getStudentsByRoute(route: Route, academicYear: AcademicYear): Seq[StudentMember]
	def getStudentCourseDetailsByScjCode(scjCode: String): Option[StudentCourseDetails]
	def getStudentCourseDetailsBySprCode(sprCode: String): Seq[StudentCourseDetails]
	def countStudentsByRestrictions(department: Department, restrictions: Seq[ScalaRestriction]): Int
	def findStudentsByRestrictions(department: Department, restrictions: Seq[ScalaRestriction], orders: Seq[ScalaOrder] = Seq(), maxResults: Int = 50, startResult: Int = 0): Seq[StudentMember]
	def findAllStudentsByRestrictions(department: Department, restrictions: Seq[ScalaRestriction], orders: Seq[ScalaOrder] = Seq()): Seq[StudentMember]
	def findAllUniversityIdsByRestrictions(department: Department, restrictions: Seq[ScalaRestriction]): Seq[String]
	def allModesOfAttendance(department: Department): Seq[ModeOfAttendance]
	def allSprStatuses(department: Department): Seq[SitsStatus]
}

abstract class AbstractProfileService extends ProfileService with Logging {

	self: MemberDaoComponent with StudentCourseDetailsDaoComponent =>

	var profileIndexService = Wire.auto[ProfileIndexService]

	def getMemberByUniversityId(universityId: String) = transactional(readOnly = true) {
		memberDao.getByUniversityId(universityId)
	}

	def getAllMembersWithUniversityIds(universityIds: Seq[String]) = transactional(readOnly = true) {
		memberDao.getAllWithUniversityIds(universityIds)
	}

	def getAllMembersWithUserId(userId: String, disableFilter: Boolean = false) = transactional(readOnly = true) {
		memberDao.getAllByUserId(userId, disableFilter)
	}

	def getMemberByUserId(userId: String, disableFilter: Boolean = false) = transactional(readOnly = true) {
		memberDao.getByUserId(userId, disableFilter)
	}

	def getStudentBySprCode(sprCode: String) = transactional(readOnly = true) {
		studentCourseDetailsDao.getStudentBySprCode(sprCode)
	}

	def getMemberByPrsCode(prsCode: String) = transactional(readOnly = true) {
		if (prsCode != null && prsCode.length() > 2) {
			PrsCode.getUniversityId(prsCode) match {
				case Some(uniId: String) => memberDao.getByUniversityId(uniId)
				case None => None
			}
		}
		else None
	}

	def findMembersByQuery(query: String, departments: Seq[Department], userTypes: Set[MemberUserType], isGod: Boolean) = transactional(readOnly = true) {
		profileIndexService.find(query, departments, userTypes, isGod)
	}

	def findMembersByDepartment(department: Department, includeTouched: Boolean, userTypes: Set[MemberUserType]) = transactional(readOnly = true) {
		profileIndexService.find(department, includeTouched, userTypes)
	}

	def listMembersUpdatedSince(startDate: DateTime, max: Int) = transactional(readOnly = true) {
		memberDao.listUpdatedSince(startDate, max)
	}

	def save(member: Member) = memberDao.saveOrUpdate(member)

	def saveOrUpdate(relationship: StudentRelationship) = memberDao.saveOrUpdate(relationship)

  def countStudentsByDepartment(department: Department): Int = transactional(readOnly = true) {
			memberDao.getStudentsByDepartment(department.rootDepartment).count(department.filterRule.matches)
	}

	def getStudentsByRoute(route: Route): Seq[StudentMember] = transactional(readOnly = true) {
		studentCourseDetailsDao.getByRoute(route)
			.filter{s => s.sprStatus!= null && !s.sprStatus.code.startsWith("P")}
			.filter(s => s.mostSignificant == true)
			.map(_.student)
	}

	def getStudentsByRoute(route: Route, academicYear: AcademicYear): Seq[StudentMember] = transactional(readOnly = true) {
		studentCourseDetailsDao.getByRoute(route)
			.filter{s => s.sprStatus!= null && !s.sprStatus.code.startsWith("P")}
			.filter(s => s.mostSignificant == true)
			.filter(_.freshStudentCourseYearDetails.exists(s => s.academicYear == academicYear))
			.map(_.student)
	}

	def getStudentCourseDetailsByScjCode(scjCode: String): Option[StudentCourseDetails] =
		studentCourseDetailsDao.getByScjCode(scjCode)

	def getStudentCourseDetailsBySprCode(sprCode: String): Seq[StudentCourseDetails] =
		studentCourseDetailsDao.getBySprCode(sprCode)

	private def studentDepartmentFilterMatches(department: Department)(member: StudentMember) = department.filterRule.matches(member)

	def findStudentsByRestrictions(department: Department, restrictions: Seq[ScalaRestriction], orders: Seq[ScalaOrder] = Seq(), maxResults: Int = 50, startResult: Int = 0) = transactional(readOnly = true) {
		// If we're a sub-department then we have to fetch everyone, rhubarb! Otherwise, we can use nice things
		if (department.hasParent) {
			val allRestrictions = ScalaRestriction.is(
				"studentCourseYearDetails.enrolmentDepartment", department.rootDepartment,
				FiltersStudents.AliasPaths("studentCourseYearDetails") : _*
			) ++ restrictions

			memberDao.findStudentsByRestrictions(allRestrictions, orders, Int.MaxValue, 0)
				.filter(studentDepartmentFilterMatches(department))
				.slice(startResult, startResult + maxResults)
		}	else {
			val allRestrictions = ScalaRestriction.is(
				"studentCourseYearDetails.enrolmentDepartment", department,
				FiltersStudents.AliasPaths("studentCourseYearDetails") : _*
			) ++ restrictions

			memberDao.findStudentsByRestrictions(allRestrictions, orders, maxResults, startResult)
		}
	}

	def findAllStudentsByRestrictions(department: Department, restrictions: Seq[ScalaRestriction], orders: Seq[ScalaOrder] = Seq()) = transactional(readOnly = true) {
		if (department.hasParent) {
			val allRestrictions = ScalaRestriction.is(
				"studentCourseYearDetails.enrolmentDepartment", department.rootDepartment,
				FiltersStudents.AliasPaths("studentCourseYearDetails") : _*
			) ++ restrictions

			memberDao.findStudentsByRestrictions(allRestrictions, orders, Int.MaxValue, 0)
				.filter(studentDepartmentFilterMatches(department))
		}	else {
			val allRestrictions = ScalaRestriction.is(
				"studentCourseYearDetails.enrolmentDepartment", department,
				FiltersStudents.AliasPaths("studentCourseYearDetails") : _*
			) ++ restrictions

			memberDao.findStudentsByRestrictions(allRestrictions, orders, Int.MaxValue, 0)
		}
	}

	def findAllUniversityIdsByRestrictions(department: Department, restrictions: Seq[ScalaRestriction]) = transactional(readOnly = true) {
		val allRestrictions = {
			if (department.hasParent) {
				ScalaRestriction.is(
					"studentCourseYearDetails.enrolmentDepartment", department.rootDepartment,
					FiltersStudents.AliasPaths("studentCourseYearDetails") : _*
				) ++ restrictions
			}	else {
				ScalaRestriction.is(
					"studentCourseYearDetails.enrolmentDepartment", department,
					FiltersStudents.AliasPaths("studentCourseYearDetails") : _*
				) ++ restrictions
			}
		}
		memberDao.findUniversityIdsByRestrictions(allRestrictions)
	}

	def countStudentsByRestrictions(department: Department, restrictions: Seq[ScalaRestriction]): Int = transactional(readOnly = true) {
		// Because of the implementation of sub-departments, unfortunately we can't get optimisations here.
		if (department.hasParent) findAllStudentsByRestrictions(department, restrictions).size
		else {
			val allRestrictions = ScalaRestriction.is(
				"studentCourseYearDetails.enrolmentDepartment", department,
				FiltersStudents.AliasPaths("studentCourseYearDetails") : _*
			) ++ restrictions

			memberDao.countStudentsByRestrictions(allRestrictions)
		}
	}

	def allModesOfAttendance(department: Department): Seq[ModeOfAttendance] = transactional(readOnly = true) {
		memberDao.getAllModesOfAttendance(department).filter(_ != null)
	}
	def allSprStatuses(department: Department): Seq[SitsStatus] = transactional(readOnly = true) {
		memberDao.getAllSprStatuses(department).filter(_ != null)
	}
}

trait ProfileServiceComponent {
	def profileService: ProfileService
}

trait AutowiringProfileServiceComponent extends ProfileServiceComponent {
	var profileService = Wire[ProfileService]
}

@Service("profileService")
class ProfileServiceImpl
	extends AbstractProfileService
	with AutowiringMemberDaoComponent
	with AutowiringStudentCourseDetailsDaoComponent
