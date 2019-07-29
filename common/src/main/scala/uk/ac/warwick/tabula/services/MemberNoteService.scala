package uk.ac.warwick.tabula.services

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.{AutowiringMemberNoteDaoComponent, MemberNoteDaoComponent}
import org.springframework.stereotype.Service
import uk.ac.warwick.tabula.data.model.{Member, MemberNote}

trait MemberNoteServiceComponent {
  def memberNoteService: MemberNoteService
}

trait AutowiringMemberNoteServiceComponent extends MemberNoteServiceComponent {
  var memberNoteService: MemberNoteService = Wire[MemberNoteService]
}

trait MemberNoteService {

  def getNoteById(id: String): Option[MemberNote]

  def listNotes(student: Member): Seq[MemberNote]

  def listNonDeletedNotes(student: Member): Seq[MemberNote]

  def saveOrUpdate(memberNote: MemberNote)

  def delete(note: MemberNote)

}


abstract class AbstractMemberNoteService extends MemberNoteService {
  self: MemberNoteDaoComponent =>

  def getNoteById(id: String): Option[MemberNote] =
    memberNoteDao.getNoteById(id)

  def listNotes(student: Member): Seq[MemberNote] =
    memberNoteDao.listNotes(student, includeDeleted = true)

  def listNonDeletedNotes(student: Member): Seq[MemberNote] =
    memberNoteDao.listNotes(student, includeDeleted = false)

  def saveOrUpdate(memberNote: MemberNote): Unit =
    memberNoteDao.saveOrUpdate(memberNote)

  def delete(memberNote: MemberNote): Unit =
    memberNoteDao.delete(memberNote)

}

@Service("memberNoteService")
class MemberNoteServiceImpl
  extends AbstractMemberNoteService
    with AutowiringMemberNoteDaoComponent