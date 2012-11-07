<#if !submission?? && assignment.collectSubmissions>
	<#include "assignment_deadline.ftl" />
</#if>

<#if (canSubmit && !submission??) || canReSubmit>

	<#if submission??>
	<hr>
	<h2>Re-submit</h2>
	<p>You can re-submit your work in case you've made a mistake,
		<#if isExtended>
			up until the end of your extension, <@fmt.date date=extension.expiryDate timezone=true /> (in ${durationFormatter(extension.expiryDate)}).
		<#else>
			up until the deadline, <@fmt.date date=assignment.closeDate timezone=true /> (in ${durationFormatter(assignment.closeDate)}).
	    </#if>
	</p>
	</#if>

	<#if assignment.closed && !isExtended>
		<div class="alert alert-error">
			<h3>Submission date has passed</h3>
			<p>
				You can still submit to this assignment but your mark may be affected. 
			</p>
		</div>
	</#if>

	<@f.form cssClass="submission-form form-horizontal" enctype="multipart/form-data" method="post" action="/module/${module.code}/${assignment.id}#submittop" modelAttribute="submitAssignmentCommand">
	<@f.errors cssClass="error form-errors">
	</@f.errors>
	
	<@form.row>
	 <label class="control-label">Your University ID</label>
	 <@form.field>
	   <div class="uneditable-input">${user.apparentUser.warwickId}</div>
	 </@form.field>
    </@form.row>
	
	<div class="submission-fields">
	
	<#list assignment.fields as field>
	<div class="submission-field">
	<#include "/WEB-INF/freemarker/submit/formfields/${field.template}.ftl" >
	</div>
	</#list>
	
	<#if features.privacyStatement>
	<@form.row>
	<label class="control-label">Privacy statement</label>
	<@form.field>
		<p class="privacy-field">
			The data on this form relates to your submission of 
			coursework. The date and time of your submission, your 
			identity and the work you have submitted will all be 
			stored, but will not be used for any purpose other than 
			administering and recording your coursework submission.
		</p>
	</@form.field>
	</@form.row>
	</#if>
	
	<#if assignment.displayPlagiarismNotice>
	<@form.row>
	<label class="control-label">Plagiarism declaration</label>
	<@form.field>
		<p class="plagiarism-field">
			Work submitted to the University of Warwick for official
			assessment must be all your own work and any parts that
			are copied or used from other people must be appropriately
			acknowledged. Failure to properly acknowledge any copied 
			work is plagiarism and may result in a mark of zero. 
		</p>
		<p>
			<@f.errors path="plagiarismDeclaration" cssClass="error" />
			<label><@f.checkbox path="plagiarismDeclaration" /> I confirm that this assignment is all my own work</label>
		</p>
	</@form.field>
	</@form.row>
	</#if>
	
	</div>
	
	<div class="submit-buttons">
	<input class="btn btn-large btn-primary" type="submit" value="Submit">
	</div>
	</@f.form>
	
<#elseif !submission??>

	<#if !assignment.collectSubmissions>
		<p>
			This assignment isn't collecting submissions through this system, but you may get
			an email to retrieve your feedback from here.
		</p>
		
		<h3>Expecting your feedback?</h3>
		
		<p>
			Sorry, but there doesn't seem to be anything here for you. 
			If you've been told to come here to retrieve your feedback 
			then you'll need to get in touch directly with your 
			course/module convenor to see why it hasn't been published yet. 
			When it's published you'll receive an automated email.
		</p>
		
	<#elseif assignment.closed>
		<div class="alert alert-error">
			<h3>Submission date has passed</h3>
			
			This assignment doesn't allow late submissions.
		</div>
	<#elseif !assignment.opened>
		<p>This assignment isn't open yet - it will open on <@fmt.date date=assignment.openDate at=true timezone=true />.</p>
	<#else>
		<p>
			
		</p>
	</#if>
	
</#if>
