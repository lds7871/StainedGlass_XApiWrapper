package LDS.Person.repository;

import LDS.Person.entity.TwitterToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Twitter Token 数据访问对象
 */
@Repository
public interface TwitterTokenRepository extends JpaRepository<TwitterToken, Long> {
    /**
     * 根据 Twitter 用户 ID 查找 token
     */
    Optional<TwitterToken> findByTwitterUserId(String twitterUserId);

    /**
     * 查找所有即将过期的 token（用于定时刷新）
     */
    List<TwitterToken> findAll();
}
