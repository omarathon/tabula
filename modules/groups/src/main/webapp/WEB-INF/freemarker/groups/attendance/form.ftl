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
		<input type="hidden" value="<@url page="${returnTo}" />" />
		<#list command.members as student>
			<#assign checked = false />
			<#list command.attendees as attendedStudent>
				<#if attendedStudent == student.universityId>
					<#assign checked = true />
				</#if>
			</#list>
			<div class="row-fluid item-info clickable">
				<label>
				<div class="span10">
					<@fmt.member_photo student "tinythumbnail" false />
					<div class="full-height">${student.fullName}</div>
				</div>
				<div class="span1">
					<div class="full-height">
						<input type="checkbox" name="attendees" value="${student.universityId}" <#if checked>checked="checked"</#if>/>
					</div>
				</div>
				</label>
			</div>
		</#list>
		
		<div class="pull-right">
			<input type="submit" value="Save" class="btn btn-primary">
			<a class="btn" href="<@url page="${returnTo}" />">Cancel</a>
		</div>
	</form>
</div>