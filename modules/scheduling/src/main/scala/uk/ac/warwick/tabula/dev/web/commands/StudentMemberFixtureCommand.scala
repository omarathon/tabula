package uk.ac.warwick.tabula.dev.web.commands

import uk.ac.warwick.tabula.commands.{Unaudited, ComposableCommand, CommandInternal}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.data.{RouteDao, MemberDao}
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
	var yearOfStudy: Int = 1

	var memberDao = Wire[MemberDao]
	var routeDao = Wire[RouteDao]

	def applyInternal() {
		val userLookupUser = userLookup.getUserByUserId(userId)
		assert(userLookupUser != null)
		transactional() {
			val existing = memberDao.getByUniversityId(userLookupUser.getWarwickId)
			val route = if (routeCode != "") routeDao.getByCode(routeCode) else None
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
			if (route.isDefined) scd.route = route.get
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


