package uk.ac.warwick.tabula.api.web.helpers

import uk.ac.warwick.tabula.TestBase

class APIFieldRestrictionTest extends TestBase {

	@Test
	def parse(): Unit = {
		val input = "member.fullName.lazy.yes,member.touchedDepartments,member.fullName.steve,member.fullName.lazy.no,steven"

		val memberRestriction = APIFieldRestriction("member", Map(
			"fullName" -> APIFieldRestriction("fullName", Map(
				"lazy" -> APIFieldRestriction("lazy", Map(
					"yes" -> APIFieldRestriction("yes"),
					"no" -> APIFieldRestriction("no")
				)),
				"steve" -> APIFieldRestriction("steve")
			)),
			"touchedDepartments" -> APIFieldRestriction("touchedDepartments"),
		))

		val expected = Map(
			"member" -> memberRestriction,
			"steven" -> APIFieldRestriction("steven", Map())
		)

		APIFieldRestriction.parse(input) should be (expected)
		APIFieldRestriction.parse("") should be ('empty)

		APIFieldRestriction.restriction("member", input) should be (memberRestriction)
		APIFieldRestriction.restriction("member", "") should be (APIFieldRestriction("member"))
		APIFieldRestriction.restriction("studentCourseDetails", input) should be (APIFieldRestriction("studentCourseDetails", expected))
	}

	@Test
	def isAllowed(): Unit = {
		val input = "member.fullName.lazy.yes,member.touchedDepartments,member.fullName.steve,member.fullName.lazy.no,steven"
		val restriction = APIFieldRestriction.restriction("member", input)
		
		val tests = Seq(
			"fullName" -> true,
			"fullname" -> true,
			"fullName.lazy.yes" -> true,
			"fullName.lazy.what" -> false,
			"fullName.lazy" -> true,
			"fullName.steve" -> true,
			"fullName.steve.YES.STEVE" -> true,
			"simon" -> false,
			"touchedDepartments" -> true,
			"touchedDepartments.code" -> true,
			"touchedDepartments.modules.code" -> true,
		)

		tests.foreach { case (k, expected) => restriction.isAllowed(k) should be (expected) }

		// Test that the behaviour of nested is analogous to isAllowed, if it's allowed it should return a wildcard nested restriction
		tests.foreach {
			case (k, true) => restriction.nested(k) should not be ('empty)
			case (k, false) => restriction.nested(k) should be ('empty)
		}
	}

}
