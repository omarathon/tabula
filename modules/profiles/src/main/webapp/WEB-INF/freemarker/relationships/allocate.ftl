<#assign department=allocateStudentsToRelationshipCommand.department />
<#assign mappingById=allocateStudentsToRelationshipCommand.mappingById />
<#assign membersById=allocateStudentsToRelationshipCommand.membersById />



<#macro student_item profile bindpath="">
	<li class="student well well-small"
	data-f-gender="${(profile.gender.dbValue)!}"
	data-f-route="${(profile.mostSignificantCourseDetails.route.code)!}"
	data-f-year="${(profile.mostSignificantCourseDetails.latestStudentCourseYearDetails.yearOfStudy)!}">
		<div class="profile clearfix">
			<@fmt.member_photo profile "tinythumbnail" false />
			<div class="name">
				<h6>${profile.fullName}</h6>
				${(profile.mostSignificantCourseDetails.route.name)!profile.homeDepartment.name}
			</div>
		</div>
		<input type="hidden" name="${bindpath}" value="${profile.universityId}" />
	</li>
</#macro>

<#escape x as x?html>
	<h1>Allocate students to ${relationshipType.description}s</h1>
	
	<noscript>
		<div class="alert">This page requires Javascript.</div>
	</noscript>

	<div class="tabbable">
		<ul class="nav nav-tabs">
			<li class="active">
				<a href="#allocatestudents-tab1" data-toggle="tab">Drag and drop</a>
			</li>
			<li >
				<a href="#allocatestudents-tab2" data-toggle="tab">Upload spreadsheet</a>
			</li>
		</ul>

		<div class="tab-content">
			<div id="allocatestudents-tab1" class="tab-pane active">

			<p>Drag students onto a ${relationshipType.agentRole} to allocate them to the ${relationshipType.agentRole}. Select multiple students by dragging a box around them.
				 You can also hold the <kbd class="keyboard-control-key">Ctrl</kbd> key to add to a selection.</p>

			<@spring.hasBindErrors name="allocateStudentsToRelationshipCommand">
				<#if errors.hasErrors()>
					<div class="alert alert-error">
						<h3>Some problems need fixing</h3>
						<#if errors.hasGlobalErrors()>
							<#list errors.globalErrors as e>
								<div><@spring.message message=e /></div>
							</#list>
						<#elseif errors.hasFieldErrors('file')>
							<#list errors.getFieldErrors('file') as e>
								<div><@spring.message message=e /></div>
							</#list>
						<#else>
							<div>See the errors below.</div>
						</#if>
					</div>
				</#if>
			</@spring.hasBindErrors>

			<#assign submitUrl><@routes.relationship_allocate department relationshipType /></#assign>
			<@f.form method="post" action="${submitUrl}" commandName="allocateStudentsToRelationshipCommand" cssClass="form-horizontal">
			<div class="tabula-dnd" 
					 data-item-name="student" 
					 data-text-selector=".name h6"
					 data-use-handle="false"
					 data-selectables=".students .drag-target"
					 data-scroll="true"
					 data-remove-tooltip="Remove this student from this ${relationshipType.agentRole}">
				<div class="btn-toolbar">
					<a class="random btn" data-toggle="randomise" data-disabled-on="empty-list"
					   href="#" >
						<i class="icon-random"></i> Randomly allocate
					</a>
					<a class="return-items btn" data-toggle="return" data-disabled-on="no-allocation"
					   href="#" >
						<i class="icon-arrow-left"></i> Remove all
					</a>
				</div>
				<div class="row-fluid fix-on-scroll-container">
					<div class="span5">
						<div id="studentslist" 
								 class="students tabula-filtered-list"
								 data-item-selector=".student-list li">
							<h3>Students</h3>
							<div class="well ">
									<h4>Students with no ${relationshipType.agentRole}</h4>
									<#if features.personalTutorAssignmentFiltering>
										<div class="filter" id="filter-by-gender-controls">
											<select data-filter-attr="fGender">
												<option data-filter-value="*">Any Gender</option>
												<option data-filter-value="M">Male</option>
												<option data-filter-value="F">Female</option>
											</select>
										</div>
										<div class="filter" id="filter-by-year-controls">
											<select data-filter-attr="fYear">
												<option data-filter-value="*">Any Year of study</option>
												<#list allocateStudentsToRelationshipCommand.allMembersYears as year>
													<option data-filter-value="${year}">Year ${year}</option>
												</#list>
											</select>
										</div>
										<div class="filter" id="filter-by-route-controls">
											<select data-filter-attr="fRoute">
												<option data-filter-value="*">Any Route</option>
												<#list allocateStudentsToRelationshipCommand.allMembersRoutes as route>
													<option data-filter-value="${route.code}"><@fmt.route_name route /></option>
												</#list>
											</select>
										</div>
									</#if>
								<div class="student-list drag-target">
									<ul class="drag-list return-list unstyled" data-bindpath="unallocated">
										<@spring.bind path="unallocated">
											<#list status.actualValue as student>
												<@student_item student "${status.expression}[${student_index}]" />
											</#list>
										</@spring.bind>
									</ul>
								</div>
							</div>
						</div>
					</div>
					<div class="span2">
						<#-- I, for one, welcome our new jumbo icon overlords -->
						<div class="direction-icon fix-on-scroll">
							<i class="icon-arrow-right"></i>
						</div>
					</div>
					<div class="span5">
						<div id="agentslist" class="agents fix-on-scroll">
							<button type="button" class="btn btn-primary pull-right" data-toggle="modal" data-target="#add-agents">Add ${relationshipType.agentRole}s</button>
							
							<#-- Modal to add students manually -->
							<div class="modal fade hide" id="add-agents" tabindex="-1" role="dialog" aria-labelledby="add-agents-label" aria-hidden="true">
								<div class="modal-header">
									<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
									<h3 id="add-agents-label">Add ${relationshipType.agentRole}s</h3>
								</div>
					
								<div class="modal-body">
									<p>
										Lookup ${relationshipType.agentRole}s by typing their names, usercodes or university IDs below, then click <code>Add</code>.
									</p>
									
									<@form.labelled_row "additionalAgents" "${relationshipType.agentRole?cap_first}s">
										<@form.flexipicker path="additionalAgents" placeholder="User name" list=true multiple=true />
									</@form.labelled_row>
								</div>
					
								<div class="modal-footer">
									<button type="button" class="btn btn-primary refresh-form">Add</button>
								</div>
							</div>
						
							<h3>${relationshipType.agentRole?cap_first}s</h3>
							
							<#macro agent_item university_id full_name existing_students=[]>
								<div class="drag-target well clearfix agent-${university_id}">
									<div class="agent-header">
										<a href="#" class="delete pull-right" data-toggle="tooltip" title="Remove ${full_name}">
											<i class="icon-remove icon-large"></i>
										</a>
									
										<#local popoverHeader>${full_name}'s ${relationshipType.studentRole}s</#local>

										<h4 class="name">
											${full_name}
										</h4>

										<div>
											<#local count = existing_students?size />
											<span class="drag-count">${count}</span> <span class="drag-counted" data-singular="student" data-plural="students">student<#if count != 1>s</#if></span>

											<a id="show-list-${university_id}" class="show-list" title="View students" data-container=".agent-${university_id}" data-title="${popoverHeader}" data-placement="left"><i class="icon-question-sign"></i></a>
										</div>
									</div>

									<ul class="drag-list hide" data-bindpath="mapping[${university_id}]">
										<#list existing_students as student>
											<@student_item student "mapping[${university_id}][${student_index}]" />
										</#list>
									</ul>
								</div>
							</#macro>
							
							<#list allocateStudentsToRelationshipCommand.mapping?keys as agent>
								<#assign existingStudents = mappingById[agent.universityId]![] />
								
								<@agent_item agent.universityId agent.fullName existingStudents />
							</#list>
						</div>
					</div>
				</div>
			</div>
			
				<#include "_allocate_notifications_modal.ftl" />

				<div class="submit-buttons">
					<button type="button" class="btn btn-primary" data-toggle="modal" data-target="#notify-modal">Save</button>
					<a href="<@routes.home />" class="btn">Cancel</a> <#-- TODO better url -->
				</div>
			</@f.form>
			</div><!-- end 1st tab -->

			<div class="tab-pane" id="allocatestudents-tab2">
				<#include "upload_form.ftl" />
			</div><!-- end 2nd tab-->

		</div><!-- end tab-content -->

	</div> <!-- end tabbable -->

	<script type="text/javascript">
		(function($) {
			<!--TAB-1008 - fix scrolling bug when student list is shorter than the group list-->
			$('#studentslist').css('min-height', function() {
				return $('#agentslist').outerHeight();
			});
			
			$('.btn.refresh-form').on('click', function(e) {
				var $form = $(e.target).closest('form');
				$form.prepend($('<input />').attr({type: 'hidden', name: 'action', value: 'refresh'}));
				$form.submit();
			});
			
			$('#agentslist .agent-header > .delete').on('click', function(e) {
				e.preventDefault();
				e.stopPropagation();
				
				// Get the drag and drop instance
				var dnd = $('.tabula-dnd').data('tabula-dnd');
				var $container = $(this).closest('.drag-target')
				var $dragList = $container.find('.drag-list');
				
				dnd.batchMove([{
					target: $('.return-list'),
					items: $dragList.find('li'),
					sources: $dragList
				}]);
				
				$container.remove();
			});

			// When the return list has changed, make sure the filter is re-run			
			$('.return-list').on('changed.tabula', function(e) {
				// Make sure it exists before doing it
				var filter = $('.tabula-filtered-list').data('tabula-filtered-list');
				if (filter) {
					filter.filter();
				}
			});
		})(jQuery);
	</script>

</#escape>