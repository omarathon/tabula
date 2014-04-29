package uk.ac.warwick.tabula.web.views

import uk.ac.warwick.tabula.TestBase

class MarkdownRendererTest extends TestBase {

	val renderer = new MarkdownRendererImpl {}

	// Trying to reproduce ClassCast error.
	@Test def TAB_2169() {
		val input = """This message confirms that you made a submission for the assignment 'Assessment 2' for EC204 Economics 2.
									|
									|- Submission date: ${submissionDate}
									|- Submission ID: ${submission.id}
									|- University ID: ${user.warwickId}
									|
									|Uploaded attachments:
									|
									|- Whatever
									|- Who cares
								""".stripMargin
		val output = renderer.renderMarkdown(input)
		output should (include("<li>Whatever</li>") and include("<li>University ID: ${user.warwickId}</li>"))
	}

}
