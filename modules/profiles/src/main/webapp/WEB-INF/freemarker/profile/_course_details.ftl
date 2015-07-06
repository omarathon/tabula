<section id="course-details" class="clearfix">
	<hr class="full-width"></hr>

	<!-- drop-down to choose other courses: -->
	<#if (profile.freshStudentCourseDetails)?? && (profile.freshStudentCourseDetails?size > 1)>
		<div class="dropdown">
			<a id="course-dropdown" class="dropdown-toggle pull-right" data-toggle="dropdown" role="button" href="#">
				Other courses
				<b class="caret"></b>
			</a>
			<ul class="dropdown-menu pull-right" aria-labelledby="course-dropdown" role="menu">
				<#list profile.freshStudentCourseDetails as scd>
					<#if scd.scjCode != studentCourseDetails.scjCode>
						<li role="presentation">
							<a href="/profiles/view/course/${scd.urlSafeId}" role="menuitem"
								<#if scd.scjCode == profile.mostSignificantCourse.scjCode>
							   id="most-significant"
								</#if>
									>
							<#-- when choosing from the "Other courses" drop-down, make the most signicant course
							  -- bold to assist with navigating back if you have previously gone into a different course -->
										${(scd.course.code)!}
										<@fmt.course_year_span scd /> ${scd.scjCode}
							</a>
						</li>
					</#if>
				</#list>
			</ul>
		</div>
	</#if>

	<!-- course heading -->
	<h3 class="course-title">
		Course: ${(studentCourseDetails.course.name)!}, ${(studentCourseDetails.course.code)!}
	<@fmt.course_year_span studentCourseDetails />

	</h3>

	<#macro moreDetails>
		<#if studentCourseDetails.award??>
			<tr>
				<th>Intended award</th>
				<td>${(studentCourseDetails.award.name)!}</td>
			</tr>
		</#if>
		<#if !isSelf && studentCourseDetails.statusOnCourse??>
			<tr>
				<th>Status on Course</th>
				<td><@fmt.status_on_course studentCourseDetails /></td>
			</tr>
		</#if>
		<#if studentCourseDetails.beginDate??>
			<tr>
				<th>Start date</th>
				<td>
					<#if studentCourseDetails.beginDate??>
						<@fmt.date date=studentCourseDetails.beginDate includeTime=false />
					</#if>
				</td>
			</tr>
		</#if>
		<#if studentCourseDetails.endDate?? || studentCourseDetails.expectedEndDate??>
			<tr>
				<#if studentCourseDetails.endDate??>
					<th>End date</th>
					<td><@fmt.date date=studentCourseDetails.endDate includeTime=false /></td>
				<#elseif studentCourseDetails.expectedEndDate??>
					<th>Expected end date</th>
					<td><@fmt.date date=studentCourseDetails.expectedEndDate includeTime=false/></td>
				</#if>
			</tr>
		</#if>
		<#if studentCourseDetails.sprCode??>
			<tr>
				<th>Programme route code</th>
				<td>${studentCourseDetails.sprCode}
				</td>
			</tr>
		</#if>
		<#if studentCourseDetails.scjCode??>
			<tr>
				<th>Course join code</th>
				<td>${studentCourseDetails.scjCode}
				</td>
			</tr>
		</#if>
	</#macro>

	<#if studentCourseDetails.ended>
		<div class="data clearfix col1">
			<div class="col1 basic-course-details">
				<table class="profile-or-course-info">
					<tbody>
						<@moreDetails />
					</tbody>
				</table>
			</div>
		</div>

	<#else>

		<!-- basic course details across years -->
		<div class="data clearfix col1">
			<div class="col1 basic-course-details">
				<table class="profile-or-course-info">
					<tbody>
						<#if studentCourseDetails.route??>
							<tr>
								<th>Route</th>
								<td>${(studentCourseDetails.route.name)!} (${(studentCourseDetails.route.code?upper_case)!})
								</td>
							</tr>
						</#if>
						<#if studentCourseDetails.department??>
							<tr>
								<th>Department</th>
								<td>${(studentCourseDetails.department.name)!} (${((studentCourseDetails.department.code)!)?upper_case})
								</td>
							</tr>
						</#if>
						<#if !isSelf && studentCourseDetails.statusOnRoute??>
							<tr>
								<th>Status on Route</th>
								<td><@fmt.status_on_route studentCourseDetails />
								</td>
							</tr>
						</#if>
						<#if studentCourseDetails.route?? && studentCourseDetails.route.degreeType??>
							<tr>
								<th>UG/PG</th>
								<td>${(studentCourseDetails.route.degreeType.toString)!}
								</td>
							</tr>
						</#if>
					</tbody>
				</table>
			</div>
		</div>

		<!-- expandable course details -->
		<div class="col2">
			<details>
				<summary class="collapsible large-chevron">
					<span>More details</span>
				</summary>
				<div class="course-info">
					<table class="profile-or-course-info sb-no-wrapper-table-popout">
						<tbody>
							<@moreDetails />
						</tbody>
					</table>
				</div>
			</details>
		</div>
	</#if>


</section>

<#if features.showAccreditedPriorLearning && studentCourseDetails.accreditedPriorLearning??>
	<div id="accredited-prior-learning">
		<#if can.do("Profiles.Read.AccreditedPriorLearning", studentCourseDetails) && studentCourseDetails.hasAccreditedPriorLearning>
			<h5>Accredited Prior Learning</h5>
				<table>
					<tr>
						<th>Level</th>
						<th>Credit</th>
						<th>Year</th>
						<th>Reason</th>
					</tr>
					<#list studentCourseDetails.accreditedPriorLearning as apl>
						<tr>
							<td>${apl.level.name}</td>
							<td align="center">${apl.cats!}</td>
							<td>${apl.academicYear.toString}</td>
							<td>${apl.reason!}</td>
						</tr>
					</#list>
				</table>
		</#if>
	</div>
</#if>