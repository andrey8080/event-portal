package itmo.is.eventportal.service.filter;

import itmo.is.eventportal.util.JwtTokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenUtil jwtTokenUtil;

	public JwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil) {
		this.jwtTokenUtil = jwtTokenUtil;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		String token = request.getHeader("Authorization");
		if (token != null) {
			if (jwtTokenUtil.validateJwtToken(token)) {
				String name = jwtTokenUtil.getEmailFromJwtToken(token);
				SecurityContextHolder.getContext().setAuthentication(
						new UsernamePasswordAuthenticationToken(name, null, new ArrayList<>())
				);
				System.out.println("Valid token for user: " + name);
			} else {
				System.out.println("Invalid token");
			}
		} else {
			System.out.println("Authorization header missing");
		}
		filterChain.doFilter(request, response);
	}
}

