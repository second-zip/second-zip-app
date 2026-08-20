package com.secondzip.backend.report.service.external.client;

import lombok.extern.slf4j.Slf4j;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;

@Slf4j
// CODEF 비밀번호 암호화
public class CodefRsaEncryptor {

    /**
     plainText 평문 (예: 4자리 비밀번호)
     publicKeyBase64 CODEF 대시보드에서 발급받은 공개키 문자열
     */
    public static String encrypt(String plainText, String publicKeyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(spec);

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encrypted = cipher.doFinal(plainText.getBytes());

            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("CODEF 비밀번호 RSA 암호화 실패", e);
            return null;
        }
    }
}