<div class="persist-area">
	<div class="persist-header">
		<@f.form commandName="command" action="${thisPath}" method="GET" cssClass="form-inline">
			<@f.errors cssClass="error form-errors" />
			<@f.hidden path="academicYear"/>
			<#if command.hasBeenFiltered??>
				<@f.hidden path="hasBeenFiltered" value="true"/>
			</#if>
			<#if command.page??>
				<@f.hidden path="page" />
			</#if>
			<#if command.studentsPerPage??>
				<@f.hidden path="studentsPerPage" />
			</#if>
			<#if command.sortOrder??>
				<@f.hidden path="sortOrder" />
			</#if>

			<div class="btn-group-group">
				<i class="icon-filter"></i>

				<#macro filter path name currentFilter allItems validItems=allItems>
					<@spring.bind path=path>
						<span class="very-subtle filter-type">${name}:</span>
						<div class="btn-group">
							<a class="btn btn-mini dropdown-toggle" data-toggle="dropdown">
								<span class="filter-short-values">${currentFilter}</span>
								<span class="caret"></span>
							</a>
							<div class="dropdown-menu filter-list">
								<button type="button" class="close" data-dismiss="dropdown" aria-hidden="true" title="Close">×</button>
								<ul>
									<#list allItems as item>
										<#assign isValid = (allItems?size == validItems?size)!true />
										<#if !isValid>
											<#list validItems as validItem>
												<#if ((validItem.id)!0) == ((item.id)!0)>
													<#assign isValid = true />
												</#if>
											</#list>
										</#if>
										<li class="check-list-item" data-natural-sort="${item_index}">
											<label class="checkbox <#if !isValid>disabled</#if>">
												<#nested item isValid/>
											</label>
										</li>
									</#list>
								</ul>
							</div>
						</div>
					</@spring.bind>
				</#macro>

				<#macro current_filter_value path><#compress>
					<@spring.bind path=path>
						<#if status.actualValue?has_content>
							<#list status.actualValue as item><#nested item /><#if item_has_next>, </#if></#list>
						<#else>
							Any
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

				<#assign current_course_types_filter><@current_filter_value "courseTypes"; courseType>${courseType.code}</@current_filter_value></#assign>
				<@filter "courseTypes" "Course type" current_course_types_filter command.allCourseTypes; courseType>
					<input type="checkbox" name="${status.expression}" value="${courseType.code}" data-short-value="${courseType.code}" ${contains_by_code(command.courseTypes, courseType)?string('checked','')}>
				${courseType.description}
				</@filter>

				<#assign current_routes_filter><@current_filter_value "routes"; route>${route.code?upper_case}</@current_filter_value></#assign>
				<@filter "routes" "Route" current_routes_filter command.allRoutes command.visibleRoutes; route, isValid>
					<input type="checkbox" name="${status.expression}" value="${route.code}" data-short-value="${route.code?upper_case}" ${contains_by_code(command.routes, route)?string('checked','')} <#if !isValid>disabled</#if>>
					<@fmt.route_name route false />
				</@filter>

				<#assign current_moa_filter><@current_filter_value "modesOfAttendance"; moa>${moa.shortName?capitalize}</@current_filter_value></#assign>
				<@filter "modesOfAttendance" "Attendance" current_moa_filter command.allModesOfAttendance; moa>
					<input type="checkbox" name="${status.expression}" value="${moa.code}" data-short-value="${moa.shortName?capitalize}" ${contains_by_code(command.modesOfAttendance, moa)?string('checked','')}>
				${moa.fullName}
				</@filter>

				<#assign current_years_filter><@current_filter_value "yearsOfStudy"; year>${year}</@current_filter_value></#assign>
				<@filter "yearsOfStudy" "Year of study" current_years_filter command.allYearsOfStudy; yearOfStudy>
					<input type="checkbox" name="${status.expression}" value="${yearOfStudy}" data-short-value="${yearOfStudy}" ${command.yearsOfStudy?seq_contains(yearOfStudy)?string('checked','')}>
				${yearOfStudy}
				</@filter>

				<#assign current_statuses_filter><@current_filter_value "sprStatuses"; sprStatus>${sprStatus.shortName?capitalize}</@current_filter_value></#assign>
				<@filter "sprStatuses" "Status" current_statuses_filter command.allSprStatuses; sprStatus>
					<input type="checkbox" name="${status.expression}" value="${sprStatus.code}" data-short-value="${sprStatus.shortName?capitalize}" ${contains_by_code(command.sprStatuses, sprStatus)?string('checked','')}>
				${sprStatus.fullName}
				</@filter>

				<#assign current_modules_filter><@current_filter_value "modules"; module>${module.code?upper_case}</@current_filter_value></#assign>
				<@filter "modules" "Module" current_modules_filter command.allModules; module>
					<input type="checkbox" name="${status.expression}" value="${module.code}" data-short-value="${module.code?upper_case}" ${contains_by_code(command.modules, module)?string('checked','')}>
					<@fmt.module_name module false />
				</@filter>

				<button type="button" class="btn btn-link clear-filter"><i class="icon-remove"></i> Clear filter</button>
			</div>
		</@f.form>
	</div>

	<div id="filter-results">
		<#include filter_results_path />
	</div>
</div>

<script type="text/javascript">
	jQuery(function($) {
		$('.persist-area').fixHeaderFooter();

		var prependClearLink = function($list) {
			if (!$list.find('input:checked').length) {
				$list.find('.clear-this-filter').remove();
			} else {
				if (!$list.find('.clear-this-filter').length) {
					$list.find('> ul').prepend(
							$('<li />').addClass('clear-this-filter')
									.append(
											$('<button />').attr('type', 'button').addClass('btn btn-link')
													.html('<i class="icon-remove"></i> Clear selected items')
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
			if (shortValues.length) {
				$el.closest('.btn-group').removeClass('empty-filter');
				$el.closest('.btn-group').find('.filter-short-values').html(shortValues.join(', '));
			} else {
				$el.closest('.btn-group').addClass('empty-filter');
				$el.closest('.btn-group').find('.filter-short-values').html('Any');
			}
		};

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

				$('.scrollable-points-table').find('table').each(function() {
					var t = $(this);
					if (Math.floor(t.width()) > t.parent().width()) {
						t.wrap($('<div><div class="sb-wide-table-wrapper"></div></div>'));
						Attendance.scrollablePointsTableSetup();
					}
				});
			}));
		};
		window.doRequest = doRequest;

		$('#command input').on('change', function(e) {
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

		$('.clear-filter').on('click', function() {
			$('.filter-list').each(function() {
				var $list = $(this);

				$list.find('input:checked').each(function() {
					var $checkbox = $(this);
					$checkbox.prop('checked', false);
					updateFilter($checkbox);
				});

				prependClearLink($list);
			});

			doRequest($('#command'));
		});
	});
</script>