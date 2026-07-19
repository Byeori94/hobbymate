package com.byeori.hobbymate.auth.security;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.byeori.hobbymate.member.dao.MemberDao;
import com.byeori.hobbymate.member.vo.MemberAuthInfo;

@Service
public class HobbyMateUserDetailsService implements UserDetailsService {

    private static final Pattern LOGIN_ID_PATTERN = Pattern.compile("^[a-z][a-z0-9]{4,19}$");

    private final MemberDao memberDao;

    public HobbyMateUserDetailsService(MemberDao memberDao) {
        this.memberDao = memberDao;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String loginId = username == null ? "" : username.trim();
        if (!LOGIN_ID_PATTERN.matcher(loginId).matches()) {
            throw authenticationFailed();
        }

        MemberAuthInfo member = memberDao.findAuthByLoginId(loginId);
        if (member == null || !"ACTIVE".equals(member.memberStatus())) {
            throw authenticationFailed();
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if ("ADMIN".equals(member.memberRole())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        return new HobbyMateUserDetails(
                member.memberId(),
                member.loginId(),
                member.password(),
                member.nickname(),
                member.profileImageUrl(),
                authorities);
    }

    private UsernameNotFoundException authenticationFailed() {
        return new UsernameNotFoundException("Authentication failed");
    }
}
