package LDS.Person.service.impl;

import LDS.Person.entity.MediaLibrary;
import LDS.Person.repository.MediaLibraryRepository;
import LDS.Person.service.MediaLibraryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 媒体库服务实现类
 */
@Service
@Slf4j
@Transactional
public class MediaLibraryServiceImpl implements MediaLibraryService {
    
    @Autowired
    private MediaLibraryRepository mediaLibraryRepository;
    
    @Override
    public MediaLibrary save(MediaLibrary mediaLibrary) {
        if (mediaLibrary.getCreateTime() == null) {
            mediaLibrary.setCreateTime(LocalDateTime.now());
        }
        if (mediaLibrary.getEndTime() == null) {
            mediaLibrary.setEndTime(mediaLibrary.getCreateTime().plusHours(24));
        }
        if (mediaLibrary.getStatus() == null) {
            mediaLibrary.setStatus(0);
        }
        
        log.info("保存媒体记录: mediaId={}, mediaKey={}", 
                mediaLibrary.getMediaId(), 
                mediaLibrary.getMediaKey());
        
        return mediaLibraryRepository.save(mediaLibrary);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<MediaLibrary> findAll() {
        return mediaLibraryRepository.findAll();
    }
}
