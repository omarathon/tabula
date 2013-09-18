package uk.ac.warwick.tabula.dev.web.commands

import uk.ac.warwick.tabula.commands.{Unaudited, ComposableCommand, CommandInternal}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.data.Transactions.transactional
import uk.ac.warwick.tabula.services.{AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{StudentCourseYearDetails, StudentCourseDetails, Gender, StudentMember}
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions

class StudentMemberFixtureCommand extends CommandInternal[Unit] with Logging {
	this: UserLookupComponent =>


	var userId: String = _
	var genderCode: String = _
	var routeCode: String = ""
	var courseCode:String=""
	var yearOfStudy: Int = 1
  var deptCode:String=""

	var memberDao = Wire[MemberDao]
	var routeDao = Wire[RouteDao]
	var courseDao= Wire[CourseDao]
  var deptDao = Wire[DepartmentDao]
	var statusDao = Wire[SitsStatusDao]

	def applyInternal() {
		val userLookupUser = userLookup.getUserByUserId(userId)
		assert(userLookupUser != null)
		transactional() {
			val existing = memberDao.getByUniversityId(userLookupUser.getWarwickId)
			val route = if (routeCode != "") routeDao.getByCode(routeCode) else None
			val course = if (courseCode!= "") courseDao.getByCode(courseCode) else None
			val dept = if (deptCode!= "") deptDao.getByCode(deptCode) else None
			val currentStudentStatus = statusDao.getByCode("C").get

			existing foreach {
				memberDao.delete
			}

			val newMember = new StudentMember
			newMember.universityId = userLookupUser.getWarwickId
			newMember.userId = userId
			newMember.gender = Gender.fromCode(genderCode)
			newMember.firstName = userLookupUser.getFirstName
			newMember.lastName = userLookupUser.getLastName
			newMember.inUseFlag = "Active"

			val scd = new StudentCourseDetails(newMember, userLookupUser.getWarwickId + "/" + yearOfStudy)
			scd.mostSignificant = true
			scd.sprCode = scd.scjCode
			scd.sprStatus = currentStudentStatus

			if (route.isDefined) scd.route = route.get
			if (course.isDefined) scd.course = course.get
			if (dept.isDefined)  scd.department = dept.get
			val yd = new StudentCourseYearDetails(scd, 1)
			yd.yearOfStudy = yearOfStudy

			scd.attachStudentCourseYearDetails(yd)
			newMember.studentCourseDetails.add(scd)

			memberDao.saveOrUpdate(newMember)
		}
	}
}

object StudentMemberFixtureCommand {
	def apply() = {
		new StudentMemberFixtureCommand with ComposableCommand[Unit]
			with AutowiringUserLookupComponent
			with Unaudited
			with PubliclyVisiblePermissions
	}
}


