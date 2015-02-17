<#escape x as x?html>
	<@fmt.deptheader "Notification settings" "for" department routes.admin "notificationsettings" "" />

	<@f.form method="post" class="form-horizontal notification-settings-form" action="" commandName="command">
		<div id="sgt-reminders" class="striped-section collapsible expanded">
			<fieldset class="clearfix">
				<h2 class="section-title">
					Small group attendance notifications
				</h2>
				<div class="striped-section-contents">
					<div class="item-info">
						<h5>Why are they sent?</h5>

						<p>Attendance for a small group event must be recorded within 5 working days in order
						   for it to qualify as evidence for monitoring purposes.</p>

						<p>Tabula will send a notification reminding people to record attendance. Tabula will only
						   send these notifications if the groups are set to collect attendance, and if the register
						   still needs some attendance recording.</p>

						<@form.label checkbox=true>
							<@f.checkbox path="smallGroupEventAttendanceReminderEnabled" id="smallGroupEventAttendanceReminderEnabled" />
							Turn on these notifications
						</@form.label>
						<@f.errors path="smallGroupEventAttendanceReminderEnabled" cssClass="error" />
					</div>

					<div class="item-info">
						<h5>When are they sent?</h5>

						<ul>
							<li>When the event has finished</li>
							<li>3 days after the event</li>
							<li>6 days after the event</li>
						</ul>
					</div>

					<div class="item-info">
						<h5>Who will receive them?</h5>

						<p>Unless an individual has opted out of small group attendance notifications, the following groups
						   of people will receive notifications: </p>

						<ul>
							<li><@form.label checkbox=true>
								<@f.checkbox path="smallGroupEventAttendanceReminderNotifyTutors" id="smallGroupEventAttendanceReminderNotifyTutors" />
								Tutors for the small group event
							</@form.label></li>

							<li><@form.label checkbox=true>
								<@f.checkbox path="smallGroupEventAttendanceReminderNotifyModuleAssistants" id="smallGroupEventAttendanceReminderNotifyModuleAssistants" />
								Module Assistants
							</@form.label></li>

							<li><@form.label checkbox=true>
								<@f.checkbox path="smallGroupEventAttendanceReminderNotifyModuleManagers" id="smallGroupEventAttendanceReminderNotifyModuleManagers" />
								Module Managers
							</@form.label></li>

							<li><@form.label checkbox=true>
								<@f.checkbox path="smallGroupEventAttendanceReminderNotifyDepartmentAdministrators" id="smallGroupEventAttendanceReminderNotifyDepartmentAdministrators" />
								Departmental Administrators
							</@form.label></li>
						</ul>

						<p>These notifications can be sent to all the selected groups of people, or only to the first group
						   with someone in it (e.g. Module Managers will only be notified if an event has no tutor).</p>

						<@form.label checkbox=true>
							<@f.radiobutton path="smallGroupEventAttendanceReminderNotifyFirstNonEmptyGroupOnly" value="false" />
							Notify all selected groups of people
						</@form.label>
						<@form.label checkbox=true>
							<@f.radiobutton path="smallGroupEventAttendanceReminderNotifyFirstNonEmptyGroupOnly" value="true" />
							Only notify the first matching group of people
						</@form.label>
					</div>
				</div>
			</fieldset>
		</div>

		<div class="submit-buttons">
			<input type="submit" value="Save" class="btn btn-primary">
			<#if (returnTo!"")?length gt 0>
				<#assign cancelDestination=returnTo />
			<#else>
				<#assign cancelDestination><@routes.admin.departmenthome department=department /></#assign>
			</#if>
			<a class="btn" href="${cancelDestination}">Cancel</a>
		</div>
	</@f.form>
</#escape>