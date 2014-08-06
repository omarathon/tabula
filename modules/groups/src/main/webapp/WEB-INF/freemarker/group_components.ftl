<#ftl strip_text=true />

<#-- Common template parts for use in other small groups templates. -->

<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />

<#function module_anchor module>
	<#return "module-${module.code}" />
</#function>

<#macro event_schedule_info event>
<#if event.unscheduled>
	<span class="badge badge-warning use-tooltip" data-toggle="tooltip" data-placement="bottom" data-title="This event has not yet been scheduled">Not scheduled</span>
<#else>
	<#-- Weeks, day/time, location -->
	<#if event.title?has_content><span class="eventTitle">${event.title} - </span></#if>
	<@fmt.weekRanges event />,
	${event.day.shortName} <@fmt.time event.startTime /> - <@fmt.time event.endTime /><#if ((event.location.name)!)?has_content>,</#if>
	${(event.location.name)!"[no location]"}
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
<#macro module_info data expand_by_default=true>
<div class="small-group-modules-list">
	<#-- List of students modal -->
	<div id="students-list-modal" class="modal fade"></div>

	<#-- Immediately start waiting for collapsibles to load - don't wait to wire this handler in, because we initialise collapsibles before the DOM has loaded below -->
	<script type="text/javascript">
		jQuery(document.body).on('loaded.collapsible', '.module-info', function() {
			var $module = jQuery(this);
			Groups.zebraStripeGroups($module);
			Groups.wireModalButtons($module);
			AjaxPopup.wireAjaxPopupLinks($module);
			$module.find('.use-tooltip').tooltip();
			$module.find('.use-popover').tabulaPopover({
				trigger: 'click',
				container: '#container'
			});
		});
	</script>

	<#list data.moduleItems as moduleItem>
		<@single_module moduleItem=moduleItem canManageDepartment=data.canManageDepartment expand_by_default=expand_by_default />
		
		<#if !expand_by_default>
			<#-- If we're not expanding by default, initialise the collapsible immediate - don't wait for DOMReady -->
			<script type="text/javascript">
				GlobalScripts.initCollapsible(jQuery('#module-${module.code}').filter(':not(.empty)'));
			</script>
		</#if>
	</#list>
</div> <!-- small-group-modules-list-->

</#macro>


<#macro single_module moduleItem canManageDepartment expand_by_default=true>

<#assign module=moduleItem.module />
<span id="${module_anchor(module)}-container">

<#assign has_groups=(moduleItem.setItems!?size gt 0) />
<#assign has_archived_groups=false />
<#list moduleItem.setItems as setItem>
	<#if setItem.set.archived>
		<#assign has_archived_groups=true />
	</#if>
</#list>

<div id="${module_anchor(module)}" class="module-info striped-section<#if has_groups> collapsible<#if expand_by_default> expanded</#if></#if><#if canManageDepartment && !has_groups> empty</#if>"
	<#if has_groups && !expand_by_default>
		data-populate=".striped-section-contents"
		data-href="<@routes.modulehome module />"
	</#if>
	 data-name="${module_anchor(module)}">
	<div class="clearfix">

		<div class="btn-group section-manage-button">
			<@dropdown_menu "Manage" "wrench">
				<#if moduleItem.canManageGroups>
					<li><a href="<@routes.moduleperms module />">
						<i class="icon-user icon-fixed-width"></i> Edit module permissions
					</a></li>
					<li>
						<#assign create_url><@routes.createset module /></#assign>
						<@fmt.permission_button
							permission='SmallGroups.Create'
							scope=module
							action_descr='add small groups'
							href=create_url>
							<i class="icon-group icon-fixed-width"></i> Add small groups</a>
						</@fmt.permission_button>
					</li>
				</#if>
				
				<#if can.do('SmallGroupEvents.ViewRegister', module)>
					<#assign module_attendance_url><@routes.moduleAttendance module /></#assign>
					<li>
						<@fmt.permission_button permission='SmallGroupEvents.ViewRegister' scope=module action_descr='view attendance' href=module_attendance_url
					  						tooltip='View attendance at groups' data_attr='data-popup-target=.btn-group data-container=body'>
					  	<i class="icon-group icon-fixed-width"></i> Attendance
					  </@fmt.permission_button>
					</li>
				</#if>
				
				<#if moduleItem.canManageGroups && has_archived_groups>
					<li><a class="show-archived-small-groups" href="#">
						<i class="icon-eye-open icon-fixed-width"></i> Show archived small groups
					</a>
					</li>
				</#if>
			</@dropdown_menu>
		</div>

		<h2 class="section-title with-button"><@fmt.module_name module /></h2>

		<#if has_groups>
			<div class="striped-section-contents">
				<#if expand_by_default>
					<@groupsets_info moduleItem />
	      </#if>
      </div>
    </#if>
  </div>
