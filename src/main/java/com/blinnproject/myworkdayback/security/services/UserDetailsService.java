package com.blinnproject.myworkdayback.security.services;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public interface UserDetailsService {
  UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException;
}