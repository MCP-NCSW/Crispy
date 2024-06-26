package com.mcp.crispy.auth.controller;

import com.mcp.crispy.admin.dto.AdminDto;
import com.mcp.crispy.auth.domain.AdminPrincipal;
import com.mcp.crispy.auth.domain.EmployeePrincipal;
import com.mcp.crispy.auth.domain.LoginRequest;
import com.mcp.crispy.auth.service.AuthenticationService;
import com.mcp.crispy.common.config.CrispyUserDetailsService;
import com.mcp.crispy.common.exception.InvalidLoginRequestException;
import com.mcp.crispy.common.utils.CookieUtil;
import com.mcp.crispy.common.utils.JwtUtil;
import com.mcp.crispy.employee.dto.EmployeeDto;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final CrispyUserDetailsService crispyUserDetailsService;
    private final JwtUtil jwtUtil;

    /**
     * 로그인 요청 처리
     * 배영욱 (24. 06. 01)
     * @param loginRequest 로그인 요청 정보 (username, password)
     * @param response HTTP 응답 객체
     * @return ResponseEntity 로그인 성공 시 사용자 정보, 실패 시 에러 메시지
     */
    @PostMapping("/login/v1")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        log.info("로그인 요청 수신: {}", loginRequest.getUsername());

        try {
            UserDetails userDetails = authenticationService.login(loginRequest.getUsername(), loginRequest.getPassword());
            log.info("로그인 성공: {}", userDetails.getUsername());

            if (userDetails instanceof EmployeePrincipal) {
                EmployeeDto employee = ((EmployeePrincipal) userDetails).getEmployee();
                setTokensAndCookies(employee.getAccessToken(), employee.getRefreshToken(), response);
                return ResponseEntity.ok(employee);
            } else if (userDetails instanceof AdminPrincipal) {
                AdminDto admin = ((AdminPrincipal) userDetails).getAdmin();
                setTokensAndCookies(admin.getAccessToken(), admin.getRefreshToken(), response);
                return ResponseEntity.ok(admin);
            }
            return ResponseEntity.status(401).body("로그인 실패");
        } catch (InvalidLoginRequestException ex) {
            return ResponseEntity.badRequest().body(ex.getErrors());
        }
    }

    /**
     * 현재 사용자 정보 조회
     * 배영욱 (24. 06. 01)
     * @param authentication 인증 정보
     * @return ResponseEntity 현재 사용자 정보
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(null);
        }

        String username = authentication.getName();
        UserDetails userDetails = crispyUserDetailsService.loadUserByUsername(username);
        log.info("현재 사용자: {}", userDetails);

        if (userDetails instanceof EmployeePrincipal) {
            return ResponseEntity.ok(((EmployeePrincipal) userDetails).getEmployee());
        } else if (userDetails instanceof AdminPrincipal) {
            return ResponseEntity.ok(((AdminPrincipal) userDetails).getAdmin());
        }

        return ResponseEntity.status(401).body(null);
    }

    /**
     * 리프레시 토큰을 통한 토큰 갱신
     * 배영욱 (24. 06. 01)
     * @param refreshToken 리프레시 토큰
     * @param response HTTP 응답 객체
     * @return ResponseEntity 갱신된 토큰을 포함한 사용자 정보
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue("refreshToken") String refreshToken, HttpServletResponse response) {

        log.info("Refresh 요청 수신: {}", refreshToken);

        if(!jwtUtil.validateToken(refreshToken)) {
            return ResponseEntity.status(401).body("검증된 토큰이 아닙니다.");
        }

        String username = jwtUtil.getUsernameFromToken(refreshToken);
        UserDetails userDetails = crispyUserDetailsService.loadUserByUsername(username);
        log.info("Refresh 토큰을 통한 사용자: {}", userDetails);

        if (userDetails instanceof EmployeePrincipal employeePrincipal) {
            String newAccessToken = jwtUtil.createAccessToken(employeePrincipal);
            log.info("newAccessToken: {}", newAccessToken);
            log.info("RefreshToken: {}", refreshToken);
            employeePrincipal.getEmployee().setAccessToken(newAccessToken);

            return ResponseEntity.ok(employeePrincipal.getEmployee());
        } else if (userDetails instanceof AdminPrincipal adminPrincipal) {
            String newAccessToken = jwtUtil.createAccessToken(adminPrincipal);
            adminPrincipal.getAdmin().setAccessToken(newAccessToken);
            return ResponseEntity.ok(adminPrincipal.getAdmin());
        }

        return ResponseEntity.status(401).body("Invalid user type");
    }


    /**
     * 로그아웃 처리
     * 배영욱 (24. 06. 01)
     * @param response HTTP 응답 객체
     * @return ResponseEntity 로그아웃 성공 메시지
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        authenticationService.logout(auth.getName());
        // 쿠키에서 토큰 삭제
        CookieUtil.deleteCookie(response, "accessToken");
        CookieUtil.deleteCookie(response, "refreshToken");

        log.info("로그아웃 성공: 쿠키 삭제 완료");

        return ResponseEntity.ok("로그아웃 성공");
    }

    /**
     * 토큰을 쿠키에 저장
     * 배영욱 (24. 06. 01)
     * @param accessToken 엑세스 토큰
     * @param refreshToken 리프레시 토큰
     * @param response HTTP 응답 객체
     */
    private void setTokensAndCookies(String accessToken, String refreshToken, HttpServletResponse response) {
        // JWT에서 직접 만료 시간을 추출하여 쿠키의 유효기간을 설정
        int accessTokenMaxAge = jwtUtil.getExpiryDurationFromToken(accessToken);
        int refreshTokenMaxAge = jwtUtil.getExpiryDurationFromToken(refreshToken);
        // 쿠키에 새 토큰 저장
        Cookie accessTokenCookie = new Cookie("accessToken", accessToken);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setHttpOnly(false);
        accessTokenCookie.setMaxAge(accessTokenMaxAge);
        response.addCookie(accessTokenCookie);

        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setHttpOnly(false);
        refreshTokenCookie.setMaxAge(refreshTokenMaxAge);
        response.addCookie(refreshTokenCookie);

        log.info("새로운 AccessToken: {}", accessToken);
        log.info("새로운 RefreshToken: {}", refreshToken);
    }
}