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
					<li><a href="/module/${module.code}/${assignment.id}/attachment/${attachment.name}">${attachment.name}</a></li>
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
	
</div>