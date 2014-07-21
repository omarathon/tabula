<#escape x as x?html>
<#import "*/modal_macros.ftl" as modal />
<#import "../attendance_macros.ftl" as attendance_macros />

<#macro pagination currentPage totalResults resultsPerPage extra_classes="">
	<#local totalPages = (totalResults / resultsPerPage)?ceiling />
	<div class="pagination pagination-right ${extra_classes}">
		<ul>
			<#if currentPage lte 1>
				<li class="disabled"><span>&laquo;</span></li>
			<#else>
				<li><a data-page="${currentPage - 1}">&laquo;</a></li>
			</#if>

			<#list 1..totalPages as page>
				<#if page == currentPage>
					<li class="active"><span>${page}</span></li>
				<#else>
					<li><a data-page="${page}">${page}</a></li>
				</#if>
			</#list>

			<#if currentPage gte totalPages>
				<li class="disabled"><span>&raquo;</span></li>
			<#else>
				<li><a data-page="${currentPage + 1}">&raquo;</a></li>
			</#if>
		</ul>
	</div>
</#macro>

<div class="add-student-to-scheme">

	<@spring.bind path="persistanceCommand.staticStudentIds">
		<#if status.error>
			<div class="alert alert-error"><@f.errors path="persistanceCommand.staticStudentIds" cssClass="error"/></div>
		</#if>
	</@spring.bind>

	<#if summaryString?has_content>
		<p><#noescape>${summaryString}</#noescape></p>
	</#if>

	<details class="all-students" data-href="<@routes.manageAddStudentsAllStudents scheme />">
		<summary class="large-chevron collapsible">
			<span class="legend">View all students on this scheme</span>
		</summary>

		<div class="loading" style="display: none;">
			<i class="icon-spinner icon-spin"></i><em> Loading&hellip;</em>
		</div>
	</details>

	<details class="find-students" <#if expandFind>open</#if> data-submitparam="${ManageSchemeMappingParameters.findStudents}">
		<summary class="large-chevron collapsible">
			<span class="legend">Find students
				<small>Select students by route, year of study etc.</small>
			</span>

		</summary>

		<p>Use the filters to add students to this scheme:</p>

		<@f.hidden path="findCommand.page" />
		<@f.hidden path="findCommand.sortOrder" />

		<div class="student-filter btn-group-group well well-small">

			<button type="button" class="clear-all-filters btn btn-link">
				<span class="icon-stack">
					<i class="icon-filter"></i>
					<i class="icon-ban-circle icon-stack-base"></i>
				</span>
			</button>

			<#macro filter path placeholder currentFilter allItems validItems=allItems prefix="">
				<@spring.bind path=path>
					<div class="btn-group<#if currentFilter == placeholder> empty-filter</#if>">
						<a class="btn btn-mini dropdown-toggle" data-toggle="dropdown">
							<span class="filter-short-values" data-placeholder="${placeholder}" data-prefix="${prefix}"><#if currentFilter != placeholder>${prefix}</#if>${currentFilter}</span>
							<span class="caret"></span>
						</a>
						<div class="dropdown-menu filter-list">
							<button type="button" class="close" data-dismiss="dropdown" aria-hidden="true" title="Close">×</button>
							<ul>
								<#if allItems?has_content>
									<#list allItems as item>
										<#local isValid = (allItems?size == validItems?size)!true />
										<#if !isValid>
											<#list validItems as validItem>
												<#if ((validItem.id)!0) == ((item.id)!0)>
													<#local isValid = true />
												</#if>
											</#list>
										</#if>
										<li class="check-list-item" data-natural-sort="${item_index}">
											<label class="checkbox <#if !isValid>disabled</#if>">
												<#nested item isValid/>
											</label>
										</li>
									</#list>
								<#else>
									<li><small class="muted" style="padding-left: 5px;">N/A for this department</small></li>
								</#if>
							</ul>
						</div>
					</div>
				</@spring.bind>
			</#macro>

			<#macro current_filter_value path placeholder><#compress>
				<@spring.bind path=path>
					<#if status.actualValue?has_content>
						<#list status.actualValue as item><#nested item /><#if item_has_next>, </#if></#list>
					<#else>
					${placeholder}
					</#if>
				</@spring.bind>
			</#compress></#macro>

			<#function contains_by_code collection item>
				<#list collection as c>
					<#if c.code == item.code>
						<#return true />
					</#if>
				</#list>
				<#return false />
			</#function>

			<#assign placeholder = "All course types" />
			<#assign currentfilter><@current_filter_value "findCommand.courseTypes" placeholder; courseType>${courseType.code}</@current_filter_value></#assign>
			<@filter "findCommand.courseTypes" placeholder currentfilter findCommand.allCourseTypes; courseType>
				<input type="checkbox" name="${status.expression}" value="${courseType.code}" data-short-value="${courseType.code}" ${contains_by_code(findCommand.courseTypes, courseType)?string('checked','')}>
			${courseType.description}
			</@filter>

			<#assign placeholder = "All routes" />
			<#assign currentfilter><@current_filter_value "findCommand.routes" placeholder; route>${route.code?upper_case}</@current_filter_value></#assign>
			<@filter "findCommand.routes" placeholder currentfilter findCommand.allRoutes findCommand.visibleRoutes; route, isValid>
				<input type="checkbox" name="${status.expression}" value="${route.code}" data-short-value="${route.code?upper_case}" ${contains_by_code(findCommand.routes, route)?string('checked','')} <#if !isValid>disabled</#if>>
				<@fmt.route_name route false />
			</@filter>

			<#assign placeholder = "All attendance" />
			<#assign currentfilter><@current_filter_value "findCommand.modesOfAttendance" placeholder; moa>${moa.shortName?capitalize}</@current_filter_value></#assign>
			<@filter "findCommand.modesOfAttendance" placeholder currentfilter findCommand.allModesOfAttendance; moa>
				<input type="checkbox" name="${status.expression}" value="${moa.code}" data-short-value="${moa.shortName?capitalize}"
				${contains_by_code(findCommand.modesOfAttendance, moa)?string('checked','')}>
			${moa.fullName}
			</@filter>

			<#assign placeholder = "All years" />
			<#assign currentfilter><@current_filter_value "findCommand.yearsOfStudy" placeholder; year>${year}</@current_filter_value></#assign>
			<@filter "findCommand.yearsOfStudy" placeholder currentfilter findCommand.allYearsOfStudy findCommand.allYearsOfStudy "Year "; yearOfStudy>
				<input type="checkbox" name="${status.expression}" value="${yearOfStudy}" data-short-value="${yearOfStudy}"
				${findCommand.yearsOfStudy?seq_contains(yearOfStudy)?string('checked','')}>
			${yearOfStudy}
			</@filter>

			<#assign placeholder = "All statuses" />
			<#assign currentfilter><@current_filter_value "findCommand.sprStatuses" placeholder; sprStatus>${sprStatus.shortName?capitalize}</@current_filter_value></#assign>
			<@filter "findCommand.sprStatuses" placeholder currentfilter findCommand.allSprStatuses; sprStatus>
				<input type="checkbox" name="${status.expression}" value="${sprStatus.code}" data-short-value="${sprStatus.shortName?capitalize}" ${contains_by_code(findCommand.sprStatuses, sprStatus)?string('checked','')}>
			${sprStatus.fullName}
			</@filter>

			<#assign placeholder = "All modules" />
			<#assign currentfilter><@current_filter_value "findCommand.modules" placeholder; module>${module.code?upper_case}</@current_filter_value></#assign>
			<@filter "findCommand.modules" placeholder currentfilter findCommand.allModules; module>
				<input type="checkbox" name="${status.expression}"
					   value="${module.code}"
					   data-short-value="${module.code?upper_case}"
				${contains_by_code(findCommand.modules, module)?string('checked','')}>
				<@fmt.module_name module false />
			</@filter>

			<div class="btn-group">
				<button class="btn btn-mini btn-primary search" type="submit" name="${ManageSchemeMappingParameters.findStudents}">
					<i class="icon-search"></i> Find
				</button>
			</div>

		</div>

		<#if (findCommandResult.membershipItems?size > 0)>
			<div class="pull-right">
				<@pagination
					currentPage=findCommand.page
					resultsPerPage=findCommand.studentsPerPage
					totalResults=findCommand.totalResults
					extra_classes="pagination-small"
				/>
			</div>

			<#assign startIndex = ((findCommand.page - 1) * findCommand.studentsPerPage) />
			<#assign endIndex = startIndex + findCommandResult.membershipItems?size />
			<p>
				Results ${startIndex + 1} - ${endIndex} of ${findCommand.totalResults}
			</p>

			<@attendance_macros.manageStudentTable
				membershipItems=findCommandResult.membershipItems
				doSorting=true
				command=findCommand
				checkboxName="excludeIds"
				onlyShowCheckboxForStatic=true
				showRemoveButton=true
			/>
		</#if>
	</details>

	<details class="manually-added" <#if expandManual>open</#if>>
		<summary class="large-chevron collapsible">
			<span class="legend">Manually add students
				<small>Add a list of students by university ID or username</small>
			</span>
		</summary>

		<p style="margin-bottom: 1em;">
			<input style="margin-right: 8px;" class="btn" type="submit" name="${ManageSchemeMappingParameters.manuallyAddForm}" value="Add students manually" />
			<@fmt.p editMembershipCommandResult.includedStudentIds?size "student" />
			added manually and
			<@fmt.p editMembershipCommandResult.excludedStudentIds?size "student" />
			removed manually
		</p>

		<#if (addUsersResult.missingMembers?size > 0 || addUsersResult.noPermissionMembers?size > 0)>
			<div class="alert alert-warning">
				<#if (addUsersResult.missingMembers?size > 0)>
					The following students could not be added as they were not found:
					<ul>
						<#list addUsersResult.missingMembers as member>
							<li>${member}</li>
						</#list>
					</ul>
				</#if>
				<#if (addUsersResult.noPermissionMembers?size > 0)>
					The following students could not be added as you do not have permission to manage their attendance:
					<ul>
						<#list addUsersResult.noPermissionMembers as member>
							<li>${member.fullName} (${member.universityId})</li>
						</#list>
					</ul>
				</#if>
			</div>
		</#if>

		<@attendance_macros.manageStudentTable
			membershipItems=editMembershipCommandResult.membershipItems
			checkboxName="resetStudentIds"
			checkAll=true
			showResetButton=true
		/>

	</details>

	<input type="hidden" name="returnTo" value="${returnTo}">
</div>

</#escape>