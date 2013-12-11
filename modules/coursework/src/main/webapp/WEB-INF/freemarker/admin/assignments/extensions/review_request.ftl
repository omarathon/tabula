<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>
	<div class="modal-header">
		<button type="button" class="close" data-dismiss="modal">×</button>
		<h3>Review extension request by ${userFullName}</h3>
	</div>
	<@f.form method="post" action="${url('/admin/module/${module.code}/assignments/${assignment.id}/extensions/review-request/${universityId}')}" commandName="modifyExtensionCommand">
		<div class="modal-body reviewExtensionModal">
			<div class="control-group clearfix alert-info requestDateInfo">
				<div>
					<label>Assignment deadline</label>
					<@fmt.date date=assignment.closeDate at=true/>
				</div>

				<i class="icon-arrow-right"></i>
				<div>
					<label>Requested extension deadline</label>
					<@fmt.date date=extension.requestedExpiryDate at=true/>
				</div>
			</div>
			<div class="extension-details">
				<div class="extensionReason">
					<div class="control-group" >
						<label><h4>Reason for extension request</h4></label>
						<div class="controls">
							${extension.reason}
						</div>
					</div>
				</div>

				<div class="extensionFurtherDetail">

					<h4>Further Details</h4>
					<#if extension.attachments?has_content>
					<details>
					<summary><h5>Supporting documents</h5></summary>
						<div class="controls">
							<ul class="unstyled">
								<#list extension.attachments as attachment>
									<li>
										<a href="<@routes.extensionreviewattachment assignment=assignment userid=universityId filename=attachment.name />">
										${attachment.name}
										</a>
									</li>
								</#list>
							</ul>
						</div>
					</details>
					</#if>

					<#if moduleManagers?has_content >
						<details>
							<summary><h5>Module Managers</h5></summary>
							<ul class="unstyled">
								<#list moduleManagers as manager>
									<li>
										<h6>${manager.getFullName()} (${manager.getWarwickId()})</h6>
									${manager.getEmail()}

									</li>
								</#list>
							</ul>
						</details>
					</#if>

					<#if student?has_content >
					<details>
						<summary><h5>Student Contact Details</h5></summary>
						<ul class="unstyled">
							<li>
								<h6>Mobile Number</h6>
								${(student.mobileNumber)!"Not available"}
							</li>
							<li>
								<h6>Telephone Number</h6>
								${(student.phoneNumber)!"Not available"}
							</li>
							<li>
								<h6>Email Address</h6>
								${(student.email)!"Not available"}
							</li>
						</ul>

					</details>
					<#else>
						<details>
							<summary><h5>Contact Details</h5></summary>
							<ul class="unstyled">
								<li>
									<h6>Email Address</h6>
								${(email)!"Not available"}
								</li>
							</ul>

						</details>
					</#if>

					<#if relationships?has_content >
						<details>
							<summary><h5>Student Relationships</h5></summary>
							<#list relationships?keys as key>
							<#if relationships[key]?has_content>
								<h6>${key}<#if (relationships[key]?size > 1)>s</#if></h6>
								<ul class="unstyled">
									<#list relationships[key] as agent>
										<li>
											<strong>${agent.agentMember.fullName} (${agent.agentMember.universityId})</strong>
											${agent.agentMember.description}
										</li>
									</#list>
								</ul>
							</#if>
							</#list>
						</details>
					</#if>

					<#if studentCourseDetails?has_content >
					<details>
						<summary><h5>Student Course Details</h5></summary>
						<ul class="unstyled">
						<li>
							<h6>Route</h6>
							${(studentCourseDetails.route.name)!} (${(studentCourseDetails.route.code?upper_case)!})
						</li>
							<li>
								<h6>Course</h6>
								<@fmt.course_description studentCourseDetails />
							</li>
							<li>
								<h6>Intended award and type</h6>
								${(studentCourseDetails.awardCode)!} (${(studentCourseDetails.route.degreeType.toString)!})
							</li>
							<li>
								<h6>Attendance</h6>
								${(studentCourseDetails.latestStudentCourseYearDetails.modeOfAttendance.fullNameToDisplay)!}
							</li>
						</ul>
					</details>
					</#if>
				</div>
				<div class="clearfix"></div>
			</div>
			<@f.input type="hidden" path="extensionItems[0].universityId" value="${universityId}" />
			<div class="control-group extensionNewDate">
				<@form.label path="extensionItems[0].expiryDate"><strong>New deadline</strong></@form.label>
				<div class="controls" >
					<@f.input id="picker0" path="extensionItems[0].expiryDate" class="date-time-picker" />
				</div>
			</div>
			<div class="control-group extensionComments">
				<@form.label path="extensionItems[0].approvalComments"><strong>Comments</strong></@form.label>
				<div class="controls">
					<@f.textarea path="extensionItems[0].approvalComments"/>
				</div>
			</div>
		</div>
		<@f.hidden class="approveField" path="extensionItems[0].approved" />
		<@f.hidden class="rejectField" path="extensionItems[0].rejected" />
		<div class="modal-footer request-controls">
			<input id="approveButton" type="submit" class="btn btn-success" value="Grant" />
			<input id="rejectButton" type="submit" class="btn btn-danger" value="Reject" />
			<a href="#" class="close-model btn" data-dismiss="modal">Cancel</a>
		</div>
	</@f.form>
</#escape>