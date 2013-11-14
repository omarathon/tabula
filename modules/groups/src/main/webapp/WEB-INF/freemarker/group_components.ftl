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
				
				<#if can.do('SmallGroupEvents.Register', module)>
					<#assign module_attendance_url><@routes.moduleAttendance module /></#assign>
					<li>
						<@fmt.permission_button permission='SmallGroupEvents.Register' scope=module action_descr='view attendance' href=module_attendance_url
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
										
										<#if features.smallGroupTeachingRecordAttendance && can.do('SmallGroupEvents.Register', group) && group.hasScheduledEvents>
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
																				href=allocateset_url
																				classes='notify-group-link'
																				data_attr='data-toggle=modal data-target=#modal-container data-container=body'>
																				<i class="icon-envelope-alt icon-fixed-width"></i> Notify
													            </@fmt.permission_button>
													          </li>
                                        
                                    <#assign set_attendance_url><@routes.setAttendance groupSet /></#assign>
																		<li>
																			<@fmt.permission_button permission='SmallGroupEvents.Register' scope=groupSet action_descr='view attendance' href=set_attendance_url
																		  						tooltip='View attendance at groups' data_attr='data-popup-target=.btn-group data-container=body'>
																		  	<i class="icon-group icon-fixed-width"></i> Attendance
																		  </@fmt.permission_button>
																		</li>    
                                        
                                        
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

<#macro studentAttendanceRow student attendance instances group showStudent=true>
	<#local set = group.groupSet />
	<#local module = set.module />
	<#local department = module.department />
	<#local academicYear = set.academicYear />
	<#local missedCount = 0 />

	<tr>
		<#if showStudent><td class="nowrap" data-sortBy="${student.lastName}, ${student.firstName}">${student.fullName}</td></#if>
		<#list instances as instance>
			<#local state = mapGet(attendance, instance) />
		
			<td>
				<#if state.name == 'Attended'>
					<i class="icon-ok icon-fixed-width attended" title="${student.fullName} attended: <@instanceFormat instance academicYear department />"></i>
				<#elseif state.name == 'Missed'>
					<#local missedCount = missedCount + 1 />
					<i class="icon-remove icon-fixed-width unauthorised" title="${student.fullName} did not attend: <@instanceFormat instance academicYear department />"></i>
				<#elseif state.name == 'Late'> <#-- Late -->
					<i class="icon-warning-sign icon-fixed-width late" title="No data: <@instanceFormat instance academicYear department />"></i>
				<#else> <#-- Not recorded -->
					<i class="icon-minus icon-fixed-width" title="<@instanceFormat instance academicYear department />"></i>
				</#if>
			</td>
		</#list>
		<td>
			<span class="badge badge-<#if (missedCount > 2)>important<#elseif (missedCount > 0)>warning<#else>success</#if>">${missedCount}</span>
		</td>
	</tr>
</#macro>

<#macro singleGroupAttendance group instances studentAttendance singleStudent={} showRecordButtons=true>
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
							<#if can.do("SmallGroupEvents.Register", event)>
								<div class="eventRegister">
									<a class="btn btn-mini" href="<@routes.registerForWeek event week />" title="Record attendance for <@instanceFormat instance academicYear department />">
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
				<@studentAttendanceRow student=singleStudent attendance=studentAttendance instances=instances group=group showStudent=false />
			<#else>
				<#list studentAttendance?keys as student>
					<#local attendance = mapGet(studentAttendance, student) />
					<@studentAttendanceRow student=student attendance=attendance instances=instances group=group showStudent=true />
				</#list>
			</#if>
		</tbody>
	</table>
	
	<#if !singleStudent?has_content>
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
									<small><@fmt.p (group.students.includeUsers?size)!0 "student" "students" /></small>
								</a>
							<#else>
								<small><@fmt.p (group.students.includeUsers?size)!0 "student" "students" /></small>
							</#if>
						</h4>
						
						<span id="group-attendance-container-${group.id}">
							<#local attendanceInfo = mapGet(groups, group) />
			
							<@singleGroupAttendance group attendanceInfo.instances attendanceInfo.attendance />
			      </span>
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
								<small><@fmt.p (group.students.includeUsers?size)!0 "student" "students" /></small>
							</a>
						<#else>
							<small><@fmt.p (group.students.includeUsers?size)!0 "student" "students" /></small>
						</#if>
					</h4>
					
					<span id="group-attendance-container-${group.id}">
						<#local attendanceInfo = mapGet(groups, group) />
		
						<@singleGroupAttendance group attendanceInfo.instances attendanceInfo.attendance />
		      </span>
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