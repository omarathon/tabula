<div class="modal-header">
	<a class="close" data-dismiss="modal" aria-hidden="true">&times;</a>
	<h1><@fmt.profile_name profile /></h1>
</div>
<div class="modal-body">
	<@fmt.member_photo profile "smallthumbnail" false />

	<div class="data clearfix">
		<div class="col1">
			<table class="profile-or-course-info">
				<tbody>
					<tr>
						<th>Official name</th>
						<td>${profile.officialName}</td>
					</tr>

					<#if profile.gender??>
						<tr>
							<th>Gender</th>
							<td>${profile.gender.description}</td>
						</tr>
					</#if>

					<#if profile.email??>
						<tr>
							<th>Warwick email</th>
							<td><a href="mailto:${profile.email}">${profile.email}</a></td>
						</tr>
					</#if>

					<#if profile.universityId??>
						<tr>
							<th>University number</th>
							<td>${profile.universityId}</td>
						</tr>
					</#if>

					<#if profile.userId??>
						<tr>
							<th>IT code</th>
							<td>${profile.userId}</td>
						</tr>
					</#if>
				</tbody>
			</table>
		</div>
	</div>

	<!-- basic course details -->
	<div class="data clearfix">
		<div class="col1 basic-course-details">
			<table class="profile-or-course-info subset-course">
				<tbody>
				<#if (studentCourseDetails.route)??>
				<tr>
					<th>Route</th>
					<td>${(studentCourseDetails.route.name)!} (${(studentCourseDetails.route.code?upper_case)!})
					</td>
				</tr>
				</#if>
				<#if (studentCourseDetails.department)??>
				<tr>
					<th>Department</th>
					<td>${(studentCourseDetails.department.name)!} (${((studentCourseDetails.department.code)!)?upper_case})
					</td>
				</tr>
				</#if>
				<tr>
					<th>Status on route</th>
					<td><@fmt.status_on_route studentCourseDetails />
					</td>
				</tr>
				<tr>
					<th>Attendance</th>
					<td>${(studentCourseDetails.latestStudentCourseYearDetails.modeOfAttendance.fullNameAliased)!}
					</td>
				</tr>
				<tr>
					<th>UG/PG</th>
					<td>${(studentCourseDetails.route.degreeType.toString)!}
					</td>
				</tr>
				<tr>
					<th>Year of study</th>
					<td>${(studentCourseDetails.latestStudentCourseYearDetails.yearOfStudy)!}
					</td>
				</tr>
				</tbody>
			</table>
		</div>
	</div>
</div>
