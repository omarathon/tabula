<#import "*/modal_macros.ftl" as modal />
<#escape x as x?html>

<#if !isSelf>
	<details class="indent">
		<summary>${member.officialName}</summary>
		<#if member.userId??>
		${member.userId}<br/>
		</#if>
		<#if member.email??>
			<a href="mailto:${member.email}">${member.email}</a><br/>
		</#if>
		<#if member.phoneNumber??>
		${phoneNumberFormatter(member.phoneNumber)}<br/>
		</#if>
		<#if member.mobileNumber??>
		${phoneNumberFormatter(member.mobileNumber)}<br/>
		</#if>
	</details>
</#if>

<h1>${relationshipType.agentRole?cap_first}</h1>

<#if !relationships?has_content>
	<div class="alert alert-info">No ${relationshipType.agentRole} details found for this course and academic year</div>
</#if>

<div class="row">
	<#list relationships as relationship>
		<div class="col-md-4">
			<div class="clearfix">
				<#if relationship.agentMember??>
					<@fmt.relation_photo member relationship "thumbnail" />
				</#if>

			</div>
			<br/>
			<p>
				<a class="btn btn-default" data-target="#timeline" data-toggle="modal">View timeline</a>
			</p>
			<#if relationship.agentMember??>
				<p>
					<strong>Name:</strong> ${relationship.agentMember.fullName!relationshipType.agentRole?cap_first}<br />
					<strong>Warwick email:</strong> <a href="mailto:${relationship.agentMember.email}">${relationship.agentMember.email}</a><br />
					<strong>University number:</strong> ${relationship.agentMember.universityId}<br />
					<#if relationship.agentMember.userId??><strong>IT code:</strong> ${relationship.agentMember.userId}<br /></#if>
					<span class="peoplesearch-info" data-href="<@routes.profiles.peoplesearchData relationship.agentMember />"></span>
				</p>
			<#else>
				<p>
					<strong>Name:</strong> ${relationship.agentName}<br/>
					<span class="muted">External to Warwick</span>
				</p>
			</#if>
		</div>
	</#list>
</div>

<div id="timeline" class="modal fade">
	<@modal.wrapper>
		<@modal.header>
			<h3 class="modal-title">Changes to ${relationshipType.agentRole}</h3>
		</@modal.header>
		<@modal.body>
			<#if !relationships?has_content>
				<em>No ${relationshipType.agentRole} details found for this course</em>
			<#else>
				<table class="table table-hover table-condensed sb-no-wrapper-table-popout">
					<thead>
					<tr>
						<th></th>
						<th>Start date</th>
						<th>End date</th>
					</tr>
					</thead>
					<tbody>
						<#macro timeline_row relationship>
						<tr>
							<td>
								<@fmt.relation_photo member relationship "tinythumbnail" />
								${relationship.agentMember.fullName!relationship.relationshipType.agentRole?cap_first}
								<#if relationship.percentage?has_content>
									<span class="percentage muted">(${relationship.percentage}%)</span>
								</#if>
							</td>
							<td>
								<#if relationship.startDate??>
									<@fmt.date date=relationship.startDate includeTime=false />
								<#else>
									<em>Unknown</em>
								</#if>
							</td>
							<td>
								<#if relationship.endDate??>
									<@fmt.date date=relationship.endDate includeTime=false />
								<#else>
									<em>None</em>
								</#if>
							</td>
						</tr>
						</#macro>
						<#list relationships as rel>
							<#if rel.current>
								<@timeline_row rel />
							</#if>
						</#list>
						<#list relationships as rel>
						<#if !rel.current>
							<@timeline_row rel />
						</#if>
					</#list>
					</tbody>
				</table>
			</#if>
		</@modal.body>
	</@modal.wrapper>
</div>

<h2>Record of meetings</h2>

<#if !(canReadMeetings!true)>
	<div class="alert alert-error">You do not have permission to view the meetings for this student</div>
