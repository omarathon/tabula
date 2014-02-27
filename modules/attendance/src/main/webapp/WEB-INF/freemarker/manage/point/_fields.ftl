<#function extractParam collection param>
	<#local result = [] />
	<#list collection as item>
		<#local result = result + [item[param]] />
	</#list>
	<#return result />
</#function>

<@form.labelled_row "name" "Name">
	<@f.input path="name" cssClass="input-block-level"/>
</@form.labelled_row>
<div class="dateTimePair">
<@form.labelled_row "validFromWeek" "Start">
	<@f.select path="validFromWeek" cssClass="startDateTime selectOffset">
		<#list 1..52 as week>
			<@f.option value="${week}"><@fmt.monitoringPointWeeksFormat week week command.academicYear command.dept /></@f.option>
		</#list>
	</@f.select>
	<@fmt.help_popover id="validFromWeek" content="You cannot mark a point as attended or missed (unauthorised) before its start date" />
</@form.labelled_row>

<@form.labelled_row "requiredFromWeek" "End">
	<@f.select path="requiredFromWeek" cssClass="endDateTime selectOffset">
		<#list 1..52 as week>
			<@f.option value="${week}"><@fmt.monitoringPointWeeksFormat week week command.academicYear command.dept /></@f.option>
		</#list>
	</@f.select>
	<@fmt.help_popover id="requiredFromWeek" content="A warning will appear for unrecorded attendance after its end date" />
</@form.labelled_row>
</div>
<@form.labelled_row "pointType" "Type">
	<@form.label clazz="radio" checkbox=true>
		<input id="pointType1" name="pointType" type="radio" value="" <#if !command.pointType??>checked</#if>>
		Standard
	</@form.label>
	<@form.label clazz="radio" checkbox=true>
		<@f.radiobutton path="pointType" value="meeting" />
		Meeting
		<@fmt.help_popover id="pointType-meeting" content="This monitoring point will be marked as 'attended' if there is a record in Tabula of a meeting taking place between the start and end dates" />
	</@form.label>
	<#if features.attendanceMonitoringSmallGroupPointType>
		<@form.label clazz="radio" checkbox=true>
			<@f.radiobutton path="pointType" value="smallGroup" />
			Teaching event
			<@fmt.help_popover id="pointType-smallGroup" content="This monitoring point will be marked as 'attended' if the student attends a small group teaching event recorded in Tabula between the start and end dates" />
		</@form.label>
	</#if>
</@form.labelled_row>

<#if features.attendanceMonitoringMeetingPointType>

	<#assign meetingRelationshipsStrings = extractParam(command.meetingRelationships, 'urlPart') />
	<#assign meetingFormatsStrings = extractParam(command.meetingFormats, 'description') />
	<div class="pointTypeOption meeting row-fluid" <#if ((command.pointType.dbValue)!'null') == 'meeting'>style="display:none"</#if>>
		<div class="span5">
			<@form.labelled_row "meetingRelationships" "Meeting with">
				<#list command.dept.displayedStudentRelationshipTypes as relationship>
					<@form.label checkbox=true>
						<input type="checkbox" name="meetingRelationships" id="meetingRelationships-${relationship.urlPart}" value="${relationship.urlPart}" <#if meetingRelationshipsStrings?seq_contains(relationship.urlPart)>checked</#if> />
						${relationship.agentRole?capitalize}
						<@fmt.help_popover id="meetingRelationships-${relationship.urlPart}" content="This monitoring point will be marked as 'attended' when a meeting record is created or approved by the student's ${relationship.agentRole?capitalize}" />
					</@form.label>
				</#list>
			</@form.labelled_row>

			<@form.labelled_row "meetingQuantity" "Number of meetings">
				<@f.input path="meetingQuantity" cssClass="input-mini"/>
				<@fmt.help_popover id="meetingQuantity" content="The student must have this many meetings between the start and end dates in order to meet this monitoring point" />
			</@form.labelled_row>
		</div>

		<div class="span6">
			<@form.labelled_row path="meetingFormats" label="Meeting formats" helpPopover="Only selected meeting formats will count towards this monitoring point">
				<#list allMeetingFormats as format>
					<@form.label checkbox=true>
						<input type="checkbox" name="meetingFormats" id="meetingFormats-${format.code}" value="${format.description}" <#if meetingFormatsStrings?seq_contains(format.description)>checked</#if> />
						${format.description}
					</@form.label>
				</#list>
			</@form.labelled_row>
		</div>
	</div>

