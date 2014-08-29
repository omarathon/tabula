<#import "*/group_components.ftl" as components />
<#escape x as x?html>
	<div class="btn-toolbar dept-toolbar">
		<#if !modules?has_content && department.children?has_content>
			<a class="btn btn-medium dropdown-toggle disabled use-tooltip" title="This department doesn't directly contain any modules. Check subdepartments.">
				<i class="icon-wrench"></i>
				Manage
			</a>
		<#else>
			<div class="btn-group dept-settings">
				<a class="btn btn-medium dropdown-toggle" data-toggle="dropdown" href="#">
					<i class="icon-wrench"></i>
					Manage
					<span class="caret"></span>
				</a>
				<ul class="dropdown-menu pull-right">
					<li>
						<#assign settings_url><@routes.displaysettings department />?returnTo=${(info.requestedUri!"")?url}</#assign>
						<@fmt.permission_button
							permission='Department.ManageDisplaySettings'
							scope=department
							action_descr='manage department settings'
							href=settings_url>
							<i class="icon-list-alt icon-fixed-width"></i> Settings
						</@fmt.permission_button>
					</li>

					<#if features.smallGroupTeachingStudentSignUp>
						<li ${hasOpenableGroupsets?string(''," class='disabled use-tooltip' title='There are no self-signup groups to open' ")}>
							<#assign open_url><@routes.batchopen department /></#assign>
							<@fmt.permission_button
								permission='SmallGroups.Update'
								scope=department
								action_descr='open small groups'
								href=open_url>
								<i class="icon-unlock-alt icon-fixed-width"></i> Open
							</@fmt.permission_button>
						</li>
						<li ${hasCloseableGroupsets?string(''," class='disabled use-tooltip' title='There are no self-signup groups to close' ")}>
							<#assign close_url><@routes.batchclose department /></#assign>
							<@fmt.permission_button
								permission='SmallGroups.Update'
								scope=department
								action_descr='close small groups'
								href=close_url>
								<i class="icon-lock icon-fixed-width"></i> Close
							</@fmt.permission_button>
						</li>
					</#if>
					<li ${hasUnreleasedGroupsets?string(''," class='disabled use-tooltip' title='All modules already notified' ")} >
						<#assign notify_url><@routes.batchnotify department /></#assign>
						<@fmt.permission_button
							permission='SmallGroups.Update'
							scope=department
							action_descr='notify students and staff'
							href=notify_url>
							<i class="icon-envelope-alt icon-fixed-width"></i> Notify
						</@fmt.permission_button>
					</li>
					<li<#if !hasGroupAttendance> class="disabled"</#if>>
						<a href="<@routes.departmentAttendance department />"><i class="icon-group icon-fixed-width"></i> Attendance</a>
					</li>

					<#if features.smallGroupCrossModules>
						<li>
							<#assign cross_module_url><@routes.crossmodulegroups department /></#assign>
							<@fmt.permission_button
							permission='SmallGroups.Create'
							scope=department
							action_descr='create reusable small group allocations'
							href=cross_module_url>
								<i class="icon-group icon-fixed-width"></i> Reusable small groups
							</@fmt.permission_button>
						</li>
					</#if>
				</ul>
			</div>
		</#if>

		<div class="btn-group dept-settings">
			<a class="btn btn-medium dropdown-toggle" data-toggle="dropdown" href="#">
				<i class="icon-calendar"></i>
				${adminCommand.academicYear.label}
				<span class="caret"></span>
			</a>
			<ul class="dropdown-menu pull-right">
				<#list academicYears as year>
					<li>
						<a href="<@routes.departmenthome department year />">
							<#if year.startYear == adminCommand.academicYear.startYear>
								<strong>${year.label}</strong>
							<#else>
								${year.label}
							</#if>
						</a>
					</li>
				</#list>
			</ul>
		</div>
	</div>

	<#macro deptheaderroutemacro department>
		<@routes.departmenthome department adminCommand.academicYear />
	</#macro>
	<#assign deptheaderroute = deptheaderroutemacro in routes />

	<@fmt.deptheader "" "" department routes "deptheaderroute" "with-settings" />

	<#if !hasGroups>
		<p class="alert alert-info empty-hint"><i class="icon-lightbulb"></i> There are no small groups set up for ${adminCommand.academicYear.label} in ${department.name}.</p>
	</#if>

	<h2>Create groups</h2>

	<div class="form-inline creation-form">
		<label for="module-picker">For this module:</label>
		<select id="module-picker">
			<option value=""></option>
			<#list modules as module>
				<option value="${module.code}"><@fmt.module_name module false /></option>
			</#list>
		</select>
		<button type="button" class="btn disabled">Create</button>
	</div>

	<script type="text/javascript">
		(function($) {
			<#assign template_module={"code":"__MODULE_CODE__"} />
			var url = '<@routes.createset template_module />?academicYear=${adminCommand.academicYear.startYear?c}';

			var $picker = $('.creation-form #module-picker');
			var $button = $('.creation-form button');

			$picker.on('change', function() {
				var value = $(this).find(':selected').val();
				if (value) {
					$button.removeClass('disabled');
				} else {
					$button.addClass('disabled');
				}
			});
			$button.on('click', function() {
				var moduleCode = $picker.find(':selected').val();
				if (moduleCode) {
					window.location.href = url.replace('__MODULE_CODE__', moduleCode);
				}
			});
		})(jQuery);
	</script>

	<#-- This is the big list of sets -->
	<@components.sets_info sets sets?size lte 5 />

	<div id="modal-container" class="modal fade"></div>
</#escape>
