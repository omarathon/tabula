<#escape x as x?html>

<h1>Add monitoring point to ${template.templateName}</h1>

<#assign action><@url page="/sysadmin/attendancetemplates/${template.id}/points/add"/></#assign>
<@f.form action="${action}" method="POST" commandName="command" class="form-horizontal">

	<@form.labelled_row "name" "Name">
		<@f.input path="name" cssClass="input-block-level"/>
	</@form.labelled_row>

	<div class="dateTimePair">
		<#if template.pointStyle.dbValue == "week">

			<@form.labelled_row "startWeek" "Start">
				<@f.select path="startWeek" cssClass="startDateTime selectOffset">
					<#list 1..52 as week>
						<@f.option value="${week}">Week ${week}</@f.option>
					</#list>
				</@f.select>
			</@form.labelled_row>

			<@form.labelled_row "endWeek" "End">
				<@f.select path="endWeek" cssClass="endDateTime selectOffset">
					<#list 1..52 as week>
						<@f.option value="${week}">Week ${week}</@f.option>
					</#list>
				</@f.select>
			</@form.labelled_row>

		<#else>

			<@form.labelled_row "startDate" "Start">
				<@f.input type="text" path="startDate" cssClass="input-medium date-picker startDateTime" placeholder="Pick the start date" />
				<input class="endoffset" type="hidden" data-end-offset="0" />
				<@fmt.help_popover id="startDate" content="You cannot mark a point as attended or missed (unauthorised) before its start date" />
			</@form.labelled_row>

			<@form.labelled_row "endDate" "End">
				<@f.input type="text" path="endDate" cssClass="input-medium date-picker endDateTime" placeholder="Pick the end date" />
				<@fmt.help_popover id="endDate" content="A warning will appear for unrecorded attendance after its end date" />
			</@form.labelled_row>

		</#if>
	</div>

	<div>
		<button class="btn btn-primary spinnable spinner-auto" type="submit" name="submit" data-loading-text="Adding&hellip;">
			Add
		</button>
		<button class="btn" type="submit" name="cancel">Cancel</button>
	</div>
</@f.form>


</#escape>