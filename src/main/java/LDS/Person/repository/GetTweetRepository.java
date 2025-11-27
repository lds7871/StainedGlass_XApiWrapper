package LDS.Person.repository;

import LDS.Person.entity.GetTweet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * get_tweets 表数据访问
 */
@Repository
public interface GetTweetRepository extends JpaRepository<GetTweet, Long> {
    boolean existsByTweetId(String tweetId);
}