</div> <!-- module-info striped-section-->
</span>
</#macro>

<#macro groupsets_info moduleItem>
	<#list moduleItem.setItems as setItem>
		<span id="groupset-container-${setItem.set.id}">
      <@single_groupset setItem moduleItem/>
    </span>
  </#list>
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
									<span class="use-tooltip" title="You cannot change this group allocation via Tabula. Please speak to your department if you need to change groups"><i class="icon-lock"></i></span>
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
								<#elseif setItem.isLinked()>
									<span class="use-tooltip" title="Allocations for this group are linked and reused"><i class="icon-link"></i></span>
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
													<small><@fmt.p (group.students.size)!0 "student" "students" /></small>
												</a>
											<#else>
												<small><@fmt.p (group.students.size)!0 "student" "students" /></small>
											</#if>
										</h4>
										
										<#if features.smallGroupTeachingRecordAttendance && can.do('SmallGroupEvents.ViewRegister', group) && group.hasScheduledEvents && group.groupSet.collectAttendance>
                    	<div class="pull-right">
                    		<a href="<@routes.groupAttendance group />" class="btn btn-primary btn-small">
                    			Attendance
                    		</a>
                    	</div>
                    </#if>

										<#if setItem.viewerIsStudent
												&& setItem.isStudentSignUp()
												&& setItem.set.allowSelfGroupSwitching
												&& setItem.set.openForSignups >
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


										<ul class="unstyled margin-fix">
											<#list group.events as event>
												<li class="clearfix">
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
									<i class="icon-info-sign"></i>  <a href="<@routes.unallocatedstudentslist setItem.set />" class="ajax-modal" data-target="#students-list-modal"><@fmt.p unallocatedSize "student has" "students have" /> not been allocated to a group</a>
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
                                    <li>
                                    	<#assign edit_url><@routes.editset groupSet /></#assign>
																			<@fmt.permission_button 
																				permission='SmallGroups.Update' 
																				scope=groupSet 
																				action_descr='edit small group properties' 
																				href=edit_url>
													            	<i class="icon-wrench icon-fixed-width"></i> Edit properties
													            </@fmt.permission_button>
                                    </li>
                                     <#if features.smallGroupTeachingStudentSignUp>
																			 <#if groupSet.openForSignups>
																			 	<li ${(groupSet.allocationMethod.dbValue == "StudentSignUp")?string
									                              (''," class='disabled use-tooltip' title='Not a self-signup group' ")
									                      }>
									                      	<#assign closeset_url><@routes.closeset groupSet /></#assign>
																					<@fmt.permission_button 
																						permission='SmallGroups.Update' 
																						scope=groupSet 
																						classes='close-group-link'
																						action_descr='close small group' 
																						href=closeset_url
																						data_attr='data-toggle=modal data-target=#modal-container'>
															            	<i class="icon-lock icon-fixed-width"></i> Close
															            </@fmt.permission_button>
									                     	</li>
																			 <#else>
																			 	<li ${(groupSet.allocationMethod.dbValue == "StudentSignUp")?string
									                              (''," class='disabled use-tooltip' title='Not a self-signup group' ")
									                      }>
									                      	<#assign openset_url><@routes.openset groupSet /></#assign>
																					<@fmt.permission_button 
																						permission='SmallGroups.Update' 
																						scope=groupSet 
																						classes='open-group-link'
																						action_descr='open small group' 
																						href=openset_url
																						data_attr='data-toggle=modal data-target=#modal-container data-container=body'>
															            	<i class="icon-unlock-alt icon-fixed-width"></i> Open
															            </@fmt.permission_button>
									                     	</li>
																			</#if>
																		</#if>
                                    <li>
                                    	<#assign allocateset_url><@routes.allocateset groupSet /></#assign>
																			<@fmt.permission_button 
																				permission='SmallGroups.Allocate' 
																				scope=groupSet 
																				action_descr='allocate students' 
																				href=allocateset_url>
																				<i class="icon-random icon-fixed-width"></i> Allocate students
													            </@fmt.permission_button>
                                    </li>
                                    <li ${groupSet.fullyReleased?string(" class='disabled use-tooltip' title='Already notified' ",'')} >
                                    	<#assign notifyset_url><@routes.releaseset groupSet /></#assign>
																			<@fmt.permission_button 
																				permission='SmallGroups.Update' 
																				scope=groupSet 
																				action_descr='notify students and staff' 
																				href=notifyset_url
																				classes='notify-group-link'
																				data_attr='data-toggle=modal data-target=#modal-container data-container=body'>
																				<i class="icon-envelope-alt icon-fixed-width"></i> Notify
													            </@fmt.permission_button>
													          </li>
                                        
                                    <#if groupSet.collectAttendance>
	                                    <#assign set_attendance_url><@routes.setAttendance groupSet /></#assign>
																			<li>
																				<@fmt.permission_button permission='SmallGroupEvents.ViewRegister' scope=groupSet action_descr='view attendance' href=set_attendance_url
																			  						tooltip='View attendance at groups' data_attr='data-popup-target=.btn-group data-container=body'>
																			  	<i class="icon-group icon-fixed-width"></i> Attendance
																			  </@fmt.permission_button>
																			</li>
																		</#if>    
                                        
                                        
                                    <li>
                                        <#if groupSet.archived>
                                           <#assign archive_caption>Unarchive groups</#assign>
                                        <#else>
                                            <#assign archive_caption>Archive groups</#assign>
                                        </#if>

                                        <#assign archive_url><@routes.archiveset groupSet /></#assign>

                                        <@fmt.permission_button permission='SmallGroups.Archive' scope=groupSet action_descr='${archive_caption}'?lower_case classes='archive-group-link ajax-popup' href=archive_url
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

