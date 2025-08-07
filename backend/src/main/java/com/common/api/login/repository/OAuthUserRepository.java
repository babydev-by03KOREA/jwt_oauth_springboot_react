package com.common.api.login.repository;

import com.common.api.login.entity.user.OAuthUser;
import com.common.api.login.enums.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OAuthUserRepository extends JpaRepository<OAuthUser, Long> {
    Optional<OAuthUser> findByProviderAndProviderUserId(
            OAuthProvider provider, String providerUserId
    );
}
