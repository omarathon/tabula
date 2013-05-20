<#if meeting.pendingApproval && viewer == meeting.creator>
	<small class="muted">Pending approval. Submitted by ${meeting.creator.fullName}, <@fmt.date meeting.lastUpdatedDate /></small>
	<div class="alert alert-info">
		This meeting record needs to be approved.
	</div>
<#elseif meeting.pendingApprovalBy(viewer)>
	<small class="muted">Pending approval. Submitted by ${meeting.creator.fullName}, <@fmt.date meeting.lastUpdatedDate /></small>
	<div class="pending-action alert alert-warning">
		This record needs your approval. Please review, then approve or reject it.
		If you reject it, please explain why.
	</div>
	<!-- not a spring form as we don't want the issue of binding multiple sets of data to the same command -->
	<form method="post" id="meeting-${meeting.id}" action="<@routes.save_meeting_approval meeting />" >
		<@form.row>
			<@form.field>
				<label class="radio inline">
					<input type="radio" name="approved" value="true">
					Approve
				</label>
				<label class="radio inline">
					<input class="reject" type="radio" name="approved" value="false">
					Reject
				</label>
			</@form.field>
		</@form.row>
		<div class="rejection-comment" style="display:none">
			<@form.row>
				<@form.field>
					<textarea class="big-textarea" name="rejectionComments"></textarea>
				</@form.field>
			</@form.row>
		</div>
		<button type="submit" class="btn btn-primary">Submit</button>
	</form>
<#elseif meeting.pendingRevisionBy(viewer)>
	<small class="muted">Pending approval. Submitted by ${meeting.creator.fullName}, <@fmt.date meeting.lastUpdatedDate /></small>
	<div class="pending-action alert alert-error">
		<#list meeting.rejectedApprovals as rejectedApproval>
			<div class="rejection">
				<p>This record has been rejected by ${rejectedApproval.approver.fullName} because:</p>
				<p class="reason">${rejectedApproval.comments}</p>
				<p>Please edit the record and submit it for approval again.</p>
			</div>
		</#list>
	</div>
	<div class="submit-buttons">
		<a class="btn btn-primary" href="<@routes.edit_meeting_record meeting />">Edit</a>
	</div>
<#else>
	<small class="muted">${(meeting.format.description)!"Unknown format"} between ${(meeting.relationship.agentName)!meeting.relationship.relationshipType.description} and ${(meeting.relationship.studentMember.fullName)!"student"}. Created by ${meeting.creator.fullName}, <@fmt.date meeting.lastUpdatedDate /></small>
</#if>
