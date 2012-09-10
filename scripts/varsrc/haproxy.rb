require 'uri'
require 'socket'

module HAProxy
  class CSVParser

    # Uses CSV header from HAProxy 1.5, which is backwards compatible
    COLUMNS = "pxname,svname,qcur,qmax,scur,smax,slim,stot,bin,bout,dreq,dresp,ereq,econ,eresp,wretr,wredis,status,weight,act,bck,chkfail,chkdown,lastchg,downtime,qlimit,pid,iid,sid,throttle,lbtot,tracked,type,rate,rate_lim,rate_max,check_status,check_code,check_duration,hrsp_1xx,hrsp_2xx,hrsp_3xx,hrsp_4xx,hrsp_5xx,hrsp_other,hanafail,req_rate,req_rate_max,req_tot,cli_abrt,srv_abrt".split(",").map(&:to_sym)
    LIMIT = COLUMNS.length

    def self.parse(line)
      line.strip!
      unless line.start_with? "#"
        data = line.split(',')
        pxname = data.shift

        stats = { :pxname => pxname }

        data.each_with_index do |value, i|
          if i < LIMIT
            stats[COLUMNS[i + 1]] = value
          else
            stats[:extra] = Array.new if stats[:extra].nil?
            stats[:extra] << value
          end
        end

        stats
      else
        raise ArgumentError, "Line is a comment"
      end
    end
  end

  class SocketReader
    attr_reader :path

    def initialize(path)
      raise ArgumentError, "Socket #{path} doesn't exists or is not a UNIX socket" unless File.exists?(path) and File.socket?(path)
      @path = path
    end

    def info
      returning({}) do |info|
        send_cmd "show info" do |line|
          if not line.strip.empty? then
            key, value = line.split(/: ?/)
            info[key.downcase.gsub('-', '_').to_sym] = value
          end
        end
      end
    end

    def errors
      returning("") do |errors|
        send_cmd "show errors" do |line|
          errors << line
        end
      end
    end

    def sessions
      returning([]) do |sess|
        send_cmd "show sess" do |line|
          sess << line
        end
      end
    end

    TYPES = {
      :frontend => 1,
      :backend => 2,
      :server => 4
    }

    def stats(types=[:all], options={})
      params = {
        :proxy => :all,
        :server => :all
      }.merge(options)

      params[:proxy] = "-1" if params[:proxy].eql?(:all)
      params[:server] = "-1" if params[:server].eql?(:all)

      types = [types] unless types.is_a?(Array)

      params[:type] = case
                      when types.eql?([:all])
                        "-1"
                      else
                        types.map{ |type| TYPES[type] }.inject(:+)
                      end

      cmd = "show stat #{params[:proxy]} #{params[:type]} #{params[:server]}"

      returning([]) do |stats|
        send_cmd(cmd) do |line|
          stats << CSVParser.parse(line) unless line.start_with?('#')
        end
      end
    end

    def frontends
      stats :frontend, :proxy => :all, :server => :all
    end

    def backends
      stats :backend, :proxy => :all, :server => :all
    end

    def servers
      stats :server, :proxy => :all, :server => :all
    end

    def enable_server(backend, server)
      returning("") do |errors|
        send_cmd "enable server %s/%s" % [backend, server] do |line|
          errors << line
        end
      end
    end  

    def disable_server(backend, server)
      returning("") do |errors|
        send_cmd "disable server %s/%s" % [backend, server] do |line|
          errors << line
        end
      end
    end

    protected

    def send_cmd(cmd, &block)
      socket = UNIXSocket.new(@path)
      socket.write(cmd + ';')
      socket.each do |line|
        yield(line.strip)
      end
    end

    # Borrowed from Rails 3
    def returning(value)
      yield(value)
      value
    end
  end
end