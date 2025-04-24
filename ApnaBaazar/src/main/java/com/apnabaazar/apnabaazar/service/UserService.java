package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.exceptions.PasswordMismatchException;
import com.apnabaazar.apnabaazar.exceptions.ResourceNotFoundException;
import com.apnabaazar.apnabaazar.model.dto.AddressUpdateDTO;
import com.apnabaazar.apnabaazar.model.dto.UpdatePasswordDTO;
import com.apnabaazar.apnabaazar.model.users.Address;
import com.apnabaazar.apnabaazar.model.users.User;
import com.apnabaazar.apnabaazar.repository.AddressRepository;
import com.apnabaazar.apnabaazar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;
    private final MessageSource messageSource;

    public User getUserByEmail(String email) {
        Locale locale = LocaleContextHolder.getLocale();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", email);
                    String message = messageSource.getMessage("user.not.found", new Object[]{email}, locale);
                    return new UsernameNotFoundException(message);
                });
    }

    public Address getAddressById(String addressId) {
        Locale locale = LocaleContextHolder.getLocale();
        return addressRepository.findById(addressId)
                .orElseThrow(() -> {
                    String message = messageSource.getMessage("address.not.found", new Object[]{addressId}, locale);
                    return new ResourceNotFoundException(message);
                });
    }

    public boolean validateAddressOwnership(User user, String addressId) throws AccessDeniedException {
        Locale locale = LocaleContextHolder.getLocale();
        boolean ownsAddress = user.getAddresses().stream()
                .anyMatch(address -> address.getId().equals(addressId));
        if (!ownsAddress) {
            log.warn("Address [ID: {}] does not belong to user: {}", addressId, user.getEmail());
            String message = messageSource.getMessage("address.unauthorized", null, locale);
            throw new AccessDeniedException(message);
        }
        return true;
    }

    public void updatePassword(User user, UpdatePasswordDTO updatePasswordDTO) {
        Locale locale = LocaleContextHolder.getLocale();

        if (!passwordEncoder.matches(updatePasswordDTO.getOldPassword(), user.getPassword()))
            throw new PasswordMismatchException(messageSource.getMessage("password.old.incorrect", null, locale));

        if (passwordEncoder.matches(updatePasswordDTO.getNewPassword(), user.getPassword()))
            throw new PasswordMismatchException(messageSource.getMessage("password.new.password.incorrect", null, locale));

        if (!updatePasswordDTO.getNewPassword().equals(updatePasswordDTO.getConfirmPassword()))
            throw new PasswordMismatchException(messageSource.getMessage("password.mismatch", null, locale));

        user.setPassword(passwordEncoder.encode(updatePasswordDTO.getNewPassword()));
        user.setPasswordUpdateDate(LocalDateTime.now());
        userRepository.save(user);

        log.info("Password updated successfully for user: {}", user.getEmail());
    }

    public void updateAddress(User user, String addressId, AddressUpdateDTO addressUpdateDTO) throws AccessDeniedException {
        validateAddressOwnership(user, addressId);
        Address address = getAddressById(addressId);
        updateAddressFields(address, addressUpdateDTO);
        address.setLabel(getUpdatedValue(addressUpdateDTO.getLabel(), address.getLabel()));

        addressRepository.save(address);
        log.info("Address updated successfully for user: {}", user.getEmail());
    }

    public void deleteAddress(User user, String addressId) throws AccessDeniedException {
        if (validateAddressOwnership(user, addressId)) {
            addressRepository.deleteAddressById(addressId);
            log.info("Address deleted successfully for user: {}", user.getEmail());
        }
    }

    protected String getUpdatedValue(String newValue, String oldValue) {
        return (newValue != null && !newValue.isBlank()) ? newValue : oldValue;
    }

    void updateAddressFields(Address address, AddressUpdateDTO addressUpdateDTO) {
        address.setAddressLine(getUpdatedValue(addressUpdateDTO.getAddressLine(), address.getAddressLine()));
        address.setCity(getUpdatedValue(addressUpdateDTO.getCity(), address.getCity()));
        address.setState(getUpdatedValue(addressUpdateDTO.getState(), address.getState()));
        address.setZipCode(getUpdatedValue(addressUpdateDTO.getZipCode(), address.getZipCode()));
        address.setCountry(getUpdatedValue(addressUpdateDTO.getCountry(), address.getCountry()));
    }

}