</#if>

<#if features.attendanceMonitoringSmallGroupPointType>

	<div class="pointTypeOption smallGroup row-fluid" <#if ((command.pointType.dbValue)!'null') != 'smallGroup'>style="display:none"</#if>>

		<div class="module-choice">
			<@form.labelled_row "smallGroupEventModules" "Modules">
				<@form.label clazz="radio" checkbox=true>
					<input type="radio" <#if (command.anySmallGroupEventModules)>checked </#if> value="true" name="isAnySmallGroupEventModules"/>
					Any
					<@fmt.help_popover id="isAnySmallGroupEventModules" content="Attendance at any module recorded in Tabula will count towards this monitoring point" />
				</@form.label>

				<@form.label clazz="radio pull-left specific" checkbox=true>
					<input class="specific" type="radio" <#if (!command.anySmallGroupEventModules)>checked </#if> value="false" name="isAnySmallGroupEventModules"/>
					Specific
				</@form.label>
				<div class="module-search input-append">
					<input class="module-search-query module-picker" type="text" value=""/>
					<span class="add-on"><i class="icon-search"></i></span>
				</div>
				<button class="btn add-module"><i class="icon-plus"></i> </button>
				<@fmt.help_popover id="isAnySmallGroupEventModules" content="Attendance at any of the specified modules recorded in Tabula will count towards this monitoring point" />
				<div class="modules-list">
					<input type="hidden" name="_smallGroupEventModules" value="false" />
					<ul>
						<#list command.smallGroupEventModules![] as module>
							<li>
								<input type="hidden" name="smallGroupEventModules" value="${module.id}" />
								<i class="icon-fixed-width"></i><span title="<@fmt.module_name module false />"><@fmt.module_name module false /></span><button class="btn btn-danger"><i class="icon-remove"></i></button>
							</li>
						</#list>

					</ul>

				</div>

			</@form.labelled_row>

		</div>


		<@form.labelled_row "smallGroupEventQuantityAll" "Number of events">
			<#-- <@form.label>
			<input type="radio" class="input-mini" <#if (command.smallGroupEventQuantity?? && command.smallGroupEventQuantity > 0)>checked</#if>" value="false" name="smallGroupEventQuantityAll" > -->
				<input class="input-mini" type="text" <#if (command.smallGroupEventQuantity?? && command.smallGroupEventQuantity > 0)>value="${command.smallGroupEventQuantity}"</#if> name="smallGroupEventQuantity" />
				<@fmt.help_popover id="smallGroupEventQuantity" content="The student must have attended this many events for any of the specified modules between the start and end dates in order to meet this monitoring point" />
			<#-- </@form.label>
			<@form.label clazz="radio" checkbox=true>
				<input type="radio" class="input-mini" <#if !(command.smallGroupEventQuantity?? && command.smallGroupEventQuantity > 0)>checked</#if>" value="true" name="smallGroupEventQuantityAll" >
				All
				<@fmt.help_popover id="smallGroupEventQuantityAll" content="The student must have attended all events set up in Tabula for any of the specified modules between the start and end dates in order to meet this mornitoring point" />
			</@form.label> -->
		</@form.labelled_row>

	</div>

</#if>

<script>
(function($) {
	// Show relavant extra options when changing point type
	if ($('form input[name=pointType]').length > 0) {
		var showOptions = function() {
			var value = $('form input[name=pointType]:checked').val();
			$('.pointTypeOption').hide();
			if (value != undefined && value.length > 0) {
				$('.pointTypeOption.' + value).show();
			}
		};
		$('form input[name=pointType]').on('click', showOptions);
		showOptions();
	}

	$(function(){
		$('.use-popover').tabulaPopover({
			trigger: 'click',
			container: '#container'
		});
		$('.module-picker').modulePicker({});

		Attendance.bindModulePicker();
	});
})(jQuery);
</script>