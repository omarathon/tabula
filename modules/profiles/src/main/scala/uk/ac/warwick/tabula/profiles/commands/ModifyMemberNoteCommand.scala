package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.data.model.{FileAttachment, MemberNote, Member}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{UploadedFile, SelfValidating, Command}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.data.Transactions._
import scala.collection.JavaConversions._
import org.joda.time.DateTime
import org.springframework.validation.{Errors, BindingResult}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.{MemberNoteService, ProfileService}
import uk.ac.warwick.tabula.helpers.StringUtils._

abstract class ModifyMemberNoteCommand(val member: Member, val submitter: CurrentUser) extends Command[MemberNote] with BindListener with SelfValidating   {

	var profileService = Wire[ProfileService]
	var memberNoteService = Wire[MemberNoteService]

	var note: String = member.fullName.getOrElse(null)
	var title: String = _
	var creationDate = DateTime.now
	var lastUpdatedDate = DateTime.now

	var file: UploadedFile = new UploadedFile
	var attachedFiles:JList[FileAttachment] = JArrayList()

	var creator: Member = _
	var attachmentTypes = Seq[String]()

	val memberNote: MemberNote

	def applyInternal(): MemberNote = transactional() {

		creator = profileService.getMemberByUniversityId(submitter.universityId).getOrElse(null)

		this.copyTo(memberNote)

		if (!file.attached.isEmpty) {
			for (attachment <- file.attached) {
				memberNote.addAttachment(attachment)
			}
		}

		memberNoteService.saveOrUpdate(memberNote)

		memberNote
	}

	def validate(errors:Errors){
		if (!note.hasText && !file.hasAttachments){
			errors.rejectValue("note", "profiles.memberNote.empty")
		}
	}

	def onBind(result: BindingResult) {
		file.onBind(result)
	}

	def copyFrom(memberNote: MemberNote) {
		this.note = memberNote.note
		this.title = memberNote.title
		this.creationDate = memberNote.creationDate
		this.creator = memberNote.creator
		this.attachedFiles = memberNote.attachments

	}

	def copyTo(memberNote: MemberNote) {
		memberNote.note = this.note
		memberNote.title = this.title
		memberNote.creationDate = this.creationDate
		memberNote.creator = this.creator
		memberNote.member = this.member
		memberNote.attachments = this.attachedFiles
		memberNote.lastUpdatedDate = new DateTime()

	}

}
