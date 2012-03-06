<#assign fmt=JspTaglibs["/WEB-INF/tld/fmt.tld"]>
<#assign warwick=JspTaglibs["/WEB-INF/tld/warwick.tld"]>
<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<#compress>
<h1>${module.name} (${module.code?upper_case}) - ${assignment.name}</h1>

<#if feedback??>

	<h2>Feedback for ${user.universityId}</h2>
	
	<#if features.collectRatings>
		<div id="feedback-rating-container" class="is-stackable">
			<!-- fallback for noscript -->
			<div style="padding:0.5em">
			<a target="_blank" href="<@routes.ratefeedback feedback />">Rate your feedback</a> (opens in a new window/tab)
			</div>
		</div>
	</#if>
	
	<p>
		<#-- Only offer a Zip if there's more than one file. -->
		<#if feedback.attachments?size gt 1>
			<p>Your feedback consists of ${feedback.attachments?size} files.</p>
			<p>
				<a href="<@url page="/module/${module.code}/${assignment.id}/all/feedback.zip"/>">
					Download all as a Zip file
				</a>
			</p>
			<p>Or download the attachments individually below.</p>
		<#else>
			<p>Your feedback file is available to download below.</p>
		</#if>
		
		<ul class="file-list">
		<#list feedback.attachments as attachment>
			<li>
			<a href="<@url page="/module/${module.code}/${assignment.id}/get/${attachment.name?url}"/>">
				${attachment.name}
			</a>
			</li>
		</#list>
		</ul>
	</p>

<#else>

	<#if user.loggedIn>
	
		<#if features.submissions>
		
			<#if assignment.submittable>
		
				<p>Submission deadline: <@warwick.formatDate value=assignment.closeDate pattern="d MMMM yyyy HH:mm (z)" /></p>
				
				<#if assignment.closed>
					
				</#if>
			
				<@f.form cssClass="submission-form" enctype="multipart/form-data" method="post" action="/module/${module.code}/${assignment.id}" modelAttribute="submitAssignmentCommand">
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
				
			<#else>
				
			</#if>
			
		<#else>
		
			<h2>${user.fullName} (${user.universityId})</h2>
		
			<p>
				If you've submitted your assignment, you should be able to access your
				feedback here once it's ready.
			</p>	
		
		</#if>
	
	<#else><!-- not logged in -->
		
		<p>
		You'll need to <a class="sso-link" href="<@sso.loginlink />">Sign in</a>
		first.
		</p>
		
	</#if>

</#if>

</#compress>
</#escape>