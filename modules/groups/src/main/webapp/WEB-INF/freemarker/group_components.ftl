<#ftl strip_text=true />
<#-- Common template parts for use in other small groups templates. -->

<#function module_anchor module>
	<#return "module-${module.code}" />
</#function>

<#macro event_schedule_info event>
<#if event.unscheduled>
	<span class="badge badge-warning use-tooltip" data-toggle="tooltip" data-placement="bottom" data-title="This event has not yet been scheduled">Not scheduled</span>
<#else>
	<#-- Weeks, day/time, location -->
	<@fmt.weekRanges event />,
	${event.day.shortName} <@fmt.time event.startTime /> - <@fmt.time event.endTime /><#if event.location?has_content>,</#if>
	${event.location!"[no location]"}
</#if>
</#macro>

<#-- Output a dropdown menu only if there is anything in it. -->
<#macro dropdown_menu text icon>
	<#-- Capture the content between the macro tags into a string -->
	<#local content><#nested /></#local>
	<#if content?trim?has_content>
	<a class="btn btn-medium dropdown-toggle" data-toggle="dropdown"><i class="icon-${icon}"></i> ${text} <span class="caret"></span></a>
	<ul class="dropdown-menu pull-right">
	${content}
	</ul>
	</#if>
</#macro>

<#-- module_info: takes a GroupsViewModel.ViewModules and renders out
 	a collection of modules with group sets and groups.

 	How the data is organised (which modules/sets/groups) is up to
 	the command generating the view model. No user checks in here!
 -->
<#macro module_info data>
<div class="small-group-modules-list">
<#list data.moduleItems as moduleItem>
<@single_module moduleItem data.canManageDepartment/>
</#list>
</div> <!-- small-group-modules-list-->
<#-- List of students modal -->
<div id="students-list-modal" class="modal fade">
</div>

</#macro>


<#macro single_module moduleItem canManageDepartment>

<#assign module=moduleItem.module />
<span id="${module_anchor(module)}-container">

<#assign has_groups=(moduleItem.setItems!?size gt 0) />
<#assign has_archived_groups=false />
<#list moduleItem.setItems as setItem>
	<#if setItem.set.archived>
		<#assign has_archived_groups=true />
	</#if>
</#list>

<a id="${module_anchor(module)}"></a>
<div class="module-info striped-section<#if has_groups> collapsible expanded</#if><#if canManageDepartment && !has_groups> empty</#if>"
	 data-name="${module_anchor(module)}">
	<div class="clearfix">

		<div class="btn-group section-manage-button">
			<@dropdown_menu "Manage" "wrench">
				<#if moduleItem.canManageGroups>
					<li><a href="<@routes.moduleperms module />">
						<i class="icon-user icon-fixed-width"></i> Edit module permissions
					</a></li>
					<li><a href="<@routes.createset module />"><i class="icon-group icon-fixed-width"></i> Add small groups</a></li>
				</#if>
				<#if has_archived_groups>
					<li><a class="show-archived-small-groups" href="#">
						<i class="icon-eye-open icon-fixed-width"></i> Show archived small groups
					</a>
					</li>
				</#if>
			</@dropdown_menu>
		</div>

		<h2 class="section-title with-button"><@fmt.module_name module /></h2>

		<#if moduleItem.setItems?has_content>
		<div class="striped-section-contents">
		<#list moduleItem.setItems as setItem>
		<span id="groupset-container-${setItem.set.id}">
          <@single_groupset setItem moduleItem/>
          </span>
        </#list>
        </div>
        </#if>
    </div>
</div> <!-- module-info striped-section-->
</span>
</#macro>


