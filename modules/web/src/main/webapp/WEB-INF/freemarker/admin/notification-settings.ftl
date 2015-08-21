<#escape x as x?html>
	<#function route_function dept>
		<#local result><@routes.admin.notificationsettings dept /></#local>
		<#return result />
	</#function>
	<@fmt.id7_deptheader "Notification settings" route_function "for" />

	<#assign submitUrl><@routes.admin.notificationsettings department /></#assign>
	<@f.form method="post" class="notification-settings-form" action=submitUrl commandName="command">
		<input type="hidden" name="returnTo" value="${returnTo}">
		<div class="striped-section collapsible expanded">
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

					<@bs3form.checkbox path="smallGroupEventAttendanceReminderEnabled">
						<@f.checkbox path="smallGroupEventAttendanceReminderEnabled" id="smallGroupEventAttendanceReminderEnabled" />
						Turn on these notifications
					</@bs3form.checkbox>
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
						<li>
							<@bs3form.checkbox path="smallGroupEventAttendanceReminderNotifyTutors">
								<@f.checkbox path="smallGroupEventAttendanceReminderNotifyTutors" id="smallGroupEventAttendanceReminderNotifyTutors" />
								Tutors for the small group event
							</@bs3form.checkbox>
						</li>

						<li>
							<@bs3form.checkbox path="smallGroupEventAttendanceReminderNotifyModuleAssistants">
								<@f.checkbox path="smallGroupEventAttendanceReminderNotifyModuleAssistants" id="smallGroupEventAttendanceReminderNotifyModuleAssistants" />
								Module Assistants
							</@bs3form.checkbox>
						</li>

						<li>
							<@bs3form.checkbox path="smallGroupEventAttendanceReminderNotifyModuleManagers">
								<@f.checkbox path="smallGroupEventAttendanceReminderNotifyModuleManagers" id="smallGroupEventAttendanceReminderNotifyModuleManagers" />
								Module Managers
							</@bs3form.checkbox>
						</li>

						<li>
							<@bs3form.checkbox path="smallGroupEventAttendanceReminderNotifyDepartmentAdministrators">
								<@f.checkbox path="smallGroupEventAttendanceReminderNotifyDepartmentAdministrators" id="smallGroupEventAttendanceReminderNotifyDepartmentAdministrators" />
								Departmental Administrators
							</@bs3form.checkbox>
						</li>
					</ul>

					<p>These notifications can be sent to all the selected groups of people, or only to the first group
					   with someone in it (e.g. Module Managers will only be notified if an event has no tutor).</p>

					<@bs3form.radio>
						<@f.radiobutton path="smallGroupEventAttendanceReminderNotifyFirstNonEmptyGroupOnly" value="false" />
						Notify all selected groups of people
					</@bs3form.radio>
					<@bs3form.radio>
						<@f.radiobutton path="smallGroupEventAttendanceReminderNotifyFirstNonEmptyGroupOnly" value="true" />
						Only notify the first matching group of people
					</@bs3form.radio>
				</div>
			</div>
		</div>

		<div class="striped-section collapsible expanded">
			<h2 class="section-title">
				Coursework marking workflow "send to administrator" notifications
			</h2>
			<div class="striped-section-contents">
				<div class="item-info">
					<h5>Why are they sent?</h5>

					<p>When marking workflows are used to mark coursework submissions, it can be difficult to
					   keep track of when feedback has been "completed" by the final marker and has been sent
					   to an administrator, so that it can be released to students.</p>

					<p>Tabula will send a notification when the final marker selects relevant submissions and
					   clicks on the "Send to administrator" button.</p>

					<@bs3form.checkbox path="finaliseFeedbackNotificationEnabled">
						<@f.checkbox path="finaliseFeedbackNotificationEnabled" id="finaliseFeedbackNotificationEnabled" />
						Turn on these notifications
					</@bs3form.checkbox>
				</div>

				<div class="item-info">
					<h5>When are they sent?</h5>

					<ul>
						<li>Each time a final marker selects one or more submissions and chooses "send to administrator"</li>
					</ul>
				</div>

				<div class="item-info">
					<h5>Who will receive them?</h5>

					<p>Unless an individual has opted out of marking completed notifications, the following groups
						of people will receive notifications: </p>

					<ul>
						<li>
							<@bs3form.checkbox path="finaliseFeedbackNotificationNotifyModuleManagers">
								<@f.checkbox path="finaliseFeedbackNotificationNotifyModuleManagers" id="finaliseFeedbackNotificationNotifyModuleManagers" />
								Module Managers
							</@bs3form.checkbox>
						</li>

						<li>
							<@bs3form.checkbox path="finaliseFeedbackNotificationNotifyDepartmentAdministrators">
								<@f.checkbox path="finaliseFeedbackNotificationNotifyDepartmentAdministrators" id="finaliseFeedbackNotificationNotifyDepartmentAdministrators" />
								Departmental Administrators
							</@bs3form.checkbox>
						</li>
					</ul>

					<p>These notifications can be sent to all the selected groups of people, or only to the first group
						with someone in it (e.g. Departmental Administrators will only be notified if there are no Module
						Managers for that module).</p>

					<@bs3form.radio>
						<@f.radiobutton path="finaliseFeedbackNotificationNotifyFirstNonEmptyGroupOnly" value="false" />
						Notify all selected groups of people
					</@bs3form.radio>
					<@bs3form.radio>
						<@f.radiobutton path="finaliseFeedbackNotificationNotifyFirstNonEmptyGroupOnly" value="true" />
						Only notify the first matching group of people
					</@bs3form.radio>
				</div>
			</div>
		</div>

		<@bs3form.form_group>
			<input type="submit" value="Save" class="btn btn-primary">
			<#if (returnTo!"")?length gt 0>
				<#assign cancelDestination=returnTo />
			<#else>
				<#assign cancelDestination><@routes.admin.departmenthome department=department /></#assign>
			</#if>
			<a class="btn btn-default" href="${cancelDestination}">Cancel</a>
		</@bs3form.form_group>
	</@f.form>
</#escape>