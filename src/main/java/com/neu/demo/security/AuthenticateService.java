package com.neu.demo.security;

import com.google.common.base.Strings;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.Date;

/**
 * @author lyupingdu
 * @date 2019-07-29.
 */
@Service
public class AuthenticateService {

    private static RSAKey rsaJWK;

    static {
        try {
            rsaJWK = new RSAKeyGenerator(2048).keyID("123").generate();
        } catch (JOSEException e) {
            e.printStackTrace();
        }
    }

    public static String generatePrivateKey() throws JOSEException {
        // create RSA-signer with the private key
        JWSSigner signer = new RSASSASigner(rsaJWK);

        // prepare JWT with claims set
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("sub")
                .issuer("iss")
                .expirationTime(new Date(System.currentTimeMillis() + 600 * 1000))
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJWK.getKeyID()).build(), claimsSet
        );

        // compute the RSA signature
        signedJWT.sign(signer);

        // serialize
        return signedJWT.serialize();
    }

    private static boolean verify(String serializedPrivateKey) throws ParseException, JOSEException {
        if (Strings.isNullOrEmpty(serializedPrivateKey)) {
            return false;
        }
        RSAKey reaPublicJWK = rsaJWK.toPublicJWK();
        SignedJWT signedJWT = SignedJWT.parse(serializedPrivateKey);
        JWSVerifier verifier = new RSASSAVerifier(reaPublicJWK);
        return signedJWT.verify(verifier);
    }

    public static boolean authenticate(HttpServletRequest request) throws ParseException, JOSEException {
        String auth = request.getHeader("Authorization");
        if (Strings.isNullOrEmpty(auth) || !auth.startsWith("Bearer ")) {
            return false;
        }
        int startIndex = "Bearer ".length();
        String token = auth.substring(startIndex);
        return verify(token);
    }
}
