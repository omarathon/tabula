<@form.labelled_row "name" "Name">
	<@f.input path="name" cssClass="input-block-level"/>
</@form.labelled_row>

<@form.labelled_row "validFromWeek" "Start">
	<@f.select path="validFromWeek">
		<#list 1..52 as week>
			<@f.option value="${week}"><@fmt.singleWeekFormat week command.academicYear command.dept /></@f.option>
		</#list>
	</@f.select>
	<a class="use-popover" id="popover-validFromWeek" data-content="You cannot mark a point as attended or missed (unauthorised) before its start date"><i class="icon-question-sign"></i></a>
</@form.labelled_row>

<@form.labelled_row "requiredFromWeek" "End">
	<@f.select path="requiredFromWeek">
		<#list 1..52 as week>
			<@f.option value="${week}"><@fmt.singleWeekFormat week command.academicYear command.dept /></@f.option>
		</#list>
	</@f.select>
	<a class="use-popover" id="popover-requiredFromWeek" data-content="A warning will appear for unrecorded attendance after its end date"><i class="icon-question-sign"></i></a>
</@form.labelled_row>

<script>
jQuery(function($){
	$('.use-popover').tabulaPopover({
		trigger: 'click',
		container: '#container'
	});
});
</script>