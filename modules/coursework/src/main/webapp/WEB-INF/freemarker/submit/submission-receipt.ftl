<#escape x as x?html>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
	<style type="text/css">
		body {font-family: "Helvetica Neue", Helvetica, Arial, sans-serif
		}
	</style>
	<title>${submission.assignment.module.name} (${submission.assignment.module.code?upper_case} ${submission.assignment.module.name}) - ${submission.assignment.name}</title>
</head>
<body>
	<div id="header">
		<img src="/static/images/logo-full-black.png" style="float: right;" />
	</div>

	<h2>${submission.assignment.module.code?upper_case} ${submission.assignment.module.name}</h2>
	<h2>${submission.assignment.name}</h2>
	<h3>Submission receipt for ${user.universityId}</h3>

	<div class="submission-receipt-container">
		<div class="submission-receipt">
			<p>Submission received <@fmt.date date=submission.submittedDate at=true seconds=true relative=false />.</p>
			<p>Submission ID: ${submission.id}</p>
			<#if submission.allAttachments??>
				<p>
					Uploaded attachments:
					<ul>
						<#list submission.allAttachments as attachment>
							<li>${attachment.name}</li>
						</#list>
					</ul>
				</p>
			</#if>
		</div>
	</div>
</body>
</html>
</#escape>