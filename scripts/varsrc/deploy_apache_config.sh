#!/usr/bin/bash

cd /var/src/checkout/
git pull

/opt/csw/bin/ruby /var/src/web/sitebuilder2/docs/apache/conf/deploy_apache_config.rb /var/src/web/sitebuilder2/docs/apache/conf/config.yaml.www2

