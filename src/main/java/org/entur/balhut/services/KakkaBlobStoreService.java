package org.entur.balhut.services;

import org.entur.balhut.blobStoreRepository.BlobStoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class KakkaBlobStoreService extends BlobStoreService {

    public KakkaBlobStoreService(
            @Value("${blobstore.gcs.kakka.bucket.name:kakka-dev}") String bucketName,
            @Autowired BlobStoreRepository repository) {
        super(bucketName, repository);
    }
}