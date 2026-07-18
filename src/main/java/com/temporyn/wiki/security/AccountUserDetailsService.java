package com.temporyn.wiki.security;

import com.temporyn.wiki.repository.AccountRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AccountUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    public AccountUserDetailsService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return accountRepository.findByUsernameAndEnabledTrue(username)
                .map(account -> User.withUsername(account.getUsername())
                        .password(account.getPassword())
                        .roles(account.getRole())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("계정을 찾을 수 없습니다: " + username));
    }
}
