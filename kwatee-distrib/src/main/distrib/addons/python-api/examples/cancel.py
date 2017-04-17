#
# This example script shows how a deployment operation can be canceled using the kwatee json api
#

import kwatee
import sys

if len(sys.argv) < 2:
	print 'Cancels ongoing deploy operation'
	print 'Usage: python cancel.py http://[<user[:password>]@]<host>:<port>/<kwateeUrl>'
else:
	kwateeUrl = sys.argv[1]
	operationRef = sys.argv[2]
	session = kwatee.api.Session(kwateeUrl)
	session.connect();
	session.cancel(operationRef)
	print 'Operation canceled'
	session.disconnect()
