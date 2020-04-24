package uk.ac.warwick.tabula.data.model.forms

import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.MockUserLookup
import uk.ac.warwick.tabula.commands.UploadedFile
import org.springframework.validation.BindException
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.{UserGroup, UnspecifiedTypeUserGroup, FileAttachment, Assignment}
import uk.ac.warwick.tabula.services.{UserGroupCacheManager, AssessmentService}

// scalastyle:off magic.number
class FormFieldTest extends TestBase with Mockito {

  val userLookup = new MockUserLookup

  @Test def commentField(): Unit = {
    val field = new CommentField
    field.value should be(null)
    field.value = "my comment\n\nwith newlines!"
    field.value should be("my comment\n\nwith newlines!")
    field.formattedHtml.getOutputFormat.getMarkupString(field.formattedHtml) should be("<p>my comment</p>\n<p>with newlines!</p>\n")
    field.propertiesMap should be(Map("value" -> "my comment\n\nwith newlines!"))
    field.template should be("comment")
  }

  @Test def textField(): Unit = {
    val field = new TextField
    field.value should be(null)
    field.value = "my comment"
    field.value should be("my comment")
    field.propertiesMap should be(Map("value" -> "my comment"))
    field.template should be("text")
  }

  @Test def textAreaField(): Unit = {
    val field = new TextareaField
    field.value should be(null)
    field.value = "my comment"
    field.value should be("my comment")
    field.propertiesMap should be(Map("value" -> "my comment"))
    field.template should be("textarea")
  }

  @Test def nullWordCount(): Unit = {
    val field = new WordCountField
    field.min = 3
    field.max = 10

    val number = new IntegerFormValue(field)
    number.value = null

    val errors = new BindException(number, "string")

    field.validate(number, errors)
    errors.getErrorCount should be(1)
    errors.getFieldError.getField should be("value")
    errors.getFieldError.getCodes should contain("assignment.submit.wordCount.missing")
  }

  @Test def nullWordCountMinAndMax(): Unit = {
    val field = new WordCountField

    val number = new IntegerFormValue(field)
    number.value = 372

    val errors = new BindException(number, "string")

    field.validate(number, errors)
    errors.hasErrors should be(false)
  }

  @Test def wordCountField(): Unit = {
    val field = new WordCountField
    field.max = 10
    field.min = 3
    field.conventions = "Between 3 and 10 words"

    field.propertiesMap should be(Map("max" -> 10, "min" -> 3, "conventions" -> "Between 3 and 10 words"))
    field.template should be("wordcount")

    val value = field.blankFormValue
    value.value = null

    var errors = new BindException(value, "value")
    field.validate(value, errors)

    errors.getErrorCount should be(1)
    errors.getFieldError.getField should be("value")
    errors.getFieldError.getCode should be("assignment.submit.wordCount.missing")

    value.value = 1
    errors = new BindException(value, "value")
    field.validate(value, errors)

    errors.getErrorCount should be(1)
    errors.getFieldError.getField should be("value")
    errors.getFieldError.getCode should be("assignment.submit.wordCount.outOfRange")

    value.value = 100
    errors = new BindException(value, "value")
    field.validate(value, errors)

    errors.getErrorCount should be(1)
    errors.getFieldError.getField should be("value")
    errors.getFieldError.getCode should be("assignment.submit.wordCount.outOfRange")

    value.value = 5
    errors = new BindException(value, "value")
    field.validate(value, errors)

    errors.hasErrors should be(false)
  }

  @Test def checkboxField(): Unit = {
    val field = new CheckboxField
    field.template should be("checkbox")
  }

  def wireUserLookup(userGroup: UnspecifiedTypeUserGroup): Unit = userGroup match {
    case cm: UserGroupCacheManager => wireUserLookup(cm.underlying)
    case ug: UserGroup => ug.userLookup = userLookup
  }

