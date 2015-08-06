## Populate request header X-Requested-URI with requested URI (including query string)
# If query string is empty, don't add a question mark
RewriteCond %{QUERY_STRING} ^$
RewriteRule .? - [E=REQ_URI:%{ENV:SCRIPT_URI}]
RewriteCond %{QUERY_STRING} !^$
RewriteRule .? - [E=REQ_URI:%{ENV:SCRIPT_URI}?%{QUERY_STRING}]
RequestHeader set X-Requested-URI "%{REQ_URI}e" env=!no-requested-uri
####

RewriteRule ^/favicon\.ico - [G,L]
RewriteRule \.php$ [G,L]

# static content suffixes
RewriteRule ^(/static/.*?)\.[0-9]{10,16}$ $1$2 [QSA]

# TAB-46 Rewrite legacy /courses to /coursework
RewriteRule ^/courses(.*)$ /coursework$1 [QSA,L,R=301]

# Add trailing slash because otherwise ensadness occurs
RewriteRule ^/exams$ /exams/ [QSA,L,R=301]
RewriteRule ^/coursework$ /coursework/ [QSA,L,R=301]
RewriteRule ^/profiles$ /profiles/ [QSA,L,R=301]
RewriteRule ^/scheduling$ /scheduling/ [QSA,L,R=301]
RewriteRule ^/groups$ /groups/ [QSA,L,R=301]
RewriteRule ^/attendance$ /attendance/ [QSA,L,R=301]

# Remove legacy /web context
RewriteRule ^/web(.*)$ $1 [QSA,L,R=301]

# Rewrite everything for coursework into the courses module
RewriteCond %{REQUEST_URI} !^/coursework/exams(.*)
RewriteRule ^/coursework/(.*)$ http://127.0.0.1:${proxy:port}/courses/$1 [QSA,P,L]

RewriteRule ^/exams/(.*)$ http://127.0.0.1:${proxy:port}/courses/exams/$1 [QSA,P,L]

# Rewrite scheduling directly to the scheduler instance
RewriteCond %{REQUEST_URI} ^/scheduling/(.*)
RewriteRule (.*) http://127.0.0.1:${scheduler:port}$1 [QSA,P,L]

# Rewrite API to the API instances
RewriteCond %{REQUEST_URI} ^/api/v1/(.*)
RewriteRule (.*) http://127.0.0.1:${api:port}$1 [QSA,P,L]

# Go straight to app server
RewriteCond %{REQUEST_URI} !^/errordocuments/(.*)
RewriteRule (.*) http://127.0.0.1:${proxy:port}$1 [QSA,P,L]