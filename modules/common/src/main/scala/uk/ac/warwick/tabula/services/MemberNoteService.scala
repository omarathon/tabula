package uk.ac.warwick.tabula.services

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.{AutowiringMemberNoteDaoComponent, MemberNoteDaoComponent}
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.{Member, MemberNote}

trait MemberNoteServiceComponent {
	def memberNoteService: MemberNoteService
}

trait AutowiringMemberNoteServiceComponent extends MemberNoteServiceComponent {
	var memberNoteService = Wire[MemberNoteService]
}

trait MemberNoteService {

	def getNoteById(id: String): Option[MemberNote]
	def list(student: Member): Seq[MemberNote]
	def listNonDeleted(student: Member): Seq[MemberNote]
	def saveOrUpdate(memberNote: MemberNote)
	def deleteNote(note: MemberNote)

}


abstract class AbstractMemberNoteService extends MemberNoteService {
	self: MemberNoteDaoComponent =>

	def getNoteById(id: String): Option[MemberNote] = memberNoteDao.getById(id)
	def list(student: Member): Seq[MemberNote] = memberNoteDao.list(student, true)
	def listNonDeleted(student: Member): Seq[MemberNote] = memberNoteDao.list(student, false)
	def saveOrUpdate(memberNote: MemberNote) = memberNoteDao.saveOrUpdate(memberNote)
	def deleteNote(memberNote: MemberNote) = memberNoteDao.delete(memberNote)

}

@Service("memberNoteService")
class MemberNoteServiceImpl
	extends AbstractMemberNoteService
	with AutowiringMemberNoteDaoComponent