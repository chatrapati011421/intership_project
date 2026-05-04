package com.voting.app.model;

import jakarta.persistence.*;

@Entity
@Table(name = "voters")
public class Voter {
    @Id
    private String voterId;
    private String name;
    private String email;
    
    // Tracks which candidate this voter chose
    private Long votedFor; 

    @Column(columnDefinition = "LONGTEXT")
    private String faceData;
    private boolean hasVoted = false;

    // Getters and Setters
    public String getVoterId() { return voterId; }
    public void setVoterId(String voterId) { this.voterId = voterId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Long getVotedFor() { return votedFor; }
    public void setVotedFor(Long votedFor) { this.votedFor = votedFor; }
    public String getFaceData() { return faceData; }
    public void setFaceData(String faceData) { this.faceData = faceData; }
    public boolean isHasVoted() { return hasVoted; }
    public void setHasVoted(boolean hasVoted) { this.hasVoted = hasVoted; }
}