<#elseif !meetings?has_content>
	<div class="alert alert-info">No meeting records exist for this academic year</div>
<#else>
	<section class="meetings">
		<#if meetings?has_content>
			<table class="table table-striped table-condensed expanding-row-pairs">
				<thead>
					<tr>
						<th>Date</th>
						<th>Title</th>
					</tr>
				</thead>
				<tbody>
					<#list meetings as meeting>
						<#assign expand = defaultExpand(meeting)/>
						<tr ${expand?string('class=expand','')} <#if meeting.deleted>class="deleted subtle"</#if>>
							<td data-sortby="${meeting.meetingDate.millis?c}"><@fmt.date date=meeting.meetingDate includeTime=false /></td>
							<td>
								<#if meeting.scheduled>
									<#assign editUrl><@routes.profiles.edit_scheduled_meeting_record meeting studentCourseDetails academicYear relationshipType /></#assign>
								<#else>
									<#assign editUrl><@routes.profiles.edit_meeting_record studentCourseDetails academicYear meeting /></#assign>
								</#if>
								<#if ((meeting.scheduled && can.do_with_selector("Profiles.ScheduledMeetingRecord.Manage", meeting, relationshipType)) ||
									(!meeting.scheduled && user.universityId! == meeting.creator.universityId && !meeting.approved))
								>
									<div class="pull-right">
										<i class="fa fa-spinner fa-spin invisible"></i>
										<span class="dropdown">
											<a class="btn btn-default btn-xs dropdown-toggle" data-toggle="dropdown">Actions <span class="caret"></span></a>
											<ul class="dropdown-menu pull-right">
												<li>
													<a href="${editUrl}" class="edit-meeting-record <#if meeting.deleted>disabled</#if>" title="Edit record">Edit</a>
												</li>
												<li>
													<a href="<@routes.profiles.delete_meeting_record meeting />" class="delete-meeting-record <#if meeting.deleted>disabled</#if>" title="Delete record">Delete</a>
												</li>
												<li>
													<a href="<@routes.profiles.restore_meeting_record meeting />" class="restore-meeting-record <#if !meeting.deleted>disabled</#if>" title="Restore record">Restore</a>
												</li>
												<li>
													<a href="<@routes.profiles.purge_meeting_record meeting />" class="purge-meeting-record <#if !meeting.deleted>disabled</#if>" title="Purge record">Purge</a>
												</li>
											</ul>
										</span>
									</div>
								</#if>
								${meeting.title!}
							</td>
						</tr>
						<tr>
							<td colspan="2">
								<#if meeting.description??>
									<div class="description">
										<#noescape>${meeting.escapedDescription}</#noescape>
									</div>
								</#if>

								<#if (meeting.attachments?? && meeting.attachments?size > 0)>
									<#assign mrDownloadUrl><@routes.profiles.download_meeting_record_attachment relationshipType meeting /></#assign>
									<@fmt.download_attachments meeting.attachments mrDownloadUrl "for this meeting record" "${meeting.title?url}" />
								</#if>

								<#if meeting.scheduled>
									<@scheduledMeetingState meeting />
								<#else>
									<@meetingState meeting />
								</#if>
							</td>
						</tr>
					</#list>
				</tbody>
			</table>
		</#if>

		<#if relationships?has_content>
			<p>
				<#if canCreateScheduledMeetings && features.scheduledMeetings>
					<#if isSelf>
						<a class="btn btn-primary new-meeting-record" href="<@routes.profiles.create_scheduled_meeting_record studentCourseDetails activeAcademicYear relationshipType />">Request new meeting</a>
					<#else>
						<a class="btn btn-primary new-meeting-record" href="<@routes.profiles.create_scheduled_meeting_record studentCourseDetails activeAcademicYear relationshipType />">New meeting</a>
					</#if>
				</#if>
				<#if canCreateMeetings>
					<#if isSelf>
						<a class="btn btn-default new-meeting-record" href="<@routes.profiles.create_meeting_record studentCourseDetails activeAcademicYear relationshipType />">Add new</a>
					<#else>
						<a class="btn btn-default new-meeting-record" href="<@routes.profiles.create_meeting_record studentCourseDetails activeAcademicYear relationshipType />">New record</a>
					</#if>
				</#if>
			</p>
		</#if>

		<div id="meeting-modal" class="modal fade" style="display:none;"></div>
	</section>
