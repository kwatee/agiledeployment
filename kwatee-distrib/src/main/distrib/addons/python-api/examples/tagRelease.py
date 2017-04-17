#
# This example script shows how to tag a release with the kwatee json api
#

import kwatee
import sys, os

if len(sys.argv) < 4:
	print 'Tags snapshot release'
	print 'Usage: python tagRelease.py http://[<user[:password>]@]<host>:<port>/<kwateeUrl> <environment> <release_name> [<description>]'
else:
	kwateeUrl = sys.argv[1]
	environmentName = sys.argv[2]
	releaseName = sys.argv[3]
	description = None if sys.argv == 4 else {'description': sys.argv[4]}
	session = kwatee.api.Session(kwateeUrl)
	session.connect()
	session.tagRelease(environmentName, releaseName, description)
	print 'Release tagged'
	session.disconnect()
