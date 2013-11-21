<#escape x as x?html>
<#import "../attendance_variables.ftl" as attendance_variables />
<#import "../attendance_macros.ftl" as attendance_macros />

<article class="profile">
	<section id="personal-details">
		<@fmt.member_photo student />
		<header>
			<h1><@fmt.profile_name student /></h1>
			<h5><@fmt.profile_description student /></h5>
		</header>

		<div class="data clearfix">
			<div class="col1">
				<table class="profile-info">
					<tbody>
					<tr>
						<th>Official name</th>
						<td>${student.officialName}</td>
					</tr>

					<tr>
						<th>Preferred name</th>
						<td>${student.fullName}</td>
					</tr>

						<#if student.gender??>
						<tr>
							<th>Gender</th>
							<td>${student.gender.description}</td>
						</tr>
						</#if>

						<#if student.nationality??>
						<tr>
							<th>Nationality</th>
							<td><@fmt.nationality student.nationality?default('Unknown') /></td>
						</tr>
						</#if>

						<#if student.dateOfBirth??>
						<tr>
							<th>Date of birth</th>
							<td><@warwick.formatDate value=student.dateOfBirth.toDateTimeAtStartOfDay() pattern="dd/MM/yyyy" /></td>
						</tr>
						</#if>

						<#if student.student && student.termtimeAddress??>
						<tr class="address">
							<th>Term-time address</th>
							<td><@student_macros.address student.termtimeAddress /></td>
						</tr>
						</#if>

						<#if student.student && student.nextOfKins?? && student.nextOfKins?size gt 0>
						<tr>
							<th>Emergency contacts</th>
							<td>
								<#list student.nextOfKins as kin>
									<div>
										<#if kin.firstName?? && kin.lastName??>${kin.fullName}</#if>
										<#if kin.relationship??>(${kin.relationship})</#if>
									</div>
								</#list>
							</td>
						</tr>
						</#if>
					</tbody>
				</table>

				<br class="clearfix">
			</div>

			<div class="col2">
				<table class="profile-info">
					<tbody>
						<#if student.email??>
						<tr>
							<th>Warwick email</th>
							<td><i class="icon-envelope-alt"></i> <a href="mailto:${student.email}">${student.email}</a></td>
						</tr>
						</#if>

						<#if student.homeEmail??>
						<tr>
							<th>Alternative email</th>
							<td><i class="icon-envelope-alt"></i> <a href="mailto:${student.homeEmail}">${student.homeEmail}</a></td>
						</tr>
						</#if>

						<#if student.phoneNumber??>
						<tr>
							<th>Phone number</th>
							<td>${phoneNumberFormatter(student.phoneNumber)}</td>
						</tr>
						</#if>

						<#if student.mobileNumber??>
						<tr>
							<th>Mobile phone</th>
							<td>${phoneNumberFormatter(student.mobileNumber)}</td>
						</tr>
						</#if>

						<#if student.universityId??>
						<tr>
							<th>University number</th>
							<td>${student.universityId}</td>
						</tr>
						</#if>

						<#if student.userId??>
						<tr>
							<th>IT code</th>
							<td>${student.userId}</td>
						</tr>
						</#if>

						<#if student.student && student.homeAddress??>
						<tr class="address">
							<th>Home address</th>
							<td><@student_macros.address student.homeAddress /></td>
						</tr>
						</#if>
					</tbody>
				</table>
			</div>
		</div>

	</section>
</article>

<#assign thisPath><@routes.agentStudentView student relationshipType command.academicYear /></#assign>
<@attendance_macros.academicYearSwitcher thisPath command.academicYear command.thisAcademicYear />

<#macro pointsInATerm term>
	<div class="striped-section end-floats">
		<h2 class="section-title">${term}</h2>
		<div class="striped-section-contents">
			<#list pointsByTerm[term] as pointAndCheckpoint>
				<div class="item-info row-fluid point">
					<div class="span10">
						${pointAndCheckpoint._1().name} (<@fmt.monitoringPointFormat pointAndCheckpoint._1() />)
					</div>
					<div class="span2">
						<#if pointAndCheckpoint._2() == "attended">
							<span class="label label-success">Attended</span>
						<#elseif pointAndCheckpoint._2() == "authorised">
							<span class="label label-info" title="Missed (authorised)">Missed</span>
						<#elseif pointAndCheckpoint._2() == "unauthorised">
							<span class="label label-important" title="Missed (unauthorised)">Missed</span>
						<#elseif pointAndCheckpoint._2() == "late">
							<span class="label label-warning" title="Unrecorded">Unrecorded</span>
						</#if>
					</div>
				</div>
			</#list>
		</div>
	</div>
</#macro>



<div class="monitoring-points">
	<#if pointsByTerm?keys?size == 0>
		<p><em>No monitoring points found for this academic year.</em></p>
	<#else>
		<a class="btn btn-primary" href="<@routes.agentStudentRecord student relationshipType command.academicYear thisPath/>">Record attendance</a>
		<#list attendance_variables.monitoringPointTermNames as term>
			<#if pointsByTerm[term]??>
				<@pointsInATerm term />
			</#if>
		</#list>
	</#if>
</div>

</#escape>