  @Test def fileField(): Unit = {
    val field = new FileField
    field.attachmentLimit = 2
    field.attachmentTypes = Seq("doc", "txt")

    val expectedTypes = Seq("doc", "txt")

    field.propertiesMap should be(Map("attachmentLimit" -> 2, "attachmentTypes" -> expectedTypes))
    field.template should be("file")

    val value = field.blankFormValue
    value.file = new UploadedFile
    value.file.attached = JArrayList()

    var errors = new BindException(value, "value")
    field.validate(value, errors)

    errors.getErrorCount should be(1)
    errors.getFieldError.getField should be("file")
    errors.getFieldError.getCode should be("file.missing")

    value.file.attached = JArrayList()

    var attachment = new FileAttachment
    attachment.name = "file.doc"

    // There are 3 of them, honest
    value.file.attached.add(attachment)
    value.file.attached.add(attachment)
    value.file.attached.add(attachment)

    errors = new BindException(value, "value")
    field.validate(value, errors)

    errors.getErrorCount should be(1)
    errors.getFieldError.getField should be("file")
    errors.getFieldError.getCode should be("file.toomany")

    // Ignore attachment limit if unlimited allowed
    value.file.attached = JArrayList()

    attachment = new FileAttachment
    attachment.name = "file.doc"
    var anotherAttachment = new FileAttachment
    anotherAttachment.name = "another-file.doc"
    var yetAnotherAttachment = new FileAttachment
    yetAnotherAttachment.name = "yet-another-file.doc"

    value.file.attached.add(attachment)
    value.file.attached.add(anotherAttachment)
    value.file.attached.add(yetAnotherAttachment)

    field.unlimitedAttachments = true
    field.propertiesMap should be(Map("attachmentLimit" -> 2, "attachmentTypes" -> expectedTypes, "unlimitedAttachments" -> true))
    errors = new BindException(value, "value")
    field.validate(value, errors)
    errors.getErrorCount should be(0)

    value.file.attached = JArrayList()

    attachment = new FileAttachment
    attachment.name = "file.doc"

    // duplicate files
    value.file.attached.add(attachment)
    value.file.attached.add(attachment)

    errors = new BindException(value, "value")
    field.validate(value, errors)

    errors.getErrorCount should be(1)
    errors.getFieldError.getField should be("file")
    errors.getFieldError.getCode should be("file.duplicate")

    value.file.attached = JArrayList()

    // wrong type
    attachment = new FileAttachment
    attachment.name = "file.exe"
    value.file.attached.add(attachment)

    errors = new BindException(value, "value")
    field.validate(value, errors)

    errors.getErrorCount should be(1)
    errors.getFieldError.getField should be("file")
    errors.getFieldError.getCode should be("file.wrongtype.one")

    value.file.attached = JArrayList()

    // wrong type
    attachment = new FileAttachment {
      override def length = Some(10000000L)
    }
    attachment.name = "file.doc"
    value.file.attached.add(attachment)

    errors = new BindException(value, "value")
    field.validate(value, errors)

    errors.hasErrors should be(false)

    field.individualFileSizeLimit = 5

    errors = new BindException(value, "value")
    field.validate(value, errors)
    errors.hasErrors should be(true)
    errors.getFieldError("file").getCode should be("file.toobig.one")
  }

  @Test def commentFieldFormatting(): Unit = {
    val comment = new CommentField

    comment.value = " Text.\nMore text.\n\n   <b>New</b> paragraph "
    comment.formattedHtml.getOutputFormat.getMarkupString(comment.formattedHtml) should be("<p>Text.\nMore text.</p>\n<p>&lt;b&gt;New&lt;/b&gt; paragraph</p>\n")
  }

  @Test def fileFieldCustomProperties(): Unit = {
    val file = new FileField
    file.attachmentLimit should be(1)
    file.attachmentTypes should be(Symbol("empty"))

    file.attachmentLimit = 5
    file.attachmentTypes = Seq("pdf", "doc")

    file.attachmentLimit should be(5)
    file.attachmentTypes should be(Seq("pdf", "doc"))

    // TAB-705
    file.json = json
    file.setProperties(file.getProperties())

    file.attachmentTypes should be(Seq("pdf", "doc"))
  }

  @Test def wordCountFieldRange(): Unit = {
    val wc = new WordCountField
    wc.min should be(null)
    wc.max should be(null)
    wc.conventions should be(null)

    wc.conventions = "Don't include words in Judaeo-Piedmontese."
    wc.min = 500
    wc.max = 5000

    wc.conventions should be("Don't include words in Judaeo-Piedmontese.")
    wc.min should be(500)
    wc.max should be(5000)
  }

  @Test def maintainFieldOrder(): Unit = {
    val assignment = new Assignment
    assignment.assignmentService = mock[AssessmentService]
    assignment.addDefaultSubmissionFields()

    val commentField = assignment.findField(Assignment.defaultCommentFieldName).get
    commentField.position should be(0)

    val uploadField = assignment.findField(Assignment.defaultUploadName).get
    uploadField.position should be(1)

    val wc = new WordCountField
    wc.name = Assignment.defaultWordCountName
    wc.conventions = "Don't include words in Judaeo-Piedmontese."
    wc.min = 500
    wc.max = 5000
    assignment.addField(wc)

    val wordCountField = assignment.findField(Assignment.defaultWordCountName).get
    wordCountField.position should be(2)

    assignment.removeField(wordCountField)
    commentField.position should be(0)
    uploadField.position should be(1)

    commentField.position should be(0)
    uploadField.position should be(1)

    assignment.addDefaultFeedbackFields()
    commentField.position should be(0)
    uploadField.position should be(1)
    val feedbackComment = assignment.findField(Assignment.defaultFeedbackTextFieldName)
    feedbackComment should not be None
    feedbackComment.get.position should be(0)
  }

}
