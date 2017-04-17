#
# This example script shows how to update a release description with the kwatee json api
#

import kwatee
import sys, os

if len(sys.argv) < 5:
	print 'update release description'
	print 'Usage: python updateReleaseDescription.py http://[<user[:password>]@]<host>:<port>/<kwateeUrl> <environment> <release_name> <description>'
else:
	kwateeUrl = sys.argv[1]
	environmentName = sys.argv[2]
	releaseName = sys.argv[3]
	description = sys.argv[4]
	session = kwatee.api.Session(kwateeUrl)
	session.connect()
	options = {'description': description}
	session.updateRelease(environmentName, releaseName, options)
	print 'Release description updated'
	session.disconnect()
