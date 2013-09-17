package uk.ac.warwick.tabula.profiles

import uk.ac.warwick.tabula.profiles.pages.SubDepartmentFixture

class SubDepartmentPermissionsTest  extends SubDepartmentFixture{

	/**
	 * These tests all assume that:
	 *
	 *  - you need to have explicit permissions (Permissions.Profiles.Read.StudentCourseDetails.Core) for the course-pane content
	 *  to be displayed when you view another user's profile
	 *
	 *  - the DepartmentAdmin role grants the holder that permission on any student who touches the department
	 *
	 */

		before{
			beforeEverything()
		}

	"A departmental administrator" should "have admin rights on students in their department" in {

		Given("Student1 is an undergraduate, registered on a module in xxx")

		createModule("xxx","xxx198","Top level module")
		registerStudentsOnModule(Seq(P.Student1),"xxx198")

		Then("Admin1 can view Student1's course details")
		signIn as (P.Admin1) to (Path(s"/profiles/view/${P.Student1.warwickId}"))
		find(cssSelector("li#course-pane")) should be ('defined)
	}

	"A departmental administrator" should "have admin rights on students on modules in their sub-departments" in {
		Given("Student2 is registered on a module in xxx-ug")
		createModule("xxx-ug","xug19","Top level module")
		registerStudentsOnModule(Seq(P.Student2),"xug19")

		Then("Admin1 can view Student2's profile")
		signIn as (P.Admin1) to (Path(s"/profiles/view/${P.Student2.warwickId}"))
		find(cssSelector("li#course-pane")) should be ('defined)
	}


	"A sub-departmental administrator" should "not have admin rights on students in their parent department" in {
		Given("Student3 is a postgraduate, registered on a module in xxx")
		createModule("xxx","xpg12","Postgrad module")
		registerStudentsOnModule(Seq(P.Student3),"xpg12")

		Then("Admin3 can't view Student3's profile")
		signIn as (P.Admin3) to (Path(s"/profiles/view/${P.Student3.warwickId}"))
		find(cssSelector("li#course-pane")) should not be ('defined)
	}

	"A sub-departmental administrator" should "have admin rights on students who match their sub-departments" in {
		Given("Student1 is an undergraduate, registered on a module in xxx")

		createModule("xxx","xxx198","Top level module")
		registerStudentsOnModule(Seq(P.Student1),"xxx198")

		Then("Admin3 can view Student1's profile")
		signIn as (P.Admin3) to (Path(s"/profiles/view/${P.Student1.warwickId}"))
		find(cssSelector("li#course-pane")) should be ('defined)
	}

	"A sub-departmental administrator" should "have admin rights on students on modules in their sub-department" in {

		Given("Student2 is registered on a module in xxx-ug")
		createModule("xxx-ug","xug19","Top level module")
		registerStudentsOnModule(Seq(P.Student2),"xug19")

		Then("Admin3 can view Student2's profile")
		signIn as (P.Admin3) to (Path(s"/profiles/view/${P.Student2.warwickId}"))
		find(cssSelector("li#course-pane")) should be ('defined)
	}

	"A sub-sub-departmental administrator" should "have admin rights on students who match their sub-sub-department" in{

		Given("Student1 is a first-year undergraduate, registered on a module in xxx")
		createModule("xxx","xxx198","Top level module")
		registerStudentsOnModule(Seq(P.Student1),"xxx198")

		And("Student2 is a second-year undergraduate, registered on a module in xxx-ug")
		createModule("xxx-ug","xug19","Top level module")
		registerStudentsOnModule(Seq(P.Student2),"xug19")

		Then("Admin4 can view Student1's profile")
		signIn as (P.Admin4) to (Path(s"/profiles/view/${P.Student1.warwickId}"))
		find(cssSelector("li#course-pane")) should be ('defined)

		And("Admin4 cannot view student2's profile")

		go to(Path(s"/profiles/view/${P.Student2.warwickId}"))
		find(cssSelector("li#course-pane")) should not be ('defined)
	}

}
