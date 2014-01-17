package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.roles.DepartmentalAdministratorRoleDefinition
import uk.ac.warwick.tabula.roles.ExtensionManagerRoleDefinition
import collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupAllocationMethod.{StudentSignUp, Manual}
import uk.ac.warwick.tabula.data.model.Department._
import scala.util.{Failure, Try}
import uk.ac.warwick.tabula.helpers.Tap
import Tap.tap
import uk.ac.warwick.tabula.JavaImports.JArrayList
import uk.ac.warwick.tabula.data.model.Department.CompositeFilterRule
import scala.util.Failure
import uk.ac.warwick.tabula.data.model.permissions.CustomRoleDefinition
import uk.ac.warwick.tabula.roles.StudentRelationshipAgentRoleDefinition
import uk.ac.warwick.tabula.permissions.PermissionsSelector

class DepartmentTest extends TestBase with Mockito {

	val permissionsService = mock[PermissionsService]

	@Test def settings {
		val department = new Department
		department.collectFeedbackRatings should be (false)
		department.allowExtensionRequests should be (false)
		department.canRequestExtension should be (false)
		department.extensionGuidelineSummary should be (null)
		department.formattedGuidelineSummary should be ("")
		department.extensionGuidelineLink should be (null)
		department.showStudentName should be (false)
		department.plagiarismDetectionEnabled should be (true)
		department.turnitinExcludeBibliography should be (true)
		department.turnitinExcludeQuotations should be (true)
		department.turnitinSmallMatchPercentageLimit should be (0)
		department.turnitinSmallMatchWordLimit should be (0)
    department.defaultGroupAllocationMethod should be (Manual)
		department.autoGroupDeregistration should be (true)

		department.collectFeedbackRatings = true
		department.allowExtensionRequests = true
		department.extensionGuidelineSummary = "Here is my magic summary.\n\n    Do everything good!"
		department.extensionGuidelineLink = "http://warwick.ac.uk"
		department.showStudentName = true
		department.plagiarismDetectionEnabled = false
		department.turnitinExcludeBibliography = false
		department.turnitinExcludeQuotations = false
		department.turnitinSmallMatchPercentageLimit = 0
		department.turnitinSmallMatchWordLimit = 50
    department.defaultGroupAllocationMethod = StudentSignUp
    department.autoGroupDeregistration = false

		department.collectFeedbackRatings should be (true)
		department.allowExtensionRequests should be (true)
		department.canRequestExtension should be (true)
		department.extensionGuidelineSummary should be ("Here is my magic summary.\n\n    Do everything good!")
		department.formattedGuidelineSummary should be ("<p>Here is my magic summary.</p><p>Do everything good!</p>")
		department.extensionGuidelineLink should be ("http://warwick.ac.uk")
		department.showStudentName should be (true)
		department.plagiarismDetectionEnabled should be (false)
		department.turnitinExcludeBibliography should be (false)
		department.turnitinExcludeQuotations should be (false)
		department.turnitinSmallMatchPercentageLimit should be (0)
		department.turnitinSmallMatchWordLimit should be (50)
    department.defaultGroupAllocationMethod should be (StudentSignUp)
		department.autoGroupDeregistration should be (false)
	}

	@Test def groups {
		val department = new Department
		department.permissionsService = permissionsService

		val ownersGroup = UserGroup.ofUsercodes
		val extmanGroup = UserGroup.ofUsercodes

		permissionsService.ensureUserGroupFor(department, DepartmentalAdministratorRoleDefinition) returns (ownersGroup)
		permissionsService.ensureUserGroupFor(department, ExtensionManagerRoleDefinition) returns (extmanGroup)

		department.isOwnedBy("cuscav") should be (false)

		department.owners.addUser("cuscav")
		department.owners.addUser("cusebr")
		department.owners.addUser("curef")

		department.owners.removeUser("cusebr")

		department.isOwnedBy("cuscav") should be (true)
		department.isOwnedBy("curef") should be (true)
		department.isOwnedBy("cusebr") should be (false)

		ownersGroup.includeUsers.asScala.toSeq should be (Seq("cuscav", "curef"))

		department.isExtensionManager("cuscav") should be (false)
		extmanGroup.addUser("cuscav")
		department.isExtensionManager("cuscav") should be (true)
	}

	@Test
	def filterRuleDefaultsToAll(){
		val department = new Department
		department.filterRule should be (AllMembersFilterRule)
	}

	private trait FilterRuleFixture{
		val undergraduate = new StudentMember().tap(m=>{
			val scd = new StudentCourseDetails().tap(s=>{
				s.mostSignificant = true
				s.attachStudentCourseYearDetails(new StudentCourseYearDetails().tap(_.yearOfStudy =1))
				s.route = new Route().tap(_.degreeType = DegreeType.Undergraduate)
			})
			m.attachStudentCourseDetails(scd)
			m.mostSignificantCourse = scd
		})
		val postgraduate= new StudentMember().tap(m=>{
			val scd = new StudentCourseDetails().tap(s=>{
				s.mostSignificant = true
				s.attachStudentCourseYearDetails(new StudentCourseYearDetails().tap(_.yearOfStudy =7))
				s.route = new Route().tap(_.degreeType = DegreeType.Postgraduate)
			})
			m.attachStudentCourseDetails(scd)
			m.mostSignificantCourse = scd
		})

