package uk.ac.warwick.courses.data.model

import uk.ac.warwick.courses.TestBase
import uk.ac.warwick.courses.Mockito
import org.junit.Test
import uk.ac.warwick.courses.services.UserLookupService
import uk.ac.warwick.userlookup.User
import org.junit.Before
import uk.ac.warwick.userlookup.AnonymousUser

class AssignmentMembershipTest extends TestBase with Mockito {

	var userDatabase = Seq(
		("0000000","aaaaa"),
		("0000001","aaaab"),
		("0000002","aaaac"),
		("0000003","aaaad"),
		("0000004","aaaae"),
		("0000005","aaaaf"),
		("0000006","aaaag"),
		("0000007","aaaah")
	) map { case(id,code) =>
		val user = new User(code)
		user.setWarwickId(id)
		user.setFullName("Roger " + code.head.toUpper + code.tail)
		user
	}
	
	var userLookup: UserLookupService = _
	val nobody = new UserGroup
	
	
	
	@Before def before {
		userLookup = mock[UserLookupService]
		userLookup.getUserByUserId(any[String]) answers { id =>
			userDatabase find {_.getUserId == id} getOrElse (new AnonymousUser())			
		}
		userLookup.getUserByWarwickUniId(any[String]) answers { id =>
			userDatabase find {_.getWarwickId == id} getOrElse (new AnonymousUser())
		}
	}
	
	@Test def empty {
		val membership = AssignmentMembership.determineMembership(None, nobody)(userLookup)
		membership.size should be (0)
	}
	
	@Test def plainSits {
		val upstream = newAssessmentGroup(Seq("0000005","0000006"))
		val membership = AssignmentMembership.determineMembership(Some(upstream), nobody)(userLookup)
		membership.size should be (2)
		membership(0).user.getFullName should be ("Roger Aaaaf")
		membership(1).user.getFullName should be ("Roger Aaaag")
	}
	
	@Test def includeAndExclude {
		val upstream = newAssessmentGroup(Seq("0000005","0000006"))
		val others = new UserGroup
		others.includeUsers.add("aaaaa")
		others.excludeUsers.add("aaaaf")
		val membership = AssignmentMembership.determineMembership(Some(upstream), others)(userLookup)
		println(membership)
		membership.size should be (3)
		
		membership(0).user.getFullName should be ("Roger Aaaaa")
		membership(0).itemType should be ("include")
		membership(0).extraneous should be (false)
		
		membership(1).user.getFullName should be ("Roger Aaaaf")
        membership(1).itemType should be ("exclude")
        membership(1).extraneous should be (false)
		
		membership(2).user.getFullName should be ("Roger Aaaag")
        membership(2).itemType should be ("sits")
        membership(2).extraneous should be (false)
	}
	
	/**
	 * Test that the "extraneous" flag is set because "aaaaf" is already
	 * part of the SITS group, and excluded code "aaaah" is not in the
	 * group anyway so the exclusion does nothing. 
	 */
	@Test def redundancy {
		val upstream = newAssessmentGroup(Seq("0000005","0000006"))
        val others = new UserGroup
        others.includeUsers.add("aaaaf")
        others.excludeUsers.add("aaaah")
        val membership = AssignmentMembership.determineMembership(Some(upstream), others)(userLookup)
        println(membership)
        membership.size should be (3)
        
        membership(0).user.getFullName should be ("Roger Aaaaf")
        membership(0).itemType should be ("include")
        membership(0).extraneous should be (true)
        
        membership(1).user.getFullName should be ("Roger Aaaah")
        membership(1).itemType should be ("exclude")
        membership(1).extraneous should be (true)
        
        membership(2).user.getFullName should be ("Roger Aaaag")
        membership(2).itemType should be ("sits")
        membership(2).extraneous should be (false)
	}
	
	
    def newAssessmentGroup(uniIds:Seq[String]) = {
        val upstream = new UpstreamAssessmentGroup
        uniIds foreach upstream.members.addUser
        upstream
    }
}