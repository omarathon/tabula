<#escape x as x?html>
<#import "../attendance_variables.ftl" as attendance_variables />
<#import "../attendance_macros.ftl" as attendance_macros />
<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />

<div id="profile-modal" class="modal fade profile-subset"></div>

<#if (filterResult.totalResults > 0)>

	<#assign filterQuery = filterCommand.serializeFilter />
	<#assign sortOrderString = "" />
	<#if (filterCommand.sortOrder?size > 0)>
		<#assign sortOrderString><#if filterCommand.sortOrder?first.ascending>asc<#else>desc</#if>(${filterCommand.sortOrder?first.propertyName})</#assign>
	</#if>
	<#assign sendToSitsUrl><@routes.viewReport department academicYear.startYear?c filterQuery /></#assign>
	<div class="studentResults" data-sits-url="${sendToSitsUrl}">
		<#assign returnTo><@routes.viewStudents department academicYear.startYear?c filterQuery filterCommand.page sortOrderString /></#assign>
		<#assign returnTo = returnTo?url />
		<#if (filterResult.totalResults > 0)>
			<div class="clearfix fix-header pad-when-fixed">
				<#if (filterResult.totalResults > filterCommand.studentsPerPage)>
					<div class="pull-right">
						<@attendance_macros.pagination filterCommand.page filterResult.totalResults filterCommand.studentsPerPage "pagination-small" />
					</div>
				</#if>

				<#assign startIndex = ((filterCommand.page - 1) * filterCommand.studentsPerPage) />
				<#assign endIndex = startIndex + filterResult.results?size />
				<p>Results ${startIndex + 1} - ${endIndex} of ${filterResult.totalResults}</p>
			</div>

			<@attendance_macros.scrollablePointsTable
				command=filterCommand
				filterResult=filterResult
				visiblePeriods=visiblePeriods
				monthNames=monthNames
				department=department
			; result>
				<td class="unrecorded">
					<a href="<@routes.viewSingleStudent department academicYear.startYear?c result.student />">
						<span class="badge badge-<#if (result.checkpointTotal.unrecorded > 2)>important<#elseif (result.checkpointTotal.unrecorded > 0)>warning<#else>success</#if>">
							${result.checkpointTotal.unrecorded}
						</span>
					</a>
				</td>
				<td class="missed">
					<a href="<@routes.viewSingleStudent department academicYear.startYear?c result.student />">
						<span class="badge badge-<#if (result.checkpointTotal.unauthorised > 2)>important<#elseif (result.checkpointTotal.unauthorised > 0)>warning<#else>success</#if>">
							${result.checkpointTotal.unauthorised}
						</span>
					</a>
				</td>
				<td class="record">
					<#assign record_url><@routes.viewRecordStudent department academicYear.startYear?c result.student returnTo /></#assign>
					<@fmt.permission_button
						permission='MonitoringPoints.Record'
						scope=result.student
						action_descr='record monitoring points'
						classes='btn btn-primary btn-mini'
						href=record_url
						tooltip='Record'
					>
						<i class="icon-pencil icon-fixed-width late"></i>
					</@fmt.permission_button>
				</td>
			</@attendance_macros.scrollablePointsTable>

			<#if !student_table_script_included??>
				<script type="text/javascript">
					(function($) {
						$(function() {
							$(".scrollable-points-table .left table, .scrollable-points-table .right table").addClass('tablesorter')
									.find('th.sortable').addClass('header')
									.on('click', function(e) {
										var $th = $(this)
												, sortDescending = function(){
													$('#sortOrder').val('desc(' + $th.data('field') + ')');
													$th.closest('thead').find('th').removeClass('headerSortUp').removeClass('headerSortDown');
													$th.addClass('headerSortUp');
												}, sortAscending = function(){
													$('#sortOrder').val('asc(' + $th.data('field') + ')');
													$th.closest('thead').find('th').removeClass('headerSortUp').removeClass('headerSortDown');
													$th.addClass('headerSortDown');
												};

										if ($th.hasClass('headerSortUp')) {
											sortAscending();
										} else if ($th.hasClass('headerSortDown')) {
											sortDescending();
										} else {
											// if unrecorded or missed, sort desc on first click
											if ($th.hasClass('unrecorded-col') || $th.hasClass('missed-col')) {
												sortDescending();
											} else {
												sortAscending();
											}
										}

										if (typeof(window.doRequest) === 'function') {
											window.doRequest($('#filterCommand'), true);
										} else {
											$('#filterCommand').submit();
										}
									});
						});
						$(window).on('load', function(){
							Attendance.scrollablePointsTableSetup();
						});
					})(jQuery);
				</script>
				<#assign student_table_script_included=true />
			</#if>

			<div class="clearfix">
				<#if (filterResult.totalResults <= filterCommand.studentsPerPage)>
					<div class="pull-left">
						<@fmt.bulk_email_students students=filterResult.students />
					</div>
				<#else>
					<@attendance_macros.pagination filterCommand.page filterResult.totalResults filterCommand.studentsPerPage "pagination-small" />
				</#if>
			</div>

		<#else>
			<p>No students were found.</p>
		</#if>
	</div>
</#if>

<script type="text/javascript">
	jQuery(function($) {
		$('.pagination a').on('click', function(e) {
			e.preventDefault();
			e.stopPropagation();

			var page = $(this).data('page');
			$('#page').val(page);

			if (typeof(window.doRequest) === 'function') {
				window.doRequest($('#filterCommand'), true);
			} else {
				$('#filterCommand').submit();
			}
		});
	});
	// Enable any freshly loaded popovers
	jQuery('.use-popover').tabulaPopover({
		trigger: 'click',
		container: '#container'
	});
</script>

</#escape>