		val notStudentMemeber = new StaffMember()
	}

	@Test
	def AllMembersFilterRuleLetsAnyoneIn(){new FilterRuleFixture {
		val rule = AllMembersFilterRule
		rule.matches(notStudentMemeber) should be(true)
		rule.matches(undergraduate) should be(true)
		rule.matches(postgraduate) should be(true)
	}}

	/**
	 * The undergraduate / postgraduate filter rules use the Course Type enum
	 * from the StudentCourseDetails to determine degree type.
	 */
	@Test
	def UGFilterRuleAllowsUndergrads(){new FilterRuleFixture {
		val rule = UndergraduateFilterRule
		rule.matches(notStudentMemeber) should be(false)
		rule.matches(undergraduate) should be(true)
		rule.matches(postgraduate) should be(false)

		// test the various remaining different route types
		postgraduate.mostSignificantCourseDetails.get.route.degreeType = DegreeType.InService
		rule.matches(postgraduate) should be(false)

		postgraduate.mostSignificantCourseDetails.get.route.degreeType = DegreeType.PGCE
		rule.matches(postgraduate) should be(false)
	}}

	@Test
	def PGFilterRuleAllowsPostgrads(){new FilterRuleFixture {
		val rule = PostgraduateFilterRule
		rule.matches(notStudentMemeber) should be(false)
		rule.matches(undergraduate) should be(false)
		rule.matches(postgraduate) should be(true)

		// test the various remaining different course types
		postgraduate.mostSignificantCourseDetails.get.route.degreeType = DegreeType.InService
		rule.matches(postgraduate) should be(true)

		postgraduate.mostSignificantCourseDetails.get.route.degreeType = DegreeType.PGCE
		rule.matches(postgraduate) should be(true)

	}}

	@Test
	def YearOfStudyRuleAllowsMatchingYear(){new FilterRuleFixture {
		val firstYearRule = new InYearFilterRule(1)
		val secondYearRule = new InYearFilterRule(2)
		firstYearRule.matches(undergraduate) should be (true)
		firstYearRule.matches(postgraduate) should be(false)
		firstYearRule.matches(notStudentMemeber) should be(false)

		secondYearRule.matches(undergraduate) should be(false)
		undergraduate.mostSignificantCourseDetails.get.latestStudentCourseYearDetails.yearOfStudy = 2
		secondYearRule.matches(undergraduate) should be(true)
		firstYearRule.matches(undergraduate) should be (false)
	}}
	
	@Test
	def replacedRoleDefinitionFor {
		val department = new Department
		
		val roleDefinition = DepartmentalAdministratorRoleDefinition
		
		department.replacedRoleDefinitionFor(roleDefinition) should be ('empty)
		
		val customDefinition = new CustomRoleDefinition
		customDefinition.baseRoleDefinition = roleDefinition
		
		department.customRoleDefinitions.add(customDefinition)
		
		customDefinition.replacesBaseDefinition = false
		
		department.replacedRoleDefinitionFor(roleDefinition) should be ('empty)
		
		customDefinition.replacesBaseDefinition = true
		
		department.replacedRoleDefinitionFor(roleDefinition) should be (Some(customDefinition))
	}
	
	@Test
	def replacedRoleDefinitionForSelector {
		val department = new Department
		
		val selector = new StudentRelationshipType
		
		val customDefinition = new CustomRoleDefinition
		customDefinition.baseRoleDefinition = StudentRelationshipAgentRoleDefinition(selector)
		
		assert(StudentRelationshipAgentRoleDefinition(selector) === StudentRelationshipAgentRoleDefinition(selector))
		
		department.customRoleDefinitions.add(customDefinition)
		
		customDefinition.replacesBaseDefinition = true
		
		department.replacedRoleDefinitionFor(StudentRelationshipAgentRoleDefinition(selector)) should be (Some(customDefinition))
		department.replacedRoleDefinitionFor(StudentRelationshipAgentRoleDefinition(new StudentRelationshipType)) should be ('empty)
		department.replacedRoleDefinitionFor(StudentRelationshipAgentRoleDefinition(PermissionsSelector.Any)) should be ('empty)
		
		customDefinition.baseRoleDefinition = StudentRelationshipAgentRoleDefinition(PermissionsSelector.Any)
		
		department.replacedRoleDefinitionFor(StudentRelationshipAgentRoleDefinition(selector)) should be (Some(customDefinition))
		department.replacedRoleDefinitionFor(StudentRelationshipAgentRoleDefinition(new StudentRelationshipType)) should be (Some(customDefinition))
		department.replacedRoleDefinitionFor(StudentRelationshipAgentRoleDefinition(PermissionsSelector.Any)) should be (Some(customDefinition))
	}

}