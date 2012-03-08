<#assign fmt=JspTaglibs["/WEB-INF/tld/fmt.tld"]>
<#assign warwick=JspTaglibs["/WEB-INF/tld/warwick.tld"]>
<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
<#compress>
<h1>${module.name} (${module.code?upper_case})
<br><strong>${assignment.name}</strong></h1>

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

	<#if features.submissions>
		<#if submission??>
			<div class="submission-received">
				<p>Thanks for your submission.</p>
				
				<div class="submission-receipt-container is-stackable">
				<div class="submission-receipt">
				<h3>Submission receipt</h3>
				<p>Submission received <@warwick.formatDate value=submission.submittedDate pattern="d MMMM yyyy 'at' HH:mm:ss" />.</p>
				<p>Submission ID: ${submission.id}</p>
				</div>
				</div>
				
				<p>You should have been sent an email confirming the submission. Check your spam folders if it doesn't show up in your inbox. 
				If it's been a few minutes and it still hasn't reached you, click the button below to send a fresh copy.</p>
				
				<#assign receiptFormUrl><@routes.assignmentreceipt assignment=assignment /></#assign>
				<form action="${receiptFormUrl}" method="POST">
					<input type="submit" name="resend" value="Re-send email">
				</form> 
			</div>
		</#if>
			
		<#-- At some point, also check if resubmission is allowed for this assignment -->
		<#if !submission??>
			<p>Submission deadline: <@warwick.formatDate value=assignment.closeDate pattern="d MMMM yyyy HH:mm (z)" /></p>
		
			<#if assignment.submittable>
		
				<#if assignment.closed>
					<div class="potential-problem">
						<h3>Submission date has passed</h3>
						<p>
							You can still submit to this assignment but your mark may be affected. 
						</p>
					</div>
				</#if>
			
				<@f.form cssClass="submission-form" enctype="multipart/form-data" method="post" action="/module/${module.code}/${assignment.id}" modelAttribute="submitAssignmentCommand">
				<@f.errors cssClass="error form-errors">
				</@f.errors>
				
				<@form.row>
				 <span class="label">Your University ID</span>
				 <@form.field>
				   ${user.apparentUser.warwickId}
				 </@form.field>
			    </@form.row>
				
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
			
				<#if !assignment.collectSubmissions>
					<p>
						This assignment isn't collecting submissions through this system, but you may get
						an email to retrieve your feedback from here.
					</p>
				<#elseif assignment.closed>
					<div class="potential-problem">
						<h3>Submission date has passed</h3>
						<p>
							This assignment doesn't allow late submissions.
						</p>
					</div>
				<#elseif !assignment.opened>
					<p>This assignment isn't open yet - it will open on <@warwick.formatDate value=assignment.openDate pattern="d MMMM yyyy 'at' HH:mm (z)" />.</p>
				<#else>
					<p>
						
					</p>
				</#if>
				
			</#if>
		</#if><#-- submission?? -->
		
	<#else>
	
		<h2>${user.fullName} (${user.universityId})</h2>
	
		<p>
			If you've submitted your assignment, you should be able to access your
			feedback here once it's ready.
		</p>	
	
	</#if>

</#if>

</#compress>
</#escape>