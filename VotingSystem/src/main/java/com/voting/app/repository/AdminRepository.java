package com.voting.app.repository;

import com.voting.app.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminRepository extends JpaRepository<Admin, String> {
    // String is used here because 'username' is the Primary Key
}