<#macro instanceFormat instance academicYear department><#compress>
	<#local event = instance._1() />
	<#local week = instance._2() />
	${event.day.shortName} <@fmt.time event.startTime />, <@fmt.singleWeekFormat week academicYear department />
</#compress></#macro>

<#macro studentAttendanceRow student attendance notes instances group showStudent=true>
	<#local set = group.groupSet />
	<#local module = set.module />
	<#local department = module.department />
	<#local academicYear = set.academicYear />
	<#local missedCount = 0 />

	<tr>
		<#if showStudent>
			<td class="nowrap" data-sortBy="${student.lastName}, ${student.firstName}">
				${student.fullName}&nbsp;<@pl.profile_link student.warwickId! />
			</td>
		</#if>
		<#list instances as instance>
			<#local state = mapGet(attendance, instance) />
			<#local title><@instanceFormat instance academicYear department /></#local>

			<#if state.name == 'Attended'>
				<#local class = "icon-ok attended" />
				<#local title = "${student.fullName} attended: " + title />
			<#elseif state.name == 'MissedAuthorised'>
				<#local class = "icon-remove-circle authorised" />
				<#local title = "${student.fullName} did not attend (authorised absence): " + title />
			<#elseif state.name == 'MissedUnauthorised'>
				<#local class = "icon-remove unauthorised" />
				<#local title = "${student.fullName} did not attend (unauthorised): " + title />
				<#local missedCount = missedCount + 1 />
			<#elseif state.name == 'Late'>
				<#local class = "icon-warning-sign late" />
				<#local title = "No data: " + title />
			<#else>
				<#local class = "icon-minus" />
			</#if>

			<#local titles = [title] />

			<#if mapGet(notes, instance)??>
				<#local studentNote = mapGet(notes, instance) />
				<#local note>
				${studentNote.truncatedNote}
					<#if (studentNote.truncatedNote?length > 0)>
						<br/>
					</#if>
					<a class='attendance-note-modal' href='<@routes.viewNote studentNote.student studentNote.occurrence />'>View attendance note</a>
				</#local>
				<#local titles = titles + [note] />
			</#if>

			<#local renderedTitle>
				<#list titles as t>
					<#if (titles?size > 1)>
						<p>${t}</p>
					<#else>
					${t}
					</#if>
				</#list>
			</#local>

			<td>
				<i class="use-popover icon-fixed-width ${class}" data-content="${renderedTitle}" data-html="true"></i>
			</td>
		</#list>
		<td>
			<span class="badge badge-<#if (missedCount > 2)>important<#elseif (missedCount > 0)>warning<#else>success</#if>">${missedCount}</span>
		</td>
	</tr>
