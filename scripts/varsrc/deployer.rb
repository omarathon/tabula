require 'fileutils'
require 'rexml/document'
require 'uri'
require 'open-uri'
require 'yaml'
require 'net/http'
require 'time'

include REXML

def log(string)
  puts "[#{Time.now.iso8601}] #{string}"
end

class Deployment
  attr_reader :jbosses, :scheduler_jboss, :apaches

  # NOTE this doesn't run when loading from YAML
  def initialize(lockfile,jboss_instances,jboss_scheduler_instance,apache_instances,notification_emails)
     @lockfile=lockfile
     @jbosses=jboss_instances
     @scheduler_jboss=jboss_scheduler_instance
     @apaches=apache_instances 
     @notification_emails=notification_emails
     @build_status='starting'
  end
  
  def init
    log "Pumping JBosses full of fun"
    @jbosses.each do |name, group|
       group.each do |jboss|
         jboss.group_name = name
       end
    end  
  end
 
  def run
     if (preconditions_ok?) then
       set_running
       build_war
       deploy_war_to_scheduler
       deploy_static
       deploy_war
       unset_running
     end
  end
  
  # all jbosses in all groups, except scheduler
  def all_jbosses
    @jbosses.map{|name,group| group}.flatten
  end

  def print_status
    log ""
    puts "***********************************************************"
    status="#{@build_status}. \n   "
    status += "scheduler: #{@scheduler_jboss.state}  #{@scheduler_jboss.deployment_state} " 
    all_jbosses.each { |jboss| status += "#{jboss.name}: #{jboss.state} #{jboss.deployment_state}. " }
    status +="\n   "
    @apaches.each{ |apache| status +="#{apache.name}: #{apache.state} "}
    puts status
    puts "***********************************************************"
  end

  def preconditions_ok?
    if(File.exists?(@lockfile)) then
      log("There seems to be a deploy running already! If not, remove #{@lockfile} and try again")
      return false
    end
	
    system('bash /var/src/static_deploy.sh preconditions')
    return false unless $? == 0
    
    #gets apaches whose preconditions aren't ok, and checks that collection is empty.
    return false unless @apaches.reject{|a| a.preconditions_ok? }.empty?
    return false unless @scheduler_jboss.preconditions_ok?
    return false unless all_jbosses.reject{|j| j.preconditions_ok? }.empty?
    return true
  end
  
  def set_running
     @build_status= 'started'
     FileUtils.touch(@lockfile)
     print_status 
  end

  def build_war
     @build_status= 'building war'
     print_status
     system("bash /var/src/build.sh")
     build_ok = ($? == 0)
     raise "Build failed!" unless build_ok

     @warfile='/var/src/arch/sitebuilder2.war.current'
     @build_status = "built war #{@warfile}"
     print_status
  end

  def deploy_war_to_scheduler
     # no need to monkey with apache
     @scheduler_jboss.deploy(@warfile, self)
     @build_status='deploying war'
     print_status
  end

  def deploy_war
  	raise "@warfile is not set" if @warfile.nil?
    @jbosses.each do |group_name, group|
      done_first_deploy=false
      log "Deploying group '#{group_name}'"
      first_server=group[0]
      group.each do |jboss|
        if (!done_first_deploy) then
          hide_jboss(jboss)
        end
        jboss.deploy(@warfile, self)
        done_first_deploy = true
        use_this_jboss_only(first_server)
      end
      use_all_jbosses(group_name)
      log "Finished deploying group '#{group_name}'"
    end
    @build_status='deployed war'  
    print_status
  end
  
  #Redeploy a specific JBoss.
  #DOESN'T REMOVE FROM PORT BALANCING! Remove from Apache first.
  #see hide_jboss(jboss)
  def deploy_war_to (jboss)
  	build_war
    jboss = find_jboss_by_name(jboss) unless jboss.is_a? Jboss
    jboss.deploy(@warfile, self)
  end
  
  # Finds the Apache whose name matches the name of the JBoss group,
  # as this one is responsible for serving content for this group.
  def apaches_for(jboss)
    apaches = @apaches.select{|a| a.name == jboss.group_name }
    raise "No apaches found for #{jboss}" if apaches.empty?
    return apaches
  end
  
  # Like apaches_for(jboss), but just takes the name of a JBoss group.
  def apaches_by_group(group_name)
    @apaches.select{|a| a.name == group_name }
  end

  # remove from apache load balancing
  def hide_jboss(jboss)
    jboss = find_jboss_by_name(jboss) unless jboss.is_a? Jboss
    log "Hiding JBoss #{jboss.name}"
    apaches_for(jboss).each{ |a| a.remove_server(jboss.port,self)}
  end
  
  # point ALL apache requests at this jboss
  # (by ALL, we mean all Apaches that serve this JBoss' group)
  def use_this_jboss_only(jboss)
    jboss = find_jboss_by_name(jboss) unless jboss.is_a? Jboss
    log "Using only #{jboss.name}"
    apaches_for(jboss).each{|a|
      a.use_server_exclusively(jboss.port,self) 
    }
  end
  
  def use_all_jbosses(group_name=nil)
    if group_name.nil?
      # enable port balancing for all port balancer groups
      @jbosses.each do |name, group|
        apaches_by_group(name).each { |a| a.use_balancer(self) }
      end
    else
      # enable port balancing for a specific group
      apaches_by_group(group_name).each { |a| a.use_balancer(self) }
    end
  end
  
  def find_jboss_by_name(name)
	  really_all = all_jbosses + [ @scheduler_jboss ]
    jboss = really_all.select {|j| name == j.name }.first
    raise "JBoss '#{name}' not found" unless jboss
    return jboss
  end

  def deploy_static
    system('bash /var/src/static_deploy.sh')
    raise "static deploy failed!" unless $? ==0
    @build_status = 'built war and deployed static'
    print_status
  end

  def unset_running
     @build_status='complete'
     File.delete(@lockfile)
     print_status
  end

