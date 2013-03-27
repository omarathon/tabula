<div class="submission-received">
	<#if justSubmitted!false>
	<div class="alert alert-success">
		<a class="close" data-dismiss="alert">&times;</a>Thanks, we've received your submission.
	</div>
	</#if>
	
	<div class="submission-receipt-container is-stackable">
	<div class="submission-receipt">
	<h3>Submission receipt</h3>
	<p>Submission received <@fmt.date date=submission.submittedDate at=true seconds=true relative=false />.</p>
	<p>Submission ID: ${submission.id}</p>
	<#if submission.allAttachments??>
		<p>
			Uploaded attachments:
			<ul>
				<#list submission.allAttachments as attachment>
					<li><a href="${url('/module/${module.code}/${assignment.id}/attachment/${attachment.name?url}')}">${attachment.name}</a></li>
				</#list>  
			</ul>
		</p>
	</#if>
	</div>
	</div>
	
	<p>You should have been sent an email confirming the submission. Check your spam folders if it doesn't show up in your inbox. 
	If it's been a few minutes and it still hasn't reached you, click the button below to send a fresh copy.</p>
	
	
	<#assign receiptFormUrl><@routes.assignmentreceipt assignment=assignment /></#assign>
	<form action="${receiptFormUrl}" method="POST">
		<div class="submit-buttons">
		<button class="btn" name="resend" value=""><i class="icon-envelope"></i> Re-send email receipt</button>
		</div>
	</form> 
	
	<#if assignment.hasReleasedFeedback && !feedback??>
		<h3>Expecting your feedback?</h3>
		<p>
			There is no feedback available for you yet. 
			If you've been told to come here to retrieve your feedback 
			then you'll need to get in touch directly with your 
			course/module convenor to see why it hasn't been published yet. 
			<#if !features.emailStudents>
				When it's published you'll receive an automated email.
			</#if>
		</p>
	</#if>

</div>