</#macro>

<#macro singleGroupAttendance group instances studentAttendance attendanceNotes singleStudent={} showRecordButtons=true>
	<#local set = group.groupSet />
	<#local module = set.module />
	<#local department = module.department />
	<#local academicYear = set.academicYear />
	
	<table id="group_attendance_${group.id}" class="table table-striped table-bordered table-condensed attendance-table">
		<thead>
			<tr>
				<#if !singleStudent?has_content><th class="sortable nowrap">Student</th></#if>
				<#list instances as instance>
					<#local event = instance._1() />
					<#local week = instance._2() />
					
					<th class="instance-date-header">
						<div class="instance-date use-tooltip" title="Tutor<#if (event.tutors.size > 1)>s</#if>: <#if (event.tutors.size < 1)>[no tutor]</#if><#list event.tutors.users as tutor>${tutor.fullName}<#if tutor_has_next>, </#if></#list>">
							<@instanceFormat instance academicYear department />
						</div>
						
						<#if showRecordButtons && features.smallGroupTeachingRecordAttendance && !event.unscheduled>
							<#if can.do("SmallGroupEvents.ViewRegister", event)>
								<div class="eventRegister">
									<a class="btn btn-mini" href="<@routes.registerForWeek event week/>&returnTo=${(info.requestedUri!"")?url}" title="Record attendance for <@instanceFormat instance academicYear department />">
										Record
									</a>
								</div>
							</#if>
						</#if>
					</th>
				</#list>
				<th class="sortable"></th>
			</tr>
		</thead>
		<tbody>
			<#if singleStudent?has_content>
				<@studentAttendanceRow student=singleStudent attendance=studentAttendance notes=attendanceNotes instances=instances group=group showStudent=false />
			<#else>
				<#list studentAttendance?keys as student>
					<#local attendance = mapGet(studentAttendance, student) />
					<#local notes = mapGet(attendanceNotes, student) />
					<@studentAttendanceRow student=student attendance=attendance notes=notes instances=instances group=group showStudent=true />
				</#list>
			</#if>
		</tbody>
	</table>
	
	<#if !singleStudent?has_content && studentAttendance?keys?size gt 0>
	<script type="text/javascript">
		jQuery(function($){
			$('#group_attendance_${group.id}')
				.sortableTable({
					sortList: [[$('#group_attendance_${group.id} th').length - 1,1]],
					textExtraction: function(node) {
						var $el = $(node);
						if ($el.data('sortby')) {
							return $el.data('sortby');
						} else {
							return $el.text().trim();
						}
					}
				});
		});
	</script>
	</#if>
</#macro>

