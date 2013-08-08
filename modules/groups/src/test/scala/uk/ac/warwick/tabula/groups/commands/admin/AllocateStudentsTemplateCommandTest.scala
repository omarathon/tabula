package uk.ac.warwick.tabula.groups.commands.admin

import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.services.SmallGroupService
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.MockUserLookup
import uk.ac.warwick.tabula.services.AssignmentMembershipService
import uk.ac.warwick.userlookup.User
import org.junit.Before
import org.apache.poi.xssf.usermodel.XSSFSheet

class AllocateStudentsTemplateCommandTest extends TestBase with Mockito {

	val service = mock[SmallGroupService]
	val membershipService = mock[AssignmentMembershipService]
	val userLookup = new MockUserLookup

	val module = Fixtures.module("in101", "Introduction to Scala")
	val set = Fixtures.smallGroupSet("My small groups")

	@Before def itWorks = withUser("boombastic") {

		set.module = module
		set.membershipService = membershipService
		set.members.userLookup = userLookup

		val user1 = new User("cuscav")
		user1.setFoundUser(true)
		user1.setFirstName("Mathew")
		user1.setLastName("Mannion")
		user1.setWarwickId("0672089")

		val user2 = new User("cusebr")
		user2.setFoundUser(true)
		user2.setFirstName("Nick")
		user2.setLastName("Howes")
		user2.setWarwickId("0672088")

		val user3 = new User("cusfal")
		user3.setFoundUser(true)
		user3.setFirstName("Matthew")
		user3.setLastName("Jones")
		user3.setWarwickId("9293883")

		val user4 = new User("curef")
		user4.setFoundUser(true)
		user4.setFirstName("John")
		user4.setLastName("Dale")
		user4.setWarwickId("0200202")

		val user5 = new User("cusmab")
		user5.setFoundUser(true)
		user5.setFirstName("Steven")
		user5.setLastName("Carpenter")
		user5.setWarwickId("8888888")

		userLookup.users += (
			user1.getUserId -> user1,
			user2.getUserId -> user2,
			user3.getUserId -> user3,
			user4.getUserId -> user4,
			user5.getUserId -> user5
			)

		val group1 = Fixtures.smallGroup("Group 1")
		val group2 = Fixtures.smallGroup("Group 2")
		val group3 = Fixtures.smallGroup("Group 3")
		val group4 = Fixtures.smallGroup("Group 4")

		group1.name = "Group 1"
		group1.id = "abcdefgh1"
		group2.name = "Group 2"
		group2.id = "abcdefgh2"
		group3.name = "Group 3"
		group3.id = "abcdefgh3"
		group4.name = "Group 4"
		group4.id = "abcdefgh4"

		set.groups.add(group1)
		set.groups.add(group2)
		set.groups.add(group3)
		set.groups.add(group4)
		group1.groupSet = set
		group2.groupSet = set
		group3.groupSet = set
		group4.groupSet = set
		group1._studentsGroup.userLookup = userLookup
		group2._studentsGroup.userLookup = userLookup
		group3._studentsGroup.userLookup = userLookup
		group4._studentsGroup.userLookup = userLookup

		set.members.addUser(user1.getWarwickId)
		set.members.addUser(user2.getWarwickId)
		set.members.addUser(user3.getWarwickId)
		set.members.addUser(user4.getWarwickId)
		set.members.addUser(user5.getWarwickId)

	}


	@Test def allocateUsersSheet = withUser("slengteng") {

		implicit class SearchableSheet(self:XSSFSheet) {
			def containsDataRow(id:String, name:String, maxRows:Int = self.getLastRowNum):Boolean = {
				val rows = for (i<- 1 to maxRows) yield self.getRow(i)
				val matchingRow = rows.find(r=>r.getCell(0).toString == id && r.getCell(1).toString == name)
				matchingRow.isDefined
			}
		}

		val cmd = new AllocateStudentsTemplateCommand(module, set, currentUser)
		val workbook = cmd.generateWorkbook()

		val allocateSheet = workbook.getSheet(cmd.allocateSheetName)

		val headerRow = allocateSheet.getRow(0)
		headerRow.getCell(0).toString should be ("student_id")
		headerRow.getCell(1).toString should be ("Student name")
		headerRow.getCell(2).toString should be ("Group name")
		headerRow.getCell(3).toString should be ("group_id")

		allocateSheet.containsDataRow("0672089","Mathew Mannion", maxRows = 6) should be(true)
		allocateSheet.containsDataRow("0672088","Nick Howes", maxRows = 6) should be(true)
		allocateSheet.containsDataRow("8888888","Steven Carpenter", maxRows = 6) should be(true)
		allocateSheet.containsDataRow("9293883","Matthew Jones", maxRows = 6) should be(true)
		allocateSheet.containsDataRow("0200202","John Dale", maxRows = 6) should be(true)
		allocateSheet.containsDataRow("","", maxRows = 6) should be(true)

		val blankRow = allocateSheet.getRow(6)
		blankRow.getCell(0).toString should be ("")
		blankRow.getCell(1).toString should be ("")

	}

	@Test def groupLookupSheet() = withUser("satta") {

		val cmd = new AllocateStudentsTemplateCommand(module, set, currentUser)
		val workbook = cmd.generateWorkbook()

		val groupLookupSheet = workbook.getSheet(cmd.groupLookupSheetName)

		var groupRow = groupLookupSheet.getRow(1)
		groupRow.getCell(0).toString should be ("Group 1")
		groupRow.getCell(1).toString should be ("abcdefgh1")

		groupRow = groupLookupSheet.getRow(2)
		groupRow.getCell(0).toString should be ("Group 2")
		groupRow.getCell(1).toString should be ("abcdefgh2")

		groupRow = groupLookupSheet.getRow(3)
		groupRow.getCell(0).toString should be ("Group 3")
		groupRow.getCell(1).toString should be ("abcdefgh3")

		groupRow = groupLookupSheet.getRow(4)
		groupRow.getCell(0).toString should be ("Group 4")
		groupRow.getCell(1).toString should be ("abcdefgh4")
	}


	@Test def checkExcelView() = withUser("s90") {
		val cmd = new AllocateStudentsTemplateCommand(module, set, currentUser)
		val excelDownload = cmd.applyInternal

		excelDownload.getContentType() should be ("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
	}

}