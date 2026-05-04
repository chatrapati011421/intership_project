package com.voting.app.repository;

import com.voting.app.model.Voter; // Essential import!
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoterRepository extends JpaRepository<Voter, String> {
}