</#if>

<#function defaultExpand meeting>
	<#if meeting.scheduled>
		<#return meeting.pendingAction && can.do("Profiles.ScheduledMeetingRecord.Confirm", meeting) />
	<#else>
		<#return meeting.pendingApprovalBy(user) || meeting.pendingRevisionBy(user) />
	</#if>
</#function>

<#macro meetingState meeting>
	<#if meeting.pendingApproval && !meeting.pendingApprovalBy(user)>
		<p class="very-subtle">Pending approval. Submitted by ${meeting.creator.fullName}, <@fmt.date meeting.creationDate /></p>
		<div class="alert alert-info">
			This meeting record needs to be approved by <#list meeting.pendingApprovers as approver>${approver.fullName}<#if approver_has_next>, </#if></#list>.
		</div>
	<#elseif meeting.pendingApprovalBy(user)>
		<p class="very-subtle">Pending approval. Submitted by ${meeting.creator.fullName}, <@fmt.date meeting.creationDate /></p>
		<div class="alert alert-info">
			This record needs your approval. Please review, then approve or return it with comments.
			<#if meetingApprovalWillCreateCheckpoint[meeting.id]>
				<br />
				Approving this meeting record will mark a monitoring point as attended.
			</#if>
		</div>

		<!-- not a spring form as we don't want the issue of binding multiple sets of data to the same command -->
		<form method="post" class="approval" id="meeting-${meeting.id}" action="<@routes.profiles.save_meeting_approval meeting />" >
			<@bs3form.form_group>
				<@bs3form.radio>
					<input type="radio" name="approved" value="true">
					Approve
				</@bs3form.radio>
				<@bs3form.radio>
					<input class="reject" type="radio" name="approved" value="false">
					Return with comments
				</@bs3form.radio>
				<div class="rejection-comment" style="display:none">
					<textarea class="form-control" name="rejectionComments"></textarea>
				</div>
			</@bs3form.form_group>

			<button type="submit" class="btn btn-primary spinnable spinner-auto">Submit</button>
		</form>
	<#elseif meeting.rejectedBy(currentMember)>
		<p class="very-subtle">Pending revision. Submitted by ${meeting.creator.fullName}, <@fmt.date meeting.creationDate /></p>
		<div class="alert alert-info">
			<p>You sent this record back to the other party, who will review the record and submit it for approval again.</p>
		</div>
	<#elseif meeting.pendingRevisionBy(user)>
		<p class="very-subtle">Pending approval. Submitted by ${meeting.creator.fullName}, <@fmt.date meeting.creationDate /></p>
		<div class="alert alert-info">
			<#list meeting.rejectedApprovals as rejectedApproval>
				<p>This record has been returned with comments by ${rejectedApproval.approver.fullName} because:</p>
				<blockquote>${rejectedApproval.comments}</blockquote>
				<p>Please edit the record and submit it for approval again.</p>
			</#list>
			<a class="edit-meeting-record btn btn-primary" href="<@routes.profiles.edit_meeting_record studentCourseDetails activeAcademicYear meeting/>">Edit</a>
		</div>
	<#else>
		<p class="very-subtle">
			${(meeting.format.description)!"Unknown format"} between ${(meeting.relationship.agentName)!meeting.relationship.relationshipType.agentRole} and ${(meeting.relationship.studentMember.fullName)!"student"}.
			Created by ${meeting.creator.fullName}, <@fmt.date meeting.lastUpdatedDate />.
			<#if meeting.approved && (meeting.approvedBy?has_content || meeting.approvedDate?has_content)>
				Approved<#if meeting.approvedBy?has_content> by ${meeting.approvedBy.fullName},</#if><#if meeting.approvedDate?has_content> <@fmt.date meeting.approvedDate /></#if>
			</#if>
		</p>
	</#if>
