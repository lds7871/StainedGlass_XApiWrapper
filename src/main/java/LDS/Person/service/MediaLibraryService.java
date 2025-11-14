package LDS.Person.service;

import LDS.Person.entity.MediaLibrary;
import java.util.List;

/**
 * 媒体库服务接口
 */
public interface MediaLibraryService {
    
    /**
     * 保存媒体记录
     */
    MediaLibrary save(MediaLibrary mediaLibrary);
    
    /**
     * 查询所有媒体
     */
    List<MediaLibrary> findAll();
}