end


class Jboss
  attr_accessor :name, :group_name, :state, :deployment_state,:port

  def initialize(instance_name,port)
    @name= instance_name
    @state= "running"
    @deployment_state= 'old'
    @port=port
	
	
  end
  
  def war_path
  	"/usr/local/jboss/server/#{@name}/deploy/sitebuilder2.war"
  end
  
  def preconditions_ok?
  	unless File.can_write_to? war_path
  	  log "Can't write to war destination: #{@war_path}"
	  false
  	end
	true
  end

  def deploy(warfile,status_printer)
    graceful_halt
    install_war(warfile)
    start
    status_printer.print_status
  end

  def install_war(warfile)
    FileUtils.cp(warfile, war_path)
    @deployment_state='new'
  end

  def graceful_halt
    keep_going = true
    tries = 5
    while (keep_going) do
      tries = tries -1
      log("Waiting for all threads on #{@name} to complete" )
      sleep(5)
      if (is_idle? || tries < 1) then 
        keep_going = false
      end
    end
    log "Finished waiting; stopping #{name} now"
    system("#{@stop_command}#{name}")
    @state='stopped'
  end

  def start
    command = "bash /var/src/start-jboss.sh #{@name} http://localhost:#{@port}/sitebuilder2/render/renderPage.htm?sbrPage=#{@test_page} #{@mode}"
    if !@mode.nil? and @mode != ""
       command << " #{@mode}"
    end     
    system(command)
    if ($? != 0) then
       raise("Couldn't start jboss instance #{name}!")
    end
    raise "Could not verify deployment!" unless verify_deployment
    sleep(2)
    @state='running'
  end

  def verify_deployment
      ok = true
      @requests.each do |url, expected|
          res = Net::HTTP.start("localhost", @port) {|http|
            http.get(url)
          }
          if (res.code.to_i != expected)
             log "[NOT OK] #{url} got #{res.code} expected #{expected}"   
             ok = false
          else
             log "[OK]     #{url} got #{res.code}"
          end
      end
     return ok
  end


  # check whether this instance has no active requests.
  def is_idle?
    host="localhost:#{@port}"
    url = "http://#{host}/status?XML=true"
    log "Checking #{url}"
    xml = open(url){|f| f.read }
    doc= Document.new(xml)
    
    # guess at right connector
    httpconnector = doc.root.elements[3]    
    # now look for the right connector
    doc.root.elements.each do |connector|
      if (connector.attributes["name"] =~ /http/) then
        httpconnector = connector
      end
    end
    workers = httpconnector.elements["workers"].elements
    numconnections = 0;
    workers.each do |w|  
      if  ((w.attributes["stage"] =~ /S/ )&&  (w.attributes["currentUri"] !~ /\/status(.*)/ )) then
        numconnections = numconnections +1 
      end
    end
    log "Jboss instance #{name} has #{numconnections} active connections"
    return (numconnections == 0)    
  end
  
