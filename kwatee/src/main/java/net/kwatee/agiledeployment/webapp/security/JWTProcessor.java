package net.kwatee.agiledeployment.webapp.security;

import java.security.SecureRandom;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;

import org.apache.commons.collections.MapUtils;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

class JWTProcessor {

	final private long expirationTimeout;
	final private byte[] secret;

	JWTProcessor(long expirationTimeout) {
		this.expirationTimeout = expirationTimeout;
		SecureRandom random = new SecureRandom();
		this.secret = new byte[32];
		random.nextBytes(this.secret);
	}

	String buildToken(Map<String, Object> claims) {
		if (MapUtils.isEmpty(claims))
			return null;
		try {
			JWSSigner signer = new MACSigner(this.secret);
			JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder();
			for (String claim : claims.keySet()) {
				builder.claim(claim, claims.get(claim));
			}
			JWTClaimsSet claimSet = builder
					.issueTime(new Date())
					.expirationTime(new Date(System.currentTimeMillis() + this.expirationTimeout))
					.build();
			SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimSet);
			signedJWT.sign(signer);
			String token = signedJWT.serialize();
			return token;
		} catch (KeyLengthException e) {} catch (JOSEException e) {}
		return null;
	}

	Map<String, Object> getClaims(String token) {
		try {
			SignedJWT signedJWT = SignedJWT.parse(token);
			JWSVerifier verifier = new MACVerifier(this.secret);
			if (signedJWT.verify(verifier)) {
				JWTClaimsSet claimSet = signedJWT.getJWTClaimsSet();
				return claimSet.getClaims();
			}
		} catch (ParseException | JOSEException e) {}
		return null;
	}

}
