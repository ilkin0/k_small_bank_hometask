package com.ilkinmehdiyev.kapitalsmallbankingrest.utils;

import lombok.Data;

@Data
public class SessionUser {

  private Long userId;
  private String userIpAddress;
  private String timeZone;
  private String role;
}
