package org.com.repair.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleAuthorizationInterceptorCouponRedeemTest {

    private RoleAuthorizationInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new RoleAuthorizationInterceptor();
    }

    @Test
    void shouldRejectCustomerWhenRedeemCoupon() throws Exception {
        MockHttpServletRequest request = buildRedeemRequest("customer");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals(403, response.getStatus());
    }

    @Test
    void shouldAllowAdminWhenRedeemCoupon() throws Exception {
        MockHttpServletRequest request = buildRedeemRequest("admin");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertTrue(allowed);
    }

    @Test
    void shouldAllowTechnicianWhenRedeemCoupon() throws Exception {
        MockHttpServletRequest request = buildRedeemRequest("technician");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertTrue(allowed);
    }

    private MockHttpServletRequest buildRedeemRequest(String role) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/gamification/coupon/redeem");
        request.setAttribute("authRole", role);
        return request;
    }
}
