<#assign tiles=JspTaglibs["/WEB-INF/tld/tiles-jsp.tld"]>
<!DOCTYPE html>
<html lang="en-GB" class="no-js">
<head>
	<meta charset="utf-8">
	<meta http-equiv="X-UA-Compatible" content="IE=edge">
	<meta name="viewport" content="width=device-width, initial-scale=1">

	<!-- Include any favicons here -->
	<meta name="theme-color" content="#5b3069">
	<!-- Use the brand colour of the site -->

	<title>Your page title</title>

	<!-- Lato web font -->
	<link href="//fonts.googleapis.com/css?family=Lato:300,400,700,300italic,400italic,700italic&amp;subset=latin,latin-ext"
		  rel="stylesheet" type="text/css">

	<!-- ID7 -->
	<@stylesheet "/static/css/id7.css" />
	<@stylesheet "/static/css/id7-fixes.css" />

	<!-- Default styling. You will probably want to replace with your own site.css -->
	<@stylesheet "/static/css/id7-theme.css" />

	<@script "/static/js/id7/id7-bundle.js" />

	<!-- HTML5 shim for IE8 support of HTML5 elements -->

	<!--[if lt IE 9]>
	<@script "/static/js/id7/vendor/html5shiv-3.7.2.min.js" />
	<![endif]-->
</head>
<body>
	<@tiles.insertAttribute name="body" />
</body>
</html>