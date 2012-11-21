package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.data.model.forms._
import org.junit.Test
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity
import org.junit.Test

class FormFieldTest extends TestBase {
	@Test def commentFieldFormatting {
		val comment = new CommentField
		
		comment.value = " Text.\nMore text.\n\n   <b>New</b> paragraph "
		comment.formattedHtml should be ("<p> Text.\nMore text.</p><p>&lt;b&gt;New&lt;/b&gt; paragraph </p>")
	}
	
	@Test def fileFieldCustomProperties {
		val file = new FileField
		file.attachmentLimit should be (1)
		file.attachmentTypes should be ('empty)
		
		file.attachmentLimit = 5
		file.attachmentTypes = Seq("pdf","doc")
		
		file.attachmentLimit should be (5)
		file.attachmentTypes should be (Seq("pdf","doc"))
	}
}