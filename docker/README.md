## Docker Compose workflow
* Start: `docker-compose up`
* Stop, but keep state `docker-compose stop`
* Stop, and destroy all state `docker-compose down`
* Rebuild docker images from new artifacts from `mvn install`: `docker-compose build`


## Setting up docker machine in virtualbox
`docker-machine create -d virtualbox --virtualbox-cpu-count=4 apix`

It's necessary share the API-X code repository with Docker Machine so build artifacts can be incorporated into
docker images.  On the Mac platform, Docker Machine automatically shares the `/Users` directory with Virtualbox,
so any code in `Users` is automatically shared.  For other platforms (especially Linux), this sharing needs to be
enabled manually.

* Start "Oracle VM VirtualBox Manager"
* Right-Click <machine name> (default)
* Settings -> Shared Folders.
    * If there is a pre-existing shared forlder for `/home`, remove it
    * Click the "folder +" icon in the extreme right
        * Folder Path: &lt;host dir&gt;.  This is your local directory that you want mounted (e.g. /home/me)
        * Folder Name: &lt;mount name&gt; This is an arbitrary label used later on for mounting within the machine
        * Check on "Auto-mount" and "Make Permanent"
* SSH into machine `docker-machine ssh default`
* `sudo vi /mnt/sda1/var/lib/boot2docker/bootlocal.sh`
    * Add
    <pre>
    mkdir -p &lt;local_dir&gt;
    mount -t vboxsf -o defaults,uid=`id -u docker`,gid=`id -g docker` &lt;mount_name&gt; &lt;local_dir&gt;
    </pre>
* now run `/bin/sh /mnt/sda1/var/lib/boot2docker/bootlocal.sh`
