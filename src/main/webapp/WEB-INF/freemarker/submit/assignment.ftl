<#assign fmt=JspTaglibs["/WEB-INF/tld/fmt.tld"]>
<#assign warwick=JspTaglibs["/WEB-INF/tld/warwick.tld"]>
<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<h1>${module.name} - ${assignment.name}</h1>

<#if feedback??>

	<h2>Feedback for ${user.universityId}</h2>
	
	<p>
		Your feedback consists of ${feedback.attachments?size} files.
		
		<p>
			<a href="<@url page="/module/${module.code}/${assignment.id}/feedback.zip"/>">
				Download all as Zip file
			</a>
		</p>
		
		<ul class="file-list">
		<#list feedback.attachments as attachment>
			<li>${attachment.name}</li>
		</#list>
		</ul>
	</p>

<#elseif assignment.resultsPublished>

	<h2>No feedback for ${user.universityId}</h2>
	
	<p>Feedback has been published for this assignment, but there doesn't appear to be
	any for you. If you submitted your assignment were told to expect marks/feedback here, please
	speak to your module convenor.</p>

<#else>

	<h2>${user.fullName} (${user.universityId})</h2>

	<p>
		If you've submitted your assignment, you should be able to access your
		feedback here once it's ready.
	</p>

<#-- Dead code for now, until submission is properly implemented -->
<#if assignment.active && false>

	<p>Submission closes <@warwick.formatDate value=assignment.closeDate pattern="d MMMM yyyy HH:mm:ss (z)" /></p>

	<@f.form cssClass="submission-form" method="post" action="/module/${module.code}/${assignment.id}" commandName="submitAssignment">
	<@f.errors cssClass="error form-errors">
	</@f.errors>
	<p>Student: ${user.apparentUser.warwickId}</p>
	
	<div class="submission-fields">
	
	<#list assignment.fields as field>
	<div class="submission-field">
	<#include "/WEB-INF/freemarker/submit/formfields/${field.template}.ftl" >
	</div>
	</#list>
	
	</div>
	
	<div class="submit-buttons">
	<input type="submit" value="Submit">
	</div>
	</@f.form>
</#if>

</#if>

</#escape>