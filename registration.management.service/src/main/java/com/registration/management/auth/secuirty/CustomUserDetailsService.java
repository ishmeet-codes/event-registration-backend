package com.registration.management.auth.secuirty;

import com.registration.management.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;
    @org.springframework.transaction.annotation.Transactional
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        com.registration.management.auth.entities.User user = userRepository.findByEmail(username)
                .orElseThrow(()-> new UsernameNotFoundException("username not found"));
        user.getAuthorities();
        return user;
    }
}
