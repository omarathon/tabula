<#assign spring = JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f = JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#assign extension = command.extension />

<#escape x as x?html>
	<div class="content extension-detail">
		<@f.form method="post" enctype="multipart/form-data" action="${url('/coursework/admin/module/${module.code}/assignments/${assignment.id}/extensions/detail/${universityId}')}" commandName="modifyExtensionCommand" cssClass="form-horizontal double-submit-protection">
			<@f.input type="hidden" path="universityId" value="${universityId}" />

			<#if extension.awaitingReview>
				<input type="hidden" name="rawRequestedExpiryDate" value="${extension.requestedExpiryDate}" />
				<h5><i class="icon icon-question icon-large"></i> Requested <@fmt.date date=extension.requestedExpiryDate at=true/>&ensp;
				<span class="muted">${durationFormatter(assignment.closeDate, extension.requestedExpiryDate)} after the set deadline</span></h5>
				<#if extension.approved>
					<p><b>This is a revised request from the student.</b>
					There is already an extension approved until <@fmt.date date=extension.expiryDate at=true/>
					(${durationFormatter(assignment.closeDate, extension.expiryDate)} after the set deadline).</p>
				</#if>

				<#if features.disabilityRenderingInExtensions && extension.disabilityAdjustment && student?? && student.disability.reportable && can.do("Profiles.Read.Disability", student)>
					<p><i class="icon icon-stethoscope icon-large"></i> ${student.firstName} has requested their ${(student.disability.definition)!"recorded disability"} be taken into consideration.</p>
				</#if>

				<#if extension.reason?has_content>
					<details>
						<summary><i class="icon-quote-left"></i> Reason for request</summary>
						<p class="well">${extension.reason}</p>
					</details>
				</#if>

				<#if extension.attachments?has_content>
					<details>
						<summary><i class="icon-file"></i> Supporting documents</summary>
						<ul>
							<#list extension.attachments as attachment>
								<li>
									<a href="<@routes.extensionreviewattachment assignment=assignment userid=universityId filename=attachment.name />">
									${attachment.name}
									</a>
								</li>
							</#list>
						</ul>
					</details>
				</#if>

				<details>
					<summary><i class="icon-user"></i> About this student</summary>
					<dl class="unstyled">
						<#if (studentContext.course)?has_content>
							<#assign c = studentContext.course />
							<dt>Course details</dt>
							<dd>
								<@fmt.course_description c />
								<span class="muted">${(c.latestStudentCourseYearDetails.modeOfAttendance.fullNameToDisplay)!}</span>
							</dd>
						</#if>
						<#if (studentContext.relationships)?has_content>
							<dt>Relationships</dt>
							<dd>
								<#assign rels = studentContext.relationships />
								<ul class="unstyled">
									<#list rels?keys as key>
										<#list rels[key] as agent>
											<li>
												${agent.agentMember.fullName}, ${agent.agentMember.description} &ensp;<span class="muted">${key}</span>
											</li>
										</#list>
									</#list>
								</ul>
							</dd>
						</#if>
						<#if student.mobileNumber??><dt>Mobile number</dt><dd>${student.mobileNumber}</dd></#if>
						<#if student.phoneNumber?? && student.phoneNumber != student.mobileNumber!""><dt>Telephone number</dt><dd>${student.phoneNumber}</dd></#if>
						<#if student.email??><dt>Email address</dt><dd>${student.email}</dd></#if>
					</dl>
				</details>

				<hr />
			</#if>

			<div class="control-group">
				<@form.label path="expiryDate">Extended deadline</@form.label>
				<div class="controls">
					<@f.input id="picker0" path="expiryDate" cssClass="date-time-picker" />
					<#if !extension.manual && extension.unreviewed>
						<#-- FIXME write JS to make this button do what it says it will -->
						<button class="btn setExpiryToRequested">Use requested date</button>
					</#if>
				</div>
			</div>
			<div class="control-group">
				<@form.label path="reviewerComments">Comments</@form.label>
				<div class="controls">
					<@f.textarea path="reviewerComments" cssClass="span7" rows="6" />
				</div>
			</div>

			<@f.errors path="*" cssClass="error form-errors" />

			<div class="submit-buttons">
				<input class="btn btn-primary" type="submit" value="${approvalAction}" name="action">
				<#-- FIXME I think this should *probably* be changed to revoke for manual extensions. to ponder. -->
				<input class="btn btn-danger" type="submit" value="${rejectionAction}" name="action">
				<a class="btn discard-changes" href="">Discard changes</a>
			</div>
		</@f.form>
	</div>
</#escape>
