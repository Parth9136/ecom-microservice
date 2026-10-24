
package com.ecommerce.user.services;

import com.ecommerce.user.dto.AddressDTO;
import com.ecommerce.user.dto.UserRequest;
import com.ecommerce.user.dto.UserResponse;
import com.ecommerce.user.model.Address;
import com.ecommerce.user.model.User;
import com.ecommerce.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user;
    private Address address;
    private UserRequest userRequest;

    @BeforeEach
    void setUp() {
        address = new Address();
        address.setStreet("MG Road");
        address.setCity("Bengaluru");
        address.setState("Karnataka");
        address.setCountry("India");
        address.setZipcode("560001");

        user = new User();
        user.setId(1L);
        user.setFirstName("Parth");
        user.setLastName("Shukla");
        user.setEmail("parth@example.com");
        user.setPhone("9876543210");
        user.setAddress(address);

        userRequest = new UserRequest();
        userRequest.setFirstName("Parth");
        userRequest.setLastName("Shukla");
        userRequest.setEmail("parth@example.com");
        userRequest.setPhone("9876543210");

        AddressDTO addressDTO = new AddressDTO();
        addressDTO.setStreet("MG Road");
        addressDTO.setCity("Bengaluru");
        addressDTO.setState("Karnataka");
        addressDTO.setCountry("India");
        addressDTO.setZipcode("560001");

        userRequest.setAddress(addressDTO);
    }

    @Test
    void fetchAllUsers_shouldReturnMappedUsers() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserResponse> result = userService.fetchAllUsers();

        assertEquals(1, result.size());
        assertEquals("1", result.get(0).getId());
        assertEquals("Parth", result.get(0).getFirstName());
        assertEquals("Shukla", result.get(0).getLastName());
        assertEquals("parth@example.com", result.get(0).getEmail());
        assertEquals("9876543210", result.get(0).getPhone());

        verify(userRepository).findAll();
    }

    @Test
    void fetchAllUsers_whenNoUsersExist_shouldReturnEmptyList() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<UserResponse> result = userService.fetchAllUsers();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(userRepository).findAll();
    }

    @Test
    void addUser_shouldSaveUserWithCorrectDetails() {
        userService.addUser(userRequest);

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertEquals("Parth", savedUser.getFirstName());
        assertEquals("Shukla", savedUser.getLastName());
        assertEquals("parth@example.com", savedUser.getEmail());
        assertEquals("9876543210", savedUser.getPhone());

        assertNotNull(savedUser.getAddress());
        assertEquals("MG Road", savedUser.getAddress().getStreet());
        assertEquals("Bengaluru", savedUser.getAddress().getCity());
        assertEquals("Karnataka", savedUser.getAddress().getState());
        assertEquals("India", savedUser.getAddress().getCountry());
        assertEquals("560001", savedUser.getAddress().getZipcode());
    }

    @Test
    void addUser_whenAddressIsNull_shouldSaveUserWithoutAddress() {
        userRequest.setAddress(null);

        userService.addUser(userRequest);

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertEquals("Parth", savedUser.getFirstName());
        assertNull(savedUser.getAddress());
    }

    @Test
    void fetchUser_whenUserExists_shouldReturnMappedUser() {
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        Optional<UserResponse> result = userService.fetchUser(1L);

        assertTrue(result.isPresent());

        UserResponse response = result.get();

        assertEquals("1", response.getId());
        assertEquals("Parth", response.getFirstName());
        assertEquals("Shukla", response.getLastName());
        assertEquals("parth@example.com", response.getEmail());

        assertNotNull(response.getAddress());
        assertEquals("MG Road", response.getAddress().getStreet());
        assertEquals("Bengaluru", response.getAddress().getCity());
        assertEquals("Karnataka", response.getAddress().getState());
        assertEquals("India", response.getAddress().getCountry());
        assertEquals("560001", response.getAddress().getZipcode());

        verify(userRepository).findById(1L);
    }

    @Test
    void fetchUser_whenUserDoesNotExist_shouldReturnEmpty() {
        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        Optional<UserResponse> result = userService.fetchUser(99L);

        assertTrue(result.isEmpty());

        verify(userRepository).findById(99L);
    }

    @Test
    void fetchUser_whenUserHasNoAddress_shouldReturnUserWithoutAddress() {
        user.setAddress(null);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        Optional<UserResponse> result = userService.fetchUser(1L);

        assertTrue(result.isPresent());
        assertNull(result.get().getAddress());
    }

    @Test
    void updateUser_whenUserExists_shouldUpdateNamesAndReturnTrue() {
        User updatedUser = new User();
        updatedUser.setFirstName("UpdatedFirst");
        updatedUser.setLastName("UpdatedLast");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        boolean result = userService.updateUser(1L, updatedUser);

        assertTrue(result);
        assertEquals("UpdatedFirst", user.getFirstName());
        assertEquals("UpdatedLast", user.getLastName());

        verify(userRepository).findById(1L);
        verify(userRepository).save(user);
    }

    @Test
    void updateUser_whenUserDoesNotExist_shouldReturnFalse() {
        User updatedUser = new User();
        updatedUser.setFirstName("UpdatedFirst");
        updatedUser.setLastName("UpdatedLast");

        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        boolean result = userService.updateUser(99L, updatedUser);

        assertFalse(result);

        verify(userRepository).findById(99L);
        verify(userRepository, never()).save(any(User.class));
    }
}
