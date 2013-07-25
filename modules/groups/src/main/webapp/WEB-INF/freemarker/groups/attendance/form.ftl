<h1>Record attendance for 
	${command.event.group.groupSet.name}, 
	${command.event.group.name}:<br />
	${command.event.day.name} <@fmt.time event.startTime /> - <@fmt.time event.endTime />, Week ${command.week}
</h1>

<div class="row-fluid">
	<div class="span1 offset10">
		Attended
	</div>
</div>

<div class="striped-section-contents attendees">
	<form action="" method="post">
		<#list command.event.group.students.users as student>
			<#assign checked = false />
			<#list command.attendees as attendedStudent>
				<#if attendedStudent == student.userId>
					<#assign checked = true />
				</#if>
			</#list>
			<div class="row-fluid item-info clickable">
				<label>
				<div class="span10">
					<@fmt.member_photo student "tinythumbnail" false />
					${student.fullName}
				</div>
				<div class="span1">
					<input type="checkbox" name="attendees" value="${student.userId}" <#if checked>checked="checked"</#if>/>
				</div>
				</label>
			</div>
		</#list>
		
		<div class="pull-right">
			<input type="submit" value="Save" class="btn btn-primary">
			<a class="btn" href="<@url page="${cancel}" />">Cancel</a>
		</div>
	</form>
</div>