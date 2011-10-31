#!/usr/bin/bash

cd /var/src/web/sitebuilder2/docs/apache/conf
cvs update -d
cd /var/src

/opt/csw/bin/ruby /var/src/web/sitebuilder2/docs/apache/conf/deploy_apache_config.rb /var/src/web/sitebuilder2/docs/apache/conf/config.yaml.www2

