package uk.ac.warwick.tabula.data.model

import org.hibernate.annotations._
import javax.persistence._
import javax.persistence.CascadeType._
import javax.persistence.FetchType._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports._
import org.hibernate.annotations.AccessType
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.data.model.forms.FormattedHtml
import javax.persistence.Entity


@Entity @AccessType("field")
class MemberNote extends GeneratedId with CanBeDeleted with PermissionsTarget with FormattedHtml {

	@ManyToOne
	@JoinColumn(name="memberid")
	var member: Member =_

	var note: String =_

	def escapedNote: String = formattedHtml(note)

	var title: String =_

	@OneToMany(mappedBy="memberNote", fetch=LAZY, cascade=Array(ALL))
	@BatchSize(size=200)
	var attachments: JList[FileAttachment] = JArrayList()

	@ManyToOne
	@JoinColumn(name="creatorid")
	var creator: Member =_

	@Type(`type`="org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	var creationDate: DateTime = DateTime.now

	@Type(`type`="org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	var lastUpdatedDate: DateTime = creationDate

	def addAttachment(attachment:FileAttachment) {
		if (attachment.isAttached) throw new IllegalArgumentException("File already attached to another object")
		attachment.temporary = false
		attachment.memberNote = this
		attachments.add(attachment)
	}

	def permissionsParents = Stream(member)

	override def toString = "MemberNote(" + id + ")"
}
