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
							<i class="icon-list-alt icon-fixed-width"></i> Department settings
						</@fmt.permission_button>
					</li>

					<li class="divider"></li>

					<li<#if !modules?has_content> class="disabled"</#if>>
						<#assign import_url><@routes.import_groups_for_year department adminCommand.academicYear /></#assign>
						<@fmt.permission_button
						permission='SmallGroups.ImportFromExternalSystem'
						scope=department
						action_descr='import small groups from Syllabus+'
						href=import_url>
							<i class="icon-cloud-download"></i> Create small groups from Syllabus+
						</@fmt.permission_button>
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

					<li class="divider"></li>

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

					<li class="divider"></li>

					<li<#if !hasGroupAttendance> class="disabled"</#if>>
						<a href="<@routes.departmentAttendance department />"><i class="icon-group icon-fixed-width"></i> View attendance</a>
					</li>
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

	<div class="form-inline creation-form" style="margin-bottom: 10px;">
		<label for="module-picker">For this module:</label>
		<select id="module-picker" class="input-xlarge" placeholder="Start typing a module code or name&hellip;" data-provide="typeahead">
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

			var manageButtonState = function(value) {
				if (value) {
					$button.removeClass('disabled').addClass('btn-primary');
				} else {
					$button.addClass('disabled').removeClass('btn-primary');
				}
			};
			manageButtonState($picker.find(':selected').val());

			$picker.on('change', function() {
				var value = $(this).find(':selected').val();
				manageButtonState(value);
			});
			$button.on('click', function() {
				if ($button.is('.disabled')) return;

				var moduleCode = $picker.find(':selected').val();
				if (moduleCode) {
					window.location.href = url.replace('__MODULE_CODE__', moduleCode);
				}
			});
		})(jQuery);
	</script>

	<#if hasGroups>
		<#-- Filtering -->
		<div class="fix-area">
			<div class="fix-header pad-when-fixed">
				<@f.form commandName="adminCommand" action="${info.requestedUri.path}" method="GET" cssClass="form-inline">
					<@f.errors cssClass="error form-errors" />

					<div class="small-groups-filter btn-group-group well well-small">
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

						<#function contains_by_filter_name collection item>
							<#list collection as c>
								<#if c.name == item.name>
									<#return true />
								</#if>
							</#list>
							<#return false />
						</#function>

						<#assign placeholder = "All modules" />
						<#assign currentfilter><@current_filter_value "moduleFilters" placeholder; f>${f.module.code?upper_case}</@current_filter_value></#assign>
						<@filter "moduleFilters" placeholder currentfilter allModuleFilters; f>
							<input type="checkbox" name="${status.expression}"
								   value="${f.name}"
								   data-short-value="${f.description}"
							${contains_by_filter_name(adminCommand.moduleFilters, f)?string('checked','')}>
							${f.description}
						</@filter>

						<#assign placeholder = "All statuses" />
						<#assign currentfilter><@current_filter_value "statusFilters" placeholder; f>${f.description}</@current_filter_value></#assign>
						<@filter "statusFilters" placeholder currentfilter allStatusFilters; f>
							<input type="checkbox" name="${status.expression}"
								   value="${f.name}"
								   data-short-value="${f.description}"
							${contains_by_filter_name(adminCommand.statusFilters, f)?string('checked','')}>
							${f.description}
						</@filter>

						<#assign placeholder = "All allocation methods" />
						<#assign currentfilter><@current_filter_value "allocationMethodFilters" placeholder; f>${f.description}</@current_filter_value></#assign>
						<@filter "allocationMethodFilters" placeholder currentfilter allAllocationFilters; f>
							<input type="checkbox" name="${status.expression}"
								   value="${f.name}"
								   data-short-value="${f.description}"
							${contains_by_filter_name(adminCommand.allocationMethodFilters, f)?string('checked','')}>
							${f.description}
						</@filter>

						<#assign placeholder = "All terms" />
						<#assign currentfilter><@current_filter_value "termFilters" placeholder; f>${f.description}</@current_filter_value></#assign>
						<@filter "termFilters" placeholder currentfilter allTermFilters; f>
							<input type="checkbox" name="${status.expression}"
								   value="${f.name}"
								   data-short-value="${f.description}"
							${contains_by_filter_name(adminCommand.termFilters, f)?string('checked','')}>
							${f.description}
						</@filter>
					</@f.form>
				</div>
			</div>

			<div id="filter-results">
				<#-- This is the big list of sets -->
				<@components.sets_info sets sets?size lte 5 />
			</div>
		</div>

		<div id="modal-container" class="modal fade"></div>

		<script type="text/javascript">
			jQuery(function($) {
				$('.fix-area').fixHeaderFooter();

				var prependClearLink = function($list) {
					if (!$list.find('input:checked').length) {
						$list.find('.clear-this-filter').remove();
					} else {
						if (!$list.find('.clear-this-filter').length) {
							$list.find('> ul').prepend(
									$('<li />').addClass('clear-this-filter')
											.append(
											$('<button />').attr('type', 'button')
													.addClass('btn btn-link')
													.html('<i class="icon-ban-circle"></i> Clear selected items')
													.on('click', function(e) {
														$list.find('input:checked').each(function() {
															var $checkbox = $(this);
															$checkbox.prop('checked', false);
															updateFilter($checkbox);
														});

														doRequest($list.closest('form'));
													})
									)
											.append($('<hr />'))
							);
						}
					}
				};

				var updateFilter = function($el) {
					// Update the filter content
					var $list = $el.closest('ul');
					var shortValues = $list.find(':checked').map(function() { return $(this).data('short-value'); }).get();
					var $fsv = $el.closest('.btn-group').find('.filter-short-values');
					if (shortValues.length) {
						$el.closest('.btn-group').removeClass('empty-filter');
						$fsv.html($fsv.data("prefix") + shortValues.join(', '));
					} else {
						$el.closest('.btn-group').addClass('empty-filter');
						$fsv.html($fsv.data('placeholder'));
					}

					updateClearAllButton($el);
				};

				var updateClearAllButton = function($el) {
					var $filterList = $el.closest(".student-filter");

					if ($filterList.find(".empty-filter").length == $filterList.find(".btn-group").length) {
						$('.clear-all-filters').attr("disabled", "disabled");
					} else {
						$('.clear-all-filters').removeAttr("disabled");
					}
				}

				var doRequest = function($form, preventPageReset) {
					if (typeof history.pushState !== 'undefined')
						history.pushState(null, null, $form.attr('action') + '?' + $form.serialize());

					if ($form.data('request')) {
						$form.data('request').abort();
						$form.data('request', null);
					}

					if (!preventPageReset) {
						$form.find('input[name="page"]').val('1');
					}

					$('#filter-results').addClass('loading');
					$form.data('request', $.post($form.attr('action'), $form.serialize(), function(data) {
						$('#filter-results').html(data);

						$form.data('request', null);
						$('#filter-results').removeClass('loading');

						$('.use-wide-popover').tabulaPopover({
							trigger: 'click',
							container: '#container',
							template: '<div class="popover wide"><div class="arrow"></div><div class="popover-inner"><button type="button" class="close" aria-hidden="true">&#215;</button><h3 class="popover-title"></h3><div class="popover-content"><p></p></div></div></div>'
						});

						$('.use-tooltip').tooltip();

						// callback for hooking in local changes to results
						$(document).trigger("tabula.filterResultsChanged");
					}));
				};
				window.doRequest = doRequest;

				$('#adminCommand input').on('change', function(e) {
					// Load the new results
					var $checkbox = $(this);
					var $form = $checkbox.closest('form');

					doRequest($form);
					updateFilter($checkbox);
				});

				// Re-order elements inside the dropdown when opened
				$('.filter-list').closest('.btn-group').find('.dropdown-toggle').on('click.dropdown.data-api', function(e) {
					var $this = $(this);
					if (!$this.closest('.btn-group').hasClass('open')) {
						// Re-order before it's opened!
						var $list = $this.closest('.btn-group').find('.filter-list');
						var items = $list.find('li.check-list-item').get();

						items.sort(function(a, b) {
							var aChecked = $(a).find('input').is(':checked');
							var bChecked = $(b).find('input').is(':checked');

							if (aChecked && !bChecked) return -1;
							else if (!aChecked && bChecked) return 1;
							else return $(a).data('natural-sort') - $(b).data('natural-sort');
						});

						$.each(items, function(item, el) {
							$list.find('> ul').append(el);
						});

						prependClearLink($list);
					}
				});

				$('.clear-all-filters').on('click', function() {
					$('.filter-list').each(function() {
						var $list = $(this);

						$list.find('input:checked').each(function() {
							var $checkbox = $(this);
							$checkbox.prop('checked', false);
							updateFilter($checkbox);
						});

						prependClearLink($list);
					});

					doRequest($('#adminCommand'));
				});
			});
		</script>
	</#if>
</#escape>
