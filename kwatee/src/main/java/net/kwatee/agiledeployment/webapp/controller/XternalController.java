
// /*
// * ${kwatee_copyright}
// */
//
// package net.kwatee.agiledeployment.webapp.controller;
//
// import org.springframework.stereotype.Controller;
// import org.springframework.web.bind.annotation.RequestMapping;
//
// @Controller
// @RequestMapping(value = "/api")
// /**
// *
// * @author mac
// *
// */
// public class XternalController {
//
// // private final static Logger LOG = LoggerFactory.getLogger(XternalController.class);
//
// // @RequestMapping(method = RequestMethod.GET, value = "/xternal/{token:.+}")
// // public void xternalRequest(HttpServletResponse res,
// // @PathVariable(value = "token") String token) throws IOException, KwateeException {
// // APIToken apiToken = APIToken.getInstance(token);
// // if (apiToken != null) {
// // UserShortDto tempUser = new UserShortDto();
// // tempUser.setLogin("$" + apiToken.getUserName());
// // Authority auth = new Authority();
// // auth.setAuthority(Authority.ROLE_EXTERNAL);
// // tempUser.getAuthorities().add(auth);
// // UsernamePasswordAuthenticationToken tempAuthentication = new UsernamePasswordAuthenticationToken(tempUser,
// StringUtils.EMPTY,
// tempUser.getAuthorities());
// // Authentication saveAuthentication = SecurityContextHolder.getContext().getAuthentication();
// // try {
// // SecurityContextHolder.getContext().setAuthentication(tempAuthentication);
// // String[] command = apiToken.getOptional().split(",");
// // if ("export".equals(command[0])) {
// // this.adminHandler.adminExport(true, res);
// // return;
// // }
// // if ("deployment".equals(command[0])) {
// // this.deploymentHandler.deploymentDownloadInstaller(command[1], command[2], res);
// // return;
// // }
// // if ("info".equals(command[0])) {
// // this.infoHandler.infoContext(res);
// // return;
// // }
// // } finally {
// // SecurityContextHolder.getContext().setAuthentication(saveAuthentication);
// // }
// // }
// // res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
// // }
// //
// // @RequestMapping(method = RequestMethod.GET, value =
// "/external_url/deployment/{environmentName}/{releaseName}.url")
// // public void xternalObtainDownloadInstallerUrl(HttpServletResponse res,
// // @PathVariable(value = "environmentName") String environmentName,
// // @PathVariable(value = "releaseName") String releaseName) {
// // LOG.info("xternalObtainDownloadInstallerUrl {}-{}", environmentName, releaseName);
// // ServletHelper.sendData(res, getExternalRef("deployment," + environmentName + "," + releaseName),
// HttpServletResponse.SC_OK);
// // }
// //
// // @RequestMapping(method = RequestMethod.GET, value = "/external_url/export.url")
// // public void xternalObtainExportUrl(HttpServletResponse res) {
// // LOG.info("xternalObtainExportUrl");
// // ServletHelper.sendData(res, getExternalRef("export"), HttpServletResponse.SC_OK);
// // }
// //
// // @RequestMapping(method = RequestMethod.GET, value = "/external_url/info.url")
// // public void xternalObtainInfoUrl(HttpServletResponse res) {
// // LOG.info("xternalObtainInfoUrl");
// // ServletHelper.sendData(res, getExternalRef("info"), HttpServletResponse.SC_OK);
// // }
// //
// // private String getExternalRef(String optional) {
// // APIToken token = APIToken.getInstance("external", StringUtils.EMPTY);
// // token.setOptional(optional);
// // return token.getEncryptedToken();
// // }
//}