<#macro single_groupset setItem moduleItem>
			<#assign groupSet=setItem.set />
			<#if !groupSet.deleted>
				<div class="item-info row-fluid<#if groupSet.archived> archived</#if> groupset-${groupSet.id}" >
				<#if setItem.viewerMustSignUp>
				  <form id="select-signup-${setItem.set.id}" method="post" action="<@routes.signup_to_group setItem.set />">
				</#if>
					<div class="span2">
						<h3 class="name">
							<small>
							${groupSet.name}
								<#if groupSet.archived>
									(Archived)
								</#if>
							</small>
							<#if setItem.viewerIsStudent >
								<#if !setItem.isStudentSignUp()
									 || (setItem.isStudentSignUp() && !setItem.set.allowSelfGroupSwitching)
									 || (setItem.isStudentSignUp() && !setItem.set.openForSignups)
								>
									<span class="use-tooltip" title="You cannot change this group allocation via tabula. Please speak to your department if you need to change groups"><i class="icon-lock"></i></span>
								<#else>
									<span class="use-tooltip" title="This group is open for self sign-up"><i class="icon-unlock-alt"></i></span>
								</#if>
							<#else>
								<#if setItem.isStudentSignUp()>
									<#if setItem.set.openForSignups>
										<span class="use-tooltip" title="This group is open for self sign-up"><i class="icon-unlock-alt"></i></span>
									<#else>
										<span class="use-tooltip" title="This group is closed for self sign-up"><i class="icon-lock"></i></span>
									</#if>
								<#else>
									<span class="use-tooltip" title="This is a manually allocated group"><i class="icon-random"></i></span>
								</#if>
							</#if>
						</h3>

						<span class="format">
						${groupSet.format.description}
						</span>
					</div>

						<div class="${moduleItem.canManageGroups?string('span8','span10')}">
						<#if allocated?? && allocated.id == groupSet.id>
							<div class="alert alert-success">
								<a class="close" data-dismiss="alert">&times;</a>
								<p>Changes saved.</p>
							</div>
						</#if>
						<#if notificationSentMessage??>
							<div class="alert alert-success">
								<a class="close" data-dismiss="alert">&times;</a>
								<p>${notificationSentMessage}</p>
							</div>
						</#if>

						<#list setItem.groups as group>
							<div class="row-fluid group">
								<div class="span12">
									<#if setItem.viewerMustSignUp>
										<div class="pull-left ${group.full?string('use-tooltip" title="There are no spaces left on this group"','"')}>
											<input type="radio"
												name="group"
												value="${group.id}"
												${group.full?string(' disabled ','')}
												class="radio inline group-selection-radio"/>
										</div>
										<div style="margin-left: 20px;">
									<#else>
										<div>
									</#if>
										<h4 class="name">
											${group.name!""}
											<#if setItem.canViewMembers >
												<a href="<@routes.studentslist group />" class="ajax-modal" data-target="#students-list-modal">
													<small><@fmt.p (group.students.includeUsers?size)!0 "student" "students" /></small>
												</a>
											<#else>
												<small><@fmt.p (group.students.includeUsers?size)!0 "student" "students" /></small>
											</#if>
										</h4>

										<#if setItem.viewerIsStudent >
											<#if !setItem.isStudentSignUp()
												 || (setItem.isStudentSignUp() && !setItem.set.allowSelfGroupSwitching)
												 || (setItem.isStudentSignUp() && !setItem.set.openForSignups)
												 >
											  <form style="display: none;"> <!-- targetless form here to make the DOM match the student-sign-up version, for ease of testing -->
												 <input type="submit"
														disabled
														class="disabled btn btn-primary btn-medium pull-right use-tooltip"
														value="Leave" />
											  </form>
											<#else >
												<#if !setItem.viewerMustSignUp >
												<form id="leave-${setItem.set.id}" method="post" action="<@routes.leave_group setItem.set />" >
													<input type="hidden" name="group" value="${group.id}" />
													<input type="submit"
														   class="btn btn-primary pull-right use-tooltip"
														   title='Leave this group. You will need to sign up for a different group.'
														   value="Leave"/>
												 </form>
												</#if>
											</#if>
										</#if>


										<ul class="unstyled margin-fix">
											<#list group.events as event>
												<li class="clearfix">
													<#if features.smallGroupTeachingRecordAttendance && !event.unscheduled>
														<#if can.do("SmallGroupEvents.Register", event)>
															<div class="pull-right eventRegister">
																<form method="get" action="/groups/event/${event.id}/register">

																	<#if !returnTo??>
																		<#local returnTo=info.requestedUri />
																	</#if>
																	<input type="hidden" name="returnTo" value="${returnTo}" />
																	<span class="form-horizontal">
																		<#if weekRangeSelectFormatter(event)?has_content>
																			<@fmt.weekRangeSelect event />
																			<button type="submit" class="btn btn-small btn-primary register-button">Record</button>
																		<#else>
																			<button type="button" class="btn btn-small btn-primary register-button disabled use-tooltip" title="There are no events running in this term">Record</button>
																		</#if>
																	</span>
																</form>
															</div>
														</#if>
													</#if>
													<#-- Tutor, weeks, day/time, location -->
													<div class="eventWeeks">
														<#if setItem.canViewTutors && event.tutors?? >
															<h6>Tutor<#if (event.tutors.size > 1)>s</#if>:
																<#if (event.tutors.size < 1)>[no tutor]</#if>
															<#list event.tutors.users as tutor>${tutor.fullName}<#if tutor_has_next>, </#if></#list>
															</h6>
														</#if>
														<@event_schedule_info event />
													</div>
												</li>
											</#list>
										</ul>
									</div>
								</div>
							</div>
						</#list>
						<#if setItem.viewerMustSignUp>
							<input type="submit" class="btn btn-primary pull-right sign-up-button" value="Sign Up"/>
							</form>
                        </#if>
						<#-- Only show warnings to users that can do somthing about them -->
						<#if moduleItem.canManageGroups>
							<#assign unallocatedSize = groupSet.unallocatedStudentsCount />
							<#if unallocatedSize gt 0>
								<div class="alert">
									<i class="icon-info-sign"></i> <@fmt.p unallocatedSize "student has" "students have" /> not been allocated to a group
								</div>
							</#if>

							<#if groupSet.hasAllocated >
								 <#-- not released at all -->
								  <#if (!groupSet.releasedToStudents && !groupSet.releasedToTutors)>
								<p class="alert">
									<i class="icon-info-sign"></i> Notifications have not been sent for these groups
								</p>
								 <#-- only released to tutors-->
								 <#elseif (!groupSet.releasedToStudents && groupSet.releasedToTutors)>
								  <p class="alert">
									   <i class="icon-info-sign"></i> Notifications have not been sent to students for these groups
								   </p>
								  <#-- only released to students-->
								  <#elseif (groupSet.releasedToStudents && !groupSet.releasedToTutors)>
									  <p class="alert">
										  <i class="icon-info-sign"></i> Notifications have not been sent to tutors for these groups
									  </p>
								 </#if>
							</#if>
						</#if>
                    </div>

                    <#if moduleItem.canManageGroups>
                    <div class="span2">
                        <div class="btn-toolbar pull-right">
                            <div class="btn-group">

                                <@dropdown_menu "Actions" "cog">
                                    <li><a href="<@routes.editset groupSet />"><i class="icon-wrench icon-fixed-width"></i> Edit properties</a></li>
                                     <#if features.smallGroupTeachingStudentSignUp>
										 <#if groupSet.openForSignups>
										 <li  ${(groupSet.allocationMethod.dbValue == "StudentSignUp")?string
                                         		   (''," class='disabled use-tooltip' title='Not a self-signup group' ")
                                         }>
                                         <a  class="close-group-link" data-toggle="modal" data-target="#modal-container"
                                         href="<@routes.closeset groupSet />"><i class="icon-lock icon-fixed-width"></i> Close</a></li>

										 <#else>
										<li  ${(groupSet.allocationMethod.dbValue == "StudentSignUp")?string
												   (''," class='disabled use-tooltip' title='Not a self-signup group' ")
										}>
										<a  class="open-group-link" data-toggle="modal" data-target="#modal-container"
										href="<@routes.openset groupSet />"><i class="icon-unlock-alt icon-fixed-width"></i> Open</a></li>
										</#if>
									</#if>
                                    <li><a href="<@routes.allocateset groupSet />"><i class="icon-random icon-fixed-width"></i> Allocate students</a></li>
                                    <li ${groupSet.fullyReleased?string(" class='disabled use-tooltip' title='Already notified' ",'')} >
                                            <a class="notify-group-link" data-toggle="modal" data-target="#modal-container" href="<@routes.releaseset groupSet />">
                                            <i class="icon-envelope-alt icon-fixed-width"></i>
                                            Notify
                                        </a></li>
                                    <li>
                                        <#if groupSet.archived>
                                           <#assign archive_caption>Unarchive groups</#assign>
                                        <#else>
                                            <#assign archive_caption>Archive groups</#assign>
                                        </#if>

                                        <#assign archive_url><@routes.archiveset groupSet /></#assign>

                                        <@fmt.permission_button permission='SmallGroups.Archive' scope=groupSet.module action_descr='${archive_caption}'?lower_case classes='archive-group-link ajax-popup' href=archive_url
                                        						tooltip='Archive small group' data_attr='data-popup-target=.btn-group data-container=body'>
                                        <i class="icon-folder-close icon-fixed-width"></i> ${archive_caption}
                                        </@fmt.permission_button>
                                    </a></li>
                                </@dropdown_menu>
                            </div>
                        </div>
                    </div>
                    </#if>

					<#if setItem.viewerMustSignUp>
					</form>
                    </#if>
                </div>
            </#if>
</#macro>
