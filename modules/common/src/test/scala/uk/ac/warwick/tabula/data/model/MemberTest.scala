package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.TestBase
import org.junit.Test
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.services.ProfileService

class MemberTest extends TestBase with Mockito {
	
	@Test def testAffiliatedDepartments {
		val member = new Member()
		member.universityId = "01234567"
		
		// create their home department
		val homeDept = new Department
		homeDept.code = "zx"
			
		// create another department where they're taking a module
		val extDept = new Department
		extDept.code = "pi"
			
		// mock profile service to fetch list of registered modules (from these depts)
		val mod1 = new Module
		val mod2 = new Module
		mod1.department = extDept
		mod2.department = homeDept
		member.profileService = mock[ProfileService]
		member.profileService.getRegisteredModules(member.universityId) returns (Seq(mod1, mod2))
		
		// set home department and test
		member.homeDepartment = homeDept
		member.affiliatedDepartments should be (Seq(homeDept))
		member.touchedDepartments should be (Seq(homeDept, extDept))
		
		// also set study department and test
		val studyDept = new Department
		studyDept.code = "pq"
			
		member.studyDepartment = studyDept
		member.affiliatedDepartments should be (Seq(homeDept, studyDept))
		member.touchedDepartments should be (Seq(homeDept, studyDept, extDept))
		
		// also set route department and test
		val routeDept = new Department
		routeDept.code = "ch"
		val route = new Route
		route.department = routeDept
		member.route = route
		
		member.affiliatedDepartments should be (Seq(homeDept, studyDept, routeDept))
		member.touchedDepartments should be (Seq(homeDept, studyDept, routeDept, extDept))
		
		// reset route to home, and check it appears only once
		route.department = homeDept
		member.route = route
		
		member.affiliatedDepartments should be (Seq(homeDept, studyDept))
		member.touchedDepartments should be (Seq(homeDept, studyDept, extDept))
	}
}