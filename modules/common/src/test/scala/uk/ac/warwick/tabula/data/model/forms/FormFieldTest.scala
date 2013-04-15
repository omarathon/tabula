package uk.ac.warwick.tabula.data.model.forms

import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.MockUserLookup
import uk.ac.warwick.tabula.commands.UploadedFile
import org.springframework.validation.BindException
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.FileAttachment

class FormFieldTest extends TestBase with Mockito {
	
	val userLookup = new MockUserLookup
	
	@Test def commentField {
		val field = new CommentField
		field.value should be (null)
		field.value = "my comment\n\nwith newlines!"
		field.value should be ("my comment\n\nwith newlines!")
		field.formattedHtml should be ("<p>my comment</p><p>with newlines!</p>")
		field.propertiesMap should be (Map("value" -> "my comment\n\nwith newlines!"))
		field.template should be ("comment")
	}
	
	@Test def textField {
		val field = new TextField
		field.value should be (null)
		field.value = "my comment"
		field.value should be ("my comment")
		field.propertiesMap should be (Map("value" -> "my comment"))
		field.template should be ("text")
	}
	
	@Test def textAreaField {
		val field = new TextareaField
		field.value should be (null)
		field.value = "my comment"
		field.value should be ("my comment")
		field.propertiesMap should be (Map("value" -> "my comment"))
		field.template should be ("textarea")
	}
	
	@Test def wordCountField {
		val field = new WordCountField
		field.max = 10
		field.min = 3
		field.conventions = "Between 3 and 10 words"
			
		field.propertiesMap should be (Map("max" -> 10, "min" -> 3, "conventions" -> "Between 3 and 10 words"))
		field.template should be ("wordcount")
			
		val value = field.blankSubmissionValue
		value.value = "not a word count"
		
		var errors = new BindException(value, "value")
		field.validate(value, errors)
		
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("value")
		errors.getFieldError.getCode should be ("assignment.submit.wordCount.missing")
		
		value.value = "1"
		errors = new BindException(value, "value")
		field.validate(value, errors)
		
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("value")
		errors.getFieldError.getCode should be ("assignment.submit.wordCount.outOfRange")
		
		value.value = "100"
		errors = new BindException(value, "value")
		field.validate(value, errors)
		
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("value")
		errors.getFieldError.getCode should be ("assignment.submit.wordCount.outOfRange")
		
		value.value = "5"
		errors = new BindException(value, "value")
		field.validate(value, errors)
		
		errors.hasErrors should be (false)
	}
	
	@Test def checkboxField {
		val field = new CheckboxField
		field.template should be ("checkbox")
	}
	
	@Test def markerSelectField {
		val assignment = Fixtures.assignment("my assignment")
		val workflow = Fixtures.markingWorkflow("my workflow")
		workflow.firstMarkers.addUser("cuscav")
		workflow.firstMarkers.addUser("cusebr")
		
		val user1 = new User("cuscav")
		val user2 = new User("cusebr")
		
		userLookup.users ++= Map(
				"cuscav" -> user1,
				"cusebr" -> user2
		)
		
		val field = new MarkerSelectField
		field.userLookup = userLookup
		
		field.assignment = assignment
		field.markers should be ('empty)
		
		assignment.markingWorkflow = workflow
		field.markers should be (Seq(user1, user2))
		
		field.value should be (null)
		field.value = "my comment"
		field.value should be ("my comment")
		field.propertiesMap should be (Map("value" -> "my comment"))
		field.template should be ("marker")
		
		val value = field.blankSubmissionValue
		
		var errors = new BindException(value, "value")
		field.validate(value, errors)
		
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("value")
		errors.getFieldError.getCode should be ("marker.missing")
		
		value.value = "steve"
		errors = new BindException(value, "value")
		field.validate(value, errors)
		
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("value")
		errors.getFieldError.getCode should be ("marker.invalid")
		
		value.value = "cuscav"
		errors = new BindException(value, "value")
		field.validate(value, errors)
		
		errors.hasErrors should be (false)
	}
	
	@Test def fileField {
		val field = new FileField
		field.attachmentLimit = 2
		field.attachmentTypes = Seq("doc", "txt")
		
		val expectedTypes: JList[String] = JArrayList()
		expectedTypes.add("doc")
		expectedTypes.add("txt")
			
		field.propertiesMap should be (Map("attachmentLimit" -> 2, "attachmentTypes" -> expectedTypes))
		field.template should be ("file")
			
		val value = field.blankSubmissionValue
		value.file = new UploadedFile
		value.file.attached = JArrayList()
		
		var errors = new BindException(value, "value")
		field.validate(value, errors)
		
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("file")
		errors.getFieldError.getCode should be ("file.missing")
		
		value.file.attached = JArrayList()
		
		var attachment = new FileAttachment
		attachment.name = "file.doc"
			
		// There are 3 of them, honest
		value.file.attached.add(attachment)
		value.file.attached.add(attachment)
		value.file.attached.add(attachment)
		
		errors = new BindException(value, "value")
		field.validate(value, errors)
		
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("file")
		errors.getFieldError.getCode should be ("file.toomany")
		
		value.file.attached = JArrayList()
		
		attachment = new FileAttachment
		attachment.name = "file.doc"
			
		// duplicate files
		value.file.attached.add(attachment)
		value.file.attached.add(attachment)
		
		errors = new BindException(value, "value")
		field.validate(value, errors)
		
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("file")
		errors.getFieldError.getCode should be ("file.duplicate")
		
		value.file.attached = JArrayList()
		
		// wrong type
		attachment = new FileAttachment
		attachment.name = "file.exe"
		value.file.attached.add(attachment)
		
		errors = new BindException(value, "value")
		field.validate(value, errors)
		
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("file")
		errors.getFieldError.getCode should be ("file.wrongtype.one")
		
		value.file.attached = JArrayList()
		
		// wrong type
		attachment = new FileAttachment
		attachment.name = "file.doc"
		value.file.attached.add(attachment)
		
		errors = new BindException(value, "value")
		field.validate(value, errors)
		
		errors.hasErrors should be (false)
	}

}