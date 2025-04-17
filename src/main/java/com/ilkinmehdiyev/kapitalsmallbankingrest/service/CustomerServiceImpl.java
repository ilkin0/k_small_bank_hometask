package com.ilkinmehdiyev.kapitalsmallbankingrest.service;

import com.ilkinmehdiyev.kapitalsmallbankingrest.exception.CustomerNotFoundException;
import com.ilkinmehdiyev.kapitalsmallbankingrest.repository.CustomerRepository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerServiceImpl implements UserDetailsService {
  private final CustomerRepository customerRepository;

  @Override
  public CustomUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    var customer =
        customerRepository
            .findByPhoneNumber(username)
            .orElseThrow(
                () -> {
                  log.error("User with phone number {} not found", username);
                  return new CustomerNotFoundException(
                      "User with phone number %s not found".formatted(username));
                });

    return new CustomUserDetails(
        customer.id(), customer.uid(), customer.password(), customer.phoneNumber());
  }

  public record CustomUserDetails(Long id, UUID uid, String password, String phoneNumber)
      implements UserDetails {
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
      return List.of();
    }

    @Override
    public String getPassword() {
      return password;
    }

    @Override
    public String getUsername() {
      return phoneNumber;
    }

    @Override
    public boolean isAccountNonExpired() {
      return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
      return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
      return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
      return UserDetails.super.isEnabled();
    }
  }
}
