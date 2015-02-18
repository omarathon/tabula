<#escape x as x?html>

<h1>Edit ${template.templateName}</h1>

<#assign action><@url page="/sysadmin/attendancetemplates/${template.id}/edit"/></#assign>
<@f.form id="newScheme" method="POST" commandName="command" class="form-horizontal" action="${action}">

	<@form.labelled_row "name" "Name">
		<@f.input path="name" />
	</@form.labelled_row>

	<#if template.points?size == 0>
		<@form.labelled_row "pointStyle" "Date format">
			<@form.label clazz="radio" checkbox=true>
				<@f.radiobutton path="pointStyle" value="week" />
			term weeks
				<@fmt.help_popover id="pointStyle-week" content="Create points which cover term weeks e.g. Personal tutor meeting weeks 2-3" />
			</@form.label>
			<@form.label clazz="radio" checkbox=true>
				<@f.radiobutton path="pointStyle" value="date" />
			calendar dates
				<@fmt.help_popover id="pointStyle-date" content="Create points which use calendar dates e.g. Supervision 1st-31st October" />
			</@form.label>
		<span class="hint">Select the date format to use for points on this scheme</span>
		</@form.labelled_row>
	<#else>
		<@form.labelled_row "pointStyle" "Date format">
			<@f.hidden path="pointStyle" />
			<@form.label clazz="radio" checkbox=true>
				<@f.radiobutton path="pointStyle" value="week" disabled=true />
			term weeks
				<@fmt.help_popover id="pointStyle-week" content="Create points which cover term weeks e.g. Personal tutor meeting weeks 2-3" />
			</@form.label>
			<@form.label clazz="radio" checkbox=true>
				<@f.radiobutton path="pointStyle" value="date" disabled=true />
			calendar dates
				<@fmt.help_popover id="pointStyle-date" content="Create points which use calendar dates e.g. Supervision 1st-31st October" />
			</@form.label>
		<span class="hint">You cannot change the type of points once some points have been added to a scheme</span>
		</@form.labelled_row>
	</#if>

	<input
		type="submit"
		class="btn btn-primary"
		name="create"
		value="Save"
		data-container="body"
	/>
	<a class="btn" href="<@url page="/sysadmin/attendancetemplates" />">Cancel</a>
</@f.form>

<hr />

<div class="monitoring-points">

	<div class="striped-section">
		<div class="pull-right">
			<a href="<@url page="/sysadmin/attendancetemplates/${template.id}/points/add" />" class="btn btn-primary new-point"><i class="icon-plus"></i> Create new point</a>
		</div>
		<h2 class="section-title">Monitoring points</h2>
		<div class="striped-section-contents">
			<#if template.points?size == 0>
				<div class="item-info">
					<em>No points exist for this template</em>
				</div>
			<#else>
				<#if template.pointStyle.dbValue == "week">
					<#assign points = template.points?sort_by("startWeek") />
				<#else>
					<#assign points = template.points?sort_by("startDate") />
				</#if>
				<#list points as point>
					<div class="item-info row-fluid point">
						<div class="span12">
							<div class="pull-right">
								<a class="btn btn-primary edit-point" href="<@url page="/sysadmin/attendancetemplates/${template.id}/points/${point.id}/edit"/>">Edit</a>
								<a class="btn btn-danger delete-point" title="Delete" href="<@url page="/sysadmin/attendancetemplates/${template.id}/points/${point.id}/delete"/>"><i class="icon-remove"></i></a>
							</div>
							<#if template.pointStyle.dbValue == "week">
								${point.name} (week ${point.startWeek} - ${point.endWeek})
							<#else>
								${point.name} (<#noescape>${command.dateString(point.startDate)} - ${command.dateString(point.endDate)}</#noescape>)
							</#if>
						</div>
					</div>
				</#list>
			</#if>
		</div>
	</div>

</div>

</#escape>