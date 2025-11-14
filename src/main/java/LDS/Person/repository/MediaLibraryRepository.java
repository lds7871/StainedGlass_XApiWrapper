package LDS.Person.repository;

import LDS.Person.entity.MediaLibrary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 媒体库数据访问层
 */
@Repository
public interface MediaLibraryRepository extends JpaRepository<MediaLibrary, Long> {
}
