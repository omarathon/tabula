package uk.ac.warwick.tabula.services

import scala.collection.JavaConverters._

import org.junit.Before
import org.springframework.transaction.annotation.Transactional

import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.{AssignmentMembershipDaoImpl, DepartmentDaoImpl, ExtensionDaoComponent, ExtensionDaoImpl}
import uk.ac.warwick.tabula.data.model.UpstreamAssessmentGroup
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.data.model.forms.{CommentField, WordCountField}
import uk.ac.warwick.userlookup.User

class AssignmentMembershipServiceTest extends TestBase {

	@Transactional @Test def testDetermineMembership {
		val userLookup = new MockUserLookup
		userLookup.registerUsers("aaaaa", "bbbbb", "ccccc", "ddddd", "eeeee", "fffff")

		val user1 = userLookup.getUserByUserId("aaaaa")
		user1.setLastName("Aaaaa")
		val user2 = userLookup.getUserByUserId("bbbbb")
		user2.setLastName("Bbbbb")
		val user3 = userLookup.getUserByUserId("ccccc")
		user3.setLastName("Ccccc")
		val user4 = userLookup.getUserByUserId("ddddd")
		user4.setLastName("Ddddd")
		val user5 = userLookup.getUserByUserId("eeeee")
		user5.setLastName("Eeeee")
		val user6 = userLookup.getUserByUserId("fffff")
		user6.setLastName("Fffff")

		val assignmentMembershipService = new AssignmentMembershipServiceImpl
		assignmentMembershipService.userLookup = userLookup

		val uag = new UpstreamAssessmentGroup
		uag.assessmentGroup = "A"
		uag.moduleCode = "AM101"
		uag.members.add(user3)
		uag.members.add(user1)
		uag.members.add(user2)

		val other = UserGroup.ofUsercodes
		other.userLookup = userLookup

		other.add(user5)
		other.add(user4)
		other.add(user6)

		val upstream = Seq[UpstreamAssessmentGroup](uag)

		val others = Some(other)

		val info = assignmentMembershipService.determineMembership(upstream, others)
		info.items.size should be (6)
		info.items(0).userId should be (Some("aaaaa"))
		info.items(1).userId should be (Some("bbbbb"))
		info.items(2).userId should be (Some("ccccc"))
	}

}
