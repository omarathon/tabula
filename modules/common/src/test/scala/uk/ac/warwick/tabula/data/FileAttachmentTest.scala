package uk.ac.warwick.tabula.data
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.FileAttachment

class FileAttachmentTest extends TestBase {

	/** 
	 * HFC-136 
	 * Filenames can be prefixed with whitespace, preventing download
	 */
	@Test
	def untrimmedFilenames {
		new FileAttachment(" 0123456.docx").name should be ("0123456.docx")
		new FileAttachment(null).name should be (null)
	}

	@Test
	def badWindowsFilenames {
		new FileAttachment(""" \Fun: good or bad? <You|decide>/.docx""").name should be ("Fun good or bad Youdecide.docx")
	}

	@Test
	def removeSemicolons() {
		new FileAttachment("We've had our fun; now let's go home.xlsx").name should be ("We've had our fun now let's go home.xlsx")
	}

}