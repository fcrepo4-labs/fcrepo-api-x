#!/bin/sh
sudo apt-get -y install samba
(echo vagrant; echo vagrant) | sudo smbpasswd -a vagrant -s
cat /vagrant/dockerhost/smb.conf > /etc/samba/smb.conf
