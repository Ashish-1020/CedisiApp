package com.example.ragApp.repository;

import com.example.ragApp.data.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Modifying
    @Transactional
    @Query("update User u set u.noOfTokensConsumed = coalesce(u.noOfTokensConsumed, 0) + :delta where u.id = :userId")
    int incrementTokensConsumed(@Param("userId") String userId, @Param("delta") int delta);
}
