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

<h1>Select students</h1>
<h4><span class="muted">for</span> ${scheme.displayName}</h4>

<div class="fix-area">

	<form method="POST" action="">

		<#list findCommand.staticStudentIds as id>
			<input type="hidden" name="staticStudentIds" value="${id}" />
		</#list>
		<#list editMembershipCommand.includedStudentIds as id>
			<input type="hidden" name="includedStudentIds" value="${id}" />
		</#list>
		<#list editMembershipCommand.excludedStudentIds as id>
			<input type="hidden" name="excludedStudentIds" value="${id}" />
		</#list>
		<input type="hidden" name="filterQueryString" value="${findCommand.filterQueryString!""}" />

		<details class="find-students" <#if expandFind>open</#if> data-submitparam="${ManageSchemeMappingParameters.findStudents}">
			<summary class="large-chevron collapsible">
				<span class="legend">Find students
					<small>Select students by route, year of study etc.</small>
				</span>

			</summary>

			<p>Use the filters to add students to this scheme:</p>

			<#list findCommand.updatedStaticStudentIds as id>
				<input type="hidden" name="updatedStaticStudentIds" value="${id}" />
			</#list>


			<input type="hidden" name="updatedFilterQueryString" value="${findCommand.serializeFilter}" />
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
					<button disabled class="btn btn-mini btn-primary search" type="submit" name="${ManageSchemeMappingParameters.findStudents}">
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
					<input class="btn btn-warning hideOnClosed btn-small use-tooltip"
						<#if findCommandResult.membershipItems?size == 0>disabled</#if>
						type="submit"
						name="${ManageSchemeMappingParameters.manuallyExclude}"
						value="Remove selected"
						title="Remove selected students from this scheme"
						style="margin-left: 0.5em;"
					/>
				</p>

				<@attendance_macros.manageStudentTable
					membershipItems=findCommandResult.membershipItems
					doSorting=true
					command=findCommand
					checkboxName="excludeIds"
					onlyShowCheckboxForStatic=true
				/>
			</#if>
		</details>


		<details class="manually-added" <#if expandManual>open</#if>>
			<summary class="large-chevron collapsible">
				<span class="legend">Manually add students
					<small>Add a list of students by university ID or username</small>
				</span>

				<p>
					<input class="btn" type="submit" name="${ManageSchemeMappingParameters.manuallyAddForm}" value="Add students manually" />
					<#if (editMembershipCommandResult.updatedIncludedStudentIds?size > 0 || editMembershipCommandResult.updatedExcludedStudentIds?size > 0)>
						<input class="btn btn-warning hideOnClosed use-tooltip"
						   type="submit"
						   name="${ManageSchemeMappingParameters.resetMembership}"
						   value="Reset"
						   data-container="body"
						   title="Restore the manually removed and remove the manually added students selected"
						/>
					</#if>
				</p>
			</summary>

			<p>
				<@fmt.p editMembershipCommandResult.updatedIncludedStudentIds?size "student" />
				added manually and
				<@fmt.p editMembershipCommandResult.updatedExcludedStudentIds?size "student" />
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
			/>

			<#list editMembershipCommandResult.updatedIncludedStudentIds as id>
				<input type="hidden" name="updatedIncludedStudentIds" value="${id}" />
			</#list>
			<#list editMembershipCommandResult.updatedExcludedStudentIds as id>
				<input type="hidden" name="updatedExcludedStudentIds" value="${id}" />
			</#list>
		</details>

		<input type="hidden" name="returnTo" value="${returnTo}">
	</form>


	<div class="fix-footer submit-buttons">
		<form action="${returnTo}" method="POST">
			<input type="hidden" name="filterQueryString" value="${findCommand.filterQueryString!""}">
			<input type="hidden" name="updatedFilterQueryString" value="${findCommand.serializeFilter}">
			<#list findCommand.staticStudentIds as id>
				<input type="hidden" name="staticStudentIds" value="${id}" />
			</#list>
			<#list findCommandResult.updatedStaticStudentIds as id>
				<input type="hidden" name="updatedStaticStudentIds" value="${id}" />
			</#list>
			<#list editMembershipCommand.includedStudentIds as id>
				<input type="hidden" name="includedStudentIds" value="${id}" />
			</#list>
			<#list editMembershipCommandResult.updatedIncludedStudentIds as id>
				<input type="hidden" name="updatedIncludedStudentIds" value="${id}" />
			</#list>
			<#list editMembershipCommand.excludedStudentIds as id>
				<input type="hidden" name="excludedStudentIds" value="${id}" />
			</#list>
			<#list editMembershipCommandResult.updatedExcludedStudentIds as id>
				<input type="hidden" name="updatedExcludedStudentIds" value="${id}" />
			</#list>
			<#if summaryString?has_content>
				<p>${summaryString}</p>
			</#if>
			<input
				type="submit"
				value="Link to SITS"
				class="btn btn-success use-tooltip"
				name="${ManageSchemeMappingParameters.linkToSits}"
				data-container="body"
				title="Link this filter so that this group of students will be automatically updated from SITS"
			>
			<input
				type="submit"
				value="Import as list"
				class="btn btn-primary use-tooltip"
				name="${ManageSchemeMappingParameters.importAsList}"
				data-container="body"
				data-html="true"
				title="Import these students into a static list which will <strong>not</strong> be updated from SITS"
			>
			<input type="submit" value="Cancel" class="btn" name="${ManageSchemeMappingParameters.reset}">
		</form>
	</div>
