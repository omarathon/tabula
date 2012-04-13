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
	
	<#if features.privacyStatement>
	<@form.row>
	<span class="label">Privacy statement</span>
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