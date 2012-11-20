<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>


<@f.form method="post" action="/admin/module/${module.code}/assignments/${assignment.id}/feedback/delete" commandName="deleteSubmissionsAndFeedbackCommand">

<h1>Delete submissions and/or feedback for ${assignment.name}</h1>

<@form.errors path="" />

<input type="hidden" name="confirmScreen" value="true" />

<@spring.bind path="students">
<@form.errors path="students" />
<#assign studentIds=status.actualValue />
<p>Deleting submissions and feedbacks for <strong><@fmt.p studentIds?size "students" /></strong>:
</p>
<p>
<ul>
<#list studentIds as studentId>
<li>${studentId}</li>
</#list>
</ul>
<br />
<p>
Please specify what you would like to delete:
</p>
<p><input type="radio" name="submissionOrFeedback" value="submissionOnly"> Submission only</input>
<br /><input type="radio" name="submissionOrFeedback" value="feedbackOnly"> Feedback only</input>
<br /><input type="radio" name="submissionOrFeedback" value="submissionAndFeedback"> Both submission and feedback</input>
</p>

<#list studentIds as studentId>
<input type="hidden" name="student" value="${studentId}" />
</#list>
</@spring.bind>
<br />
</p>
You only need to do this if if an erroneous submission has been made or the wrong feedback has been uploaded. 
If you are trying to re-use this assignment, you should go back and create a separate assignment instead.
</p>
<br />
<p>
<@form.errors path="confirm" />
<@form.label checkbox=true><@f.checkbox path="confirm" /> I confirm that I want to permanently delete these submissions and feedback items.</@form.label> 
</p>

<div class="submit-buttons">
<input class="btn btn-danger" type="submit" value="Delete">
</div>
</@f.form>

</#escape>