</#macro>

<#macro scheduledMeetingState meeting>
	<#local pendingAction = meeting.pendingAction />
	<#local canConvertMeeting = can.do("Profiles.ScheduledMeetingRecord.Confirm", meeting) />

	<#if pendingAction && !canConvertMeeting>
		<p class="very-subtle">Pending confirmation. Submitted by ${meeting.creator.fullName}, <@fmt.date meeting.creationDate /></p>
		<#if !meeting.pendingActionBy(user)>
			<div class="alert alert-info">
				${meeting.creator.fullName} needs to confirm that this meeting took place.
			</div>
		</#if>
	<#elseif pendingAction && canConvertMeeting>
		<p class="very-subtle">Pending confirmation. ${(meeting.format.description)!"Unknown format"} between ${(meeting.relationship.agentName)!meeting.relationship.relationshipType.agentRole} and ${(meeting.relationship.studentMember.fullName)!"student"}. Created by ${meeting.creator.fullName}, <@fmt.date meeting.lastUpdatedDate /></p>
		<#if meeting.deleted>
			<p class="very-subtle">This meeting has been deleted</p>
		<#else>
			<div class="alert alert-info">
				Please confirm whether this scheduled meeting took place.
			</div>

			<form method="post" class="scheduled-action" id="meeting-${meeting.id}" action="<@routes.profiles.choose_action_scheduled_meeting_record meeting studentCourseDetails activeAcademicYear meeting.relationship.relationshipType />" >
				<@bs3form.form_group>
					<@bs3form.radio>
						<input checked type="radio" name="action" value="confirm" data-formhref="<@routes.profiles.confirm_scheduled_meeting_record meeting studentCourseDetails activeAcademicYear meeting.relationship.relationshipType />">
						Confirm
					</@bs3form.radio>
					<@bs3form.radio>
						<input type="radio" name="action" value="reschedule" />
						Reschedule
					</@bs3form.radio>
					<@bs3form.radio>
						<input type="radio" name="action" value="missed" class="reject" data-formhref="<@routes.profiles.missed_scheduled_meeting_record meeting meeting.relationship.relationshipType />">
						Record that the meeting did not take place
					</@bs3form.radio>
					<div class="rejection-comment" style="display:none">
						<textarea class="form-control" name="missedReason"></textarea>
					</div>
				</@bs3form.form_group>

				<div class="ajaxErrors alert alert-danger" style="display: none;"></div>
				<button type="submit" class="btn btn-primary">Submit</button>
			</form>
		</#if>
	<#elseif meeting.missed>
		<div class="alert alert-danger">
			<#if meeting.missedReason?has_content>
				<p>This meeting did not take place because:</p>
				<blockquote>${meeting.missedReason}</blockquote>
			<#else>
				<p>This meeting did not take place (no reason given)</p>
			</#if>
		</div>
		<p class="very-subtle">${(meeting.format.description)!"Unknown format"} between ${(meeting.relationship.agentName)!meeting.relationship.relationshipType.agentRole} and ${(meeting.relationship.studentMember.fullName)!"student"}. Created by ${meeting.creator.fullName}, <@fmt.date meeting.lastUpdatedDate /></p>
	<#else>
		<p class="very-subtle">${(meeting.format.description)!"Unknown format"} between ${(meeting.relationship.agentName)!meeting.relationship.relationshipType.agentRole} and ${(meeting.relationship.studentMember.fullName)!"student"}. Created by ${meeting.creator.fullName}, <@fmt.date meeting.lastUpdatedDate /></p>
	</#if>
</#macro>

</#escape>