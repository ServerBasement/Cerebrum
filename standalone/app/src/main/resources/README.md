Caused by: com.sun.jna.LastErrorException: [13] Permission denied

sudo usermod -aG docker $USER

needs to have $USER defined in your shell. This is often there by default, but you may need to set the value to your login id in some shells.


Changing the groups of a user does not change existing logins, terminals, and shells that a user has open. To avoid performing a login again, you can simply run:

newgrp docker