<#macro single_groupset_attendance groupSet groups>
	<#local module = groupSet.module />

	<div class="striped-section"
		 data-name="${module_anchor(module)}">
		<div class="clearfix">
			<h2 class="section-title">${groupSet.name}</h2>
	
			<div class="striped-section-contents">
				<#list groups?keys as group>
					<div class="item-info clearfix">
						<h4 class="name">
							${group.name}
							<#if can.do("SmallGroups.ReadMembership", group)>
								<a href="<@routes.studentslist group />" class="ajax-modal" data-target="#students-list-modal">
									<small><@fmt.p (group.students.size)!0 "student" "students" /></small>
								</a>
							<#else>
								<small><@fmt.p (group.students.size)!0 "student" "students" /></small>
							</#if>
						</h4>
						
						<div id="group-attendance-container-${group.id}">
							<#local attendanceInfo = mapGet(groups, group) />
			
							<@singleGroupAttendance group attendanceInfo.instances attendanceInfo.attendance attendanceInfo.notes />
			      </div>
		      </div>
	      </#list>
	    </div>
	  </div>
	</div> <!-- attendance-info striped-section-->
</#macro>

<#macro single_module_attendance module sets>
	<div class="striped-section"
		 data-name="${module_anchor(module)}">
		<div class="clearfix">
			<h2 class="section-title"><@fmt.module_name module /></h2>
	
			<div class="striped-section-contents">
				<@single_module_attendance_contents module sets />
	    </div>
	  </div>
	</div> <!-- attendance-info striped-section-->
</#macro>

<#macro single_module_attendance_contents module sets>
	<#list sets?keys as set>
		<#local groups = mapGet(sets, set) />
	
		<div class="item-info row-fluid clearfix">
			<div class="span2">
				<h3 class="name">
					<small>
					${set.name}
						<#if set.archived>
							(Archived)
						</#if>
					</small>
				</h3>

				<span class="format">
					${set.format.description}
				</span>
			</div>
		
			<div class="span10">
				<#list groups?keys as group>
					<h4 class="name">
						${group.name}
						<#if can.do("SmallGroups.ReadMembership", group)>
							<a href="<@routes.studentslist group />" class="ajax-modal" data-target="#students-list-modal">
								<small><@fmt.p (group.students.size)!0 "student" "students" /></small>
							</a>
						<#else>
							<small><@fmt.p (group.students.size)!0 "student" "students" /></small>
						</#if>
					</h4>
					
					<div id="group-attendance-container-${group.id}">
						<#local attendanceInfo = mapGet(groups, group) />
		
						<@singleGroupAttendance group attendanceInfo.instances attendanceInfo.attendance attendanceInfo.notes />
		      </div>
		    </#list>
	    </div>
    </div>
  </#list>
</#macro>

<#macro department_attendance department modules>
	<div class="small-group-modules-list">
	<#list modules as module>
		<span id="${module_anchor(module)}-container">
			<#local has_groups=(module.groupSets!?size gt 0) />
			<#local has_archived_groups=false />
			<#list module.groupSets as set>
				<#if set.archived>
					<#local has_archived_groups=true />
				</#if>
			</#list>
			
			<a id="${module_anchor(module)}"></a>
			<div class="striped-section collapsible"
				 data-populate=".striped-section-contents"
				 data-href="<@routes.moduleAttendance module />"
				 data-name="${module_anchor(module)}">
				<div class="clearfix">		
					<h2 class="section-title with-button"><@fmt.module_name module /></h2>
			
					<div class="striped-section-contents">
			    </div>
			  </div>
			</div> <!-- module-info striped-section-->
		</span>
	</#list>
	</div> <!-- small-group-modules-list-->
</#macro>

<#function sortClass field command>
	<#list command.sortOrder as order>
		<#if order.propertyName == field>
			<#if order.ascending>
				<#return "headerSortDown" />
			<#else>
				<#return "headerSortUp" />
			</#if>
		</#if>
	</#list>
	<#return "" />
</#function>

<#macro manageStudentTable
	membershipItems
	doSorting=false
	command=""
	checkboxName=""
	onlyShowCheckboxForStatic=false
	checkAll=false
	showRemoveButton=false
	showResetButton=false
