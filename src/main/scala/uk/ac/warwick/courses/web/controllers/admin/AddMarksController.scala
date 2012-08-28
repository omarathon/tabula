package uk.ac.warwick.courses.web.controllers.admin

import scala.collection.JavaConversions._
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import uk.ac.warwick.courses.web.controllers.BaseController
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.courses.commands.assignments.AddMarksCommand
import uk.ac.warwick.courses.CurrentUser
import uk.ac.warwick.courses.commands.assignments.AddFeedbackCommand
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.courses.web.Mav
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.Module
import uk.ac.warwick.courses.actions.Participate
import org.springframework.validation.Errors
import uk.ac.warwick.courses.web.Routes
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.courses.services.AssignmentService
import uk.ac.warwick.courses.services.docconversion.MarkItem
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.courses.data.model.Feedback

@Controller
@RequestMapping(value=Array("/admin/module/{module}/assignments/{assignment}/marks"))
class AddMarksController extends BaseController{
	
    @Autowired var assignmentService:AssignmentService = _
    
    @ModelAttribute def command(@PathVariable assignment:Assignment, user:CurrentUser) = new AddMarksCommand(assignment, user)
	
	// Add the common breadcrumbs to the model.
	def crumbed(mav:Mav, module:Module) = mav.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	
	@RequestMapping(method=Array(HEAD,GET))
	def uploadZipForm(@PathVariable module:Module, @PathVariable(value="assignment") assignment:Assignment, @ModelAttribute cmd:AddMarksCommand):Mav = {
		mustBeLinked(assignment,module)
		mustBeAbleTo(Participate(module))
		val members = assignmentService.determineMembershipUsers(assignment)
		
        var marksToDisplay = List[MarkItem]()
        
        logger.debug("sizeof marksToDisplay is " + marksToDisplay.size)
        
        members.foreach ( member => {
            val feedback = assignmentService.getStudentFeedback( assignment, member.getWarwickId()) 
            feedback.foreach ( marksToDisplay ::= noteMarkItem(member, _))          
        });
        
        crumbed(Mav("admin/assignments/marks/marksform", "marksToDisplay" -> marksToDisplay), module)
		
	}
    
  def noteMarkItem(member: User, feedback: Feedback) = {

    logger.debug("in noteMarkItem (logger.debug)");

    val markItem = new MarkItem()
    markItem.universityId = member.getWarwickId()
    markItem.actualMark = feedback.actualMark.map{ _.toString() }.getOrElse("")
    markItem.actualGrade = feedback.actualGrade

    markItem
  } 
    
	@RequestMapping(method=Array(POST), params=Array("!confirm"))
	def confirmBatchUpload(@PathVariable module:Module, @PathVariable assignment:Assignment, @ModelAttribute cmd:AddMarksCommand, errors: Errors):Mav = {
		cmd.onBind
		cmd.postExtractValidation(errors)
		mustBeLinked(assignment,module)
		mustBeAbleTo(Participate(module))
		crumbed(Mav("admin/assignments/marks/markspreview"), module)
	}
	
	@RequestMapping(method=Array(POST), params=Array("confirm=true"))
	def doUpload(@PathVariable module:Module, @PathVariable assignment:Assignment, @ModelAttribute cmd:AddMarksCommand, errors: Errors):Mav = {
		mustBeLinked(assignment,module)
		mustBeAbleTo(Participate(module))
		cmd.onBind
		cmd.apply()
		Redirect(Routes.admin.module(module))
	}

}