package uk.ac.warwick.tabula.profiles.commands

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.{MemberNote, Member}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.commands.{SelfValidating, UploadedFile, Description, Command}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.helpers.StringUtils._
import org.joda.time.DateTime
import org.springframework.validation.{Errors, BindingResult}

class CreateMemberNoteCommand(val member: Member, val submitter: CurrentUser) extends Command[MemberNote] with BindListener with Daoisms with SelfValidating {

	var profileService = Wire[ProfileService]

	PermissionCheck(Permissions.MemberNotes.Create, member)

	var note: String = member.fullName.getOrElse(null)
	var title: String = _
	var file: UploadedFile = new UploadedFile
	var attachmentTypes = Seq[String]()

	/**
	Subclasses do their work in here.

		Classes using a command should NOT call this method! call apply().
		The method here is protected but subclasses can easily override it
		to be publicly visible, so there's little to stop you from calling it.
		TODO somehow stop this being callable
		*/
	protected def applyInternal(): MemberNote = transactional() {
		   val memberNote = new MemberNote
		   memberNote.note = note
		   memberNote.title = title
		   memberNote.creationDate = DateTime.now
		   memberNote.lastUpdatedDate = memberNote.creationDate
		   memberNote.creator = profileService.getMemberByUniversityId(submitter.universityId).getOrElse(null)
		   memberNote.member = member
			if (!file.attached.isEmpty) {
				for (attachment <- file.attached) {
					memberNote.addAttachment(attachment)
				}
			}
			 session.saveOrUpdate(memberNote)

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

	// describe the thing that's happening.
	def describe(d: Description) {
		d.member(member)
	}

}
