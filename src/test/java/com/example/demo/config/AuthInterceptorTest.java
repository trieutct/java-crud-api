package com.example.demo.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.demo.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {

    static class Handlers {

        @RequireAuth
        public void activeOnly() {
        }

        @RequireAuth(allowInactive = true)
        public void inactiveAllowed() {
        }

        public void open() {
        }
    }

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthInterceptor authInterceptor;

    private boolean preHandle(String methodName) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer abc");
        HandlerMethod handler = new HandlerMethod(new Handlers(), methodName);
        return authInterceptor.preHandle(request, new MockHttpServletResponse(), handler);
    }

    @Test
    void requireAuthRejectsInactiveByDefault() throws Exception {
        assertThat(preHandle("activeOnly")).isTrue();

        verify(authService).authenticate("Bearer abc", false);
    }

    @Test
    void requireAuthPassesAllowInactiveOption() throws Exception {
        assertThat(preHandle("inactiveAllowed")).isTrue();

        verify(authService).authenticate("Bearer abc", true);
    }

    @Test
    void handlerWithoutAnnotationSkipsAuthentication() throws Exception {
        assertThat(preHandle("open")).isTrue();

        verifyNoInteractions(authService);
    }
}