</div>

</#escape>

<script>
	jQuery(function($){
		$('.fix-area').fixHeaderFooter();
		$('details').on('open.details close.details', function(){
			setTimeout(function(){
				$(window).trigger('resize');
			}, 10);
		});

		if ($('input[name="updatedFilterQueryString"]').val().length > 0) {
			$('button.search').attr('disabled', false);
		}

		$('.tablesorter').find('th.sortable').addClass('header').on('click', function() {
			var filter_$th = $(this)
				, sortDescending = function(){
					$('#sortOrder').val('desc(' + $th.data('field') + ')');
					$th.closest('thead').find('th').removeClass('headerSortUp').removeClass('headerSortDown');
					$th.addClass('headerSortUp');
				}, sortAscending = function(){
					$('#sortOrder').val('asc(' + $th.data('field') + ')');
					$th.closest('thead').find('th').removeClass('headerSortUp').removeClass('headerSortDown');
					$th.addClass('headerSortDown');
				}, $form = $th.closest('form')
				, $details = $th.closest('details');

			if ($th.hasClass('headerSortUp')) {
				sortAscending();
			} else if ($th.hasClass('headerSortDown')) {
				sortDescending();
			} else {
				// not currently sorted on this column, default sort depends on column
				if ($th.hasClass('unrecorded-col') || $th.hasClass('missed-col')) {
					sortDescending();
				} else {
					sortAscending();
				}
			}

			if ($details.data('submitparam').length > 0) {
				$form.append($('<input/>').attr({
					'type' : 'hidden',
					'name' : $details.data('submitparam'),
					'value' : true
				}));
			}
			$form.submit();
		});

		$('.pagination').on('click', 'a', function(){
			var $this = $(this), $form = $this.closest('form'), $details = $this.closest('details');
			if ($this.data('page').toString.length > 0) {
				$form.find('input[name="page"]').remove().end()
					.append($('<input/>').attr({
						'type' : 'hidden',
						'name' : 'page',
						'value' : $this.data('page')
					})
				);
			}
			if ($details.data('submitparam').length > 0) {
				$form.find('input[name="' + $details.data('submitparam') + '"]').remove().end()
					.append($('<input/>').attr({
						'type' : 'hidden',
						'name' : $details.data('submitparam'),
						'value' : true
					})
				);
			}
			$form.submit();
		});

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
			// Add in route search
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

			updateSearchButton($el);
			updateClearAllButton($el);
		};

		var updateClearAllButton = function($el) {
			var $filterList = $el.closest(".student-filter");

			if ($filterList.find(".empty-filter").length == $filterList.find(".btn-group").length) {
				$('.clear-all-filters').attr("disabled", "disabled");
			} else {
				$('.clear-all-filters').removeAttr("disabled");
			}
		};

		var updateSearchButton = function($el){
			var $filter = $el.closest('.student-filter');
			if ($filter.find('input:checked').length > 0) {
				$filter.find('button.search').attr('disabled', false);
			} else {
				$filter.find('button.search').attr('disabled', true);
			}
		};

		$('.student-filter input').on('change', function() {
			// Load the new results
			var $checkbox = $(this);
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
		});
	});
</script>