>

	<#if (membershipItems?size > 0)>

		<table class="manage-student-table table table-bordered table-striped table-condensed table-hover table-sortable table-checkable sticky-table-headers tabula-darkRed tablesorter sb-no-wrapper-table-popout">
			<thead>
			<tr>
				<th style="width: 50px;" <#if doSorting> class="${sortClass("source", command)} sortable" data-field="source"</#if>>Source</th>
				<th <#if doSorting> class="${sortClass("firstName", command)} sortable" data-field="firstName"</#if>>First name</th>
				<th <#if doSorting> class="${sortClass("lastName", command)} sortable" data-field="lastName"</#if>>Last name</th>
				<th <#if doSorting> class="${sortClass("universityId", command)} sortable" data-field="universityId"</#if>>ID</th>
				<th <#if doSorting> class="${sortClass("userId", command)} sortable" data-field="userId"</#if>>User</th>
				<#if checkboxName?has_content>
					<th style="width: 65px; padding-right: 5px;" <#if checkAll>class="for-check-all"</#if>>
						<#if showRemoveButton>
							<input class="btn btn-warning hideOnClosed btn-small use-tooltip"
							  <#if findCommandResult.membershipItems?size == 0>disabled</#if>
							  type="submit"
							  name="${ManageDepartmentSmallGroupsMappingParameters.manuallyExclude}"
							  value="Remove"
							  title="Remove selected students from these groups"
							  style="margin-left: 0.5em;"
							/>
						</#if>
						<#if (showResetButton && (editMembershipCommandResult.includedStudentIds?size > 0 || editMembershipCommandResult.excludedStudentIds?size > 0))>
							<input class="btn btn-warning hideOnClosed btn-small use-tooltip"
								   type="submit"
								   style="float: right; padding-left: 5px; padding-right: 5px; margin-left: 5px;"
								   name="${ManageDepartmentSmallGroupsMappingParameters.resetMembership}"
								   value="Reset"
								   data-container="body"
								   title="Restore the manually removed and remove the manually added students selected"
							/>
						</#if>
					</th>
				</#if>
			</tr>
			</thead>
			<tbody>
				<#list membershipItems as item>
					<tr class="${item.itemTypeString}">
						<td>
							<#if item.itemTypeString == "static">
								<span class="use-tooltip" title="Automatically linked from SITS" data-placement="right"><i class="icon-list-alt"></i></span>
							<#elseif item.itemTypeString == "exclude">
								<span class="use-tooltip" title="Removed manually, overriding SITS" data-placement="right"><i class="icon-ban-circle"></i></span>
							<#else>
								<span class="use-tooltip" title="Added manually" data-placement="right"><i class="icon-hand-up"></i></span>
							</#if>
						</td>
						<td>${item.firstName}</td>
						<td>${item.lastName}</td>
						<td>${item.universityId}</td>
						<td>${item.userId}</td>
						<#if checkboxName?has_content>
							<td>
								<#if !onlyShowCheckboxForStatic || item.itemTypeString == "static">
									<input type="checkbox" name="${checkboxName}" value="${item.universityId}" />
								</#if>
							</td>
						</#if>
					</tr>
				</#list>
			</tbody>
		</table>
	</#if>
</#macro>

