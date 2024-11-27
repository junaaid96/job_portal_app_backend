package org.example.jobportalappbackend.service;

import org.example.jobportalappbackend.repository.UserRepository;
import org.example.jobportalappbackend.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    UserRepository userRepository;

    public User saveUser(User user) {
        return userRepository.save(user);
    }
}
