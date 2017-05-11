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

		output.trim should be ("<p>&lt;a href=&quot;<a href=\"http://www2.warwick.ac.uk/fac/soc/economics/current/shared/assessment-feedback/ec226_test_2_16-17.pdf\">http://www2.warwick.ac.uk/fac/soc/economics/current/shared/assessment-feedback/ec226_test_2_16-17.pdf</a>&quot;&gt;View Generic Feedback&lt;/a&gt;</p>")
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
