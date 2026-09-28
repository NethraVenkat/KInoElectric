package com.backend.javaleap.service;

import com.backend.javaleap.entity.User;
import com.backend.javaleap.entity.enums.Role;
import com.backend.javaleap.model.AppProperties;
import com.backend.javaleap.model.UserPrincipal;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit tests for JWT creation and validation (no Spring context). */
class JwtServiceTest {

    private static final String SECRET =
            "UGFkYVNoYWt0aUtpbmV0aWNGbG9vckVuZXJneUhhcnZlc3RpbmdTZWNyZXRLZXkyMDI2IQ==";

    private static JwtService service(String secret, long expirationMs) {
        return new JwtService(new AppProperties(
                new AppProperties.Cors(List.of("http://localhost:5173")),
                new AppProperties.Jwt(secret, expirationMs),
                new AppProperties.Energy(2, 5, 8, 0.71),
                new AppProperties.Simulation(false, 1500),
                new AppProperties.Kinetic(0.95, 80, 85, 40, 24, 90)));
    }

    private static UserPrincipal principal() {
        User u = User.builder().username("accountant").passwordHash("x").fullName("Transit Accountant")
                .role(Role.ACCOUNTANT).enabled(true).build();
        u.setId(7L);
        return new UserPrincipal(u);
    }

    @Test
    void generatedTokenContainsUserClaims() {
        JwtService jwt = service(SECRET, 60_000);
        Optional<Claims> claims = jwt.parse(jwt.generateToken(principal()));
        assertThat(claims).isPresent();
        assertThat(claims.get().getSubject()).isEqualTo("accountant");
        assertThat(claims.get().get(JwtService.CLAIM_ROLE, String.class)).isEqualTo("ACCOUNTANT");
        assertThat(claims.get().get(JwtService.CLAIM_UID, Long.class)).isEqualTo(7L);
        assertThat(claims.get().getIssuer()).isEqualTo(JwtService.ISSUER);
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwt = service(SECRET, 60_000);
        String token = jwt.generateToken(principal());
        assertThat(jwt.isValid(token.substring(0, token.length() - 3) + "abc")).isFalse();
        assertThat(jwt.isValid("not-a-jwt")).isFalse();
    }

    @Test
    void tokenSignedWithOtherKeyIsRejected() {
        String otherSecret = "T3RoZXJTZWNyZXRLZXlGb3JUZXN0aW5nUGFkYVNoYWt0aTIwMjZYWVo=";
        String token = service(otherSecret, 60_000).generateToken(principal());
        assertThat(service(SECRET, 60_000).isValid(token)).isFalse();
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService jwt = service(SECRET, -1_000);
        assertThat(jwt.isValid(jwt.generateToken(principal()))).isFalse();
    }

    @Test
    void shortSecretFailsFast() {
        assertThatThrownBy(() -> service("c2hvcnQ=", 60_000)).isInstanceOf(IllegalStateException.class);
    }
}
