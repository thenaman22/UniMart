package com.unimart.api;

import com.unimart.domain.UserAccount;

public record AuthContext(UserAccount user) {
}
