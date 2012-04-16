<div class="submission-received">
	<p>Thanks for your submission.</p>
	
	<div class="submission-receipt-container is-stackable">
	<div class="submission-receipt">
	<h3>Submission receipt</h3>
	<p>Submission received <@fmt.date date=submission.submittedDate at=true seconds=true relative=false />.</p>
	<p>Submission ID: ${submission.id}</p>
	</div>
	</div>
	
	<p>You should have been sent an email confirming the submission. Check your spam folders if it doesn't show up in your inbox. 
	If it's been a few minutes and it still hasn't reached you, click the button below to send a fresh copy.</p>
	
	
	<#assign receiptFormUrl><@routes.assignmentreceipt assignment=assignment /></#assign>
	<form action="${receiptFormUrl}" method="POST">
		<div class="submit-buttons">
		<input type="submit" name="resend" value="Re-send email receipt">
		</div>
	</form> 
	
</div>