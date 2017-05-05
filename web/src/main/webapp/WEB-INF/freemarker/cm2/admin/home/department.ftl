<#import "*/coursework_components.ftl" as components />
<#import "../_filters.ftl" as filters />
<#escape x as x?html>

	<div class="btn-toolbar dept-toolbar">
		<div class="btn-group">
			<a class="btn btn-link dropdown-toggle" data-toggle="dropdown">
				Assignments
				<span class="caret"></span>
			</a>
			<ul class="dropdown-menu">
				<li>
					<#assign setup_Url><@routes.cm2.create_sitsassignments department /></#assign>
					<@fmt.permission_button
						permission='Assignment.ImportFromExternalSystem'
						scope=department
						action_descr='setup assignments from SITS'
						href=setup_Url>
							Create assignments from SITS
					</@fmt.permission_button>
				</li>
				<li>
					<#assign copy_url><@routes.cm2.copy_assignments_previous department /></#assign>
					<@fmt.permission_button
						permission='Assignment.Create'
						scope=department
						action_descr='copy existing assignments'
						href=copy_url>
							Create assignments from previous
					</@fmt.permission_button>
				</li>
			</ul>
		</div>

		<#assign extensions_url><@routes.cm2.filterExtensions />?departments=${department.code}</#assign>
		<@fmt.permission_button
			permission='Extension.Read'
			scope=department
			action_descr='manage extensions'
			href=extensions_url
			classes='btn btn-link'>
				Extension requests
		</@fmt.permission_button>

		<#if features.markingWorkflows>
			<#assign markingflow_url><@routes.cm2.reusableWorkflowsHome department academicYear /></#assign>
			<@fmt.permission_button
				permission='MarkingWorkflow.Read'
				scope=department
				action_descr='manage marking workflows'
				href=markingflow_url
				classes='btn btn-link'>
					Marking workflows
			</@fmt.permission_button>
		</#if>

		<div class="btn-group">
			<a class="btn btn-link dropdown-toggle" data-toggle="dropdown">
				Feedback
				<span class="caret"></span>
			</a>
			<ul class="dropdown-menu">
				<#if features.feedbackTemplates>
					<li>
						<#assign feedback_url><@routes.cm2.feedbacktemplates department /></#assign>
						<@fmt.permission_button
							permission='FeedbackTemplate.Manage'
							scope=department
							action_descr='create feedback template'
							href=feedback_url>
								Feedback templates
						</@fmt.permission_button>
					</li>
				</#if>

				<li>
					<#assign feedbackrep_url><@routes.cm2.feedbackreport department /></#assign>
					<@fmt.permission_button
						permission='Department.DownloadFeedbackReport'
						scope=department
						action_descr='generate a feedback report'
						href=feedbackrep_url>
							Feedback reports
					</@fmt.permission_button>
				</li>
			</ul>
		</div>

		<div class="btn-group">
			<a class="btn btn-link dropdown-toggle" data-toggle="dropdown">
				Settings
				<span class="caret"></span>
			</a>
			<ul class="dropdown-menu pull-right">
				<li>
					<#assign settings_url><@routes.admin.displaysettings department />?returnTo=${(info.requestedUri!"")?url}</#assign>
					<@fmt.permission_button
						permission='Department.ManageDisplaySettings'
						scope=department
						action_descr='manage department settings'
						href=settings_url>
							Department settings
					</@fmt.permission_button>
				</li>
				<li>
					<#assign settings_url><@routes.admin.notificationsettings department />?returnTo=${(info.requestedUri!"")?url}</#assign>
					<@fmt.permission_button
						permission='Department.ManageNotificationSettings'
						scope=department
						action_descr='manage department notification settings'
						href=settings_url>
							Notification settings
					</@fmt.permission_button>
				</li>
				<li>
					<#assign extensions_url><@routes.cm2.extensionSettings department /></#assign>
					<@fmt.permission_button
						permission='Department.ManageExtensionSettings'
						scope=department
						action_descr='manage extension settings'
						href=extensions_url>
							Extension settings
					</@fmt.permission_button>
				</li>
			</ul>
		</div>
	</div>

	<#function route_function dept>
		<#local result><@routes.cm2.departmenthome dept academicYear /></#local>
		<#return result />
	</#function>
	<@fmt.id7_deptheader "Assignments" route_function "in" />

	<#-- Filtering -->
	<div class="fix-area">
		<div class="fix-header pad-when-fixed">
			<div class="filters admin-assignment-filters btn-group-group well well-sm" data-lazy="true">
				<@f.form commandName="command" action="${info.requestedUri.path}" method="GET" cssClass="form-inline">
					<@f.errors cssClass="error form-errors" />

					<button type="button" class="clear-all-filters btn btn-link">
						<span class="fa-stack">
							<i class="fa fa-filter fa-stack-1x"></i>
							<i class="fa fa-ban fa-stack-2x"></i>
						</span>
					</button>

					<#assign placeholder = "All modules" />
					<#assign modulesCustomPicker>
						<@bs3form.checkbox path="showEmptyModules">
							<@f.checkbox path="showEmptyModules" />
							Show modules with no filtered assignments
						</@bs3form.checkbox>

						<div class="module-search input-group">
							<input class="module-search-query module-picker module prevent-reload form-control" type="text" value="" placeholder="Search for a module" data-department="${department.code}" data-name="moduleFilters" data-wrap="true" />
							<span class="input-group-addon"><i class="fa fa-search"></i></span>
						</div>
					</#assign>
					<#assign currentfilter><@filters.current_filter_value "moduleFilters" placeholder; f>${f.module.code?upper_case}</@filters.current_filter_value></#assign>
					<@filters.filter name="module" path="command.moduleFilters" placeholder=placeholder currentFilter=currentfilter allItems=allModuleFilters customPicker=modulesCustomPicker; f>
						<input type="checkbox" name="${status.expression}"
									 value="${f.name}"
									 data-short-value="${f.description}"
						${filters.contains_by_filter_name(command.moduleFilters, f)?string('checked','')}>
						${f.description}
					</@filters.filter>

					<#assign placeholder = "All workflows" />
					<#assign currentfilter><@filters.current_filter_value "workflowTypeFilters" placeholder; f>${f.description}</@filters.current_filter_value></#assign>
					<@filters.filter "workflowType" "command.workflowTypeFilters" placeholder currentfilter allWorkflowTypeFilters; f>
						<input type="checkbox" name="${status.expression}"
									 value="${f.name}"
									 data-short-value="${f.description}"
						${filters.contains_by_filter_name(command.workflowTypeFilters, f)?string('checked','')}>
						${f.description}
					</@filters.filter>

					<#assign placeholder = "All assignment statuses" />
					<#assign currentfilter><@filters.current_filter_value "statusFilters" placeholder; f>${f.description}</@filters.current_filter_value></#assign>
					<@filters.filter "status" "command.statusFilters" placeholder currentfilter allStatusFilters; f>
						<input type="checkbox" name="${status.expression}"
									 value="${f.name}"
									 data-short-value="${f.description}"
						${filters.contains_by_filter_name(command.statusFilters, f)?string('checked','')}>
						${f.description}
					</@filters.filter>

					<@bs3form.labelled_form_group path="dueDateFilter.from" labelText="Assignment due date from">
						<div class="input-group">
							<@f.input type="text" path="dueDateFilter.from" cssClass="form-control date-picker input-sm" />
							<span class="input-group-addon"><i class="fa fa-calendar"></i></span>
						</div>
					</@bs3form.labelled_form_group>

					<@bs3form.labelled_form_group path="dueDateFilter.to" labelText="to">
						<div class="input-group">
							<@f.input type="text" path="dueDateFilter.to" cssClass="form-control date-picker input-sm" />
							<span class="input-group-addon"><i class="fa fa-calendar"></i></span>
						</div>
					</@bs3form.labelled_form_group>
				</@f.form>
			</div>
		</div>

		<div class="filter-results admin-assignment-list">
			<i class="fa fa-spinner fa-spin"></i> Loading&hellip;
		</div>
	</div>

</#escape>