<#assign department=allocateStudentsToRelationshipCommand.department />
<#assign mappingById=allocateStudentsToRelationshipCommand.mappingById />

<#macro student_item profile bindpath="">
	<li class="student well well-small"
	data-f-gender="${(profile.gender.dbValue)!}"
	data-f-route="${(profile.mostSignificantCourseDetails.route.code)!}"
	data-f-year="${(profile.mostSignificantCourseDetails.latestStudentCourseYearDetails.yearOfStudy)!}">
		<div class="profile clearfix">
			<div class="name">
				<h6>${profile.fullName}</h6>
				${(profile.mostSignificantCourseDetails.route.name)!profile.homeDepartment.name}
			</div>
		</div>
		<input type="hidden" name="${bindpath}" value="${profile.universityId}" />
	</li>
</#macro>

<#escape x as x?html>
	<h1>Allocate students</h1>
	<h4><span class="muted">to</span> ${relationshipType.description}s</h4>

	<noscript>
		<div class="alert">This page requires Javascript.</div>
	</noscript>

	<div class="alert">
		<button type="button" class="close" data-dismiss="alert">&times;</button>
		<h4>Students with multiple ${relationshipType.agentRole}s</h4>

		<p>This page expects students to have exactly one ${relationshipType.agentRole}. If you allocate a student to a ${relationshipType.agentRole},
		it will remove any existing ${relationshipType.agentRole}s that the student has. This makes this page unsuitable for students who
		are expected to have multiple ${relationshipType.agentRole}s.</p>

		<p>We hope to change this behaviour in the future to make it easier to manage ${relationshipType.agentRole}s for students.</p>
	</div>

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
			<div class="fix-area">
			<@f.form method="post" action="${submitUrl}" commandName="allocateStudentsToRelationshipCommand" cssClass="form-horizontal">
			<div class="tabula-dnd"
					 data-item-name="student"
					 data-text-selector=".name h6"
					 data-use-handle="false"
					 data-selectables=".students .drag-target"
					 data-scroll="true"
					 data-remove-tooltip="Remove this student from this ${relationshipType.agentRole}">
				<div class="fix-header pad-when-fixed">
					<div class="btn-toolbar">
						<a class="random btn" data-toggle="randomise" data-disabled-on="empty-list"
						   href="#" >
							<i class="icon-random"></i> Randomly allocate
						</a>
						<a href="#" title="" class="btn use-popover .tabulaPopover-init" data-disabled-on="no-allocation" data-title="Remove All Allocations" data-html="true" data-trigger="hover"
						   data-content=
							'<div class="alert" >Are you sure you want to remove all existing allocations
							including ${department.name} students allocated by other departments?</div>
							<button id="confirm-remove-all" type="button" class="btn btn-primary">Confirm</button>'
						   data-original-title="Remove All Allocations">
							<i class="icon-arrow-left"></i> Remove all
							<div id="remove-all-tutors" data-toggle="return"></div>
						</a>
					</div>

				<div class="row-fluid">
					<div class="span5"><h3>Students</h3></div>
					<div class="span2">
					</div>
					<div class="span5">
						<button type="button" class="btn btn-primary pull-right" data-toggle="modal" data-target="#add-agents">Add ${relationshipType.agentRole}s</button>
						<h3>${relationshipType.agentRole?cap_first}s</h3>

					</div>

				</div>




				</div><!-- end fix-header -->

				<div class="row-fluid fix-on-scroll-container">
					<div class="span5">
						<div id="studentslist"
								 class="students tabula-filtered-list"
								 data-item-selector=".student-list li">
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
						<div class="direction-icon">
							<i class="icon-arrow-right"></i>
						</div>
					</div>

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
								<@form.flexipicker path="additionalAgents" placeholder="User name" membersOnly="true" list=true multiple=true />
							</@form.labelled_row>
						</div>

						<div class="modal-footer">
							<button type="button" class="btn btn-primary refresh-form">Add</button>
						</div>
					</div>

					<div class="span5">
						<div id="agentslist" class="agents fix-on-scroll clearfix">


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

										<div class="well-count">
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

					<div class="submit-buttons fix-footer">
						<button type="button" class="btn btn-primary" data-toggle="modal" data-target="#notify-modal">Save</button>
						<a href="<@routes.home />" class="btn">Cancel</a> <#-- TODO better url -->
					</div>



			</@f.form>
			</div><!-- end fix-area -->

			</div><!-- end 1st tab -->

			<div class="tab-pane" id="allocatestudents-tab2">
				<#include "upload_form.ftl" />
			</div><!-- end 2nd tab-->

		</div><!-- end tab-content -->

	</div> <!-- end tabbable -->


	<script type="text/javascript">
		(function($) {

			<!-- TAB-1266 - warning popup for 'Remove All' button -->
			$('body').on('click','.popover #confirm-remove-all', function() {
				$('#remove-all-tutors').trigger('click');
				$(this).closest('.popover').find('.close').trigger('click');
			});

			<!--TAB-1008 - fix scrolling bug when student list is shorter than the group list-->
			$('#studentslist').css('min-height', function() {
				return $('#agentslist').outerHeight();
			});

			var fixHeaderFooter = $('.fix-area').fixHeaderFooter();
			var singleColumnDragTargetHeight =  $('#agentslist .drag-target').outerHeight(true);

			$(window).scroll(function() {
				fixHeaderFooter.fixDirectionIcon();
				fixHeaderFooter.fixTargetList('#agentslist'); // eg. personal tutors column
			});

			// onload reformat agents layout
			formatAgentsLayout();

			// debounced on window resize, reformat agents layout...
			on_resize(formatAgentsLayout());

			// when new agents are added, reformat agents layout...
			$('#add-agents').on('hidden', formatAgentsLayout());

			function formatAgentsLayout() {
				var agentslist = $('#agentslist');
				var heightOfSingleColumnList = agentslist.find('.drag-target').length * singleColumnDragTargetHeight;

				if(agentslist.height() > fixHeaderFooter.viewableArea()) {
					agentslist.addClass('drag-list-two-col');
				} else if(fixHeaderFooter.viewableArea() > heightOfSingleColumnList) {
					agentslist.removeClass('drag-list-two-col');
				}
			}


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
				formatAgentsLayout(); // after deleting item from list, re-format the grid layout for the agents
			});

			// When the return list has changed, make sure the filter is re-run
			$('.return-list').on('changed.tabula', function(e) {
				// Make sure it exists before doing it
				var filter = $('.tabula-filtered-list').data('tabula-filtered-list');
				if (filter) {
					filter.filter();
				}
			});


			// debounced on_resize to avoid retriggering while resizing window
			function on_resize(c,t) {
				onresize = function() {
					clearTimeout(t);
					t = setTimeout(c,100)
				};
				return c;
			}

		})(jQuery);
	</script>

</#escape>