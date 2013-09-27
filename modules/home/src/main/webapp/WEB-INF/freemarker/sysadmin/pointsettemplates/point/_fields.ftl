<@form.labelled_row "name" "Name">
	<@f.input path="name" cssClass="input-block-level"/>
</@form.labelled_row>

<@form.labelled_row "validFromWeek" "'Valid from' week">
	<@f.select path="validFromWeek">
		<#list 1..52 as week>
			<@f.option value="${week}">Week ${week}</@f.option>
		</#list>
	</@f.select>
	<a class="use-popover" id="popover-validFromWeek" data-content="Before this week a point can only be marked as Missed (authorised)"><i class="icon-question-sign"></i></a>
</@form.labelled_row>

<@form.labelled_row "requiredFromWeek" "'Required from' week">
	<@f.select path="requiredFromWeek">
		<#list 1..52 as week>
			<@f.option value="${week}">Week ${week}</@f.option>
		</#list>
	</@f.select>
	<a class="use-popover" id="popover-requiredFromWeek" data-content="From this week a point with a checkpoint for each student will be marked as unrecorded"><i class="icon-question-sign"></i></a>
</@form.labelled_row>

<script>
jQuery(function($){
	$('.use-popover').tabulaPopover({
		trigger: 'click',
		container: '#container'
	});
});
</script>