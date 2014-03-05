<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#assign extension=command.extension />

<#escape x as x?html>
	<div class="content extension-detail">
		<@f.form method="post" enctype="multipart/form-data" action="${url('/coursework/admin/module/${module.code}/assignments/${assignment.id}/extensions/detail/${universityId}')}" commandName="modifyExtensionCommand" cssClass="form-horizontal double-submit-protection">
			<@f.input type="hidden" path="universityId" value="${universityId}" />

			<div>
				<#if extension.requestedExpiryDate??>
					<@fmt.date date=extension.requestedExpiryDate at=true/>
				</#if>
			</div>

			<div class="extension-details">
				<#if extension.reason??>
					<div class="extensionReason">
						<div class="control-group">
							<label><h4>Reason for extension request</h4></label>
							<div class="controls">
							${extension.reason}
							</div>
						</div>
					</div>
				</#if>

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
								${(studentCourseDetails.award.name)!} (${(studentCourseDetails.route.degreeType.toString)!})
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

				<#if features.disabilityRenderingInExtensions && extension.disabilityAdjustment && student?? && can.do("Profiles.Read.Disability", student)>
					<p class="alert alert-warning">Student has requested their ${student.disability.definition} be taken into consideration.</p>
				</#if>
			</div>

			<div class="control-group">
				<@form.label path="expiryDate">New submission deadline</@form.label>
				<div class="controls">
					<@f.input id="picker0" path="expiryDate" class="date-time-picker" />
				</div>
			</div>
			<div class="control-group">
				<@form.label path="reviewerComments">Comments</@form.label>
				<div class="controls">
					<@f.textarea path="reviewerComments" />
				</div>
			</div>
			<input type="hidden" name="action" class="action" />
			<div class="submit-buttons">
				<input class="btn btn-primary" type="submit" value="Save">
				<a class="btn discard-changes" href="">Discard</a>
			</div>
		</@f.form>
	</div>
</#escape>