end

class Apache
  attr_accessor :name, :restart_command,:state
 
  def initialize(display_name,balancer,remotehost = nil)
     @name=display_name    
     @state='balanced'
     @port_balancer=balancer
     @remotehost=remotehost
  end
  
  def preconditions_ok?
  	unless File.can_write_to? @rewrite_map
  	  log "Can't write to #{@rewrite_map}"
	  return false 
  	end
	true
  end

  def reconfigure
    if (@remotehost != nil) then 
      # note the escaped redirection \\> ; otherwise the output will go onto the local host, not
      # the remote one!
      command = "ssh #{@remotehost} echo \"port #{@port_balancer.current_port}\" \\> #{@rewrite_map}"
    else
      command="echo \"port #{@port_balancer.current_port}\" > #{@rewrite_map}"
    end 
    log "Reconfiguring apache #{@name}: running #{command}"
    system command
    if ($? != 0) then
       raise("Failed to write to port map file!")
    end	
  end

  def restart
    reconfigure
    command = @restart_command
    if (@remotehost != nil) then
      command = "ssh #{@remotehost} " + command
    end
    log ("Restarting apache #{@name}: running #{command}")
    system command
    if ($? != 0) then
       raise("Apache restart command failed")
    end 	
  end

  def remove_server(port,status_printer)
     @port_balancer.remove_port(port)
     restart
     @state="unbalanced: #{@port_balancer.current_port}"
     status_printer.print_status
  end
  def add_server(port, status_printer)
    @port_balancer.add_port(port)
    #restart
    @state="balanced: #{@port_balancer.current_port}"
    status_printer.print_status
  end

  def use_server_exclusively(port, status_printer)
    log "Switching Apache #{name} to only serve to port #{port}"
    @port_balancer.use_exclusively(port)
    restart
    @state="unbalanced: #{@port_balancer.current_port}"
    status_printer.print_status
  end

  def use_balancer(status_printer)
    @port_balancer.use_balancer
    restart
    @state="balanced: #{@port_balancer.current_port}"
  end

  
end

class LoadBalancedPortTracker
  attr_reader :current_port

  # nasty hack that only really works properly with 2 servers
  def initialize(balancer_port, balanced_ports)
    @balancer_port= balancer_port
    @balanced_ports= balanced_ports
    @current_port=balancer_port
  end

  def remove_port(port)
    @balanced_ports.reject!{ |p| p == port}
    raise "All ports removed from loadbalancer!" if @balanced_ports.length==0
    @current_port= @balanced_ports[0]
  end

  def add_port(port)
    @balanced_ports << port
    @current_port= @balancer_port
  end

  def use_balancer
    @current_port = @balancer_port
  end
  def use_exclusively(port)
    @current_port = port	
  end

end


class File
  # Test whether the file is writable OR can be created
  def File.can_write_to?(filename)
    if File.exist? filename then
      File.writable? filename
    else
      File.writable?(File.dirname(filename))
    end 	
  end
end

