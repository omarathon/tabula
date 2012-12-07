<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>


<@f.form method="post" action="${url('/admin/module/${module.code}/assignments/${assignment.id}/submissionsandfeedback/delete')}" commandName="deleteSubmissionsAndFeedbackCommand">

<h1>Delete submissions and/or feedback for ${assignment.name}</h1>

<@form.errors path="" />

<input type="hidden" name="confirmScreen" value="true" />

<@spring.bind path="studentsAsUsers">
<@form.errors path="students" />
<#assign students=status.actualValue />
<p>Deleting submissions and feedbacks for <strong><@fmt.p students?size "student" /></strong>:</p>
<ul>
<#list students as student>
<li>${student.fullName} (${student.warwickId})</li>
<input type="hidden" name="students" value="${student.warwickId}" />
</#list>
</ul>
<p>
Please specify what you would like to delete:
</p>
<br>
<@form.row>
<label><@f.radiobutton path="submissionOrFeedback" value="submissionOnly" /> Submissions only</label>
<label><@f.radiobutton path="submissionOrFeedback" value="feedbackOnly" /> Feedback only</label>
<label><@f.radiobutton path="submissionOrFeedback" value="submissionAndFeedback" /> Both submissions and feedback</label>
<br>
</@form.row>
</@spring.bind>

</p>
You only need to do this if if an erroneous submission has been made or the wrong feedback has been uploaded. 
If you are trying to re-use this assignment, you should go back and create a separate assignment instead.
</p>
<p>
<@form.errors path="confirm" />
<@form.label checkbox=true><@f.checkbox path="confirm" /> I confirm that I want to permanently delete these submissions/feedback items.</@form.label> 
</p>

<div class="submit-buttons">
<input class="btn btn-danger" type="submit" value="Delete">
</div>
</@f.form>

</#escape>