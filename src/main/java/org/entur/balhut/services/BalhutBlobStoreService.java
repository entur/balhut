package org.entur.balhut.services;

import org.entur.balhut.blobStoreRepository.BlobStoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BalhutBlobStoreService extends BlobStoreService {

    public BalhutBlobStoreService(
            @Value("${blobstore.gcs.balhut.bucket.name:balhut-dev}") String bucketName,
            @Autowired BlobStoreRepository repository) {
        super(bucketName, repository);
    }
}