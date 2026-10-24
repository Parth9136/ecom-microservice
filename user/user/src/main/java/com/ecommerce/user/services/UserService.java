package com.ecommerce.user.services;

import com.ecommerce.user.dto.AddressDTO;
import com.ecommerce.user.dto.UserRequest;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.model.User;
import com.ecommerce.user.model.Address;
import com.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
//    private List<User> userList = new ArrayList<>();
//    private Long nextId = 1L;


    public List<UserResponse> fetchAllUsers(){
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    public void addUser(UserRequest userRequest){
//        user.setId(nextId++);
        User user = new User();
        updateUserFromRequest(user, userRequest);
        userRepository.save(user);
    }

//public void addUser(UserRequest userRequest) {
//    System.out.println("Received request: " + userRequest);
//    System.out.println("Received address: " + userRequest.getAddress());
//
//    User user = new User();
//    updateUserFromRequest(user, userRequest);
//
//    System.out.println("Address assigned to user: " + user.getAddress());
//
//    userRepository.save(user);
//}

    public Optional<UserResponse> fetchUser(Long id){

        return userRepository.findById(id)
                .map(this::mapToUserResponse);

//        return userList.stream()
//                .filter(user -> user.getId().equals(id))
//                .findFirst();
    }

    public boolean updateUser(Long id , User updatedUser){
        return userRepository.findById(id)
                .map(existingUser ->{
                    existingUser.setFirstName(updatedUser.getFirstName());
                    existingUser.setLastName(updatedUser.getLastName());
                    userRepository.save(existingUser);
                    return true;
                }).orElse(false);
    }


//    private void updateUserFromRequest(User user, UserRequest userRequest) {
//        user.setFirstName(userRequest.getFirstName());
//        user.setLastName(userRequest.getLastName());
//        user.setEmail(userRequest.getEmail());
//        user.setPhone(userRequest.getPhone());
//    }


    private void updateUserFromRequest(User user, UserRequest userRequest) {
        user.setFirstName(userRequest.getFirstName());
        user.setLastName(userRequest.getLastName());
        user.setEmail(userRequest.getEmail());
        user.setPhone(userRequest.getPhone());

        if (userRequest.getAddress() != null) {
            Address address = new Address();
            address.setStreet(userRequest.getAddress().getStreet());
            address.setCity(userRequest.getAddress().getCity());
            address.setState(userRequest.getAddress().getState());
            address.setCountry(userRequest.getAddress().getCountry());
            address.setZipcode(userRequest.getAddress().getZipcode());

            user.setAddress(address);
        }
    }



    private UserResponse mapToUserResponse(User user){
        UserResponse response = new UserResponse();
        response.setId(String.valueOf(user.getId()));
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());

        if (user.getAddress() != null){
            AddressDTO addressDTO = new AddressDTO();
            addressDTO.setStreet(user.getAddress().getStreet());
            addressDTO.setCity(user.getAddress().getCity());
            addressDTO.setState(user.getAddress().getState());
            addressDTO.setCountry(user.getAddress().getCountry());
            addressDTO.setZipcode(user.getAddress().getZipcode());
            response.setAddress(addressDTO);
        }
        return response;
    }
}
