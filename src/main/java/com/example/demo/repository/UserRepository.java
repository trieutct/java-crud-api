package com.example.demo.repository;

import com.example.demo.entity.User;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private final EntityManager entityManager;

    public UserRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<User> findAll() {
        return entityManager.createQuery("SELECT u FROM User u", User.class)
                .getResultList();
    }

    public Optional<User> findById(Integer id) {
        return Optional.ofNullable(entityManager.find(User.class, id));
    }

    public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(entityManager.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                .setParameter("email", email)
                .getSingleResultOrNull());
    }

    // email có unique index uk_users_email nên COUNT chỉ đọc tối đa một dòng
    public boolean existsByEmail(String email) {
        return entityManager.createQuery("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email", Boolean.class)
                .setParameter("email", email)
                .getSingleResult();
    }

    public boolean existsByEmailAndIdNot(String email, Integer id) {
        return entityManager
                .createQuery("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.id <> :id", Boolean.class)
                .setParameter("email", email)
                .setParameter("id", id)
                .getSingleResult();
    }

    // Chỉ xóa khi token vẫn khớp, để không xóa nhầm token mới mà client vừa đăng ký
    public void clearFcmToken(Integer id, String fcmToken) {
        entityManager.createQuery("UPDATE User u SET u.fcmToken = NULL WHERE u.id = :id AND u.fcmToken = :fcmToken")
                .setParameter("id", id)
                .setParameter("fcmToken", fcmToken)
                .executeUpdate();
    }

    // User mới (chưa có id) thì persist, User đã có id thì merge
    public User save(User user) {
        if (user.getId() == null) {
            entityManager.persist(user);
            return user;
        }
        return entityManager.merge(user);
    }

    public User saveAndFlush(User user) {
        User saved = save(user);
        entityManager.flush();
        return saved;
    }

    // Phải gọi trong cùng transaction đã tải user
    public void delete(User user) {
        entityManager.remove(user);
    }
}
