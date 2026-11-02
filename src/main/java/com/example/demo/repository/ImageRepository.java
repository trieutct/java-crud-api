package com.example.demo.repository;

import com.example.demo.entity.Image;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ImageRepository {

    private final EntityManager entityManager;

    public ImageRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<Image> findAll() {
        return entityManager.createQuery("SELECT i FROM Image i", Image.class)
                .getResultList();
    }

    public Optional<Image> findById(Integer id) {
        return Optional.ofNullable(entityManager.find(Image.class, id));
    }

    public Image save(Image image) {
        entityManager.persist(image);
        return image;
    }
}
