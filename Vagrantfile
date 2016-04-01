# -*- mode: ruby -*-
# vi: set ft=ruby :
#
Vagrant.require_version ">= 1.6.0"

# NOTE:  Due to virtualbox mounting constraints, this must be run in squential mode:
# export VAGRANT_NO_PARALLEL=yes
# or
# vagrant up --no-parallel 
ENV['VAGRANT_DEFAULT_PROVIDER'] = 'docker'

DOCKER_HOST_NAME = "dockerhost-apix"
DOCKER_HOST_VAGRANTFILE = "./DockerHostVagrantfile"
DOCKER_HOST_FORCE_VM = true
DOCKER_HOST_USER = "1000:1000"
 
Vagrant.configure("2") do |config|

  # Do not share the current directory as /vagrant with Docker
  config.vm.synced_folder ".", "/vagrant", disabled: true

  config.vm.define "fcrepo" do |m|
    m.vm.provider :docker do |d|
      d.name = 'fcrepo'
      d.build_dir = "poc-services/poc-service-fcrepo"
      d.build_args = ["-t", "apix-poc/service-fcrepo"]
      d.volumes = ["/shared:/shared"]
      d.expose = [8080, 61613,61616]
      d.ports = ["8080:8080"]
      d.remains_running = true

      d.create_args = ["--user=#{DOCKER_HOST_USER}", "--net=apix"]

      d.force_host_vm = DOCKER_HOST_FORCE_VM 
      d.vagrant_machine = DOCKER_HOST_NAME
      d.vagrant_vagrantfile = DOCKER_HOST_VAGRANTFILE
    end
  end

 config.vm.define "fuseki" do |m|
    m.vm.provider :docker do |d|
      d.name = 'fuseki'
      d.build_dir = "poc-services/poc-service-fuseki"
      d.build_args = ["-t", "apix-poc/service-fuseki"]
      d.volumes = ["/shared:/shared"]
      d.expose = [3030]
      d.ports = ["3030:3030"]
      d.remains_running = true
      d.env = {
        FUSEKI_DEFAULT_DATASET: "/fcrepo"
      }

      d.create_args = ["--user=#{DOCKER_HOST_USER}", "--net=apix"]

      d.force_host_vm = DOCKER_HOST_FORCE_VM 
      d.vagrant_machine = DOCKER_HOST_NAME
      d.vagrant_vagrantfile = DOCKER_HOST_VAGRANTFILE
    end
  end


  config.vm.define "route-indexing-triplestore" do |m|
    m.vm.provider :docker do |d|
      d.name = 'route-indexing-triplestore'
      d.build_dir = "poc-services/poc-route-indexing-triplestore"
      d.build_args = ["-t", "apix-poc/route-indexing-triplestore"]
      d.volumes = ["/shared:/shared"]
      d.remains_running = true
      d.env = {
        FCREPO_BASEURL: "fcrepo:8080/rest",
        JMS_BROKERURL: "tcp://fcrepo:61616",
        TRIPLESTORE_BASEURL: "fuseki:3030/fcrepo/update",
        ERROR_MAMXREDELIVERIES: "100000"
      }
 
      d.create_args = [ "--user=#{DOCKER_HOST_USER}", "--net=apix"]

      d.force_host_vm = DOCKER_HOST_FORCE_VM 
      d.vagrant_machine = DOCKER_HOST_NAME
      d.vagrant_vagrantfile = DOCKER_HOST_VAGRANTFILE
    end
  end

  config.vm.define "acrepo-apix" do |m|
    m.vm.provider :docker do |d|
      d.name = 'acrepo-apix'
      d.build_dir = "poc-services/poc-service-acrepo-apix"
      d.build_args = ["-t", "apix-poc/service-acrepo-apix"]
      d.volumes = ["/shared:/shared"]
      d.expose = [8081]
      d.ports = ["8081:8081"]
      d.remains_running = true
      d.env = {
        FCREPO_BASEURL: "fcrepo:8080/rest",
        APIX_REST_PROXY: "/rest"
      }

      d.create_args = [ "--user=#{DOCKER_HOST_USER}", "--net=apix"]

      d.force_host_vm = DOCKER_HOST_FORCE_VM
      d.vagrant_machine = DOCKER_HOST_NAME
      d.vagrant_vagrantfile = DOCKER_HOST_VAGRANTFILE
    end
  end

end
