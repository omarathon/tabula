<#if detail.extension.requestedOn?has_content>
	<div>
		<label>Request recieved:</label> <@fmt.date date=detail.extension.requestedOn />
	</div>
<#else>
	<div>
		<label>Manually granted:</label> <@fmt.date date=detail.extension.reviewedOn />
	</div>
</#if>


<#if extension.requestedExpiryDate?has_content>
	<div>
		<label>Requested extension length:</label>
		<@fmt.p detail.extension.requestedExtraDuration "day"/> past the deadline.
		<@fmt.date date=detail.extension.requestedExpiryDate />
	</div>
</#if>
<#if detail.extension.feedbackDueDate?has_content>
	<div>
		<label>Feedback due:</label> <@fmt.date date=detail.extension.feedbackDueDate />
	</div>
</#if>
<div>
	<label>Previous extension requests:</label>
	<#if detail.previousExtensions?has_content>
		<a href="" data-toggle="modal" data-target="#prev-extensions-${detail.extension.id}">
			${detail.previousExtensions?size}
		</a>
	<#else>
		0
	</#if>
</div>
<div>
	<label>Previous submissions:</label>
	<#if detail.previousSubmissions?has_content>
		<a href="" data-toggle="modal" data-target="#prev-submissions-${detail.extension.id}">
			${detail.previousSubmissions?size}
		</a>
	<#else>
		0
	</#if>
</div>
<#if detail.extension.reason??>
	<details>
		<summary>Reason for request</summary>
		<textarea class="form-control" rows="3" disabled="disabled">${detail.extension.reason}</textarea>

		<#if detail.extension.attachments?has_content>
			<label>Supporting documents</label>
			<ul>
				<#list detail.extension.attachments as attachment>
					<li>
						<a href="<@routes.cm2.extensionAttachment detail.extension attachment.name />">
						${attachment.name}
						</a>
					</li>
				</#list>
			</ul>
		</#if>
	</details>
</#if>

<#assign formAction><@routes.cm2.extensionUpdate detail.extension /></#assign>

<@f.form
	method="post"
	class="modify-extension double-submit-protection"
	action="${formAction}"
	commandName="modifyExtensionCommand"
>

	<@bs3form.labelled_form_group "expiryDate" "Extended deadline">
		<div class="input-group">
			<@f.input path="expiryDate" cssClass="form-control date-time-picker" />
			<span class="input-group-addon"><i class="fa fa-calendar"></i></span>
		</div>
	</@bs3form.labelled_form_group>

	<@bs3form.labelled_form_group "reviewerComments" "Comments">
		<@f.textarea path="reviewerComments" cssClass="form-control text big-textarea" maxlength=4000/>
	</@bs3form.labelled_form_group>

	<div class="buttons form-group">
		<#if detail.extension.approved>
			<button type="submit" name="state" value="${modifyExtensionCommand.state.dbValue}" class="btn btn-default">Update</button>
			<button type="submit" name="state" value="${states.Revoked.dbValue}" class="btn btn-danger">Revoke</button>
		<#elseif detail.extension.rejected || detail.extension.revoked>
			<button type="submit" name="state" value="${modifyExtensionCommand.state.dbValue}" class="btn btn-default">Update</button>
			<button type="submit" name="state" value="${states.Approved.dbValue}" class="btn btn-primary">Accept</button>
		<#elseif detail.extension.moreInfoRequired>
			<button type="submit" name="state" value="${modifyExtensionCommand.state.dbValue}" class="btn btn-default">Update</button>
			<button type="submit" name="state" value="${states.Approved.dbValue}" class="btn btn-primary">Accept</button>
			<button type="submit" name="state" value="${states.Rejected.dbValue}" class="btn btn-danger">Reject</button>
		<#elseif detail.extension.unreviewed || detail.extension.moreInfoRequired>
			<button type="submit" name="state" value="${states.Approved.dbValue}" class="btn btn-primary">Accept</button>
			<button type="submit" name="state" value="${states.Rejected.dbValue}" class="btn btn-danger">Reject</button>
			<button type="submit" name="state" value="${states.MoreInformationRequired.dbValue}" class="btn btn-default">Request more info</button>
		</#if>
	</div>

</@f.form>



<#if detail.previousExtensions?has_content>
	<div id="prev-extensions-${detail.extension.id}" class="modal fade" role="dialog">
		<div class="modal-dialog" role="document">
			<div class="modal-content">
				<div class="modal-header">
					<button type="button" class="close" data-dismiss="modal" aria-label="Close">
						<span aria-hidden="true">&times;</span>
					</button>
					<h4 class="modal-title">Previous extension requests</h4>
				</div>
				<div class="modal-body">
					<h5>${detail.student.fullName} - ${detail.student.warwickId}</h5>
					<div><strong>Accepted requests: </strong> ${detail.numAcceptedExtensions}</div>
					<div><strong>Denied requests: </strong> ${detail.numRejectedExtensions}</div>
					<table class="table table-striped">
						<thead>
							<tr>
								<th>Module</th>
								<th>Assignment</th>
								<th>Status</th>
								<th>Made</th>
							</tr>
						</thead>
						<tbody>
							<#list detail.previousExtensions as e>
								<tr>
									<td>${e.assignment.module.code}</td>
									<td>${e.assignment.name}</td>
									<td>${e.state.description}</td>
									<td>
										<#if e.requestedOn?has_content>
											<@fmt.date date=e.requestedOn />
										<#else>
											<@fmt.date date=e.reviewedOn />
										</#if>
									</td>
								</tr>
							</#list>
						</tbody>
					</table>
				</div>
			</div>
		</div>
	</div>
</#if>

<#if detail.previousSubmissions?has_content>
	<div id="prev-submissions-${detail.extension.id}" class="modal fade" role="dialog">
		<div class="modal-dialog" role="document">
			<div class="modal-content">
				<div class="modal-header">
					<button type="button" class="close" data-dismiss="modal" aria-label="Close">
						<span aria-hidden="true">&times;</span>
					</button>
					<h4 class="modal-title">Previous submissions</h4>
				</div>
				<div class="modal-body">
					<h5>${detail.student.fullName} - ${detail.student.warwickId}</h5>
					<table class="table table-striped">
						<thead>
						<tr>
							<th>Module</th>
							<th>Assignment</th>
							<th>Status</th>
						</tr>
						</thead>
						<tbody>
						<#list detail.previousSubmissions as submission>
							<tr>
								<td>${submission.assignment.module.code}</td>
								<td>${submission.assignment.name}</td>
								<td>
									<#if submission.isAuthorisedLate()>
										Within extension
									<#elseif submission.isLate()>
										Late
									<#else>
										On time
									</#if>
								</td>
							</tr>
						</#list>
						</tbody>
					</table>
				</div>
			</div>
		</div>
	</div>
</#if>