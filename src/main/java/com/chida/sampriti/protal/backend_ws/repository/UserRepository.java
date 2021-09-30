package com.chida.sampriti.protal.backend_ws.repository;

import com.chida.sampriti.protal.backend_ws.data.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository  extends JpaRepository<UserEntity, Long> {

    UserEntity findUserByUserName(String username);

    UserEntity findUserByEmail(String email);
}
