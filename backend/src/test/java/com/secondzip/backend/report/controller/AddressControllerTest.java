package com.secondzip.backend.report.controller;

import com.secondzip.backend.common.exception.BusinessException;
import com.secondzip.backend.report.service.AddressSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AddressControllerTest {

    @Test
    @DisplayName("인증 사용자의 검색어를 주소 검색 서비스에 전달한다")
    void delegatesSearchForAuthenticatedAccount() {
        AddressSearchService service = mock(AddressSearchService.class);
        AddressController controller = new AddressController(service);
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(7L);

        assertEquals(
                HttpStatus.OK,
                controller.search("판교역로", authentication).getStatusCode()
        );

        verify(service).search("판교역로");
    }

    @Test
    @DisplayName("인증 정보가 없거나 익명이면 주소 API를 호출하지 않는다")
    void rejectsUnauthenticatedAndAnonymousRequests() {
        AddressSearchService service = mock(AddressSearchService.class);
        AddressController controller = new AddressController(service);
        Authentication unauthenticated = mock(Authentication.class);
        Authentication anonymous = mock(Authentication.class);
        when(anonymous.isAuthenticated()).thenReturn(true);
        when(anonymous.getPrincipal()).thenReturn("anonymousUser");

        assertThrows(
                BusinessException.class,
                () -> controller.search("판교역로", null)
        );
        assertThrows(
                BusinessException.class,
                () -> controller.search("판교역로", unauthenticated)
        );
        assertThrows(
                BusinessException.class,
                () -> controller.search("판교역로", anonymous)
        );
        verifyNoInteractions(service);
    }
}
