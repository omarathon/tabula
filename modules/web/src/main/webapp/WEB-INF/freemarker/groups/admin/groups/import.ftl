<#macro eventDetails event><#compress>
	<div class="day-time">
		${(event.day.name)!""}
		<#if event.startTime??><@fmt.time event.startTime /><#else>[no start time]</#if>
		-
		<#if event.endTime??><@fmt.time event.endTime /><#else>[no end time]</#if>
	</div>
	<#if event.staff?size gt 0>
		Tutor<#if event.staff?size gt 1>s</#if>:
		<#list event.staff as user>
			${user.fullName}<#if user_has_next>,</#if>
		</#list>
	</#if>
	<#if ((event.location.name)!"")?has_content>
		<div class="location">
			Room: <@fmt.location event.location />
		</div>
	</#if>
	<div class="running">
		Running: <#compress>
			<#if event.weekRanges?size gt 0 && event.day??>
				${weekRangesFormatter(event.weekRanges, event.day, academicYear, department)}
			<#elseif event.weekRanges?size gt 0>
				[no day of week selected]
			<#else>
				[no dates selected]
			</#if>
		</#compress>
	</div>
</#compress></#macro>

<#macro groups_details timetabledEvent>
	<div class="set-info striped-section collapsible">
		<div class="clearfix">
			<div class="section-title row-fluid">
				<div class="span8 icon-container">
					<span class="h6 colour-h6">${timetabledEvent.module.code?upper_case} ${timetabledEvent.eventType.displayName}s</span>
				</div>
				<div class="span2">
					<@fmt.p timetabledEvent.events?size "group" />
				</div>
				<div class="span2">
					<#local studentsCount = 0 />
					<#list timetabledEvent.events as event>
						<#local studentsCount = studentsCount + event.students?size />
					</#list>

					<@fmt.p studentsCount "student" />
				</div>
			</div>

			<div class="striped-section-contents">
				<#list timetabledEvent.events as event>
					<div class="item-info row-fluid">
						<div class="span2">
							<h4 class="name">
								Group ${event_index + 1}
							</h4>
						</div>
						<div class="span2">
							<@fmt.p event.students?size "student" />
						</div>

						<div class="span8">
							<#if event.startTime??><@fmt.time event.startTime /></#if> ${(event.day.name)!""}

							<#local popoverContent><@eventDetails event /></#local>
							<a class="use-popover"
							   data-html="true"
							   data-content="${popoverContent?html}"><i class="icon-question-sign"></i></a>
						</div>
					</div>
				</#list>
			</div>
		</div>
	</div> <!-- module-info striped-section-->
</#macro>

<#escape x as x?html>
	<#import "*/group_components.ftl" as components />

	<#macro deptheaderroutemacro department>
		<@routes.groups.import_groups_for_year department academicYear />
	</#macro>
	<#assign deptheaderroute = deptheaderroutemacro in routes.groups />

	<@fmt.deptheader "Import small groups from Syllabus+" "for" department routes.groups "deptheaderroute" "" />

	<#assign post_url><@routes.groups.import_groups department /></#assign>
	<div class="fix-area">
		<@f.form method="post" id="import-form" action="${post_url}" commandName="command" cssClass="form-horizontal">
			<input type="hidden" name="action" value="" />

			<p>Below are all of the scheduled small groups defined for modules in this department in Syllabus+, the central timetabling system.</p>

			<p>Use the checkboxes on the left hand side to choose which ones you want to import into Tabula.
			   If there is already a set of small groups with the same name for this academic year, it will have
			   been unchecked, but you can check them if you want to import them again (note: they will be imported
			   as a <strong>separate</strong> set of small groups to the ones already imported.</p>

			<@form.labelled_row "academicYear" "Academic year">
				<@f.select path="academicYear" id="academicYearSelect">
					<@f.options items=academicYearChoices itemLabel="label" itemValue="storeValue" />
				</@f.select>
			</@form.labelled_row>

			<table class="table table-bordered table-striped" id="import-groups-table">
				<tr>
					<th>
						<div class="check-all checkbox">
							<label><span class="very-subtle"></span>
								<input type="checkbox" checked="checked" class="collection-check-all use-tooltip" title="Select/unselect all">
							</label>
						</div>
					</th>
					<td></td>
				</tr>

				<#list timetabledEvents as timetabledEvent>
					<tr class="itemContainer">
						<td class="selectable"><@f.checkbox path="selected[${timetabledEvent_index}]" cssClass="collection-checkbox" /></td>
						<td class="selectable">
							<@groups_details timetabledEvent />
						</td>
					</tr>
				</#list>
			</table>

			<div class="fix-footer submit-buttons">
				<div class="pull-right">
					<input id="submit-button" type="submit" value="Import groups" class="btn btn-primary" data-loading-text="Importing&hellip;" autocomplete="off">
					<a class="btn" href="<@routes.groups.departmenthome department academicYear />">Cancel</a>
				</div>
			</div>
		</@f.form>
	</div>

	<script type="text/javascript">
		jQuery(function($){
			var $form = $('#import-form');

			// reload page when academic field dropdown changes, as it changes the contents of the list.
			$('#academicYearSelect').change(function(){
				$form.find('input[name=action]').val('change-year');
				$form.submit();
			});

			$('#import-groups-table').bigList({
				onChange : function() {
					this.closest("tr").toggleClass("selected", this.is(":checked"));
				},

				onSomeChecked : function() {
					$('#submit-button').removeClass('disabled');
				},

				onNoneChecked : function() {
					$('#submit-button').addClass('disabled');
				}
			});

			// cool selection mechanism...
			var batchTableMouseDown = false;
			$('#import-groups-table')
					.on('mousedown', 'td.selectable', function(){
						batchTableMouseDown = true;
						var $row = $(this).closest('tr');
						$row.toggleClass('selected');
						var checked = $row.hasClass('selected');
						$row.find('.collection-checkbox').attr('checked', checked);
						return false;
					})
					.on('mouseenter', 'td.selectable', function(){
						if (batchTableMouseDown) {
							var $row = $(this).closest('tr');
							$row.toggleClass('selected');
							var checked = $row.hasClass('selected');
							$row.find('.collection-checkbox').attr('checked', checked);
						}
					})
					.on('mousedown', 'a.name-edit-link, .striped-section, input[type="checkbox"]', function(e){
						// prevent td.selected toggling when clicking
						e.stopPropagation();
					});

			$(document).mouseup(function(){
				batchTableMouseDown = false;
				$('#import-groups-table').bigList('changed');
			});
		});
	</script>
</#escape>