#
# This example script shows how to upload a version package with the kwatee json api
#

import kwatee
import sys, os

if len(sys.argv) < 5:
	print 'Updates version package with local file'
	print 'Usage: python update.py http://[<user[:password>]@]<host>:<port>/<kwateeUrl> <artifact> <version> <path>'
else:
	kwateeUrl = sys.argv[1]
	artifactName = sys.argv[2]
	versionName = sys.argv[3]
	path = os.path.abspath(sys.argv[4])
	session = kwatee.api.Session(kwateeUrl)
	session.connect()
	session.uploadPackage(artifactName, versionName, path)
	print 'Version package updated'
	session.disconnect()
