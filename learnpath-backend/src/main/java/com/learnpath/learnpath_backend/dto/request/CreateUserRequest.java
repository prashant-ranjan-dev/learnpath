package com.learnpath.learnpath_backend.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserRequest {
    private String email;
    private String fullName;
    private String password;
}
