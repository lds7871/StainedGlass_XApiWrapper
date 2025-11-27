package LDS.Person.service;

import LDS.Person.entity.GetTweet;
import LDS.Person.repository.GetTweetRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 持久化存储 Twitter 推文数据
 */
@Service
@Slf4j
public class GetTweetStorageService {

    @Autowired
    private GetTweetRepository getTweetRepository;

    @Transactional
    public void saveTweets(List<Map<String, Object>> tweets) {
        if (tweets == null || tweets.isEmpty()) {
            return;
        }

        List<GetTweet> toSave = new ArrayList<>();

        for (Map<String, Object> tweet : tweets) {
            String tweetId = safeString(tweet.get("id"));
            if (tweetId == null || tweetId.isBlank()) {
                continue;
            }

            if (getTweetRepository.existsByTweetId(tweetId)) {
                log.debug("推文 {} 已存在，跳过保存", tweetId);
                continue;
            }

            GetTweet entity = new GetTweet();
            entity.setTweetId(tweetId);
            entity.setAuthorId(safeString(tweet.get("author_id")));
            entity.setText(safeString(tweet.get("text")));

            Instant createdAt = parseInstant(safeString(tweet.get("created_at")));
            entity.setCreatedAt(createdAt != null ? createdAt : Instant.now());

            toSave.add(entity);
        }

        if (!toSave.isEmpty()) {
            getTweetRepository.saveAll(toSave);
            log.info("已持久化 {} 条新推文", toSave.size());
        }
    }

    private String safeString(Object value) {
        return value instanceof String ? (String) value : (value != null ? value.toString() : null);
    }

    private Instant parseInstant(String isoTime) {
        if (isoTime == null || isoTime.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(isoTime);
        } catch (DateTimeParseException e) {
            log.warn("解析推文时间失败: {}", isoTime);
            return null;
        }
    }
}