<#macro set_wizard is_new current_step is_linked=false>
	<p class="progress-arrows">
		<#if is_linked && (current_step == 'students' || current_step == 'groups' || current_step == 'allocate')>
			<#local properties_url><#if is_new><@routes.createeditproperties smallGroupSet /><#else><@routes.editsetproperties smallGroupSet /></#if></#local>
			<@wizard_link
				label="Properties"
				is_first=true
				is_active=(current_step == 'properties')
				is_available=true
				tooltip="Save and edit properties"
				url=properties_url />

			<#local groups_url><#if is_new><@routes.createsetgroups smallGroupSet /><#else><@routes.editsetgroups smallGroupSet /></#if></#local>
			<@wizard_link
				label="Groups"
				is_first=false
				is_active=(current_step == 'groups')
				is_available=true
				tooltip="Save and edit groups"
				url=groups_url />

			<#local students_url><#if is_new><@routes.createsetstudents smallGroupSet /><#else><@routes.editsetstudents smallGroupSet /></#if></#local>
			<@wizard_link
				label="Students"
				is_first=false
				is_active=(current_step == 'students')
				is_available=true
				tooltip="Save and edit students"
				url=students_url />

			<#local events_url><#if is_new><@routes.createsetevents smallGroupSet /><#else><@routes.editsetevents smallGroupSet /></#if></#local>
			<@wizard_link
				label="Events"
				is_first=false
				is_active=(current_step == 'events')
				is_available=true
				tooltip="Save and edit events"
				url=events_url />

			<#local allocate_url><#if is_new><@routes.createsetallocate smallGroupSet /><#else><@routes.editsetallocate smallGroupSet /></#if></#local>
			<@wizard_link
				label="Allocate"
				is_first=false
				is_active=(current_step == 'allocate')
				is_available=true
				tooltip="Save and allocate students to groups"
				url=allocate_url />
		<#else>
			<#local properties_action><#if is_new>${ManageSmallGroupsMappingParameters.createAndEditProperties}<#else>${ManageSmallGroupsMappingParameters.editAndEditProperties}</#if></#local>
			<@wizard_button
				label="Properties"
				is_first=true
				is_active=(current_step == 'properties')
				is_available=true
				tooltip="Save and edit properties"
				action=properties_action />

			<#local groups_action><#if is_new>${ManageSmallGroupsMappingParameters.createAndAddGroups}<#else>${ManageSmallGroupsMappingParameters.editAndAddGroups}</#if></#local>
			<@wizard_button
				label="Groups"
				is_first=false
				is_active=(current_step == 'groups')
				is_available=true
				tooltip="Save and edit groups"
				action=groups_action />

			<#local students_action><#if is_new>${ManageSmallGroupsMappingParameters.createAndAddStudents}<#else>${ManageSmallGroupsMappingParameters.editAndAddStudents}</#if></#local>
			<@wizard_button
				label="Students"
				is_first=false
				is_active=(current_step == 'students')
				is_available=true
				tooltip="Save and edit students"
				action=students_action />

			<#local events_action><#if is_new>${ManageSmallGroupsMappingParameters.createAndAddEvents}<#else>${ManageSmallGroupsMappingParameters.editAndAddEvents}</#if></#local>
			<@wizard_button
				label="Events"
				is_first=false
				is_active=(current_step == 'events')
				is_available=true
				tooltip="Save and edit events"
				action=events_action />

			<#local allocate_action><#if is_new>${ManageSmallGroupsMappingParameters.createAndAllocate}<#else>${ManageSmallGroupsMappingParameters.editAndAllocate}</#if></#local>
			<@wizard_button
				label="Allocate"
				is_first=false
				is_active=(current_step == 'allocate')
				is_available=true
				tooltip="Save and allocate students to groups"
				action=allocate_action />
		</#if>
	</p>
</#macro>

<#macro wizard_button label is_first is_active is_available tooltip="" action="">
	<span class="arrow-right<#if !is_first> arrow-left</#if><#if is_active> active</#if><#if is_available && !is_active> use-tooltip</#if>" <#if is_available && !is_active>title="${tooltip}"</#if>><#compress>
		<#if is_available && !is_active>
			<button type="submit" class="btn btn-link" name="${action}">${label}</button>
		<#else>
			${label}
		</#if>
	</#compress></span>
</#macro>

<#macro wizard_link label is_first is_active is_available tooltip="" url="">
	<span class="arrow-right<#if !is_first> arrow-left</#if><#if is_active> active</#if><#if is_available && !is_active> use-tooltip</#if>" <#if is_available && !is_active>title="${tooltip}"</#if>><#compress>
		<#if is_available && !is_active>
			<a href="${url}">${label}</a>
		<#else>
			${label}
		</#if>
	</#compress></span>
</#macro>