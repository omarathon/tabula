package uk.ac.warwick.tabula.data.model.forms

import uk.ac.warwick.tabula.TestBase

class FormattedHtmlTest extends TestBase {

	val formatter = new FormattedHtml {}

	@Test
	def normal(): Unit = {
		val input =
			"""
				|Here is your feedback.
				|
				|I hope you like it
			""".stripMargin
		val output = formatter.formattedHtml(input)

		output.trim should be ("<p>Here is your feedback.</p>\n<p>I hope you like it</p>")
	}

	@Test
	def singleLine(): Unit = {
		val input = "Here is your feedback."
		val output = formatter.formattedHtml(input)

		output.trim should be ("<p>Here is your feedback.</p>")
	}

	@Test
	def htmlEscaped(): Unit = {
		val input = """<a href="http://www2.warwick.ac.uk/fac/soc/economics/current/shared/assessment-feedback/ec226_test_2_16-17.pdf">View Generic Feedback</a>"""
		val output = formatter.formattedHtml(input)

		// TAB-6685 changed &quot; to &#34;
		// they are the same thing, and &quot; is preferred, but OWASP sanitiser preferred the latter, so i have to chenge this test case
		// this does not technically affect the purpose of this test tho
		output.trim should be ("<p>&lt;a href&#61;&#34;<a href=\"http://www2.warwick.ac.uk/fac/soc/economics/current/shared/assessment-feedback/ec226_test_2_16-17.pdf\">http://www2.warwick.ac.uk/fac/soc/economics/current/shared/assessment-feedback/ec226_test_2_16-17.pdf</a>&#34;&gt;View Generic Feedback&lt;/a&gt;</p>")
	}

	@Test
	def markdown(): Unit = {
		val input =
			"""
				|Here is your feedback.
				|
				|I hope you like it.
				|
				|[View Generic Feedback](http://www2.warwick.ac.uk/fac/soc/economics/current/shared/assessment-feedback/ec226_test_2_16-17.pdf)
			""".stripMargin
		val output = formatter.formattedHtml(input)

		output.trim should be ("<p>Here is your feedback.</p>\n<p>I hope you like it.</p>\n<p><a href=\"http://www2.warwick.ac.uk/fac/soc/economics/current/shared/assessment-feedback/ec226_test_2_16-17.pdf\">View Generic Feedback</a></p>")
	}

}
