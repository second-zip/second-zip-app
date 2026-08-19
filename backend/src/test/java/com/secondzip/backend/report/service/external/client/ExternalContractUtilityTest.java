package com.secondzip.backend.report.service.external.client;

import com.secondzip.backend.report.enums.TelecomProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalContractUtilityTest {

    @ParameterizedTest
    @MethodSource("telecomCodes")
    void mapsTelecomProviderToCodefContractCode(TelecomProvider provider, String expectedCode) {
        assertThat(provider.getCode()).isEqualTo(expectedCode);
    }

    @Test
    void encryptsPasswordWithTheSuppliedRsaPublicKey() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        String encrypted = CodefRsaEncryptor.encrypt("1234", publicKey);

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encrypted));
        assertThat(new String(decrypted, StandardCharsets.UTF_8)).isEqualTo("1234");
    }

    @Test
    void returnsNullWhenTheRsaPublicKeyIsInvalid() {
        assertThat(CodefRsaEncryptor.encrypt("1234", "not-a-public-key")).isNull();
    }

    private static Stream<Arguments> telecomCodes() {
        return Stream.of(
                Arguments.of(TelecomProvider.SKT, "0"),
                Arguments.of(TelecomProvider.KT, "1"),
                Arguments.of(TelecomProvider.LG_U_PLUS, "2")
        );
    }
}
