package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.{Mockito, TestBase}
import uk.ac.warwick.tabula.data.model.forms._
import org.junit.Test
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity
import org.junit.Test
import uk.ac.warwick.tabula.services.AssignmentService

class FormFieldTest extends TestBase with Mockito {
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
	
	@Test def wordCountFieldRange {
		val wc = new WordCountField
		wc.min should be (null)
		wc.max should be (null)
		wc.conventions should be (null)
		
		wc.conventions = "Don't include words in Judaeo-Piedmontese."
		wc.min = 500
		wc.max = 5000
		
		wc.conventions should be ("Don't include words in Judaeo-Piedmontese.")
		wc.min should be (500)
		wc.max should be (5000)
	}

	@Test def maintainFieldOrder
	{
		val assignment = new Assignment
		assignment.assignmentService = mock[AssignmentService]
		assignment.addDefaultFields()

		val commentField = assignment.findField(Assignment.defaultCommentFieldName).get
		commentField.position should be (0)

		val uploadField = assignment.findField(Assignment.defaultUploadName).get
		uploadField.position should be (1)

		val wc = new WordCountField
		wc.name = Assignment.defaultWordCountName
		wc.conventions = "Don't include words in Judaeo-Piedmontese."
		wc.min = 500
		wc.max = 5000
		assignment.addField(wc)

		val wordCountField = assignment.findField(Assignment.defaultWordCountName).get
		wordCountField.position should be (2)

		val mf = new MarkerSelectField
		mf.name = Assignment.defaultMarkerSelectorName
		assignment.addField(mf)

		val markerField = assignment.findField(Assignment.defaultMarkerSelectorName).get
		markerField.position should be (3)

		assignment.removeField(wordCountField)
		commentField.position should be (0)
		uploadField.position should be (1)
		markerField.position should be (2)

		assignment.removeField(markerField)
		commentField.position should be (0)
		uploadField.position should be (1)

	}
}