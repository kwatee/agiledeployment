#
# This example script shows how to retrieve servers information with the kwatee json api
#

import kwatee
import sys

if len(sys.argv) != 2:
	print 'Lists available servers'
	print 'Usage: python servers.py http://[<user[:password>]@]<host>:<port>/<kwateeUrl>'
else:
	kwateeUrl = sys.argv[1]
	session = kwatee.api.Session(kwateeUrl)
	session.connect()
	print session.getServers()
	session.disconnect()
