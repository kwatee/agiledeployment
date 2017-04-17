#
# This example script shows how to delete an artifact version using the kwatee json api
#

import kwatee
import sys, os

if len(sys.argv) < 4:
	print 'Deletes a version'
	print 'Usage: python deleteVersion.py http://[<user[:password>]@]<host>:<port>/<kwateeUrl> <artifact> <version>'
else:
	kwateeUrl = sys.argv[1]
	artifactName = sys.argv[2]
	versionName = sys.argv[3]
	session = kwatee.api.Session(kwateeUrl)
	session.connect()
	session.deleteVersion(artifactName, versionName)
	print 'Version deleted'
	session.disconnect()
