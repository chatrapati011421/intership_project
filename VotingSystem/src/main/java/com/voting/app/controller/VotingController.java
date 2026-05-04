package com.voting.app.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;
import com.voting.app.model.*;
import com.voting.app.repository.*;
import com.voting.app.service.FaceService;

@RestController
@RequestMapping("/api")
public class VotingController {

    @Autowired private VoterRepository voterRepo;
    @Autowired private CandidateRepository candidateRepo;
    @Autowired private AdminRepository adminRepo;
    @Autowired private FaceService faceService;
    @Autowired private JavaMailSender mailSender; 

    // --- ADMIN LOGIC ---
    @PostMapping("/admin/login")
    public String adminLogin(@RequestBody Map<String, String> req) {
        Admin a = adminRepo.findById(req.get("username")).orElse(null);
        return (a != null && a.getPassword().equals(req.get("password"))) ? "SUCCESS" : "INVALID";
    }
 // Check if you have this endpoint for the Voters stats
   
    // Check if you have this endpoint for the Chart results
    
    @PostMapping("/admin/register-voter")
    public String regV(@RequestBody Voter v) {
        if (voterRepo.existsById(v.getVoterId())) return "ALREADY_EXISTS";
        voterRepo.save(v);
        return "Voter Registered Successfully!";
    }
 // Corrected: Remove the extra "/api" from the string
    @GetMapping("/admin/voters")
    public List<Voter> getAllVoters() {
        return voterRepo.findAll();
    }

    // Corrected: Remove the extra "/api" from the string
    @GetMapping("/candidates")
    public List<Candidate> getCandidates() {
        return candidateRepo.findAll();
    }
    @PostMapping("/admin/register-candidate")
    public String regC(@RequestBody Candidate c) {
        candidateRepo.save(c);
        return "Candidate Registered Successfully!";
    }

//    @GetMapping("/candidates")
//    public List<Candidate> getCandidates() {
//        return candidateRepo.findAll();
//    }

    // --- VOTER VERIFICATION ---
    @PostMapping("/voter/verify")
    public String verifyV(@RequestBody Map<String, String> req) {
        Voter v = voterRepo.findById(req.get("voterId")).orElse(null);
        if (v == null) return "NOT_FOUND";
        if (v.isHasVoted()) return "ALREADY_VOTED";
        
        String liveFace = req.get("faceData").contains(",") ? req.get("faceData").split(",")[1] : req.get("faceData");
        String storedFace = v.getFaceData().contains(",") ? v.getFaceData().split(",")[1] : v.getFaceData();
        
        boolean match = faceService.compareFaces(liveFace, storedFace);
        return match ? "MATCH" : "MISMATCH";
    }

    // --- VOTING & AUTOMATIC EMAIL RECEIPT ---
    @PostMapping("/voter/vote")
    public String vote(@RequestParam String voterId, @RequestParam Long candidateId) {
        Optional<Voter> vOpt = voterRepo.findById(voterId);
        Optional<Candidate> cOpt = candidateRepo.findById(candidateId);

        if (vOpt.isPresent() && cOpt.isPresent()) {
            Voter v = vOpt.get();
            Candidate c = cOpt.get();

            if (v.isHasVoted()) return "ALREADY_VOTED";

            v.setHasVoted(true);
            v.setVotedFor(candidateId); // Save WHO they voted for
            c.setVoteCount(c.getVoteCount() + 1);

            voterRepo.save(v);
            candidateRepo.save(c);

            sendEmailReceipt(v.getEmail(), v.getName(), c.getName());
            return "SUCCESS";
        }
        return "ERROR";
    }

    // --- UPDATED DELETE LOGIC WITH SYNCHRONIZATION ---
    @DeleteMapping("/admin/delete")
    public String deleteData(@RequestParam String type, @RequestParam String scope, @RequestParam(required = false) String id) {
        if (type.equals("voter")) {
            if (scope.equals("all")) {
                // Wipe all voters and reset ALL candidate vote counts to 0
                voterRepo.deleteAll();
                List<Candidate> candidates = candidateRepo.findAll();
                for (Candidate c : candidates) {
                    c.setVoteCount(0);
                    candidateRepo.save(c);
                }
                return "All voters deleted and results reset.";
            } else {
                // Delete a single voter and subtract their vote from the candidate total
                Voter v = voterRepo.findById(id).orElse(null);
                if (v != null && v.isHasVoted() && v.getVotedFor() != null) {
                    Candidate c = candidateRepo.findById(v.getVotedFor()).orElse(null);
                    if (c != null) {
                        c.setVoteCount(Math.max(0, c.getVoteCount() - 1)); // Decrement count
                        candidateRepo.save(c);
                    }
                }
                voterRepo.deleteById(id);
                return "Voter deleted and vote removed from results.";
            }
        } else if (type.equals("candidate")) {
            if (scope.equals("all")) {
                candidateRepo.deleteAll();
                return "All candidates deleted.";
            } else {
                candidateRepo.deleteById(Long.parseLong(id));
                return "Candidate " + id + " deleted.";
            }
        }
        return "Invalid Request";
    }

    private void sendEmailReceipt(String toEmail, String voterName, String candidateName) {
        System.out.println("Attempting to send email to: " + toEmail); 

        if (toEmail == null || toEmail.isEmpty()) {
            System.out.println("ERROR: No email address found for this voter!");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Voting Confirmation - Digital Receipt");
            message.setText("Hello " + voterName + ",\n\nYour vote has been successfully recorded for: " + candidateName + ".\n\nThank you for using the Biometric Voting System.");
            mailSender.send(message);
            System.out.println("Email sent successfully to " + toEmail);
        } catch (Exception e) {
            System.err.println("Email failed: " + e.getMessage());
        }
    }
}