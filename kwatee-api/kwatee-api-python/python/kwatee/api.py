import sys, copy, time
from urllib import urlencode
import urllib2
import json
from poster.encode import multipart_encode
from poster.streaminghttp import register_openers

class Session:
	"Wrapper to Kwatee RESTful Json API"
	def __init__(self, connectionUrl):
		"""
		Establishes an authenticated session to the kwatee host
		@param connectionUrl: a url of the format C{http://I{user}:I{password}@I{host}:I{port}/kwatee}
		                      for instance: C{http://admin:password@localhost:8080/kwatee}
		"""
		self.__authToken = None
		self.__reason = 'initialization error'
		userIdx = connectionUrl.index('://') + 3
		pwdIdx = connectionUrl.index(':', userIdx)
		hostIdx = connectionUrl.index('@', pwdIdx)
		self.__user = connectionUrl[userIdx:pwdIdx]
		self.__password = connectionUrl[pwdIdx+1:hostIdx]
		self.__base_url = connectionUrl[0:userIdx] + connectionUrl[hostIdx+1:]
		if not self.__base_url.endswith('/'):
			self.__base_url += '/'
		self.__base_url += 'api/'
		register_openers()
		self.__authToken = None
		url = self.__base_url+'/authenticate/'+self.__user+'?version=${project.version}'
		req = urllib2.Request(url, headers={'X-API-AUTH':self.__password})
		r = self.__openurl(req, method='POST', expectedCode=200, error='Login failed', authenticationRequired=False)
		self.__authToken = r.read()

	def isConnected(self):
		"""
		@return: C{True} if the session is connected C{False} otherwise
		"""
		return self.__authToken is not None

	def close(self):
		"""
		Cleanly disposes of the session
		"""
		if self.__authToken:
			url = self.__base_url+'/logout'
			req = urllib2.Request(url)
			self.__openurl(req, method='POST')
			self.__authToken = None

	def getArtifacts(self):
		"""
		Retrieves the list of artifacts in the repository
		@return: an array of artifacts
		"""
		url = self.__base_url+'/artifacts.json'
		req = urllib2.Request(url)
		r = self.__openurl(req, expectedCode=200)
		return json.load(r)

	def getArtifact(self, artifactName):
		"""
		Retrieves an artifact's properties
		@param artifactName: the name of an artifact
		@return: the artifact's properties
		"""
		url = self.__base_url+'/artifacts/'+artifactName+'.json'
		req = urllib2.Request(url)
		r = self.__openurl(req, expectedCode=200)
		return json.load(r)

	def updateArtifact(self, artifactName, artifactProperties):
		"""
		Updates an artifact's properties
		@param artifactName: the name of an artifact
		@param artifactProperties: the artifact's properties to update
		"""
		url = self.__base_url+'/artifacts/'+artifactName
		data, headers = self.__jsonData(artifactProperties)
		req = urllib2.Request(url, data, headers)
		self.__openurl(req, method='PUT', expectedCode=200)

	def createArtifact(self, artifactName, artifactProperties=None):
		"""
		Creates a new artifact and optionally set additional properties
		@param artifactName: the name of the artifact to create
		@param artifactProperties: the artifact properties to set or C{None}
		"""
		url = self.__base_url+'/artifacts/'+artifactName
		data, headers = self.__jsonData(artifactProperties)
		req = urllib2.Request(url, data, headers)
		self.__openurl(req, method='POST', expectedCode=201)

	def deleteArtifact(self, artifactName):
		"""
		Deletes an artifact
		@param artifactName: the name of an artifact
		"""
		url = self.__base_url+'/artifacts/'+artifactName
		req = urllib2.Request(url)
		self.__openurl(req, method='DELETE', expectedCode=200)

	def getVersion(self, artifactName, versionName):
		"""
		Retrieves the properties of an artifact's version
		@param artifactName: the name of the version's artifact
		@param versionName: the name of a version
		@return: the version's properties
		"""
		url = self.__base_url+'/artifacts/'+artifactName+'/'+versionName+'.json'
		req = urllib2.Request(url)
		r = self.__openurl(req, expectedCode=200)
		return json.load(r)

	def updateVersion(self, artifactName, versionName, versionProperties):
		"""
		Updates an artifact's version properties
		@param artifactName: the name of the version's artifact
		@param versionName: the name of a version
		@param versionProperties: the version properties to update (plain string JSON)
		"""
		url = self.__base_url+'/artifacts/'+artifactName+'/'+versionName
		data, headers = self.__jsonData(versionProperties)
		req = urllib2.Request(url, data, headers)
		self.__openurl(req, method='PUT', expectedCode=200)

	def createVersion(self, artifactName, versionName, versionProperties=None):
		"""
		Creates a version in an artifact and optionally set additional properties
		@param artifactName: the name of the version's artifact
		@param versionName: the name of the version to create
		@param versionProperties: the version properties to set or C{None}
		"""
		url = self.__base_url+'/artifacts/'+artifactName+'/'+versionName
		data, headers = self.__jsonData(versionProperties)
		req = urllib2.Request(url, data, headers)
		self.__openurl(req, method='POST', expectedCode=201)

	def duplicateVersion(self, artifactName, versionName, duplicateFrom, versionProperties=None):
		"""
		Duplicates an existing version and optionally set additional properties
		@param artifactName: the name of the version's artifact
		@param versionName: the name of a version to create
		@param duplicateFrom: name of the version (within same artifact) to duplicate
		@param versionProperties: the version properties to set or C{None}
		"""
		url = self.__base_url+'/artifacts/'+artifactName+'/'+versionName
		url += '?duplicateFrom='+duplicateFrom
		data, headers = self.__jsonData(versionProperties)
		req = urllib2.Request(url, data, headers)
		self.__openurl(req, method='POST', expectedCode=201)

	def deleteVersion(self, artifactName, versionName):
		"""
		Deletes an artifact's version
		@param artifactName: the name of the version's artifact
		@param versionName: the name of a version
		"""
		url = self.__base_url+'/artifacts/'+artifactName+'/'+versionName
		req = urllib2.Request(url)
		self.__openurl(req, method='DELETE', expectedCode=200)

	def uploadArtifactPackagePost(self, artifactName, versionName, uploadFile, deleteOverlays=False):
		"""
		Uploads a package (multipart content) to an artifact's version. Replaces whatever existing package there is but retains previously uploaded overlays unless C{deleteOverlays=True}
		@param artifactName: the name of the version's artifact
		@param versionName: the name of a version
		@param uploadFile: package file path to upload
		@param deleteOverlays: if C{False}, preserves existing overlays
		"""
		datagen, headers = multipart_encode({'file': open(uploadFile,'rb'), 'deleteOverlays': deleteOverlays})
		url = self.__base_url+'/artifacts/'+artifactName+'/'+versionName+'/package'
		req = urllib2.Request(url, datagen, headers)
		self.__openurl(req, method='POST', expectedCode=200)

	def uploadArtifactPackageUrl(self, artifactName, versionName, uploadUrl, deleteOverlays=False):
		"""
		Uploads a package from a url (could be file://..) to an artifact's version. Replaces whatever existing package there is but retains previously uploaded overlays unless C{deleteOverlays=True}
		@param artifactName: the name of the version's artifact
		@param versionName: the name of a version
		@param uploadUrl: package url to upload
		@param deleteOverlays: if C{False}, preserves existing overlays
		"""
		url = self.__base_url+'/artifacts/'+artifactName+'/'+versionName+'/package?'+urlencode({'url': uploadUrl})
		if deleteOverlays:
			url += '&deleteOverlays=true'
		req = urllib2.Request(url)
		self.__openurl(req, method='POST', expectedCode=200)

	def getArtifactPackageFiles(self, artifactName, versionName, path=None):
		"""
		Retrieves the files present in a version's package at a given relative path
		@param artifactName: the name of the version's artifact
		@param versionName: the name of a version
		@param path: the relative path of the directory to list within package (C{None} for root of package listing)
		@return: the files present in a package at a given relative path as as array
		"""
		url = self.__base_url+'/artifacts/'+artifactName+'/'+versionName+'/package/files.json'
		if path:
			url += '?'+urlencode({'path': path})
		req = urllib2.Request(url)
		r = self.__openurl(req, expectedCode=200)
		return json.load(r)

	def getArtifactSpecialFiles(self, artifactName, versionName):
		"""
		Retrieves all the I{special files} (overlays, with variables, with custom flags) within the package
		@param artifactName: the name of the version's artifact
		@param versionName: the name of a version
		@return: all the I{special files} (overlays, with variables, with custom flags) within the package as an array
		"""
		url = self.__base_url+'/artifacts/'+artifactName+'/'+versionName+'/package/specialFiles.json'
		req = urllib2.Request(url)
		r = self.__openurl(req, expectedCode=200)
		return json.load(r)

	def updateArtifactPackageFileProperties(self, artifactName, versionName, path, fileProperties):
		"""
		Update custom flags (ignoreIdenty, dontDelete, ...) of a file within a package
		@param artifactName: the name of the version's artifact
		@param versionName: the name of a version
		@param path: the relative path of the file within the package
		@param fileProperties: the file's properties to update
		"""
		url = self.__base_url+'/artifacts/'+artifactName+'/'+versionName+'/package/file.json'
		url += '?'+urlencode({'path': path})
		data, headers = self.__jsonData(fileProperties)
		req = urllib2.Request(url, data, headers)
		self.__openurl(req, method='PUT', expectedCode=200)

	def downloadArtifactPackageFile(self, artifactName, versionName, path, downloadFile):
		"""
		Downloads a file within the package in the specified location
		@param artifactName: the name of the version's artifact
		@param versionName: the name of a version
		@param path: the relative path of the file within the package
		@param downloadFile: the location to store the result into
		"""
		url = self.__base_url+'/artifacts/'+artifactName+'/'+versionName+'/package/file?'+urlencode({'path':path})
		req = urllib2.Request(url)
		r = self.__openurl(req, expectedCode=200)
		out = open(downloadFile, 'wb')
		out.write(r.read())
		out.close()

	def uploadArtifactPackageOverlayPost(self, artifactName, versionName, path, uploadFile):
		"""
		Uploads (Http Post) an overlay at a relative path within the package
		@param artifactName: the name of the version's artifact
		@param versionName: the name of a version
		@param path: the relative path of the overlay directory within the package (or C{None} for root)
		@param uploadFile: the path to the file to upload
		"""
		multipart = {'file': open(uploadFile,'rb')}
		if path:
			multipart['path'] = path
		datagen, headers = multipart_encode(multipart)
		url = self.__base_url+'/artifacts/'+artifactName+'/'+versionName+'/package/overlay'
		req = urllib2.Request(url, datagen, headers)
		self.__openurl(req, method='POST', expectedCode=201)

	def uploadArtifactPackageOverlayUrl(self, artifactName, versionName, path, uploadUrl):
		"""
		Uploads (from URL) an overlay at a relative path within the package
		@param artifactName: the name of the version's artifact
		@param versionName: the name of a version
		@param path: the relative path of the overlay directory within the package
		@param uploadUrl: the url to a file (can be file:///) to upload
		"""
		url = self.__base_url+'/artifacts/'+artifactName+'/'+versionName+'/package/overlay?'+urlencode({'url': uploadUrl, 'path': path})
		req = urllib2.Request(url)
		self.__openurl(req, method='POST', expectedCode=201)

	def deleteArtifactPackageOverlay(self, artifactName, versionName, overlayPath):
		"""
		Deletes an existing version overlay
		@param artifactName: the name of the version's artifact
		@param versionName: the name of a version
		@param overlayPath: the relative path of the file within the package
		"""
		url = self.__base_url+'/artifacts/'+artifactName+'/'+versionName+'/package/overlay'
		url += '?'+urlencode({'path': overlayPath})
		req = urllib2.Request(url)
		self.__openurl(req, method='DELETE', expectedCode=200)

	def getArtifactVariables(self, artifactName, versionName):
		"""
		Retrieves the list of version variables
		@param artifactName: the name of the version's artifact
		@param versionName: the name of a version
		@return: an array of version variables
		"""
		url = self.__base_url+'/artifacts/'+artifactName+'/'+versionName+'/variables.json'
		req = urllib2.Request(url)
		r = self.__openurl(req, expectedCode=200)
		return json.load(r)

	def setArtifactVariables(self, artifactName, versionName, variables):
		"""
		Sets version variables
		@param artifactName: the name of the version's artifact
		@param versionName: the name of a version
		@param variables: array of variables
		"""
		url = self.__base_url+'/artifacts/'+artifactName+'/'+versionName+'/variables'
		data, headers = self.__jsonData(variables)
		req = urllib2.Request(url, data, headers)
		self.__openurl(req, method='PUT', expectedCode=200)

	def importActifactsBundlePost(self, bundleFile):
		"""
		Uploads (Http Post) an artifacts bundle into the repository
		@param bundleFile: the artifacts bundle to import
		"""
		datagen, headers = multipart_encode({'file': open(bundleFile,'rb')})
		url = self.__base_url+'/artifacts'
		req = urllib2.Request(url, datagen, headers)
		self.__openurl(req, method='POST', expectedCode=200)

	def importActifactsBundleUrl(self, bundleUrl):
		"""
		Uploads (from URL) an artifacts bundle into the repository
		@param bundleUrl: the url to an artifacts bundle (can be file:///) to import
		"""
		url = self.__base_url+'/artifacts?'+urlencode({'url': bundleUrl})
		req = urllib2.Request(url)
		self.__openurl(req, method='POST', expectedCode=200)

	def getServers(self):
		"""
		Retrieves the list of servers in the repository
		@return: an array of servers
		"""
		url = self.__base_url+'/servers.json'
		req = urllib2.Request(url)
		r = self.__openurl(req, expectedCode=200)
		return json.load(r)

	def getServer(self, serverName):
		"""
		Retrieves the properties of a server
		@param serverName: the name of the server
		@return: the server's properties
		"""
		url = self.__base_url+'/servers/'+serverName+'.json'
		req = urllib2.Request(url)
		r = self.__openurl(req, expectedCode=200)
		return json.load(r)

	def updateServer(self, serverName, serverProperties):
		"""
		Updates the properties of a server
		@param serverName: the name of the server
		@param serverProperties: the server properties to update
		"""
		url = self.__base_url+'/servers/'+serverName
		data, headers = self.__jsonData(serverProperties)
		req = urllib2.Request(url, data, headers)
		self.__openurl(req, method='PUT', expectedCode=200)

	def createServer(self, serverName, serverProperties=None):
		"""
		Creates a new server and optionally sets additional properties
		@param serverName: the name of the server to create
		@param serverProperties: the server properties to set or C{None}
		"""
		url = self.__base_url+'/servers/'+serverName
		data, headers = self.__jsonData(serverProperties)
		req = urllib2.Request(url, data, headers)
		self.__openurl(req, method='POST', expectedCode=201)

	def duplicateServer(self, serverName, duplicateFrom, serverProperties=None):
		"""
		Duplicates an existing server and optionally set additional properties
		@param serverName: the name of the server to create
		@param duplicateFrom: the name of the server to duplicate
		@param serverProperties: the properties to set or C{None}
		"""
		url = self.__base_url+'/servers/'+serverName
		url += '?duplicateFrom='+duplicateFrom
		data, headers = self.__jsonData(serverProperties)
		req = urllib2.Request(url, data, headers)
		self.__openurl(req, method='POST', expectedCode=201)

	def deleteServer(self, serverName):
		"""
		Deletes a server
		@param serverName: the name of the server
		"""
		url = self.__base_url+'/servers/'+serverName
		req = urllib2.Request(url)
		self.__openurl(req, method='DELETE', expectedCode=200)

	def serverDiagnostics(self, serverName, properties):
		"""
		Tests a connection to the server and returns an array of server capabilities
		@param serverName: the name of the server
		@param properties: server properties
		@return: server capabilities
		"""
		url = self.__base_url+'/servers/'+serverName+'/testConnection'
		data, headers = self.__jsonData(properties)
		req = urllib2.Request(url, data, headers)
		r = self.__openurl(req, method='POST', expectedCode=200)
		return json.load(r)

	def getEnvironments(self):
		"""
		Retrieves the list of environments in the repository
		@return: an array of environments
		"""
		url = self.__base_url+'/environments.json'
		req = urllib2.Request(url)
		r = self.__openurl(req, expectedCode=200)
		return json.load(r)

	def getEnvironment(self, environmentName):
		"""
		Retrieves an environment's properties
		@param environmentName: the name of an environment
		@return: the environment's properties
		"""
		url = self.__base_url+'/environments/'+environmentName+'.json'
		req = urllib2.Request(url)
		r = self.__openurl(req, expectedCode=200)
		return json.load(r)

	def updateEnvironment(self, environmentName, environmentProperties):
		"""
		Updates the environment with new properties
		@param environmentName: the name of an environment
		@param environmentProperties: the environment properties to update
		"""
		url = self.__base_url+'/environments/'+environmentName
		data, headers = self.__jsonData(environmentProperties)
		req = urllib2.Request(url, data, headers)
		self.__openurl(req, method='PUT', expectedCode=200)

	def createEnvironment(self, environmentName, environmentProperties=None):
		"""
		Creates a new environment and optionally sets additional properties
		@param environmentName: the name of the environment to create
		@param environmentProperties: the environment properties to set or C{None}
		"""
		url = self.__base_url+'/environments/'+environmentName
		data, headers = self.__jsonData(environmentProperties)
		req = urllib2.Request(url, data, headers)
		self.__openurl(req, method='POST', expectedCode=201)

	def duplicateEnvironment(self, environmentName, duplicateFrom, environmentProperties=None):
		"""
		Duplicates an existing environment and optionally set additional properties
		Note that only the snapshot environment is included in a duplicate operation
		@param environmentName: the name of the environment to create
		@param duplicateFrom: the name of the environment to duplicate
		@param environmentProperties: the properties to set or C{None}
		"""
		url = self.__base_url+'/environments/'+environmentName
		url += '?duplicateFrom='+duplicateFrom
		data, headers = self.__jsonData(environmentProperties)
		req = urllib2.Request(url, data, headers)
		self.__openurl(req, method='POST', expectedCode=201)

	def deleteEnvironment(self, environmentName):
		"""
		Deletes an environment
		@param environmentName: the name of an environment
		"""
		url = self.__base_url+'/environments/'+environmentName
		req = urllib2.Request(url)
		self.__openurl(req, method='DELETE', expectedCode=200)

	def getRelease(self, environmentName, releaseName=None):
		"""
		Retrieve an environment's release properties
		@param environmentName: the name of the release's environment
		@param releaseName: the name of the release or C{None} for snapshot release
		@return: the release properties
		"""
		url = self.__base_url+'/environments/'+environmentName+'/'+self.__releaseName(releaseName)+'.json'
		req = urllib2.Request(url)
		r = self.__openurl(req, expectedCode=200)
		return json.load(r)

	def updateRelease(self, environmentName, releaseName, releaseProperties):
		"""
		Updates a release with new properties
		@param environmentName: the name of the release's environment
		@param releaseName: the name of the release or C{None} for snapshot release
		@param releaseProperties: the release properties to update
		"""
		url = self.__base_url+'/environments/'+environmentName+'/'+self.__releaseName(releaseName)
		data, headers = self.__jsonData(releaseProperties)
		req = urllib2.Request(url, data, headers)
		self.__openurl(req, method='PUT', expectedCode=200)

	def tagRelease(self, environmentName, releaseName, releaseProperties=None):
		"""
		Tags a release and optionally sets additional properties (e.g. description)
		@param environmentName: the name of the release's environment
		@param releaseName: the name of tagged release to create
		@param releaseProperties: the release properties to set
		"""
		url = self.__base_url+'/environments/'+environmentName+'/'+releaseName
		data, headers = self.__jsonData(releaseProperties)
		req = urllib2.Request(url, data, headers)
		self.__openurl(req, method='POST', expectedCode=201)

	def reeditRelease(self, environmentName, releaseName):
		"""
		Reedits a previously tagged release
		@param environmentName: the name of the release's environment
		@param releaseName: the name of existing tagged release
		"""
		url = self.__base_url+'/environments/'+environmentName+'/'+releaseName+'/reedit'
		req = urllib2.Request(url)
		self.__openurl(req, method='POST', expectedCode=200)

	def deleteRelease(self, environmentName, releaseName=None):
		"""
		Deletes a release
		@param environmentName: the name of the release's environment
		@param releaseName: the name of the release or C{None} for snapshot release
		"""
		url = self.__base_url+'/environments/'+environmentName+'/'+self.__releaseName(releaseName)
		req = urllib2.Request(url)
		self.__openurl(req, method='DELETE', expectedCode=200)

	def getEffectiveReleaseArtifacts(self, environmentName, releaseName=None):
		"""
		Retrieves the effective release artifacts (resolves defaultVersions/serverVersions)
		@param environmentName: the name of the release's environment
		@param releaseName: the name of the release or C{None} for snapshot release
		@return: an array of release artifacts
		"""
		url = self.__base_url+'/environments/'+environmentName+'/'+self.__releaseName(releaseName)+'/artifacts.json'
		req = urllib2.Request(url)
		r = self.__openurl(req, expectedCode=200)
		return json.load(r)

	def setReleaseArtifactActiveVersion(self, environmentName, releaseName, artifactName, versionName, serverName=None):
		"""
		Sets the active version (default of server-specific) of a release artifact
		@param environmentName: the name of the release's environment
		@param releaseName: the name of the release or C{None} for snapshot release
		@param artifactName: the name of an artifact
		@param versionName: the active version to be set
		@param serverName: C{None} for default active version
		"""
		url = self.__base_url+'/environments/'+environmentName+'/self.__releaseName(releaseName)/activeVersion'
		url += '?versionName='+versionName
		if serverName:
			url += '&serverName='+serverName
		req = urllib2.Request(url)
		self.__openurl(req, method='PUT', expectedCode=200)

	def getReleasePackageFiles(self, environmentName, releaseName, artifactName, serverName=None, path=None):
		"""
		Retrieves the files present in an environment artifact at a given relative path within the release artifact package
		@param environmentName: the name of the release's environment
		@param releaseName: the name of the release or C{None} for snapshot release
		@param artifactName: the name of an artifact
		@param serverName: C{None} for list files in default package
		@param path: the relative path of the directory to list within package (C{None} for full recursive listing)
		@return: the files present in a package at a given relative path as an array
		"""
		params = {'artifactName': artifactName}
		if serverName:
			params['serverName'] = serverName
		if path:
			params['path'] = path
		url = self.__base_url+'/environments/'+environmentName+'/'+self.__releaseName(releaseName)+'/package/files.json'
		url += '?'+urlencode(params)
		req = urllib2.Request(url)
		r = self.__openurl(req, expectedCode=200)
		return json.load(r)

	def getReleaseSpecialFiles(self, environmentName, releaseName, artifactName, serverName=None):
		"""
		Retrieves all the I{special files} (overlays, with variables, with custom flags) within the release artifact package
		@param environmentName: the name of the release's environment
		@param releaseName: the name of the release or C{None} for snapshot release
		@param artifactName: the name of an artifact
		@param serverName: C{None} for list files in default package
		@return: all the I{special files} (overlays, with variables, with custom flags) within the package as an array
		"""
		url = self.__base_url+'/environments/'+environmentName+'/'+self.__releaseName(releaseName)+'/package/specialFiles.json'
		params = {'artifactName': artifactName}
		if serverName:
			params['serverName'] = serverName
		url += '?'+urlencode(params)
		req = urllib2.Request(url)
		r = self.__openurl(req, expectedCode=200)
		return json.load(r)

	def updateReleasePackageFileProperties(self, environmentName, releaseName, artifactName, serverName, path, fileProperties):
		"""
		Update custom flags (ignoreIdenty, dontDelete, ...) of a file within a release artifact package
		@param environmentName: the name of the release's environment
		@param releaseName: the name of the release or C{None} for snapshot release
		@param artifactName: the name of an artifact
		@param serverName: C{None} for list files in default package
		@param path: the relative path of the file within the package
		@param fileProperties: the file properties to update
		"""
		params = {'artifactName': artifactName, 'path': path}
		if serverName:
			params['serverName'] = serverName
		url = self.__base_url+'/environments/'+environmentName+'/'+self.__releaseName(releaseName)+'/package/file'
		url += '?'+urlencode(params)
		data, headers = self.__jsonData(fileProperties)
		req = urllib2.Request(url, data, headers)
		self.__openurl(req, method='PUT', expectedCode=200)

	def downloadReleasePackageFile(self, environmentName, releaseName, artifactName, serverName, path, downloadFile):
		"""
		Downloads a file within the release artifact package in the specified location
		@param environmentName: the name of the release's environment
		@param releaseName: the name of the release or C{None} for snapshot release
		@param artifactName: the name of an artifact
		@param serverName: C{None} for list files in default package
		@param path: the relative path of the file within the package
		@param downloadFile: the location to store the result into
		"""
		params = {'artifactName': artifactName, 'path': path}
		if serverName:
			params['serverName'] = serverName
		url = self.__base_url+'/environments/'+environmentName+'/'+self.__releaseName(releaseName)+'/package/file?'+urlencode(params)
		req = urllib2.Request(url)
		r = self.__openurl(req, expectedCode=200)
		out = open(downloadFile, 'wb')
		out.write(r.read())
		out.close()

	def uploadReleasePackageOverlayPost(self, environmentName, releaseName, artifactName, serverName, path, uploadFile):
		"""
		Uploads (Http Post) an overlay at a relative path within the release artifact package
		@param environmentName: the name of the release's environment
		@param releaseName: the name of the release or C{None} for snapshot release
		@param artifactName: the name of an artifact
		@param serverName: C{None} for list files in default package
		@param path: the relative path of the overlay directory within the package (C{None} for root)
		@param uploadFile: the file to upload
		"""
		multidata = {'file': open(uploadFile,'rb'), 'artifactName': artifactName}
		if serverName:
			multidata['serverName'] = serverName
		if path:
			multidata['path'] = path
		datagen, headers = multipart_encode(multidata)
		url = self.__base_url+'/environments/'+environmentName+'/'+self.__releaseName(releaseName)+'/package/overlay'
		req = urllib2.Request(url, datagen, headers)
		self.__openurl(req, method='POST', expectedCode=201)

	def uploadReleasePackageOverlayUrl(self, environmentName, releaseName, artifactName, serverName, path, uploadUrl):
		"""
		Uploads (from URL) an overlay at a relative path within the release artifact package
		@param environmentName: the name of the release's environment
		@param releaseName: the name of the release or C{None} for snapshot release
		@param artifactName: the name of an artifact
		@param serverName: C{None} for list files in default package
		@param path: the relative path of the overlay directory within the package (C{None} for root)
		@param uploadUrl: the url to the file (can be file:///) to upload
		"""
		params = {'url': uploadUrl, 'artifactName': artifactName}
		if serverName:
			params['serverName'] = serverName
		if path:
			params['path'] = path
		url = self.__base_url+'/environments/'+environmentName+'/'+self.__releaseName(releaseName)+'/package/overlay?'+urlencode(params)
		req = urllib2.Request(url)
		self.__openurl(req, method='POST', expectedCode=201)

	def deleteReleasePackageOverlay(self, environmentName, releaseName, artifactName, serverName, path):
		"""
		Deletea an existing release artifact package overlay
		@param environmentName: the name of the release's environment
		@param releaseName: the name of the release or C{None} for snapshot release
		@param artifactName: the name of an artifact
		@param serverName: C{None} for list files in default package
		@param path: the relative path of the file within the package
		"""
		params = {'path': path, 'artifactName': artifactName}
		if serverName:
			params['serverName'] = serverName
		url = self.__base_url+'/environments/'+environmentName+'/'+self.__releaseName(releaseName)+'/package/overlay'
		url += '?'+urlencode(params)
		req = urllib2.Request(url)
		self.__openurl(req, method='DELETE', expectedCode=200)

	def getReleaseVariables(self, environmentName, releaseName=None):
		"""
		Retrieves the list of release variables
		@param environmentName: the name of the release's environment
		@param releaseName: the name of the release or C{None} for snapshot release
		@return: an array of variables
		"""
		url = self.__base_url+'/environments/'+environmentName+'/'+self.__releaseName(releaseName)+'/variables.json'
		req = urllib2.Request(url)
		r = self.__openurl(req, expectedCode=200)
		return json.load(r)

	def setReleaseVariables(self, environmentName, releaseName, variables):
		"""
		Sets release variables
		@param environmentName: the name of the release's environment
		@param releaseName: the name of the release or C{None} for snapshot release
		@param variables: an array of variables
		"""
		url = self.__base_url+'/environments/'+environmentName+'/'+self.__releaseName(releaseName)+'/variables'
		data, headers = self.__jsonData(variables)
		req = urllib2.Request(url, data, headers)
		self.__openurl(req, method='PUT', expectedCode=200)

	def getDeployments(self):
		"""
		Retrieves the list of deployments
		@return: an array of deployments
		"""
		url = self.__base_url+'/deployments.json'
		req = urllib2.Request(url)
		r = self.__openurl(req, expectedCode=200)
		return json.load(r)

	def getDeployment(self, environmentName, releaseName=None):
		"""
		Retrieves the deployment properties
		@param environmentName: the name of the release's environment
		@param releaseName: the name of the release or C{None} for snapshot release
		@return: the deployment's properties
		"""
		url = self.__base_url+'/deployments/'+environmentName+'/'+self.__releaseName(releaseName)+'.json'
		req = urllib2.Request(url)
		r = self.__openurl(req, expectedCode=200)
		return json.load(r)

	def downloadLightweightInstaller(self, environmentName, releaseName, downloadFile):
		"""
		Downloads a self-contained command-line installer (to install one server at a time) in the specified location
		@param environmentName: the name of the release's environment
		@param releaseName: the name of the release or C{None} for snapshot release
		@param downloadFile: the location to store the result into
		"""
		url = self.__base_url+'/deployments/'+environmentName+'/'+self.__releaseName(releaseName)+'/installer_lightweight.tar.gz'
		req = urllib2.Request(url)
		r = self.__openurl(req, expectedCode=200)
		out = open(downloadFile, 'wb')
		out.write(r.read())
		out.close()

	def downloadInstaller(self, environmentName, releaseName, downloadFile):
		"""
		Downloads an installer in the specified location
		@param environmentName: the name of the release's environment
		@param releaseName: the name of the release or C{None} for snapshot release
		@param downloadFile: the location to store the result into
		"""
		url = self.__base_url+'/deployments/'+environmentName+'/'+self.__releaseName(releaseName)+'/installer_cli.tar.gz'
		req = urllib2.Request(url)
		r = self.__openurl(req, expectedCode=200)
		out = open(downloadFile, 'wb')
		out.write(r.read())
		out.close()

	def __manage(self, environmentName, releaseName, serverName, artifactName, operation, force=False):
		params = {} 
		if serverName:
			params['serverName'] = serverName
		if artifactName:
			params['artifactName'] = artifactName
		if force:
			params['forceUndeploy'] = 'true'
		url = self.__base_url+'/deployments/'+environmentName+'/'+self.__releaseName(releaseName)+'/'+operation
		if params:
			url += '?'+urlencode(params)
		req = urllib2.Request(url)
		r = self.__openurl(req, method='POST', expectedCode=200)
		j = json.load(r)
		return j['ref'] if j else None

	def manageDeploy(self, environmentName, releaseName=None, serverName=None, artifactName=None):
		"""
		Initiates a deploy operation
		@param environmentName: the name of the release's environment
		@param releaseName: the name of the release or C{None} for snapshot release
		@param serverName: the name of a server or C{None} for all servers in environment
		@param artifactName: the name of an artifact or C{None} for all artifacts within server/environment
		@return: a deployment operation reference
		"""
		return self.__manage(environmentName, releaseName, serverName, artifactName, "deploy")

	def manageUndeploy(self, environmentName, releaseName=None, serverName=None, artifactName=None, forceUndeploy=False):
		"""
		Initiates an undeploy operation
		@param environmentName: the name of the release's environment
		@param releaseName: the name of the release or C{None} for snapshot release
		@param serverName: the name of a server or C{None} for all servers in environment
		@param artifactName: the name of an artifact or C{None} for all artifacts within server/environment
		@param forceUndeploy: if C{True} ignores errors and removes files (default is False)
		@return: a deployment operation reference
		"""
		return self.__manage(environmentName, releaseName, serverName, artifactName, "undeploy", forceUndeploy)

	def manageCheckIntegrity(self, environmentName, releaseName=None, serverName=None, artifactName=None):
		"""
		Initiates an check integrity operation
		@param environmentName: the name of the release's environment
		@param releaseName: the name of the release or C{None} for snapshot release
		@param serverName: the name of a server or C{None} for all servers in environment
		@param artifactName: the name of an artifact or C{None} for all artifacts within server/environment
		@return: a deployment operation reference
		"""
		return self.__manage(environmentName, releaseName, serverName, artifactName, "check")

	def manageStart(self, environmentName, releaseName=None, serverName=None, artifactName=None):
		"""
		Initiates a start executables operation
		@param environmentName: the name of the release's environment
		@param releaseName: the name of a release or C{None} for snapshot release
		@param serverName: the name of the server or C{None} for all servers in environment
		@param artifactName: the name of an artifact or C{None} for all artifacts within server/environment
		@return: a deployment operation reference
		"""
		return self.__manage(environmentName, releaseName, serverName, artifactName, "start")

	def manageStop(self, environmentName, releaseName=None, serverName=None, artifactName=None):
		"""
		Initiates a stop executables operation
		@param environmentName: the name of the release's environment
		@param releaseName: the name of the release or C{None} for snapshot release
		@param serverName: the name of a server or C{None} for all servers in environment
		@param artifactName: the name of an artifact or C{None} for all artifacts within server/environment
		@return: a deployment operation reference
		"""
		return self.__manage(environmentName, releaseName, serverName, artifactName, "stop")

	def manageStatus(self, environmentName, releaseName=None, serverName=None, artifactName=None):
		"""
		Initiates a status executables operation
		@param environmentName: the name of the release's environment
		@param releaseName: the name of the release or C{None} for snapshot release
		@param serverName: the name of a server or C{None} for all servers in environment
		@param artifactName: the name of an artifact or C{None} for all artifacts within server/environment
		@return: a deployment operation reference
		"""
		return self.__manage(environmentName, releaseName, serverName, artifactName, "status")

	def getOngoingOperation(self):
		"""
		Retrieves an ongoing deployment operation
		@return: a deployment operation reference or C{None} if no operation is in progress
		"""
		url = self.__base_url+'/deployments/ongoing.json'
		req = urllib2.Request(url)
		r = self.__openurl(req, expectedCode=200)
		j = json.load(r)
		return j['ref'] if j else None

	def getOperationStatus(self, ref):
		"""
		@return: C{200} if successfully completed, C{204} if the operation is in progress, C{410} if gone and C{400} in case of error
		"""
		url = self.__base_url+'/deployments/progress/status?ref='+ref
		req = urllib2.Request(url)
		r = self.__openurl(req)
		if r.code == 200 or r.code == 204 or r.code == 410 or r.code == 400:
			return code
		raise IOError('Error '+r.code)

	def getOperationProgress(self, ref):
		"""
		Retrieves the progress of a deployment operation
		@param ref: the reference of an ongoing deployment operation
		@return: the properties of the deployment operation
		"""
		url = self.__base_url+'/deployments/progress/status.json'
		url += '?ref='+ref
		req = urllib2.Request(url)
		r = self.__openurl(req, expectedCode=200)
		return json.load(r)

	def getProgressMessages(self, ref, serverName, artifactName=None):
		"""
		Retrieves the details of a deployment operation for a given server and or artifact
		@param ref: the reference of an ongoing deployment operation
		@param serverName: the name of the server for which info is requested
		@param artifactName: the name of an artifact for which info is requested. If C{None}, server-wide information is returned
		@return: the operation details
		"""
		params = {'ref': ref, 'serverName': serverName}
		if artifactName:
			params['artifactName'] = artifactName
		url = self.__base_url+'/deployments/progress/messages.json'
		url += '?'+urlencode(params)
		req = urllib2.Request(url)
		r = self.__openurl(req, expectedCode=200)
		return json.load(r)

	def manageCancel(self, ref, dontClear=False):
		"""
		Cancels an ongoing operation
		@param ref: the reference of an ongoing deployment operation
		@param dontClear: if C{True} the status is kept active
		"""
		url = self.__base_url+'/deployments/progress/cancel'
		url += '?ref='+ref
		if dontClear:
			url += '&dontClear=true'
		req = urllib2.Request(url)
		self.__openurl(req, method='POST')

	def sendCredentials(self, environmentName, serverName, sameForAllServers=False, credentials=None):
		"""
		Supply server credentials without storing them in kwatee
		@param environmentName: the name of an environment
		@param serverName: the name of the server
		@param sameForAllServers: if C{True}, these same credentials will be applied to all servers that need it.
		@param credentials: the optional credentials
		"""
		params = {'environmentName': environmentName, 'serverName': serverName}
		if sameForAllServers:
			params['sameForAll'] = 'true'
		url = self.__base_url+'/deployments/progress/credentials?'+urlencode(params)
		data, headers = self.__jsonData(credentials)
		req = urllib2.Request(url, data, headers)
		self.__openurl(req, method='POST')

	def getInfoContext(self):
		"""
		Retrieves kwatee information (version, ...)
		@return: kwatee properties
		"""
		url = self.__base_url+'/info/context.json'
		req = urllib2.Request(url)
		r = self.__openurl(req, expectedCode=200)
		return json.load(r)

	def getInfoPlatforms(self):
		"""
		Retrieves the available platforms (operating systems)
		@return: an array of platforms
		"""
		url = self.__base_url+'/info/platforms.json'
		req = urllib2.Request(url)
		r = self.__openurl(req, expectedCode=200)
		return json.load(r)

	def getInfoConduitTypes(self):
		"""
		Retrieves the available conduit types (ssh, ftp, ...)
		@return: an array of conduit types
		"""
		url = self.__base_url+'/info/conduitTypes.json'
		req = urllib2.Request(url)
		r = self.__openurl(req, expectedCode=200)
		return json.load(r)

	def getInfoServerPoolTypes(self):
		"""
		Retrieves the available server pool types (ec2, ...)
		@return: an array of server pool types
		"""
		url = self.__base_url+'/info/serverPoolTypes.json'
		req = urllib2.Request(url)
		r = self.__openurl(req, expectedCode=200)
		return json.load(r)

	def getLastStatus(self):
		"""
		@return: the status of the last call
		"""
		return self.__status

	def __releaseName(self, releaseName):
		if releaseName:
			return releaseName
		return 'snapshot'

	def __openurl(self, req, method='GET', expectedCode=None, error='Error', authenticationRequired=True):
		if authenticationRequired and not self.__authToken:
			raise IOError('Not connected')
		if self.__authToken:
			req.add_header('X-API-AUTH', self.__authToken)
		req.get_method = lambda: method
		r = urllib2.urlopen(req)
		self.__status = r.code
		if expectedCode and expectedCode != r.code:
			raise IOError(error+' (expected code='+str(expectedCode)+', got code='+str(r.code)+')')
		return r

	def __jsonData(self, data):
		if not data:
			return '', {'Content-Type': 'text/plain'}
		return json.dumps(data), {'Content-Type': 'application/json'}
