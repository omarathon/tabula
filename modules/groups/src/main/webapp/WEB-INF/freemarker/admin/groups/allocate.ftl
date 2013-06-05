<#assign set=allocateStudentsToGroupsCommand.set />
<#assign mappingById=allocateStudentsToGroupsCommand.mappingById />
<#assign membersById=allocateStudentsToGroupsCommand.membersById />

<#macro student_item student bindpath="">
	<#assign profile = membersById[student.warwickId]!{} />
	<li class="student well well-small">
		<div class="profile clearfix">
			<div class="photo">
				<#if profile.universityId??>
					<img src="<@url page="/view/photo/${profile.universityId}.jpg" context="/profiles" />" />
				<#else>
					<img src="<@url resource="/static/images/no-photo.png" />" />
				</#if>
			</div>
			
			<div class="name">
				<h6>${profile.fullName!student.fullName}</h6>
				${(profile.studyDetails.route.name)!student.shortDepartment}
			</div>
		</div>
		<input type="hidden" name="${bindpath}" value="${student.userId}" />
	</li>
</#macro>

<#escape x as x?html>
	<h1>Assign students to ${set.name}</h1>
	
	<noscript>
		<div class="alert">This page requires Javascript.</div>
	</noscript>
	
	<p>Drag students onto a group to allocate them to it. Select multiple students by dragging a box around them.
		 You can also hold the <kbd class="keyboard-control-key">Ctrl</kbd> key to add to a selection.</p>
	
	<@spring.hasBindErrors name="allocateStudentsToGroupsCommand">
		<#if errors.hasErrors()>
			<div class="alert alert-error">
				<h3>Some problems need fixing</h3>
				<#if errors.hasGlobalErrors()>
					<#list errors.globalErrors as e>
						<div><@spring.message message=e /></div>
					</#list>
				<#else>
					<div>See the errors below.</div>
				</#if>
			</div>
		</#if>
	</@spring.hasBindErrors>
	
	<#assign submitUrl><@routes.allocateset set /></#assign>
	<@f.form method="post" action="${submitUrl}" commandName="allocateStudentsToGroupsCommand">
		<div class="btn-toolbar">
			<a class="random btn"
			   href="#" >
				<i class="icon-random"></i> Randomly allocate
			</a>
			<a class="return-items btn"
			   href="#" >
				<i class="icon-arrow-left"></i> Remove all
			</a>
		</div>
		<div class="row-fluid fix-on-scroll-container">
			<div class="span5">
				<div class="students">
					<h3>Students</h3>
					<div class="well student-list drag-target">
						<h4>Not allocated to a group</h4>
					
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
			<div class="span2">
				<#-- I, for one, welcome our new jumbo icon overlords -->
				<div class="direction-icon fix-on-scroll">
					<i class="icon-arrow-right"></i>
				</div>
			</div>
			<div class="span5">
				<div class="groups fix-on-scroll">
					<h3>Groups</h3>			
					<#list set.groups as group>
						<#assign existingStudents = mappingById[group.id]![] />
						<div class="drag-target well clearfix">
							<div class="group-header">
								<#assign popoverHeader>
									Students in ${group.name}
									<button type='button' onclick="jQuery('#show-list-${group.id}').popover('hide')" class='close'>&times;</button>
								</#assign>
								<#assign groupDetails>
									<ul class="unstyled">
										<#list group.events as event>
											<li>
												<#-- Tutor, weeks, day/time, location -->
	
												<@fmt.weekRanges event />,
												${event.day.shortName} <@fmt.time event.startTime /> - <@fmt.time event.endTime />,
												${event.location}
											</li>
										</#list>
									</ul>
								</#assign>
							
								<h4 class="name">
									${group.name}
								</h4>
								
								<div>
									<span class="drag-count">${existingStudents?size}</span> students
									
									<a id="show-list-${group.id}" class="show-list" data-title="${popoverHeader}" data-prelude="${groupDetails}" data-placement="left"><i class="icon-question-sign"></i></a>
								</div>
							</div>
		
							<ul class="drag-list hide" data-bindpath="mapping[${group.id}]">
								<#list existingStudents as student>
									<@student_item student "mapping[${group.id}][${student_index}]" />
								</#list>
							</ul>
						</div>
					</#list>
				</div>
			</div>
		</div>		
		
		<div class="submit-buttons">
			<input type="submit" class="btn btn-primary" value="Save">
			<a href="<@routes.depthome module />" class="btn">Cancel</a>
		</div>
	</@f.form>
	
</#escape>