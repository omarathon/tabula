<h1>Set ${monitoringPoint.name}</h1>


<div class="striped-section-contents attendees">
	<form action="" method="post">
		<input type="hidden" name="monitoringPoint" value="${monitoringPoint.id}" />
		<input type="hidden" value="<@url page="${returnTo}" />" />
		<#list command.members as student>
			<#assign checked = false />
			<#list command.membersChecked as studentChecked>
				<#if studentChecked.universityId == student.universityId>
					<#assign checked = true />
				</#if>
			</#list>

			<div class="row-fluid item-info clickable">
				<label>
					<div class="span10">

						<@fmt.member_photo student "tinythumbnail" true />
						<div class="full-height">${student.fullName}</div>
					</div>
					<div class="span1">
						<div class="full-height">
							<input type="checkbox" name="studentIds" value="${student.universityId}" <#if checked>checked="checked"</#if>/>
						</div>
					</div>
				</label>
			</div>
		</#list>

		<div class="pull-right">
			<input type="submit" value="Save" class="btn btn-primary">
			<a class="btn" href="<@url page="${returnTo}" context="/attendance" />">Cancel</a>
		</div>
	</form>
</div>
